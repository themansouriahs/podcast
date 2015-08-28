package org.bottiger.podcast.Player.sonic;

import android.support.annotation.NonNull;

import com.aocate.presto.service.IDeathCallback_0_8;
import com.aocate.presto.service.IOnBufferingUpdateListenerCallback_0_8;
import com.aocate.presto.service.IOnCompletionListenerCallback_0_8;
import com.aocate.presto.service.IOnErrorListenerCallback_0_8;
import com.aocate.presto.service.IOnInfoListenerCallback_0_8;
import com.aocate.presto.service.IOnPitchAdjustmentAvailableChangedListenerCallback_0_8;
import com.aocate.presto.service.IOnPreparedListenerCallback_0_8;
import com.aocate.presto.service.IOnSeekCompleteListenerCallback_0_8;
import com.aocate.presto.service.IOnSpeedAdjustmentAvailableChangedListenerCallback_0_8;
import com.aocate.presto.service.IPlayMedia_0_8;

import org.bottiger.podcast.service.PlayerService;

/**
 * Created by Arvid on 8/27/2015.
 */
public class SonicPlayer {

    private static final String TAG = "SonicPlayer";

    @NonNull
    private PlayerService mPlayerService;

    public SonicPlayer(@NonNull PlayerService argPlayerService) {
        mPlayerService = argPlayerService;
    }

}
