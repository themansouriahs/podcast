<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="org.bottiger.podcast.web.QRModel" %>
<html>
<head>
<link href="https://fonts.googleapis.com/css?family=Roboto" rel="stylesheet">
<script type="text/javascript" src="/js/auth_checker.js"></script>
    <style>
      body {
        font-family: 'Roboto', sans-serif;
        font-size: 28px;
      }
    </style>

</head>
<body style="margin: 0px;">
<center>
<h1 style="margin-top: 20px;">Use SoundWaves to scan the QR code</h1>
<div style="background: #cc0000; padding: 40px;">
<img src="<%= QRModel.getQRUrl(session) %>" style="border-radius: 25px;" />
</div>
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

      <script src="https://www.gstatic.com/firebasejs/3.3.0/firebase.js"></script>
      <script>
        // Initialize Firebase
        var config = {
          apiKey: "AIzaSyDZvX6mPV2Bpa4-W-AJILKi_bmPPmIwGEA",
          authDomain: "soundwaves-bottiger.firebaseapp.com",
          databaseURL: "https://soundwaves-bottiger.firebaseio.com",
          storageBucket: "soundwaves-bottiger.appspot.com",
        };
        firebase.initializeApp(config);
      </script>
</body>
</html>