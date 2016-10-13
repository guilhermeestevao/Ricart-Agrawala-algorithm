package br.ufc.mdcc.distributedmutualexclusion.entities;

import static br.ufc.mdcc.distributedmutualexclusion.enums.State.HELD;
import static br.ufc.mdcc.distributedmutualexclusion.enums.State.RELEASED;
import static br.ufc.mdcc.distributedmutualexclusion.enums.State.WANTED;
import static br.ufc.mdcc.distributedmutualexclusion.enums.TypeMessage.OK;
import static br.ufc.mdcc.distributedmutualexclusion.enums.TypeMessage.REQUEST_IN_CR;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.jgroups.Address;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;

import br.ufc.mdcc.distributedmutualexclusion.enums.State;
import br.ufc.mdcc.distributedmutualexclusion.enums.TypeMessage;
import br.ufc.mdcc.distributedmutualexclusion.listeners.AccessCriticalRegionFinishedListener;

public class Process extends ReceiverAdapter implements AccessCriticalRegionFinishedListener{

	private String name;
	private JChannel channel;
	private State state;
	private int lamport;
	private int timeAtCR;
	private Map<String, Address> requestsQueue;
	private List<Address> allAtCluster;
	private Map<Address, Boolean> processAcceptedAcess;

	public Process(String name, String channel, int time) throws Exception{
		this.name = name;
		this.channel = new JChannel();
		this.channel.connect(channel);
		this.channel.setReceiver(this);
		this.state = RELEASED;
		this.timeAtCR = time;
		this.lamport = 0;
		this.requestsQueue = new HashMap<String, Address>();
		this.allAtCluster = this.channel.getView().getMembers();
		this.processAcceptedAcess = new HashMap<Address, Boolean>();
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
		List<Address> previousMenbenrs = allAtCluster;
		allAtCluster = view.getMembers();
		if(state == WANTED)
			processChangeAtCluster(previousMenbenrs);
	}

	private void processChangeAtCluster(List<Address> previousMenbenrs){

		if(allAtCluster.size() > previousMenbenrs.size()){
			//foi adicionado
			for(Address adr : allAtCluster){
				if(!previousMenbenrs.contains(adr)){
					//Aplica entrada
					try {
						noifyInCluster(adr);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}
			}

		}else{
			//foi removido

			for(Address adr : previousMenbenrs){
				if(!allAtCluster.contains(adr)){
					//aplica saída
					notifyOutCluster(adr);
				}
			}

		}
	}

	private void noifyInCluster(Address adr) throws Exception{
		processAcceptedAcess.put(adr, false);
		sendMessageResquest(adr);
	}

	private void notifyOutCluster(Address adr){
		processAcceptedAcess.remove(adr);
		tryAccessCriticalRegion(adr, null);
	}

	private void fillMapProcessAcceptedAcess(){
		for(Address adr : allAtCluster)
			processAcceptedAcess.put(adr, false);
	}

	public void receive(Message msg) {

		RequestCriticalRegion request = (RequestCriticalRegion) msg.getObject();

		updateLamport(request.getTime());

		Address src = msg.src();

		String nameOrigin = request.getProcessName();

		if(!nameOrigin.equals(name)){			
			try {
				processRequest(src, request);

			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}else{
			tryAccessCriticalRegion(src, nameOrigin);
		}

	}

	public void sendMulticastRequest() throws Exception{
		if(state == WANTED){
			System.out.println("Já foi realizado uma requisição anteriormente!");
		}else if(state == State.HELD){
			System.out.println("Já está na região crítica");
		}else{
			updateLamport();
			RequestCriticalRegion request = new RequestCriticalRegion(name, lamport, REQUEST_IN_CR);
			Message msg = new Message(null, null, request);
			fillMapProcessAcceptedAcess();
			state = WANTED;
			channel.send(msg);
		}
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
			tryAccessCriticalRegion(src, request.getProcessName());
			break;

		default:
			break;
		}

	}

	private void verifyRequest(Address src, RequestCriticalRegion request) throws Exception{

		switch (state) {
		case HELD:
			//Esse processo está na região critica, logo a requisição será enfieirada
			enqueueRequest(request.getProcessName(), src);
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

	private void enqueueRequest(String key, Address src){
		updateLamport();
		requestsQueue.put(key, src);
	}

	private void sendMessageOk(Address dest) throws Exception{
		updateLamport();
		RequestCriticalRegion ok = new RequestCriticalRegion(name, lamport, OK);
		Message msg = new Message(dest, null, ok);
		channel.send(msg);
	}

	private void sendMessageResquest(Address adr) throws Exception{
		updateLamport();
		RequestCriticalRegion request = new RequestCriticalRegion(name, lamport, REQUEST_IN_CR);
		Message msg = new Message(adr, null, request);
		channel.send(msg);
	}

	private void compareLamportTime(Address src, RequestCriticalRegion request) throws Exception{
		if (request.getTime() <= lamport){
			//enfileira esse processo
			enqueueRequest(request.getProcessName(), src);
		}else{
			sendMessageOk(src);
		}
	}

	private void tryAccessCriticalRegion(Address src, String process){
		updateLamport();

		if(process != null){
			System.out.println("Recebido OK de: "+process);
			processAcceptedAcess.put(src, true);
		}	
		else
			System.out.println("Um processo saiu do cluster insesperadamente");

		if(verifyPermissonToAcessCR()){
			state = HELD;
			accessCriticalRegion();
		}

	}

	private boolean verifyPermissonToAcessCR(){

		for (Entry<Address, Boolean> pair : processAcceptedAcess.entrySet()) 
			if(!pair.getValue())
				return false;

		return true;
	}

	private void accessCriticalRegion(){
		Thread tread = new Thread(new CriticalRegionSimulator(this, timeAtCR));
		System.out.println("Acessando região critica!");
		tread.start();
	}

	public void alertFinish() {
		//Liberado a região critica
		System.out.println("Saiu da região critíca! Liberando fila de requisições...");
		state = RELEASED;

		for (Entry<String, Address> pair : requestsQueue.entrySet()){
			try {
				sendMessageOk(pair.getValue());
			} catch (Exception e) {

				e.printStackTrace();
				continue;
			}
		}
		requestsQueue.clear();
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

	public Map<String, Address> getRequestsQueue() {
		return requestsQueue;
	}

	public void setRequestsQueue(Map<String, Address> requestsQueue) {
		this.requestsQueue = requestsQueue;
	}

	public List<Address> getOthersMenbers() {
		return allAtCluster;
	}

	public void setOthersMenbers(List<Address> othersMenbers) {
		this.allAtCluster = othersMenbers;
	}



}
