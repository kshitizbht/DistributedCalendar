package com.rest.resources;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.apache.commons.io.FileUtils;

import com.rest.calendar.Calendar;
import com.rest.calendar.CalendarManager;
import com.rest.calendar.Event;
import com.rest.calendar.NotifierThread;
import com.rest.chord.Node;
import com.rest.classes.User;
import com.rest.classes.Utils;

/**
 * Main servlet that starts along with the server.
 * Creates an instance of calendar manager and load all the calendar objects for all users as well
 * @author Kshitiz
 *
 */
public class Main extends HttpServlet{

	private static final long serialVersionUID = 1L;
	/**
	 * The proxy calendar manager is static so that we can at least maintain client states based
	 * on cookie.
	 */
	public static CalendarManager calendarManager;
	public static String directory="";
	public static String primary = "";
	public static String backup = "";
	public static String userDB = "";
	public static String location = "";
	public static Node node=null;
	public static String serverPath="";
	private Thread notifierThread = new NotifierThread();
	/**
	 * Initializes the proxy calendarmanager as soon as the server 
	 * starts.
	 */
	@SuppressWarnings("deprecation")
	@Override
	public void init() throws ServletException{
		System.out.println("Distributed Calendar booting....");
		calendarManager = new CalendarManager();
		try {
			String path = getServletContext().getRealPath("/").replace("\\", "/");
			serverPath = path;
			Main.directory = path+"loadCalendarDB/";
			Main.userDB = path+"users.txt";
			Main.primary = path + "dbPrimary/";
			Main.backup = path + "dbBackup/";
			//notifierThread.start();
			File outFile = new File(serverPath+"log.txt");
			Date now = new Date();
			StringBuilder stringBuilder = new StringBuilder();
			stringBuilder.append(now.toLocaleString());
			stringBuilder.append(":: Distributed Calendar logging");
			stringBuilder.append(System.lineSeparator());
			FileUtils.write(outFile,stringBuilder.toString(),false);
			System.out.println("Logging on " + serverPath + "log.txt");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	/**
	 * Called when server needs to stop this service, it will inherently stop the thread
	 * that is looking at all the events.
	 */
	@Override
	public void destroy(){
		if(notifierThread != null)
			notifierThread.interrupt();
		if(node != null){
			node.shutdownNotRequested = false;
			node.interrupt();
		}
	}
	
	/**
	 * Load all the persistent calendar objects and puts it in calendar manager 
	 * and loads all the users as well.
	 * @throws IOException 
	 */
	public static void loadPersistentCalendars() throws IOException {
		ArrayList<String> usernames = Utils.getUsernames();
		for(String username: usernames){
			calendarManager.users.put(username, new User(username,""));
			Calendar calender = new Calendar();
			ArrayList<Event> events = Utils.readFileFromDB(username);
			Utils.copyFile(Main.directory+username+".txt", Main.primary+username+".txt");
			calender.setEvents(events);
			calendarManager.calendars.put(username, calender);
		}	
	}
	
	public static void log(String d,String s){
		File outFile = new File(serverPath+"log.txt");
		String out = d+":["+s+"]"+System.lineSeparator();
		try {
			System.out.print(out);
			FileUtils.write(outFile,out, true);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
