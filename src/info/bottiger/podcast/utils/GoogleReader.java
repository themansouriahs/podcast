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

import info.bottiger.podcast.R;
import info.bottiger.podcast.provider.Subscription;

import com.loopj.android.http.*;

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
import java.util.ArrayList;
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

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
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
 * More about google reader API: 
 *  - http://blog.martindoms.com/2010/01/20/using-the-google-reader-api-part-3/
 *  - http://www.chrisdadswell.co.uk/grapicalls/
 *  
 *  
 * Collection of OAuth 2 utilities to achieve "one-click" approval on Android.
 * 
 * @author Chirag Shah <chirags@google.com>
 */
public class GoogleReader {
	private static final String TAG = GoogleReader.class.getName();
	private static String CLIENT= null;

	public static final String PREF_NAME = "Random Name";
	public static final String PREF_TOKEN = "accessToken";
	public static final String SCOPE = "oauth2:http://www.google.com/reader/api"; // Or
	public static final String COMSUMER_KEY = "13654253758.apps.googleusercontent.com";
	public static final String TOKEN_URL = "http://www.google.com/reader/api/0/token";
	
	private static Account mAccount;
	private static AccountManagerFuture<Bundle> amf = null;
	private static Context mContext = null;
	
	private static String baseURL = "http://www.google.com/reader/api/0/subscription/";
	private static URL getURL;
	private static URL editURL;
	
	private static Object mTokenLock = new Object();
	private static String mToken;
	
	public enum ReaderAction {
	    GET, ADD, DELETE 
	}
	
	private static SharedPreferences mSettings = null;
	
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
					final SharedPreferences.Editor editor = mSettings.edit();
					editor.putString(PREF_TOKEN, authToken);
					editor.commit();
				} else if (authIntent != null) {
					mContext.startActivity(authIntent);
				} else {
					Log.e(TAG,
							"AccountManager was unable to obtain an authToken.");
				}
			} catch (Exception e) {
				Log.e(TAG, "Auth Error", e);
			}
		}
	};
	
	public GoogleReader(Context context, Account account) {
		init(context, account);
	}
	
	public static void init(Context context, Account account) {
		//readerToken();
		mContext = context;
		mAccount = account;
		CLIENT = mContext.getString(R.string.http_client_name);
		mSettings = PreferenceManager.getDefaultSharedPreferences(mContext);
		
		try {
			getURL = new URL(baseURL + "list");
			editURL = new URL(baseURL + "edit");
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}
	
	public void oauth() throws Exception {
		new HTTPRequest(ReaderAction.GET).execute();
	}

	public void refreshAuthToken() {
		//final SharedPreferences settings = activity.getSharedPreferences(
		//		PREF_NAME, 0);
		String accessToken = mSettings.getString(PREF_TOKEN, "");
		AccountManager.get(mContext).invalidateAuthToken("com.google",
				accessToken);
		this.amf = AccountManager.get(mContext).getAuthToken(mAccount, SCOPE,
				true, cb, null);
	}

	public static List<Subscription> getSubscriptionsFromReader() {
		// http://www.google.com/reader/api/0/stream/contents/user/-/label/Listen%20Subscriptions?client=myApplication
		new HTTPRequest(ReaderAction.GET).execute(getURL);
		return null; // FIXME
	}

	public static void addSubscriptionstoReader(Context context, Account account, List<Subscription> subscriptions) {
		for (Subscription s : subscriptions) {
			addSubscriptiontoReader(context, account, s);
		}
	}
	
	public void removeSubscriptionsfromReader(Context context, Account account, List<Subscription> subscriptions) {
		for (Subscription s : subscriptions) {
			removeSubscriptionfromReader(context, account, s);
		}
	}

	public static void addSubscriptiontoReader(Context context, Account account, Subscription subscription) {
		init(context, account);
		String feed = subscription.url;
		String ac = "subscribe";
		String t = subscription.title;
		String a = "Listen Subscriptions";
		
    	final RequestParams params = new RequestParams();
    	params.put("quickadd", feed);
    	params.put("s", "feed/" + feed);
    	params.put("ac", ac);
		params.put("t", t);
		params.put("a", a);
		
		AsyncHttpResponseHandler handler = new AsyncHttpResponseHandler() {
	    	@Override
	    	public void onSuccess(String response) {
	        	Log.d("TOKEN ->",response);
	        	params.put("T", response);
	        	addSubscriptionHTTPRequest(params);
	    	}
		};
		
		
		readerToken(handler);
	}
	
	private static String buildURL(String action) {
		final StringBuilder urlString = new StringBuilder(baseURL);
		urlString.append(action);
		urlString.append("?");
		urlString.append("client=");
		urlString.append(CLIENT);
		return urlString.toString();
	}
	
	private static void addSubscriptionHTTPRequest(final RequestParams params) {
		
		String action = "quickadd";
		
		AsyncHttpClient client = new AsyncHttpClient();
		Log.d("OAuth auth: ", authHeaderValue());
		client.addHeader(authHeaderKey(), authHeaderValue());
		client.post(buildURL(action), params, new AsyncHttpResponseHandler() {
		    @Override
		    public void onSuccess(String response) {
		        Log.d("RESPONSE ->",response);
		        tagSubscriptionHTTPRequest(params);
		    }
		    
		    @Override
		     public void onFailure(Throwable e, String response) {
		         // Response failed :(
		    	int i;
		    	i = 5;
		     }
		});
	}
	
	private static void tagSubscriptionHTTPRequest(RequestParams params) {
		
		String action = "edit";
		params.put("a", "user/-/label/Listen Subscriptions");
		params.put("ac", "edit");
		
		AsyncHttpClient client = new AsyncHttpClient();
		Log.d("OAuth auth: ", authHeaderValue());
		client.addHeader(authHeaderKey(), authHeaderValue());
		client.post(buildURL(action), params, new AsyncHttpResponseHandler() {
		    @Override
		    public void onSuccess(String response) {
		        Log.d("RESPONSE ->",response);
		    }
		    
		    @Override
		     public void onFailure(Throwable e, String response) {
		         // Response failed :(
		    	int i;
		    	i = 5;
		     }
		});
	}
	
	private static void removeSubscriptionHTTPRequest(RequestParams params) {
		
		String action = "edit";
		params.put("ac", "unsubscribe");
		
		AsyncHttpClient client = new AsyncHttpClient();
		Log.d("OAuth auth: ", authHeaderValue());
		client.addHeader(authHeaderKey(), authHeaderValue());
		client.post(buildURL(action), params, new AsyncHttpResponseHandler() {
		    @Override
		    public void onSuccess(String response) {
		        Log.d("RESPONSE ->",response);
		    }
		    
		    @Override
		     public void onFailure(Throwable e, String response) {
		         // Response failed :(
		    	int i;
		    	i = 5;
		     }
		});
	}

	public void removeSubscriptionfromReader(Context context, Account account, Subscription subscription) {
		init(context, account);
		String feed = subscription.url;
		String t = subscription.title;
		
    	final RequestParams params = new RequestParams();
    	params.put("s", "feed/" + feed);
		
		AsyncHttpResponseHandler handler = new AsyncHttpResponseHandler() {
	    	@Override
	    	public void onSuccess(String response) {
	        	Log.d("TOKEN ->",response);
	        	params.put("T", response);
	        	tagSubscriptionHTTPRequest(params);
	    	}
		};
		readerToken(handler);
	}
	
	
	private static class HTTPRequest extends AsyncTask<URL, Void, String> {		
		private URL url;
		private URLConnection conn;
		private String authKey;
		private ReaderAction action;

		HTTPRequest(ReaderAction ra) {
			this.action = ra;
		}
		
		protected void onPreExecute() {
			try {
				authKey = amf.getResult().getString("authtoken");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		protected void onPostExecute(String result) {}

		protected String doInBackground(URL... urls) {

			url = urls[0];
			
			try {
				conn = (HttpURLConnection) url.openConnection();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			conn.setRequestProperty("Authorization", "OAuth " + authKey);
			InputStream response = null;
			String result = null;
			try {
				response = conn.getInputStream();
				if (action == ReaderAction.GET) {
					result = parseGoogleReader(response);
				} else if (action == ReaderAction.ADD || action == ReaderAction.DELETE) {
					result = response.toString();
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				
				//hack http://stackoverflow.com/questions/11810447/httpurlconnection-worked-fine-in-android-2-x-but-not-in-4-1-no-authentication-c
				//response = conn.get
			}
			return result;
		}
	}
	
	private static String parseGoogleReader(InputStream input) {
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
			//document = builder.parse(conn.getInputStream());
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
			podcast.subscribe(GoogleReader.mContext);
			//podcast.subscribe(getContentResolver());
			//contentService.addSubscription(podcast);
		
		}
		
		
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
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
	
	private String getAuthToken() {
		this.amf =  AccountManager.get(mContext).getAuthToken(mAccount, SCOPE,
				true, cb, null);
		try {
			return this.amf.getResult().getString("authtoken");
		} catch (OperationCanceledException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (AuthenticatorException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	private static void readerToken(AsyncHttpResponseHandler handler) {
			AsyncHttpClient client = new AsyncHttpClient();
			client.addHeader(authHeaderKey(), authHeaderValue());
			client.get(TOKEN_URL, handler);
	}
	
	private static String authHeaderKey() {
		return "Authorization";
	}
	
	private static String authHeaderValue() {
		String authKey = null;
		try {
			authKey = amf.getResult().getString("authtoken");
		} catch (OperationCanceledException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (AuthenticatorException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "OAuth " + authKey;
	}
}