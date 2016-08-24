package org.bottiger.podcast.web;

import java.math.BigInteger;
import java.security.SecureRandom;

import javax.servlet.http.HttpSession;

/**
 * Created by aplb on 19-08-2016.
 */

public class QRModel {

    private static final String sSeparator = "-";
    static SecureRandom randomizer = new SecureRandom();

    public static String getQRUrl(HttpSession argSession) {
        return "https://zxing.org/w/chart?cht=qr&chs=400x400&chld=L&choe=UTF-8&chl=" + getAndSaveQRValue(argSession);
    }

    public static String getAndSaveQRValue(HttpSession argSession) {
        String id = nextSessionId();
        argSession.setAttribute(AuthServlet.AUTH_TOKEN, id);
        return  id + sSeparator + getSessionID(argSession);
    }

    public static String parseQRValue(String argQRValue) {
        return argQRValue.split(sSeparator)[1];
    }

    public static String getSessionID(HttpSession argSession) {
        return argSession.getId();
    }

    public static String nextSessionId() {
        return new BigInteger(130, randomizer).toString(32);
    }
}
