package de.unimannheim.spa.process.rest;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.jena.ext.com.google.common.base.Joiner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseEntity.BodyBuilder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.noodlesandwich.rekord.Rekord;
import com.noodlesandwich.rekord.serialization.MapSerializer;

import de.unima.core.application.SPA;
import de.unima.core.domain.model.DataBucket;
import de.unima.core.domain.model.DataPool;
import de.unima.core.domain.model.Project;
import de.unimannheim.spa.process.rekord.builders.DataPoolBuilder;
import de.unimannheim.spa.process.rekord.builders.ProjectBuilder;
import de.unimannheim.spa.process.util.FileUtils;

@RestController
@RequestMapping("/projects")
public class ProjectRestController {
  
  private final static String PROJECT_BASE_URL = "http://www.uni-mannheim.de/spa/Project/";
  private final static String PROCESS_BASE_URL = "http://www.uni-mannheim.de/spa/DataBucket/";
  
  private final static MediaType JSON_CONTENT_TYPE = new MediaType(MediaType.APPLICATION_JSON.getType(), 
                                                    MediaType.APPLICATION_JSON.getSubtype(),
                                                    Charsets.UTF_8);
  
  private final Joiner joinerOnEmpty = Joiner.on("");
  
  private final SPA spaService;
  
  @Autowired
  public ProjectRestController(SPA spaService) {
      this.spaService = spaService;
  }
  
  @RequestMapping(method = RequestMethod.GET)
  public ResponseEntity<List<Map<String, Object>>> getAllProjects(){
      List<Map<String, Object>> allProjects = Lists.newArrayList();
      for(Project project : spaService.findAllProjects()){
        Rekord<Project> projectTmp = ProjectBuilder.rekord.with(ProjectBuilder.id, project.getId())
                                                   .with(ProjectBuilder.label, project.getLabel())
                                                   .with(ProjectBuilder.dataPools, DataPoolBuilder.rekord.with(DataPoolBuilder.buckets, project.getDataPools().get(0).getDataBuckets()))
                                                   .with(ProjectBuilder.linkedSchemas, project.getLinkedSchemas());
        allProjects.add(projectTmp.serialize(new MapSerializer()));
      }
      return ResponseEntity.ok()
                           .contentType(JSON_CONTENT_TYPE)
                           .body(allProjects);
  }
  
  @RequestMapping(method = RequestMethod.POST)
  public ResponseEntity<Map<String, Object>> createProject(@RequestParam String projectLabel){
      Project projectCreated = spaService.createProject(projectLabel);
      spaService.saveProject(projectCreated);
      spaService.createDataPool(projectCreated, "Default DataPool"); 
      Rekord<Project> projectTmp = ProjectBuilder.rekord.with(ProjectBuilder.id, projectCreated.getId())
                                                        .with(ProjectBuilder.label, projectCreated.getLabel())
                                                        .with(ProjectBuilder.dataPools, DataPoolBuilder.rekord.with(DataPoolBuilder.buckets, projectCreated.getDataPools().get(0).getDataBuckets()))
                                                        .with(ProjectBuilder.linkedSchemas, projectCreated.getLinkedSchemas());
      return ResponseEntity.status(HttpStatus.CREATED)
                           .contentType(JSON_CONTENT_TYPE)
                           .body(projectTmp.serialize(new MapSerializer()));
  }
  
  @RequestMapping(value="/{projectID}/processes", method = RequestMethod.GET)
  public ResponseEntity<Map<String, Object>> getAllProcesses(@PathVariable String projectID){
      return spaService.findProjectById(PROJECT_BASE_URL+projectID)
                       .map(project -> project.getDataPools())
                       .map(processes -> {
                         Rekord<DataPool> dataPoolTmp = DataPoolBuilder.rekord.with(DataPoolBuilder.buckets, processes.get(0).getDataBuckets());
                         return ResponseEntity.ok()
                               .contentType(JSON_CONTENT_TYPE)
                               .body(dataPoolTmp.serialize(new MapSerializer()));
                         })
                       .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                               .contentType(JSON_CONTENT_TYPE)
                               .body(Collections.emptyMap())); 
  }
  
  @RequestMapping(value="/{projectID}/processes", method = RequestMethod.POST)
  public ResponseEntity<Object> createProcessWithFile(@PathVariable("projectID") String projectID,
                                                                    @RequestParam("processLabel") String processLabel,
                                                                    @RequestParam("format") String format,
                                                                    @RequestPart("processFile") MultipartFile processFile) throws IllegalStateException, IOException{
      ResponseEntity<Object> resp = null;
      if(spaService.findProjectById(PROJECT_BASE_URL+projectID).isPresent()){
        Project project = spaService.findProjectById(PROJECT_BASE_URL+projectID).get();
        final DataPool dataPool = project.getDataPools().get(0);
        File fileToImport = FileUtils.convertMultipartToFile(processFile);
        DataBucket process = spaService.importData(fileToImport, format, processLabel, dataPool);
        fileToImport.delete();
        resp = ResponseEntity.status(HttpStatus.CREATED)
                             .contentType(JSON_CONTENT_TYPE)
                             .body(process);
      } else {
        resp = ResponseEntity.status(HttpStatus.NOT_FOUND)
            .contentType(JSON_CONTENT_TYPE)
            .body(null); 
      }      
      return resp;
  }
  
  @RequestMapping(value="/importformats", method = RequestMethod.GET)
  public ResponseEntity<List<String>> getSupportedImportFormats(){
      return ResponseEntity.status(HttpStatus.OK)
                           .contentType(JSON_CONTENT_TYPE)
                           .body(spaService.getSupportedImportFormats());
  }
  
  @RequestMapping(value="/{projectID}/processes/{processID}", method = RequestMethod.DELETE)
  public ResponseEntity<Void> deleteProcessFromProject(@PathVariable("projectID") String projectID, 
                                                       @PathVariable("processID") String processID){
      BodyBuilder resp = null;      
      final Optional<DataPool> dataPool = spaService.findProjectById(joinerOnEmpty.join(PROJECT_BASE_URL, projectID))
                                                    .map(project -> project.getDataPools().get(0));
      final Optional<DataBucket> dataBucketToRemove = (dataPool.isPresent()) ? dataPool.get().findDataBucketById(joinerOnEmpty.join(PROCESS_BASE_URL, processID)) 
                                                                             : Optional.empty();
      if(dataPool.isPresent() && dataBucketToRemove.isPresent()){
        spaService.removeDataBucket(dataPool.get(), dataBucketToRemove.get());
        resp = ResponseEntity.status(HttpStatus.OK);
      } else {
        resp = ResponseEntity.status(HttpStatus.NOT_FOUND);
      }
      return resp.build();
  }
  
}
