package com.rest.resources;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;

import com.rest.calendar.Calendar;
import com.rest.calendar.Event;
import com.rest.chord.WebCaller;
import com.rest.classes.User;
import com.rest.classes.Utils;

/**
 * Web service resource to handle login 
 * @author Kshitiz Bhattarai
 *
 */
@Path("login")
@SuppressWarnings("deprecation")
public class LoginResource {

	/**
	 * Tries to login with the provided username and password,
	 * if successful returns the calendar page with the event for the user for this month
	 * else redirects to the failed login page
	 * Seconldy, adds this user to the cookie
	 */
	@GET
	public void login(@QueryParam("username") String username, 
			@Context HttpServletResponse response,@Context HttpServletRequest request){
		HttpSession session = request.getSession(true);
		User user = null;
		try {
			List<Integer> visited = new ArrayList<Integer>();
			visited.add(null);
			String ip = Main.node.lookup(username,visited);
			Main.log("login request for " + username, "found user at "+ip);
			if(ip.equals(Main.node.getIp())){
				user = new User(username,username);
				Main.calendarManager.login(user);
			}else{
				response.sendRedirect("http://"+ip+":8080/DistCalendar/rest/login/?username="+username);
			}
		} catch (Exception e1) {
			try {
				response.sendRedirect("/DistCalendar/errorpage.html");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		session.setAttribute("User", user);
		try{
			if(user.isLoggedIn()){
				Date date = new Date();
				int year = date.getYear();int month=date.getMonth();
				user.browsingDate = date;
				response.sendRedirect("/DistCalendar/rest/calendar/getMyCaldendar/"+user.getUsername()+"/"+year+"/"+month);
			}else{
				response.sendRedirect("../loginfailed.html");			
			}
		}
		catch(Exception e){
			System.out.println(e.getMessage());
		}		
	}

	/**
	 * Adds the given username and password to the persistent user list at the correct node.
	 * @throws IOException 
	 */
	@GET
	@Path("signup")
	public void signup(@QueryParam("username") String username, @QueryParam("password") String password,
			@Context HttpServletResponse response) throws IOException{
		boolean thisNode = false;
		int myID = Main.node.getNodeID();
		int successorID = Main.node.getHash(Main.node.getSuccessorIP());
		int usernameID = Main.node.getHash(username);
		int predecessorID = Main.node.getHash(Main.node.getPredecessorIP());
		Main.log("Sign up request for " + username + " with id=" + usernameID, "myId="+myID+" succID" + successorID);
		
		//1 Node case
		if(myID == successorID){
			thisNode = true;
		}
		else if(usernameID > predecessorID && usernameID <= myID){
			thisNode = true;
		}else if(usernameID > myID && myID > successorID){ //at the edge of ring last guy needs to hold all keys
			thisNode = true;
		}else if(usernameID <= myID && myID < predecessorID){//at another edge the smallest node.
			thisNode = true;
		}
		if(thisNode){
			Main.log("Sign up request for " + username + " with id=" + usernameID, "doing it at my node");
			try{
				Main.node.myUsers.add(username);
				Utils.addUser(username, password);
				File file = new File(Main.primary+username+".txt");
				file.createNewFile();
				Main.calendarManager.users.put(username, new User(username,""));
				Calendar calender = new Calendar();
				ArrayList<Event> events = Utils.readFileFromPrimary(username);
				calender.setEvents(events);
				Main.calendarManager.calendars.put(username,calender);
				WebCaller.callNodeSendFile(Main.node.getSuccessorIP(), "addToBackup", Main.primary+username+".txt");
				response.sendRedirect("/DistCalendar/login.html");
			}catch(Exception e){
				try {
					System.out.println(e.getMessage());
					response.sendRedirect("/DistCalendar/errorpage.html");
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		}else{
			//forward to correct guy.
			Main.log("Sign up request for " + username + " with id=" + usernameID, "Forward to successor ip");
			try {
				response.sendRedirect("http://"+Main.node.getSuccessorIP()+":8080/DistCalendar/rest/login/signup?username="+username+"&password=null");
			} catch (Exception e) {
				response.sendRedirect("/DistCalendar/errorpage.html");
			}
		}
		
	}
	
	
	@POST
	@Path("logOut")
	public void logOut(@Context HttpServletResponse response,@Context HttpServletRequest request){
		HttpSession session = request.getSession(true);
		session.setAttribute("User", null);
		try {
			response.sendRedirect("/DistCalendar/login.html");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
