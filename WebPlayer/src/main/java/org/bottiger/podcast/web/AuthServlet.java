package org.bottiger.podcast.web;

/**
 * Created by aplb on 19-08-2016.
 */

import java.io.IOException;
import java.io.PrintWriter;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;


// [START example]
@SuppressWarnings("serial")
public class AuthServlet extends HttpServlet {

    public static final String POST_KEY = "4350967";
    public static final String AUTH_TOKEN = "token";
    public static final String AUTHENTICATED = "logged_in";

    public static final String EPISODE_URL = "episode_url";
    public static final String EPISODE_cover = "episode_cover";
    public static final String EPISODE_TITLE = "episode_title";
    public static final String EPISODE_SUBTITLE = "episode_subtitle";
    public static final String EPISODE_WEBSITE = "episode_website";
    public static final String EPISODE_DESCRIPTION = "episode_description";
    public static final String EPISODE_POSITION = "episode_position";

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession();

        if (isAuthenticated(session)) {
            PrintWriter out = resp.getWriter();
            //out.println("OK");
            long position = EpisodeModel.getPosition(session);
            //out.print("--" + session.getAttribute(EPISODE_POSITION) + "--");
            out.print(position);
            return;
        }

        resp.sendError(401, "Not Authenticated: " + isAuthenticated(session) + " sessionatuh: " + session.getAttribute(AUTHENTICATED) + " sessionid: " + session.getId());
    }

    // Process the http POST of the form
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        String postValue = req.getParameter(POST_KEY);
        //String parsedPostValue = QRModel.parseQRValue(postValue);

        HttpSession session = req.getSession();
        String token = (String)session.getAttribute(AUTH_TOKEN);

        String url = req.getParameter(EPISODE_URL);
        String cover = req.getParameter(EPISODE_cover);
        String title = req.getParameter(EPISODE_TITLE);
        String subtitle = req.getParameter(EPISODE_SUBTITLE);
        String website = req.getParameter(EPISODE_WEBSITE);
        String description = req.getParameter(EPISODE_DESCRIPTION);
        String position = req.getParameter(EPISODE_POSITION);

        session.setAttribute(EPISODE_URL, url);
        session.setAttribute(EPISODE_cover, cover);
        session.setAttribute(EPISODE_TITLE, title);
        session.setAttribute(EPISODE_SUBTITLE, subtitle);
        session.setAttribute(EPISODE_WEBSITE, website);
        session.setAttribute(EPISODE_DESCRIPTION, description);
        session.setAttribute(EPISODE_POSITION, position);

        if (token == null) {
            resp.sendError(501, "sessionid: " + session.getId() );
            return;
        }

        boolean isAuthenticated = token.equals(postValue);
        if (isAuthenticated) {
            session.setAttribute(AUTHENTICATED, Boolean.TRUE);
        }

        if (isAuthenticated) {
            resp.sendError(502, "postValue: " + postValue + " token: " + token + " isAuth:" + isAuthenticated);
        } else {
            resp.sendError(503, "postValue: " + postValue + " token: " + token + " isAuth:" + isAuthenticated);
        }
    }

    public static boolean isAuthenticated(@Nonnull  HttpSession argSession) {
        Object obj = argSession.getAttribute(AUTHENTICATED);
        return Boolean.TRUE.equals(obj);
    }

}
