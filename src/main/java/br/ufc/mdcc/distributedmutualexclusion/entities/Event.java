package br.ufc.mdcc.distributedmutualexclusion.entities;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Map.Entry;

import org.jgroups.Address;

public class Event {
	
	private BufferedReader in;
	private Process process;
	
	private final String[] OPTIONS = {
			"(1) - Solicitar acesso ao recurso",
			"(2) - Exibir fila de requisições",
			"(3) - Ver o estado atual do processo",
			"(4) - Sair do cluster"
	};
	
	private final int REQUEST_IN_CR = 1;
	private final int REQUEST_QUEUE_RESQUESTS = 2;
	private final int SEE_STATE = 3;
	private final int OUT_CLUSTER = 4;
	
	public Event(Process process){
		this.process = process;
		in = new BufferedReader(new InputStreamReader(System.in));
	}
	
	private int getOption() throws IOException{
		
		System.out.println("-------------------");
		System.out.println(">>> "+process.getName());
		System.out.println("-------------------");
		
		for (String string : OPTIONS) {
			System.out.println(string);
		}
		
		String s = in.readLine();
		return Integer.parseInt(s);
	}
	
	private void showRequestsQueue(){
		Map<String, Address> queue =  process.getRequestsQueue();
		
		if(queue.isEmpty())
			System.out.println("[LISTA VAZIA]");
		
		for(Entry<String, Address> pair : queue.entrySet()){
			
			System.out.println("[ "+pair.getKey()+" ] "+pair.getValue());
			
		}
	}
	
	private void processOption(int opt) throws Exception{
		switch (opt) {
		case REQUEST_IN_CR:
			process.sendMulticastRequest();
			break;
		
		case REQUEST_QUEUE_RESQUESTS:
			showRequestsQueue();
			break;
		
		case SEE_STATE:
			System.out.println(process.getState().name());
			break;
			
		case OUT_CLUSTER:
			System.out.println("Processo encerrado");
			break;
			
		default:
			System.out.println("Opção invalida");
			break;
		}
	}
	
	public void init() throws Exception{
		int opt = 0;
		
		while(opt != OUT_CLUSTER){
			opt = getOption();
			processOption(opt);
			
		}
		
		process.stop();
	}
	
}
