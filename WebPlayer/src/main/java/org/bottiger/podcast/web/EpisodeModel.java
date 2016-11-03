package org.bottiger.podcast.web;

import javax.servlet.http.HttpSession;

/**
 * Created by aplb on 22-08-2016.
 */

public class EpisodeModel {

    private static final String EPISODE_URL = "episode_url";
    private static final String EPISODE_COVER = "episode_cover";
    private static final String EPISODE_TITLE = "episode_title";
    private static final String EPISODE_SUBTITLE = "episode_subtitle";
    private static final String EPISODE_WEBSITE = "episode_website";
    private static final String EPISODE_DESCRIPTION = "episode_description";
    private static final String EPISODE_POSITION = "episode_position";

    public static String getUrl(HttpSession argSession) {
        return getAttr(argSession, EPISODE_URL);
    }

    public static String getCover(HttpSession argSession) {
        return getAttr(argSession, EPISODE_COVER);
    }

    public static String getTitle(HttpSession argSession) {
        return getAttr(argSession, EPISODE_TITLE);
    }

    public static String getSubtitle(HttpSession argSession) {
        return getAttr(argSession, EPISODE_SUBTITLE);
    }

    public static String getWebsite(HttpSession argSession) {
        return getAttr(argSession, EPISODE_WEBSITE);
    }

    public static String getDescription(HttpSession argSession) {
        return getAttr(argSession, EPISODE_DESCRIPTION).replaceAll("\\p{Cntrl}", "");
    }

    static long getPosition(HttpSession argSession) {
        String pos = getAttr(argSession, EPISODE_POSITION);

        if (pos == null || pos == "")
            return 0L;

        return Long.parseLong(pos);
    }

    private static String getAttr(HttpSession argSession, String argName) {
        String value = (String) argSession.getAttribute(argName);

        if (value == null)
            return "";

        return (value).replace("\"", "'");
    }
}
