package org.bottiger.podcast.cloud;

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

import java.io.IOException;
import java.net.URL;

import org.bottiger.podcast.R;
import org.bottiger.podcast.provider.Subscription;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

/**
 * More about google reader API: -
 * http://blog.martindoms.com/2010/01/20/using-the-google-reader-api-part-3/ -
 * http://www.chrisdadswell.co.uk/grapicalls/
 * 
 * 
 * Collection of OAuth 2 utilities to achieve "one-click" approval on Android.
 * 
 * @author Chirag Shah <chirags@google.com>
 */
public class GoogleReader extends AbstractCloudProvider {
	private static final String TAG = GoogleReader.class.getName();
	private static String CLIENT = null;

	private static Account mAccount;
	static AccountManagerFuture<Bundle> amf = null;
	static Context mContext = null;

	public static final String PREF_NAME = "Random Name";
	public static final String PREF_TOKEN = "accessToken";
	public static final String SCOPE = "oauth2:http://www.google.com/reader/api"; // Or
	// public static final String COMSUMER_KEY =
	// "13654253758.apps.googleusercontent.com";
	public static String COMSUMER_KEY = null;
	public static final String TOKEN_URL = "http://www.google.com/reader/api/0/token";

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
		@Override
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

	/*
	 * Initialize the GoogleReader object. The API key should be removed from
	 * the arguments and extracted from the context if possible
	 */
	public GoogleReader(Context context, Account account, String APIKey) {
		// GoogleReader.COMSUMER_KEY = ((SoundWaves)
		// mContext.getApplicationContext()).getGoogleReaderConsumerKey();
		GoogleReader.COMSUMER_KEY = APIKey;
		init(context, account);
	}

	public static void init(Context context, Account account) {
		// readerToken();
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

	@Override
	public boolean auth() {
		try {
			oauth();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return true;
	}

	public void oauth() throws Exception {
		new RemoteHTTPRequest(ReaderAction.GET).execute();
	}

	public boolean refreshAuthToken() {
		String accessToken = mSettings.getString(PREF_TOKEN, "");
		AccountManager.get(mContext).invalidateAuthToken("com.google",
				accessToken);

		// Deprecated way
		// GoogleReader.amf =
		// AccountManager.get(mContext).getAuthToken(mAccount, SCOPE,
		// true, cb, null);
		if (mContext instanceof Activity) {
			GoogleReader.amf = AccountManager.get(mContext).getAuthToken(
					mAccount, SCOPE, null, (Activity) mContext, cb, null);
			return true;
		}

		return false;
	}

	@Override
	public AsyncTask<URL, Void, Void> getSubscriptionsFromReader() {
		// http://www.google.com/reader/api/0/stream/contents/user/-/label/Listen%20Subscriptions?client=myApplication
		return new ReaderHTTPRequest().execute(getURL);
	}

	private class ReaderHTTPRequest extends AsyncTask<URL, Void, Void> {
		@Override
		protected Void doInBackground(URL... urls) {
			Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
			if (GoogleReader.this.refreshAuthToken()) {
				new RemoteHTTPRequest(ReaderAction.GET).execute(urls[0]);
			}
			return null;
		}
	}

	@Override
	public void addSubscriptiontoReader(Context context, Account account,
			Subscription subscription) {
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
				Log.d("TOKEN ->", response);
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
				Log.d("RESPONSE ->", response);
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
				Log.d("RESPONSE ->", response);
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
				Log.d("RESPONSE ->", response);
			}

			@Override
			public void onFailure(Throwable e, String response) {
				// Response failed :(
				int i;
				i = 5;
			}
		});
	}

	@Override
	public void removeSubscriptionfromReader(Context context, Account account,
			Subscription subscription) {
		init(context, account);
		String feed = subscription.url;
		String t = subscription.title;

		final RequestParams params = new RequestParams();
		params.put("s", "feed/" + feed);

		AsyncHttpResponseHandler handler = new AsyncHttpResponseHandler() {
			@Override
			public void onSuccess(String response) {
				Log.d("TOKEN ->", response);
				params.put("T", response);
				removeSubscriptionHTTPRequest(params);
			}
		};
		readerToken(handler);
	}

	@Deprecated
	private String getAuthToken() {
		GoogleReader.amf = AccountManager.get(mContext).getAuthToken(mAccount,
				SCOPE, true, cb, null);
		try {
			return GoogleReader.amf.getResult().getString("authtoken");
		} catch (OperationCanceledException e) {
			e.printStackTrace();
		} catch (AuthenticatorException e) {
			e.printStackTrace();
		} catch (IOException e) {
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