package org.bottiger.podcast.web;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import static org.bottiger.podcast.web.AuthServlet.isAuthenticated;

/**
 * Created by aplb on 22-08-2016.
 */

public class WelcomeServlet extends HttpServlet {

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession();

        if (session != null && isAuthenticated(session)) {
            resp.sendRedirect("/player");
        }

        resp.sendRedirect("/login");
    }
}