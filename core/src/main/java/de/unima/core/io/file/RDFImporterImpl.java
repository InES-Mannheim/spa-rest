package de.unima.core.io.file;

import java.io.File;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.ModelFactory;

public class RDFImporterImpl implements FileBasedImporter<OntModel> {

	@Override
	public OntModel importData(File rdfSource) {
		OntModel m = ModelFactory.createOntologyModel(new OntModelSpec(OntModelSpec.OWL_MEM));
	    m.read(rdfSource.getPath());
	    return m;
	}

	@Override
	public String getID() {
		// TODO Auto-generated method stub
		return null;
	}

}
