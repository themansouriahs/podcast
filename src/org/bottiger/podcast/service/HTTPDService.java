package org.bottiger.podcast.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import fi.iki.elonen.HelloServer;
import fi.iki.elonen.NanoHTTPD.Method;

public class HTTPDService  extends Service {

	private HelloServer mServer = null;
	
    @Override
    public void onCreate() {
    	mServer = new HelloServer();
    	mServer.run();
    }
    
    public String getTest() {
    	//mDebugServer.
    	Map<String, String> params = new HashMap<String, String>();
    	params.put("username", "bottiger");
    	InputStream is = mServer.serve("", Method.GET, null, params, null).getData();
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
    	
    	return total.toString();
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
        //Log.getLog(getClass())("LocalService", "Received start id " + startId + ": " + intent);
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }


}
