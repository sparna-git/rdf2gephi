package fr.sparna.rdf.gexf.converter;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.eclipse.rdf4j.repository.Repository;

import it.uniroma1.dis.wsngroup.gexf4j.core.Gexf;

public interface RdfToGexfParserIfc {
	
	/**
	 * Convert a rdf file to gexf file
	 * @param args
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public Gexf rdfToGexf(Repository repository) throws FileNotFoundException, IOException ;

}
