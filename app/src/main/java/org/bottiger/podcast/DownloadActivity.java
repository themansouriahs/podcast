package org.bottiger.podcast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.PriorityQueue;

import org.bottiger.podcast.adapters.QueueAdapter;
import org.bottiger.podcast.provider.QueueEpisode;
import org.bottiger.podcast.service.Downloader.EpisodeDownloadManager;

import android.app.ListActivity;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;

public class DownloadActivity extends ListActivity {

	private QueueAdapter adapter;

	private QueueEpisode[] array;
	private ArrayList<QueueEpisode> list;

	private PriorityQueue<QueueEpisode> testQueue = new PriorityQueue<QueueEpisode>();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_download);
		getActionBar().setDisplayHomeAsUpEnabled(true);

		testQueue = EpisodeDownloadManager.getQueue();

		// array = Arrays.sort(testQueue.toArray());
		QueueEpisode[] array = new QueueEpisode[testQueue.size()];

		int i = 0;
		while (!testQueue.isEmpty()) {
			array[i] = testQueue.remove();
			i++;
		}

		list = new ArrayList<QueueEpisode>(Arrays.asList(array));

		adapter = new QueueAdapter(this, R.layout.download_list_item,
				R.id.text, list);
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

    /*
	private DragSortListView.DropListener onDrop = new DragSortListView.DropListener() {
		@Override
		public void drop(int from, int to) {
			QueueEpisode item = adapter.getItem(from);

			if (to == 0) {
				QueueEpisode itemAfter = adapter.getItem(from + 1);
				if (itemAfter != null)
					item.setPriority(itemAfter.getPriority() - 1);
			} else if (to == list.size() - 1) {
				QueueEpisode itemBefore = adapter.getItem(from);
				if (itemBefore != null)
					item.setPriority(itemBefore.getPriority() + 1);
			} else {
				QueueEpisode itemAfter = adapter.getItem(from);
				QueueEpisode itemBefore = adapter.getItem(from);
				if (itemBefore != null && itemAfter != null) {
					Integer meanPriority = (itemBefore.getPriority() + itemAfter.getPriority())/2;
					item.setPriority(meanPriority);
				}
			}

			PodcastDownloadManager.replace(item);
			
			adapter.notifyDataSetChanged();
			adapter.remove(item);
			adapter.insert(item, to);
		}
	};*/

    /*
	private DragSortListView.RemoveListener onRemove = new DragSortListView.RemoveListener() {
		@Override
		public void remove(int which) {
			adapter.remove(adapter.getItem(which));
		}
	};

	private DragSortListView.DragScrollProfile ssProfile = new DragSortListView.DragScrollProfile() {
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
	*/

	/*
	 * @Override public void selectItem(int position) { // TODO Auto-generated
	 * method stub
	 * 
	 * }
	 */

}
