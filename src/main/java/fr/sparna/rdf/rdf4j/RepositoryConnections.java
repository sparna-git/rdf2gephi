package fr.sparna.rdf.rdf4j;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.List;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResultHandler;
import org.eclipse.rdf4j.query.TupleQueryResultHandlerException;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFWriterFactory;
import org.eclipse.rdf4j.rio.RDFWriterRegistry;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.BufferedGroupingRDFHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.sparna.rdf.gexf.converter.RdfToGexfParserImpl;

public class RepositoryConnections {
    
    private static Logger log = LoggerFactory.getLogger(RdfToGexfParserImpl.class.getName());



    public static void select(RepositoryConnection connection, String query, TupleQueryResultHandler handler) 
	throws TupleQueryResultHandlerException, QueryEvaluationException, RepositoryException {			
		log.trace("Executing SPARQL SELECT :\n"+query);
		TupleQuery tupleQuery = connection.prepareTupleQuery(QueryLanguage.SPARQL, query);
		// sets bindings, inferred statement flags and datasets

		// on execute la query
		tupleQuery.evaluate(handler);
	}

	public static boolean ping(RepositoryConnection connection) {
		PingSparqlHelper ping = new PingSparqlHelper();
		log.trace("Pinging...");
		RepositoryConnections.select(connection, PingSparqlHelper.PING_QUERY, ping);
		log.trace("Ping !");
		return ping.isPinged(); 
	}

	/**
	 * Write the repository to the file with the given path. The format used for serializing is deduced from
	 * the file extension. The file is created if it does not exists.
	 * 
	 * @param fout	The file to write to
	 */
	public static void writeToFile(RepositoryConnection connection, File fout, List<IRI> namedGraphsToDump)
	throws RepositoryException, MalformedQueryException, IOException, QueryEvaluationException, RDFHandlerException {
		log.debug("Will dump repository into file "+fout+"...");

		if (!fout.exists()) {
			// create parent dir if needed
			File parentDir = fout.getParentFile();
			if(parentDir != null && !parentDir.exists()) {
				parentDir.mkdirs();
			}
			// create output file
			fout.createNewFile();
		}
		
		RepositoryConnections.writeToStream(
				connection,
				new FileOutputStream(fout),
				Rio.getParserFormatForFileName(fout.getName()).orElse(RDFFormat.RDFXML),
				false,
				namedGraphsToDump
		);
		
		log.debug("Done dumping.");
	}

		
	/**
	 * Writes the Repository into an OutputStream using the given RDFFormat.
	 * Handles sorting, namespaces, and SPARQL query execution.
	 * 
	 * @param stream	Stream to write to
	 * @param format	Format to use for serialisation
	 */
	public static void writeToStream(
		RepositoryConnection connection,
		OutputStream stream,
		RDFFormat format,
		boolean sorting,
		List<IRI> namedGraphsToDump
	) 
	throws RepositoryException, MalformedQueryException, IOException, QueryEvaluationException, RDFHandlerException {
		
		// use pretty print RDF/XML handler
		try {
			RDFWriterRegistry.getInstance().add((RDFWriterFactory)RepositoryConnections.class.getClassLoader().loadClass("org.eclipse.rdf4j.rio.rdfxml.util.RDFXMLPrettyWriterFactory").newInstance());
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		// picks up the correct RDF format based on target file extension (.rdf, .n3, .ttl, etc...)
		RDFHandler writer = RDFWriterRegistry.getInstance().get(format).get().getWriter(new OutputStreamWriter(stream));
		
		if(sorting) {
			writer = new BufferedGroupingRDFHandler(100000, writer);
		}

		// on dump tout le repository si aucune query n'est precisee
		if(namedGraphsToDump == null) {
			connection.exportStatements(
					// subject - predicate - object
					null,
					null,
					null,
					// includeInferredStatements
					true,
					// writer
					writer
			);
		} else {
			connection.exportStatements(
					// subject - predicate - object
					null,
					null,
					null,
					// includeInferredStatements
					true,
					// writer
					writer,
					namedGraphsToDump.toArray(new IRI[]{})
			);
		}

	}

}
