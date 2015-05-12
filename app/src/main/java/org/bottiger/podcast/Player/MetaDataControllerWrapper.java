package org.bottiger.podcast.Player;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.service.PlayerService;

/**
 * Created by apl on 12-02-2015.
 */
public class MetaDataControllerWrapper {

    @Nullable
    private PlayerStateManager mPlayerStateManager = null;

    @Nullable
    private LegacyRemoteController mLegacyRemoteController = null;

    @TargetApi(21)
    public MetaDataControllerWrapper(@NonNull PlayerStateManager argPlayerStateManager) {
        mPlayerStateManager = argPlayerStateManager;
    }

    public MetaDataControllerWrapper(@NonNull LegacyRemoteController argLegacyRemoteController) {
        mLegacyRemoteController = argLegacyRemoteController;
    }

    public void updateState(@NonNull IEpisode argEpisode, boolean updateAlbumArt) {
        if (Build.VERSION.SDK_INT >= 21) {
            mPlayerStateManager.updateState(argEpisode, updateAlbumArt);
        } else {
            mLegacyRemoteController.updateMetaData();
        }
    }

    public void register(@NonNull PlayerService argPlayerService) {
        if (Build.VERSION.SDK_INT < 21) {
            mLegacyRemoteController.register(argPlayerService);
        }
    }
}
