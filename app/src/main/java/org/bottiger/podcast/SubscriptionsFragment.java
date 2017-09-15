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

import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

import static android.support.annotation.RestrictTo.Scope.TESTS;

public class SubscriptionsFragment extends LifecycleFragment {

    private static final String TAG = "SubscriptionsFragment";

    private static final boolean SHARE_ANALYTICS_DEFAULT = !BuildConfig.LIBRE_MODE;
    private static final boolean SHARE_CLOUD_DEFAULT = false;
    private static final int OPML_ACTIVITY_STATUS_CODE = 999; //This number is needed but it can be any number ^^
    private static final String EXTRAS_CODE = "path";
    private static final String OPML_SUBS_LIST_EXTRA_CODE = "EXTRACODE_SUBS_LIST";
    public static final int RESULT_IMPORT = 201;
    public static final int RESULT_EXPORT = 202;
    public static final int RESULT_EXPORT_TO_CLIPBOARD = 203;
    private static final String EXPORT_FILENAME = "/podcast_export.opml";

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

    private rx.Subscription mRxSubscriptionChanged;

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

        SortedList<Subscription> subscriptionsList = mLibrary.getLiveSubscriptions().getValue();
        int listSize = subscriptionsList == null ? 0 : subscriptionsList.size();
        setSubscriptionFragmentLayout(listSize);

        mGridView = mContainerView.findViewById(R.id.gridview);


        mGridLayoutmanager = new PreloadGridLayoutManager(getActivity(), numberOfColumns());
        mGridView.setHasFixedSize(true);
        mGridView.setLayoutManager(mGridLayoutmanager);
        mGridView.setAdapter(mAdapter);

        mLibrary.getLiveSubscriptions().observe(this, new Observer<SortedList<Subscription>>() {
            @Override
            public void onChanged(@Nullable SortedList<Subscription> subscriptionSortedList) {
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
            }
        });

        mRxSubscriptionChanged = SoundWaves.getRxBus()
                .toObserverable()
                .onBackpressureBuffer(10000)
                .ofType(SubscriptionChanged.class)
                .filter(new Func1<SubscriptionChanged, Boolean>() {
                    @Override
                    public Boolean call(SubscriptionChanged subscriptionChanged) {
                        return subscriptionChanged.getAction() == SubscriptionChanged.CHANGED;
                    }
                })
                .subscribe(new Action1<SubscriptionChanged>() {
                    @Override
                    public void call(SubscriptionChanged itemChangedEvent) {
                        Log.v(TAG, "Refreshing Subscription: " + itemChangedEvent.getId());
                        // Update the subscription fragment when a image is updated in an subscription
                        SortedList<Subscription> subscriptions = mLibrary.getSubscriptions();
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
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        VendorCrashReporter.handleException(throwable);
                        Log.wtf(TAG, "Missing back pressure. Should not happen anymore :(");
                    }
                });


        return mContainerView;

    }

    @Override
    public void onDestroyView() {
        if (mRxSubscriptionChanged != null && !mRxSubscriptionChanged.isUnsubscribed()) {
            mRxSubscriptionChanged.unsubscribe();
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

        mEmptySubscrptionImportOPMLButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getActivity() instanceof TopActivity) {
                    Intent i = new Intent(mActivity.getApplicationContext(), OPMLImportExportActivity.class);
                    startActivityForResult(i, OPML_ACTIVITY_STATUS_CODE);
                } else {
                    Log.wtf(TAG, "getActivity() is not an instance of TopActivity. Please investigate"); // NoI18N
                }
            }
        });

        mAdapter.setDataset(mLibrary.getSubscriptions());
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
        OPMLImportExport importExport = new OPMLImportExport(getActivity());

        if (requestCode == OPML_ACTIVITY_STATUS_CODE) {
            if (resultCode == RESULT_IMPORT){
                //Log.d("OPML", data.getData().getPath());// NoI18N
                Uri extraData = data.getData();

                if (ActivityCompat.checkSelfPermission(mActivity, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(mActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }

                Log.d(TAG, "IMPORT SUBSCRIPTIONS. File: " + extraData);// NoI18N
                Intent selectSubs = new Intent(mActivity.getApplicationContext(), OpenOpmlFromIntentActivity.class);
                selectSubs.setData(extraData);
                startActivity(selectSubs);

            }
            else if (resultCode == RESULT_EXPORT) {
                String export_dir = null;
                try {
                    export_dir = SDCardManager.getExportDir();
                } catch (IOException e) {
                    VendorCrashReporter.report("EXPORT FAIL", "Cannot export OPML file");
                }
                Log.d(TAG, "EXPORT SUBSCRIPTIONS");// NoI18N
                Log.d(TAG, "Export to: " + export_dir + EXPORT_FILENAME);// NoI18N
                importExport.exportSubscriptions(new File(export_dir + EXPORT_FILENAME));
                Toast.makeText(getActivity().getApplicationContext(), getString(R.string.opml_exported_to_toast) + export_dir + EXPORT_FILENAME, Toast.LENGTH_SHORT).show();
            } else if (resultCode == RESULT_EXPORT_TO_CLIPBOARD) {
                Log.d(TAG, "EXPORT SUBSCRIPTIONS TO CLIPBOARD");// NoI18N

                importExport.exportSubscriptionsToClipboard();
                Toast.makeText(getActivity().getApplicationContext(), getString(R.string.opml_exported_to_clipboard_toast), Toast.LENGTH_SHORT).show();
            }
        }
    }

    @RestrictTo(TESTS)
    public static int getOPMLStatusCode() {
        return OPML_ACTIVITY_STATUS_CODE;
    }
}
