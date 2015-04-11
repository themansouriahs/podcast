package org.bottiger.podcast.flavors.MediaCast;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.media.MediaControlIntent;
import android.support.v7.media.MediaItemMetadata;
import android.support.v7.media.MediaItemStatus;
import android.support.v7.media.MediaRouter;
import android.support.v7.media.MediaSessionStatus;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.common.images.WebImage;

import org.bottiger.podcast.R;
import org.bottiger.podcast.provider.FeedItem;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by apl on 11-04-2015.
 */
public class MediaRouteCast implements IMediaCast {

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
    private boolean mStreamAdvancing;
    private ResultBundleHandler mMediaResultHandler;
    private MediaInfo mPendingMedia;

    @NonNull private Activity mActivity;

    public MediaRouteCast(Activity argActivity) {
        mActivity = argActivity;

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

        // from onResume
        mActivity.registerReceiver(mSessionStatusBroadcastReceiver, mSessionStatusBroadcastIntentFilter);
        mActivity.registerReceiver(mMediaStatusBroadcastReceiver, mMediaStatusBroadcastIntentFilter);
    }

    @Override
    public void connect() {

    }

    @Override
    public void disconnect() {

    }

    @Override
    public boolean isConnected() {
        return false;
    }

    @Override
    public boolean loadEpisode(FeedItem argEpisode) {
        return false;
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

    private void clearStreamState() {
        mStreamAdvancing = false;
        mStreamPositionTimestamp = 0;
        mLastKnownStreamPosition = 0;
        //setPlayerState(PLAYER_STATE_NONE);
        refreshPlaybackPosition(0, 0);
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

                //setPlayerState(playerState);
                mCurrentItemId = itemId;
                //setCurrentMediaMetadata(title, artist, imageUrl);
                //updateButtonStates();

                mStreamDuration = itemStatus.getContentDuration();
                mLastKnownStreamPosition = itemStatus.getContentPosition();
                mStreamPositionTimestamp = itemStatus.getTimestamp();

                Log.d(TAG, "stream position now: " + mLastKnownStreamPosition);

                // Only refresh playback position if stream is moving.
                mStreamAdvancing = (playbackState == MediaItemStatus.PLAYBACK_STATE_PLAYING);
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
                    onPlayMedia(media);
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

    protected void onPlayMedia(final MediaInfo media) {
        if (media == null) {
            return;
        }

        if (mSessionId == null) {
            // Need to start a session first.
            mPendingMedia = media;
            startSession();
            return;
        }

        MediaMetadata metadata = media.getMetadata();
        Log.d(TAG, "Casting " + metadata.getString(MediaMetadata.KEY_TITLE) + " ("
                + media.getContentType() + ")");

        Intent intent = new Intent(MediaControlIntent.ACTION_PLAY);
        intent.addCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK);
        intent.setDataAndType(Uri.parse(media.getContentId()), media.getContentType());
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
        //setPlayerState(PLAYER_STATE_NONE);
        mCurrentItemId = null;
        //updateButtonStates();
    }

    private interface ResultBundleHandler {
        public void handleResult(Bundle bundle);
    }
}
