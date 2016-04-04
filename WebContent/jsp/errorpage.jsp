<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
	<title>Distributed Calendar</title>
	<link href="/DistCalendar/css/calendar.css" rel="stylesheet" type="text/css">
	<%
		String errMessage = (String) pageContext.findAttribute("it");
	%>
</head>
<body>
<div id="mainContainer">
	<div id="header">
		<div id="headerImage">
		</div>
	</div>
	<div id="contentContainer">
		<h1>Error Report</h1>
		<h2><%=errMessage%></h2>
		<div>
		The server returned with the following error <%=errMessage%>.
		Please click <span style="color:#006699" onClick="history.back()">back</span> on the browser or go to <a href="/DistCalendar/login.html">login page</a> to log back in.</div>
	</div>
	<div class="clear"></div>
	<hr/>
	<div id="footer">(c)Copyright kb</div>
</div>
</body>
</html>
