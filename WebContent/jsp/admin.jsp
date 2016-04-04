<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@page import="com.rest.chord.*"%>
<%@page import="com.rest.resources.Main"%>
<!DOCTYPE html>
<html>
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
	<title>Distributed Calendar</title>
	<link href="/DistCalendar/css/calendar.css" rel="stylesheet" type="text/css">
	<script src="/DistCalendar/js/distCalendar.js" type="text/javascript"></script>
	<%
		Node thisNode = (Node) pageContext.findAttribute("it");
		String file = "http://"+thisNode.getIp()+":8080/DistCalendar/log.txt";
	%>
</head>
<body>
<div id="mainContainer">
	<div id="header">
		<div id="headerImage">
		</div>
	</div>
	<div id="contentContainer">
		<h1>Administrator Page</h1>
		<hr/>
		<p><a href="/DistCalendar/rest/admin">Refresh</a> . <a href="<%=file%>">View Logs</a> . <a href="/DistCalendar">Login</a></p>
		<div id="join">
			<h2>Start/Stop Node</h2>
			<form name="joinNodes" method="POST" action="/DistCalendar/rest/admin/callAppropiateNode">
				<label for="nodeIP">Enter one of the ip (use the head if possible)</label>
				<input name="nodeIP" type="text" placeholder="192.168.1.8" class="textBox"/><br/>
				<div id="joinForm">
				<input type="submit" class="button" style="display:inline-block" value="Join Chord"/>
				</div>
			</form>
			<div id="shutdownForm">
				<form name="shutdown" method="POST" action="/DistCalendar/rest/admin/shutdown">
				<input type="submit" class="button" style="display:inline-block" value="Shutdown Node"/>
				</form>
			</div>
			<div id="loadDBForm">
				<form name="loadDB" method="POST" action="/DistCalendar/rest/admin/loadDatabase">
				<input type="submit" class="button" style="display:inline-block" value="Load Database"/>
				</form>
			</div>
			<div style="clear:left"></div>
		</div>
		<hr/>
		<div id="nodeInforation">
		<h2>Information about this node</h2>
		Node ID/IP : <%=thisNode.getNodeID()%>/<%=thisNode.getIp()%><br/>
		Successor ID/IP: <%=thisNode.getHash(thisNode.getSuccessorIP())%>/<%=thisNode.getSuccessorIP()%><br/>
		Predecessor ID/IP: <%=thisNode.getHash(thisNode.getPredecessorIP())%>/<%=thisNode.getPredecessorIP()%><br/>
		SOS / POP : <%=thisNode.getSOSIP()%>/<%=thisNode.getPOPIP()%><br/>
		<table>
		<col width="175">
		<col width="175">
		<thead>
		<tr>
		<td><h3>Primary Keys::</h3></td>
		<td><h3>Backup Keys::</h3></td>
		</tr>
		</thead>
		<tbody>
		<tr>
		<td>
			<ul><%
				for(String username: thisNode.getMyUsers()){
					out.println("<li>"+username+" / hash="+ thisNode.getHash(username)+"</li>");
				}
			%>
			</ul>
		</td>
		<td>
			<ul><%
				for(String username: thisNode.getBackup()){
					out.println("<li>"+username+" / hash="+ thisNode.getHash(username)+"</li>");
				}
			%>
			</ul>
		</td>
		</tr>
		</tbody>
		</table>
		</div>
		<hr/>
	</div>
	<div class="clear"></div>
	<hr/>
	<div id="footer">(c)Copyright kb</div>
</div>
</body>
</html>