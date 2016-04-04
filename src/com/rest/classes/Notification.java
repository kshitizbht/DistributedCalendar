package com.rest.classes;

/**
 * Notification for clients when an event is either scheduled, updated or deleted.
 * @author Kshitiz
 *
 */
public class Notification {

	private String message;
	private String url;

	public Notification(String message, String url){
		this.message = message;
		this.url = url;
	}
	
	/**
	 * The string is in json format which can be easily parsed by javascript
	 */
	@Override
	public String toString(){
		return "{'notification':{'message':'"+message+"','url':'"+url+"'}}";
	}
}
