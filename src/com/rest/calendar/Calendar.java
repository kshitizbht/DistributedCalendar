package com.rest.calendar;

import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.UUID;

import com.rest.classes.Notification;
/**
 * This class denotes a calendar for a single user, it contains a list of events.
 * You can schedule, delete, update events as well as get events for the user as well.
 * @author Kshitiz Bhattarai
 */
@SuppressWarnings("deprecation")
public class Calendar{
	
	public ArrayList<Event> events;
	public ArrayList<Notification> notifications;
	
	public Calendar(){
		this.events = new ArrayList<Event>();
		notifications = new ArrayList<Notification>();
	}

	/**
	 * This method returns all the event in the user calendar that are within the range of given time.
	 * @param begin The begining date range. Ex. 10/10/1986 6:30 am
	 * @param end The ending date range.Ex. 10/11/1986 6:30 am
	 * @return the list of events contained within range.
	 */
	public ArrayList<Event> getEvents(Date begin, Date end){
		ArrayList<Event> relatedEvents = new ArrayList<Event>();
		for(Event e: events){
			if(e.getBegin().after(begin) && e.getBegin().before(end)) 
				relatedEvents.add(e);
		}
		return relatedEvents;
	}
	
	
	/**
	 * Schedules given event to this user calendar.
	 * @param e Event to be scheduled
	 */
	public void scheduleEvent(Event e){
		events.add(e);
		this.notify("New Event " + e.toString() + " was added.","");
	}

	/**
	 * Updates an existing event from the user calendar based on the event ID.
	 * @param e Modified event
	 */
	public void updateEvent(Event e){
		for(int i=0;i<events.size();i++){
			Event event = events.get(i);
			if(event.getID().equals(e.getID())){
				event.setAccessControl(e.getAccessControl());
				event.setBegin(e.getBegin());
				event.setEnd(e.getEnd());
				event.setDescription(e.getDescription());
				this.notify("Event " + event.toString() + " was modified.", "");
			}
		}

	}
	
	/**
	 * Deletes an event from this calendar if it exists.
	 * @param eventID  ID of the event to be deleted.
	 */
	public void deleteEvent(UUID eventID) {
		Event toDelete = null;
		for(Event e: events)
			if(e.getID().equals(eventID))
				toDelete = e;
		if(toDelete!=null){
			events.remove(toDelete);
			this.notify("Event " + toDelete.toString() + " was deleted.", "http://www.google.com");	
		}
	}

	/**
	 * Returns all the events that occur in the given month
	 * @param begining the beginning date for this month
	 * @return list of events
	 */
	public ArrayList<Event> getEventsForAMonth(Date begining){
		ArrayList<Event> relatedEvents = new ArrayList<Event>();
		GregorianCalendar c = new GregorianCalendar();
		c.set(GregorianCalendar.MONTH,begining.getMonth());
		int lastDay = c.getActualMaximum(GregorianCalendar.DAY_OF_MONTH);
		Date ending = new Date(begining.getYear(),begining.getMonth(),lastDay);
		relatedEvents = this.getEvents(begining, ending);
		return relatedEvents;
	}
	
	/**
	 * Returns all the events that occur on the given day
	 * @param begining the date for the given day
	 * @return list of events 
	 */
	public ArrayList<Event> getEventsForaDay(Date begining){
		ArrayList<Event> relatedEvents = new ArrayList<Event>();
		Date ending = new Date(begining.getYear(),begining.getMonth(),begining.getDate(),23,59);
		relatedEvents = this.getEvents(begining, ending);
		return relatedEvents;
	}

	/**
	 * Returns all the events from this calendar.
	 * @return list of events from this calendar.
	 */
	public ArrayList<Event> getEvents() {
		return events;
	}

	public void setEvents(ArrayList<Event> events) {
		this.events = events;
	}

	/**
	 * Returns the event matching the given id.
	 * @param id Event id
	 * @return Event e that has the given id
	 */
	public Event getEventByID(UUID id){
		for(Event e: events){
			if(e.getID().equals(id))
				return e;
		}
		return null;
	}
	
	/**
	 * Return if any of the events in this calendar conflicts with the given event or not.
	 * Secondly, the given event should be different for it to conflict. 
	 * @param event Event that is to be checked if it conflicts with any of the events in this calendar
	 * @return true if it conflicts else false
	 */
	public boolean isConflicting(Event event){
		for(Event e: events){
			if((e.intersects(event) || event.intersects(e)) && !e.getID().equals(event.getID()))
				return true;
		}
		return false;
	}

	/**
	 * Adds a notification to this calendar notification list.
	 * @param message Notification message
	 * @param url URL of the notification if any. 
	 */
	public void notify(String message, String url){
		this.notifications.add(new Notification(message,url));
	}

	/**
	 * Returns the first notification in the notification list
	 * @return the first notification in the notification list else an empty notification
	 */
	public String getNotification() {
		if(this.notifications.size()>0)
			return this.notifications.get(0).toString();
		else
			return new Notification(".",".").toString();
	}
	
	/**
	 * Once client interface has consumed the notification it needs to tell the calendar that the event can now be removed.
	 */
	public void safeToRemoveNotification(){
		if(this.notifications.size()>0)
			this.notifications.remove(0);
	}
}
