package org.bottiger.podcast.flavors.MediaCast;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.Menu;

import com.google.android.gms.cast.framework.CastButtonFactory;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.SessionManager;
import com.google.android.gms.cast.framework.SessionManagerListener;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;

import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.flavors.player.googlecast.GoogleCastPlayer;
import org.bottiger.podcast.player.SoundWavesPlayer;
import org.bottiger.podcast.provider.IEpisode;

/**
 * Created by apl on 11-04-2015.
 */
public class VendorMediaRouteCast extends GoogleCastPlayer {

    private static final String TAG = "MediaRouteCast";

    @NonNull
    private final Activity mActivity;

    @NonNull
    private final CastContext mCastContext;

    @Nullable
    private RemoteMediaClient mRemoteMediaClient;

    private CastSession mCastSession;
    private SessionManager mSessionManager;
    private final SessionManagerListener mSessionManagerListener =
            new SessionManagerListenerImpl();

    @Override
    public void startAndFadeIn() {
        start();
    }

    @Override
    public void FaceOutAndStop(int argDelayMs) {
        stop();
    }

    private class SessionManagerListenerImpl implements SessionManagerListener<CastSession> {
        @Override
        public void onSessionStarting(CastSession session) {
        }

        @Override
        public void onSessionStarted(CastSession session, String sessionId) {
            mCastSession = session;
            mActivity.invalidateOptionsMenu();
            setClient();
        }

        @Override
        public void onSessionStartFailed(CastSession session, int i) {

        }

        @Override
        public void onSessionEnding(CastSession session) {

        }

        @Override
        public void onSessionResumeFailed(CastSession session, int i) {

        }

        @Override
        public void onSessionSuspended(CastSession session, int i) {

        }

        @Override
        public void onSessionEnded(CastSession session, int error) {
            if (session == mCastSession) {
                mCastSession = null;
            }
            mActivity.invalidateOptionsMenu();
            unsetClient();
        }

        @Override
        public void onSessionResuming(CastSession session, String s) {

        }

        @Override
        public void onSessionResumed(CastSession castSession, boolean b) {
            mCastSession = castSession;
            mActivity.invalidateOptionsMenu();
        }
    }


    public VendorMediaRouteCast(@NonNull Activity argActivity) {
        super(argActivity);
        mActivity = argActivity;

        // New
        mCastContext = CastContext.getSharedInstance(argActivity);

    }

    private void setClient() {
        mRemoteMediaClient = mCastSession.getRemoteMediaClient();
        mRemoteMediaClient.addListener(getRemoteMediaClientListener());
        SoundWaves.getAppContext(mActivity).setPlayer(this);

        IEpisode episode = SoundWaves.getAppContext(mActivity).getPlaylist().first();
        if (episode != null) {
            setDataSourceAsync(episode);
        }
    }

    private void unsetClient() {
        mRemoteMediaClient = null;
        SoundWaves.getAppContext(mActivity).setPlayer(new SoundWavesPlayer(mActivity));
    }

    public void setupMediaButton(@NonNull Context argContext, @NonNull Menu menu, @IdRes int argMenuResource) {
        CastButtonFactory.setUpMediaRouteButton(argContext, menu, argMenuResource);
    }

    public void onCreate() {
        mSessionManager = CastContext.getSharedInstance(mActivity).getSessionManager();
    }

    public void onResume() {
        mCastSession = mSessionManager.getCurrentCastSession();
        mSessionManager.addSessionManagerListener(mSessionManagerListener);
    }

    public void onPause() {
        mSessionManager.removeSessionManagerListener(mSessionManagerListener);
        mCastSession = null;
    }

    @Nullable
    @Override
    public RemoteMediaClient getRemoteMediaClient() {
        return mRemoteMediaClient;
    }


}
