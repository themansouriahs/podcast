package org.bottiger.podcast.fetcher;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class StreamDownloader {
	
	public void startServer() {
		new Thread(new Runnable() {
			public void run() {
				URL url = null;
				HttpURLConnection connection = null;
				try {
					url = new URL("http://127.0.0.1:8080/test");
					connection = (HttpURLConnection) url.openConnection();
				} catch (MalformedURLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				InputStream in = null;
				try {
					// Read the response.
					in = connection.getInputStream();
					// byte[] response = readFully(in);
					// return new String(response, "UTF-8");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				InputStream bin = new BufferedInputStream(in);

				BufferedReader r = new BufferedReader(
						new InputStreamReader(bin));

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
	}

}
