package org.bottiger.podcast.Player;

import android.content.Context;
import android.media.AudioManager;
import android.support.annotation.NonNull;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import org.bottiger.podcast.service.PlayerService;

import java.util.Date;

/**
 * Created by apl on 20-01-2015.
 */
public class PlayerPhoneListener extends PhoneStateListener {

    private static final int RESTART_THRESHOLD_SECONDS = 5*60; // 5 min

    private PlayerService mPlayerService;

    private Date mStoppedAt = null;

    public PlayerPhoneListener(@NonNull PlayerService argPlayerService) {
        mPlayerService = argPlayerService;
    }

    @Override
    public void onCallStateChanged(int state, String incomingNumber) {
        if (state == TelephonyManager.CALL_STATE_RINGING) {
            AudioManager audioManager = (AudioManager) mPlayerService.getSystemService(Context.AUDIO_SERVICE);
            int ringvolume = audioManager
                    .getStreamVolume(AudioManager.STREAM_RING);
            if (ringvolume > 0 && mPlayerService.isPlaying()) {
                pausePlayer();
            }
        } else if (state == TelephonyManager.CALL_STATE_OFFHOOK) {
            pausePlayer();
        } else if (state == TelephonyManager.CALL_STATE_IDLE) {
            resumePlayer();
        }
    }

    private void pausePlayer() {
        if (mPlayerService.isPlaying()) {
            mStoppedAt = new Date();
            mPlayerService.pause();
        }
    }

    private void resumePlayer() {
        if (mStoppedAt == null)
            return;

        Date now = new Date();

        long msDiff = now.getTime()-mStoppedAt.getTime();
        if (msDiff < RESTART_THRESHOLD_SECONDS*1000) {
            mPlayerService.startAndFadeIn();
        }

        mStoppedAt = null;

    }
}
