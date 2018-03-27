package fr.sparna.rdf.gexf;

import java.io.FileNotFoundException;
import java.io.IOException;

public interface RdfToGexfIfc {
	
	/**
	 * Convert a rdf file to gexf file
	 * @param args
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public void rdfToGexf(Object args) throws FileNotFoundException, IOException ;

}
