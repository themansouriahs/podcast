package org.bottiger.podcast.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.bottiger.podcast.utils.SDCardManager;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.dragontek.mygpoclient.http.HttpClient;

import fi.iki.elonen.HelloServer;
import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Method;

public class HTTPDService extends Service {

	private HelloServer mServer = null;
    public static final String host = "http://localhost";
	public static final int port = 8080;
    public static final String episode_key = "eid";

	@Override
	public void onCreate() {
		mServer = new HelloServer(getContentResolver(), port);
		mServer.run();
	}

    public static String proxyURL(long id) {
        return host + ":" + String.valueOf(port) + "/?" + episode_key + "=" + String.valueOf(id);
    }

	public String getTest() {
		// mDebugServer.

		new Thread(new Runnable() {
			public void run() {

                DefaultHttpClient httpclient = new DefaultHttpClient();

                // Prepare a request object
                HttpGet httpget = new HttpGet("http://localhost:8080/?url=http://downloads.bbc.co.uk/podcasts/radio4/timc/timc_20140310-1029b.mp3");

                // Execute the request
                HttpResponse response;
                try {
                    response = httpclient.execute(httpget);
                    // Examine the response status
                    Log.i("Praeda",response.getStatusLine().toString());

                    // Get hold of the response entity
                    HttpEntity entity = response.getEntity();
                    // If the response does not enclose an entity, there is no need
                    // to worry about connection release

                    if (entity != null) {
                    }


                } catch (Exception e) {
                    e.printStackTrace();
                }

                Map<String, String> params = new HashMap<String, String>();
				params.put("username", "bottiger");
                NanoHTTPD.Response resp = mServer.serve(
                        "http://localhost:8080/?id=stackoverflow.com/questions/2922210/reading-text-file-from-server-on-android", Method.GET,
                        null, params, null);

                if (resp == null)
                    return;

				InputStream is = resp.getData();

				BufferedReader r = new BufferedReader(new InputStreamReader(is));

				StringBuilder total = new StringBuilder();
				String line;
				try {
					while ((line = r.readLine()) != null) {
						total.append(line);
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				String res = total.toString();
				res = res + "sdfd";
				res = res + "123";

			}
		}).start();

		return "hej";
	}

	private final IBinder binder = new HTTPDBinder();

	public class HTTPDBinder extends Binder {
		public HTTPDService getService() {
			return HTTPDService.this;
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// Log.getLog(getClass())("LocalService", "Received start id " + startId
		// + ": " + intent);
		// We want this service to continue running until it is explicitly
		// stopped, so return sticky.
		return START_STICKY;
	}

}
