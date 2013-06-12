package org.bottiger.podcast;

import java.util.ArrayList;

import org.bottiger.podcast.R;
import org.bottiger.podcast.R.id;
import org.bottiger.podcast.R.layout;
import org.bottiger.podcast.adapters.ItemCursorAdapter;
import org.bottiger.podcast.adapters.ReorderCursor;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.utils.Playlist;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.mobeta.android.dslv.DragSortController;
import com.mobeta.android.dslv.DragSortListView;

public abstract class PlaylistDSLVFragment extends AbstractEpisodeFragment {

    private String[] array;
    private ArrayList<String> list;
    
    //private DragSortListView.DropListener onDrop2 = new ReorderCursor(mCursor, PlaylistDSLVFragment.this); 

    private DragSortListView.DropListener onDrop =
            new DragSortListView.DropListener() {
                @Override
                public void drop(int from, int to) {
                    if (from != to) {
                    	// Playlist.setFrom(from);
                    	// Playlist.setTo(to);
                    	ItemCursorAdapter adapter = ((ItemCursorAdapter) mAdapter);
                    	
                    	FeedItem precedingItem = null;
                    	if (to > 0) {
                    		Cursor precedingItemCursor = (Cursor)adapter.getItem(to-1);
                    		precedingItem = FeedItem.getByCursor(precedingItemCursor);
                    	}
                    	
                        Cursor item = (Cursor)adapter.getItem(from);
                        FeedItem feedItem = FeedItem.getByCursor(item);
                        
                        Context c = PlaylistDSLVFragment.this.getActivity();
                        feedItem.setPriority(precedingItem, c);
                    }
                }
            };

    /*
         private DragSortListView.DropListener onDrop =
            new DragSortListView.DropListener() {
                @Override
                public void drop(int from, int to) {
                    if (from != to) {
                        String item = adapter.getItem(from);
                        adapter.remove(item);
                        adapter.insert(item, to);
                    }
                }
            };

    private DragSortListView.RemoveListener onRemove = 
            new DragSortListView.RemoveListener() {
                @Override
                public void remove(int which) {
                    adapter.remove(adapter.getItem(which));
                }
            };
     */
            
    protected int getLayout() {
        // this DSLV xml declaration does not call for the use
        // of the default DragSortController; therefore,
        // DSLVFragment has a buildController() method.
        return R.layout.tmp_dslv_fragment_main;
    }
    
    /**
     * Return list item layout resource passed to the ArrayAdapter.
     */
    protected int getItemLayout() {
        /*if (removeMode == DragSortController.FLING_LEFT_REMOVE || removeMode == DragSortController.SLIDE_LEFT_REMOVE) {
            return R.layout.list_item_handle_right;
        } else */
    	if (removeMode == DragSortController.CLICK_REMOVE) {
            return R.layout.episode_list;
        } else {
            return R.layout.episode_list;
        }
    }

    protected DragSortListView mDslv;
    private DragSortController mController;

    public int dragStartMode = DragSortController.ON_DOWN;
    public boolean removeEnabled = false;
    public int removeMode = DragSortController.FLING_REMOVE;
    public boolean sortEnabled = true;
    public boolean dragEnabled = true;

    public DragSortController getController() {
        return mController;
    }

    /**
     * Called from DSLVFragment.onActivityCreated(). Override to
     * set a different adapter.
     */
    public abstract void setListAdapter(); // {
    	//setListAdapter(mAdapter);
    	/*
        array = getResources().getStringArray(R.array.jazz_artist_names);
        list = new ArrayList<String>(Arrays.asList(array));

        adapter = new ArrayAdapter<String>(getActivity(), getItemLayout(), R.id.text, list);
        setListAdapter(adapter);
        */
    //}

    /**
     * Called in onCreateView. Override this to provide a custom
     * DragSortController.
     */
    public DragSortController buildController(DragSortListView dslv) {
        // defaults are
        //   dragStartMode = onDown
        //   removeMode = flingRight
        DragSortController controller = new DragSortController(dslv);
        controller.setDragHandleId(R.id.drag_handle);
        //controller.setClickRemoveId(R.id.click_remove);
        controller.setRemoveEnabled(removeEnabled);
        controller.setSortEnabled(sortEnabled);
        controller.setDragInitMode(dragStartMode);
        controller.setRemoveMode(removeMode);
        return controller;
    }


    /** Called when the activity is first created. */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        //mDslv = (DragSortListView) inflater.inflate(getLayout(), container, false);
    	View fragmentView = inflater.inflate(getLayout(), container, false);
    	mDslv = (DragSortListView) fragmentView.findViewById(android.R.id.list);
    	
    	
        mController = buildController(mDslv);
        mDslv.setFloatViewManager(mController);
        mDslv.setOnTouchListener(mController);
        mDslv.setDragEnabled(dragEnabled);

        return mDslv;
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	//mDslv.setDropListener(onDrop2);
    	//mDslv.setDropListener((DragSortListView.DropListener)mCursor);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        //mDslv = (DragSortListView) getListView(); 

        //mDslv.setDropListener(onDrop);
        //mDslv.setRemoveListener(onRemove);

        Bundle args = getArguments();
        int headers = 0;
        int footers = 0;
        if (args != null) {
            headers = args.getInt("headers", 0);
            footers = args.getInt("footers", 0);
        }

        for (int i = 0; i < headers; i++) {
            addHeader(getActivity(), mDslv);
        }
        for (int i = 0; i < footers; i++) {
            addFooter(getActivity(), mDslv);
        }

        //setListAdapter(mAdapter);
    }


    public static void addHeader(Activity activity, DragSortListView dslv) {
        /*
    	LayoutInflater inflater = activity.getLayoutInflater();
        int count = dslv.getHeaderViewsCount();

        TextView header = (TextView) inflater.inflate(R.layout.header_footer, null);
        header.setText("Header #" + (count + 1));

        dslv.addHeaderView(header, null, false);
        */
    }

    public static void addFooter(Activity activity, DragSortListView dslv) {
        /*
    	LayoutInflater inflater = activity.getLayoutInflater();
        int count = dslv.getFooterViewsCount();

        TextView footer = (TextView) inflater.inflate(R.layout.header_footer, null);
        footer.setText("Footer #" + (count + 1));

        dslv.addFooterView(footer, null, false);
        */
    }

}
