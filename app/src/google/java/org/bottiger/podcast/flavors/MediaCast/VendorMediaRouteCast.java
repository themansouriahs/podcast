package org.bottiger.podcast.flavors.MediaCast;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Menu;

import com.google.android.gms.cast.framework.CastButtonFactory;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.SessionManager;
import com.google.android.gms.cast.framework.SessionManagerListener;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.flavors.player.googlecast.GoogleCastPlayer;
import org.bottiger.podcast.listeners.PlayerStatusObservable;
import org.bottiger.podcast.player.SoundWavesPlayer;
import org.bottiger.podcast.provider.IEpisode;

/**
 * Created by apl on 11-04-2015.
 */
public class VendorMediaRouteCast extends GoogleCastPlayer {

    private static final String TAG = VendorMediaRouteCast.class.getSimpleName();

    @NonNull
    private final Activity mActivity;

    @Nullable
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
    public void FadeOutAndStop(int argDelayMs) {
        stop();
    }

    public VendorMediaRouteCast(@NonNull Activity argActivity) {
        super(argActivity);
        mActivity = argActivity;

        // New
        GoogleApiAvailability api = GoogleApiAvailability.getInstance();
        int code = api.isGooglePlayServicesAvailable(mActivity);
        if (code == ConnectionResult.SUCCESS) {
            // Do Your Stuff Here
            mCastContext = CastContext.getSharedInstance(argActivity);
        }else {
            mCastContext = null;
        }
    }

    private void setClient() {
        if (isMissingPlayServices())
            return;

        mRemoteMediaClient = mCastSession.getRemoteMediaClient();

        if (mRemoteMediaClient == null)
            return;

        mRemoteMediaClient.addListener(getRemoteMediaClientListener());

        IEpisode episode = SoundWaves.getAppContext(mActivity).getPlaylist().first();
        if (episode != null) {
            setDataSourceAsync(episode);
        }
    }

    private void setSoundWavesPlayer() {
        SoundWaves.getAppContext(mActivity).setPlayer(this);
    }

    private void unsetClient() {
        mRemoteMediaClient = null;
        SoundWaves.getAppContext(mActivity).setPlayer(new SoundWavesPlayer(mActivity));
    }

    public void setupMediaButton(@NonNull Context argContext, @NonNull Menu menu, @IdRes int argMenuResource) {
        if (isMissingPlayServices())
            return;

        CastButtonFactory.setUpMediaRouteButton(argContext, menu, argMenuResource);
    }

    public void onCreate() {
        if (isMissingPlayServices())
            return;

        mSessionManager = CastContext.getSharedInstance(mActivity).getSessionManager();
    }

    public void onResume() {
        if (isMissingPlayServices())
            return;

        mCastSession = mSessionManager.getCurrentCastSession();
        mSessionManager.addSessionManagerListener(mSessionManagerListener);
    }

    public void onPause() {
        if (isMissingPlayServices())
            return;

        mSessionManager.removeSessionManagerListener(mSessionManagerListener);
        mCastSession = null;
    }

    @Nullable
    @Override
    public RemoteMediaClient getRemoteMediaClient() {
        return mRemoteMediaClient;
    }

    public boolean isMissingPlayServices() {
        return mCastContext == null;
    }

    private class SessionManagerListenerImpl implements SessionManagerListener<CastSession> {
        @Override
        public void onSessionStarting(CastSession session) {
            Log.d(TAG, "session starting: " + session.getApplicationStatus());

            setSoundWavesPlayer();

            setState(PlayerStatusObservable.PREPARING);
        }

        @Override
        public void onSessionStarted(CastSession session, String sessionId) {
            Log.d(TAG, "session started: " + session.getApplicationStatus());

            mCastSession = session;
            mActivity.invalidateOptionsMenu();
            setClient();

            setState(PlayerStatusObservable.PLAYING);
        }

        @Override
        public void onSessionStartFailed(CastSession session, int i) {
            Log.d(TAG, "session start failed: " + session.getApplicationStatus());
            setState(PlayerStatusObservable.STOPPED);
        }

        @Override
        public void onSessionEnding(CastSession session) {
            Log.d(TAG, "session ending: " + session.getApplicationStatus());
        }

        @Override
        public void onSessionResumeFailed(CastSession session, int i) {
            Log.d(TAG, "session resumed failed: " + session.getApplicationStatus());
            setState(PlayerStatusObservable.STOPPED);
        }

        @Override
        public void onSessionSuspended(CastSession session, int i) {
            Log.d(TAG, "session suspended: " + session.getApplicationStatus());
            setState(PlayerStatusObservable.STOPPED);
        }

        @Override
        public void onSessionEnded(CastSession session, int error) {
            Log.d(TAG, "session ended: " + session.getApplicationStatus());
            if (session == mCastSession) {
                mCastSession = null;
            }
            mActivity.invalidateOptionsMenu();
            unsetClient();

            setState(PlayerStatusObservable.STOPPED);
        }

        @Override
        public void onSessionResuming(CastSession session, String s) {
            Log.d(TAG, "session resuming: " + session.getApplicationStatus());
            setState(PlayerStatusObservable.PREPARING);
        }

        @Override
        public void onSessionResumed(CastSession castSession, boolean b) {
            Log.d(TAG, "session resumed: " + castSession.getApplicationStatus());
            mCastSession = castSession;
            mActivity.invalidateOptionsMenu();

            setState(PlayerStatusObservable.PLAYING);
        }
    }

}
