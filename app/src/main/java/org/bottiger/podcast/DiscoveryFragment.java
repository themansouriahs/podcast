package org.bottiger.podcast;

import android.app.Activity;
import android.content.ContentResolver;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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
import android.widget.SearchView;
import android.widget.TextView;

import org.bottiger.podcast.adapters.DiscoverySearchAdapter;
import org.bottiger.podcast.provider.ISubscription;
import org.bottiger.podcast.provider.SlimImplementations.SlimSubscription;
import org.bottiger.podcast.provider.Subscription;
import org.bottiger.podcast.webservices.directories.IDirectoryProvider;
import org.bottiger.podcast.webservices.directories.ISearchParameters;
import org.bottiger.podcast.webservices.directories.ISearchResult;
import org.bottiger.podcast.webservices.directories.generic.GenericSearchParameters;
import org.bottiger.podcast.webservices.directories.gpodder.GPodder;

import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;

/**
 * Created by apl on 13-04-2015.
 */
public class DiscoveryFragment extends Fragment {

    private static final String TAG = "DiscoveryFragment";

    private static final int HANDLER_WHAT = 27407; // whatever
    private static final int HANDLER_DELAY = 300; // ms
    private static final String HANDLER_QUERY = "query";
    private SearchHandler mSearchHandler = new SearchHandler(this);

    private SearchView mSearchView;
    private TextView mSearchLabel;
    private RecyclerView mResultsRecyclerView;

    private DiscoverySearchAdapter mResultsAdapter;

    private String mEmptySearchLabel;

    private IDirectoryProvider mDirectoryProvider = null;
    private IDirectoryProvider.Callback mSearchResultCallback = new IDirectoryProvider.Callback() {
        @Override
        public void result(ISearchResult argResult) {
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        setHasOptionsMenu(true);

        View mContainerView = inflater.inflate(R.layout.discovery_fragment, container, false);

        mEmptySearchLabel = getResources().getString(R.string.discovery_recommendations);
        mDirectoryProvider = new GPodder();

        return mContainerView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mSearchLabel = (TextView) view.findViewById(R.id.search_label);

        String queryHint = String.format(getResources().getString(R.string.search_query_hint), mDirectoryProvider.getName());
        mSearchView = (SearchView) view.findViewById(R.id.discovery_searchView);
        mSearchView.setQueryHint(queryHint);
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

    private void searchviewQueryChanged(@NonNull String argQuery, boolean argDelaySearch) {

        mSearchHandler.removeMessages(HANDLER_WHAT);

        if (TextUtils.isEmpty(argQuery)) {
            populateRecommendations();
            return;
        }

        Message msg = createHandlerMessage(argQuery);
        if (argDelaySearch) {
            mSearchHandler.sendMessageDelayed(msg, HANDLER_DELAY);
        } else {
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

    private Message createHandlerMessage(String argQuery) {
        Bundle bundle = new Bundle();
        bundle.putString(HANDLER_QUERY, argQuery);
        Message msg = new Message();
        msg.what = HANDLER_WHAT;
        msg.setData(bundle);
        return msg;
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
                String query = msg.getData().getString(HANDLER_QUERY);
                fragment.performSearch(query);
            }
        }
    }

    /*
    private static final Runnable sPerformSearch = new Runnable() {
        @Override
        public void run() {

        }
    };
    */
}
