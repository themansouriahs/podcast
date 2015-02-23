package org.bottiger.podcast;

import android.app.Activity;
import android.content.Intent;
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
public class ViewPagerSubscriptionFragment extends Fragment implements BackButtonListener, PodcastBaseFragment.OnItemSelectedListener {

    public enum State { SUBSCRIPTION, FEED };
    private State mCurrentState = State.SUBSCRIPTION;

    private Activity mActivity;
    private FragmentManager mFragmentManager;

    private PodcastBaseFragment.OnItemSelectedListener mOnItemSelectedListener;
    private SubscriptionsFragment mSubscriptionFragment;
    private FrameLayout mContainerView;

    private long subid = -1;

    public void setSubid(long subid) {
        this.subid = subid;
    }

    public void setOnItemSelectedListener(PodcastBaseFragment.OnItemSelectedListener argOnItemSelectedListener) {
        mOnItemSelectedListener = argOnItemSelectedListener;
    }

    @Override
    public void onAttach(Activity activity) {
        mActivity = activity;
        super.onAttach(activity);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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

    private FeedFragment mFeedFragment = null;

    public void fillContainerWithSubscriptions() {
        mFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.slide_out_bottom, R.anim.slide_out_bottom)
                .replace(R.id.subscription_fragment_container, mSubscriptionFragment).commit();
    }

    public void fillContainerWithFeed(long SubscriptionFeedID) {
        Subscription subscription = Subscription.getById(getActivity().getContentResolver(),
                SubscriptionFeedID);
        mFeedFragment = FeedFragment.newInstance(subscription);
        mFeedFragment.bindNewSubscrption(subscription, false);
        /*
        mFragmentManager.beginTransaction()
                .setCustomAnimations(fragmentInAnimation(), fragmentOutAnimation())
                .replace(R.id.subscription_fragment_container, mFeedFragment)
                .addToBackStack("OpenFeed")
                .commit();
                */
        FragmentTransaction ft = mFragmentManager.beginTransaction();
        ft.setCustomAnimations(R.anim.slide_in_bottom, R.anim.slide_out_bottom);
        ft.add(R.id.subscription_fragment_container, mFeedFragment).commit();
        ((ToolbarActivity)mActivity).makeToolbarTransparent(true);
    }

    @Override
    public void onItemSelected(long id) {
        setState(State.FEED);


        Intent intent = new Intent(getActivity(), FeedActivity.class);

        Bundle b = new Bundle();
        b.putLong(FeedActivity.SUBSCRIPTION_ID_KEY, id); //Your id
        intent.putExtras(b); //Put your id to your next Intent

        startActivity(intent);


        //fillContainerWithFeed(id);
    }

    private void setState(State argState) {
        if (mCurrentState == argState) {
            return;
        }

        mCurrentState = argState;
    }

    @Override
    public void back() {
        setState(State.SUBSCRIPTION);
        fillContainerWithSubscriptions();
    }

    @Override
    public boolean canGoBack() {
        return mCurrentState == State.FEED;
    }
}
