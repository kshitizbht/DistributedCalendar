package com.rest.classes;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.FileUtils;

import com.rest.calendar.Event;
import com.rest.resources.Main;
/**
 * Helper class that provide static function to read calendar objects from
 * file, write back new events as once they get scheduled.
 * @author Kshitiz
 *
 */
@SuppressWarnings("deprecation")
public class Utils {
	
	/**
	 * Read events from file for a single user specified by username
	 * @param username the user for which to load events for
	 * @return list of events for the specific user
	 * @throws IOException 
	 */
	public static ArrayList<Event> readFileFromPrimary(String username) throws IOException{
		ArrayList<Event> events = new ArrayList<Event>();
		File file = new File(Main.primary+username+".txt");
		String eventsFromFile = FileUtils.readFileToString(file);
		String[] eventAsStrings = eventsFromFile.split("<nextEvent>");
		for(String s: eventAsStrings)
			if(s.length()>3) //ignore the last line
				events.add(new Event(s));
		return events;
	}
	
	public static ArrayList<Event> readFileFromDB(String username) throws IOException{
		ArrayList<Event> events = new ArrayList<Event>();
		File file = new File(Main.directory+username+".txt");
		String eventsFromFile = FileUtils.readFileToString(file);
		String[] eventAsStrings = eventsFromFile.split("<nextEvent>");
		for(String s: eventAsStrings)
			if(s.length()>3) //ignore the last line
				events.add(new Event(s));
		return events;
	}
	
	public static void writeStringToFile(String username, String data) throws IOException{
		FileUtils.write(new File(Main.primary+username+".txt"), data);
	}
	public static ArrayList<Event> readCalendarFromString(String data) {
		ArrayList<Event> events = new ArrayList<Event>();
		String[] eventAsStrings = data.split("<nextEvent>");
		for(String s: eventAsStrings)
			if(s.length()>2)//handling the last null event.
				events.add(new Event(s));
		return events;
	}
	public static void copyFile(String srcFilePath, String destFilePath) throws IOException{
		File srcFile = new File(srcFilePath);
		File destFile = new File(destFilePath);
		FileUtils.copyFile(srcFile, destFile);
	}
	
	/**
	 * Write back list of events for the given user
	 * @param username name of the user
	 * @param events list of events to be added to the file
	 * @throws IOException 
	 */
	public static void writeToFile(String username, ArrayList<Event> events) throws IOException{
		File file = new File(Main.primary+username+".txt");
		for(Event e: events){
			FileUtils.write(file,e.getEntireEventAsString());
		}
	}
	
	/**
	 * Returns list of usernames for which persistent calendar object exists
	 * @return list of usernames
	 */
	public static ArrayList<String> getUsernames(){
		File dir = new File(Main.directory);
		String[] filenames = dir.list();
		ArrayList<String> usernames = new ArrayList<String>();
		for(String s: filenames){
			usernames.add(s.substring(0, s.indexOf('.')));
		}
		return usernames;
	}
	
	/**
	 * Write back one event to the file for the given user
	 * @param username the owner of the calendar
	 * @param event event that is to be written to persistent calendar objects
	 * @throws IOException 
	 */
	public static void writeOneEvent(String username, Event event) throws IOException{
		File file = new File(Main.primary+username+".txt");
		FileUtils.write(file, event.getEntireEventAsString(), true);
	}
	
	/**
	 * Returns event object based on given parameter
	 * @param begin beginning of the event as "2:00"
	 * @param end   ending of the event as "2:00"
	 * @param year  year in XXXX format
	 * @param month month in 0-11 format
	 * @param day   day from 1-31
	 * @param description string description of the event
	 * @param accessControl type of event
	 * @return event object based on provided data.
	 */
	public static Event getEventFromData(String begin, String end, int year,int month, int day,
			String description, AccessControl accessControl) throws Exception{
		int b_hr = Integer.parseInt(begin.split(":")[0]);
		int b_min = Integer.parseInt(begin.split(":")[1]);
		int e_hr = Integer.parseInt(end.split(":")[0]);
		int e_min = Integer.parseInt(end.split(":")[1]);
		Date beginDate = new Date(year,month,day,b_hr,b_min);
		Date endDate = new Date(year,month,day,e_hr,e_min);
		Event e = new Event(beginDate,endDate,description,accessControl);
		return e;
	}
	
	/**
	 * Read the persistent list of user and password from file and compare the given 
	 * username against the file.
	 * @param username username
	 * @param password password
	 * @return true if username and password are correct false otherwise
	 * @throws IOException 
	 */
	public static boolean isValidUser(String username, String password) throws IOException{
		/*File file = new File(Main.userDB);
		List<String> unpwCombo = FileUtils.readLines(file);
		for(String s: unpwCombo){
			String[] unpw = s.split(",");
			if(unpw[0].equals(username) && unpw[1].equals(password))
				return true;
		}
		return false;*/ //PASSWORD VERIFICATION HAS BEEN REMOVED
		File file = new File(Main.primary+username+".txt");
		return file.exists();
	}
	
	/**
	 * Called by signup method to add user to persistent user list
	 * @param username name of the user
	 * @param password password for the user
	 * @throws Exception if user name already exists or a bad character is found on username or password
	 */
	public static void addUser(String username,String password) throws Exception{
		if(username.contains(",") || password.contains(","))
			throw new Exception("Invalid character found on username or password");
		else if(Utils.isValidUser(username, password))
			throw new Exception("User already exists");
		else{
			File file = new File(Main.userDB);
			String data = username + "," + password + System.getProperty("line.separator");
			FileUtils.write(file, data, true);
		}
	}


}
