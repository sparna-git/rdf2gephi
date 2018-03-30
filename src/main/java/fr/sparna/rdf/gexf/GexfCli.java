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
				names = { "-w", "--weight" },
				description = "Chemin vers le fichier properties définissant le poids des liens",
				required = false
		)
		private String weight;
		

		public String getInput() {
			return this.input;
		}
		
		public String getWeight() {
			return this.weight;
		}
		
		public String getOutput() {
			return this.output;
		}
}
