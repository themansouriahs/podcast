package org.bottiger.podcast.Player;

import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

import org.bottiger.podcast.R;
import org.bottiger.podcast.service.PlayerService;

/**
 * Created by apl on 20-01-2015.
 */
public class PlayerHandler {

    private static final boolean CONTINUOUS_PLAYING_DEFAULT = false;

    public static final int FADEIN = 0;
    public static final int TRACK_ENDED = 1;
    public static final int SERVER_DIED = 2;

    private static PlayerService mPlayerService;

    @NonNull
    private static SharedPreferences mSharedpreferences;

    public PlayerHandler(@NonNull PlayerService argPlayerService) {
        mPlayerService = argPlayerService;
        mSharedpreferences = PreferenceManager.getDefaultSharedPreferences(mPlayerService.getApplicationContext());
    }

    public static final Handler handler = new Handler() {
        float mCurrentVolume = 1.0f;

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case FADEIN:
                    if (!mPlayerService.isPlaying()) {
                        mCurrentVolume = 0f;
                        mPlayerService.getPlayer().setVolume(mCurrentVolume);
                        mPlayerService.getPlayer().start();
                        handler.sendEmptyMessageDelayed(FADEIN, 10);
                    } else {
                        mCurrentVolume += 0.01f;
                        if (mCurrentVolume < 1.0f) {
                            handler.sendEmptyMessageDelayed(FADEIN, 10);
                        } else {
                            mCurrentVolume = 1.0f;
                        }
                        mPlayerService.getPlayer().setVolume(mCurrentVolume);
                    }
                    break;
                case TRACK_ENDED:
                    String key = mPlayerService.getResources().getString(R.string.pref_continuously_playing_key);
                    boolean doPlayNext = mSharedpreferences.getBoolean(key, CONTINUOUS_PLAYING_DEFAULT);

                    if (mPlayerService.getCurrentItem() != null) {

                        if (mPlayerService.getNextTrack() == PlayerService.NextTrack.NEXT_IN_PLAYLIST) {
                            long nextItemId = mPlayerService.getNextId();

                            if (nextItemId == -1) {
                                mPlayerService.dis_notifyStatus();
                                mPlayerService.getPlayer().stop();
                            } else if (doPlayNext) {
                                mPlayerService.playNext();
                            }
                        }
                    }

                    break;

                case SERVER_DIED:
                    break;

            }
        }
    };

}
