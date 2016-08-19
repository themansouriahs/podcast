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
</body>
</html>