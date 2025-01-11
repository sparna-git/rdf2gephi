package fr.sparna.rdf.gexf.app;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;
import org.eclipse.rdf4j.repository.Repository;

import fr.sparna.rdf.gexf.converter.RdfToGexfParserImpl;
import fr.sparna.rdf.rdf4j.toolkit.repository.RepositoryBuilder;
import fr.sparna.rdf.rdf4j.toolkit.repository.RepositoryBuilderFactory;
import it.uniroma1.dis.wsngroup.gexf4j.core.Gexf;

public class GexfCommand implements CommandIfc {

	public void execute(Object o) throws Exception {

		//chargement du fichier rdf et stockage dans le repository
		GexfArguments args=(GexfArguments)o;
		RepositoryBuilder builder = RepositoryBuilderFactory.fromString(args.getInput());
		Repository repository = builder.get();

		//Chargement du fichier de config
		Properties weights = null;
		if(args.getConfig()!=null){
			weights = new java.util.Properties();
			weights.load(new FileInputStream(new File(args.getConfig())));
		}

		RdfToGexfParserImpl parser = new RdfToGexfParserImpl();
		Gexf gexf = parser.rdfToGexf(
			repository,
			args.getStartDateProperty(),
			args.getEndDateProperty(),
			weights
		);

		File f = new File(args.getOutput());
		RdfToGexfParserImpl.writeGexf(gexf, f);

	}
}
