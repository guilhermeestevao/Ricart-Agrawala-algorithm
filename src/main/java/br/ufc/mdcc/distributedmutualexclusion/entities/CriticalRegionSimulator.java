package br.ufc.mdcc.distributedmutualexclusion.entities;

import br.ufc.mdcc.distributedmutualexclusion.listeners.AccessCriticalRegionFinishedListener;

public class CriticalRegionSimulator implements Runnable{

	private AccessCriticalRegionFinishedListener listener;
	private int time = 25;
	
	
	public CriticalRegionSimulator(AccessCriticalRegionFinishedListener listener, int time) {
		// TODO Auto-generated constructor stub
		this.listener = listener;
		this.time = time;
	}
	
	public void run() {
		
		try {
			int t = time; 
			while(t > 0){
				System.out.println("Saindo da região critica em: "+t);
				Thread.sleep(1000);
				t--;
			}	
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally {
			listener.alertFinish();
		}
		
	}

}
