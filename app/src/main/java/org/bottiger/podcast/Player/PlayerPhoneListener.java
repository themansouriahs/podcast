package org.bottiger.podcast.Player;

import android.content.Context;
import android.media.AudioManager;
import android.support.annotation.NonNull;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import org.bottiger.podcast.service.PlayerService;

/**
 * Created by apl on 20-01-2015.
 */
public class PlayerPhoneListener extends PhoneStateListener {

    private PlayerService mPlayerService;

    private boolean mResumePlayback = false;

    public PlayerPhoneListener(@NonNull PlayerService argPlayerService) {
        mPlayerService = argPlayerService;
    }

    @Override
    public void onCallStateChanged(int state, String incomingNumber) {
        if (state == TelephonyManager.CALL_STATE_RINGING) {
            AudioManager audioManager = (AudioManager) mPlayerService.getSystemService(Context.AUDIO_SERVICE);
            int ringvolume = audioManager
                    .getStreamVolume(AudioManager.STREAM_RING);
            if (ringvolume > 0) {
                mResumePlayback = (mPlayerService.isPlaying() || mResumePlayback)
                        && (mPlayerService.getCurrentItem() != null);
                mPlayerService.pause();
            }
        } else if (state == TelephonyManager.CALL_STATE_OFFHOOK) {
            mResumePlayback = (mPlayerService.isPlaying() || mResumePlayback)
                    && (mPlayerService.getCurrentItem() != null);
            mPlayerService.pause();
        } else if (state == TelephonyManager.CALL_STATE_IDLE) {
            if (mResumePlayback) {

                mPlayerService.startAndFadeIn();
                mResumePlayback = false;
            }
        }
    }
}
