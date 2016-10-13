package br.ufc.mdcc.distributedmutualexclusion;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import br.ufc.mdcc.distributedmutualexclusion.entities.Process;

/**
 * @author guilhermeestevao
 * Ricart Agrawala algorithm for distributed mutual exclusion using JGroup
 */

public class App{


	private static Options options = new Options();

	public static void main( String[] args ) throws ParseException{

		setupOptions();

		CommandLineParser parser = new DefaultParser();
		CommandLine cmdLine = parser.parse(options, args);

		String processName = null;
		String channelName = null;
		int time = 25;
		
		if(cmdLine.hasOption('p')){
			processName = cmdLine.getOptionValue('p');
		}
		if(cmdLine.hasOption('c')){
			channelName = cmdLine.getOptionValue('c');
		}else{
			channelName = "default";
		}
		if(cmdLine.hasOption('t')){
			time = Integer.parseInt(cmdLine.getOptionValue('t'));
		}
		
		System.out.println("Criado processo "+processName+" no cluster "+channelName);
		
		try {
		
			Process p = new Process(processName, channelName, time);
			p.start();

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private static void setupOptions(){
		Option process  = Option.builder("p")                 
				.required(false)                 
				.desc("Identificador do processo (String)")                 
				.longOpt("identificador")                 
				.hasArg()                 
				.build();

		Option channel  = Option.builder("c")                 
				.required(false)                 
				.desc("Cluster a qual o processo pertence")                 
				.longOpt("canal")                 
				.hasArg()                 
				.build();
		
		Option time  = Option.builder("t")                 
				.required(false)                 
				.desc("Tempo que o processo tem para acessar a região crítica")                 
				.longOpt("tempo")                 
				.hasArg()                 
				.build();

		options.addOption(process);
		options.addOption(channel);
		options.addOption(time);


	}
}