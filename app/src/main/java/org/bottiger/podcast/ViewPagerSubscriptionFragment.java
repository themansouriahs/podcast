package org.bottiger.podcast;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import org.bottiger.podcast.provider.Subscription;

/**
 * Created by apl on 22-09-2014.
 */
public class ViewPagerSubscriptionFragment extends Fragment implements PodcastBaseFragment.OnItemSelectedListener {

    private FragmentManager mFragmentManager;

    private PodcastBaseFragment.OnItemSelectedListener mOnItemSelectedListener;
    private SubscriptionsFragment mSubscriptionFragment;
    private FrameLayout mContainerView;

    private long subid = -1;

    public void setSubid(long subid) {
        this.subid = subid;
    }

    public ViewPagerSubscriptionFragment(PodcastBaseFragment.OnItemSelectedListener argOnItemSelectedListener) {
        mOnItemSelectedListener = argOnItemSelectedListener;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //mFragmentManager = getSupportFragmentManager(); // getSupportFragmentManager();
        mFragmentManager = getChildFragmentManager();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        mContainerView = (FrameLayout)inflater.inflate(R.layout.subscription_container, container, false);

        mSubscriptionFragment = new SubscriptionsFragment();
        mSubscriptionFragment.mCLickListener = this;
        mFragmentManager.beginTransaction().replace(R.id.subscription_fragment_container, mSubscriptionFragment).commit();

        return mContainerView;
    }

    public void fillContainer() {
        if (subid > 0) {
            fillContainerWithSubscriptions();
        } else {
            fillContainerWithFeed(subid);
        }
    }

    public void fillContainerWithSubscriptions() {
        mFragmentManager.beginTransaction()
                .setCustomAnimations(fragmentInAnimation(), fragmentOutAnimation())
                .replace(R.id.subscription_fragment_container, mSubscriptionFragment).commit();
    }

    public void fillContainerWithFeed(long SubscriptionFeedID) {
        Subscription subscription = Subscription.getById(getActivity().getContentResolver(),
                SubscriptionFeedID);
        FeedFragment mFeedFragment = FeedFragment.newInstance(subscription);
        mFragmentManager.beginTransaction()
                .setCustomAnimations(fragmentInAnimation(), fragmentOutAnimation())
                .replace(R.id.subscription_fragment_container, mFeedFragment).commit();
    }

    @Override
    public void onItemSelected(long id) {
        //fillContainerWithFeed(id);
        mOnItemSelectedListener.onItemSelected(id);
    }

    private int fragmentInAnimation() {
        return R.anim.slide_in_top;
    }

    private int fragmentOutAnimation() {
        return R.anim.slide_in_top;
    }
}
