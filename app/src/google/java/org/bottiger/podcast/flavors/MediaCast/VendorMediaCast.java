package org.bottiger.podcast.flavors.MediaCast;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.media.MediaRouter;
import android.util.Log;

import com.google.android.gms.cast.ApplicationMetadata;
import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.MediaStatus;
import com.google.android.gms.cast.RemoteMediaPlayer;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

import java.io.IOException;

/**
 * Created by apl on 28-03-2015.
 */
public class VendorMediaCast {

    private static final String TAG = "VendorMediaCast";
    private static final String APPLICATION_ID = "id";
    private final CastDevice mSelectedDevice;

    private Context mContext;
    private GoogleApiClient mApiClient;
    //private Cast.Listener mCastClientListener;
    private ConnectionCallbacks mConnectionCallbacks;
    private ConnectionFailedListener mConnectionFailedListener;
    private boolean mWaitingForReconnect = false;
    private boolean mApplicationStarted;

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

    public void launch() {

    }

    public void connect() {
        mApiClient.connect();
    }


    private class ConnectionCallbacks implements
            GoogleApiClient.ConnectionCallbacks {

        @Override
        public void onConnected(Bundle connectionHint) {
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
                                                //...
                                            } else {
                                                //teardown();
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
        Cast.CastApi.launchApplication(mApiClient, "YOUR_APPLICATION_ID", false)
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
