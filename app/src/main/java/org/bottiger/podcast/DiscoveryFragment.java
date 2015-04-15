package org.bottiger.podcast;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SearchViewCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.SearchView;

import org.bottiger.podcast.adapters.DiscoverySearchAdapter;
import org.bottiger.podcast.provider.ISubscription;
import org.bottiger.podcast.provider.SlimImplementations.SlimSubscription;

import java.util.ArrayList;

import jp.wasabeef.recyclerview.animators.adapters.AlphaInAnimationAdapter;

/**
 * Created by apl on 13-04-2015.
 */
public class DiscoveryFragment extends Fragment {

    private SearchView mSearchView;

    private RecyclerView mResultsRecyclerView;
    private DiscoverySearchAdapter mResultsAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        setHasOptionsMenu(true);

        View mContainerView = inflater.inflate(R.layout.discovery_fragment, container, false);

        return mContainerView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mSearchView = (SearchView) view.findViewById(R.id.discovery_searchView);
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

        AlphaInAnimationAdapter alphaAdapter = new AlphaInAnimationAdapter(mResultsAdapter);
        alphaAdapter.setDuration(1000);
        //recyclerView.setAdapter(alphaAdapter);

        mResultsRecyclerView = (RecyclerView) view.findViewById(R.id.search_result_view);
        mResultsRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mResultsRecyclerView.setHasFixedSize(true);
        mResultsRecyclerView.setAdapter(alphaAdapter);

        populate(mResultsAdapter);

    }

    private void populate(DiscoverySearchAdapter argAdapter) {
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

        argAdapter.setDataset(subscriptions);


    }
}
