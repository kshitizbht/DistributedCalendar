package com.rest.resources;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;

import org.apache.commons.io.FileUtils;
import org.apache.http.client.ClientProtocolException;

import com.rest.calendar.Calendar;
import com.rest.calendar.Event;
import com.rest.chord.WebCaller;
import com.rest.classes.AccessControl;
import com.rest.classes.Notification;
import com.rest.classes.User;
import com.rest.classes.Utils;
import com.sun.jersey.api.view.Viewable;

/**
 * Proxy class that is invoked by web request from client interface and thus invokes necessary methods on CalendarManager object
 * @author Kshitiz
 *
 */
@Path("calendar")
@SuppressWarnings("deprecation")
public class CalenderManagerService {

	public HttpSession session;
	
	/**
	 * Returns specific html or jsp page only for valid user.
	 * @param user the logged in user
	 * @param page the page user should be navigated to
	 * @param data the data the page should receive by default
	 * @return page that is requested by user
	 */
	public Viewable getPage(User user,String page,Object data){
		if(user.isLoggedIn())
			return new Viewable(page,data);
		else
			return new Viewable("/jsp/errorpage","Bad cookie or Server failed to log you in.");

	}
	
	public void setCalendarOnCookie(HttpServletRequest request, String username, String loggedinUser) throws ClientProtocolException, IOException{
		HttpSession session = request.getSession();
		if(Main.node.hasUser(username))
			session.setAttribute("Calendar",Main.calendarManager.getCalendar(username));
		else{
			Main.log("view calendar request from user " + loggedinUser, " viewing user " + username);
			List<Integer> visited = new ArrayList<Integer>();
			visited.add(null);
			String ip = Main.node.lookup(username, visited);
			Main.log("lookup " + username, "found on " + ip);
			if(!ip.equals(Main.node.getIp())){
				String data = WebCaller.callNodeForData("http://"+ip+":8080/DistCalendar/rest/calendar/fetchCalendar/"+username);
				Calendar calendar = new Calendar();
				ArrayList<Event> events = Utils.readCalendarFromString(data);
				calendar.setEvents(events);
				session.removeAttribute("Calendar");
				session.setAttribute("Calendar", calendar);
			}
		}
	}
	/**
	 * Get the cookie from the request and the user object that is on the cookie
	 * @param request http request from the browser
	 * @param response http response to the browser
	 * @return user object from the cookie
	 */
	public User getCurrentState(HttpServletRequest request,HttpServletResponse response){
		  HttpSession session = request.getSession();
		  if(session.getAttribute("User") == null){
			  System.out.println("Bad cookie");
			  User user = new User(false);
			  return user;
		  }else{		  
			  return (User) request.getSession().getAttribute("User");
		  }
	}
	
	/**
	 * Returns the calendar for given user(which can be anybody that has a calendar object) with the given month and year.
	 * @return the calendar.jsp for the given user with the given month and year
	 * @throws IOException 
	 */
	@GET
	@Path("getMyCaldendar/{username}/{year}/{month}")
	@Produces("text/html")
	public Viewable getMyCalendar(@PathParam("month") int month,@PathParam("year") int year,@PathParam("username") String username,
			@Context HttpServletRequest request,@Context HttpServletResponse response) throws IOException{
		User user = getCurrentState(request, response);
		Date thisMonth = new Date(year,month,1);
		user.browsingDate = thisMonth;
		setCalendarOnCookie(request, username, user.getUsername());
		return getPage(user,"/jsp/calendar",username);
	}
	

	@GET
	@Path("fetchCalendar/{username}")
	@Produces("text/html")
	public String fetchCalendar(@PathParam("username") String username){
		try {
			return FileUtils.readFileToString(new File(Main.primary+username+".txt"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			Main.log("error sending file to node", e.getMessage());
			return ".";
		}
	}
	
	/**
	 * Returns new page with the events that are scheduled for the given date on the path
	 * @return page with the events for the day
	 */
	@GET
	@Path("getEventsForDay/{username}/{year}/{month}/{day}")
	@Produces("text/html")
	public Viewable getEventsForDay(@PathParam("day") int day,@PathParam("month") int month,@PathParam("year") int year,
			@PathParam("username") String username,@Context HttpServletRequest request,@Context HttpServletResponse response){
		try{
			User user = getCurrentState(request, response);
			HttpSession session = request.getSession();
			Calendar calendar = (Calendar) session.getAttribute("Calendar");//Main.calendarManager.getCalendar(username);
			Date begining = new Date(year,month,day,0,0);
			ArrayList<Event> selectedDay = calendar.getEventsForaDay(begining);
			user.browsingDate = begining;
			Object[] data = {username,selectedDay};
			return getPage(user,"/jsp/day",data);
		}catch(Exception e){
			return new Viewable("/jsp/errorpage",e.getMessage());
		}
	}

	private List<String> getMembers(String membersStr){
		String[] membersSplit = membersStr.split(",");
		List<String> members = new ArrayList<String>();
		for(String s: membersSplit){
			members.add(s);
		}
		return members;
	}
	/**
	 * Schedules given event to the user calendar with the data submitted from the form
	 * @return the calendar page with updated events
	 */
	@POST
	@Path("scheduleEvent/{username}/{year}/{month}/{day}")
	@Produces("text/html")
	public Viewable scheduleEvent(@PathParam("day") int day,@PathParam("month") int month,@PathParam("year") int year,
			@FormParam("begin") String begin,@FormParam("end") String end,@FormParam("description") String description,
			@FormParam("access") AccessControl accessControl,@PathParam("username") String username,@FormParam("members")String membersStr,
			@FormParam("sleep") String sleep,
			@Context HttpServletRequest request,@Context HttpServletResponse response){
		try{
			List<String> members = getMembers(membersStr);
			getCurrentState(request, response);
			Event e = Utils.getEventFromData(begin, end, year, month, day, description, accessControl);
			if(sleep.equals("true"))
				Main.calendarManager.scheduleEvent(members,e,true);
			else
				Main.calendarManager.scheduleEvent(members,e,false);
			return getMyCalendar(month,year,username,request,response);
		}catch(Exception e){
			return new Viewable("/jsp/errorpage","Unable to schedule either conflicting or node failure");
		}
	}
	
	/**
	 * Returns the schedule page where user can type in the details of the event that they want to schedule
	 * @return the schedule page
	 */
	@POST
	@Path("schedule")
	@Produces("text/html")
	public Viewable schedule(@Context HttpServletRequest request,@Context HttpServletResponse response){
		User user = getCurrentState(request, response);
		return getPage(user,"/jsp/schedule",user.getUsername());
	}
	
	/**
	 * Deletes the event from the user calendar as long as s/he is the owner or a member for the given event
	 * @return the calendar page with updated details
	 */
	@POST
	@Path("delete/{username}/{year}/{month}/{day}")
	@Produces("text/html")
	public Viewable deleteEvent(@PathParam("day") int day,@PathParam("month") int month,@PathParam("year") int year,
			@FormParam("id") UUID id,@PathParam("username") String username,
			@Context HttpServletRequest request,@Context HttpServletResponse response){
		try{
			getCurrentState(request, response);
			List<Integer> visited = new ArrayList<Integer>();
			visited.add(null);
			Main.calendarManager.deleteEvent(id,visited);
			return getMyCalendar(month,year,username,request,response);
		}catch(Exception e){
			return new Viewable("/jsp/errorpage","Unable to delete either locked or node failure");
		}

	}
	
	/**
	 * Returns the edit event page that is specified by given event id
	 * @return the page that allows user to edit an existing event
	 */
	@POST
	@Path("editEvent/{username}")
	@Produces("text/html")
	public Viewable editEvent(@FormParam("id") UUID id,@PathParam("username") String username,
			@Context HttpServletRequest request,@Context HttpServletResponse response){
		try{
			User user = getCurrentState(request, response);
			ArrayList<String> users = new ArrayList<String>();
			users.add(user.getUsername());
			Event e = Main.calendarManager.getEventByID(username, id);
			Object[] data = {username,e};
			System.out.println(e.getBegin().toLocaleString());
			return getPage(user,"/jsp/editEvent",data);
		}catch(Exception e){
			return new Viewable("/jsp/errorpage",e.getMessage());
		}
	}
	
	/**
	 * Updates the event that user has just edited. Most of the error handling
	 * is already done in client side.
	 * @return the calendar for the given user with the updated event change
	 */
	@POST
	@Path("updateEvent/{username}/{year}/{month}/{day}")
	@Produces("text/html")
	public Viewable updateEvent(@PathParam("day") int day,@PathParam("month") int month,@PathParam("year") int year,
			@FormParam("begin") String begin,@FormParam("end") String end,@FormParam("description") String description,
			@FormParam("access") AccessControl accessControl,@FormParam("id") UUID id,@FormParam("members")String membersStr,
			@PathParam("username") String username,@Context HttpServletRequest request,@Context HttpServletResponse response){
		try{
			getCurrentState(request, response);
			Event e = Utils.getEventFromData(begin, end, year, month, day, description, accessControl);
			e.setID(id);
			List<String> members = getMembers(membersStr);
			Main.calendarManager.updateEvent(members,e);
			return getMyCalendar(month,year,username,request,response);
		}catch(Exception e){
			return new Viewable("/jsp/errorpage","Unable to update event either conflicting or node failure");
		}
	}
	
	/**
	 * Returns a notification for the given user if there exists a notification else 
	 * returns an empty notification.
	 * @return notification if there exists any for the given user
	 */
	@GET
	@Path("getNotification/{username}")
	@Produces("application/json")
	public String getNotification(@PathParam("username") String username){
		Calendar cal = Main.calendarManager.getCalendar(username);
		if(cal != null){
			String notification = cal.getNotification();
			if(!notification.contains("'.',"))
				Main.log("Notification", notification);
			return notification;
		}else{
			return new Notification(".",".").toString();
		}
	}
	
	/**
	 * Removes the notification from the notification list if it has been consumed by the user
	 * @return empty notification
	 */
	@GET
	@Path("removeNotification/{username}")
	@Produces("application/json")
	public String removeNotification(@PathParam("username") String username){
		Calendar cal = Main.calendarManager.getCalendar(username);
		cal.safeToRemoveNotification();
		return new Notification(".",".").toString();
	}
	
	/**
	 * Set up a backup point, checks to see if user has any colliding event, finally lock
	 * Return true if successful as locking false othwerise
	 * @param username
	 * @param event
	 */
	@POST
	@Path("lock")
	public String lock(@FormParam("username")String username, @FormParam("event") String event){
		try {
			//create backup
			Main.calendarManager.users.get(username).calendarBackup = FileUtils.readFileToString(new File(Main.primary+username+".txt"));
			//Main.log("temp backup in lock", Main.calendarManager.users.get(username).calendarBackup);
			//check for collision
			Event e = new Event(event);
			if(Main.calendarManager.calendars.get(username).isConflicting(e) && !Main.calendarManager.isMemberOfGroup(username, e.getID())){
				throw new Exception("Conflicting times"); //only throw error during if scheduling and not updating.
			}
			else{
				//finally lock
				Main.calendarManager.users.get(username).selfLock.lock();
				return "true";
			}
		} catch (Exception e) {
			Main.log("calendar manager during lock",e.getMessage());
			return "false";
		}
	}
	/**
	 * Unlock user calendar object
	 * @param username
	 * @return true
	 */
	@GET
	@Path("unlock/{username}")
	public String unlock(@PathParam("username") String username){
		Main.calendarManager.users.get(username).selfLock.unlock();
		return "true";
	}
	/**
	 * After transaction abort each calendar is move back to its earlier state.
	 * @param username
	 * @return
	 */
	@GET
	@Path("rollback/{username}")
	public String rollback(@PathParam("username") String username){
		try{
			Main.calendarManager.users.get(username).selfLock.unlock();
			Utils.writeToFile(username, Utils.readCalendarFromString(Main.calendarManager.users.get(username).calendarBackup));
			WebCaller.callNodeSendFile(Main.node.getSuccessorIP(), "addToBackup", Main.primary+username+".txt");
			return "true";
		}catch(Exception e){
			Main.log("error during rollback", e.getMessage());
			return e.getMessage();
		}
	}
	/**
	 * Method called my initiator to schedule an event and backup
	 * @param username
	 * @param event
	 * @return
	 */
	@POST
	@Path("scheduleAndBackup")
	public String scheduleAndBackup(@FormParam("username")String username, @FormParam("event")String event){
		try {
			Main.calendarManager.scheduleAndBackup(username, new Event(event));
			return "true";
		} catch (Exception e) {
			Main.log("scheduleAndBackup",e.getMessage());
			return "false";
		}
	}
	
	/**
	 * Similar to schedule above.
	 * @param username
	 * @param event
	 * @return
	 */
	@POST
	@Path("updateAndBackup")
	public String updateAndBackup(@FormParam("username")String username, @FormParam("event")String event){
		try {
			Main.calendarManager.updateEventAndBackup(username, new Event(event));
			return "true";
		} catch (Exception e) {
			Main.log("updateAndBackup",e.getMessage());
			return "false";
		}
	}
	
	/**
	 * Similar to update above.
	 * @param eventID
	 * @param visited
	 * @return
	 */
	@GET
	@Path("deleteAndBackup")
	public String deleteAndBackup(@QueryParam("eventID")UUID eventID, @QueryParam("visited")List<Integer> visited){
		try {
			Main.calendarManager.deleteEvent(eventID,visited);
			return "true";
		} catch (Exception e) {
			Main.log("deleteAndBackup",e.getMessage());
			return "false";
		}
	}
}
