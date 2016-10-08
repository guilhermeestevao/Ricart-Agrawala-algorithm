package br.ufc.mdcc.distributedmutualexclusion.entities;

import br.ufc.mdcc.distributedmutualexclusion.listeners.AccessCriticalRegionFinishedListener;

public class CriticalRegionSimulator implements Runnable{

	private AccessCriticalRegionFinishedListener listener;
	
	public CriticalRegionSimulator(AccessCriticalRegionFinishedListener listener) {
		// TODO Auto-generated constructor stub
		this.listener = listener;
	}
	
	
	public void run() {
		
		try {
			Thread.sleep(25000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally {
			listener.alertFinish();
		}
		
	}

}
