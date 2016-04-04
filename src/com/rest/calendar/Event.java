package com.rest.calendar;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;


import com.rest.classes.AccessControl;

/**
 * Event describes a calendar event that has a time range along with a description and access control
 * @author Kshitiz
 *
 */
public class Event {

	private Date begin=null;
	private Date end=null;
	private String description;
	private AccessControl accessControl;
	private UUID id;
	public boolean userNotified = false;
	
	public Event(Date begin, Date end, String description, AccessControl accessControl) {
		this.begin = begin;
		this.end = end;
		this.description = description;
		this.accessControl = accessControl;
		this.id = UUID.randomUUID();
		isValid();
	}
	public Event(String beginStr, String endStr, String description, AccessControl accessControl){
		try{
			begin = DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).parse(beginStr);
			end = DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).parse(endStr);
			this.description = description;
			this.accessControl = accessControl;
			this.id = UUID.randomUUID();
			isValid();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	/**
	 * Create event object from the given String, this is used at the beginning to create event objects from a file.
	 * @param s
	 */
	public Event(String s) {
		String[] data = s.split("<nextField>");
		try{
			begin = DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).parse(data[0]);
			end = DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).parse(data[1]);
			this.description = data[2];
			this.accessControl = Enum.valueOf(AccessControl.class, data[3]);
			this.id = UUID.fromString(data[4]);
			isValid();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	/**
	 * Returns an event as string that is used to writeback in the file.
	 * @return
	 */
	public String getEntireEventAsString(){
		SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yy hh:mm a");
		String result = formatter.format(begin) + "<nextField>";
		result+= formatter.format(end) + "<nextField>";
		result+= description +"<nextField>" + this.accessControl +"<nextField>" + this.id + "<nextField><nextEvent>";
		return result;
	}
	/**
	 * Make sure the given event time range makes is correct
	 * @return true if the begin time is < than end time.
	 */
	public boolean isValid(){
		return begin.before(end);
	}

	/**
	 * Return true iff this event contains the given event
	 * @param e Event that need to be checked for containment
	 * @return true iff this event contains the given event
	 */
	public boolean contains(Event e){
		return (begin.before(e.begin) && end.after(e.end));
	}
	
	/**
	 * Return true iff if the given event intersects with this event
	 * @param e Event that need to be checked for intersection
	 * @return true iff if the given event intersects with this event
	 */
	public boolean intersects(Event e){
		boolean beginBefore = begin.before(e.begin) && end.before(e.begin);
		boolean endAfter = begin.after(e.end) && end.after(e.end);
		return !(beginBefore || endAfter);		
	}

	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public AccessControl getAccessControl() {
		return accessControl;
	}
	public void setAccessControl(AccessControl accessControl) {
		this.accessControl = accessControl;
	}
	public boolean isWeekend(){
		String day = this.getDayOfaWeek();
		return day.equals("Saturday") || day.equals("Sunday");
	}
	@Override
	public String toString(){
		SimpleDateFormat formatter = new SimpleDateFormat("hh:mm");
		return this.description +"["+ formatter.format(this.begin) +" - "+ formatter.format(end)+ "]" + " ["+this.accessControl+"]";
	}
	public Date getEnd() {
		return end;
	}
	public void setEnd(Date end) {
		this.end = end;
	}
	public void setBegin(Date begin) {
		this.begin = begin;
	}
	
	/**
	 * Return time range in a familiar format for easy reading for ex 2:00-3:00pm
	 * @return event range in easy readable format
	 */
	public String getEventTimeRange(){
		String beginTime = new SimpleDateFormat("hh:mm").format(begin);
		String endTime = new SimpleDateFormat("hh:mm a").format(end);
		return beginTime +" - "+endTime;
	}
	public Date getBegin(){
		return begin;
	}
	public String getBeginAsString() {
		return new SimpleDateFormat("hh:mm").format(begin);
	}
	public String getEndAsString(){
		return new SimpleDateFormat("hh:mm").format(end);
	}
	
	public String getDayOfaWeek(){
		//TODO: remove most probably not used
		SimpleDateFormat formatter = new SimpleDateFormat("EEEE");
		String text = formatter.format(begin);
		return text;
	}
	public UUID getID(){
		return id;
	}
	public void setID(UUID id){
		this.id = id;
	}
	
	public static void main(String[] args){
		try {
			String path = new java.io.File(".").getCanonicalPath().replace("\\", "/");
			System.out.println(path);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Returns true if this event is 15 or less minutes from current time
	 * @return true
	 */
	@SuppressWarnings("deprecation")
	public boolean isApproaching() {
		Date fewMinutesBefore = new Date();	
		boolean sameDate = (begin.getYear()==fewMinutesBefore.getYear())&&(begin.getMonth()==fewMinutesBefore.getMonth())
							&&(begin.getDate()==fewMinutesBefore.getDate());
		int range = begin.getMinutes() - fewMinutesBefore.getMinutes(); 
		if(range <= 15 && range >= 0 && sameDate)
			return true;
		else
			return false;
	}
}
