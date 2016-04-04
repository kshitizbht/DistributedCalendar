//uri used to fetch nofitication modify to the server ip address instead of localhost
var uri = "/DistCalendar/rest/calendar/getNotification/";
var uri2Delete = "/DistCalendar/rest/calendar/removeNotification/";

/**
 * Called when client wants to browse different date or user
 */
function updateDate(){
	var month = document.getElementById("month").value;
	var year = document.getElementById("year").value;
	var viewerOfCalendar = document.getElementById("CalendarOwner").value;
	var url = "/DistCalendar/rest/calendar/getMyCaldendar/"+viewerOfCalendar+"/"+year+"/"+month;
	var form = document.getDayForm;
	form.action = url;
	form.submit();
}

/**
 * Fetches a new page from server that has all the events for the given day
 * @param day
 */
function getEventsForday(day){
	var month = document.getElementById("month").value;
	var year = document.getElementById("year").value;
	var viewerOfCalendar = document.getElementById("viewerOfCalendar").innerHTML;
	var url = "/DistCalendar/rest/calendar/getEventsForDay/"+viewerOfCalendar+"/"+year+"/"+month+"/"+day;
	var form = document.getDayForm;
	form.action = url;
	form.submit();
}
/**
 * Logs the current user out.
 */
function logOut(){
	var url = "/DistCalendar/rest/login/logOut";
	var form = document.createElement("form");
	form.setAttribute("action",url);
	form.setAttribute("method","post");
	form.submit();
}
/**
 * Fetches the schedule page from server
 */
function schedule(){
	var url = "/DistCalendar/rest/calendar/schedule";
	var form = document.eventForm;
	form.action = url;
	form.submit();
}
/**
 * Fetches the edit event page from server for the given event
 * @param id the event that is going to be edited
 */
function editEvent(id){
	var viewerOfCalendar = document.getElementById("viewerOfCalendar").innerHTML;
	var ownerOfCalendar = document.getElementById("ownerOfCalendar").innerHTML;
	var access = document.getElementById("accessControl").innerHTML;
	if(viewerOfCalendar == ownerOfCalendar || access=="GROUP"){
		var url = "/DistCalendar/rest/calendar/editEvent/"+viewerOfCalendar;
		var form = document.eventForm;
		form.action = url;
		form.id.value = id;
		form.submit();
	}else{
		alert("You do not have enough rights to change this event");
	}
}

/**
 * Asks server to schedule an event specified by the form parameters
 */
function scheduleEvent(){
	var month = document.getElementById("month").value;
	var year = document.getElementById("year").value;
	var day = document.getElementById("day").value;
	var viewerOfCalendar = document.getElementById("viewerOfCalendar").innerHTML;
	var ownerOfCalendar = document.getElementById("ownerOfCalendar").innerHTML;
	var url = "/DistCalendar/rest/calendar/scheduleEvent/"+viewerOfCalendar+"/"+year+"/"+month+"/"+day;	
	var form = document.scheduleEventForm;
	var accessControl = form.access.value;
	var timeBegin = document.scheduleEventForm.begin.value;
	var timeEnd = document.scheduleEventForm.end.value;
	if(viewerOfCalendar != ownerOfCalendar && accessControl == "Private"){
		alert("You can not schedule Private events on someone's else calendar");
	}
	if(formValidator(timeBegin,timeEnd)){
		form.action = url;
		form.submit();
	}
}
/**
 * Asks server to update an existing event with given form parameters
 */
function updateEvent(){
	var month = document.getElementById("month").value;
	var year = document.getElementById("year").value;
	var day = document.getElementById("day").value;
	var viewerOfCalendar = document.getElementById("viewerOfCalendar").innerHTML;
	var ownerOfCalendar = document.getElementById("ownerOfCalendar").innerHTML;
	var url = "/DistCalendar/rest/calendar/updateEvent/"+viewerOfCalendar+"/"+year+"/"+month+"/"+day;	
	var form = document.scheduleEventForm;
	var accessControl = form.access.value;
	var timeBegin = document.scheduleEventForm.begin.value;
	var timeEnd = document.scheduleEventForm.end.value;
	if(viewerOfCalendar != ownerOfCalendar && accessControl == "Private"){
		alert("You can not schedule Private events on someone's else calendar");
	}
	if(formValidator(timeBegin,timeEnd)){
		form.action = url;
		form.submit();
	}
}
/**
 * Asks server to delete the event as long as this user has the right to do so
 * @param id the event id to be removed
 */
function deleteEvent(id){
	var year=document.getElementById("year1").innerHTML;
	var month=document.getElementById("month1").innerHTML;
	var day=document.getElementById("day1").innerHTML;
	var viewerOfCalendar = document.getElementById("viewerOfCalendar").innerHTML;
	var ownerOfCalendar = document.getElementById("ownerOfCalendar").innerHTML;
	if(viewerOfCalendar == ownerOfCalendar){
		var url = "/DistCalendar/rest/calendar/delete/"+viewerOfCalendar+"/"+year+"/"+month+"/"+day;
		var form = document.eventForm;
		form.action = url;
		form.id.value = id;
		form.submit();
	}else{
		alert("You do not have enough rights to change this event Future implementation need to handle allow of deleting of group events as well");
	}
}

/**
 * Used to display list of users for group events
 */
function enableUsersList(){
	var form = document.scheduleEventForm;
	var accessControl = form.access.value;
	if(accessControl == 'GROUP'){
		document.getElementById("memberList").style.display = "block";
	}else{
		document.getElementById("memberList").style.display = "none";		
	}
}

/**
 * Called by the client to poll new notification from the server every 3 seconds.
 */
function startTimer(){
	uri = "http://" + window.location.host + uri;
	uri2Delete = "http://"+ window.location.host+uri2Delete;
	window.setInterval("update(\""+uri+"\")", 3000);
	
}

/**
 * Makes an asyn request to the server to poll for notifications
 * @param uri server notification url
 */
function update(uri){
	var username = document.getElementById("ownerOfCalendar").innerHTML;
	var url = uri+username;
	var asyncRequest;
	try{
		asyncRequest = new XMLHttpRequest();
		asyncRequest.onreadystatechange = function(){parseResult(asyncRequest);};
		asyncRequest.open('GET',url);
		asyncRequest.setRequestHeader("Accept","application/json; charset=utf-8" );
		asyncRequest.send(null);
	}catch (exception){//if the server fails forward the request to its successor prior to failure.
		alert(exception);
	}
}
/**
 * Upon receiving result from server parse the result
 * @param asyncRequest the asyn notification poll request
 */
function parseResult(asyncRequest){
	try{
		if ( asyncRequest.readyState == 4 && asyncRequest.status == 200){
			var result = eval('('+asyncRequest.responseText+')');
			displayResult(result);
	    }else if(asyncRequest.status == 0){
	    	var millisecondsToWait = 12000;
	    	setTimeout(function() {
	    		var form = document.serverFailed;
				var successor = document.getElementById("successorIP").innerHTML;
				form.action="http://"+successor+":8080/DistCalendar/rest/login";
				form.submit();
	    	}, millisecondsToWait);
	    	
	    }                                                        
	}catch(exception){
		//ignore exception
	}
}
/**
 * Display the notification to client and ask server to remove this notification form future
 * @param data the notification result from server
 */
function displayResult(data){
	var notification = data.notification.message;
	if(notification != '.'){
		document.getElementById('notification').innerHTML = notification;		
		document.getElementById("notification").style.display = "block";
		update(uri2Delete);
	}/*else{
		document.getElementById("notification").style.display = "none";
	}*/
}

/**
 * Checks to make sure the data field are valid on schedule and edit event pages
 * @returns {Boolean} true if the date are valid false otherwise
 */
function checkDate(){
	if(document.scheduleEventForm.day.value !=  document.scheduleEventForm.dayEnd.value)
		return false;
	else if(document.scheduleEventForm.month.value !=  document.scheduleEventForm.monthEnd.value)
		return false;
	else if(document.scheduleEventForm.year.value !=  document.scheduleEventForm.yearEnd.value)
		return false;
	else
		return true;
}
/**
 * A generic string validity checker
 * @param item the string that needs to be checked
 * @param regularExprs the regular expr that the item to match
 * @returns {Boolean} true if the item follows the pattern
 */
function validRegularExpression(item, regularExprs){
	for(var i=0;i<item.length;i++){
		var c = item.charAt(i);
		if (!c.match(regularExprs)){
			return false;
		}
	}
	return true;
}

/**
 * Make sure items are non null
 * @param item item 
 * @returns {Boolean} true if item is not null
 */
function isValid(item){
	if (item == null || item =="")
		return false;
	else
		return true;
}
/**
 * Checks if the user entered time is in correct range and correct format
 * @param begin the begin time
 * @param end the end time
 * @returns {Boolean} true iff the begin and end time follows XX:XX where X belongs to Integer
 */
function isValidRange(begin,end){
	var beginHr = parseInt(begin.split(':')[0]);
	var beginMin = parseInt(begin.split(':')[1]);
	var endHr = parseInt(end.split(':')[0]);
	var endMin = parseInt(end.split(':')[1]);
	if((beginHr<0 || beginHr>23)||(endHr<0 || endHr>23))
		return false;
	else if((endMin<0 || endMin >59 )||(beginMin<0 || beginMin > 59))
		return false;
	if(beginHr < endHr)
		return true;
	else if(beginHr == endHr && beginMin < endMin)
		return true;
	else 
		return false;
	
}
/**
 * Validates user input times
 * @returns {Boolean} true iff user entered timerange meets all the criteria false otherwise
 */
function formValidator(timeBegin,timeEnd){
	var errorMessage = "";
	if (isValid(timeBegin) && isValid(timeEnd)){
		var time = /^[0-9:]$/;
		if (!validRegularExpression(timeBegin,time) || !validRegularExpression(timeEnd,time)){
			errorMessage += "Time contains invalid character\n";
		}
		else if(!isValidRange(timeBegin,timeEnd)) {
			errorMessage +=" Invalid time proviced either incorrect time format or begin was > end";
		}else if(!checkDate()){
			errorMessage +="Multiple day event is not allowed yet.";
		}
	}else{
		errorMessage += " Time range can not be empty\n";
	}	
	if (errorMessage != ""){
		alert(errorMessage);
		return false;
	}else{
		return true;
	}
}
