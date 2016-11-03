package org.bottiger.podcast.common;

import java.text.MessageFormat;
import java.text.ParseException;
import java.util.regex.Pattern;

public class WebPlayerShared {

    public static final String WEB_URL = "http://web.soundwavesapp.com";

    public static final String SEPARATOR = "$";

    public static final String POST_KEY = "4350967";
    public static final String AUTH_TOKEN = "token";

    public static final String EPISODE_URL = "episode_url";
    public static final String EPISODE_cover = "episode_cover";
    public static final String EPISODE_TITLE = "episode_title";
    public static final String EPISODE_SUBTITLE = "episode_subtitle";
    public static final String EPISODE_WEBSITE = "episode_website";
    public static final String EPISODE_DESCRIPTION = "episode_description";
    public static final String EPISODE_POSITION = "episode_position";
    public static final String PHONE_ID = "phone_id";

    public static final String URL_KEY = "url";
    public static final String TIME_KEY = "time";

    private static final String TEMPLATE = "'{' \"data\": '{'\n" +
            "    \"" + URL_KEY + "\": \"{0}\",\n" +
            "    \"" + TIME_KEY + "\": \"{1}\"\n" +
            "  '}',\n" +
            "  \"to\" : \"{2}\"\n" +
            "'}'"; // ' around { or } escpaes it.


    public static String[] parseQRValue(String argInput) throws ParseException {
        String[] values = argInput.split(Pattern.quote(WebPlayerShared.SEPARATOR));

        if (values.length != 2) {
            String errorMsg = argInput + " length: " + values.length + " sep: " + WebPlayerShared.SEPARATOR;
            throw new ParseException(errorMsg, 0);
        }

        return values;
    }

    public static String createMessage(String argPhoneID, String argEpisodeUrl, double argEpisodeOffsetInSeconds) throws IllegalArgumentException {
        String offsetMs = String.valueOf(Math.round(argEpisodeOffsetInSeconds*1000));
        Object[] params = new String[]{ argEpisodeUrl, offsetMs, argPhoneID};
        String msg = MessageFormat.format(TEMPLATE, params);

        // crop longer messages
        if (msg.length() > 1000) {
            throw new IllegalArgumentException("Message too long");
        }

        return msg;
    }
}
