package fr.sparna.rdf.gexf;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

public class RepositoryParser {

	protected Repository repo;
	
	public void init() {
		this.repo = new SailRepository(new MemoryStore());
		this.repo.initialize();
	}

	public void storeRepository(InputStream input, RDFFormat format) throws IOException{		
		// load some data in the repository
		try(RepositoryConnection c = this.repo.getConnection();) {
			c.add(
					input, 
					RDF.NAMESPACE,
					format
					);
		}					
	}
	
	public Repository getRepository() {
		return this.repo;
	}
}
