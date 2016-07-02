package org.bottiger.podcast.player;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.Log;

import org.bottiger.podcast.R;
import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.service.PlayerService;

import java.lang.ref.WeakReference;

/**
 * Created by apl on 20-01-2015.
 */
public class PlayerHandler {

    private static final String TAG  = "PlayerHandler";

    private static final boolean CONTINUOUS_PLAYING_DEFAULT = false;

    public static final int FADEIN = 0;
    public static final int TRACK_ENDED = 1;
    public static final int SERVER_DIED = 2;
    public static final int FADEOUT = 3;

    private Context mContext;
    private final InnerPlayerHandler mHandler;

    public PlayerHandler(@NonNull Context argContext) {
        mContext = argContext;
        mHandler  = new InnerPlayerHandler(mContext);
    }

    public void sendEmptyMessage(int msg) {
        mHandler.sendEmptyMessage(msg);
    }

    public void sendEmptyMessageDelayed(int msg, int delay) {
        mHandler.sendEmptyMessageDelayed(msg, delay);
    }

    public void sendMessageDelayed(int msg, int delay) {
        mHandler.sendMessageDelayed(
                mHandler.obtainMessage(msg), delay);
    }

    private static class InnerPlayerHandler extends Handler {
        private final WeakReference<Context> mService;
        private float mCurrentVolume = 1.0f;

        public InnerPlayerHandler(Context service) {
            mService = new WeakReference<>(service);
        }

        @Override
        public void handleMessage(Message msg) {

            PlayerService playerService = PlayerService.getInstance();

            if (playerService == null)
                return;

            switch (msg.what) {
                case FADEIN:
                    if (!playerService.isPlaying()) {
                        mCurrentVolume = 0f;
                        playerService.getPlayer().setVolume(mCurrentVolume);
                        playerService.getPlayer().start();
                        this.sendEmptyMessageDelayed(FADEIN, 10);
                    } else {
                        mCurrentVolume += 0.01f;
                        if (mCurrentVolume < 1.0f) {
                            this.sendEmptyMessageDelayed(FADEIN, 10);
                        } else {
                            mCurrentVolume = 1.0f;
                        }
                        playerService.getPlayer().setVolume(mCurrentVolume);
                    }
                    break;
                case TRACK_ENDED:
                    String key = playerService.getResources().getString(R.string.pref_continuously_playing_key);
                    boolean doPlayNext = PreferenceManager
                            .getDefaultSharedPreferences(playerService.getApplicationContext())
                            .getBoolean(key, CONTINUOUS_PLAYING_DEFAULT);

                    if (playerService.getCurrentItem() != null) {

                        if (playerService.getNextTrack() == PlayerService.NEXT_IN_PLAYLIST) {
                            IEpisode nextItemId = playerService.getNext();

                            if (nextItemId == null) {
                                playerService.dis_notifyStatus();
                                playerService.getPlayer().stop();
                            } else if (doPlayNext) {
                                playerService.playNext();
                            }
                        }
                    }

                    break;

                case SERVER_DIED:
                    break;

                case FADEOUT: {
                    if (playerService.isPlaying()) {
                        mCurrentVolume -= 0.01f;
                        if (mCurrentVolume > 0.0f) {
                            this.sendEmptyMessageDelayed(FADEOUT, 10);
                        } else {
                            mCurrentVolume = 0.0f;
                        }
                        playerService.getPlayer().setVolume(mCurrentVolume);
                        Log.d(TAG, "Setting volume to: " + mCurrentVolume); // NoI18N

                        if (mCurrentVolume <= 0.0f) {
                            playerService.stop();
                            Log.d(TAG, "Stopping playback"); // NoI18N
                        }
                    }

                    break;
                }

            }
        }
    }

}
