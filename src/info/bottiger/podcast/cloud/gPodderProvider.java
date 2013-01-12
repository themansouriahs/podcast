package info.bottiger.podcast.cloud;

import info.bottiger.podcast.provider.Subscription;

import java.util.List;

import android.accounts.Account;
import android.content.Context;

public class gPodderProvider extends AbstractCloudProvider {

	@Override
	public boolean auth() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public List<Subscription> getSubscriptionsFromReader() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addSubscriptiontoReader(Context context, Account account,
			Subscription subscription) {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeSubscriptionfromReader(Context context, Account account,
			Subscription subscription) {
		// TODO Auto-generated method stub

	}

}
