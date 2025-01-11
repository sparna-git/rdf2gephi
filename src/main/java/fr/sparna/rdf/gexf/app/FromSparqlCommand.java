package fr.sparna.rdf.gexf.app;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import org.eclipse.rdf4j.repository.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.sparna.rdf.gexf.converter.RdfToGexfParserImpl;
import fr.sparna.rdf.rdf4j.toolkit.repository.RepositoryBuilder;
import fr.sparna.rdf.rdf4j.toolkit.repository.RepositoryBuilderFactory;
import it.uniroma1.dis.wsngroup.gexf4j.core.Gexf;

public class FromSparqlCommand implements CommandIfc {

	private static Logger log = LoggerFactory.getLogger(FromSparqlCommand.class.getName());

	public void execute(Object o) throws FileNotFoundException, IOException {

		//chargement du fichier rdf et stockage dans le repository
		FromSparqlArguments args=(FromSparqlArguments)o;
		RepositoryBuilder builder = RepositoryBuilderFactory.fromString(args.getInput());
		Repository repository = builder.get();

		
		log.info("Loaded a repository with "+repository.getConnection().size()+" triples");

		RdfToGexfParserImpl parser = new RdfToGexfParserImpl();
		Gexf gexf = parser.buildGexf(
			repository,
			new String(Files.readAllBytes(args.getEdgesQuery().toPath())),
			new String(Files.readAllBytes(args.getLabelsQuery().toPath())),
			new String(Files.readAllBytes(args.getAttributeQuery().toPath()))
		);

		File f = new File(args.getOutput());
		RdfToGexfParserImpl.writeGexf(gexf, f);

	}
}
