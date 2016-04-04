package com.rest.chord;

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

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import com.rest.calendar.Event;
import com.rest.resources.Main;

/**
 * Static helper class that is used for node to node communication
 * as well as login, signup, request.
 * @author kshitiz
 *
 */
public class WebCaller {
	
	public static int timeoutConnection = 3000;
	public static int timeoutSocket = 3000;
	public static String nodeToNodeCallPath="/DistCalendar/rest/admin/";
	
	/**
	 * Returns the string returned by a webcall.
	 * When a get call is done this method will read what is sent in respone.
	 */
	public static String readStringFromResponse(HttpEntity entity) throws IllegalStateException, IOException{
		String result = "";
		try{
			if (entity != null) {
			    InputStream instream = entity.getContent();
		    	StringWriter writer = new StringWriter();
		    	IOUtils.copy(instream, writer);
		    	result = writer.toString();
		    	instream.close();
			}
		}catch(Exception e){
			Main.log("Read data from web call failed", e.getMessage());
		}
		return result;
	}
	/**
	 * Each of the web call (node 2 node) requires a http client and this method will create a client with our
	 * socket timeout and connection timeout parameters.
	 * @return
	 */
	public static HttpClient setUpCaller(){
		HttpParams httpParameters = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
		HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);
		return new DefaultHttpClient(httpParameters);
	}
	/**
	 * Calls the node (GET request) to another node mentioned in the url and return the string obtained in the response
	 */
	public static String callNodeForData(String url) throws ClientProtocolException, IOException{
		HttpClient httpclient = setUpCaller();
		HttpGet request = new HttpGet(url);
		HttpResponse response = httpclient.execute(request);
		HttpEntity entity = response.getEntity();
		return readStringFromResponse(entity);
	}
	/**
	 * Node to Node communication similar to above function except this is only used to pass message
	 * around the ring.
	 */
	public static String callNodeForData(String nextNodeIP, String url) throws ClientProtocolException, IOException{
		HttpClient httpclient = setUpCaller();
		HttpGet request = new HttpGet("http://"+nextNodeIP+":8080"+nodeToNodeCallPath+url);
		HttpResponse response = httpclient.execute(request);
		HttpEntity entity = response.getEntity();
		return readStringFromResponse(entity);
	}
	
	/**
	 * Node to Node Communication 
	 */
	public static void callNode(String nextNodeIP, String url) throws ClientProtocolException, IOException{
		HttpClient httpclient = setUpCaller();
		String uri = "http://"+nextNodeIP+":8080"+nodeToNodeCallPath+url;
		HttpGet request = new HttpGet(uri);
		httpclient.execute(request);
	}
	
	/**
	 * Similar to above function but generic enough to allow any url.
	 */
	public static void callNode(String url) throws ClientProtocolException, IOException{
		HttpClient httpclient = setUpCaller();
		HttpGet request = new HttpGet(url);
		httpclient.execute(request);
	}
	/**
	 * Adds a file in the body of post request and sends the nextNode the file to either save
	 * in the primary or backup.
	 */
	public static void callNodeSendFile(String nextNodeIP, String url, String filePath) throws ClientProtocolException, IOException{
		HttpClient httpclient = setUpCaller();
	    HttpPost post = new HttpPost("http://"+nextNodeIP+":8080"+nodeToNodeCallPath+url);
	    FileBody file = new FileBody(new File(filePath));
	    MultipartEntity mime = new MultipartEntity();
	    mime.addPart("file",file);
	    post.setEntity((HttpEntity)mime);
	    HttpResponse response = httpclient.execute(post);
		HttpEntity entity = response.getEntity();
		Main.log("result of " + url + " call", readStringFromResponse(entity));
	}
	/**
	 * Call node to add/update/delete event
	 */
	public static String callNodeWithEvent(String url, Event event, String username) throws Exception{
		HttpClient httpclient = setUpCaller();
	    HttpPost post = new HttpPost(url);
	    List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
	    nameValuePairs.add(new BasicNameValuePair("username", username));
	    nameValuePairs.add(new BasicNameValuePair("event", event.getEntireEventAsString()));
	    post.setEntity(new UrlEncodedFormEntity(nameValuePairs));
	    HttpResponse response = httpclient.execute(post);
		HttpEntity entity = response.getEntity();
		return readStringFromResponse(entity);
	}
	/**
	 * Fetches a file from the node and saves on Primary
	 */
	public static void downloadFileFromNode(String username, String nextNodeIP) throws ClientProtocolException, IOException{
		HttpClient httpclient = setUpCaller();
		HttpGet request = new HttpGet("http://"+nextNodeIP+":8080"+nodeToNodeCallPath+"downloadFile?username="+username);
		HttpResponse response = httpclient.execute(request);
		HttpEntity entity = response.getEntity();
		if (entity != null) {
		    InputStream instream = entity.getContent();
	    	FileUtils.copyInputStreamToFile(instream, new File(Main.primary+username+".txt"));
	    	instream.close();
		}
	}

	/**
	 * Only used during signup to forward the signup request to correct node.
	 */
	public static void callNodeForSignup(String url, String username, String password) throws Exception {
		HttpClient httpclient = setUpCaller();
	    HttpPost post = new HttpPost(url);
	    List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
	    nameValuePairs.add(new BasicNameValuePair("username", username));
	    nameValuePairs.add(new BasicNameValuePair("password", password));
	    post.setEntity(new UrlEncodedFormEntity(nameValuePairs));
	    httpclient.execute(post);
	}
	
	//http://commons.apache.org/fileupload/using.html

	public static String saveFile(HttpServletRequest req, String destPath) throws Exception{ //destPath is directory
		String username = "";
		if (ServletFileUpload.isMultipartContent(req)){
            ServletFileUpload upload = new ServletFileUpload();
            FileItemIterator iter = (FileItemIterator) upload.getItemIterator(req);
            while (iter.hasNext()) {
                FileItemStream item = (FileItemStream) iter.next();
                username = item.getName();
                Main.log("extracting file and saving to " + destPath,"Processing file " + username);
                InputStream stream = item.openStream();
                writeToFile(stream,new File(destPath + username));
            }
		}
		return username;	
	}
	public static void writeToFile(InputStream is, File file) throws Exception {
        DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
        InputStream is2 = is;
        boolean again = true;
        while(again) {
     	   	   int i = is2.read();
                if(i > -1)
                        out.writeByte(i);
                else again = false;
        }
        is.close();
        out.close();
	}
		
}
