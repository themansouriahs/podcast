package org.bottiger.podcast;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ProgressBar;

import org.bottiger.podcast.activities.discovery.DiscoverySearchAdapter;
import org.bottiger.podcast.activities.feedview.FeedActivity;
import org.bottiger.podcast.flavors.CrashReporter.VendorCrashReporter;
import org.bottiger.podcast.provider.ISubscription;
import org.bottiger.podcast.provider.SlimImplementations.SlimSubscription;
import org.bottiger.podcast.utils.IntUtils;
import org.bottiger.podcast.utils.StrUtils;
import org.bottiger.podcast.utils.UIUtils;
import org.bottiger.podcast.views.dialogs.DialogSearchDirectory;
import org.bottiger.podcast.webservices.directories.IDirectoryProvider;
import org.bottiger.podcast.webservices.directories.ISearchParameters;
import org.bottiger.podcast.webservices.directories.ISearchResult;
import org.bottiger.podcast.webservices.directories.audiosearch.AudioSearch;
import org.bottiger.podcast.webservices.directories.generic.GenericDirectory;
import org.bottiger.podcast.webservices.directories.generic.GenericSearchParameters;
import org.bottiger.podcast.webservices.directories.gpodder.GPodder;
import org.bottiger.podcast.webservices.directories.itunes.ITunes;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import okhttp3.OkHttpClient;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

import static org.bottiger.podcast.webservices.directories.IDirectoryProvider.BY_AUTHOR;
import static org.bottiger.podcast.webservices.directories.IDirectoryProvider.POPULAR;
import static org.bottiger.podcast.webservices.directories.IDirectoryProvider.SEARCH_RESULTS;
import static org.bottiger.podcast.webservices.directories.IDirectoryProvider.TRENDING;

/**
 * Created by apl on 13-04-2015.
 */
public class DiscoveryFragment extends Fragment implements SharedPreferences.OnSharedPreferenceChangeListener,
        AdapterView.OnItemSelectedListener {

    private static final String TAG = DiscoveryFragment.class.getSimpleName();

    // Match with entries_webservices_discovery_engine
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({ GPODDER_INDEX, ITUNES_INDEX, AUDIOSEARCH_INDEX})
    public @interface SearchEngine {}
    private static final int GPODDER_INDEX = 0;
    private static final int ITUNES_INDEX  = 1;
    private static final int AUDIOSEARCH_INDEX  = 2;

    public static final int[] ENGINE_IDS = {GPODDER_INDEX, ITUNES_INDEX, AUDIOSEARCH_INDEX};
    public static final @StringRes int[] ENGINE_RES = {GPodder.getNameRes(), ITunes.getNameRes(), AudioSearch.getNameRes()};

    private static final int HANDLER_WHAT_SEARCH   = 27407; // whatever
    private static final int HANDLER_WHAT_CANCEL   = 27408; // whatever

    private static final int HANDLER_DELAY = 1000; // ms
    private static final String HANDLER_QUERY = "query";
    private SearchHandler mSearchHandler = new SearchHandler(this);

    // Spinner values
    private static final int SPINNER_BY_AUTHOR_POSITION = 0;

    private android.support.v7.widget.SearchView mSearchView;
    private AppCompatSpinner mSpinner;
    private ImageButton mSearchEngineButton;
    private RecyclerView mResultsRecyclerView;
    private ProgressBar mProgress;

    private ArrayAdapter<String> mSpinnerAdapter;
    private DiscoverySearchAdapter mResultsAdapter;

    private String mDiscoveryEngineKey;

    private Subscription mRxSubscription = null;

    private IDirectoryProvider mDirectoryProvider = null;
    private IDirectoryProvider.Callback mSearchResultCallback = new IDirectoryProvider.Callback() {
        @Override
        public void result(@NonNull ISearchResult argResult) {
            Log.d(TAG, "Search results for: " + argResult.getSearchQuery());
            ArrayList<ISubscription> subscriptions = new ArrayList<>();
            for (ISubscription subscription : argResult.getResults()) {
                subscriptions.add(subscription);
                subscription.fetchImage(getContext());

                // Log the subscription
                SoundWaves.getAppContext(getActivity()).getAnalystics().logFeed(subscription.getURLString(), false);
            }
            mResultsAdapter.setDataset(subscriptions);

            if (argResult.getSearchQuery().equals(mSearchView.getQuery().toString()) )
                mProgress.setVisibility(View.GONE);
        }

        @Override
        public void error(@NonNull Exception argException) {
            Log.e(TAG, "Search failed", argException);
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mProgress.setVisibility(View.GONE);
                }
            });
        }
    };

    @Override
    public void onAttach(final Context context) {
        PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext()).registerOnSharedPreferenceChangeListener(this);
        mDiscoveryEngineKey = getResources().getString(R.string.pref_webservices_discovery_engine_key);

        super.onAttach(context);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        setHasOptionsMenu(true);
        return inflater.inflate(R.layout.discovery_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mSpinner = (AppCompatSpinner) view.findViewById(R.id.search_spinner);

        mSearchEngineButton = (ImageButton) view.findViewById(R.id.discovery_searchIcon);
        mSearchEngineButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<Integer> ids = new LinkedList<>();
                List<Integer> res = new LinkedList<>();

                for (int i = 0; i < ENGINE_IDS.length; i++) {
                    @SearchEngine int id = ENGINE_IDS[i];
                    if (isEnabled(id, getContext())) {
                        ids.add(id);
                        res.add(ENGINE_RES[i]);
                    }
                }

                int[] enabled_ids = IntUtils.toIntArray(ids);
                int[] enabled_res = IntUtils.toIntArray(res);

                DialogSearchDirectory dialogSearchDirectory = DialogSearchDirectory.newInstance(enabled_ids, enabled_res);
                dialogSearchDirectory.show(getFragmentManager(), "SearchEnginePicker"); // NoI18N
            }
        });

        mSearchView = (android.support.v7.widget.SearchView) view.findViewById(R.id.discovery_searchView);
        mSearchView.setOnQueryTextListener(new android.support.v7.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchviewQueryChanged(query, false);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                searchviewQueryChanged(newText, true);
                return false;
            }
        });
        mSearchView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.discovery_searchView:
                        mSearchView.onActionViewExpanded();
                        break;
                }
            }
        });

        // requires both mSearchEngineButton and mSearchView to be NonNull
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
        onSharedPreferenceChanged(sharedPreferences, mDiscoveryEngineKey);

        mSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinner.setAdapter(mSpinnerAdapter);
        mSpinner.setOnItemSelectedListener(this);

        mResultsAdapter = new DiscoverySearchAdapter(getActivity());
        mResultsAdapter.setHasStableIds(true);

        mResultsRecyclerView = (RecyclerView) view.findViewById(R.id.search_result_view);
        mResultsRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mResultsRecyclerView.setHasFixedSize(true);
        mResultsRecyclerView.setAdapter(mResultsAdapter);

        mProgress = (ProgressBar) view.findViewById(R.id.discovery_progress);

        populateRecommendations();

        mRxSubscription = SoundWaves
                .getAppContext(getContext())
                .getLibraryInstance()
                .mSubscriptionsChangeObservable
                .onBackpressureLatest()
                .ofType(org.bottiger.podcast.provider.Subscription.class)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<org.bottiger.podcast.provider.Subscription>() {
                    @Override
                    public void call(org.bottiger.podcast.provider.Subscription argSubscription) {
                        mResultsAdapter.populateSubscribedUrls();
                        mResultsAdapter.notifyDataSetChanged();
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        VendorCrashReporter.report("subscribeError" , throwable.toString());
                        Log.d(TAG, "error: " + throwable.toString());
                    }
                });
    }

    @Override
    public void onDestroyView () {
        super.onDestroyView();
        if (mRxSubscription != null && !mRxSubscription.isUnsubscribed()) {
            mRxSubscription.unsubscribe();
        }
    }

    private void performSearch(@NonNull String argQuery) {
        if (TextUtils.isEmpty(argQuery))
            return;

        updateSpinnerValues(SEARCH_RESULTS, argQuery);
        //updateLabel(argQuery);

        ISearchParameters searchParameters = new GenericSearchParameters();
        searchParameters.addSearchTerm(argQuery);

        mDirectoryProvider.search(searchParameters, mSearchResultCallback);
        mProgress.setVisibility(View.VISIBLE);
    }

    private void abortSearch() {
        mDirectoryProvider.abortSearch();
        mProgress.setVisibility(View.GONE);
    }

    private void fetchTrending() {
        mDirectoryProvider.toplist(mSearchResultCallback);
    }

    private void fetchPopular() {
        mDirectoryProvider.toplist(mSearchResultCallback);
    }

    private void searchviewQueryChanged(@Nullable String argQuery, boolean argDelaySearch) {

        Log.d(TAG, "searchviewQueryChanged: " + argQuery + " delay: " + argDelaySearch);

        mSpinner.setSelection(SPINNER_BY_AUTHOR_POSITION);

        mSearchHandler.removeMessages(HANDLER_WHAT_SEARCH);

        if (TextUtils.isEmpty(argQuery)) {
            mProgress.setVisibility(View.GONE);
            mSearchEngineButton.setVisibility(View.VISIBLE);
            populateRecommendations();
            return;
        }

        mSearchEngineButton.setVisibility(View.GONE);

        Message msg = createHandlerMessage(argQuery, HANDLER_WHAT_SEARCH);
        if (argDelaySearch) {
            mSearchHandler.sendMessageDelayed(msg, HANDLER_DELAY);
        } else {
            instantMessage(msg);
        }
    }

    private void instantMessage(@NonNull Message argMsg) {
        Message abortMsg = createHandlerMessage("", HANDLER_WHAT_CANCEL); // The query is not used when we send a cancel msg

        mSearchHandler.sendMessage(abortMsg);
        mSearchHandler.sendMessage(argMsg);
        mSearchView.clearFocus();
    }

    private Message createHandlerMessage(String argQuery, int argWhat) {
        Bundle bundle = new Bundle();
        bundle.putString(HANDLER_QUERY, argQuery);
        Message msg = new Message();
        msg.what = argWhat;
        msg.setData(bundle);
        return msg;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (mDiscoveryEngineKey.equals(key)) {
            @SearchEngine int searchEngine = Integer.valueOf(sharedPreferences.getString(mDiscoveryEngineKey, Integer.toString(getDefaultSearchEngine())));

            setSearchEngine(searchEngine);

            // FIXME: I do not fully understand why this is needed
            // bug: 5600e81488f8ad5351d0bdd7
            if (isAdded()) {
                updateSpinnerValues(BY_AUTHOR, null);
                setQueryHint();
            }
        }
    }

    public static boolean isEnabled(@DiscoveryFragment.SearchEngine int argEngine, @NonNull Context argContext) {
        IDirectoryProvider directoryProvider = null;
        switch (argEngine) {
            case DiscoveryFragment.GPODDER_INDEX:
                directoryProvider = new GPodder(argContext);
                break;
            case DiscoveryFragment.ITUNES_INDEX:
                directoryProvider = new ITunes(argContext);
                break;
            case DiscoveryFragment.AUDIOSEARCH_INDEX:
                directoryProvider = new AudioSearch(argContext);
                break;
        }

        return directoryProvider != null && directoryProvider.isEnabled();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FeedActivity.FEED_ACTIVITY_CANCELED) {
            UIUtils.disPlayBottomSnackBar(getView(), R.string.feed_activity_loading_canceled, null, false);
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

        String value = mSpinnerAdapter.getItem(position);

        if (position == 0) {
            searchviewQueryChanged(null, false);
            return;
        }

        if (value.equals(getListModeName(POPULAR))) {
            mProgress.setVisibility(View.VISIBLE);
            fetchPopular();
            return;
        }

        if (value.equals(getListModeName(TRENDING))) {
            mProgress.setVisibility(View.VISIBLE);
            fetchTrending();
            return;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        return;
    }

    private void setSearchEngine(@SearchEngine int argSearchEngine) {

        Context context = getContext();

        if (context == null) {
            VendorCrashReporter.report(TAG, "Context is null");
            return;
        }

        switch (argSearchEngine) {
            case GPODDER_INDEX: {
                mDirectoryProvider = new GPodder(context);
                mSearchEngineButton.setImageResource(R.drawable.discovery_gpodder);
                break;
            }
            case ITUNES_INDEX: {
                mDirectoryProvider = new ITunes(context);
                mSearchEngineButton.setImageResource(R.drawable.discovery_itunes);
                break;
            }
            case AUDIOSEARCH_INDEX: {
                mDirectoryProvider = new AudioSearch(context);
                mSearchEngineButton.setImageResource(R.drawable.audiosearch_logo);
                break;
            }
        }

        boolean engineSupported = mDirectoryProvider != null && mDirectoryProvider.isEnabled();

        if (!engineSupported) {
            mDirectoryProvider = new GPodder(context);
            mSearchEngineButton.setImageResource(R.drawable.discovery_gpodder);
        }

        updateSpinnerValues(GenericDirectory.defaultMode(), null);
    }

    private int getDefaultSearchEngine() {
        return BuildConfig.LIBRE_MODE ? GPODDER_INDEX : ITUNES_INDEX;
    }

    private void setQueryHint() {
        String queryHint = String.format(getResources().getString(R.string.search_query_hint), mDirectoryProvider.getName());
        mSearchView.setQueryHint(queryHint);
    }

    private void updateSpinnerValues(int argFirstItem, @Nullable String argQuery) {
        String[] array = supportedModeNames();
        array[0] = getListModeName(argFirstItem);

        if (!TextUtils.isEmpty(argQuery)) {
            String resultLabel = getResources().getString(R.string.discovery_search_results);
            array[0] = StrUtils.fromHtmlCompat(String.format(resultLabel, argQuery));
        }

        ArrayList<String> lst = new ArrayList<>(Arrays.asList(array));

        if (mSpinnerAdapter == null) {
            mSpinnerAdapter = new ArrayAdapter<>(getContext(), R.layout.discovery_spinner_item, lst);
        } else {
            mSpinnerAdapter.clear();
            for (int i = 0; i < array.length; i++) {
                mSpinnerAdapter.insert(array[i], i);
            }
        }
    }

    private String[] supportedModeNames() {

        if (mDirectoryProvider == null) {
            return new String[]{ getListModeName(GenericDirectory.defaultMode()) };
        }

        int[] modes = mDirectoryProvider.supportedListModes();
        String[] names = new String[modes.length+1];

        names[0] = getListModeName(BY_AUTHOR);

        for (int i = 0; i < modes.length; i++) {
            names[i+1] = getListModeName(modes[i]);
        }

        return names;
    }

    private String getListModeName(@StringRes int argRes) {
        return getString(argRes);
    }

    /**
     * Create a handler to perform the search query
     */
    private static class SearchHandler extends Handler {
        private final WeakReference<DiscoveryFragment> mFragment;

        SearchHandler(DiscoveryFragment fragment) {
            mFragment = new WeakReference<>(fragment);
        }

        @Override
        public void handleMessage(Message msg) {
            DiscoveryFragment fragment = mFragment.get();
            if (fragment != null) {
                switch (msg.what) {
                    case HANDLER_WHAT_SEARCH: {
                        String query = msg.getData().getString(HANDLER_QUERY);
                        Log.d(TAG, "Perform query: " + query);
                        if (query != null)
                            fragment.performSearch(query);
                        break;
                    }
                    case HANDLER_WHAT_CANCEL: {
                        Log.d(TAG, "abort query:");
                        fragment.abortSearch();
                        break;
                    }
                }
            }
        }
    }

    private void populateRecommendations() {

        ArrayList<ISubscription> subscriptions = new ArrayList<>();

        try {
            URL url1 = new URL("http://www.hpmorpodcast.com/?feed=rss2");
            URL url2 = new URL("http://www.npr.org/rss/podcast.php?id=510289");
            URL url3 = new URL("http://leoville.tv/podcasts/sn.xml");
            URL url4 = new URL("http://feeds.themoth.org/themothpodcast");
            URL url5 = new URL("http://www.heritageradionetwork.org/programs/51-Cooking-Issues.xml");

            ISubscription sub1 = new SlimSubscription("Harry Potter and the Methods of Rationality", url1, "http://www.hpmorpodcast.com/wp-content/uploads/powerpress/HPMoR_Podcast_new.jpg");
            ISubscription sub2 = new SlimSubscription("Planet Money", url2, "http://media.npr.org/images/podcasts/primary/icon_510289-d5d79b164ba7670399f0287529ce31a94523b224.jpg?s=500");
            ISubscription sub3 = new SlimSubscription("Security Now", url3, "http://twit.cachefly.net/coverart/sn/sn600audio.jpg");
            ISubscription sub4 = new SlimSubscription("The Moth Podcast", url4, "http://cdn.themoth.prx.org/wp-content/uploads/powerpress/moth_podcast_prx_480x480.jpeg");
            ISubscription sub5 = new SlimSubscription("Cooking Issues", url5, "http://s3.amazonaws.com/hrn/logos/51/original/Cooking-Issues.jpg?1380728747");

            String locale = StrUtils.getUserCountry(getContext());
            if ("dk".equals(locale)) {
                URL url_hyggenord = new URL("http://hyggenord.dk/feed/podcast");
                ISubscription sub_hyggenord = new SlimSubscription("Hyggenørderi", url_hyggenord, "http://hyggenord.dk/wp-content/uploads/2015/11/IMG_20151104_195039.jpg");
                subscriptions.add(sub_hyggenord);
            }

            subscriptions.add(sub1);
            subscriptions.add(sub2);
            subscriptions.add(sub3);
            subscriptions.add(sub4);
            subscriptions.add(sub5);
        } catch (MalformedURLException mue) {
            return;
        }

        updateSpinnerValues(GenericDirectory.defaultMode(), null);
        mResultsAdapter.setDataset(subscriptions);
    }

    @RestrictTo(RestrictTo.Scope.TESTS)
    public OkHttpClient getOkHttpClient() {
        return mDirectoryProvider.getOkHttpClient();
    }
}
