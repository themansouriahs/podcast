package org.bottiger.podcast.listeners;


import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.service.PlayerService;

import android.content.ContentResolver;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;

import com.squareup.otto.Subscribe;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class PlayerStatusObservable {

    /**
     * How often the UI should refresh
     */
    private final static long REFRESH_INTERVAL = 100; // 16 ms => 60 fps

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({PLAYING, PAUSED, STOPPED, PREPARING})
    public @interface PlayerStatus {}
    public static final int PLAYING = 1;
    public static final int PAUSED = 2;
    public static final int STOPPED = 3;
    public static final int PREPARING = 4;

    private static Handler sHandler = new ProgressHandler();
    private static long lastUpdate = System.currentTimeMillis();

    public static void updateProgress(@NonNull PlayerService argPlayerService) {
        IEpisode currentItem = argPlayerService.getCurrentItem();
        if (currentItem != null && currentItem instanceof FeedItem) {
            FeedItem feedItem = (FeedItem)currentItem;
            long offset = argPlayerService.getPlayer().getCurrentPosition();
            updateEpisodeOffset(argPlayerService.getContentResolver(),
                    feedItem,
                    offset);

            SoundWaves.getBus().post(new PlayerStatusProgressData(feedItem.getOffset()));
        }
    }

    public static void updateEpisodeOffset(@NonNull ContentResolver argContentResolver,
                                           @NonNull FeedItem argEpisode,
                                           long argOffset) {
        long now = System.currentTimeMillis();

        // more than a second ago
        if (now - lastUpdate > 1000) {
            argEpisode.setPosition(argOffset);
            lastUpdate = now;
        }
    }

    public static void startProgressUpdate(boolean argIsPlaying) {
        sHandler.removeMessages(PLAYING);

        if (argIsPlaying) {
            Message msg = sHandler.obtainMessage(PLAYING);
            sHandler.sendMessage(msg);
        }
    }

    private static class ProgressHandler extends Handler {

        @Override
        public void handleMessage(Message inputMessage) {
            // Gets the task from the incoming Message object.
            //PhotoTask photoTask = (PhotoTask) inputMessage.obj;

            // Gets the ImageView for this task
            switch (inputMessage.what) {
                // The decoding is done
                case PLAYING:
                    PlayerService ps = PlayerService.getInstance();

                    if (ps != null) {
                        updateProgress(ps);

                        int currentStatus = ps.isPlaying() || ps.getPlayer().isCasting() ? PLAYING : STOPPED;
                        inputMessage = sHandler.obtainMessage(currentStatus);
                        sHandler.sendMessageDelayed(inputMessage, REFRESH_INTERVAL);
                    }
                    break;
                default:

                    //super.handleMessage(inputMessage);
            }
        }

    }
}
