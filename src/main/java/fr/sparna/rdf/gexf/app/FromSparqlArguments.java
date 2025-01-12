package fr.sparna.rdf.gexf.app;

import java.io.File;

import com.beust.jcommander.Parameter;

public class FromSparqlArguments {
	
		@Parameter(
				names = { "-i", "--input" },
				description = "Path to RDF input file. Can be repeated to read multiple files",
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
				description = "Path to the file containing the SPARQL query to retrieve edges",
				required = true
		)
		private File edgesQuery;

		@Parameter(
				names = { "-l", "--labels" },
				description = "Path to the file containing the SPARQL query to retrieve labels",
				required = false
		)
		private File labelsQuery;

		@Parameter(
				names = { "-a", "--attributes" },
				description = "Path to the file containing the SPARQL query to retrieve attributes",
				required = false
		)
		private File attributesQuery;
		
		@Parameter(
				names = { "-d", "--dates" },
				description = "Path to the file containing the SPARQL query to retrieve date ranges",
				required = false
		)
		private File datesQuery;

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

		

}
