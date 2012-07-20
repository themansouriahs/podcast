package info.bottiger.podcast;

import info.bottiger.podcast.R;
import info.bottiger.podcast.service.PlayerService;
import info.bottiger.podcast.service.PodcastService;
import info.bottiger.podcast.utils.Log;

import android.app.Activity;
import android.app.ListActivity;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.support.v4.app.ListFragment;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.widget.SimpleCursorAdapter;

/* Copy of PodcastBaseActivity */
public class PodcastBaseFragment extends ListFragment {

    private static final int SWIPE_MIN_DISTANCE = 120;
    private static final int SWIPE_MAX_OFF_PATH = 250;
	private static final int SWIPE_THRESHOLD_VELOCITY = 200;
	
	public static final int COLUMN_INDEX_TITLE = 1;
	

	protected  static PlayerService mPlayerServiceBinder = null;
	//protected  static PodcastService mServiceBinder = null;
	//protected final Log log = Log.getLog(getClass());

	protected SimpleCursorAdapter mAdapter;
	//protected Cursor mCursor = null;

	//protected static ComponentName mService = null;
	
	//protected boolean mInit = false;
	protected Intent mPrevIntent = null;
	
	protected Intent mNextIntent = null;
	
	OnEpisodeSelectedListener mListener;
	
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnEpisodeSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnArticleSelectedListener");
        }
    }
    
	// Container Activity must implement this interface
    public interface OnEpisodeSelectedListener {
        public void onEpisodeSelected(Uri episodeUri);
    }
    
	protected static ServiceConnection serviceConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			mPlayerServiceBinder = ((PlayerService.PlayerBinder) service)
					.getService();
			//log.debug("onServiceConnected");
		}

		public void onServiceDisconnected(ComponentName className) {
			mPlayerServiceBinder = null;
			//log.debug("onServiceDisconnected");
		}
	};	
	
	
	/*protected static ServiceConnection serviceConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			mServiceBinder = ((PodcastService.PodcastBinder) service)
					.getService();
			mServiceBinder.start_update();
			//log.debug("onServiceConnected");
		}

		public void onServiceDisconnected(ComponentName className) {
			mServiceBinder = null;
			//log.debug("onServiceDisconnected");
		}
	};*/

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		try {
			//unbindService(serviceConnection); TODO
		} catch (Exception e) {
			e.printStackTrace();

		}
	}

	/*
	@Override
	public void onResume() {
		super.onResume();
		if (mInit) {
			mInit = false;

			if (mCursor != null)
				mCursor.close();

			getActivity().unbindService(serviceConnection);

			startInit();

		}

	}
	*/

	@Override
	public void onPause() {
		super.onPause();

	}

	/*
	@Override
	public void onLowMemory() {
		super.onLowMemory();
		mInit = true;

		log.debug("onLowMemory()");
		//finish();
	}
	*/
	public void startInit() {

		SwipeActivity.mService = getActivity().startService(new Intent(getActivity(), PodcastService.class));

		Intent bindIntent = new Intent(getActivity(), PodcastService.class);
		getActivity().bindService(bindIntent, SwipeActivity.serviceConnection, Context.BIND_AUTO_CREATE);
	}
	
}
