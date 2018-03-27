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
				description = "Chemin vers le dossier où sera enregistré la sortie",
				required = true
		)
		private String output;
	

		public String getInput() {
			return this.input;
		}
		
		public String getOutput() {
			return this.output;
		}
}
