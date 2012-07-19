package info.bottiger.podcast;

import info.bottiger.podcast.R;
import info.bottiger.podcast.service.PodcastService;
import info.bottiger.podcast.utils.Log;

import android.app.ListActivity;
import android.database.Cursor;
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
	

	protected  static PodcastService mServiceBinder = null;
	protected final Log log = Log.getLog(getClass());

	protected SimpleCursorAdapter mAdapter;
	protected Cursor mCursor = null;

	protected static ComponentName mService = null;
	
	protected boolean mInit = false;
	protected Intent mPrevIntent = null;
	
	protected Intent mNextIntent = null;
	
	
	/*
	protected GestureDetector gestureDetector;
	protected View.OnTouchListener gestureListener;	
	
    class MyGestureDetector extends SimpleOnGestureListener {
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        	//log.debug("onFling");
            try {
                if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH)
                    return false;
                // right to left swipe
                if(e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                	if(mPrevIntent!=null)
                		startActivity(mPrevIntent);
                	finish();

                }  else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                	if(mPrevIntent!=null)
                		startActivity(mNextIntent);
                	finish();
                }
            } catch (Exception e) {
                // nothing
            }
            return false;
        }
    }	
	*/
	protected static ServiceConnection serviceConnection = new ServiceConnection() {
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
	};

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

	@Override
	public void onPause() {
		super.onPause();

	}

	@Override
	public void onLowMemory() {
		super.onLowMemory();
		mInit = true;

		log.debug("onLowMemory()");
		//finish();
	}

	public void startInit() {

		log.debug("startInit()");

		mService = getActivity().startService(new Intent(getActivity(), PodcastService.class));

		Intent bindIntent = new Intent(getActivity(), PodcastService.class);
		getActivity().bindService(bindIntent, serviceConnection, Context.BIND_AUTO_CREATE);
		
		/*
        gestureDetector = new GestureDetector(new MyGestureDetector());
        gestureListener = new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                if (gestureDetector.onTouchEvent(event)) {
                    return true;
                }
                return false;
            }
        };
        */
        
        //getListView().setOnTouchListener(gestureListener);	
	}
}
