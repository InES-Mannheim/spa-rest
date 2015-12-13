package de.unima.core.domain.service;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.jena.ext.com.google.common.collect.Lists;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.out.XesXmlSerializer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.unima.core.domain.DataBucket;
import de.unima.core.domain.DataPool;
import de.unima.core.domain.Project;
import de.unima.core.domain.Repository;
import de.unima.core.domain.Schema;
import de.unima.core.io.impl.BPMN20FileImpl;
import de.unima.core.io.impl.BPMN20ImporterImpl;
import de.unima.core.io.impl.OntModelToXESExporter;
import de.unima.core.io.impl.XMLFileImpl;
import de.unima.core.io.impl.XMLToOntModelImporter;
import de.unima.core.io.impl.XSDToOntModelImporter;

public class StorageIntegrationTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(StorageIntegrationTest.class);
	
	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();
	
	private RepositoryService persistentService;
	
	@Before
	public void setUp() throws IOException{
		this.persistentService = RepositoryService.withDataInFolder(temporaryFolder.newFolder().toPath());
	}
	
	@Test
	public void storeFindAndDelete10Bpmn2Schemas() throws IOException {
		final Model bpmn2Schema = loadBpmn2Schema();
		LOGGER.info(String.format("Storing BPMN 2 schema with %s statements. 10 times", bpmn2Schema.size()));
		final List<Schema> schemas = IntStream.range(0, 10).mapToObj(number -> persistentService.addDataAsNewSchema("Schema Nr. "+number, bpmn2Schema)).collect(Collectors.toList());
		schemas.forEach(schema -> assertThat(persistentService.findDataOfSchema(schema).isPresent(), is(true)));
		schemas.forEach(schema -> assertThat(persistentService.findDataOfSchema(schema).get().size(),is(bpmn2Schema.size())));
		
		final List<Schema> foundSchemas = persistentService.findAllSchemas();
		assertThat(foundSchemas, hasItems(schemas.toArray(new Schema[]{})));
		
		schemas.forEach(persistentService::deleteSchema);
		schemas.forEach(schema -> assertThat(persistentService.findDataOfSchema(schema).isPresent(), is(false)));
	}
	
	@Test
	public void simpleCrudUsageScenario() throws IOException{
		LOGGER.info("Simple CRUD Usage scenario.");
		final Schema schema = persistentService.addDataAsNewSchema("BPMN 2.0 ontology", loadBpmn2Schema());
		final Project project = persistentService.createPersistentProjectWithGeneratedId("Test project with BPMN 2.0 schema");
		final Repository repository = project.getRepository();
		final DataPool dataPool = persistentService.createPeristentDataPoolForProjectWithGeneratedId(project, "Test data pool with some data.");
		final DataBucket bucket = persistentService.addDataAsNewDataBucketToDataPool(dataPool, "Partial process data which is consitent with the schema.", createProcessData());
		
		project.linkSchema(schema);
		persistentService.saveProject(project);
	
		assertThat(project.getRepository().getSchemas(), hasItem(schema));
		assertThat(project.getLinkedSchemas(), hasItem(schema));
		assertThat(project.getDataPools(), hasItem(dataPool));
		assertThat(dataPool.getDataBuckets(), hasItem(bucket));
		
		persistentService.deleteDataPool(dataPool);
		assertThat(project.getDataPools(), not(hasItem(dataPool)));
		assertThat(persistentService.findDataOfDataBucket(bucket).isPresent(), is(false));

		persistentService.deleteSchema(schema);
		assertThat(project.getLinkedSchemas(), not(hasItem(schema)));
		
		persistentService.deleteProject(project);
		assertThat(repository.getProjects(), not(hasItem(project)));
	}
	
	@Test
	public void crudUsageScenarioWithFindAll() throws IOException{
		LOGGER.info("CRUD Usage scenario with find all.");
		final Model bpmn2Schema = loadBpmn2Schema();
		final Schema bpmnSchema1 = persistentService.addDataAsNewSchema("BPMN 2.0 ontology", bpmn2Schema);
		final Schema bpmnSchema2 = persistentService.addDataAsNewSchema("BPMN 2.0 ontology", bpmn2Schema);
		final Project project1 = persistentService.createPersistentProjectWithGeneratedId("First Test project with BPMN 2.0 schema");
		final Project project2 = persistentService.createPersistentProjectWithGeneratedId("Second Test project with BPMN 2.0 schema");
		final DataPool dataPool1 = persistentService.createPeristentDataPoolForProjectWithGeneratedId(project1, "Test data pool with some data.");
		final DataPool dataPool2 = persistentService.createPeristentDataPoolForProjectWithGeneratedId(project2, "Test data pool with some data.");
		final DataBucket bucket1 = persistentService.addDataAsNewDataBucketToDataPool(dataPool1, "Partial process data which is consitent with the schema.", createProcessData());
		final DataBucket bucket2 = persistentService.addDataAsNewDataBucketToDataPool(dataPool2, "Partial process data which is consitent with the schema.", createProcessData());
		
		assertThat(persistentService.findAllDataPools(), hasItems(dataPool1, dataPool2));
		assertThat(persistentService.findAllProjects(), hasItems(project1, project2));
		assertThat(persistentService.findAllSchemas(), hasItems(bpmnSchema1, bpmnSchema2));
		
		final List<Project> fullyLoadedProjects = persistentService.findAllProjects()
				.stream()
				.map(project -> persistentService.findProjectById(project.getId()))
				.filter(Optional::isPresent)
				.map(Optional::get)
				.collect(Collectors.toList());
		assertThat(fullyLoadedProjects, hasSize(2));
		final List<DataPool> fullyLoadedDataPools = fullyLoadedProjects.stream().flatMap(project -> project.getDataPools().stream()).collect(Collectors.toList());
		assertThat(fullyLoadedDataPools, hasItem(dataPool1));
		assertThat(fullyLoadedDataPools, hasItem(dataPool2));
		final List<DataBucket> fullyLoadDataBuckets = fullyLoadedDataPools.stream().flatMap(pool -> pool.getDataBuckets().stream()).collect(Collectors.toList());
		assertThat(fullyLoadDataBuckets, hasItem(bucket1));
		assertThat(fullyLoadDataBuckets, hasItem(bucket2));
	}
	
	@Test
	public void Bpmn2ImportIntegrationTest() throws IOException{
		LOGGER.info("BPM 2 import test.");
		// Import schema for BPMN 2.0
		final Schema schema = persistentService.addDataAsNewSchema("BPMN 2.0", loadBpmn2Schema());
		// Create new project with specified schema
		final Project project = persistentService.createPersistentProjectWithGeneratedId("Test Project");
		project.linkSchema(schema);
		persistentService.saveProject(project);
		// Create data pool in project
		final DataPool dataPool = persistentService.createPeristentDataPoolForProjectWithGeneratedId(project, "Sample Data Pool");
		// Import data as new data bucket
		final BPMN20ImporterImpl importer = new BPMN20ImporterImpl("http://spa.org/TestProject/SampleDataPool#");
		final OntModel importedData = importer.importData(new BPMN20FileImpl(getFilePath("example-spa.bpmn").toString()));
		final DataBucket bucket = persistentService.addDataAsNewDataBucketToDataPool(dataPool, "Example SPA process", importedData);
		// Load data according to data schema; not consistent with data model
		final Model m = persistentService.findDataOfDataBucket(bucket).get();
		
		// assert that statements are loaded
		final List<String> expectedStringStatements = Lists.newArrayList(
				"[http://spa.org/TestProject/SampleDataPool#StartEvent_1, http://www.w3.org/1999/02/22-rdf-syntax-ns#type, http://dkm.fbk.eu/index.php/BPMN2_Ontology#startEvent]"
				,"[http://spa.org/TestProject/SampleDataPool#StartEvent_1, http://dkm.fbk.eu/index.php/BPMN2_Ontology#id, \"StartEvent_1\"]"
				,"[http://spa.org/TestProject/SampleDataPool#StartEvent_1, http://spa.org/TestProject/SampleDataPool#isInterrupting, \"true\"^^http://www.w3.org/2001/XMLSchema#boolean]"
				,"[http://spa.org/TestProject/SampleDataPool#Process_1, http://www.w3.org/1999/02/22-rdf-syntax-ns#type, http://dkm.fbk.eu/index.php/BPMN2_Ontology#process]"
				,"[http://spa.org/TestProject/SampleDataPool#Process_1, http://dkm.fbk.eu/index.php/BPMN2_Ontology#id, \"Process_1\"]"
				,"[http://spa.org/TestProject/SampleDataPool#Process_1, http://dkm.fbk.eu/index.php/BPMN2_Ontology#isExecutable, \"false\"^^http://www.w3.org/2001/XMLSchema#boolean]"
				,"[http://spa.org/TestProject/SampleDataPool#Process_1, http://dkm.fbk.eu/index.php/BPMN2_Ontology#isClosed, \"false\"^^http://www.w3.org/2001/XMLSchema#boolean]"
				,"[http://spa.org/TestProject/SampleDataPool#Process_1, http://dkm.fbk.eu/index.php/BPMN2_Ontology#has_startEvent, http://spa.org/TestProject/SampleDataPool#StartEvent_1]"
				,"[http://spa.org/TestProject/SampleDataPool#Process_1, http://dkm.fbk.eu/index.php/BPMN2_Ontology#processType, \"None\"]");
		m.listStatements().toList().stream().map(Object::toString).forEach(statement -> assertThat(expectedStringStatements.contains(statement.toString()), is(true)));
	}
	
	@Test
	public void xesImportIntegrationTest() throws IOException {
		LOGGER.info("XES import test.");
		
		// Create owl file from xsd
		final XSDToOntModelImporter schemaimporter = new XSDToOntModelImporter();
		final OntModel schemaModel = schemaimporter.importData(new XMLFileImpl(getFilePath("xes.xsd").toString()));
	
		// Add owl file as new schema
		final Schema schema = persistentService.addDataAsNewSchema("XES2.0", schemaModel);
		
		// Create new project and link schema
		final Project project = persistentService.createPersistentProjectWithGeneratedId("Test Project");
		project.linkSchema(schema);
		persistentService.saveProject(project);
		
		// Create data pool in project
		final DataPool dataPool = persistentService.createPeristentDataPoolForProjectWithGeneratedId(project, "Sample Data Pool");

		// Import xes data as xml
		final XMLToOntModelImporter instanceImporter = new XMLToOntModelImporter(schemaModel);
		final Model instanceModel = instanceImporter.importData(new XMLFileImpl(getFilePath("running-example.xes").toString()));
		
		// Add data as new data bucket
		final DataBucket bucket = persistentService.addDataAsNewDataBucketToDataPool(dataPool, "Running example", instanceModel);
		final Optional<Model> foundDataForBucket = persistentService.findDataOfDataBucket(bucket);
		
		// assert that the statements are loaded
		assertThat(foundDataForBucket.isPresent(), is(true));
	}
	
	@Test
	public void xesExportIntegrationTest() throws IOException {
		final Path exportPath = getFilePath("xes.owl");
		final OntModel schemaModel = ModelFactory.createOntologyModel();
		schemaModel.read("xes.owl");
		
		final OntModel instanceModel = ModelFactory.createOntologyModel();
		instanceModel.read("running-example.owl");
		
		final OntModel combinedSchemaAndInstanceModel = ModelFactory.createOntologyModel(new OntModelSpec(OntModelSpec.OWL_MEM));
		combinedSchemaAndInstanceModel.add(schemaModel);
		combinedSchemaAndInstanceModel.add(instanceModel);
	    
		final OntModelToXESExporter xesExporter = new OntModelToXESExporter();
		final Set<XLog> logs = xesExporter.exportModel(combinedSchemaAndInstanceModel);
		
		int i=0;
		for(XLog log:logs) {
			final String filePath = exportPath.getParent() + "/running-example-exported-" + i++ + ".xes";
			final OutputStream outputStream = new FileOutputStream(new File(filePath));
			final XesXmlSerializer serializer = new XesXmlSerializer();
			serializer.serialize(log, outputStream);
			outputStream.flush();
			outputStream.close();
		}
	
		assertThat(new File(exportPath.getParent() + "/running-example-exported-0.xes").exists(), is(true));
	}

	private Model createProcessData() throws IOException {
		return loadFileAsModel("scanMailProcessModel.owl");
	}
	
	private Model loadBpmn2Schema() throws IOException {
		return loadFileAsModel("BPMN_2.0_ontology.owl");
	}

	private Model loadFileAsModel(final String fileName) throws IOException {
		final Path path = getFilePath(fileName);
		final InputStream inputStreamForFile = Files.newInputStream(path, StandardOpenOption.READ);
		return ModelFactory.createDefaultModel().read(inputStreamForFile, null);
	}

	private Path getFilePath(final String fileName) {
		try {
			final File file = new File(this.getClass().getResource("/"+fileName).toURI());
			return Paths.get(file.getAbsolutePath());
		} catch (NullPointerException | URISyntaxException exception) {
			LOGGER.error("Unable to retrieve path for file \"" + fileName + "\"");
			throw new RuntimeException(exception);
		}
	}
}
