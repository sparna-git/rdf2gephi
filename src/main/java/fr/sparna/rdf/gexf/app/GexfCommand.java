package fr.sparna.rdf.gexf.app;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;

import fr.sparna.rdf.gexf.converter.RdfToGexfParserImpl;
import fr.sparna.rdf.utils.RepositorySupplier;
import it.uniroma1.dis.wsngroup.gexf4j.core.Gexf;

public class GexfCommand {

	public void execute(Object o) throws FileNotFoundException, IOException {


		//chargement du fichier rdf et stockage dans le repository
		GexfArguments args=(GexfArguments)o;
		RepositorySupplier repositorySupplier=new RepositorySupplier(
				new FileInputStream(args.getInput()),
				Rio.getParserFormatForFileName(args.getInput())
				.orElse(RDFFormat.RDFXML)
				);

		Repository repository = repositorySupplier.getRepository();

		//Chargement du fichier de config
		Properties weights = null;
		if(args.getConfig()!=null){
			weights = new java.util.Properties();
			weights.load(new FileInputStream(new File(args.getConfig())));
		}

		RdfToGexfParserImpl parser = new RdfToGexfParserImpl(args.getStartDateProperty(), args.getEndDateProperty(), weights);
		Gexf gexf = parser.rdfToGexf(repository);

		File f = new File(args.getOutput());
		RdfToGexfParserImpl.writeGexf(gexf, f);

	}
}
