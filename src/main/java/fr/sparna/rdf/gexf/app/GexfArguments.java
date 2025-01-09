package fr.sparna.rdf.gexf.app;

import com.beust.jcommander.Parameter;

public class GexfArguments {
	
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
				names = { "-w", "--weight" },
				description = "Path to a properties file associating properties to weights",
				required = false
		)
		private String config;
		
		@Parameter(
				names = { "-s", "--startDateProperty" },
				description = "URI of the property holding the start date of entities",
				required = false
		)
		private String startDateProperty;
		
		@Parameter(
				names = { "-e", "--endDateProperty" },
				description = "URI of the property in the knowledge grapg holding the end date of entities",
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
