package org.bottiger.podcast.listeners;


import android.content.ContentResolver;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;

import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.service.PlayerService;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;

import io.reactivex.processors.PublishProcessor;

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

    private static Handler sHandler;

    public PlayerStatusObservable(@NonNull PlayerService argPlayerService) {
        sHandler = new ProgressHandler(argPlayerService);
    }

    private static void updateProgress(@NonNull PlayerService argPlayerService) {

        IEpisode currentItem = PlayerService.getCurrentItem();

        if (currentItem instanceof FeedItem) {
            FeedItem feedItem = (FeedItem)currentItem;
            long offset = argPlayerService.getPlayer().getCurrentPosition();

            feedItem.setPosition(offset, false);

            PlayerStatusProgressData pspd = new PlayerStatusProgressData(feedItem);
            SoundWaves.getRxBus2().send(pspd);
        }
    }

    public void startProgressUpdate(boolean argIsPlaying) {
        sHandler.removeMessages(PLAYING);

        if (argIsPlaying) {
            Message msg = sHandler.obtainMessage(PLAYING);
            sHandler.sendMessage(msg);
        }
    }

    private static class ProgressHandler extends Handler {

        // http://stackoverflow.com/questions/11407943/this-handler-class-should-be-static-or-leaks-might-occur-incominghandler
        private final WeakReference<PlayerService> mService;
        private long lastPosition = -1;

        ProgressHandler(PlayerService service) {
            mService = new WeakReference<>(service);
        }

        @Override
        public void handleMessage(Message inputMessage) {

            PlayerService service = mService.get();

            if (service != null) {
                switch (inputMessage.what) {
                    case PLAYING: {
                        updateProgress(service);
                        service.updateChapter(lastPosition);
                        lastPosition = service.position();

                        int currentStatus = PlayerService.isPlaying() || service.getPlayer().isCasting() ? PLAYING : STOPPED;
                        inputMessage = sHandler.obtainMessage(currentStatus);
                        sHandler.sendMessageDelayed(inputMessage, REFRESH_INTERVAL);
                        break;
                    }
                    default:
                        //super.handleMessage(inputMessage);
                }
            }
        }

    }
}
