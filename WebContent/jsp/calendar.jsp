<%@page import="com.rest.calendar.*"%>
<%@page import="com.rest.resources.*"%>
<%@page import="com.rest.classes.*"%>
<%@page import="java.util.ArrayList"%>
<%@page import="java.util.Date"%>
<%@page import="java.text.SimpleDateFormat"%>
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
			Date thisMonth = user.browsingDate;
			String viewerOfCalendar = (String) pageContext.findAttribute("it");
			String ownerOfCalendar = user.getUsername();
			Calendar calendar = (Calendar) request.getSession().getAttribute("Calendar");//Main.calendarManager.getCalendar(viewerOfCalendar);
	%>
	<script src="/DistCalendar/js/distCalendar.js" type="text/javascript"></script>
</head>
<body onload="startTimer()">
<div id="mainContainer">
	<div id="header">
		<div id="headerImage">
		</div>
	</div>
	<div id="contentContainer">
		<span id="logout" onClick="logOut()">Log out</span>
		<span id="notification">Notification Box</span>
		<h2>Welcome <%=user.getUsername()%></h2>
		<hr/>
		<%
		ArrayList<Event> eventsForMonth = calendar.getEventsForAMonth(thisMonth);
		String dayOfTheWeek = new SimpleDateFormat("EEEE").format(thisMonth);
		
		GregorianCalendar c = new GregorianCalendar();
		c.set(GregorianCalendar.MONTH,thisMonth.getMonth());
		int lastDay = c.getActualMaximum(GregorianCalendar.DAY_OF_MONTH);
		%>
		<div id="dateSelection">
			<label for="month">Month</label>
			<select id="month" class="selectOption">
				<%int displayMonth = thisMonth.getMonth();
				  String[] monthsInYear={"January","February","March","April","May","June","July","August","September","October","November","December"};
				  for(int i=0; i<monthsInYear.length;i++){
					  String sel = displayMonth==i ? "selected":"";
					  out.println("<option value="+i+" "+sel+">"+monthsInYear[i]+"</option>");
				  }
				  %>
			</select>
			
			<label for="year">Year</label>
			<select id="year" class="selectOption">
				<%int displayYear = thisMonth.getYear();
				  for(int i=2000; i<2050;i++){
					  String sel = (displayYear+1900)==i ? "selected":"";
					  out.println("<option value="+(i-1900)+" "+sel+">"+i+"</option>");
				  }
				  %>
			</select>
			 <label for="CalendarOwner">View other calendar:</label>
			<%-- <select id="CalendarOwner" class="selectOption">
				<% for(String username: Main.calendarManager.users.keySet()){
					String sel= viewerOfCalendar.equals(username) ? "selected":"";
					out.println("<option value=\""+username+"\" "+sel+">"+username+"</option>");
				}
				%>
			</select> --%>
			<input type="text" value="<%=viewerOfCalendar%>" id="CalendarOwner" class="textbox"/>
			<input type="button" class="button" value="Update Display" onClick="updateDate()"/>			
		</div>
		<hr/>
		<div class="weekHead">
				<% String[] daysOfAWeek = {"Sunday","Monday","Tuesday","Wednesday","Thursday","Friday","Saturday"};
				   int firstDay = 0;
				   for(int i=0;i<7;i++){
					   if(dayOfTheWeek.equals(daysOfAWeek[i]))
						   firstDay = i;
				   }
				   for(int i=0; i<7;i++){
					   out.println(	"<span class=\"days\">"+daysOfAWeek[firstDay]+"</span>");
					   firstDay++;
					   firstDay%=7;
				   }
				%>
		</div>
		<%
		String nl ="\r\n";
		String onClick = "onClick=\"getEventsForday(this.id)\"";
		String result = "";
		String weekly="";
		String events="";
		String eventType="";
		int day = 1;
		for(int i = 1; i <= 5; i++){
			weekly = "<div class=\"week\">" + nl;
			String dayClass = "day";
			for(int j=1; j<=7; j++){
				if(day<lastDay){
					Date dayI = new Date(thisMonth.getYear(),thisMonth.getMonth(),day,0,0);
					if(dayI.getDay() == 6 || dayI.getDay() == 0)
						dayClass = "dayInactive";
					else
						dayClass = "day";
					events="";
					ArrayList<Event> eventsForDay = calendar.getEventsForaDay(dayI);
					for(Event event: eventsForDay){
						if(event.getAccessControl().equals(AccessControl.PUBLIC))
							eventType="eventPublic";
						else if(event.getAccessControl().equals(AccessControl.PRIVATE))
							eventType="eventPrivate";
						else if(event.getAccessControl().equals(AccessControl.GROUP))
							eventType="eventGroup";
						else
							eventType="eventOpen";
						
						if(viewerOfCalendar.equals(ownerOfCalendar))
							events+= "<small class=\""+eventType+"\">"+event.getDescription()+"</small>" + nl;
						else if(eventType.equals("eventPublic"))
							events+= "<small class=\""+eventType+"\">"+event.getDescription()+"</small>" + nl;							
						else if(eventType.equals("eventGroup")){
							if(!Main.calendarManager.isMemberOfGroup(user.getUsername(), event.getID()))
								events+= "<small class=\""+eventType+"\">GroupEvent</small>" + nl;
							else
								events+= "<small class=\""+eventType+"\">"+event.getDescription()+"</small>" + nl;
						}	
					}
					weekly+="<span id=\""+day+"\" class=\""+dayClass+"\""+ onClick+ ">"+day+events+"</span>"+nl;
				}else{
					weekly+="<span id=\""+day+"\" class=\"dayInvalid\""+ onClick+ ">"+day+"</span>"+nl;
				}
				day++;	
			}
			result += weekly+"</div>";	

		}
		out.println(result);
		%>
	</div> 
	<div class="clear"></div>
	<hr/>
	<div id="footer">(c)Copyright kb</div>
	<div style="display:none">
	<span id="viewerOfCalendar"><%=viewerOfCalendar%></span>
	<span id="ownerOfCalendar"><%=user.getUsername()%></span>
	<form name="getDayForm" method="Get" action="">
	<input type="text" name="day" value="1"/>
	</form>
	<span id="successorIP"><%=Main.node.getSuccessorIP()%></span>
	<form name="serverFailed" method="get" action="">
	<input type="text" name="username" value="<%=user.getUsername()%>"/>
	</form>
	</div>
</div>
</body>
</html>
