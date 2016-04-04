package com.rest.resources;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import com.rest.calendar.Calendar;
import com.rest.calendar.Event;
import com.rest.chord.Node;
import com.rest.chord.WebCaller;
import com.rest.classes.User;
import com.rest.classes.Utils;
import com.sun.jersey.api.view.Viewable;

@Path("admin")
public class ChordService {
	
	@GET
	public Viewable getAdminPage(@Context HttpServletRequest request){
		String ip = request.getLocalAddr();
		if(Main.node == null || Main.node.getIp().equals("localhost")){
			Main.node = new Node(ip);
			Main.node.shutdownNotRequested = true;
			Main.log("ChordService",Main.node.toString());
			Main.node.start();
		}
		return new Viewable("/jsp/admin",Main.node);
	}
	
	@POST
	@Path("loadDatabase")
	public Viewable loadDatabase(@Context HttpServletRequest request){
		try {
			Main.loadPersistentCalendars();
			Main.node.setUsers(new ArrayList<String>(Main.calendarManager.users.keySet()));
		} catch (IOException e) {
			Main.log("load calendar objects failed", e.getMessage());
		}
		return getAdminPage(request);
	}
	@POST
	@Path("shutdown")
	public void graceFulShutdown(@Context HttpServletRequest request){
		Main.node.shutdownNotRequested = false;
		Main.node.interrupt();
		System.exit(0); //kills the tomcat server.
		//return getAdminPage(request);
	}
	
	@POST
	@Path("callAppropiateNode")   //asks the node what is the head and then asks the head to where to join
	public Viewable myJoin(@FormParam("nodeIP")String nodeIP,@Context HttpServletRequest request){
		try {
			HttpClient httpclient = new DefaultHttpClient();
			String ip = request.getLocalAddr();
			String headIP = getHead(nodeIP);
			Main.log("callAppropiateHead - request for head", headIP);
			HttpGet request2 = new HttpGet("http://"+headIP+":8080"+"/DistCalendar/rest/admin/join?nodeIP="+ip+"&visited=");
			httpclient.execute(request2);	
			Thread.sleep(6000);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return getAdminPage(request);
	}
	
	//nodeIP must be one of the valid nodes on the ring.
	public String getHead(String nodeIP) throws ClientProtocolException, IOException{
		String headIP = nodeIP;
		HttpClient httpclient = new DefaultHttpClient();
		HttpGet request1 = new HttpGet("http://"+nodeIP+":8080"+"/DistCalendar/rest/admin/findHead");
		HttpResponse response = httpclient.execute(request1);
		HttpEntity entity = response.getEntity();
		if (entity != null) {
		    InputStream instream = entity.getContent();
	    	StringWriter writer = new StringWriter();
	    	IOUtils.copy(instream, writer);
	    	headIP = writer.toString();
	    	instream.close();
		}
		return headIP;
	}
	
	
	@GET
	@Path("setTable")
	public String setTable(@QueryParam("left")String left,@QueryParam("right")String right){
		Main.log("ChordService",Main.node.toString());
		Main.node.setSuccessorIP(right);
		Main.node.setPredecessorIP(left);
		Main.log("ChordService",Main.node.toString());
		for(int i=0;i<5;i++){
			try{
				Main.node.probe();
				Thread.sleep(150);
			}catch(Exception e){
				Main.log("setTable:probe ERR",e.getMessage());
				return e.getMessage();
			}
		}
		try{
			Main.node.nodeBalance();
			Thread.sleep(1500);
			Main.node.initiateBackup();
			Thread.sleep(5000); //allow 3 second to copy file and stuff.
		}catch(Exception e){
			Main.log("node down during join", e.getMessage());
		}
		return "success";
	}
		
	@GET
	@Path("clearBackupList")
	public String clearBackupList(){
		@SuppressWarnings("unchecked")
		ArrayList<String> copyOfBackup = (ArrayList<String>) Main.node.backup.clone();
		for(String name: copyOfBackup){
			Main.node.backup.remove(name);
			FileUtils.deleteQuietly(new File(Main.backup + name + ".txt"));
		}
		return "true";
	}
	@GET
	@Path("startBackup")
	public String startBackup(){
		try {
			Main.node.addToSuccessorBackup();
		} catch (Exception e) {
			e.printStackTrace();
			return "false";
		}
		return "true";
	}
	
	@GET
	@Path("join")
	public String join(@QueryParam("nodeIP")String nodeIP,@QueryParam("visited")List<Integer> visited,@Context HttpServletRequest request){
		try {
			Main.log("ChordService",Main.node.toString());
			Main.node.joinRequest(nodeIP,visited);
			Main.log("ChordService",Main.node.toString());
		} catch (Exception e) {
			Main.log("Forward reqeust to successor for joining", Main.node.getSuccessorIP());
		}
		return "success";
	}
	
	@GET
	@Path("lookup")
	public String lookup(@QueryParam("username")String username,@QueryParam("visited")List<Integer> visited){
		try {
			return Main.node.lookup(username, visited);
		} catch (Exception e) {
			e.printStackTrace();
			return Main.node.getIp();
		}
	}
	
	//non - idempotent get method need to make it post	
	@GET
	@Path("setSuccessor")
	public String setSuccessor(@QueryParam("successorIP")String nodeIP,@QueryParam("sosIP")String sosIP){
		try {
			Main.node.setSuccessorIP(nodeIP);
			Main.node.setSosIP(sosIP);
			return "true";
		} catch (Exception e) {
			Main.log("Chord service, setSuccessor",e.getMessage());
			return "false";
		}
	}
		
	//non - idempotent get method need to make it post
	@GET
	@Path("setPredecessor")
	public String setPredecessor(@QueryParam("predecessorIP")String nodeIP,@QueryParam("popIP")String popIP){
		try {
			Main.node.setPredecessorIP(nodeIP);
			Main.node.setPopIP(popIP);
			return "true";
		} catch (Exception e) {
			Main.log("Chord service, setPredecessor",e.getMessage());
			return "false";
		}
	}
	
	@GET
	@Path("getHigherKeys")
	public String getHigherKeys(@QueryParam("ip")String ip){
		return Main.node.getHigherKeys(ip);
	}
	@GET
	@Path("getLowerKeys")
	public String getLowerKeys(@QueryParam("ip")String ip){
		return Main.node.getLowerKeys(ip);
	}
	
	
	@GET
	@Path("findHead")
	public String findHead(){
		try {
			return Main.node.findHead();
		}catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@GET 
	@Path("downloadFile") //download file then delete it.
	public void downloadFile(@QueryParam("username") String username,@Context HttpServletResponse response){
		try {
			response.setContentType("application/octet-stream");
	        response.addHeader("Content-Disposition", "attachment; filename=" + username + ".txt");
	        File file = new File(Main.primary+username+".txt");
	        response.setContentLength((int)(file.length()));
	        FileUtils.copyFile(file,response.getOutputStream());
	        Thread.sleep(2000);
	        Main.node.removeUserFromCalendarManager(username);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//writes output to servlet output stream
	}
	@POST
	@Path("addToBackup")
	public String addToBackup(@Context HttpServletRequest request){
		String username;
		try {
			username = WebCaller.saveFile(request,Main.backup);
			username = username.substring(0, username.lastIndexOf('.'));
			if(!Main.node.backup.contains(username))
				Main.node.backup.add(username);
			Main.log("Adding files to backup ", username);
		} catch (Exception e) {
			Main.log("error during adding to backup ",e.getMessage());
		}
		return "true";
	}
	
	@POST
	@Path("addToPrimary")
	public String addToPrimary(@Context HttpServletRequest request){
		String username;
		try {
			username = WebCaller.saveFile(request,Main.primary);
			username = username.substring(0, username.lastIndexOf('.'));
			if(!Main.node.myUsers.contains(username))
				Main.node.myUsers.add(username);
			if(!Main.calendarManager.users.containsKey(username)){
				Main.calendarManager.users.put(username, new User(username,""));
				Calendar calender = new Calendar();
				ArrayList<Event> events = Utils.readFileFromPrimary(username);
				calender.setEvents(events);
				Main.calendarManager.calendars.put(username,calender);
			}
			Main.log("Adding files to primary ", username);
		} catch (Exception e) {
			Main.log("error during adding to primary ",e.getMessage());
		}
		return "true";
	}
	
	@GET
	@Path("removeFromBackup")
	public String removeFromBackup(@QueryParam("username")String username){
		Main.node.backup.remove(username);
		FileUtils.deleteQuietly(new File(Main.backup + username + ".txt"));
		return "true";
	}
	@GET
	@Path("getSuccessor")
	public String getSuccessor(){
		return Main.node.getSuccessorIP();
	}
	
	@GET
	@Path("getPredecessor")
	public String getPredecessor(){
		return Main.node.getPredecessorIP();
	}

	@GET
	@Path("fetchBackup")
	public String fetchBackup(){
		try{
			Main.node.addToSuccessorBackup();
			return "true";
		}catch(Exception e){
			return "false";
		}
	}
	
	
}
