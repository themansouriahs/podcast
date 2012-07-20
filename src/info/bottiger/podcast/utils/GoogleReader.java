package info.bottiger.podcast.utils;

/*
 * Copyright 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import info.bottiger.podcast.provider.Subscription;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.List;

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

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import oauth.signpost.OAuth;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.basic.DefaultOAuthConsumer;
import oauth.signpost.basic.DefaultOAuthProvider;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * Collection of OAuth 2 utilities to achieve "one-click" approval on Android.
 * 
 * @author Chirag Shah <chirags@google.com>
 */
public class GoogleReader {
	private static final String TAG = GoogleReader.class.getName();

	public static final String PREF_NAME = "Random Name";
	public static final String PREF_TOKEN = "accessToken";
	public static final String SCOPE = "oauth2:http://www.google.com/reader/api"; // Or
	public static final String COMSUMER_KEY = "13654253758.apps.googleusercontent.com";

	private AccountManagerFuture<Bundle> amf = null;
	private Context context = null;
	
	public void oauth() throws Exception {
		new HTTPRequest().execute();
	}

	public void refreshAuthToken(final Context context, final Account account) {
		//final SharedPreferences settings = activity.getSharedPreferences(
		//		PREF_NAME, 0);
		this.context = context;
		final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
		String accessToken = settings.getString(PREF_TOKEN, "");
		final AccountManagerCallback<Bundle> cb = new AccountManagerCallback<Bundle>() {
			public void run(AccountManagerFuture<Bundle> future) {
				try {
					final Bundle result = future.getResult();
					final String accountName = result
							.getString(AccountManager.KEY_ACCOUNT_NAME);
					final String authToken = result
							.getString(AccountManager.KEY_AUTHTOKEN);
					final Intent authIntent = result
							.getParcelable(AccountManager.KEY_INTENT);
					if (accountName != null && authToken != null) {
						final SharedPreferences.Editor editor = settings.edit();
						editor.putString(PREF_TOKEN, authToken);
						editor.commit();
					} else if (authIntent != null) {
						context.startActivity(authIntent);
					} else {
						Log.e(TAG,
								"AccountManager was unable to obtain an authToken.");
					}
				} catch (Exception e) {
					Log.e(TAG, "Auth Error", e);
				}
			}
		};
		AccountManager.get(context).invalidateAuthToken("com.google",
				accessToken);
		this.amf = AccountManager.get(context).getAuthToken(account, SCOPE,
				true, cb, null);
	}

	public void synchronize() {
		int i;
		// get all remote subscriptions and add them to the local database
		//for (Subscription s : this.getSubscriptionsFromReader())
			//this.contentService.addSubscription(s);

		// push local subscriptions to reader
		//this.addSubscriptionstoReader(this.contentService.getSubscriptions());
	}

	public List<Subscription> getSubscriptionsFromReader() {
		// http://www.google.com/reader/api/0/stream/contents/user/-/label/Listen%20Subscriptions?client=myApplication
		new HTTPRequest().execute();
		return null; // FIXME
	}

	private boolean addSubscriptionstoReader(List<Subscription> subscriptions) {
		for (Subscription s : subscriptions) {
			if (!this.addSubscriptiontoReader(s))
				return false;
		}
		return true;
	}

	private boolean addSubscriptiontoReader(Subscription subscription) {
		return false; // FIXME
	}

	private class HTTPRequest extends AsyncTask<Void, Void, String> {
		private String Content;
		private String Error = null;

		protected void onPreExecute() {
		}

		protected void onPostExecute(String result) {

		}

		protected String doInBackground(Void... urls) {

			URL url = null;
			try {
				url = new URL(
						"http://www.google.com/reader/api/0/subscription/list");
			} catch (MalformedURLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			URLConnection conn = null;

			try {
				conn = (HttpURLConnection) url.openConnection();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			String authKey = null;
			;
			try {
				authKey = amf.getResult().getString("authtoken");
			} catch (OperationCanceledException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (AuthenticatorException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			conn.setRequestProperty("Authorization", "OAuth " + authKey);

			StringBuilder response = new StringBuilder();
			/*
			 * try { BufferedReader in = new BufferedReader(isr); String
			 * inputLine; while ((inputLine = in.readLine()) != null)
			 * response.append(inputLine); in.close(); } catch (IOException e) {
			 * Error = e.getMessage(); cancel(true); }
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
				document = builder.parse(conn.getInputStream());
			} catch (SAXException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

			Element rootElement = document.getDocumentElement();

			NodeList nodes = rootElement.getChildNodes();

			DOMSource source = new DOMSource(document);
			StringWriter xmlAsWriter = new StringWriter();
			StreamResult result = new StreamResult(xmlAsWriter);
			try {
				TransformerFactory.newInstance().newTransformer()
						.transform(source, result);
			} catch (TransformerConfigurationException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (TransformerException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (TransformerFactoryConfigurationError e1) {
				// TODO Auto-generated catch block
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
				// TODO Auto-generated catch block
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
						// String v = n.getTextContent();
						//Log.v(TAG, att);

						if (att.equalsIgnoreCase("id")) {
							podFeed = child.getTextContent().substring(5); // remove "feed/" from tge beginning
						} else if (att.equalsIgnoreCase("title")) {
							podName = child.getTextContent();
						}
					}
				}

				Subscription podcast = new Subscription(podFeed);
				podcast.subscribe(GoogleReader.this.context.getContentResolver());
				//podcast.subscribe(getContentResolver());
				//contentService.addSubscription(podcast);
			}

			/*
			 * // loop over "subscription list node" for (int i = 0; i <
			 * nodes.getLength(); i++) { Node node = nodes.item(i);
			 * 
			 * if (node instanceof Element) { Element child = (Element) node;
			 * 
			 * NodeList objectNodes = node.getChildNodes();
			 * 
			 * // loop over "object" nodes for (int j = 0; j <
			 * objectNodes.getLength(); j++) { Node object =
			 * objectNodes.item(j); if (node instanceof Element) { Element el =
			 * (Element) object;
			 * 
			 * // objectPropertiers => string, string, list, // string, number,
			 * string NodeList objectPropertiers = el.getChildNodes();
			 * 
			 * for (int k = 0; k < objectPropertiers.getLength(); k++) { Node
			 * objectNode = objectPropertiers.item(k); if (objectNode instanceof
			 * Element) { String attName = ((Element)
			 * objectNode).getAttribute("name");
			 * 
			 * if (attName.equalsIgnoreCase("categories") &&
			 * objectNode.hasChildNodes()) {
			 * 
			 * XPath xpath = XPathFactory.newInstance().newXPath(); String
			 * expression = "//object[string = \"Listen Subscriptions\"]";
			 * InputSource inputSource = new InputSource(); NodeSet nodes =
			 * (NodeSet) xpath.evaluate(expression, inputSource,
			 * XPathConstants.NODESET);
			 * 
			 * } } } } } } }
			 */

			return response.toString();
		}
	}
}