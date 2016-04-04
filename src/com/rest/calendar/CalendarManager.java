package com.rest.calendar;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.apache.commons.io.FileUtils;

import com.rest.chord.WebCaller;
import com.rest.classes.User;
import com.rest.classes.Utils;
import com.rest.resources.Main;
/**
 * Calendar Manager is use to manage all the calendar object for each user and also
 * based on client request modifies the respective calendar object.
 * @author Kshitiz Bhattarai
 *
 */
public class CalendarManager {

	public HashMap<String,User> users;
	public HashMap<String,Calendar> calendars;
	
	public CalendarManager(){
		users = new HashMap<String,User>();
		calendars = new HashMap<String,Calendar>();
	}
	
	public Calendar getCalendar(User user){
		return calendars.get(user.getUsername());
	}
	public Calendar getCalendar(String username){
		return calendars.get(username);
	}
	/**
	 * Returns a list of events pertaining to the user calendar
	 * @param username owner of the calendar
	 * @param begin the begining time range
	 * @param end the ending time range
	 * @return list of events within the given range for username
	 */
	public ArrayList<Event> getEvents(String username,Date begin, Date end){
		if(username==null || username.equals(""))
			return new ArrayList<Event>();
		else
			return calendars.get(username).getEvents(begin,end);
	}
	
	
	/**
	 * Schedule the given event to all the users specified by userlist.
	 * @param members list of users to whom the given needs to be scheduled.
	 * @param event the event to be scheduled.
	 * @throws Exception 
	 */
	public void scheduleEvent(List<String> members, Event event, boolean sleep) throws Exception{
		Collections.sort(members);
		Main.log("Event ", members.toString() +" schedule " + event.toString());
		HashMap<String,String> nodes = null;
		
		try{
			nodes = lockEachPersonInGroup(members,event);
		}catch(Exception e){
			unlockEachPersonInGroup(nodes);
			throw new Exception("Conflicting and/or node down");
		}
		Main.log("Schedule Event Thread will sleep for 10 seconds before processing", "Go and kill some nodes remember the old nodes are already locked");
		if(sleep)
			Thread.sleep(30000);
		try{
			scheduleGroupEvent(nodes,event);
			unlockEachPersonInGroup(nodes);
		}catch(Exception e){
			rollback(nodes);
			throw new Exception("Node crashed during scheudling. The schedule transaction has been aborted");
		}
	}	

	/**
	 * Rollback will avoid incosistent data, in case of node failure during mutation it will 
	 * retreive the backup from the user.calendarBackup and restore it.
	 * @param nodes
	 * @throws Exception
	 */
	private void rollback(HashMap<String, String> nodes) throws Exception {
		for(String username: nodes.keySet()){
			if(this.users.containsKey(username)){
				//assumption here there can not be any error during my own rollback.
				Utils.writeStringToFile(username, users.get(username).calendarBackup);
				Calendar calendar = new Calendar();
				calendar.events = Utils.readFileFromPrimary(username);
				Main.calendarManager.calendars.put(username, calendar);
				WebCaller.callNodeSendFile(Main.node.getSuccessorIP(), "addToBackup", Main.primary+username+".txt");
				this.users.get(username).selfLock.unlock();
			}else{
				String url = "http://"+nodes.get(username)+":8080/DistCalendar/rest/calendar/rollback/"+username;
				try{
					WebCaller.callNode(url);
				}catch(Exception e){
					//node is down, don't care.
					//ignore this node, do it for the other users.
				}
			}
		}
		
	}
	/**
	 * Unlock each user clendar object once the users thing is done.
	 * @param nodes
	 */
	private void unlockEachPersonInGroup(HashMap<String,String> nodes) {
		for(String username: nodes.keySet()){
			try{
				WebCaller.callNode("http://"+nodes.get(username)+":8080/DistCalendar/rest/calendar/unlock/"+username);
			}catch(Exception e){
				Main.log("Node " + nodes.get(username) + " was down ", "Ignore and unlock other members " + e.getMessage());
			}
		}	
	}
	/**
	 * Calls each users schedule method to schdeule the givent event
	 * @param nodes
	 * @param e
	 * @throws Exception
	 */
	private void scheduleGroupEvent(HashMap<String,String> nodes, Event e) throws Exception {
		for(String username: nodes.keySet()){
			String url = "http://"+nodes.get(username)+":8080/DistCalendar/rest/calendar/scheduleAndBackup";
			String data = WebCaller.callNodeWithEvent(url, e, username);
			if(!data.equals("true"))
				throw new Exception("unable to schedule events");
		}
	}

	/**
	 * Update group event will follow the same action as above done by schedule event except for it will update an existing event
	 * @param nodes
	 * @param event
	 * @throws Exception
	 */
	private void updateGroupEvent(HashMap<String, String> nodes, Event event) throws Exception {
		for(String username: nodes.keySet()){
			String url = "http://"+nodes.get(username)+":8080/DistCalendar/rest/calendar/updateAndBackup";
			String data = WebCaller.callNodeWithEvent(url, event, username);
			if(!data.equals("true"))
				throw new Exception("unable to update events");
		}
	}

	/**
	 * Probes the ring for each member, ask each member to lock, ask each member if the slot is available or not
	 * @param members
	 * @param event
	 * @return
	 * @throws Exception
	 */
	private HashMap<String,String> lockEachPersonInGroup(List<String> members, Event event) throws Exception{
		HashMap<String,String> nodes = new HashMap<String,String>();
		for(String username: members){
			List<Integer> visited = new ArrayList<Integer>();
			visited.add(null);
			String ip = Main.node.lookup(username, visited);
			Main.log("Locking", username + " at " + ip);
			nodes.put(username,ip);
			String url = "http://"+ip+":8080/DistCalendar/rest/calendar/lock";
			String lockResult = WebCaller.callNodeWithEvent(url, event, username);//WebCaller.callNodeForData(url);
			if(!lockResult.equals("true"))
				throw new Exception("Unable to lock all user calendars in the ring. Try again!");
		}
		return nodes;
	}

	/**
	 * Actual method to schedule the given event.
	 * @param username
	 * @param event
	 * @throws Exception
	 */
	public void scheduleAndBackup(String username, Event event) throws Exception{
		if(users.get(username).selfLock.isLocked()){
			calendars.get(username).scheduleEvent(event);
			Utils.writeOneEvent(username, event);
			WebCaller.callNodeSendFile(Main.node.getSuccessorIP(), "addToBackup", Main.primary+username+".txt");
		}else{
			throw new Exception("Reached a modification resource without acquiring lock");
		}
	}
	
	/**
	 * Acutal method to update the existing method.
	 * @param username
	 * @param event
	 * @throws Exception
	 */
	public void updateEventAndBackup(String username, Event event) throws Exception {
		if(users.get(username).selfLock.isLocked()){
			calendars.get(username).updateEvent(event);
			Utils.writeToFile(username, calendars.get(username).getEvents());
			WebCaller.callNodeSendFile(Main.node.getSuccessorIP(), "addToBackup", Main.primary+username+".txt");
		}else{
			throw new Exception("Reached a modification resource without acquiring lock");
		}
	}
	/**
	 * Actual method to delete an existing event
	 * @param username
	 * @param eventID
	 * @throws Exception
	 */
	public void deleteEventAndBackup(String username,UUID eventID) throws Exception{
		calendars.get(username).deleteEvent(eventID);
		Utils.writeToFile(username, calendars.get(username).getEvents());
		WebCaller.callNodeSendFile(Main.node.getSuccessorIP(), "addToBackup", Main.primary+username+".txt");
	}
	/**
	 * Update the given event to all the users specified by userlist.
	 * This method take similar precaution as scheduleEvent()
	 * @param userlist list of users to whom the given needs to be updated.
	 * @param event the event to be updated.
	 * @throws Exception 
	 */
	public void updateEvent(List<String> members, Event event) throws Exception{
		Collections.sort(members);
		Main.log("Event ", members.toString() +" schedule " + event.toString());
		HashMap<String,String> nodes = null;
		try{
			nodes = lockEachPersonInGroup(members,event);
		}catch(Exception e){
			unlockEachPersonInGroup(nodes);
			throw new Exception("Conflicting time unable to lock all the member, Transaction aborted.");
		}
		try{
			updateGroupEvent(nodes,event);
			unlockEachPersonInGroup(nodes);
		}catch(Exception e){
			rollback(nodes);
			throw new Exception("Update transaction aborted, rolling back to old state.");
		}
	}


	/**
	 * Deletes the given event to all the users specified by member list
	 * First it will delete the event from this node and then forward the request to its successor to delete as well.
	 * @param userlist list of users to whom the given events needs to be delted.
	 * @param event the event to be delted.
	 * @throws IOException 
	 * @throws Exception 
	 */

	public String deleteEvent(UUID eventID, List<Integer> visited) throws Exception{
		if(visited.contains(Main.node.getNodeID())){
			return "Ring complete"; //ring deletion is complete.
		}
		else{
			ArrayList<String> membersOnThisNode = this.getMembersForAnEvent(eventID);
			if(membersOnThisNode.size()>=1){
				Collections.sort(membersOnThisNode);
				for(String username: membersOnThisNode){
					User user = this.users.get(username);
					user.calendarBackup = FileUtils.readFileToString(new File(Main.primary+username+".txt"));
					try{
						user.selfLock.lock();
						this.deleteEventAndBackup(username, eventID);
						user.selfLock.unlock();
					}catch(Exception e){
						Main.log("delete event err", e.getMessage());
						//rollback
						Utils.writeToFile(username, Utils.readCalendarFromString(user.calendarBackup));
						WebCaller.callNodeSendFile(Main.node.getSuccessorIP(), "addToBackup", Main.primary+username+".txt");
						user.selfLock.unlock();
						throw e;
					}
				}
			}
			//delete my stuff forward to others to delete on their.
			visited.add(Main.node.getNodeID());
			String url ="http://"+Main.node.getSuccessorIP()+":8080/DistCalendar/rest/calendar/deleteAndBackup?eventID="+eventID + getVisitedListAsQuery(visited);
			WebCaller.callNode(url);
		}
		return "success";
	}

	
	public String getVisitedListAsQuery(List<Integer> visited){
		String out = "";
		for(Integer i: visited)
				if(i!=null)
					out += "&visited="+i;
		return out;
	}

	public HashMap<String,User> getUsers(){
		return this.users;
	}
	
	
	/**
	 * This function is similar to createCalendar() function specified by the specification with few changes.
	 * When a user tries to log in and view his/her calendar, firstly we try to check it the given username and password match to what is in the db.
	 * Secondly, if user is  in db it checks if there exists a calendar object for this user or not.
	 * If there is, it does nothing, else it creates new Calendar() for the user and appends to the calendar map.
	 * @param user The user who wants to login and view his/her calendar
	 * @throws IOException 
	 */
	public void login(User user) throws IOException{
		user.login();
		if(user.isLoggedIn()){
			if(users.containsKey(user.getUsername())){
				users.get(user.getUsername()).set(user);
				if(this.calendars.containsKey(user.getUsername())){
					System.out.println("User and calendar obj found");
				}else{
					calendars.put(user.getUsername(), new Calendar());
				}
			}else{
				users.put(user.getUsername(), user);
				calendars.put(user.getUsername(), new Calendar());
			}
		}else{
			System.out.println("Invalid userid or pw, retry requested");
		}		
	}
	
	/**
	 * Returns the event matching the given id and for the given user.
	 * @param username The name of the user to whom we need to look for the event
	 * @param id the event id.
	 * @return the event matching the id
	 */
	public Event getEventByID(String username,UUID id){
		return this.getCalendar(username).getEventByID(id);
	}
	
	/**
	 * Returns the list of user who are member of the given group event.
	 * @param id the id of the event
	 * @return the list of user who are member of the event.
	 */
	public ArrayList<String> getMembersForAnEvent(UUID id){
		ArrayList<String> members = new ArrayList<String>();
		for(String member: calendars.keySet()){
			Calendar c = calendars.get(member);
			if(c.getEventByID(id)!=null)
				members.add(member);
		}
		return members;
	}
	
	/**
	 * Checks to see if the given user if the member of the group for the given group id.
	 * @param username the user who is being checked for membership
	 * @param id the id of the group event
	 * @return true if the user is a member else false
	 */
	public boolean isMemberOfGroup(String username,UUID id){
		Calendar c = calendars.get(username);
		if(c.getEventByID(id)!=null){
				return true;
		}
		return false;
	}
}
