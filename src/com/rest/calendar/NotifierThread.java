package com.rest.calendar;

import java.util.ArrayList;

import com.rest.resources.Main;

/**
 * A thread to continuosly monitor and update users for upcoming events
 * @author Kshitiz Bhattarai
 *
 */
public class NotifierThread extends Thread{

	public void run(){
		while(true){
			try {
				sleep(3000);//sleep for a while then proceed
			} catch (InterruptedException e1) {
				Main.log("Notifier thread stopped ", e1.getMessage());
			}
			for(String users: Main.calendarManager.users.keySet()){
				Calendar cal = Main.calendarManager.getCalendar(users);
				ArrayList<Event> events = cal.getEvents();
				for(Event e: events){
					if (e.isApproaching() && !e.userNotified){
						cal.notify(e.toString() + "is approaching in few minutes.", ".");
						e.userNotified = true;
						System.out.println("User notified for approaching event " + e.toString());
					}
				}
			}
		}
	}
	
}
