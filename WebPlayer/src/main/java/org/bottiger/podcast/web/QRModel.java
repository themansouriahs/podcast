package org.bottiger.podcast.web;

import org.bottiger.podcast.common.WebPlayerShared;

import java.math.BigInteger;
import java.security.SecureRandom;

import javax.servlet.http.HttpSession;

import static org.bottiger.podcast.common.WebPlayerShared.AUTH_TOKEN;

/**
 * Created by aplb on 19-08-2016.
 */

public class QRModel {

    private static SecureRandom randomizer = new SecureRandom();

    public static String getQRUrl(HttpSession argSession) {
        return "https://zxing.org/w/chart?cht=qr&chs=400x400&chld=L&choe=UTF-8&chl=" + getAndSaveQRValue(argSession);
    }

    private static String getAndSaveQRValue(HttpSession argSession) {
        String authToken = nextAuthToken();
        argSession.setAttribute(AUTH_TOKEN, authToken);
        return  authToken + WebPlayerShared.SEPARATOR + getSessionID(argSession);
    }

    private static String getSessionID(HttpSession argSession) {
        return argSession.getId();
    }

    private static String nextAuthToken() {
        return new BigInteger(130, randomizer).toString(32);
    }
}
