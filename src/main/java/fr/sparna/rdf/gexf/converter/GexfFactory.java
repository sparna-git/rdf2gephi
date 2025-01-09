package fr.sparna.rdf.gexf.converter;

import java.util.Calendar;

import it.uniroma1.dis.wsngroup.gexf4j.core.Gexf;
import it.uniroma1.dis.wsngroup.gexf4j.core.impl.GexfImpl;

public class GexfFactory {

	/**
	 * @return a new Gexf instance
	 */
	public static Gexf newGexf() {

		// création du gexf
		Gexf gexf = new GexfImpl();
		Calendar date = Calendar.getInstance();
		gexf.getMetadata()
		.setLastModified(date.getTime())
		.setCreator("SPARNA")
		.setDescription("rdf to gexf");
		gexf.setVisualization(true);

		return gexf;
	}

}
