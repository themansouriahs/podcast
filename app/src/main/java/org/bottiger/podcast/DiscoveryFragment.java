package org.bottiger.podcast;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SearchViewCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.SearchView;

/**
 * Created by apl on 13-04-2015.
 */
public class DiscoveryFragment extends Fragment {

    private SearchView mSearchView;

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
    }
}
