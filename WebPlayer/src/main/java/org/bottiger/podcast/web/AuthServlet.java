package org.bottiger.podcast.web;

/**
 * Created by aplb on 19-08-2016.
 */

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import static org.bottiger.podcast.web.QRModel.getQRUrl;

// [START example]
@SuppressWarnings("serial")
public class AuthServlet extends HttpServlet {

    public static final String POST_KEY = "4350967";
    public static final String AUTH_TOKEN = "token";
    public static final String AUTHENTICATED = "logged_in";

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession();

        if (isAuthenticated(session)) {
            PrintWriter out = resp.getWriter();
            out.println("OK");
            return;
        }

        resp.sendError(401, "Not Authenticated" );
    }

    // Process the http POST of the form
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        String postValue = req.getParameter(POST_KEY);
        String parsedPostValue = QRModel.parseQRValue(postValue);

        HttpSession session = req.getSession();
        String token = (String)session.getAttribute(AUTH_TOKEN);

        if (parsedPostValue.equals(token)) {
            session.setAttribute(AUTHENTICATED, Boolean.TRUE);
        }

        resp.sendRedirect("/guestbook.jsp?guestbookName=" + "test");
    }

    public static boolean isAuthenticated(HttpSession argSession) {
        return (argSession.getAttribute(AUTHENTICATED) == Boolean.TRUE);
    }

}
