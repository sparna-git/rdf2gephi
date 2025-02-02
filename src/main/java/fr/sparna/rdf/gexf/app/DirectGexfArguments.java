package fr.sparna.rdf.gexf.app;

import com.beust.jcommander.Parameter;

@com.beust.jcommander.Parameters(
	commandDescription = "Converts RDF data to GEXF format directly. All literals are considered as attributes, and all triples as edges, except rdf:type. rdfsl:label is used as label."
)
public class DirectGexfArguments {
	
		@Parameter(
				names = { "-i", "--input" },
				description = "Path to RDF input file, or directory containing RDF files, or URL of a SPARQL endpoint.",
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
				names = { "-w", "--weight" },
				description = "Path to a properties file associating properties to weights",
				required = false
		)
		private String config;
		
		@Parameter(
				names = { "-s", "--startDateProperty" },
				description = "URI of the property in the knowledge graph holding the start date of entities",
				required = false
		)
		private String startDateProperty;
		
		@Parameter(
				names = { "-e", "--endDateProperty" },
				description = "URI of the property in the knowledge graph holding the end date of entities",
				required = false
		)
		private String endDateProperty;
		
		public String getInput() {
			return this.input;
		}
		
		public String getConfig() {
			return this.config;
		}
		
		public String getOutput() {
			return this.output;
		}

		public String getStartDateProperty() {
			return startDateProperty;
		}

		public String getEndDateProperty() {
			return endDateProperty;
		}

}
