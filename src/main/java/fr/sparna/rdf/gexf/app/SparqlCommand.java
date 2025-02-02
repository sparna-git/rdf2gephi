package fr.sparna.rdf.gexf.app;

import java.io.File;
import java.io.Reader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;

import org.apache.commons.io.IOUtils;
import org.eclipse.rdf4j.repository.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.sparna.rdf.gexf.converter.RdfToGexfParserImpl;
import fr.sparna.rdf.rdf4j.RepositoryBuilder;
import fr.sparna.rdf.rdf4j.RepositoryBuilderFactory;
import it.uniroma1.dis.wsngroup.gexf4j.core.Gexf;

public class SparqlCommand implements CommandIfc {

	private static Logger log = LoggerFactory.getLogger(SparqlCommand.class.getName());

	public void execute(Object o) throws FileNotFoundException, IOException {

		//chargement du fichier rdf et stockage dans le repository
		SparqlArguments args=(SparqlArguments)o;
		RepositoryBuilder builder = RepositoryBuilderFactory.fromString(args.getInput());
		Repository repository = builder.get();

		// don't do that, otherwise it issues a CONSTRUCT { ?s ?p o } query to the triplestore
		// log.info("Loaded a repository with "+repository.getConnection().size()+" triples");

		RdfToGexfParserImpl parser = new RdfToGexfParserImpl();

		// read queries and overwrite with defaults if necessary


		Gexf gexf = parser.buildGexf(
			repository,
			readFileParameterWithDefaultResource(args.getEdgesQuery(), "/default-edges.rq"),
			readFileParameterWithDefaultResource(args.getLabelsQuery(), "/default-labels.rq"),
			readFileParameterWithDefaultResource(args.getAttributesQuery(), "/default-attributes.rq"),
			args.getDatesQuery() != null ? Files.readString(args.getDatesQuery().toPath()) : null,
			args.getParentsQuery() != null ? Files.readString(args.getParentsQuery().toPath()) : null
		);

		File f = new File(args.getOutput());
		RdfToGexfParserImpl.writeGexf(gexf, f);

	}

	private String readFileParameterWithDefaultResource(File f, String resourcePath) throws IOException {
		if(f == null) {
			InputStream input = this.getClass().getResourceAsStream(resourcePath);
			if(input == null) {
				return null;
			}
			try(Reader reader = new InputStreamReader(input, "UTF-8")) {
				return IOUtils.toString(reader);
			}
		} else {
			return Files.readString(f.toPath());
		}
	}
}
