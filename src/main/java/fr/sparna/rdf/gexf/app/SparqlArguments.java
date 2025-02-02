package fr.sparna.rdf.gexf.app;

import java.io.File;

import com.beust.jcommander.Parameter;

@com.beust.jcommander.Parameters(
	commandDescription = "Converts RDF data to GEXF format using SPARQL queries."
)
public class SparqlArguments {
	
		@Parameter(
				names = { "-i", "--input" },
				description = "Path to RDF input file(s), or directory containing RDF files, or URL of a SPARQL endpoint.",
				required = true,
				variableArity = true
		)
		private String input;
		
		@Parameter(
				names = { "-o", "--output" },
				description = "Path to GEXF output file",
				required = true
		)
		private String output;
		
		@Parameter(
				names = { "-e", "--edges" },
				description = "Path to the file containing the SPARQL query to retrieve edges, e.g. 'sparql/edges.rq'. The query MUST return the following variables: ?subject, ?edge, ?object",
				required = false
		)
		private File edgesQuery;

		@Parameter(
				names = { "-l", "--labels" },
				description = "Path to the file containing the SPARQL query to retrieve labels, e.g. 'sparql/labels.rq'. The query MUST return the following variables: ?subject, ?label",
				required = false
		)
		private File labelsQuery;

		@Parameter(
				names = { "-a", "--attributes" },
				description = "Path to the file containing the SPARQL query to retrieve attributes, e.g. 'sparql/attribute.rq'. The query MUST return 3 columns: the first one is the subject, the second one is the attribute URI, the third one is the attribute value (a literal or a URI).",
				required = false
		)
		private File attributesQuery;
		
		@Parameter(
				names = { "-d", "--dates" },
				description = "Path to the file containing the SPARQL query to retrieve date ranges, e.g. 'sparql/dates.rq'",
				required = false
		)
		private File datesQuery;

		@Parameter(
				names = { "-p", "--parents" },
				description = "Deprecated. Path to the file containing the SPARQL query to retrieve parents relationship, e.g. 'sparql/parents.rq'",
				required = false
		)
		private File parentsQuery;

		public String getInput() {
			return this.input;
		}
		
		public String getOutput() {
			return this.output;
		}

		public File getEdgesQuery() {
			return this.edgesQuery;
		}

		public File getLabelsQuery() {
			return this.labelsQuery;
		}

		public File getAttributesQuery() {
			return attributesQuery;
		}

		public File getDatesQuery() {
			return datesQuery;
		}

		public File getParentsQuery() {
			return parentsQuery;
		}

}
