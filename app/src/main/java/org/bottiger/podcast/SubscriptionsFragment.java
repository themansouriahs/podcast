package org.bottiger.podcast;

import org.bottiger.podcast.adapters.SubscriptionAdapter;
import org.bottiger.podcast.adapters.SubscriptionCursorAdapter;
import org.bottiger.podcast.model.Library;
import org.bottiger.podcast.model.events.DownloadProgress;
import org.bottiger.podcast.provider.Subscription;
import org.bottiger.podcast.provider.SubscriptionLoader;
import org.bottiger.podcast.utils.FragmentUtils;
import org.bottiger.podcast.views.dialogs.DialogOPML;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
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
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import java.util.concurrent.TimeUnit;

import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subscriptions.Subscriptions;

public class SubscriptionsFragment extends Fragment implements View.OnClickListener,
                                                                SharedPreferences.OnSharedPreferenceChangeListener,
                                                                DrawerActivity.TopFound {

    private static final String TAG = "SubscriptionsFragment";

    private static final boolean SHARE_ANALYTICS_DEFAULT = !BuildConfig.LIBRE_MODE;
    private static final boolean SHARE_CLOUD_DEFAULT = false;

    private Boolean mShowEmptyView = null;

	private RecyclerView mGridView;

    private CheckBox mShareAnalytics;
    private CheckBox mCloudServices;

    private Library mLibrary;

    private GridLayoutManager mGridLayoutmanager;
    private RelativeLayout mEmptySubscrptionList;
    private Button mEmptySubscrptionImportOPMLButton;
    private SubscriptionAdapter mAdapter;
    private FrameLayout mGridContainerView;

    private Activity mActivity;
    private FrameLayout mContainerView;

    private rx.Subscription mRxSubscription;

    private SharedPreferences shareprefs;
    private static String PREF_SUBSCRIPTION_COLUMNS;
    private static String PREF_SHARE_ANALYTICS_KEY;
    private static String PREF_CLOUD_SUPPORT_KEY;

    private SubscriptionCursorAdapter.OnSubscriptionCountChanged mSubscriptionCountListener = new SubscriptionCursorAdapter.OnSubscriptionCountChanged() {
        @Override
        public void newSubscriptionCount(int argCount) {
            setSubscriptionFragmentLayout(argCount);
        }
    };

    private void setSubscriptionFragmentLayout(int argSubscriptionCount) {
        boolean showEmpty = argSubscriptionCount == 0;

        if (mShowEmptyView == null || mShowEmptyView != showEmpty) {
            if (showEmpty) {
                mEmptySubscrptionList.setVisibility(View.VISIBLE);
            } else {
                mEmptySubscrptionList.setVisibility(View.GONE);
            }
            mShowEmptyView = showEmpty;
        }
    }


    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        mActivity = getActivity();
        mLibrary = SoundWaves.getLibraryInstance();
        PREF_SUBSCRIPTION_COLUMNS = mActivity.getResources().getString(R.string.pref_subscriptions_columns_key);
        PREF_SHARE_ANALYTICS_KEY = mActivity.getResources().getString(R.string.pref_anonymous_feedback_key);
        PREF_CLOUD_SUPPORT_KEY = mActivity.getResources().getString(R.string.pref_cloud_support_key);
        shareprefs = PreferenceManager.getDefaultSharedPreferences(mActivity.getApplicationContext());
        shareprefs.registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);

        setHasOptionsMenu(true);

        mContainerView = (FrameLayout)inflater.inflate(R.layout.subscription_fragment, container, false);

        // Empty View
        mEmptySubscrptionList = (RelativeLayout) mContainerView.findViewById(R.id.subscription_empty);
        mEmptySubscrptionImportOPMLButton = (Button) mContainerView.findViewById(R.id.import_opml_button);

        mGridContainerView = (FrameLayout) mContainerView.findViewById(R.id.subscription_grid_container);

        mShareAnalytics = (CheckBox) mContainerView.findViewById(R.id.checkBox_usage);
        mCloudServices =  (CheckBox) mContainerView.findViewById(R.id.checkBox_cloud);

        //RecycelrView
        mAdapter = new SubscriptionAdapter(getActivity(), mLibrary, numberOfColumns());
        setSubscriptionFragmentLayout(mLibrary.getSubscriptions().size());

		mGridView = (RecyclerView) mContainerView.findViewById(R.id.gridview);
		registerForContextMenu(mGridView);

        mGridLayoutmanager = new GridLayoutManager(getActivity(), numberOfColumns());
        mGridView.setHasFixedSize(true);
        mGridView.setLayoutManager(mGridLayoutmanager);
        mGridView.setAdapter(mAdapter);

        mRxSubscription = mLibrary.mSubscriptionsChangeObservable
                .ofType(Subscription.class)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Subscription>() {
                    @Override
                    public void call(Subscription argSubscription) {
                        Log.v(TAG, "Recieved Subscription event: " + argSubscription);
                        mAdapter.notifyDataSetChanged();
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Log.d(TAG, "error: " + throwable.toString());
                    }
                });

		return mContainerView;

	}

    @Override
    public void onDestroyView () {
        super.onDestroyView();
        if (mRxSubscription != null && !mRxSubscription.isUnsubscribed()) {
            mRxSubscription.unsubscribe();
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        ((MainActivity)getActivity()).listeners.add(this);
        super.onViewCreated(view, savedInstanceState);

        mEmptySubscrptionImportOPMLButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getActivity() instanceof TopActivity) {
                    openImportExportDialog((TopActivity)getActivity());
                } else {
                    Log.wtf(TAG, "getActivity() is not an instance of TopActivity. Please investigate"); // NoI18N
                }
            }
        });

        onSharedPreferenceChanged(shareprefs, PREF_SHARE_ANALYTICS_KEY);
        onSharedPreferenceChanged(shareprefs, PREF_CLOUD_SUPPORT_KEY);

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
        if (mGridLayoutmanager.getSpanCount() != numberOfColumns()) {
            mGridLayoutmanager = new GridLayoutManager(getActivity(), numberOfColumns());
            mGridView.setLayoutManager(mGridLayoutmanager);
            ((SubscriptionAdapter)mGridView.getAdapter()).setNumberOfColumns(numberOfColumns());
        }
        super.onResume();
    }

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

                try {
                    TopActivity topActivity = (TopActivity) getActivity();
                    openImportExportDialog(topActivity);
                } catch (ClassCastException cce) {
                    Log.wtf(TAG, "Activity (" + getActivity().getClass().getName() + ") could not be cast to TopActivity." +
                            "This is needed in order to requets filesystem permission. Exception: " + cce.toString()); // NoI18N
                }
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
                Subscription subscription = SoundWaves.getLibraryInstance().getSubscription((long)position);

                if (subscription == null)
                    return false;

                subscription.setStatus(Subscription.STATUS_UNSUBSCRIBED);
                return true;
            default:
                return super.onContextItemSelected(menuItem);
        }
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

    public static void openImportExportDialog(@NonNull TopActivity argActivity) {

        if (!checkPermission(argActivity))
            return;

        DialogOPML dialogOPML = new DialogOPML();
        Dialog dialog = dialogOPML.onCreateDialog(argActivity);
        dialog.show();
    }

    /**
     *
     * @return True if the permissions are granted
     */
    private static boolean checkPermission(@NonNull TopActivity argActivity) {
        if (Build.VERSION.SDK_INT >= 23 &&
                (argActivity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                        argActivity.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)  != PackageManager.PERMISSION_GRANTED)) {
            // TODO: Consider calling
            //    public void requestPermissions(@NonNull String[] permissions, int requestCode)
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for Activity#requestPermissions for more details.
            argActivity.requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    TopActivity.PERMISSION_TO_IMPORT_EXPORT);

            return false;
        }

        return true;
    }

    @Override
    public void onClick(View v) {
        Log.d(TAG, "Onclikc");
    }

    @Override
    public void topfound(int i) {
        Log.d(TAG, "dfdsf");
        //mContainerView.setPadding(0,i,0,0);
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) mGridView.getLayoutParams();
        FrameLayout.LayoutParams params2 = (FrameLayout.LayoutParams) mEmptySubscrptionList.getLayoutParams();
        params2.topMargin = i;
        params.topMargin = i;
        mGridView.setLayoutParams(params);
        mEmptySubscrptionList.setLayoutParams(params);
    }
}
