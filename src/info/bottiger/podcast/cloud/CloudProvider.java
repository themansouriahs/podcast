package info.bottiger.podcast.cloud;

import info.bottiger.podcast.provider.Subscription;

import java.util.List;

import android.accounts.Account;
import android.content.Context;

public interface CloudProvider {
	
	public boolean auth();
	
	public List<Subscription> getSubscriptionsFromReader();
	
	public void addSubscriptionstoReader(Context context, Account account, List<Subscription> subscriptions);
	public void addSubscriptiontoReader(Context context, Account account, Subscription subscription);
	
	public void removeSubscriptionsfromReader(Context context, Account account, List<Subscription> subscriptions);
	public void removeSubscriptionfromReader(Context context, Account account, Subscription subscription);

}
