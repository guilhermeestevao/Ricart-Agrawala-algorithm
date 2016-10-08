package br.ufc.mdcc.distributedmutualexclusion;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;

import br.ufc.mdcc.distributedmutualexclusion.entities.Process;


/**
 * Hello world!
 *
 */
public class App extends ReceiverAdapter{
	
    public static void main( String[] args ){
    	
    	BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
    	
    	try {
			String name = in.readLine();
			//String channel = in.readLine();
			
			Process p = new Process(name, "channel");
			p.start();
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    }
}