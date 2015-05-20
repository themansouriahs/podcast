package org.bottiger.podcast;

import org.bottiger.podcast.adapters.SubscriptionGridCursorAdapter;
import org.bottiger.podcast.playlist.SubscriptionCursorLoader;
import org.bottiger.podcast.provider.Subscription;
import org.bottiger.podcast.utils.FragmentUtils;
import org.bottiger.podcast.views.dialogs.DialogOPML;

import android.app.Activity;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

public class SubscriptionsFragment extends Fragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "SubscriptionsFragment";

    private static final boolean SHARE_ANALYTICS_DEFAULT = !BuildConfig.LIBRE_MODE;
    private static final boolean SHARE_CLOUD_DEFAULT = false;

    private boolean mShowEmptyView = true;

	private FragmentUtils mFragmentUtils;
	private View fragmentView;
	private RecyclerView mGridView;

    private CheckBox mShareAnalytics;
    private CheckBox mCloudServices;

    private GridLayoutManager mGridLayoutmanager;
    private RelativeLayout mEmptySubscrptionList;
    private SubscriptionGridCursorAdapter mAdapter;

    private Activity mActivity;
    private FrameLayout mContainerView;

    private SharedPreferences shareprefs;
    private static String PREF_SUBSCRIPTION_COLUMNS;
    private static String PREF_SHARE_ANALYTICS_KEY;
    private static String PREF_CLOUD_SUPPORT_KEY;

    private Cursor mCursor = null;
    private SubscriptionGridCursorAdapter.OnSubscriptionCountChanged mSubscriptionCountListener = new SubscriptionGridCursorAdapter.OnSubscriptionCountChanged() {
        @Override
        public void newSubscriptionCount(int argCount) {
            boolean showEmpty = argCount == 0;
            int visibility = showEmpty ? View.VISIBLE : View.GONE;

            if (mShowEmptyView != showEmpty) {
                if (showEmpty) {
                    mEmptySubscrptionList.setVisibility(View.VISIBLE);
                    mGridView.setVisibility(View.GONE);
                } else {
                    mEmptySubscrptionList.setVisibility(View.GONE);
                    mGridView.setVisibility(View.VISIBLE);
                }
            }
        }
    };


    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        mActivity = getActivity();
        PREF_SUBSCRIPTION_COLUMNS = mActivity.getResources().getString(R.string.pref_subscriptions_columns_key);
        PREF_SHARE_ANALYTICS_KEY = mActivity.getResources().getString(R.string.pref_anonymous_feedback_key);
        PREF_CLOUD_SUPPORT_KEY = mActivity.getResources().getString(R.string.pref_cloud_support_key);
        shareprefs = PreferenceManager.getDefaultSharedPreferences(mActivity.getApplicationContext());
        shareprefs.registerOnSharedPreferenceChangeListener(this);
	}

    SubscriptionCursorLoader mCursorLoader;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);

        setHasOptionsMenu(true);

        mContainerView = (FrameLayout)inflater.inflate(R.layout.subscription_container, container, false);
        FrameLayout fragmentContainer = (FrameLayout)mContainerView.findViewById(R.id.subscription_fragment_container);


        // Empty View
        mEmptySubscrptionList = (RelativeLayout) mContainerView.findViewById(R.id.subscription_empty);

        mShareAnalytics = (CheckBox) mContainerView.findViewById(R.id.checkBox_usage);
        mCloudServices =  (CheckBox) mContainerView.findViewById(R.id.checkBox_cloud);

        onSharedPreferenceChanged(shareprefs, PREF_SHARE_ANALYTICS_KEY);
        onSharedPreferenceChanged(shareprefs, PREF_CLOUD_SUPPORT_KEY);


        //RecycelrView
        mAdapter = new SubscriptionGridCursorAdapter(getActivity(), mCursor, numberOfColumns());
        mAdapter.setOnSubscriptionCountChangedListener(mSubscriptionCountListener);
        mCursorLoader = new SubscriptionCursorLoader(this, mAdapter, mCursor);

        fragmentView = inflater.inflate(getLayoutType(), fragmentContainer, true);
		mFragmentUtils = new FragmentUtils(getActivity(), fragmentView, this);
		mGridView = (RecyclerView) fragmentView.findViewById(R.id.gridview);
		registerForContextMenu(mGridView);

        mGridLayoutmanager = new GridLayoutManager(getActivity(), numberOfColumns());
        mGridView.setHasFixedSize(true);
        mGridView.setLayoutManager(mGridLayoutmanager);
        mGridView.setAdapter(mAdapter);
        //fragmentView.setVisibility(View.GONE);
        mGridView.setVisibility(View.GONE);

		return mContainerView;
	}

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onclick");
            }
        });

        mShareAnalytics.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Log.d(TAG, "Setting " + PREF_SHARE_ANALYTICS_KEY + " to " + isChecked);
                shareprefs.edit().putBoolean(PREF_SHARE_ANALYTICS_KEY, isChecked).commit();
            }
        });

        mCloudServices.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Log.d(TAG, "Setting " + PREF_CLOUD_SUPPORT_KEY + " to " + isChecked);
                shareprefs.edit().putBoolean(PREF_CLOUD_SUPPORT_KEY, isChecked).commit();
            }
        });
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mGridLayoutmanager.getSpanCount() != numberOfColumns()) {
            GridLayoutManager layoutmanager = new GridLayoutManager(getActivity(), numberOfColumns());
            //mAdapter = new SubscriptionGridCursorAdapter(getActivity(), mCursor);


            mGridView.setLayoutManager(layoutmanager);
            //mGridView.setAdapter(mAdapter);
            ((SubscriptionGridCursorAdapter)mGridView.getAdapter()).setNumberOfColumns(numberOfColumns());
        }
    }

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
	}

    /*
    private void setSubscriptionBackground(Cursor argCursor) {
        if (argCursor == null || argCursor.getCount() == 0) {
            mEmptySubscrptionList.setVisibility(View.VISIBLE);
            return;
        }

        mEmptySubscrptionList.setVisibility(View.GONE);
    }*/

	@Override
	public void onCreateOptionsMenu(final Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.subscription_actionbar, menu);
		super.onCreateOptionsMenu(menu, inflater);
        return;
	}

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_import: {
                DialogOPML dialogOPML = new DialogOPML();
                Dialog dialog = dialogOPML.onCreateDialog(getActivity());
                dialog.show();
            }
            case R.id.menu_refresh_all_subscriptions: {
                SoundWaves.sSubscriptionRefreshManager.refreshAll();
            }
        }
        return super.onOptionsItemSelected(item);
    }
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getActivity().getMenuInflater();
		inflater.inflate(R.menu.subscription_context, menu);
		//PlaylistFragment.setContextMenu(PlaylistFragment.SUBSCRIPTION_CONTEXT_MENU, this);
	}

    public boolean onContextItemSelected(MenuItem menuItem) {
        int position = -1;
        try {
            position = mAdapter.getPosition();
        } catch (Exception e) {
            Log.d(TAG, e.getLocalizedMessage(), e);
            return super.onContextItemSelected(menuItem);
        }

        switch (menuItem.getItemId()) {
            case R.id.unsubscribe:
                Subscription subscription = Subscription.getById(getActivity().getContentResolver(), position); // item.getSubscription(getActivity());
                subscription.unsubscribe(getActivity());
                return true;
            default:
                return super.onContextItemSelected(menuItem);
        }
    }

	private int getLayoutType() {
		return R.layout.subscription_list;
	}

    private int numberOfColumns() {
        String number = shareprefs.getString(PREF_SUBSCRIPTION_COLUMNS, "2");
        return Integer.parseInt(number);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key == PREF_SHARE_ANALYTICS_KEY) {
            boolean isEnabled = sharedPreferences.getBoolean(key, SHARE_ANALYTICS_DEFAULT);
            if (mShareAnalytics != null) {
                mShareAnalytics.setChecked(isEnabled);
            }
            return;
        }

        if (key == PREF_CLOUD_SUPPORT_KEY) {
            boolean isEnabled = sharedPreferences.getBoolean(key, SHARE_CLOUD_DEFAULT);
            if (mCloudServices != null) {
                mCloudServices.setChecked(isEnabled);
            }
            return;
        }
    }
}
