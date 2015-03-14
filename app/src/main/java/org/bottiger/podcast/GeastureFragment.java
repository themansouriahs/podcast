package org.bottiger.podcast;

import org.bottiger.podcast.R.id;
import org.bottiger.podcast.listeners.DownloadProgressObservable;
import org.bottiger.podcast.views.FixedRecyclerView;
import org.bottiger.podcast.views.SwipeRefreshExpandableLayout;

import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;


public abstract class GeastureFragment extends AbstractEpisodeFragment implements SwipeRefreshExpandableLayout.OnRefreshListener {

    protected SwipeRefreshExpandableLayout mSwipeRefreshView;
    protected FixedRecyclerView mRecyclerView;
    protected View mOverlay;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    protected int getLayout() {
        return R.layout.playlist_fragment_main;
    }

    /** Called when the activity is first created. */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

    	View fragmentView = inflater.inflate(getLayout(), container, false);

        SwipeRefreshExpandableLayout view = (SwipeRefreshExpandableLayout) fragmentView.findViewById(id.my_frame);
        View frameLayout = (View) fragmentView.findViewById(R.id.playlist_container);

        mOverlay = fragmentView.findViewById(id.overlay_container);
        mOverlay = mOverlay.findViewById(id.playlist_overlay_layout);
        mOverlay.setVisibility(View.INVISIBLE);

        mSwipeRefreshView = view;


        mSwipeRefreshView.setOnRefreshListener(this);
        mSwipeRefreshView.setColorScheme(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);

        mSwipeRefreshView.mTarget = mRecyclerView;


        mRecyclerView = (FixedRecyclerView) fragmentView.findViewById(id.my_recycler_view);

        return mSwipeRefreshView;
    }


    @Override
    public abstract void onRefresh();

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mRecyclerView != null) {
            ViewGroup parentViewGroup = (ViewGroup) mRecyclerView.getParent();
            if (parentViewGroup != null) {
                parentViewGroup.removeAllViews();
            }
        }
    }
}
