package org.bottiger.podcast.cloud;


import java.net.URL;
import java.util.List;

import org.bottiger.podcast.provider.Subscription;

import android.accounts.Account;
import android.content.Context;
import android.os.AsyncTask;

public abstract class AbstractCloudProvider implements CloudProvider {
	
	@Override
	public void addSubscriptionstoReader(Context context, Account account, List<Subscription> subscriptions) {
		for (Subscription s : subscriptions)
			addSubscriptiontoReader(context, account, s);
	}
	
	@Override
	public void removeSubscriptionsfromReader(Context context, Account account, List<Subscription> subscriptions) {
		for (Subscription s : subscriptions)
			removeSubscriptionfromReader(context, account, s);
	}
	
	@Override
	abstract public boolean auth();

	@Override
	abstract public AsyncTask<URL, Void, Void> getSubscriptionsFromReader();


	@Override
	abstract public void addSubscriptiontoReader(Context context, Account account,
			Subscription subscription);
	
	@Override
	abstract public void removeSubscriptionfromReader(Context context, Account account,
			Subscription subscription);

}
