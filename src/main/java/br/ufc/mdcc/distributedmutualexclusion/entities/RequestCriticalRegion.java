package br.ufc.mdcc.distributedmutualexclusion.entities;

import java.io.Serializable;

import br.ufc.mdcc.distributedmutualexclusion.enums.TypeMessage;

public class RequestCriticalRegion implements Serializable{

	private static final long serialVersionUID = 1L;
	private String processName;
	private int time;
	private TypeMessage type;
	
	public RequestCriticalRegion(String processName, int time, TypeMessage type) {
		this.processName = processName;
		this.time = time;
		this.type = type;
	}
	
	public String getProcessName() {
		return processName;
	}
	public void setProcessName(String processName) {
		this.processName = processName;
	}
	public int getTime() {
		return time;
	}
	public void setTime(int time) {
		this.time = time;
	}
	public TypeMessage getType() {
		return type;
	}
	public void setType(TypeMessage type) {
		this.type = type;
	}
	
}
