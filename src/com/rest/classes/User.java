package com.rest.classes;

import java.io.IOException;
import java.util.Date;

import com.rest.chord.DistributedLock;

/**
 * User class holds username, password, and also what date was the user browsing last time.
 * Secondly, it provides interfaces to login
 */
public class User {
	private String username;
	private String password;
	public Date browsingDate;
	private boolean valid;
	public DistributedLock selfLock = new DistributedLock();//one lock for each user calendar objects.
	public String calendarBackup = "";
	
	public User(String username, String password){
		this.username = username;
		this.password = password;
		this.browsingDate = new Date();
		valid = false;
	}
	public User(boolean valid){
		this.valid = valid;
		this.username = "test";
		this.password = "user";
		this.browsingDate = new Date();
	}
	
	public void login() throws IOException{
		valid = Utils.isValidUser(username, password);
	}
	public boolean isLoggedIn(){
		return valid;
	}
	
	public String getUsername(){
		return username;
	}
	public void set(User user){
		this.username = user.username;
		this.password = user.password;
		this.valid = user.valid;
	}
}
