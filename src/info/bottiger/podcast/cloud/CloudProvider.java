package info.bottiger.podcast.cloud;

import info.bottiger.podcast.provider.Subscription;

import java.net.URL;
import java.util.List;

import android.accounts.Account;
import android.content.Context;
import android.os.AsyncTask;

public interface CloudProvider {
	
	public boolean auth();
	
	public AsyncTask<URL, Void, Void> getSubscriptionsFromReader();
	
	public void addSubscriptionstoReader(Context context, Account account, List<Subscription> subscriptions);
	public void addSubscriptiontoReader(Context context, Account account, Subscription subscription);
	
	public void removeSubscriptionsfromReader(Context context, Account account, List<Subscription> subscriptions);
	public void removeSubscriptionfromReader(Context context, Account account, Subscription subscription);

}
