package org.bottiger.podcast.views;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.player.GenericMediaPlayerInterface;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.provider.SlimImplementations.SlimEpisode;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;

/**
 * Created by aplb on 01-12-2016.
 */

public class OnTouchSeekListener {

    private static final String TAG = OnTouchSeekListener.class.getSimpleName();

    private static final int MSG_DELAY_MS = 200;
    private static final int NO_EPISODE_ID = -1;
    private static final String EPISODE_KEY = "slimEpisode";
    private static final int EVENT_BUTTON_HOLD = -1;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({FORWARD, BACKWARDS})
    public @interface Direction {}
    public static final int FORWARD = 1;
    public static final int BACKWARDS = 2;

    private static SeekHandler sSeekHandler;

    static View.OnTouchListener getSeekListener(@NonNull final SoundWaves argApp,
                                                @NonNull final IEpisode argEpisode,
                                                @Direction final int argDirection) {
        iniSeekHandler(argApp);

        return new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                Log.v(TAG, "onTouchEvent: " + motionEvent.toString());

                Message msg = null;
                boolean wasHandled = true;

                int action = motionEvent.getAction();
                switch (action) {
                    case MotionEvent.ACTION_DOWN: {
                        msg = new Message();
                        break;
                    }
                    case MotionEvent.ACTION_UP: {
                        msg = new Message();
                        sSeekHandler.removeCallbacksAndMessages(null);
                        break;
                    }
                    case MotionEvent.ACTION_CANCEL: {
                        //msg = new Message();
                        break;
                    }
                    default: {
                        wasHandled = false;
                        break;
                    }

                }

                if (msg != null) {
                    boolean isFeedItem = argEpisode instanceof FeedItem;

                    msg.what = action;
                    msg.arg1 = argDirection;
                    msg.arg2 = isFeedItem ? (int) ((FeedItem) argEpisode).getId() : NO_EPISODE_ID;

                    if (!isFeedItem) {
                        Bundle data = new Bundle();
                        data.putParcelable(EPISODE_KEY, (SlimEpisode)argEpisode);
                        msg.setData(data);
                    }

                    sSeekHandler.sendMessage(msg);
                } else if (wasHandled) {
                    sSeekHandler.removeCallbacksAndMessages(null);
                }

                return wasHandled;
            }
        };
    }

    private static class SeekHandler extends Handler {
        private final WeakReference<SoundWaves> mApp;

        SeekHandler(@NonNull final SoundWaves argApp) {
            mApp = new WeakReference<>(argApp);
        }

        @Override
        public void handleMessage(Message msg)
        {
            Log.v(TAG, "Handle message");

            if (msg.what != MotionEvent.ACTION_DOWN && msg.what != EVENT_BUTTON_HOLD && msg.what != MotionEvent.ACTION_UP)
                return;

            SoundWaves soundwaves = mApp.get();

            if (soundwaves != null) {

                int episodeId = msg.arg2;

                IEpisode episode;
                if (episodeId == NO_EPISODE_ID) {
                    Parcel parcel = Parcel.obtain();
                    Parcelable parcelable = msg.getData().getParcelable(EPISODE_KEY);

                    assert parcelable != null;

                    parcelable.writeToParcel(parcel, 0);
                    episode = SlimEpisode.CREATOR.createFromParcel(parcel);
                } else {
                    episode = soundwaves.getLibraryInstance().getEpisode(episodeId);
                }

                if (episodeId == NO_EPISODE_ID || episode == null) {
                    Log.wtf(TAG, "No episode found! id:" + episodeId);
                    return;
                }

                GenericMediaPlayerInterface player = soundwaves.getPlayer();

                boolean isFastSeeking = msg.what == EVENT_BUTTON_HOLD;
                //noinspection WrongConstant
                seekPlayer(player, episode, msg.arg1, isFastSeeking);
                Message newMsg = new Message();
                newMsg.copyFrom(msg);
                newMsg.what = EVENT_BUTTON_HOLD;

                if (msg.what != MotionEvent.ACTION_UP) {
                    this.sendMessageDelayed(newMsg, MSG_DELAY_MS);
                }
            }
        }
    }

    private static void seekPlayer(GenericMediaPlayerInterface argPlayer,
                                   @NonNull final IEpisode argEpisode,
                                   @Direction final int argDirection,
                                   boolean isFastSeeking) {
        if (argDirection == FORWARD) {
            argPlayer.fastForward(argEpisode, isFastSeeking);
        } else {
            argPlayer.rewind(argEpisode, isFastSeeking);
        }

        Log.v(TAG, "Do seek! isFast:" + isFastSeeking);
    }

    private static SeekHandler iniSeekHandler(@NonNull final SoundWaves argApp) {
        sSeekHandler = new SeekHandler(argApp);

        return sSeekHandler;
    }

}
