package org.bottiger.podcast;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.SearchView;
import android.widget.TextView;

import org.bottiger.podcast.adapters.DiscoverySearchAdapter;
import org.bottiger.podcast.provider.ISubscription;
import org.bottiger.podcast.provider.SlimImplementations.SlimSubscription;
import org.bottiger.podcast.provider.Subscription;
import org.bottiger.podcast.views.dialogs.DialogSearchDirectory;
import org.bottiger.podcast.webservices.directories.IDirectoryProvider;
import org.bottiger.podcast.webservices.directories.ISearchParameters;
import org.bottiger.podcast.webservices.directories.ISearchResult;
import org.bottiger.podcast.webservices.directories.generic.GenericSearchParameters;
import org.bottiger.podcast.webservices.directories.gpodder.GPodder;
import org.bottiger.podcast.webservices.directories.itunes.ITunes;

import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;

/**
 * Created by apl on 13-04-2015.
 */
public class DiscoveryFragment extends Fragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "DiscoveryFragment";

    // Match with entries_webservices_discovery_engine
    private static final int GPODDER_INDEX = 0;
    private static final int ITUNES_INDEX  = 1;

    private static final int HANDLER_WHAT_SEARCH = 27407; // whatever
    private static final int HANDLER_WHAT_CANCEL = 27408; // whatever
    private static final int HANDLER_DELAY = 1000; // ms
    private static final String HANDLER_QUERY = "query";
    private SearchHandler mSearchHandler = new SearchHandler(this);

    private SearchView mSearchView;
    private TextView mSearchLabel;
    private ImageButton mSearchEngineButton;
    private RecyclerView mResultsRecyclerView;

    private DiscoverySearchAdapter mResultsAdapter;

    private String mEmptySearchLabel;

    private String mDiscoveryEngineKey;

    private IDirectoryProvider mDirectoryProvider = null;
    private IDirectoryProvider.Callback mSearchResultCallback = new IDirectoryProvider.Callback() {
        @Override
        public void result(ISearchResult argResult) {
            Log.d(TAG, "Search results for: " + argResult.getSearchQuery());
            ArrayList<ISubscription> subscriptions = new ArrayList<>();
            for (ISubscription subscription : argResult.getResults()) {
                subscriptions.add(subscription);
            }
            mResultsAdapter.setDataset(subscriptions);
        }

        @Override
        public void error(Exception argException) {
            Log.e(TAG, "Search failed", argException);
            return;
        }
    };

    @Override
    public void onAttach(final Activity activity) {
        PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext()).registerOnSharedPreferenceChangeListener(this);
        mDiscoveryEngineKey = getResources().getString(R.string.pref_webservices_discovery_engine_key);

        super.onAttach(activity);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        setHasOptionsMenu(true);

        View mContainerView = inflater.inflate(R.layout.discovery_fragment, container, false);

        mEmptySearchLabel = getResources().getString(R.string.discovery_recommendations);

        return mContainerView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mSearchLabel = (TextView) view.findViewById(R.id.search_label);


        mSearchEngineButton = (ImageButton) view.findViewById(R.id.discovery_searchIcon);
        mSearchEngineButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogSearchDirectory dialogSearchDirectory = new DialogSearchDirectory();
                dialogSearchDirectory.show(getFragmentManager(), "SearchEnginePicker"); // NoI18N
            }
        });

        mSearchView = (SearchView) view.findViewById(R.id.discovery_searchView);
        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
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
        onSharedPreferenceChanged(PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext()), mDiscoveryEngineKey);

        mResultsAdapter = new DiscoverySearchAdapter(getActivity());
        mResultsAdapter.setHasStableIds(true);

        //mResultsAdapter = new AlphaInAnimationAdapter(discoveryAdapter);
        //mResultsAdapter.setDuration(1000);
        //recyclerView.setAdapter(alphaAdapter);

        mResultsRecyclerView = (RecyclerView) view.findViewById(R.id.search_result_view);
        mResultsRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mResultsRecyclerView.setHasFixedSize(true);
        mResultsRecyclerView.setAdapter(mResultsAdapter);

        populateRecommendations();

    }

    protected void performSearch(@NonNull String argQuery) {
        if (TextUtils.isEmpty(argQuery))
            return;

        updateLabel(argQuery);

        ISearchParameters searchParameters = new GenericSearchParameters();
        searchParameters.addSearchTerm(argQuery);

        mDirectoryProvider.search(searchParameters, mSearchResultCallback);
    }

    protected void abortSearch() {
        mDirectoryProvider.abortSearch();
    }

    private void searchviewQueryChanged(@NonNull String argQuery, boolean argDelaySearch) {

        Log.d(TAG, "searchviewQueryChanged: " + argQuery + " delay: " + argDelaySearch);

        mSearchHandler.removeMessages(HANDLER_WHAT_SEARCH);

        if (TextUtils.isEmpty(argQuery)) {
            mSearchEngineButton.setVisibility(View.VISIBLE);
            populateRecommendations();
            return;
        }

        mSearchEngineButton.setVisibility(View.GONE);

        Message msg = createHandlerMessage(argQuery, HANDLER_WHAT_SEARCH);
        if (argDelaySearch) {
            mSearchHandler.sendMessageDelayed(msg, HANDLER_DELAY);
        } else {
            Message abortMsg = createHandlerMessage(argQuery, HANDLER_WHAT_CANCEL);

            mSearchHandler.sendMessage(abortMsg);
            mSearchHandler.sendMessage(msg);
            mSearchView.clearFocus();
        }
    }

    private void populateRecommendations() {

        ArrayList<ISubscription> subscriptions = new ArrayList<>();

        try {
            URL url1 = new URL("http://www.hpmorpodcast.com/?feed=rss2");
            URL url2 = new URL("http://www.npr.org/rss/podcast.php?id=510289");
            URL url3 = new URL("http://leoville.tv/podcasts/sn.xml");

            ISubscription sub1 = new SlimSubscription("Harry Potter and the Methods of Rationality", url1, "http://www.hpmorpodcast.com/wp-content/uploads/powerpress/HPMoR_Podcast_new.jpg");
            ISubscription sub2 = new SlimSubscription("Planet Money", url2, "http://media.npr.org/images/podcasts/primary/icon_510289-d5d79b164ba7670399f0287529ce31a94523b224.jpg?s=500");
            ISubscription sub3 = new SlimSubscription("Security Now", url3, "http://twit.cachefly.net/coverart/sn/sn600audio.jpg");

            subscriptions.add(sub1);
            subscriptions.add(sub2);
            subscriptions.add(sub3);
        } catch (MalformedURLException mue) {
            return;
        }

        updateLabel("");
        mResultsAdapter.setDataset(subscriptions);


    }

    private void updateLabel(@NonNull String argQuery) {
        CharSequence labelText = mEmptySearchLabel;

        if (!TextUtils.isEmpty(argQuery)) {
            String resultLabel = getResources().getString(R.string.discovery_search_results);
            labelText = Html.fromHtml(String.format(resultLabel, argQuery));
        }

        mSearchLabel.setText(labelText);
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
        if (mDiscoveryEngineKey == key) {
            int searchEngine = Integer.valueOf(sharedPreferences.getString(mDiscoveryEngineKey, Integer.toString(getDefaultSearchEngine())));

            switch (searchEngine) {
                case GPODDER_INDEX: {
                    mDirectoryProvider = new GPodder();
                    mSearchEngineButton.setImageResource(R.drawable.discovery_gpodder);
                    break;
                }
                case ITUNES_INDEX: {
                    mDirectoryProvider = new ITunes();
                    mSearchEngineButton.setImageResource(R.drawable.discovery_itunes);
                    break;
                }
            }

            setQueryHint();
        }
    }

    private int getDefaultSearchEngine() {
        return BuildConfig.LIBRE_MODE ? GPODDER_INDEX : ITUNES_INDEX;
    }

    private void setQueryHint() {
        String queryHint = String.format(getResources().getString(R.string.search_query_hint), mDirectoryProvider.getName());
        mSearchView.setQueryHint(queryHint);
    }


    /**
     * Create a handler to perform the search query
     */
    private static class SearchHandler extends Handler {
        private final WeakReference<DiscoveryFragment> mFragment;

        public SearchHandler(DiscoveryFragment fragment) {
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
                        fragment.performSearch(query);
                        return;
                    }
                    case HANDLER_WHAT_CANCEL: {
                        Log.d(TAG, "abort query:");
                        fragment.abortSearch();
                        return;
                    }
                }
            }
        }
    }
}
