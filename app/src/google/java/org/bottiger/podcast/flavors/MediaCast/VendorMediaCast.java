package org.bottiger.podcast.flavors.MediaCast;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.media.MediaControlIntent;
import android.support.v7.media.MediaRouter;
import android.util.Log;

import com.google.android.gms.cast.ApplicationMetadata;
import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.MediaStatus;
import com.google.android.gms.cast.RemoteMediaPlayer;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

import org.bottiger.podcast.R;
import org.bottiger.podcast.provider.FeedItem;

import java.io.IOException;
import java.net.URLConnection;

/**
 * Created by apl on 28-03-2015.
 */
public class VendorMediaCast implements IMediaCast {

    private static final String TAG = "VendorMediaCast";
    private static final String APPLICATION_ID = "CC1AD845";
    private final CastDevice mSelectedDevice;

    private Context mContext;
    private GoogleApiClient mApiClient;
    //private Cast.Listener mCastClientListener;
    private ConnectionCallbacks mConnectionCallbacks = new ConnectionCallbacks();
    private ConnectionFailedListener mConnectionFailedListener = new ConnectionFailedListener();
    private boolean mWaitingForReconnect = false;
    private boolean mIsConnected = false;
    private boolean mApplicationStarted;

    //cast demo
    private PendingIntent mSessionStatusUpdateIntent;
    private String mSessionId;
    private MediaRouter.RouteInfo mCurrentRoute;

    private RemoteMediaPlayer mRemoteMediaPlayer = new RemoteMediaPlayer();

    private SoundWavesChannel mSoundWavesChannel;

    private Cast.Listener mCastClientListener = new Cast.Listener() {
        @Override
        public void onApplicationStatusChanged() {
            if (mApiClient != null) {
                Log.d(TAG, "onApplicationStatusChanged: "
                        + Cast.CastApi.getApplicationStatus(mApiClient));
            }
        }

        @Override
        public void onVolumeChanged() {
            if (mApiClient != null) {
                Log.d(TAG, "onVolumeChanged: " + Cast.CastApi.getVolume(mApiClient));
            }
        }

        @Override
        public void onApplicationDisconnected(int errorCode) {
            teardown();
        }
    };

    public VendorMediaCast(Context argContext, MediaRouter router, MediaRouter.RouteInfo info) {
        mContext = argContext;
        mSoundWavesChannel = new SoundWavesChannel();
        mSelectedDevice = CastDevice.getFromBundle(info.getExtras());
        String routeId = info.getId();

        mCurrentRoute = info;

        if (mSelectedDevice == null)
            return;

        Cast.CastOptions.Builder apiOptionsBuilder = Cast.CastOptions
                .builder(mSelectedDevice, mCastClientListener);

        mApiClient = new GoogleApiClient.Builder(mContext)
                .addApi(Cast.API, apiOptionsBuilder.build())
                .addConnectionCallbacks(mConnectionCallbacks)
                .addOnConnectionFailedListener(mConnectionFailedListener)
                .build();


        mRemoteMediaPlayer.setOnStatusUpdatedListener(
                new RemoteMediaPlayer.OnStatusUpdatedListener() {
                    @Override
                    public void onStatusUpdated() {
                        MediaStatus mediaStatus = mRemoteMediaPlayer.getMediaStatus();
                        boolean isPlaying = mediaStatus.getPlayerState() ==
                                MediaStatus.PLAYER_STATE_PLAYING;
                        //...
                    }
                });

        mRemoteMediaPlayer.setOnMetadataUpdatedListener(
                new RemoteMediaPlayer.OnMetadataUpdatedListener() {
                    @Override
                    public void onMetadataUpdated() {
                        MediaInfo mediaInfo = mRemoteMediaPlayer.getMediaInfo();
                        MediaMetadata metadata = mediaInfo.getMetadata();
                        //...
                    }
                });

        startSession();
        connect();
    }

    public void launch() {
        try {
            Cast.CastApi.setMessageReceivedCallbacks(mApiClient,
                    mRemoteMediaPlayer.getNamespace(), mRemoteMediaPlayer);
        } catch (IOException e) {
            Log.e(TAG, "Exception while creating media channel", e);
        }
        mRemoteMediaPlayer
                .requestStatus(mApiClient)
                .setResultCallback(
                        new ResultCallback<RemoteMediaPlayer.MediaChannelResult>() {
                            @Override
                            public void onResult(RemoteMediaPlayer.MediaChannelResult result) {
                                if (!result.getStatus().isSuccess()) {
                                    Log.e(TAG, "Failed to request status.");
                                }
                            }
                        });

    }

    public void loadMedia() {
        loadMedia("Test", "http://www.podtrac.com/pts/redirect.mp3/twit.cachefly.net/audio/sn/sn0430/sn0430.mp3", "audio/mpeg");
    }

    public void loadMedia(@NonNull String argTitle, @NonNull String argUrl, @NonNull String argContentType) {
        MediaMetadata mediaMetadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE);
        mediaMetadata.putString(MediaMetadata.KEY_TITLE, argTitle);
        MediaInfo mediaInfo = new MediaInfo.Builder(
                argUrl)
                .setContentType(argContentType) // "video/mp4"
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setMetadata(mediaMetadata)
                .build();
        try {
            mRemoteMediaPlayer.load(mApiClient, mediaInfo, true)
                    .setResultCallback(new ResultCallback<RemoteMediaPlayer.MediaChannelResult>() {
                        @Override
                        public void onResult(RemoteMediaPlayer.MediaChannelResult result) {
                            if (result.getStatus().isSuccess()) {
                                Log.d(TAG, "Media loaded successfully");
                            }
                        }
                    });
        } catch (IllegalStateException e) {
            Log.e(TAG, "Problem occurred with media during loading", e);
        } catch (Exception e) {
            Log.e(TAG, "Problem opening media during loading", e);
        }
    }

    @Override
    public void connect() {
        mApiClient.connect();
    }

    @Override
    public void disconnect() {
        mApiClient.disconnect();
    }

    @Override
    public boolean isConnected() {
        return mIsConnected;
    }

    @Override
    public boolean loadEpisode(FeedItem argEpisode) {
        String title = argEpisode.getTitle();
        String url = argEpisode.getURL();
        String mimeType= URLConnection.guessContentTypeFromName(url);

        loadMedia(title, url, mimeType);
        return true;
    }

    @Override
    public void play() {

    }

    @Override
    public void pause() {

    }

    @Override
    public void seekTo(long argPositionMs) {

    }

    private void startSession() {
        Intent intent = new Intent(MediaControlIntent.ACTION_START_SESSION);
        intent.addCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK);
        intent.putExtra(MediaControlIntent.EXTRA_SESSION_STATUS_UPDATE_RECEIVER,
                mSessionStatusUpdateIntent);
        intent.putExtra(CastMediaControlIntent.EXTRA_CAST_APPLICATION_ID,
                CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID);
        intent.putExtra(CastMediaControlIntent.EXTRA_CAST_RELAUNCH_APPLICATION,
                false); // getRelaunchApp()
        intent.putExtra(CastMediaControlIntent.EXTRA_DEBUG_LOGGING_ENABLED, true);
        if (true) { // getStopAppWhenEndingSession()
            intent.putExtra(CastMediaControlIntent.EXTRA_CAST_STOP_APPLICATION_WHEN_SESSION_ENDS,
                    true);
        }
        sendIntentToRoute(intent, new ResultBundleHandler() {
            @Override
            public void handleResult(Bundle bundle) {
                mSessionId = bundle.getString(MediaControlIntent.EXTRA_SESSION_ID);
                Log.d(TAG, "Got a session ID of: " + mSessionId);
            }
        });
    }
    private interface ResultBundleHandler {
        public void handleResult(Bundle bundle);
    }

    /*
 * Sends a prebuilt media control intent to the selected route.
 */
    private void sendIntentToRoute(final Intent intent, final ResultBundleHandler resultHandler) {
        String sessionId = intent.getStringExtra(MediaControlIntent.EXTRA_SESSION_ID);
        Log.d(TAG, "sending intent to route: " + intent + ", session: " + sessionId);
        if ((mCurrentRoute == null) || !mCurrentRoute.supportsControlRequest(intent)) {
            Log.d(TAG, "route is null or doesn't support this request");
            return;
        }

        mCurrentRoute.sendControlRequest(intent, new MediaRouter.ControlRequestCallback() {
            @Override
            public void onResult(Bundle data) {
                Log.d(TAG, "got onResult for " + intent.getAction() + " with bundle " + data);
                if (data != null) {
                    if (resultHandler != null) {
                        resultHandler.handleResult(data);
                    }
                } else {
                    Log.w(TAG, "got onResult with a null bundle");
                }
            }

            @Override
            public void onError(String message, Bundle data) {
                Log.w(TAG, "error");
                //showErrorDialog(message != null ? message
                //        : getString(R.string.mrp_request_failed));
            }
        });
    }

    private class ConnectionCallbacks implements
            GoogleApiClient.ConnectionCallbacks {

        @Override
        public void onConnected(Bundle connectionHint) {
            mIsConnected = true;
            if (mWaitingForReconnect) {
                mWaitingForReconnect = false;
                //reconnectChannels();
            } else {
                try {

                    Cast.CastApi.launchApplication(mApiClient, APPLICATION_ID, false)
                            .setResultCallback(
                                    new ResultCallback<Cast.ApplicationConnectionResult>() {
                                        @Override
                                        public void onResult(Cast.ApplicationConnectionResult result) {
                                            Status status = result.getStatus();
                                            if (status.isSuccess()) {
                                                ApplicationMetadata applicationMetadata =
                                                        result.getApplicationMetadata();
                                                String sessionId = result.getSessionId();
                                                String applicationStatus = result.getApplicationStatus();
                                                boolean wasLaunched = result.getWasLaunched();

                                                loadMedia();
                                                //...
                                            } else {
                                                teardown();
                                            }
                                        }
                                    });


                } catch (Exception e) {
                    Log.e(TAG, "Failed to launch application", e);
                }
            }
        }

        @Override
        public void onConnectionSuspended(int cause) {
            mWaitingForReconnect = true;
        }
    }

    private class ConnectionFailedListener implements
            GoogleApiClient.OnConnectionFailedListener {
        @Override
        public void onConnectionFailed(ConnectionResult result) {
            teardown();
        }
    }

    private void teardown() {
        mIsConnected = false;
    }

    class SoundWavesChannel implements Cast.MessageReceivedCallback {
        public String getNamespace() {
            return "urn:x-cast:com.example.custom";
        }

        @Override
        public void onMessageReceived(CastDevice castDevice, String namespace,
                                      String message) {
            Log.d(TAG, "onMessageReceived: " + message);
        }
    }

    public void afterConnect() {
        Cast.CastApi.launchApplication(mApiClient, APPLICATION_ID, false)
                .setResultCallback(
                        new ResultCallback<Cast.ApplicationConnectionResult>() {
                            @Override
                            public void onResult(Cast.ApplicationConnectionResult result) {
                                Status status = result.getStatus();
                                if (status.isSuccess()) {
                                    ApplicationMetadata applicationMetadata =
                                            result.getApplicationMetadata();
                                    String sessionId = result.getSessionId();
                                    String applicationStatus = result.getApplicationStatus();
                                    boolean wasLaunched = result.getWasLaunched();

                                    mApplicationStarted = true;
                                    try {
                                        Cast.CastApi.setMessageReceivedCallbacks(mApiClient,
                                                mSoundWavesChannel.getNamespace(),
                                                mSoundWavesChannel);
                                    } catch (IOException e) {
                                        Log.e(TAG, "Exception while creating channel", e);
                                    }
                                }
                            }
                        });
    }

    private void sendMessage(String message) {
        if (mApiClient != null && mSoundWavesChannel != null) {
            try {
                Cast.CastApi.sendMessage(mApiClient, mSoundWavesChannel.getNamespace(), message)
                        .setResultCallback(
                                new ResultCallback<Status>() {
                                    @Override
                                    public void onResult(Status result) {
                                        if (!result.isSuccess()) {
                                            Log.e(TAG, "Sending message failed");
                                        }
                                    }
                                });
            } catch (Exception e) {
                Log.e(TAG, "Exception while sending message", e);
            }
        }
    }
}
