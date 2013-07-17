package org.bottiger.podcast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.PriorityQueue;

import com.mobeta.android.dslv.DragSortListView;

import android.app.ListActivity;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;
import android.widget.ArrayAdapter;

public class DownloadActivity extends ListActivity {
	
	private ArrayAdapter<String> adapter;
	
    private String[] array;
    private ArrayList<String> list;
	
	private PriorityQueue<String> testQueue = new PriorityQueue<String>(); 
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.activity_download);
	    getActionBar().setDisplayHomeAsUpEnabled(true);
	    testQueue.add("First item");
	    testQueue.add("Second item");
	    testQueue.add("Third item");
	    
        DragSortListView lv = (DragSortListView) getListView(); 

        lv.setDropListener(onDrop);
        lv.setRemoveListener(onRemove);
        lv.setDragScrollProfile(ssProfile);

        //array = Arrays.sort(testQueue.toArray());
        String[] array = new String[ testQueue.size() ];

        int i = 0;
        while ( !testQueue.isEmpty() ) {
        	array[ i ] = testQueue.remove();
            i++;
        }
        
        list = new ArrayList<String>(Arrays.asList(array));

        adapter = new ArrayAdapter<String>(this, R.layout.download_list_item, R.id.text, list);
        setListAdapter(adapter);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	    // Respond to the action bar's Up/Home button
	    case android.R.id.home:
	        NavUtils.navigateUpFromSameTask(this);
	        return true;
	    }
	    return super.onOptionsItemSelected(item);
	}
	
    private DragSortListView.DropListener onDrop =
            new DragSortListView.DropListener() {
                @Override
                public void drop(int from, int to) {
                    String item=adapter.getItem(from);

                    adapter.notifyDataSetChanged();
                    adapter.remove(item);
                    adapter.insert(item, to);
                }
            };

        private DragSortListView.RemoveListener onRemove = 
            new DragSortListView.RemoveListener() {
                @Override
                public void remove(int which) {
                    adapter.remove(adapter.getItem(which));
                }
            };

        private DragSortListView.DragScrollProfile ssProfile =
            new DragSortListView.DragScrollProfile() {
                @Override
                public float getSpeed(float w, long t) {
                    if (w > 0.8f) {
                        // Traverse all views in a millisecond
                        return ((float) adapter.getCount()) / 0.001f;
                    } else {
                        return 10.0f * w;
                    }
                }
            };

	/*
	@Override
	public void selectItem(int position) {
		// TODO Auto-generated method stub

	}
	*/

}
