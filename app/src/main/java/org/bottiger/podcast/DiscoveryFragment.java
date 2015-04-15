package org.bottiger.podcast;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;

import org.bottiger.podcast.adapters.DiscoverySearchAdapter;
import org.bottiger.podcast.provider.ISubscription;
import org.bottiger.podcast.provider.SlimImplementations.SlimSubscription;
import org.bottiger.podcast.webservices.directories.IDirectoryProvider;
import org.bottiger.podcast.webservices.directories.ISearchParameters;
import org.bottiger.podcast.webservices.directories.ISearchResult;
import org.bottiger.podcast.webservices.directories.generic.GenericSearchParameters;
import org.bottiger.podcast.webservices.directories.gpodder.GPodder;

import java.util.ArrayList;

import jp.wasabeef.recyclerview.animators.adapters.AlphaInAnimationAdapter;

/**
 * Created by apl on 13-04-2015.
 */
public class DiscoveryFragment extends Fragment {

    private static final String TAG = "DiscoveryFragment";

    private SearchView mSearchView;

    private RecyclerView mResultsRecyclerView;
    private DiscoverySearchAdapter mResultsAdapter;

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

        mDirectoryProvider = new GPodder();

        return mContainerView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mSearchView = (SearchView) view.findViewById(R.id.discovery_searchView);
        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (TextUtils.isEmpty(newText))
                    return false;

                ISearchParameters searchParameters = new GenericSearchParameters();
                searchParameters.addSearchTerm(newText);

                mDirectoryProvider.search(searchParameters, mSearchResultCallback);
                return true;
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

        //populate(mResultsAdapter);

    }

    private void populate(DiscoverySearchAdapter argAdapter) {
        /*
        ISubscription sub1 = new SlimSubscription("Harry Potter");
        ISubscription sub2 = new SlimSubscription("Planet Money");
        ISubscription sub3 = new SlimSubscription("Security Now");
        ISubscription sub4 = new SlimSubscription("TWIT");
        ISubscription sub5 = new SlimSubscription("MacBreak");
        ISubscription sub6 = new SlimSubscription("American life");

        ArrayList<ISubscription> subscriptions = new ArrayList<>();
        subscriptions.add(sub1);
        subscriptions.add(sub2);
        subscriptions.add(sub3);
        subscriptions.add(sub4);
        subscriptions.add(sub5);
        subscriptions.add(sub6);

        argAdapter.setDataset(subscriptions);*/


    }
}
