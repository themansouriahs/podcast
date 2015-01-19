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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.input.TeeInputStream;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.service.HTTPDService;
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
    	String eid = parms.get(HTTPDService.episode_key);
        NanoHTTPD.Response resp = null;

    	if (eid != null) {

        // Initialize variables
    	FeedItem item = FeedItem.getById(mContentResovler, Long.parseLong(eid));
    	URL url = null;
    	HttpURLConnection connection = null;
        InputStream in = null;

        // Open the HTTP connection
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

        try {
          // Read the response.
          in = connection.getInputStream();
          //byte[] response = readFully(in);
         //return new String(response, "UTF-8");
        } catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

        //InputStream bin = new BufferedInputStream(in);
		//BufferedReader r = new BufferedReader(new InputStreamReader(bin));

        
    	StreamDownloader sd = null;
    	//InputStream newIS2 = new TeeInputStream(in, sd.getFileOutputStream());
    	InputStream newIS = null;
		try {
			newIS = new TeeInputStream(in, StreamDownloader.getFileOutputStream(item), true);
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

        String datatype = connection.getContentType(); //NanoHTTPD.MIME_DEFAULT_BINARY
        resp = new NanoHTTPD.Response(Status.OK, datatype, newIS);

            Map<String, List<String>> headerFields = connection.getHeaderFields();

            Set<String> headerFieldsSet = headerFields.keySet();
            Iterator<String> hearerFieldsIter = headerFieldsSet.iterator();

            while (hearerFieldsIter.hasNext()) {

                String headerFieldKey = hearerFieldsIter.next();
                List<String> headerFieldValue = headerFields.get(headerFieldKey);

                StringBuilder sb = new StringBuilder();
                for (String value : headerFieldValue) {
                    sb.append(value);
                    sb.append("");
                }

                //System.out.println(headerFieldKey + "=" + sb.toString());
                if (headerFieldKey != null)
                    resp.addHeader(headerFieldKey, sb.toString());
            }
    	}

    	return resp;
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
