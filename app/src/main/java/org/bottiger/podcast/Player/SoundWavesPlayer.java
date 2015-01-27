package org.bottiger.podcast.Player;

import android.content.ComponentName;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.support.annotation.NonNull;

import org.bottiger.podcast.listeners.PlayerStatusObservable;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.receiver.HeadsetReceiver;
import org.bottiger.podcast.service.PlayerService;

import java.io.IOException;

/**
 * Created by apl on 20-01-2015.
 */
public class SoundWavesPlayer {
    private MediaPlayer mMediaPlayer = new MediaPlayer();

    private PlayerService mPlayerService;
    private Handler mHandler;
    private boolean mIsInitialized = false;

    // AudioManager
    private AudioManager mAudioManager;
    private ComponentName mControllerComponentName;

    private boolean isPreparingMedia = false;

    int bufferProgress = 0;

    int startPos = 0;

    public SoundWavesPlayer(@NonNull PlayerService argPlayerService) {
        mPlayerService = argPlayerService;
        this.mControllerComponentName = new ComponentName(mPlayerService,
                HeadsetReceiver.class);
        this.mAudioManager = (AudioManager) mPlayerService.getSystemService(Context.AUDIO_SERVICE);
        // mMediaPlayer.setWakeMode(PlayerService.this,
        // PowerManager.PARTIAL_WAKE_LOCK);
    }

    public void setDataSourceAsync(String path, int startPos) {
        try {
            mMediaPlayer.reset();
            mMediaPlayer.setDataSource(path);
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

            this.startPos = startPos;
            this.isPreparingMedia = true;

            mMediaPlayer.setOnPreparedListener(preparedlistener);
            mMediaPlayer.prepareAsync();
        } catch (IOException ex) {
            // TODO: notify the user why the file couldn't be opened
            mIsInitialized = false;
            return;
        } catch (IllegalArgumentException ex) {
            // TODO: notify the user why the file couldn't be opened
            mIsInitialized = false;
            return;
        }
        mMediaPlayer.setOnCompletionListener(listener);
        mMediaPlayer.setOnBufferingUpdateListener(bufferListener);
        mMediaPlayer.setOnErrorListener(errorListener);

        mIsInitialized = true;
    }

    public boolean isInitialized() {
        return mIsInitialized;
    }

    public void toggle() {
        if (mMediaPlayer.isPlaying())
            pause();
        else
            start();
    }

    public void start() {
        mPlayerService.notifyStatus();

        // Request audio focus for playback
        int result = mAudioManager.requestAudioFocus(mPlayerService,
                // Use the music stream.
                AudioManager.STREAM_MUSIC,
                // Request permanent focus.
                AudioManager.AUDIOFOCUS_GAIN);

        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            mAudioManager
                    .registerMediaButtonEventReceiver(mControllerComponentName);
            mMediaPlayer.start();

            PlayerStatusObservable
                    .updateStatus(PlayerStatusObservable.STATUS.PLAYING);
        }
    }

    public void stop() {
        mMediaPlayer.reset();
        mIsInitialized = false;
        mPlayerService.stopForeground(true);
        PlayerStatusObservable
                .updateStatus(PlayerStatusObservable.STATUS.STOPPED);
    }

    public void release() {
        mPlayerService.dis_notifyStatus();
        stop();
        mMediaPlayer.release();
        mAudioManager
                .unregisterMediaButtonEventReceiver(mControllerComponentName);
        mIsInitialized = false;
    }

    /**
     * Test of the extended_player is playing something right now
     *
     * @return Is the extended_player playing right now
     */
    public boolean isPlaying() {
        return mMediaPlayer.isPlaying();
    }


    public void rewind(FeedItem argItem) {
        if (mPlayerService == null)
            return;

        argItem.setPosition(mPlayerService.getContentResolver(), 0);

        if (argItem.equals(mPlayerService.getCurrentItem())) {
            mPlayerService.seek(0);
        }
    }

    /**
     * Pause the current playing item
     */
    public void pause() {
        mMediaPlayer.pause();
        PlayerStatusObservable
                .updateStatus(PlayerStatusObservable.STATUS.PAUSED);
    }

    @Deprecated
    public int getBufferProgress() {
        return this.bufferProgress;
    }

    public void setHandler(Handler handler) {
        mHandler = handler;
    }

    MediaPlayer.OnCompletionListener listener = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mp) {
            FeedItem item = mPlayerService.getCurrentItem();
            // item.markAsListened();
            // item.update(getContentResolver());

            if (!isPreparingMedia)
                mHandler.sendEmptyMessage(PlayerHandler.TRACK_ENDED);

        }
    };

    MediaPlayer.OnBufferingUpdateListener bufferListener = new MediaPlayer.OnBufferingUpdateListener() {
        @Override
        public void onBufferingUpdate(MediaPlayer mp, int percent) {
            SoundWavesPlayer.this.bufferProgress = percent;
        }
    };

    MediaPlayer.OnPreparedListener preparedlistener = new MediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(MediaPlayer mp) {
            // notifyChange(ASYNC_OPEN_COMPLETE);
            mp.seekTo(startPos);
            start();
            isPreparingMedia = false;
            PlayerService.setNextTrack(PlayerService.NextTrack.NEXT_IN_PLAYLIST);
        }
    };

    MediaPlayer.OnErrorListener errorListener = new MediaPlayer.OnErrorListener() {
        @Override
        public boolean onError(MediaPlayer mp, int what, int extra) {
            switch (what) {
                case MediaPlayer.MEDIA_ERROR_SERVER_DIED:

                    mPlayerService.dis_notifyStatus();
                    mIsInitialized = false;
                    mMediaPlayer.release();

                    mMediaPlayer = new MediaPlayer();
                    mHandler.sendMessageDelayed(
                            mHandler.obtainMessage(PlayerHandler.SERVER_DIED), 2000);
                    return true;
                default:
                    break;
            }
            return false;
        }
    };

    public long duration() {
        return mMediaPlayer.getDuration();
    }

    public long position() {
        return mMediaPlayer.getCurrentPosition();
    }

    public long seek(long whereto) {
        mMediaPlayer.seekTo((int) whereto);
        return whereto;
    }

    public void setVolume(float vol) {
        mMediaPlayer.setVolume(vol, vol);
    }
}
