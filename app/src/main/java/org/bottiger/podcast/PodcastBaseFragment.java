package org.bottiger.podcast;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.widget.SeekBar;

public abstract class PodcastBaseFragment extends Fragment {

    protected RecyclerView.Adapter mAdapter;
    protected RecyclerView.LayoutManager mLayoutManager;

    protected SharedPreferences sharedPreferences;


	private static SeekBar mProgressBar = null;

	public SeekBar getProgress() {
		return mProgressBar;
	}


	// Container Activity must implement this interface
	public interface OnItemSelectedListener {
		public void onItemSelected(long id);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		sharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(getActivity());

	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	@Override
	public void onPause() {
		super.onPause();

	}

	@Override
	public void onResume() {
		super.onResume();
	}

	abstract String getWhere();

	abstract String getOrder();

}
