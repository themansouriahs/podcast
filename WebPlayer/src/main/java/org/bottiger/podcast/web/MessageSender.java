package org.bottiger.podcast.web;

import com.google.android.gcm.server.Constants;
import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.Result;
import com.google.android.gcm.server.Sender;
import com.google.api.server.spi.config.Named;
import com.google.appengine.repackaged.com.google.api.client.json.JsonFactory;
import com.google.appengine.repackaged.com.google.api.client.json.jackson2.JacksonFactory;
import com.googlecode.objectify.impl.Session;

import org.bottiger.podcast.common.WebPlayerShared;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import static org.bottiger.podcast.common.WebPlayerShared.EPISODE_URL;
import static org.bottiger.podcast.common.WebPlayerShared.PHONE_ID;

/**
 * Created by aplb on 26-08-2016.
 */

public class MessageSender extends HttpServlet {

    private static final Logger log = Logger.getLogger(MessagingEndpoint.class.getName());

    private static final String DESTINATION = "https://fcm.googleapis.com/fcm/send";

    private static final String PARM_MSG = "offset";

    private static final String WEB_KEY = "AIzaSyAtVsqTTrnjTFYzxPm-6jeAehlOKFv1sfQ";

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession();
        if (!AuthServlet.isAuthenticated(session)) {
            resp.sendError(401, "Not Authenticated");
            return;
        }

        String msg = req.getParameter(PARM_MSG);
        String phone_id = (String)session.getAttribute(PHONE_ID);
        String url = (String)session.getAttribute(EPISODE_URL);
        Double offset = Double.valueOf(msg);

        String req2 = sendMessage(req, msg, phone_id, url, offset) + "end";

        resp.sendError(402, "Message sendt: " + req2);
    }

    // For creating json: https://www.mkyong.com/java/jackson-streaming-api-to-read-and-write-json/
    private String sendMessage(HttpServletRequest req,
                               @Named("message") String message,
                               @Nonnull String argPhoneID,
                               @Nonnull String argUrl,
                               @Nonnull double argOffsetSeconds) throws IOException {
        if (message == null || message.trim().length() == 0) {
            log.warning("Not sending message because it is empty");
            return "";
        }

        URL urlObject = new URL(DESTINATION);
        HttpURLConnection connection = (HttpURLConnection)urlObject.openConnection();

        connection.setDoOutput(true);
        connection.setRequestMethod("POST");

        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Authorization", "key=" + WEB_KEY);

        //String str = TEMPLATE.replace("phone_key", argPhoneID);
        String str = WebPlayerShared.createMessage(argPhoneID, argUrl, argOffsetSeconds);

        byte[] outputInBytes = str.getBytes("UTF-8");
        OutputStream os = connection.getOutputStream();
        os.write( outputInBytes );
        os.close();


        /*
        String out = "";
        //get all headers
        Map<String, List<String>> map = connection.getHeaderFields();
        for (Map.Entry<String, List<String>> entry : map.entrySet()) {
            out += "Key : " + entry.getKey() + " ,Value : " + entry.getValue();
        }

        connection.connect();

        return out;
        */

        String out = "";
        OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
        writer.write(out); //.write(URLEncoder.encode(jsonObj.toString(), "UTF-8"));
        writer.close();

        int respCode = connection.getResponseCode();  // New items get NOT_FOUND on PUT
        if (respCode == HttpURLConnection.HTTP_OK || respCode == HttpURLConnection.HTTP_NOT_FOUND) {
            req.setAttribute("error", "");
            StringBuffer response = new StringBuffer();
            String line;

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            out = response.toString();

            req.setAttribute("response", out);
        } else {
            out = connection.getResponseMessage();
            req.setAttribute("error", connection.getResponseCode() + " " + out);
            out = connection.getResponseCode() + " - " + out;
        }

        return out;


        /*
        Sender sender = new Sender(API_KEY);
        Message msg = new Message.Builder().addData("message", message).build();

        log.info("Sending: '" + message + "' to phone: " + argPhoneID);

        Result result = sender.send(msg, argPhoneID, 5);
        {
            if (result.getMessageId() != null) {
                log.info("Message sent to " + argPhoneID);
                String canonicalRegId = result.getCanonicalRegistrationId();
                if (canonicalRegId != null) {
                    // if the regId changed, we have to update the datastore
                    log.info("Registration Id changed for " + argPhoneID + " updating to " + canonicalRegId);
                    //record.setRegId(canonicalRegId);
                    //ofy().save().entity(record).now();
                }
            } else {
                String error = result.getErrorCodeName();
                if (error.equals(Constants.ERROR_NOT_REGISTERED)) {
                    log.warning("Registration Id " + argPhoneID + " no longer registered with GCM, removing from datastore");
                    // if the device is no longer registered with Gcm, remove it from the datastore
                    //ofy().delete().entity(record).now();
                }
                else {
                    log.warning("Error when sending message : " + error);
                }
            }
        }
        */
    }
}
