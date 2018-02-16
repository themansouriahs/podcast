package org.bottiger.podcast;

import android.Manifest;
import android.app.Activity;
import android.arch.lifecycle.LifecycleFragment;
import android.arch.lifecycle.Observer;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v7.util.SortedList;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import org.bottiger.podcast.activities.main.PreloadGridLayoutManager;
import org.bottiger.podcast.activities.openopml.OPMLImportExportActivity;
import org.bottiger.podcast.activities.openopml.OpenOpmlFromIntentActivity;
import org.bottiger.podcast.adapters.SubscriptionAdapter;
import org.bottiger.podcast.flavors.CrashReporter.VendorCrashReporter;
import org.bottiger.podcast.model.Library;
import org.bottiger.podcast.model.events.SubscriptionChanged;
import org.bottiger.podcast.provider.Subscription;
import org.bottiger.podcast.utils.OPMLImportExport;
import org.bottiger.podcast.utils.SDCardManager;

import java.io.File;
import java.io.IOException;
import java.util.List;

import io.reactivex.disposables.Disposable;

import static android.support.annotation.RestrictTo.Scope.TESTS;

public class SubscriptionsFragment extends Fragment {

    private static final String TAG = "SubscriptionsFragment";

    private static final boolean SHARE_ANALYTICS_DEFAULT = !BuildConfig.LIBRE_MODE;
    private static final boolean SHARE_CLOUD_DEFAULT = false;
    private static final String EXTRAS_CODE = "path";
    private static final String OPML_SUBS_LIST_EXTRA_CODE = "EXTRACODE_SUBS_LIST";
    public static final int RESULT_IMPORT = 201;
    public static final int RESULT_EXPORT = 202;
    public static final int RESULT_EXPORT_TO_CLIPBOARD = 203;
    public static final String EXPORT_FILENAME = "/podcast_export.opml";

    private static final int OPML_ACTIVITY_STATUS_CODE = 999; //This number is needed but it can be any number ^^

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

    private Disposable mRxSubscriptionChanged;

    private SharedPreferences shareprefs;
    private static String PREF_SUBSCRIPTION_COLUMNS;
    public File file;

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

        mContainerView = (FrameLayout) inflater.inflate(R.layout.subscription_fragment, container, false);

        // Empty View
        mEmptySubscrptionList = mContainerView.findViewById(R.id.subscription_empty);
        mEmptySubscrptionImportOPMLButton = mContainerView.findViewById(R.id.import_opml_button);

        mGridContainerView = mContainerView.findViewById(R.id.subscription_grid_container);

        //RecyclerView
        mAdapter = createAdapter();

        List<Subscription> subscriptionsList = mLibrary.getLiveSubscriptions().getValue();
        int listSize = subscriptionsList == null ? 0 : subscriptionsList.size();
        setSubscriptionFragmentLayout(listSize);

        mGridView = mContainerView.findViewById(R.id.gridview);


        mGridLayoutmanager = new PreloadGridLayoutManager(getActivity(), numberOfColumns());
        mGridView.setHasFixedSize(true);
        mGridView.setLayoutManager(mGridLayoutmanager);
        mGridView.setAdapter(mAdapter);

        mLibrary.getLiveSubscriptions().observe(this, subscriptionSortedList -> {
            Log.v(TAG, "Recieved Subscription event: ");
            if (subscriptionSortedList == null) {
                return;
            }

            setSubscriptionFragmentLayout(subscriptionSortedList.size());

            mGridLayoutmanager.setSpanCount(numberOfColumns());
            mAdapter.setDataset(subscriptionSortedList);

            if (!mGridView.isComputingLayout()) {
                mAdapter.notifyDataSetChanged();
            }
        });

        mRxSubscriptionChanged = SoundWaves.getRxBus2()
                .toFlowableCommon()
                .ofType(SubscriptionChanged.class)
                .filter(subscriptionChanged ->
                        subscriptionChanged.getAction() == SubscriptionChanged.CHANGED ||
                        subscriptionChanged.getAction() == SubscriptionChanged.LOADED)
                .subscribe(itemChangedEvent -> {
                    Log.v(TAG, "Refreshing Subscription: " + itemChangedEvent.getId());
                    // Update the subscription fragment when a image is updated in an subscription
                    List<Subscription> subscriptions = mLibrary.getLiveSubscriptions().getValue();
                    Subscription subscription = mLibrary.getSubscription(itemChangedEvent.getId());

                    int index = subscriptions.indexOf(subscription); // doesn't work
                    for (int i = 0; i < subscriptions.size(); i++) {
                        Subscription currentSubscription = subscriptions.get(i);
                        if (subscription != null && subscription.equals(currentSubscription)) {
                            index = i;
                            break;
                        }
                    }

                    if (!mGridView.isComputingLayout()) {
                        mAdapter.notifyItemChanged(index);
                    }
                }, throwable -> {
                    VendorCrashReporter.handleException(throwable);
                    Log.wtf(TAG, "Missing back pressure. Should not happen anymore :(");
                });

        return mContainerView;

    }

    @Override
    public void onDestroyView() {
        if (mRxSubscriptionChanged != null && !mRxSubscriptionChanged.isDisposed()) {
            mRxSubscriptionChanged.dispose();
        }
        super.onDestroyView();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mEmptySubscrptionImportOPMLButton.setOnClickListener(v -> {
            if (getActivity() instanceof TopActivity) {
                Intent i = new Intent(mActivity.getApplicationContext(), OPMLImportExportActivity.class);
                startActivityForResult(i, OPML_ACTIVITY_STATUS_CODE);
            } else {
                Log.wtf(TAG, "getActivity() is not an instance of TopActivity. Please investigate"); // NoI18N
            }
        });

        mAdapter.setDataset(mLibrary.getLiveSubscriptions().getValue());
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
            ((SubscriptionAdapter) mGridView.getAdapter()).setNumberOfColumns(numberOfColumns());
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
            case Library.SCORE: {
                idres = R.id.menu_order_score;
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
                Intent i = new Intent(mActivity.getApplicationContext(), OPMLImportExportActivity.class);
                startActivityForResult(i, OPML_ACTIVITY_STATUS_CODE);
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
            case R.id.menu_order_score: {
                setSortOrder(item, Library.SCORE);
                break;
            }

        }
        return super.onOptionsItemSelected(item);
    }

    private void setSortOrder(MenuItem item, @Library.SortOrder int argSortOrder) {
        mLibrary.setSubscriptionOrder(argSortOrder);
        item.setChecked(true);

        mAdapter.setDataset(mLibrary.getLiveSubscriptions().getValue());
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
            if (mLibrary.getSubscriptionCount() > 3) {
                numColumns = 3;
            } else {
                numColumns = 2;
            }
        }

        return numColumns;
    }

    /**
     *
     * @return True if the permissions are granted
     */
    private static boolean checkPermission(@NonNull TopActivity argActivity) {
        if (Build.VERSION.SDK_INT >= 23 &&
                (argActivity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                        argActivity.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)) {
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


    /*
       This method captures the execution of the opml_import_export activity, to return the value and parse
            the result of the opml file
           If the activity OPMLImportExportActivity returns a "RETURN_EXPORT" it means the user pressed the export button,
                in the other way, it returns a path
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == OPML_ACTIVITY_STATUS_CODE) {
            OPMLImportExportActivity.handleResult(getActivity(), requestCode, resultCode, data);
        }
    }

    @RestrictTo(TESTS)
    public static int getOPMLStatusCode() {
        return OPML_ACTIVITY_STATUS_CODE;
    }
}
