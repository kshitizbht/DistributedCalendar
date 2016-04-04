package com.rest.chord;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.http.client.ClientProtocolException;

import com.rest.calendar.Calendar;
import com.rest.calendar.Event;
import com.rest.classes.User;
import com.rest.classes.Utils;
import com.rest.resources.Main;

/**
 * Class that forms the ring, node balancing, node failure handling etc.
 * @author kshitiz
 *
 */
public class Node extends Thread{
	private String ip;
	private int nodeID;
	public ArrayList<String> myUsers;
	public ArrayList<String> backup;
	private String successorIP;
	private String predecessorIP;
	private String sosIP;
	private String popIP;
	public boolean shutdownNotRequested=false;
	public boolean nodeBalanceRequired = false;
	
	public Node(String ip){
		this.ip = ip;
		nodeID = getHash(ip);
		this.successorIP = ip;
		this.predecessorIP = ip;
		this.sosIP = null;
		this.popIP = null;
		this.myUsers = new ArrayList<String>();
		this.backup = new ArrayList<String>();
	}
	/**
	 * Thread to keep asking its neighbor about its state
	 */
	public void run(){
		while(shutdownNotRequested){
			try {
				sleep(3000);//sleep for a while then proceed
				if(!this.successorIP.equals(ip)){
					probe(); //fix_fingers & stabilize
				}
			} catch (Exception e1) {
				Main.log("thread - stopped due to ", e1.getMessage());
			}
		}
	}

	/**
	 * Node balance will send the right files to successor and receive files from predecessor
	 * Called during node join/rejoin
	 * @throws Exception
	 */
	public void nodeBalance() throws Exception {
		Main.log("nodeBalance begining", "usernames " + this.myUsers.toString());
		String url = "getHigherKeys?ip="+this.ip;  
		String urls = "getLowerKeys?ip="+this.ip;  
		if(nodeID > getHash(this.predecessorIP)){
			String response = WebCaller.callNodeForData(this.predecessorIP, url);
			if(response.equals(""))
				Main.log("node balance received empty list of names", response);
			else{
				String[] responsibleUsers = response.split(",");
				for(String s: responsibleUsers){
					if(!this.myUsers.contains(s)){
						this.myUsers.add(s);
						WebCaller.downloadFileFromNode(s,this.predecessorIP);
						addUserToCalendarManager(s);
					}
				}
			}
		}
		if(nodeID < getHash(this.successorIP)){
			String response2 = WebCaller.callNodeForData(this.successorIP, urls);
			if(response2.equals(""))
				Main.log("node balance received empty list of names", response2);
			else{
				String[] responsibleUsers = response2.split(",");
				for(String s: responsibleUsers){
					if(!this.myUsers.contains(s)){
						this.myUsers.add(s);
						WebCaller.downloadFileFromNode(s,successorIP);
						addUserToCalendarManager(s);
					}
				}
			}
		}
	}

	/**
	 * Hearbeat communication, each 3 seconds this node sends request to its successor and predecessor 
	 * In case of failure it has the responsibility to stablize the node primary and backup keys.
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 * @throws InterruptedException 
	 * @throws Exception
	 */
	public void probe() throws Exception{
		try {
			String result = WebCaller.callNodeForData(successorIP, "setPredecessor?predecessorIP="+ip+"&popIP="+this.predecessorIP);
			if(!result.equals("true"))
				throw new Exception("set predecessor false");
		} catch (Exception e){
			Main.log("Node"+nodeID,e.getMessage() + e.getLocalizedMessage());
			this.setSuccessorIP(this.sosIP);
			this.sosIP = null;
		} 
		try {
			String result = WebCaller.callNodeForData(predecessorIP, "setSuccessor?successorIP="+ip+"&sosIP="+this.successorIP);
			if(!result.equals("true"))
				throw new Exception("set predecessor false");
		} catch (Exception e) {
			Main.log("Node"+nodeID,e.getMessage() + e.getLocalizedMessage());
			Main.log("Pre faile before sleep ", "P="+this.predecessorIP + ", POP=" + this.popIP + ", S="+this.successorIP + ", SOS=" + this.sosIP);
			this.setPredecessorIP(this.popIP);
			this.popIP = null;
			
			Thread.sleep(10000);
			
			Main.log("Pre faile after sleep ", "P="+this.predecessorIP + ", POP=" + this.popIP + ", S="+this.successorIP + ", SOS=" + this.sosIP);
			//String result = WebCaller.callNodeForData(predecessorIP, "setSuccessor?successorIP="+ip+"&sosIP="+this.successorIP);
			Main.log("Node status after failure", this.toString());
			@SuppressWarnings("unchecked") //clone is fine here.
			ArrayList<String> copyOfBackup = (ArrayList<String>) this.backup.clone();
			for(String name: copyOfBackup){
				Main.log("Predecessor down - moving backup", name);
				if(getHash(name) > nodeID){
					Main.log("Predecessor down - sending backup to",this.predecessorIP);
					try {
						WebCaller.callNodeSendFile(this.predecessorIP, "addToPrimary", Main.backup+name+".txt");
						//remove from backup
						//WebCaller.callNode(this.predecessorIP, "removeFromBackup?username="+name);
					} catch (Exception e1) {
						Main.log("Error after node failure during stablization", e1.getMessage());
					}
					//WebCaller.callNode(this.predecessorIP,"addToPrimary?username="+name);
				}
				else{
					Main.log("Predecessor down - adding user to mine",name);
					this.myUsers.add(name);
					FileUtils.copyFile(new File(Main.backup+ name+".txt"), new File(Main.primary+name+".txt"));
					addUserToCalendarManager(name);
					this.backup.remove(name);
					FileUtils.deleteQuietly(new File(Main.backup + name + ".txt"));
					WebCaller.callNodeSendFile(this.successorIP, "addToBackup", Main.primary+name+".txt");
				}
			}
			//ask predecessor to give me backup.
			try {
				WebCaller.callNode("http://"+this.predecessorIP+":8080/DistCalendar/rest/admin/fetchBackup");
			} catch (Exception e1) {
				Main.log("Thread error after node failure and during stablization", e1.getMessage());
				e.printStackTrace();
			}
			Main.log("Node status after stablizing", this.toString());
		}
	}
	
	/**
	 * Send my successor all my primary files.
	 */
	public void addToSuccessorBackup() throws ClientProtocolException, IOException{
		for(String name: this.myUsers){
			Main.log("sending file to " + this.successorIP, name);
			WebCaller.callNodeSendFile(this.successorIP, "addToBackup", Main.primary+name+".txt");
		}
	}
	/**
	 * Removes user from calendar manager and deletes the presistent data as well.
	 * @param username
	 */
	public void removeUserFromCalendarManager(String username){
		Main.calendarManager.users.remove(username);
		Main.calendarManager.calendars.remove(username);
		FileUtils.deleteQuietly(new File(Main.primary + username + ".txt"));
	}
	/**
	 * Adds users to calendar manager
	 */
	private void addUserToCalendarManager(String username) throws IOException{
		Main.calendarManager.users.put(username, new User(username,""));
		Calendar calender = new Calendar();
		ArrayList<Event> events = Utils.readFileFromPrimary(username);
		calender.setEvents(events);
		Main.calendarManager.calendars.put(username,calender);
	}
	/**
	 * Calls add2succssorbackup method on itself as well as its predecessor. 
	 */
	public void initiateBackup() throws ClientProtocolException, IOException {
		if(this.sosIP != null && !this.sosIP.equals(ip)){
			for(String name: this.myUsers){
				WebCaller.callNode(this.sosIP, "removeFromBackup?username="+name);
			}
		}
		WebCaller.callNode("http://"+this.predecessorIP+":8080/DistCalendar/rest/admin/startBackup");
		//delete successor backup list
		WebCaller.callNode("http://"+this.successorIP+":8080/DistCalendar/rest/admin/clearBackupList");
		addToSuccessorBackup();
	}
	/**
	 * New node join request. Assigns the left and right for the joining node if it can else forwards the request to its successor
	 * @param nextNodeIP the node that wants to join
	 * @param visited how may node had this request been so far
	 * @throws Exception
	 */
	public String joinRequest(String nextNodeIP, List<Integer> visited) throws Exception {
		int hash = getHash(nextNodeIP);
		int successorID = getHash(this.successorIP);
		String url="";
		//send add request to head first.
		if (nextNodeIP.equals(ip)){
			return "Single Node case";
		}else if(successorIP.equals(ip))  //2 node case 1 is asking another to form ring
			WebCaller.callNode(nextNodeIP,"setTable?left="+ip + "&right="+ip);
		else if(hash>nodeID && hash<successorID)
			WebCaller.callNode(nextNodeIP,"setTable?left="+ip + "&right="+this.successorIP);
		else if(visited.contains(nodeID))
			WebCaller.callNode(nextNodeIP,"setTable?left="+this.predecessorIP + "&right="+ip);
		else{
			url = "join?nodeIP="+nextNodeIP+"&visited="+ nodeID + getVisitedListAsQuery(visited) ;
			Main.log("Node"+nodeID,url);
			WebCaller.callNode(this.successorIP,url);	
		}
		return "Multiple node case";
	}
	public String getVisitedListAsQuery(List<Integer> visited){
		String out = "";
		for(Integer i: visited)
				if(i!=null)
					out += "&visited="+i;
		return out;
	}
	/**
	 * Finds the head of the ring.
	 */
	public String findHead() throws ClientProtocolException, IOException{
		if(nodeID<=getHash(this.predecessorIP))
			return ip;
		else
			return WebCaller.callNodeForData(this.predecessorIP,"findHead");
	}
	

	public String getSuccessorIP() {
		return successorIP;
	}
	public void setSuccessorIP(String successorIP) {
		this.successorIP = successorIP;
	}
	public String getPredecessorIP() {
		return predecessorIP;
	}
	public void setPredecessorIP(String predecessorIP) {
		this.predecessorIP = predecessorIP;
	}
	public String getIp() {
		return ip;
	}
	public int getNodeID() {
		return nodeID;
	}
	public ArrayList<String> getMyUsers() {
		return this.myUsers;
	}
	public boolean hasUser(String username){
		return this.myUsers.contains(username);
	}
	public ArrayList<String> getBackup() {
		return this.backup;
	}
	public int getHash(String data){
		return Node.hash(data);
	}
	@Override
	public String toString() {
		return "Node [ip=" + ip + ", successorIP=" + successorIP
				+ ", predecessorIP=" + predecessorIP + ", sosIP="
				+ sosIP + ", popIP="+ popIP + "]";
	}
	public void setSosIP(String sosIP2) {
		this.sosIP = sosIP2;
		
	}
	public void setPopIP(String popIP2) {
		this.popIP = popIP2;	
	}
	
	public String getSOSIP(){
		return this.sosIP;
	}

	public String getPOPIP(){
		return this.popIP;
	}
	public void setUsers(ArrayList<String> arrayList) {
		this.myUsers = arrayList;
	}
	/**
	 * Returns all the keys that has higher value than nodeID
	 * @param ip2
	 * @return
	 */
	public String getHigherKeys(String ip2) {
		Main.log("node balance getHigherKeys",ip + ">" +ip2);
		String names = "";
		String name;
		ArrayList<String> toRemove = new ArrayList<String>();
		for(int i = 0; i < this.myUsers.size(); i++){
			name = this.myUsers.get(i);
			int nameHash = this.getHash(name);
			if(nameHash > nodeID){
				names+=name+",";
				toRemove.add(name);
			}
		}
		for(String s: toRemove){
			this.myUsers.remove(s);
		}
		Main.log("node balance getHigherKeys result ",names);
		return names;
	}
	/**
	 * Returns all the keys that has lower value that given nodeID
	 * @param ip2
	 * @return
	 */
	public String getLowerKeys(String ip2) {
		Main.log("node balance getLowerKeys",ip + ">" +ip2);
		String names = "";
		String name;
		ArrayList<String> toRemove = new ArrayList<String>();
		for(int i = 0; i < this.myUsers.size(); i++){
			name = this.myUsers.get(i);
			int nameHash = this.getHash(name);
			if(nameHash <= getHash(ip2)){
				names+=name+",";
				toRemove.add(name);
			}
		}
		for(String s: toRemove){
			this.myUsers.remove(s);
		}
		Main.log("node balance getLowerKeys result ",names);
		return names;
	}
	/**
	 * Looks up the given username and return the corrent node ip, if this node does not have it forwards the request to its immediate
	 * successor.
	 */
	public String lookup(String username, List<Integer> visited) throws ClientProtocolException, IOException {
		if(this.myUsers.contains(username))
			return ip;
		else if(visited.contains(nodeID))
			return ip;
		else
			return WebCaller.callNodeForData(this.successorIP, "lookup?username="+username+"&visited="+nodeID+getVisitedListAsQuery(visited));
	}

	/**
	 * Hashes the given string and then mods it by 67 to get number within the range 0 - 66
	 *
	 */
	public static int hash(String value)
    {
        MessageDigest md;
        int hashtext = 0;
        try 
        {
            md = MessageDigest.getInstance("SHA-1");
            byte[] digest=md.digest(value.getBytes());
            BigInteger big = new BigInteger(1,digest);
            BigInteger hashed = big.mod(new BigInteger("73"));
            hashtext = hashed.intValue();
        } catch (NoSuchAlgorithmException e1) {
        	Main.log("Hashing error", e1.getMessage());
        }
        return hashtext; 
    }
}