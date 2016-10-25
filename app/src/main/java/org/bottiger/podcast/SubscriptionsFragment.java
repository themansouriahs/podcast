package org.bottiger.podcast;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.util.SortedList;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import org.bottiger.podcast.adapters.SubscriptionAdapter;
import org.bottiger.podcast.flavors.CrashReporter.VendorCrashReporter;
import org.bottiger.podcast.model.Library;
import org.bottiger.podcast.model.events.SubscriptionChanged;
import org.bottiger.podcast.provider.Subscription;
import org.bottiger.podcast.utils.PreferenceHelper;
import org.bottiger.podcast.views.ContextMenuRecyclerView;
import org.bottiger.podcast.views.dialogs.DialogOPML;

import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class SubscriptionsFragment extends Fragment implements View.OnClickListener {

    private static final String TAG = "SubscriptionsFragment";

    private static final boolean SHARE_ANALYTICS_DEFAULT = !BuildConfig.LIBRE_MODE;
    private static final boolean SHARE_CLOUD_DEFAULT = false;

     /**
     1 = Auto
     2 = list
     3 = 2 columns
     4 = 3 columns
     */
    private final int VIEW_TYPE_AUTO = 5;
    private final int VIEW_TYPE_LIST = 6;
    private final int VIEW_TYPE_2_COLUMNS = 7;
    private final int VIEW_TYPE_3_COLUMNS = 8;

    private Boolean mShowEmptyView = null;

	private RecyclerView mGridView;

    private Library mLibrary;

    private GridLayoutManager mGridLayoutmanager;
    private RelativeLayout mEmptySubscrptionList;
    private Button mEmptySubscrptionImportOPMLButton;
    private SubscriptionAdapter mAdapter;
    private FrameLayout mGridContainerView;

    private Activity mActivity;
    private FrameLayout mContainerView;

    private rx.Subscription mRxSubscription;
    private rx.Subscription mRxSubscriptionChanged;

    private SharedPreferences shareprefs;
    private static String PREF_SUBSCRIPTION_COLUMNS;

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
        mLibrary = SoundWaves.getAppContext(getContext()).getLibraryInstance();
        PREF_SUBSCRIPTION_COLUMNS = mActivity.getResources().getString(R.string.pref_subscriptions_columns_key);
        shareprefs = PreferenceManager.getDefaultSharedPreferences(mActivity.getApplicationContext());
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

        //RecyclerView
        mAdapter = createAdapter();
        setSubscriptionFragmentLayout(mLibrary.getSubscriptions().size());

		mGridView = (RecyclerView) mContainerView.findViewById(R.id.gridview);


        mGridLayoutmanager = new GridLayoutManager(getActivity(), numberOfColumns());
        mGridView.setHasFixedSize(true);
        mGridView.setLayoutManager(mGridLayoutmanager);
        mGridView.setAdapter(mAdapter);

        mRxSubscription = mLibrary.mSubscriptionsChangeObservable
                .onBackpressureLatest()
                .ofType(Subscription.class)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Subscription>() {
                    @Override
                    public void call(Subscription argSubscription) {
                        Log.v(TAG, "Recieved Subscription event: " + argSubscription.getId());
                        SortedList<Subscription> subscriptions = mLibrary.getSubscriptions();
                        setSubscriptionFragmentLayout(subscriptions.size());

                        mGridLayoutmanager.setSpanCount(numberOfColumns());
                        mAdapter.setDataset(subscriptions);

                        if (!mGridView.isComputingLayout()) {
                            mAdapter.notifyDataSetChanged();
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        VendorCrashReporter.report("subscribeError" , throwable.toString());
                        Log.d(TAG, "error: " + throwable.toString());
                    }
                });


        mRxSubscriptionChanged = SoundWaves.getRxBus()
                .toObserverable()
                .onBackpressureBuffer(10000)
                .ofType(SubscriptionChanged.class)
                .filter(new Func1<SubscriptionChanged, Boolean>() {
                    @Override
                    public Boolean call(SubscriptionChanged subscriptionChanged) {
                        return subscriptionChanged.getAction()==SubscriptionChanged.CHANGED;
                    }
                })
                .subscribe(new Action1<SubscriptionChanged>() {
                    @Override
                    public void call(SubscriptionChanged itemChangedEvent) {
                        Log.v(TAG, "Refreshing Subscription: " + itemChangedEvent.getId());
                        // Update the subscription fragment when a image is updated in an subscription
                        SortedList<Subscription> subscriptions = mLibrary.getSubscriptions();
                        Subscription subscription = mLibrary.getSubscription(itemChangedEvent.getId());
                        int index = subscriptions.indexOf(subscription);

                        if (!mGridView.isComputingLayout()) {
                            mAdapter.notifyItemChanged(index);
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        VendorCrashReporter.handleException(throwable);
                        Log.wtf(TAG, "Missing back pressure. Should not happen anymore :(");
                    }
                });

        mAdapter.setDataset(mLibrary.getSubscriptions());

		return mContainerView;

	}

    @Override
    public void onDestroyView () {
        super.onDestroyView();
        if (mRxSubscription != null && !mRxSubscription.isUnsubscribed()) {
            mRxSubscription.unsubscribe();
        }
        if (mRxSubscriptionChanged != null && !mRxSubscriptionChanged.isUnsubscribed()) {
            mRxSubscriptionChanged.unsubscribe();
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
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

        @IdRes int idres = 0;

        switch (mLibrary.getSubscriptionOrder()) {
            case Library.ALPHABETICALLY: {
                idres = R.id.menu_order_alphabetically;
                break;
            }
            case Library.ALPHABETICALLY_REVERSE: {
                idres = R.id.menu_order_alphabetically_reverse;
                break;
            }
            case Library.LAST_UPDATE: {
                idres = R.id.menu_order_last_updated;
                break;
            }
            case Library.NEW_EPISODES: {
                idres = R.id.menu_order_new_count;
                break;
            }
            default: {
                idres = R.id.menu_order_alphabetically;
            }
        }

        MenuItem item = menu.findItem(idres);
        item.setChecked(true);

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
                break;
            }
            case R.id.menu_refresh_all_subscriptions: {
                SoundWaves.getAppContext(getContext()).getRefreshManager().refreshAll();
                break;
            }
            case R.id.menu_order_alphabetically: {
                setSortOrder(item, Library.ALPHABETICALLY);
                break;
            }
            case R.id.menu_order_alphabetically_reverse: {
                setSortOrder(item, Library.ALPHABETICALLY_REVERSE);
                break;
            }
            case R.id.menu_order_last_updated: {
                setSortOrder(item, Library.LAST_UPDATE);
                break;
            }
            case R.id.menu_order_new_count: {
                setSortOrder(item, Library.NEW_EPISODES);
                break;
            }

        }
        return super.onOptionsItemSelected(item);
    }

    private void setSortOrder(MenuItem item, @Library.SortOrder int argSortOrder) {
        mLibrary.setSubscriptionOrder(argSortOrder);
        item.setChecked(true);

        mAdapter.setDataset(mLibrary.getSubscriptions());
        mAdapter.notifyDataSetChanged();
    }

    private SubscriptionAdapter createAdapter() {
        return new SubscriptionAdapter(getActivity(), numberOfColumns());
    }

    private int numberOfColumns() {
        String number = shareprefs.getString(PREF_SUBSCRIPTION_COLUMNS, "5");
        int intVal = Integer.parseInt(number);
        int numColumns = -1;

        if (intVal == VIEW_TYPE_LIST) {
            numColumns = 1;
        }

        if (intVal == VIEW_TYPE_2_COLUMNS) {
            numColumns = 2;
        }

        if (intVal == VIEW_TYPE_3_COLUMNS) {
            numColumns = 3;
        }

        if (numColumns == -1) {
            if (mLibrary.getSubscriptions().size() > 3) {
                numColumns = 3;
            } else {
                numColumns = 2;
            }
        }

        return numColumns;
    }

    static void openImportExportDialog(@NonNull TopActivity argActivity) {

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
}
