package org.bottiger.podcast.cloud;


import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.bottiger.podcast.cloud.GoogleReader.ReaderAction;
import org.bottiger.podcast.provider.Subscription;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import android.os.AsyncTask;

public class RemoteHTTPRequest extends AsyncTask<URL, Void, String> {
	private URL url;
	private HttpURLConnection conn;
	private String authKey;
	private ReaderAction action;

	RemoteHTTPRequest(ReaderAction ra) {
		this.action = ra;
	}

	@Override
	protected void onPostExecute(String result) {
	}

	@Override
	protected String doInBackground(URL... urls) {
		Thread.currentThread().setPriority(Thread.MIN_PRIORITY);

		url = urls[0];
		
		/*
		 * Request a new authKey. This has to be done async
		 */
		try {
			authKey = GoogleReader.amf.getResult().getString("authtoken");
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			conn = (HttpURLConnection) url.openConnection();
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		conn.setRequestProperty("Authorization", "OAuth " + authKey);
		InputStream response = null;
		String result = null;
		// response = conn.getInputStream();
		// Strange bug in Jelly Bean
		// http://stackoverflow.com/questions/11810447/httpurlconnection-worked-fine-in-android-2-x-but-not-in-4-1-no-authentication-c
		try {
			response = new BufferedInputStream(conn.getInputStream());
		} catch (IOException e) {
			e.printStackTrace();
			response = new BufferedInputStream(conn.getErrorStream());
		}

		if (action == ReaderAction.GET) {
			result = parseGoogleReader(response);
		} else if (action == ReaderAction.ADD || action == ReaderAction.DELETE) {
			result = response.toString();
		}
		return result;
	}

	private static String parseGoogleReader(InputStream input) {
		StringBuilder response = new StringBuilder();

		/*
		 * A giant bug in jelly bean forces me convert the input stream to a string and back to an inputstream.
		 * Maybe it's a speed issue with the parsing - maybe not.
		 */
		InputStreamReader iss = new InputStreamReader(input);
		StringBuilder sb = new StringBuilder();
		BufferedReader br = new BufferedReader(iss);
		String read;
		try {
			read = br.readLine();

			while (read != null) {
				sb.append(read);
				read = br.readLine();

			}
		} catch (IOException e2) {
			e2.printStackTrace();
		}

		String hhh = sb.toString();
		try {
			input = new ByteArrayInputStream(hhh.getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e2) {
			e2.printStackTrace();
		}  
		/*
		 * End of bugfixing
		 */
		

		DocumentBuilderFactory builderFactory = DocumentBuilderFactory
				.newInstance();
		DocumentBuilder builder = null;
		Document document = null;
		try {
			builder = builderFactory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}

		try {
			document = builder.parse(input);

			Element rootElement = document.getDocumentElement();

			NodeList nodes = rootElement.getChildNodes();

			DOMSource source = new DOMSource(document);
			StringWriter xmlAsWriter = new StringWriter();
			StreamResult result = new StreamResult(xmlAsWriter);
			try {
				TransformerFactory.newInstance().newTransformer()
						.transform(source, result);
			} catch (TransformerConfigurationException e1) {
				e1.printStackTrace();
			} catch (TransformerException e1) {
				e1.printStackTrace();
			} catch (TransformerFactoryConfigurationError e1) {
				e1.printStackTrace();
			}
			StringReader xmlReader = new StringReader(xmlAsWriter.toString());
			InputSource is = new InputSource(xmlReader);

			NodeList xpathNodes = null;
			XPath xpath = XPathFactory.newInstance().newXPath();
			// String expression =
			// "//object/string[@name = \"id\" and ../list/object/string = \"Listen Subscriptions\"]";
			String expression = "//object[list/object/string = \"Listen Subscriptions\"]";
			try {
				xpathNodes = (NodeList) xpath.evaluate(expression, is,
						XPathConstants.NODESET);
			} catch (XPathExpressionException e) {
				e.printStackTrace();
			}

			for (int i = 0; i < xpathNodes.getLength(); i++) {
				Node n = xpathNodes.item(i);

				NodeList ns = n.getChildNodes();

				String podName = null;
				String podFeed = null;

				for (int j = 0; j < ns.getLength(); j++) {
					Node propertyNode = ns.item(j);

					if (propertyNode.getNodeType() == Node.ELEMENT_NODE) {
						Element child = (Element) propertyNode;
						String att = child.getAttribute("name");

						if (att.equalsIgnoreCase("id")) {
							podFeed = child.getTextContent().substring(5); // remove
																			// "feed/"
																			// from
																			// tge
																			// beginning
						} else if (att.equalsIgnoreCase("title")) {
							podName = child.getTextContent();
						}
					}
				}

				Subscription podcast = new Subscription(podFeed);
				podcast.subscribe(GoogleReader.mContext);

			}

		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return response.toString();
	}

}