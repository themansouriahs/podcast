package org.bottiger.podcast.flavors.MediaCast;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.media.MediaControlIntent;
import android.support.v7.media.MediaItemMetadata;
import android.support.v7.media.MediaItemStatus;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.support.v7.media.MediaSessionStatus;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.common.images.WebImage;

import org.bottiger.podcast.R;
import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.flavors.Analytics.IAnalytics;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.IEpisode;

import java.net.URLConnection;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by apl on 11-04-2015.
 */
public class VendorMediaRouteCast implements IMediaCast {

    private static final String TAG = "MediaRouteCast";
    private static final String ACTION_RECEIVE_SESSION_STATUS_UPDATE =
            "com.google.android.gms.cast.samples.democastplayer.RECEIVE_SESSION_STATUS_UPDATE";
    private static final String ACTION_RECEIVE_MEDIA_STATUS_UPDATE =
            "com.google.android.gms.cast.samples.democastplayer.RECEIVE_MEDIA_STATUS_UPDATE";

    private static final String mReceiverApplicationId = "CC1AD845";

    protected static final double VOLUME_INCREMENT = 0.05;
    protected static final double MAX_VOLUME_LEVEL = 20;

    protected static final int AFTER_SEEK_DO_NOTHING = 0;
    protected static final int AFTER_SEEK_PLAY = 1;
    protected static final int AFTER_SEEK_PAUSE = 2;

    protected static final int PLAYER_STATE_NONE = 0;
    protected static final int PLAYER_STATE_PLAYING = 1;
    protected static final int PLAYER_STATE_PAUSED = 2;
    protected static final int PLAYER_STATE_BUFFERING = 3;

    private static final String MEDIA_SELECTION_DIALOG_TAG = "media_selection";
    private static final String MEDIA_TRACK_SELECTION_DIALOG_TAG = "media_track_selection";
    private static final int REFRESH_INTERVAL_MS = (int) TimeUnit.SECONDS.toMillis(1);

    private MediaRouter.RouteInfo mCurrentRoute;
    private String mLastRouteId;
    private String mSessionId;
    private boolean mSessionActive;
    private PendingIntent mSessionStatusUpdateIntent;
    private IntentFilter mSessionStatusBroadcastIntentFilter;
    private BroadcastReceiver mSessionStatusBroadcastReceiver;
    private String mCurrentItemId;
    private PendingIntent mMediaStatusUpdateIntent;
    private IntentFilter mMediaStatusBroadcastIntentFilter;
    private BroadcastReceiver mMediaStatusBroadcastReceiver;
    private long mStreamPositionTimestamp;
    private long mLastKnownStreamPosition;
    private long mStreamDuration;
    private ResultBundleHandler mMediaResultHandler;

    private MediaInfo mPendingMedia;
    private long mStartOffset = 0;

    private boolean mPlayingInitialized = false;
    private boolean mStreamAdvancing;

    private MediaRouteSelector mMediaRouteSelector;
    private MediaRouter mMediaRouter;
    private boolean mRouteSelected;
    private boolean mDiscoveryActive;
    private MediaRouter.Callback mMediaRouterCallback;

    private MediaInfo mCurrentTrack;

    @Nullable
    private IMediaRouteStateListener mListener;

    @NonNull private Activity mActivity;
    private int mPlayerState;

    public VendorMediaRouteCast(Activity argActivity) {
        mActivity = argActivity;

        mMediaRouter = MediaRouter.getInstance(mActivity.getApplicationContext());
        mMediaRouterCallback = new MyMediaRouterCallback();

        // Construct a broadcast receiver and a PendingIntent for receiving session status
        // updates from the MRP.
        mSessionStatusBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "Got a session status broadcast intent from the MRP: " + intent);
                processSessionStatusBundle(intent.getExtras());
            }
        };
        mSessionStatusBroadcastIntentFilter = new IntentFilter(
                ACTION_RECEIVE_SESSION_STATUS_UPDATE);

        Intent intent = new Intent(ACTION_RECEIVE_SESSION_STATUS_UPDATE);
        intent.setComponent(mActivity.getCallingActivity());
        mSessionStatusUpdateIntent = PendingIntent.getBroadcast(mActivity, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        // Construct a broadcast receiver and a PendingIntent for receiving media status
        // updates from the MRP.
        mMediaStatusBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "Got a media status broadcast intent from the MRP: " + intent);
                processMediaStatusBundle(intent.getExtras());
            }
        };
        mMediaStatusBroadcastIntentFilter = new IntentFilter(ACTION_RECEIVE_MEDIA_STATUS_UPDATE);

        intent = new Intent(ACTION_RECEIVE_MEDIA_STATUS_UPDATE);
        intent.setComponent(mActivity.getCallingActivity());
        mMediaStatusUpdateIntent = PendingIntent.getBroadcast(mActivity, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        mMediaResultHandler = new ResultBundleHandler() {

            @Override
            public void handleResult(Bundle bundle) {
                processMediaStatusBundle(bundle);
                //updateButtonStates();
            }
        };


        clearStreamState();
        buildRouteSelector();
    }

    @Override
    public void connect() {

    }

    @Override
    public void disconnect() {

    }

    @Override
    public boolean isConnected() {
        return mRouteSelected;
    }

    @Override
    public boolean isPlaying() {
        return mStreamAdvancing;
    }

    @Override
    public boolean isActive() {
        return isConnected() && mCurrentTrack != null;
    }

    @Override
    public boolean loadEpisode(@NonNull IEpisode argEpisode) {
        String url = argEpisode.getUrl().toString();

        if (TextUtils.isEmpty(url))
            return false;

        String mimeType= URLConnection.guessContentTypeFromName(url);

        String subTitle = argEpisode.getSubscription(mActivity).getTitle();
        String artWorkURI = argEpisode.getArtwork(mActivity).toString();//.getArtwork(mActivity);

        MediaMetadata mediaMetadata = new MediaMetadata();
        mediaMetadata.putString(MediaMetadata.KEY_TITLE, argEpisode.getTitle());
        mediaMetadata.putString(MediaMetadata.KEY_ARTIST, subTitle);
        //mediaMetadata.putString(MediaMetadata.KEY_ARTWORK_URI, artWorkURI);

        MediaInfo.Builder mediaInfoBuilder = new MediaInfo.Builder(url)
                .setContentType(mimeType)
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setMetadata(mediaMetadata);
                //.setStreamType(streamType)
                //.setContentType(mimeType)
                //.setMetadata(metadata);

        mCurrentTrack = mediaInfoBuilder.build();

        return mCurrentTrack != null;
    }

    @Override
    public void play(long argStartPosition) {

        mPlayingInitialized = true;

        if (mPlayerState == PLAYER_STATE_NONE) {
            onPlayMedia(mCurrentTrack, argStartPosition);
            SoundWaves.sAnalytics.trackEvent(IAnalytics.EVENT_TYPE.MEDIA_ROUTING);
            return;
        }

        if (mCurrentItemId == null) {
            mPlayingInitialized = false;
            return;
        }

        String action = isPlaying() ? MediaControlIntent.ACTION_PAUSE : MediaControlIntent.ACTION_RESUME;

        Intent intent = new Intent(action);
        intent.addCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK);
        intent.putExtra(MediaControlIntent.EXTRA_SESSION_ID, mSessionId);
        sendIntentToRoute(intent, mMediaResultHandler);

    }

    @Override
    public void pause() {
        mPlayingInitialized = false;
        if (mCurrentItemId == null) {
            return;
        }

        Intent intent = new Intent(MediaControlIntent.ACTION_PAUSE);
        intent.addCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK);
        intent.putExtra(MediaControlIntent.EXTRA_SESSION_ID, mSessionId);
        sendIntentToRoute(intent, mMediaResultHandler);
    }

    @Override
    public void stop() {
        pause();
    }

    @Override
    public int getCurrentPosition() {
        /*
        if ((mStreamPositionTimestamp != 0) && mStreamAdvancing
                && (mCurrentItemId != null) && (mLastKnownStreamPosition < mStreamDuration)) {
            long extrapolatedStreamPosition = mLastKnownStreamPosition
                    + (SystemClock.uptimeMillis() - mStreamPositionTimestamp);
            if (extrapolatedStreamPosition > mStreamDuration) {
                extrapolatedStreamPosition = mStreamDuration;
            }
            return (int) extrapolatedStreamPosition;
        }
        return 0;*/
        if (!mStreamAdvancing) {
            return mLastKnownStreamPosition > 0 ? (int)mLastKnownStreamPosition : 0;
        }

        long extrapolatedStreamPosition = mLastKnownStreamPosition
                + (SystemClock.uptimeMillis() - mStreamPositionTimestamp);
        if (extrapolatedStreamPosition > mStreamDuration) {
            extrapolatedStreamPosition = mStreamDuration;
        }

        if (extrapolatedStreamPosition <0 )
            extrapolatedStreamPosition = 0;

        return (int) extrapolatedStreamPosition;
    }

    @Override
    public void seekTo(long argPositionMs) {
        if (mCurrentItemId == null) {
            return;
        }

        //refreshPlaybackPosition(argPositionMs, -1);

        Intent intent = new Intent(MediaControlIntent.ACTION_SEEK);
        intent.addCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK);
        intent.putExtra(MediaControlIntent.EXTRA_ITEM_ID, mCurrentItemId);
        intent.putExtra(MediaControlIntent.EXTRA_ITEM_CONTENT_POSITION, argPositionMs);
        intent.putExtra(MediaControlIntent.EXTRA_SESSION_ID, mSessionId);
        sendIntentToRoute(intent, mMediaResultHandler);
    }

    public void registerStateChangedListener(IMediaRouteStateListener argListener) {
        mListener = argListener;
    }

    public void unregisterStateChangedListener(IMediaRouteStateListener argListener) {
        mListener = null;
    }

    private void clearStreamState() {
        mStreamAdvancing = false;
        mStreamPositionTimestamp = 0;
        mLastKnownStreamPosition = 0;
        setPlayerState(PLAYER_STATE_NONE);
        refreshPlaybackPosition(0, 0);
    }

    protected final void setPlayerState(int playerState) {
        mPlayerState = playerState;
    }

    /**
     *
     * @param position The stream position, or 0 if no media is currently loaded, or -1 to leave
     * the value unchanged.
     * @param duration The stream duration, or 0 if no media is currently loaded, or -1 to leave
     * the value unchanged.
     */
    protected final void refreshPlaybackPosition(long position, long duration) {
        /*
        if (!mIsUserSeeking) {
            if (position == 0) {
                mStreamPositionTextView.setText(R.string.no_time);
                mSeekBar.setProgress(0);
            } else if (position > 0) {
                mSeekBar.setProgress((int) TimeUnit.MILLISECONDS.toSeconds(position));
            }
            mStreamPositionTextView.setText(formatTime(position));
        }

        if (duration == 0) {
            mStreamDurationTextView.setText(R.string.no_time);
            mSeekBar.setMax(0);
        } else if (duration > 0) {
            mStreamDurationTextView.setText(formatTime(duration));
            if (!mIsUserSeeking) {
                mSeekBar.setMax((int) TimeUnit.MILLISECONDS.toSeconds(duration));
            }
        }
        */
    }

    /*
     * Processes a received media status bundle and updates the UI accordingly.
     */
    private void processMediaStatusBundle(Bundle statusBundle) {
        Log.d(TAG, "processMediaStatusBundle()");
        String itemId = statusBundle.getString(MediaControlIntent.EXTRA_ITEM_ID);
        Log.d(TAG, "itemId = " + itemId);

        String title = null;
        String artist = null;
        Uri imageUrl = null;

        // Extract item metadata, if available.
        if (statusBundle.containsKey(MediaControlIntent.EXTRA_ITEM_METADATA)) {
            Bundle metadataBundle = (Bundle) statusBundle.getParcelable(
                    MediaControlIntent.EXTRA_ITEM_METADATA);

            title = metadataBundle.getString(MediaItemMetadata.KEY_TITLE);
            artist = metadataBundle.getString(MediaItemMetadata.KEY_ARTIST);
            if (metadataBundle.containsKey(MediaItemMetadata.KEY_ARTWORK_URI)) {
                imageUrl = Uri.parse(metadataBundle.getString(MediaItemMetadata.KEY_ARTWORK_URI));
            }
        } else {
            Log.d(TAG, "status bundle had no metadata!");
        }

        // Extract the item status, if available.
        if ((itemId != null) && statusBundle.containsKey(MediaControlIntent.EXTRA_ITEM_STATUS)) {
            Bundle itemStatusBundle = (Bundle) statusBundle.getParcelable(
                    MediaControlIntent.EXTRA_ITEM_STATUS);
            MediaItemStatus itemStatus = MediaItemStatus.fromBundle(itemStatusBundle);

            int playbackState = itemStatus.getPlaybackState();
            Log.d(TAG, "playbackState=" + playbackState);

            if ((playbackState == MediaItemStatus.PLAYBACK_STATE_CANCELED)
                    || (playbackState == MediaItemStatus.PLAYBACK_STATE_INVALIDATED)
                    || (playbackState == MediaItemStatus.PLAYBACK_STATE_ERROR)
                    || (playbackState == MediaItemStatus.PLAYBACK_STATE_FINISHED)) {
                clearCurrentMediaItem();
                mStreamAdvancing = false;
            } else if ((playbackState == MediaItemStatus.PLAYBACK_STATE_PAUSED)
                    || (playbackState == MediaItemStatus.PLAYBACK_STATE_PLAYING)
                    || (playbackState == MediaItemStatus.PLAYBACK_STATE_BUFFERING)) {

                int playerState = PLAYER_STATE_NONE;
                if (playbackState == MediaItemStatus.PLAYBACK_STATE_PAUSED) {
                    playerState = PLAYER_STATE_PAUSED;
                } else if (playbackState == MediaItemStatus.PLAYBACK_STATE_PLAYING) {
                    playerState = PLAYER_STATE_PLAYING;
                } else if (playbackState == MediaItemStatus.PLAYBACK_STATE_BUFFERING) {
                    playerState = PLAYER_STATE_BUFFERING;
                }

                setPlayerState(playerState);
                mCurrentItemId = itemId;
                //setCurrentMediaMetadata(title, artist, imageUrl);
                //updateButtonStates();

                mStreamDuration = itemStatus.getContentDuration();
                mLastKnownStreamPosition = itemStatus.getContentPosition();
                mStreamPositionTimestamp = itemStatus.getTimestamp();

                Log.d(TAG, "stream position now: " + mLastKnownStreamPosition);

                // Only refresh playback position if stream is moving.
                mStreamAdvancing = (playbackState == MediaItemStatus.PLAYBACK_STATE_PLAYING);
                mPlayingInitialized = false;

                if (mStreamAdvancing) {
                    refreshPlaybackPosition(mLastKnownStreamPosition, mStreamDuration);
                }
            } else {
                Log.d(TAG, "Unexpected playback state: " + playbackState);
            }

            Bundle extras = itemStatus.getExtras();
            if (extras != null) {
                if (extras.containsKey(MediaItemStatus.EXTRA_HTTP_STATUS_CODE)) {
                    int httpStatus = extras.getInt(MediaItemStatus.EXTRA_HTTP_STATUS_CODE);
                    Log.d(TAG, "HTTP status: " + httpStatus);
                }
                if (extras.containsKey(MediaItemStatus.EXTRA_HTTP_RESPONSE_HEADERS)) {
                    Bundle headers = extras.getBundle(MediaItemStatus.EXTRA_HTTP_RESPONSE_HEADERS);
                    Log.d(TAG, "HTTP headers: " + headers);
                }
            }
        }
    }


    /*
 * Processes a received session status bundle and updates the UI accordingly.
 */
    private void processSessionStatusBundle(Bundle statusBundle) {
        Log.d(TAG, "processSessionStatusBundle()");

        String sessionId = statusBundle.getString(MediaControlIntent.EXTRA_SESSION_ID);
        MediaSessionStatus status = MediaSessionStatus.fromBundle(
                statusBundle.getBundle(MediaControlIntent.EXTRA_SESSION_STATUS));
        int sessionState = status.getSessionState();

        Log.d(TAG, "got a session status update for session " + sessionId + ", state = "
                + sessionState + ", mSessionId=" + mSessionId);

        if (mSessionId == null) {
            return;
        }

        if (!mSessionId.equals(sessionId)) {
            // Got status on a session other than the one we're tracking. Ignore it.
            Log.d(TAG, "Received status for unknown session: " + sessionId);
            return;
        }

        switch (sessionState) {
            case MediaSessionStatus.SESSION_STATE_ACTIVE:
                Log.d(TAG, "session " + sessionId + " is ACTIVE");
                mSessionActive = true;
                if (mPendingMedia != null) {
                    MediaInfo media = mPendingMedia;
                    mPendingMedia = null;
                    onPlayMedia(media, mStartOffset);
                } else {
                    syncStatus();
                }
                break;

            case MediaSessionStatus.SESSION_STATE_ENDED:
                Log.d(TAG, "session " + sessionId + " is ENDED");
                mSessionId = null;
                mSessionActive = false;
                clearCurrentMediaItem();
                break;

            case MediaSessionStatus.SESSION_STATE_INVALIDATED:
                Log.d(TAG, "session " + sessionId + " is INVALIDATED");
                mSessionId = null;
                mSessionActive = false;
                clearCurrentMediaItem();
                break;

            default:
                Log.d(TAG, "Received unexpected session state: " + sessionState);
                break;
        }

        //updateButtonStates();
    }

    protected void onPlayMedia(final MediaInfo media, final long argStartPosition) {
        if (media == null) {
            return;
        }

        if (mSessionId == null) {
            // Need to start a session first.
            mPendingMedia = media;
            mStartOffset = argStartPosition;
            startSession();
            return;
        }

        MediaMetadata metadata = media.getMetadata();
        Log.d(TAG, "Casting " + metadata.getString(MediaMetadata.KEY_TITLE) + " ("
                + media.getContentType() + ")");

        Intent intent = new Intent(MediaControlIntent.ACTION_PLAY);
        intent.addCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK);
        intent.setDataAndType(Uri.parse(media.getContentId()), media.getContentType());
        intent.putExtra(MediaControlIntent.EXTRA_ITEM_CONTENT_POSITION, argStartPosition);
        intent.putExtra(MediaControlIntent.EXTRA_SESSION_ID, mSessionId);
        intent.putExtra(MediaControlIntent.EXTRA_ITEM_STATUS_UPDATE_RECEIVER,
                mMediaStatusUpdateIntent);

        Bundle metadataBundle = new Bundle();

        String title = metadata.getString(MediaMetadata.KEY_TITLE);
        if (!TextUtils.isEmpty(title)) {
            metadataBundle.putString(MediaItemMetadata.KEY_TITLE, title);
        }

        List<WebImage> images = metadata.getImages();
        String artist = metadata.getString(MediaMetadata.KEY_ARTIST);
        if (artist == null) {
            artist = metadata.getString(MediaMetadata.KEY_STUDIO);
        }
        if (!TextUtils.isEmpty(artist)) {
            metadataBundle.putString(MediaItemMetadata.KEY_ARTIST, artist);
        }

        if ((images != null) && !images.isEmpty()) {
            Uri imageUrl = images.get(0).getUrl();
            if (imageUrl != null) {
                metadataBundle.putString(MediaItemMetadata.KEY_ARTWORK_URI, imageUrl.toString());
            }
        }

        intent.putExtra(MediaControlIntent.EXTRA_ITEM_METADATA, metadataBundle);

        sendIntentToRoute(intent, mMediaResultHandler);
    }


    private void startSession() {
        Intent intent = new Intent(MediaControlIntent.ACTION_START_SESSION);
        intent.addCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK);
        intent.putExtra(MediaControlIntent.EXTRA_SESSION_STATUS_UPDATE_RECEIVER,
                mSessionStatusUpdateIntent);
        intent.putExtra(CastMediaControlIntent.EXTRA_CAST_APPLICATION_ID,
                mReceiverApplicationId);
        intent.putExtra(CastMediaControlIntent.EXTRA_CAST_RELAUNCH_APPLICATION,
                getRelaunchApp());
        intent.putExtra(CastMediaControlIntent.EXTRA_DEBUG_LOGGING_ENABLED, true);
        if (getStopAppWhenEndingSession()) {
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

    // FIXME implement this
    private boolean getStopAppWhenEndingSession() {
        return true;
    }

    // FIXME implement this
    private boolean getRelaunchApp() {
        return true;
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
                showErrorDialog(message != null ? message
                        : mActivity.getString(R.string.mrp_request_failed));
            }
        });
    }

    protected final void showErrorDialog(String errorString) {
        /*
        if (!isFinishing()) {
            new AlertDialog.Builder(this).setTitle(R.string.error)
                    .setMessage(errorString)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    })
                    .create()
                    .show();
        }
        */
    }

    private void syncStatus() {
        Log.d(TAG, "Invoking SYNC_STATUS request");
        Intent intent = new Intent(CastMediaControlIntent.ACTION_SYNC_STATUS);
        intent.addCategory(CastMediaControlIntent.categoryForRemotePlayback());
        intent.putExtra(MediaControlIntent.EXTRA_SESSION_ID, mSessionId);
        intent.putExtra(MediaControlIntent.EXTRA_ITEM_STATUS_UPDATE_RECEIVER,
                mMediaStatusUpdateIntent);
        sendIntentToRoute(intent, mMediaResultHandler);
    }

    private void clearCurrentMediaItem() {
        //setCurrentMediaMetadata(null, null, null);
        refreshPlaybackPosition(0, 0);
        setPlayerState(PLAYER_STATE_NONE);
        mCurrentItemId = null;
        //updateButtonStates();
    }

    public void startDiscovery() {
        mMediaRouter.addCallback(mMediaRouteSelector, mMediaRouterCallback,
                MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY);
        mDiscoveryActive = true;

        // from onResume
        mActivity.registerReceiver(mSessionStatusBroadcastReceiver, mSessionStatusBroadcastIntentFilter);
        mActivity.registerReceiver(mMediaStatusBroadcastReceiver, mMediaStatusBroadcastIntentFilter);
    }

    public void stopDiscovery() {
        mMediaRouter.removeCallback(mMediaRouterCallback);
        mDiscoveryActive = false;

        // from onResume
        mActivity.unregisterReceiver(mSessionStatusBroadcastReceiver);
        mActivity.unregisterReceiver(mMediaStatusBroadcastReceiver);
    }

    public MediaRouteSelector getRouteSelector() {
        return mMediaRouteSelector;
    }

    private void buildRouteSelector() {
        mMediaRouteSelector = new MediaRouteSelector.Builder()
                .addControlCategory(getControlCategory())
                .build();
    }

    protected String getControlCategory() {
        return CastMediaControlIntent.categoryForRemotePlayback(mReceiverApplicationId);
    }

    private void setSelectedRoute(MediaRouter.RouteInfo route) {
        clearStreamState();
        mCurrentRoute = route;
        //setCurrentDeviceName(route != null ? route.getName() : null);
    }

    private void requestSessionStatus() {
        if (mSessionId == null) {
            return;
        }

        Intent intent = new Intent(MediaControlIntent.ACTION_GET_SESSION_STATUS);
        intent.addCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK);
        intent.putExtra(MediaControlIntent.EXTRA_SESSION_ID, mSessionId);
        sendIntentToRoute(intent, null);
    }

    private interface ResultBundleHandler {
        public void handleResult(Bundle bundle);
    }

    private void notifyStateChanged() {
        IMediaRouteStateListener listener = mListener;
        if (listener != null) {
            listener.onStateChanged(VendorMediaRouteCast.this);
        }
    }

    private class MyMediaRouterCallback extends MediaRouter.Callback {
        @Override
        public void onRouteSelected(MediaRouter router, MediaRouter.RouteInfo route) {
            Log.d(TAG, "onRouteSelected: route=" + route);
            mRouteSelected = true;
            setSelectedRoute(route);
            //updateButtonStates();
            String routeId = route.getId();
            if (routeId.equals(mLastRouteId) && (mSessionId != null)) {
                // Try to rejoin the session by requesting status.
                Log.d(TAG, "Trying to rejoin previous session");
                requestSessionStatus();
                //updateButtonStates();
            }
            mLastRouteId = routeId;


            notifyStateChanged();
            //mListener.onStateChanged(VendorMediaRouteCast.this);
        }

        @Override
        public void onRouteUnselected(MediaRouter router, MediaRouter.RouteInfo route) {
            Log.d(TAG, "onRouteUnselected: route=" + route);
            mRouteSelected = false;
            setSelectedRoute(null);
            clearCurrentMediaItem();
            mSessionActive = false;
            mSessionId = null;
            //updateButtonStates();

            notifyStateChanged();
            //mListener.onStateChanged(VendorMediaRouteCast.this);
        }

        @Override
        public void onRouteVolumeChanged(MediaRouter router, MediaRouter.RouteInfo route) {
            //refreshDeviceVolume(route.getVolume() / MAX_VOLUME_LEVEL, false);
        }
    }
}
