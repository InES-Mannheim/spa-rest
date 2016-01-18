package de.unimannheim.spa.process.rest;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.noodlesandwich.rekord.Rekord;
import com.noodlesandwich.rekord.serialization.MapSerializer;

import de.unima.core.application.SPA;
import de.unima.core.domain.model.Project;
import de.unimannheim.spa.process.rekord.builders.ProjectBuilder;

@RestController
@RequestMapping("/projects")
public class ProjectRestController {
  
  private final static MediaType JSON_CONTENT_TYPE = new MediaType(MediaType.APPLICATION_JSON.getType(), 
                                                    MediaType.APPLICATION_JSON.getSubtype(),
                                                    Charsets.UTF_8);
  
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
                                                   .with(ProjectBuilder.dataPools, project.getDataPools())
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
      Rekord<Project> projectTmp = ProjectBuilder.rekord.with(ProjectBuilder.id, projectCreated.getId())
                                                        .with(ProjectBuilder.label, projectCreated.getLabel())
                                                        .with(ProjectBuilder.dataPools, projectCreated.getDataPools())
                                                        .with(ProjectBuilder.linkedSchemas, projectCreated.getLinkedSchemas());
      return ResponseEntity.status(HttpStatus.CREATED)
                           .contentType(JSON_CONTENT_TYPE)
                           .body(projectTmp.serialize(new MapSerializer()));
  }
  
}
