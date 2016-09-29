package org.bottiger.podcast.web;

import com.google.android.gcm.server.Constants;
import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.Result;
import com.google.android.gcm.server.Sender;
import com.google.api.server.spi.config.Named;
import com.google.appengine.repackaged.com.google.api.client.json.JsonFactory;
import com.google.appengine.repackaged.com.google.api.client.json.jackson2.JacksonFactory;
import com.googlecode.objectify.impl.Session;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import static org.bottiger.podcast.common.WebPlayerShared.PHONE_ID;

/**
 * Created by aplb on 26-08-2016.
 */

public class MessageSender extends HttpServlet {

    private static final Logger log = Logger.getLogger(MessagingEndpoint.class.getName());

    private static final String DESTINATION = "https://fcm.googleapis.com/fcm/send";

    /** Api Keys can be obtained from the google cloud console */
    private static final String API_KEY = System.getProperty("gcm.api.key");

    private JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();

    public static final String PARM_MSG = "message";


    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession();
        if (!AuthServlet.isAuthenticated(session)) {
            resp.sendError(401, "Not Authenticated");
            return;
        }

        String msg = req.getParameter(PARM_MSG);
        String phone_id = (String)session.getAttribute(PHONE_ID);

        sendMessage(msg, phone_id);
    }

    // For creating json: https://www.mkyong.com/java/jackson-streaming-api-to-read-and-write-json/
    private void sendMessage(@Named("message") String message, @Nonnull String argPhoneID) throws IOException {
        if (message == null || message.trim().length() == 0) {
            log.warning("Not sending message because it is empty");
            return;
        }
        // crop longer messages
        if (message.length() > 1000) {
            message = message.substring(0, 1000) + "[...]";
        }
        Sender sender = new Sender(API_KEY);
        Message msg = new Message.Builder().addData("message", message).build();

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
    }
}
