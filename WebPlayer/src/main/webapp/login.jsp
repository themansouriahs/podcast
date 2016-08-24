<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="org.bottiger.podcast.web.QRModel" %>
<html>
<head>
<script type="text/javascript" src="/js/auth_checker.js"></script>
</head>
<body>
<center>
<h1>Use SoundWaves to scan the QR code</h1>
<img src="<%= QRModel.getQRUrl(session) %>" />
</center>
<%
String cookieName = "username";
Cookie cookies [] = request.getCookies ();
Cookie myCookie = null;
if (cookies != null){
  for (int i = 0; i < cookies.length; i++) {
    if (cookies [i].getName().equals (cookieName)){
      myCookie = cookies[i];
      break;
    }
  }
}
%>
<%
   Cookie cookie = null;
   Cookie[] cookies2 = null;
   // Get an array of Cookies associated with this domain
   cookies2 = request.getCookies();
   if( cookies2 != null ){
      //out.println("<h2> Found Cookies Name and Value</h2>");
      for (int i = 0; i < cookies2.length; i++){
         cookie = cookies[i];
         //out.print("Name : " + cookie.getName( ) + ",  ");
         //out.print("Value: " + cookie.getValue( )+" <br/>");
      }
  }else{
      //out.println("<h2>No cookies founds</h2>");
  }
%>

</body>
</html>