package fr.sparna.rdf.gexf.converter;

import java.util.Calendar;

import it.uniroma1.dis.wsngroup.gexf4j.core.Gexf;
import it.uniroma1.dis.wsngroup.gexf4j.core.impl.GexfImpl;

public class GexfFactory {

	/**
	 * @return a new Gexf instance
	 */
	public static Gexf newGexf(String creator, String description) {

		// création du gexf
		Gexf gexf = new GexfImpl();
		Calendar date = Calendar.getInstance();
		gexf.getMetadata()
		.setLastModified(date.getTime())
		.setCreator(creator)
		.setDescription(description);
		gexf.setVisualization(true);

		return gexf;
	}

}
