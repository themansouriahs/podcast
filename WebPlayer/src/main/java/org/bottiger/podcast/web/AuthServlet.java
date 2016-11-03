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

import static org.bottiger.podcast.common.WebPlayerShared.AUTH_TOKEN;
import static org.bottiger.podcast.common.WebPlayerShared.EPISODE_DESCRIPTION;
import static org.bottiger.podcast.common.WebPlayerShared.EPISODE_POSITION;
import static org.bottiger.podcast.common.WebPlayerShared.EPISODE_SUBTITLE;
import static org.bottiger.podcast.common.WebPlayerShared.EPISODE_TITLE;
import static org.bottiger.podcast.common.WebPlayerShared.EPISODE_URL;
import static org.bottiger.podcast.common.WebPlayerShared.EPISODE_WEBSITE;
import static org.bottiger.podcast.common.WebPlayerShared.EPISODE_cover;
import static org.bottiger.podcast.common.WebPlayerShared.PHONE_ID;
import static org.bottiger.podcast.common.WebPlayerShared.POST_KEY;


// [START example]
@SuppressWarnings("serial")
public class AuthServlet extends HttpServlet {

    private static final String AUTHENTICATED = "logged_in";

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession();

        if (isAuthenticated(session)) {
            PrintWriter out = resp.getWriter();
            long position = EpisodeModel.getPosition(session);
            out.print(position);
            return;
        }

        String token = (String)session.getAttribute(AUTH_TOKEN);

        String debugOut = "IsAuthenticated: " + isAuthenticated(session) +
                          " SessionAuth: " + session.getAttribute(AUTHENTICATED) +
                          " SessionId: " + session.getId() +
                          " AuthToken: " + token;

        resp.sendError(401, debugOut);
    }

    // Process the http POST of the form
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        String postValue = req.getParameter(POST_KEY);

        HttpSession session = req.getSession();
        String token = (String)session.getAttribute(AUTH_TOKEN);

        String url = req.getParameter(EPISODE_URL);
        String cover = req.getParameter(EPISODE_cover);
        String title = req.getParameter(EPISODE_TITLE);
        String subtitle = req.getParameter(EPISODE_SUBTITLE);
        String website = req.getParameter(EPISODE_WEBSITE);
        String description = req.getParameter(EPISODE_DESCRIPTION);
        String position = req.getParameter(EPISODE_POSITION);
        String phone_id = req.getParameter(PHONE_ID);

        session.setAttribute(EPISODE_URL, url);
        session.setAttribute(EPISODE_cover, cover);
        session.setAttribute(EPISODE_TITLE, title);
        session.setAttribute(EPISODE_SUBTITLE, subtitle);
        session.setAttribute(EPISODE_WEBSITE, website);
        session.setAttribute(EPISODE_DESCRIPTION, description);
        session.setAttribute(EPISODE_POSITION, position);
        session.setAttribute(PHONE_ID, phone_id);

        if (token == null) {
            resp.sendError(501, "sessionid: " + session.getId() );
            return;
        }

        boolean isAuthenticated = token.equals(postValue);
        session.setAttribute(AUTHENTICATED, isAuthenticated);

        int errorCode = isAuthenticated ? 502 : 503;
        resp.sendError(errorCode, "postValue: " + postValue + " token: " + token + " isAuth:" + isAuthenticated);
    }

    static boolean isAuthenticated(@Nonnull HttpSession argSession) {
        Object obj = argSession.getAttribute(AUTHENTICATED);
        return Boolean.TRUE.equals(obj);
    }

}
