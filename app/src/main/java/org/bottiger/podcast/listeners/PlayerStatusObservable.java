package org.bottiger.podcast.listeners;


import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.service.PlayerService;

import android.content.ContentResolver;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;

import com.squareup.otto.Subscribe;

public class PlayerStatusObservable {

    /**
     * How often the UI should refresh
     */
    private final static long REFRESH_INTERVAL = 100; // 16 ms => 60 fps

	public enum STATUS {
		PLAYING(0), PAUSED(1), STOPPED(2);

        private int value;
        private STATUS(int value){
            this.value = value;
        }
	}

    private static Handler sHandler = new ProgressHandler();
    private static long lastUpdate = System.currentTimeMillis();

    public static STATUS statusFromInt(int i) {
        return STATUS.values()[i];
    }

    public static void updateProgress(@NonNull PlayerService argPlayerService) {
        IEpisode currentItem = argPlayerService.getCurrentItem();
        if (currentItem != null && currentItem instanceof FeedItem) {
            FeedItem feedItem = (FeedItem)currentItem;
            int offset = argPlayerService.getPlayer().getCurrentPosition();
            updateEpisodeOffset(argPlayerService.getContentResolver(),
                    feedItem,
                    offset);

            SoundWaves.getBus().post(new PlayerStatusProgressData(feedItem.getOffset()));
        }
    }

    public static void updateEpisodeOffset(@NonNull ContentResolver argContentResolver,
                                           @NonNull FeedItem argEpisode,
                                           int argOffset) {
        long now = System.currentTimeMillis();

        // more than a second ago
        if (now - lastUpdate > 1000) {
            argEpisode.setPosition(argContentResolver, argOffset);
            lastUpdate = now;
        }
    }

    @Subscribe
    public void startProgressUpdate(PlayerStatusData argPlayerStatus) {
        sHandler.removeMessages(STATUS.PLAYING.value);

        if (argPlayerStatus.status == STATUS.PLAYING) {
            Message msg = sHandler.obtainMessage(STATUS.PLAYING.value);
            sHandler.sendMessage(msg);
        }
    }

    private static class ProgressHandler extends Handler {

        @Override
        public void handleMessage(Message inputMessage) {
            // Gets the task from the incoming Message object.
            //PhotoTask photoTask = (PhotoTask) inputMessage.obj;

            // Gets the ImageView for this task
            STATUS status = statusFromInt(inputMessage.what);
            switch (status) {
                // The decoding is done
                case PLAYING:
                    PlayerService ps = SoundWaves.sBoundPlayerService;

                    if (ps != null) {
                        updateProgress(ps);

                        int currentStatus = ps.isPlaying() || ps.getPlayer().isCasting() ? STATUS.PLAYING.value : STATUS.STOPPED.value;
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
