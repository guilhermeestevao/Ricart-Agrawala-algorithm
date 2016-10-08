package br.ufc.mdcc.distributedmutualexclusion.entities;

import java.util.ArrayList;
import java.util.List;
import org.jgroups.Address;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;
import br.ufc.mdcc.distributedmutualexclusion.enums.State;
import br.ufc.mdcc.distributedmutualexclusion.enums.TypeMessage;
import br.ufc.mdcc.distributedmutualexclusion.listeners.AccessCriticalRegionFinishedListener;

import static br.ufc.mdcc.distributedmutualexclusion.enums.State.*;
import static br.ufc.mdcc.distributedmutualexclusion.enums.TypeMessage.*;

public class Process extends ReceiverAdapter implements AccessCriticalRegionFinishedListener{

	private String name;
	private JChannel channel;
	private State state;
	private int lamport;
	private List<Address> requestsQueue;
	private List<Address> othersMenbers;
	
	public Process(String name, String channel) throws Exception{
		this.name = name;
		this.channel = new JChannel();
		this.channel.connect(channel);
		this.channel.setReceiver(this);
		this.state = RELEASED;
		lamport = 0;
		requestsQueue = new ArrayList<Address>();
		othersMenbers = new ArrayList<Address>();
	}
	
	public void start() throws Exception{
		updateLamport();
		eventLoop();
	}
	
	public void stop(){
		channel.close();
	}
	

	private void eventLoop() throws Exception{
		Event event = new Event(this);
		event.init();
	}

	public void viewAccepted(View view) {
		updateLamport();
		othersMenbers.clear();
		othersMenbers = view.getMembers();
	}

	public void receive(Message msg) {
		
		RequestCriticalRegion request = (RequestCriticalRegion) msg.getObject();
		
		updateLamport(request.getTime());
		
		if(!request.getProcessName().equals(name)){			
//			System.out.println("Name: "+request.getProcessName());
//			System.out.println("Time: "+request.getTime());
//			System.out.println("Message: "+request.getType().name());
			
			Address src = msg.src();
			
			try {
				processRequest(src, request);
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		
	}
	
	
	public void sendMulticastRequest() throws Exception{
		updateLamport();
		if(state == WANTED){
			System.out.println("Já foi realizado uma requisição anteriormente!");
			//lançar uma exceção seria melhor aqui
			return;
		}
		
		RequestCriticalRegion request = new RequestCriticalRegion(name, lamport, REQUEST_IN_CR);
		Message msg = new Message(null, null, request);
		channel.send(msg);
		state = WANTED;
	}
	
	//Utilizado na sincronização entre dois processos
	private void updateLamport(int time){
		int newLamport = time > this.lamport ? time : lamport;
		lamport = newLamport+1;
	}
	
	//Utilizado quando executa um evento interno
	private void updateLamport(){
		lamport+=1;
	}
	
	private void processRequest(Address src, RequestCriticalRegion request) throws Exception{
		
		TypeMessage type = request.getType();
		
		switch (type) {
		case REQUEST_IN_CR:
			verifyRequest(src, request);
			break;
			
		case OK:
			tryAccessCriticalRegion(src);
			break;
			
		default:
			break;
		}
		
	}
	
	private void verifyRequest(Address src, RequestCriticalRegion request) throws Exception{
		
		switch (state) {
		case HELD:
			//Esse processo está na região critica, logo a requisição será enfieirada
			enqueueRequest(src);
			break;

		case WANTED:
			//Esse processo quer entrar na região critica, logo os relógios de lamport serão comparados	
			compareLamportTime(src, request);
			break;
			
		case RELEASED:
			//Esse processo não está na região critica e nem deseja entrar
			sendMessageOk(src);
			break;
		default:
			break;
		}
		
	}
	
	private void enqueueRequest(Address src){
		updateLamport();
		requestsQueue.add(src);
	}

	private void sendMessageOk(Address dest) throws Exception{
		updateLamport();
		//Enviando mensagem para um destinatario
		RequestCriticalRegion ok = new RequestCriticalRegion(name, lamport, OK);
		Message msg = new Message(dest, null, ok);
		channel.send(msg);
	}
	
	private void compareLamportTime(Address src, RequestCriticalRegion request) throws Exception{
		if (request.getTime() > lamport){
			//enfileira esse processo
			enqueueRequest(src);
		}else{
			sendMessageOk(src);
		}
	}
	
	private void tryAccessCriticalRegion(Address src){
		updateLamport();
		requestsQueue.remove(src);
		
		if(requestsQueue.isEmpty()){
			state = HELD;
			accessCriticalRegion();
		}
		
	}
	
	private void accessCriticalRegion(){
		Thread tread = new Thread(new CriticalRegionSimulator(this));
		System.out.println("Acessando região critica!");
		tread.start();
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public JChannel getChannel() {
		return channel;
	}

	public void setChannel(JChannel channel) {
		this.channel = channel;
	}

	public State getState() {
		return state;
	}

	public void setState(State state) {
		this.state = state;
	}

	public int getLamport() {
		return lamport;
	}

	public void setLamport(int lamport) {
		this.lamport = lamport;
	}

	public List<Address> getRequestsQueue() {
		return requestsQueue;
	}

	public void setRequestsQueue(List<Address> requestsQueue) {
		this.requestsQueue = requestsQueue;
	}

	public List<Address> getOthersMenbers() {
		return othersMenbers;
	}

	public void setOthersMenbers(List<Address> othersMenbers) {
		this.othersMenbers = othersMenbers;
	}

	public void alertFinish() {
		//Liberado a região critica
		
		state = RELEASED;
			
		for (Address address : requestsQueue) {
			try {
				sendMessageOk(address);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				continue;
			}
		}
	}
	
}
