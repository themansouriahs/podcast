package info.bottiger.podcast.utils;

import info.bottiger.podcast.provider.FeedItem;
import info.bottiger.podcast.service.PlayerService;
import android.content.Context;
import android.os.SystemClock;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class PodcastProgressBar extends SeekBar {
	
	private static final int MAX_VALUE = 1000;
	
	private FeedItem mItem;
	private PlayerService mServiceBinder;
	private TextView mCurrentTime;
	
    private long mLastSeekEventTime;
    private boolean mFromTouch;
    
    protected final Log log = Log.getLog(getClass());
	
	private OnSeekBarChangeListener mSeekListener = new OnSeekBarChangeListener() {
        public void onStartTrackingTouch(SeekBar bar) {
            mLastSeekEventTime = 0;
            mFromTouch = true;
            log.debug("mFromTouch = false; ");
            
        }
        public void onProgressChanged(SeekBar bar, int progress, boolean fromuser) {
            log.debug("onProgressChanged");
       	
            if (!fromuser || (mServiceBinder == null)) return;

            long now = SystemClock.elapsedRealtime();
            if ((now - mLastSeekEventTime) > 250) {
                mLastSeekEventTime = now;
                if (mCurrentTime != null) mCurrentTime.setText("hello " + progress);
                //mPosOverride = mp.duration * progress / 1000;
                try {
                	if(mServiceBinder.isInitialized())
                		mServiceBinder.seek(mServiceBinder.duration() * progress / 1000);
                } catch (Exception ex) {
                }

                if (!mFromTouch) {
                    //refreshNow();
                    //mPosOverride = -1;
                }
            }
            
        }
        
        public void onStopTrackingTouch(SeekBar bar) {
            //mPosOverride = -1;
            mFromTouch = false;
            log.debug("mFromTouch = false; ");

        }
    };
	
	public PodcastProgressBar(Context context, 
							PlayerService serviceBinder, 
							TextView currentTime,
							FeedItem item) {
		super(context);
		this.mItem = item;
		this.mServiceBinder = serviceBinder;
		this.mCurrentTime = currentTime;

        this.setMax(MAX_VALUE);    
	}
	
	

}
