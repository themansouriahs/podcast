package org.bottiger.podcast;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import org.bottiger.podcast.provider.Subscription;
import org.bottiger.podcast.views.ToolbarActivity;

/**
 * Created by apl on 22-09-2014.
 */
public class ViewPagerSubscriptionFragment extends Fragment implements BackButtonListener, PodcastBaseFragment.OnItemSelectedListener {

    private Activity mActivity;
    private FragmentManager mFragmentManager;

    private PodcastBaseFragment.OnItemSelectedListener mOnItemSelectedListener;
    private SubscriptionsFragment mSubscriptionFragment;
    private FrameLayout mContainerView;

    private long subid = -1;
    private boolean displayingFeed = false;

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
        displayingFeed = false;
        mFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.slide_out_left, R.anim.slide_out_left)
                .replace(R.id.subscription_fragment_container, mSubscriptionFragment).commit();
    }

    public void fillContainerWithFeed(long SubscriptionFeedID) {
        displayingFeed = true;
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
        ft.setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right);
        ft.add(R.id.subscription_fragment_container, mFeedFragment).commit();
        ((ToolbarActivity)mActivity).makeToolbarTransparent(true);
    }

    @Override
    public void onItemSelected(long id) {
        fillContainerWithFeed(id);
        //mOnItemSelectedListener.onItemSelected(id);
    }

    private int fragmentInAnimation() {
        return R.anim.slide_in_right; //.slide_in_top;
    }

    private int fragmentOutAnimation() {
        return R.anim.slide_out_right; //abc_fade_out .slide_in_top;
    }

    @Override
    public void back() {
        fillContainerWithSubscriptions();
    }

    @Override
    public boolean canGoBack() {
        return displayingFeed;
    }
}
