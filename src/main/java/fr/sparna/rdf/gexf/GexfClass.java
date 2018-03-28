package fr.sparna.rdf.gexf;

import java.util.Calendar;

import it.uniroma1.dis.wsngroup.gexf4j.core.Gexf;
import it.uniroma1.dis.wsngroup.gexf4j.core.impl.GexfImpl;

public class GexfClass{

	public static Gexf getGexf(){

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
