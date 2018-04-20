package fr.sparna.rdf.gexf;

import com.beust.jcommander.Parameter;

public class GexfCli {
	
		@Parameter(
				names = { "-i", "--input" },
				description = "Chemin du fichier RDF d'entrée",
				required = true,
				variableArity = true
		)
		private String input;
		
		@Parameter(
				names = { "-o", "--output" },
				description = "Chemin vers le fichier où sera enregistré la sortie",
				required = true
		)
		private String output;
	
		@Parameter(
				names = { "-c", "--config" },
				description = "Chemin vers le fichier config.properties définissant les propriétés à prendre en compte",
				required = false
		)
		private String config;
		
		@Parameter(
				names = { "-start", "--dateEntryInForce" },
				description = "Date de mise en vigueur",
				required = false
		)
		private boolean dateEntryInForce;
		
		@Parameter(
				names = { "-end", "--dateNoLongerInForce" },
				description = "Date de fin",
				required = false
		)
		private boolean dateNoLongerInForce;
		

		public boolean isDateNoLongerInForce() {
			return this.dateNoLongerInForce;
		}
		
		public boolean isDateEntryInForce() {
			return this.dateEntryInForce;
		}
		
		public String getInput() {
			return this.input;
		}
		
		public String getConfig() {
			return this.config;
		}
		
		public String getOutput() {
			return this.output;
		}
}
