package fi.iki.elonen;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import org.apache.commons.io.input.TeeInputStream;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.utils.StreamDownloader;

import android.content.ContentResolver;
import fi.iki.elonen.NanoHTTPD.Response.Status;

/**
 * An example of subclassing NanoHTTPD to make a custom HTTP server.
 */
public class HelloServer extends NanoHTTPD {
	
	private ContentResolver mContentResovler;
	
    public HelloServer(ContentResolver contentResolver, int port) {
        super(port);
        this.mContentResovler = contentResolver;
    }

    @Override
    public Response serve(String uri, Method method, Map<String, String> header, Map<String, String> parms, Map<String, String> files) {
    	//OkHttpClient client = new OkHttpClient();
    	FeedItem item = FeedItem.getById(mContentResovler, Long.parseLong(parms.get("id")));
    	
    	URL url = null;
    	HttpURLConnection connection = null;
    	try {
			url = new URL(item.getURL());
			connection = (HttpURLConnection) url.openConnection();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	//client.open(url);
        InputStream in = null;
        try {
          // Read the response.
          in = connection.getInputStream();
          //byte[] response = readFully(in);
          //return new String(response, "UTF-8");
        } catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        InputStream bin = new BufferedInputStream(in);

		BufferedReader r = new BufferedReader(new InputStreamReader(bin));

		/*
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
		*/
        
    	StreamDownloader sd = new StreamDownloader(item);
    	//InputStream newIS2 = new TeeInputStream(newIS2, null);
    	InputStream newIS = null;
		try {
			newIS = new TeeInputStream(bin, sd.getFileOutputStream());
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		return new NanoHTTPD.Response(Status.ACCEPTED, NanoHTTPD.MIME_DEFAULT_BINARY, newIS);
    	/*
    	System.out.println(method + " '" + uri + "' ");

        String msg = "<html><body><h1>Hello server</h1>\n";
        if (parms.get("username") == null)
            msg +=
                    "<form action='?' method='get'>\n" +
                            "  <p>Your name: <input type='text' name='username'></p>\n" +
                            "</form>\n";
        else
            msg += "<p>Hello, " + parms.get("username") + "!</p>";

        msg += "</body></html>\n";

        return new NanoHTTPD.Response(msg);
        */
    }

    public void run() {
    	try {
			super.start();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        //ServerRunner.run(HelloServer.class);
    }
}
