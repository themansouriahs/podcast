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

/**
 * Created by apl on 20-01-2015.
 */
public class PlayerHandler {

    private static final String TAG  = "PlayerHandler";

    private static final boolean CONTINUOUS_PLAYING_DEFAULT = false;
    private static final int FADE_OUT_DURATION_SECONDS = 30;
    private static final float FADE_OUT_VOLUME_STEP = 0.01f;

    public static final int FADEIN = 0;
    public static final int TRACK_ENDED = 1;
    public static final int SERVER_DIED = 2;
    public static final int FADEOUT = 3;

    private Context mContext;
    private final InnerPlayerHandler mHandler;

    public PlayerHandler(@NonNull Context argContext) {
        mContext = argContext;
        mHandler  = new InnerPlayerHandler();
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

    public void removeCallbacks(int what) {
        mHandler.removeMessages(what);
    }

    private static class InnerPlayerHandler extends Handler {
        private float mCurrentVolume = 1.0f;
        private static float sInitialVolume = 1.0f;

        @Override
        public void handleMessage(Message msg) {

            PlayerService playerService = PlayerService.getInstance();

            if (playerService == null)
                return;

            GenericMediaPlayerInterface player = playerService.getPlayer();

            switch (msg.what) {
                case FADEIN:
                    if (!PlayerService.isPlaying()) {
                        mCurrentVolume = 0f;
                        player.setVolume(mCurrentVolume);
                        player.start();
                        this.sendEmptyMessageDelayed(FADEIN, 10);
                    } else {
                        mCurrentVolume += FADE_OUT_VOLUME_STEP;
                        if (mCurrentVolume < 1.0f) {
                            this.sendEmptyMessageDelayed(FADEIN, 10);
                        } else {
                            mCurrentVolume = 1.0f;
                        }
                        player.setVolume(mCurrentVolume);
                    }
                    break;
                case TRACK_ENDED:
                    String key = playerService.getResources().getString(R.string.pref_continuously_playing_key);
                    boolean doPlayNext = PreferenceManager
                            .getDefaultSharedPreferences(playerService.getApplicationContext())
                            .getBoolean(key, CONTINUOUS_PLAYING_DEFAULT);

                    if (PlayerService.getCurrentItem() != null) {

                        if (PlayerService.getNextTrack() == PlayerService.NEXT_IN_PLAYLIST) {
                            IEpisode nextItemId = playerService.getNext();

                            if (nextItemId == null) {
                                playerService.dis_notifyStatus();
                                player.stop();
                            } else if (doPlayNext) {
                                playerService.playNext();
                            }
                        }
                    }

                    break;

                case SERVER_DIED:
                    break;

                case FADEOUT: {
                    sInitialVolume = mCurrentVolume;

                    if (PlayerService.isPlaying()) {
                        mCurrentVolume -= FADE_OUT_VOLUME_STEP;
                        if (mCurrentVolume > 0.0f) {
                            this.sendEmptyMessageDelayed(FADEOUT, fadeOutMessageDelay());
                        } else {
                            mCurrentVolume = 0.0f;
                        }

                        player.setVolume(mCurrentVolume);
                        Log.d(TAG, "Setting volume to: " + mCurrentVolume); // NoI18N

                        if (mCurrentVolume <= 0.0f) {
                            playerService.pause();
                            Log.d(TAG, "Stopping playback"); // NoI18N
                            player.setVolume(sInitialVolume);
                            mCurrentVolume = sInitialVolume;
                        }
                    }

                    break;
                }

            }
        }
    }

    private static int fadeOutMessageDelay() {
        return Math.round(FADE_OUT_DURATION_SECONDS*1000 * FADE_OUT_VOLUME_STEP);
    }

}
