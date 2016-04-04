<%@page import="com.rest.calendar.*"%>
<%@page import="com.rest.resources.*"%>
<%@page import="com.rest.classes.*"%>
<%@page import="java.util.ArrayList"%>
<%@page import="java.util.Collections"%>
<%@page import="java.util.Date"%>

<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
	<title>Distributed Calendar</title>
	<link href="/DistCalendar/css/calendar.css" rel="stylesheet" type="text/css">
	<%
			User user = (User) request.getSession().getAttribute("User");
			Object[] data = (Object[]) pageContext.findAttribute("it");
			ArrayList<Event> events = (ArrayList<Event>) data[1];
			Date date = user.browsingDate;
			String viewerOfCalendar = (String) data[0];
			String ownerOfCalendar = user.getUsername();
	%>
	<script src="/DistCalendar/js/distCalendar.js" type="text/javascript"></script>
	
</head>
<body>
<div id="mainContainer">
	<div id="header">
		<div id="headerImage">
		</div>
	</div>
	<h2>Welcome, <%=user.getUsername()%></h2>
	<h3>You are viewing <%=viewerOfCalendar%> calendar.</h3>
	<div id="contentContainer">
		<%
			Collections.sort(events, new DateComparator());
			String div = "<div class=\"timeFrame\">";
			String spanTime ="<span class=\"time\">";
			String eventClass = "<span  class=\"mainEvent\">";
			for(Event e: events){
				if(e.getAccessControl().equals(AccessControl.GROUP) && !Main.calendarManager.isMemberOfGroup(ownerOfCalendar,e.getID())){
					out.println(div+spanTime+e.getEventTimeRange()+"</span>"+eventClass+"Group Event(You are not a member of this)");
					out.println("</span></div>");
				}else if(user.getUsername().equals(viewerOfCalendar) || e.getAccessControl().equals(AccessControl.PUBLIC)
						|| Main.calendarManager.isMemberOfGroup(user.getUsername(), e.getID())){
					out.println(div+spanTime+e.getEventTimeRange()+"</span>"+eventClass+e.getDescription());				
					out.println("<span id='accessControl' style='display:none'>"+e.getAccessControl()+"</span>");
					out.println("<input type=\"button\" class=\"ebutton\" value=\"Delete/Edit Event\" onClick='editEvent(\""+e.getID()+"\")'/></span></div>");
				}
			}
		%>
	</div>
	<div id="scheduleEvent">
		<input type="button" class="button" style="margin:10px" value="Schedule Event" onClick="schedule()" />
	</div>
	<div class="clear"></div>
	<hr/>
	<div id="footer">(c)Copyright kb</div>
	<div style="display:none">
	<span id="viewerOfCalendar"><%=viewerOfCalendar%></span>
	<span id="ownerOfCalendar"><%=user.getUsername()%></span>
	<span id="year"><%=date.getYear()%></span>
	<span id="month"><%=date.getMonth()%></span>
	<span id="day"><%=date.getDate()%></span>
	<form name="eventForm" method="Post" action="">
	<input type="text" name="id"/>
	</form>
	</div>
</div>
</body>
</html>
