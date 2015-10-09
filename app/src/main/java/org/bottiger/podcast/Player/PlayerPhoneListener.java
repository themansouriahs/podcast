package org.bottiger.podcast.player;

import android.Manifest;
import android.content.Context;
import android.media.AudioManager;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresPermission;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import org.bottiger.podcast.service.PlayerService;

import java.util.Date;

/**
 * Created by apl on 20-01-2015.
 *
 * This class is used for listening to changes in the phone state.
 * We monitor this because we want to handle the situation where the user was listening
 * to a podcast and a phone call comes in. The phone call will stop the podcast, but if it's a
 * short call we want to resume the playback afterwards.
 */
public class PlayerPhoneListener extends PhoneStateListener {

    private static final int RESTART_THRESHOLD_SECONDS = 5*60; // 5 min

    private PlayerService mPlayerService;

    private Date mStoppedAt = null;

    public PlayerPhoneListener(@NonNull PlayerService argPlayerService) {
        mPlayerService = argPlayerService;
        listenForChanges(true);
    }

    public void listenForChanges(boolean argDoListen) {
        TelephonyManager tmgr = (TelephonyManager) mPlayerService.getSystemService(Context.TELEPHONY_SERVICE);

        final int listenToChanges = argDoListen ? PhoneStateListener.LISTEN_CALL_STATE : PhoneStateListener.LISTEN_NONE;
        tmgr.listen(this, listenToChanges);
    }

    @RequiresPermission(Manifest.permission_group.PHONE)
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
