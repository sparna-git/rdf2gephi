package fr.sparna.rdf.gexf.app;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.MissingCommandException;
import com.beust.jcommander.ParameterException;

public class Main {

	enum COMMAND {		

		DIRECT(new DirectGexfArguments(), new DirectGexfCommand()),
		SPARQL(new FromSparqlArguments(), new FromSparqlCommand());

		private CommandIfc command;
		private Object arguments;

		private COMMAND(Object arguments, CommandIfc command) {
			this.command = command;
			this.arguments = arguments;
		}

		public CommandIfc getCommand() {
			return command;
		}

		public Object getArguments() {
			return arguments;
		}		
	}
	public static void main(String[] args) throws Exception {
		Main main = new Main();
		main.run(args);
	}

	public void run(String[] args) throws Exception {
		ArgumentsMain main = new ArgumentsMain();
		JCommander jc = new JCommander(main);
		for (COMMAND aCOMMAND : COMMAND.values()) {
			jc.addCommand(aCOMMAND.name().toLowerCase(), aCOMMAND.getArguments());
		}

		try {
			jc.parse(args);
			// a mettre avant ParameterException car c'est une sous-exception
		} catch (MissingCommandException e) {
			// if no command was found, exit with usage message and error code
			System.err.println("Commande inconnue.");
			jc.usage();
			System.exit(-1);
		} catch (ParameterException e) {
			System.err.println(e.getMessage());
			jc.usage();
			System.exit(-1);
		} 

		// if help was requested, print it and exit with a normal code
		if(main.isHelp()) {
			jc.usage();
			System.exit(0);
		}

		// if no command was found (0 parameters passed in command line)
		// exit with usage message and error code
		if(jc.getParsedCommand() == null) {
			System.err.println("Pas de commande trouvée.");
			jc.usage();
			System.exit(-1);
		}
		// executes the command with the associated arguments
		COMMAND.valueOf(jc.getParsedCommand().toUpperCase()).getCommand().execute(
			COMMAND.valueOf(jc.getParsedCommand().toUpperCase()).getArguments()
		);

	}

}
