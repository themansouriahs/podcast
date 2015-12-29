package org.bottiger.podcast.views.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.IntDef;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.TextView;

import org.bottiger.podcast.R;
import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.flavors.CrashReporter.VendorCrashReporter;
import org.bottiger.podcast.player.SoundWavesPlayer;
import org.bottiger.podcast.playlist.Playlist;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.provider.ISubscription;
import org.bottiger.podcast.provider.Subscription;
import org.bottiger.podcast.service.PlayerService;
import org.bottiger.podcast.utils.PlaybackSpeed;
import org.bottiger.podcast.utils.rxbus.RxBusSimpleEvents;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.TimeUnit;

import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.subscriptions.CompositeSubscription;

/**
 * Created by Arvid on 8/30/2015.
 */
public class DialogPlaybackSpeed extends DialogFragment {

    private static final String TAG = "DialogPlaybackSpeed";

    @IntDef({EPISODE, SUBSCRIPTION, GLOBAL})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Scope {}

    public static final int EPISODE = 0;
    public static final int SUBSCRIPTION = 1;
    public static final int GLOBAL = 2;

    private static final String scopeName = "Scope";

    private Activity mActivity;

    private Button mIncrementButton;
    private Button mDecrementButton;
    private TextView mCurrentSpeed;

    private RadioButton mRadioEpisode;
    private RadioButton mRadioSubscription;
    private RadioButton mRadioGlobal;

    private float mOriginalPlaybackSpeed = PlaybackSpeed.UNDEFINED;

    private CompositeSubscription mRxSubscriptions = new CompositeSubscription();

    public static DialogPlaybackSpeed newInstance(@Scope int argScope) {
        DialogPlaybackSpeed frag = new DialogPlaybackSpeed();
        Bundle args = new Bundle();
        args.putInt(scopeName, argScope);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mActivity = getActivity();

        @Scope int scope = getArguments().getInt(scopeName, EPISODE);

        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        // Get the layout inflater
        LayoutInflater inflater = mActivity.getLayoutInflater();

        View view = inflater.inflate(R.layout.dialog_playback_speed, null);

        // bind stuff
        mIncrementButton = (Button) view.findViewById(R.id.playback_increment_button);
        mDecrementButton = (Button) view.findViewById(R.id.playback_decrement_button);
        mCurrentSpeed = (TextView) view.findViewById(R.id.playback_speed_value);

        mRadioEpisode = (RadioButton) view.findViewById(R.id.radio_playback_speed_episode);
        mRadioSubscription = (RadioButton) view.findViewById(R.id.radio_playback_speed_subscription);
        mRadioGlobal = (RadioButton) view.findViewById(R.id.radio_playback_speed_global);

        final PlayerService ps = PlayerService.getInstance();
        final SoundWavesPlayer player;
        if (ps != null) {

            player = ps.getPlayer();

            Playlist playlist = ps.getPlaylist();
            if (playlist != null) {
                IEpisode episode = playlist.first();

                if (episode != null) {
                    mOriginalPlaybackSpeed = episode.getPlaybackSpeed();
                    scope = SUBSCRIPTION;
                }
            }


            if (mOriginalPlaybackSpeed == PlaybackSpeed.UNDEFINED) {
                mOriginalPlaybackSpeed = player.getCurrentSpeedMultiplier();
                scope = GLOBAL;
            }
        } else {
            player = null;
        }

        if (mOriginalPlaybackSpeed == PlaybackSpeed.UNDEFINED) {
            mOriginalPlaybackSpeed = PlaybackSpeed.DEFAULT;
        }

        setPlaybackSpeed(mOriginalPlaybackSpeed);

        mRxSubscriptions
                .add(SoundWaves.getRxBus().toObserverable()
                        .ofType(RxBusSimpleEvents.PlaybackSpeedChanged.class)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Action1<RxBusSimpleEvents.PlaybackSpeedChanged>() {
                            @Override
                            public void call(RxBusSimpleEvents.PlaybackSpeedChanged playbackSpeedChanged) {
                                Log.d(TAG, "new playback: " + playbackSpeedChanged.speed);
                                setSpeed(playbackSpeedChanged.speed);
                            }
                        }, new Action1<Throwable>() {
                            @Override
                            public void call(Throwable throwable) {
                                VendorCrashReporter.report("subscribeError" , throwable.toString());
                                Log.d(TAG, "error: " + throwable.toString());
                            }
                        }));

        setSpeed(mOriginalPlaybackSpeed);
        checkRadioButton(scope);

        mIncrementButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (player != null) {
                    float newPlaybackSpeed = mOriginalPlaybackSpeed + PlaybackSpeed.sSpeedIncrements;
                    Log.d(TAG, "increment playback speed to: " + newPlaybackSpeed);
                    setSpeed(newPlaybackSpeed);
                    player.setPlaybackSpeed(newPlaybackSpeed);
                }
            }
        });

        mDecrementButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (player != null) {
                    float newPlaybackSpeed = mOriginalPlaybackSpeed - PlaybackSpeed.sSpeedIncrements;
                    Log.d(TAG, "dencrement playback speed to: " + newPlaybackSpeed);
                    setSpeed(newPlaybackSpeed);
                    player.setPlaybackSpeed(newPlaybackSpeed);
                }
            }
        });

        builder.setView(view);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                setPlaybackSpeed(mOriginalPlaybackSpeed);

                if (mRadioSubscription.isChecked() && ps != null) {
                    IEpisode episode = ps.getPlaylist().first();
                    if (episode instanceof FeedItem) {
                        ISubscription subscription = episode.getSubscription();
                        if (subscription instanceof Subscription) {
                            Subscription subscription1 = (Subscription)subscription;
                            int storedSpeed = Math.round(mOriginalPlaybackSpeed * 10);
                            subscription1.setPlaybackSpeed(storedSpeed);
                        }
                    }
                }

                if (mRadioGlobal.isChecked()) {
                    /*
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mActivity);
                    String key = getResources().getString(R.string.soundwaves_player_playback_speed_key);
                    int storedSpeed = Math.round(mOriginalPlaybackSpeed * 10);
                    prefs.edit().putInt(key, storedSpeed).apply();
                    */
                    PlaybackSpeed.setGlobalPlaybackSpeed(mActivity, mOriginalPlaybackSpeed);
                }
            }
        });

        return builder.create();
    }

    @Override
    public void onDestroyView() {
        mRxSubscriptions.unsubscribe();
        super.onDestroyView();
    }

    private void checkRadioButton(@Scope int argScope) {
        RadioButton activeButton = null;
        switch (argScope) {
            case EPISODE: {
                activeButton = mRadioEpisode;
                break;
            }
            case SUBSCRIPTION: {
                activeButton = mRadioSubscription;
                break;
            }
            case GLOBAL: {
                activeButton = mRadioGlobal;
                break;
            }
        }

        if (activeButton == null) {
            Log.wtf(TAG, "activeButton should never be null");
            return;
        }

        activeButton.setChecked(true);
    }

    private void setSpeed(float argNewSpeed) {
        if (argNewSpeed > PlaybackSpeed.sSpeedMaximum || argNewSpeed < PlaybackSpeed.sSpeedMinimum)
            return;

        argNewSpeed = Math.round(argNewSpeed*10)/10.0f;

        mOriginalPlaybackSpeed = argNewSpeed;
        mCurrentSpeed.setText(getSpeedText(mOriginalPlaybackSpeed));

        mIncrementButton.setEnabled(argNewSpeed < PlaybackSpeed.sSpeedMaximum);
        mDecrementButton.setEnabled(argNewSpeed > PlaybackSpeed.sSpeedMinimum);
    }

    private String getSpeedText(float argSpeed) {
        return String.format("%.1f", argSpeed) + "x"; // NoI18N
    }

    private void setPlaybackSpeed(float argSpeed) {
        PlayerService ps = PlayerService.getInstance();
        if (ps != null) {
            ps.getPlayer().setPlaybackSpeed(argSpeed);
        }
    }

}
