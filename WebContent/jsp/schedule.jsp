<%@page import="com.rest.calendar.*"%>
<%@page import="com.rest.resources.*"%>
<%@page import="com.rest.classes.*"%>
<%@page import="java.util.ArrayList"%>
<%@page import="java.util.Date"%>
<%@page import="java.util.GregorianCalendar"%>

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
			Date date = user.browsingDate;
			String viewerOfCalendar = (String) pageContext.findAttribute("it");
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
	<div id="contentContainer">
		<h2>Add an event</h2>
		<form name="scheduleEventForm" method="post" action="">
			<label for="description">Event:</label><input type="text" name="description" value="" placeholder="Assignment Due omg.."/>
			<p>Start Time and Date
				<select id="day" name="day" class="selectOption2">
					<%int displayDay = date.getDate();
						GregorianCalendar c = new GregorianCalendar();
						c.set(GregorianCalendar.MONTH,date.getMonth());
						int lastDay = c.getActualMaximum(GregorianCalendar.DAY_OF_MONTH);
						for(int i=1; i<lastDay;i++){
						  String sel = displayDay==i ? "selected":"";
						  out.println("<option value="+i+" "+sel+">"+i+"</option>");
					  }
					  %>
				</select>
				<select id="month" name="month" class="selectOption2">
					<%int displayMonth = date.getMonth();
					  String[] monthsInYear={"January","February","March","April","May","June","July","August","September","October","November","December"};
					  for(int i=0; i<monthsInYear.length;i++){
						  String sel = displayMonth==i ? "selected":"";
						  out.println("<option value="+i+" "+sel+">"+monthsInYear[i]+"</option>");
					  }
					  %>
				</select>
				<select id="year" name="year" class="selectOption2">
					<%int displayYear = date.getYear();
					  for(int i=2000; i<2050;i++){
						  String sel = (displayYear+1900)==i ? "selected":"";
						  out.println("<option value="+(i-1900)+" "+sel+">"+i+"</option>");
					  }
					  %>
				</select>
			<label for="begin">Time:</label><input type="text" name="begin" value="" placeholder="00:00"/>
			</p>
			
			<p>	End Time and Date: 
				<select name="dayEnd" class="selectOption2">
					<%  displayDay = date.getDate();
						c = new GregorianCalendar();
						c.set(GregorianCalendar.MONTH,date.getMonth());
						lastDay = c.getActualMaximum(GregorianCalendar.DAY_OF_MONTH);
						for(int i=1; i<lastDay;i++){
						  String sel = displayDay==i ? "selected":"";
						  out.println("<option value="+i+" "+sel+">"+i+"</option>");
					  }
					  %>
				</select>
				<select name="monthEnd" class="selectOption2">
					<%displayMonth = date.getMonth();
					  for(int i=0; i<monthsInYear.length;i++){
						  String sel = displayMonth==i ? "selected":"";
						  out.println("<option value="+i+" "+sel+">"+monthsInYear[i]+"</option>");
					  }
					  %>
				</select>
				<select name="yearEnd" class="selectOption2">
					<%displayYear = date.getYear();
					  for(int i=2000; i<2050;i++){
						  String sel = (displayYear+1900)==i ? "selected":"";
						  out.println("<option value="+(i-1900)+" "+sel+">"+i+"</option>");
					  }
					  %>
				</select>
				<label for="end">Time:</label><input type="text" name="end" value="" placeholder="23:59"/>
				</p>
				<p>
				<label for="access">Event Type:</label>
				<select name="access" class="selectOption2"  onchange="enableUsersList()">
					<option>PUBLIC</option>
					<option selected>PRIVATE</option>
					<option>GROUP</option>					
				</select>
				</p>
				<div id="memberList" style="display:none">
					<label for="members">Enter group members (seperated by comma)</label>
					<input type="text" value="<%=ownerOfCalendar%>" name="members" class="textbox"/>
					<label for="sleep">Allow debugging</label>
					<input type="text" value="false"  name="sleep" class="textbox"/>
					<br/>
				</div>
				<hr/>
				<input type="button" value="Schedule Event" class="button" onClick="scheduleEvent()"/>
		</form>
	</div>
	<div class="clear"></div>
	<hr/>
	<div style="display:none">
	<span id="viewerOfCalendar"><%=viewerOfCalendar%></span>
	<span id="ownerOfCalendar"><%=user.getUsername()%></span>
	
	</div>
	<div id="footer">(c)Copyright kb</div>
</div>
</body>
</html>
