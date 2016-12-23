package org.bottiger.podcast.activities.discovery;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.util.Log;
import android.view.Menu;
import android.view.View;

import org.bottiger.podcast.R;
import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.activities.feedview.FeedActivity;
import org.bottiger.podcast.activities.feedview.FeedViewAdapter;
import org.bottiger.podcast.flavors.CrashReporter.VendorCrashReporter;
import org.bottiger.podcast.model.Library;
import org.bottiger.podcast.provider.ISubscription;
import org.bottiger.podcast.provider.SlimImplementations.SlimSubscription;
import org.bottiger.podcast.provider.Subscription;
import org.bottiger.podcast.provider.base.BaseSubscription;
import org.bottiger.podcast.utils.ColorExtractor;
import org.bottiger.podcast.utils.UIUtils;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by apl on 23-04-2015.
 */
public class DiscoveryFeedActivity extends FeedActivity {

    private static final String TAG = "DiscoveryFeedActivity";

    private boolean mIsSubscribed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "Starting DiscoveryFeedActivity"); // NoI18N
        super.onCreate(savedInstanceState);

        mNoEpisodesView.setVisibility(View.INVISIBLE);

        ISubscription subscription = getSubscription();
        if (subscription != null) {
            subscription.getColors(this)
                    .subscribeOn(io.reactivex.schedulers.Schedulers.io())
                    .observeOn(io.reactivex.android.schedulers.AndroidSchedulers.mainThread())
                    .subscribe(new BaseSubscription.BasicColorExtractorObserver<ColorExtractor>() {
                        @Override
                        public void onSuccess(ColorExtractor value) {
                            DiscoveryFeedActivity.this.onColorExtractorFound(value);
                        }
                    });
        }

        mIsSubscribed = mSubscription instanceof Subscription;

        mFloatingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Library library = SoundWaves.getAppContext(DiscoveryFeedActivity.this).getLibraryInstance();
                if (!mIsSubscribed) {
                    library.subscribe(mSubscription);
                } else {
                    library.unsubscribe(mSubscription.getURLString(), "DiscoveryFeedActivity");
                }

                mIsSubscribed = !mIsSubscribed;
                tintButton();

                UIUtils.displaySubscribedSnackbar(!mIsSubscribed, mSubscription, mMultiShrinkScroller, DiscoveryFeedActivity.this);

            }
        });

        tintButton();
    }

    @Override
    @Nullable
    protected ISubscription processIntent() {
        ISubscription subscription = super.processIntent();

        if (subscription != null)
            return subscription;

        URL url = parseIntent(getIntent());
        SlimSubscription slimSubscription = null;

        if (url != null) {
            slimSubscription = new SlimSubscription(url);
            setSubscription(slimSubscription);
        }

        return slimSubscription;
    }

    @NonNull
    @Override
    protected FeedViewAdapter getAdapter() {
        FeedViewAdapter adapter;

        mProgress = new ProgressDialog(this);
        mProgress.setMessage(getString(R.string.discovery_progress_loading_podcast_content));
        mProgress.show();
        adapter = new FeedViewDiscoveryAdapter(this, mSubscription);
        SoundWaves.getAppContext(this).getRefreshManager().refresh(mSubscription, getIDownloadCompleteCallback());

        return adapter;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    private void tintButton() {
        Drawable drawable = ContextCompat.getDrawable(this, R.drawable.ic_rss_feed_24dp);

        // Wrap the drawable so that future tinting calls work
        // on pre-v21 devices. Always use the returned drawable.
        drawable = DrawableCompat.wrap(drawable);

        // We can now set a tint
        int colorRes = mIsSubscribed ? R.color.orange : R.color.white_opaque;
        int tintColor = ContextCompat.getColor(this, colorRes);
        DrawableCompat.setTint(drawable, tintColor);
        // ...and a different tint mode
        //DrawableCompat.setTintMode(drawable, PorterDuff.Mode.SRC_OVER);

        //setFABDrawable(R.drawable.ic_rss_feed_24dp);
        mFloatingButton.setImageDrawable(drawable);
    }

    @Nullable
    private static URL parseIntent(@Nullable Intent argIntent) {

        if (argIntent == null) {
            return null;
        }

        URL url = null;

        try {
            if (Intent.ACTION_SEND.equals(argIntent.getAction())) {
                String urlstr = argIntent.getStringExtra(Intent.EXTRA_TEXT);
                url = new URL(urlstr);
            } else {
                Uri data = argIntent.getData();
                url = parseUri(data);
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
            VendorCrashReporter.handleException(e);
        }

        return url;
    }

    private static URL parseUri(@NonNull Uri argUri) throws MalformedURLException {
        String scheme = argUri.getScheme();

        String url = argUri.toString();

        switch (scheme) {
            case "http":
            case "https": {
                break;
            }
            case "pcast":
            case "soundwaves": {
                url = replaceScheme(url);
                break;
            }
        }

        return new URL(url);
    }

    private static String replaceScheme(@NonNull String url) {
        // pcast
        url = url.replace("pcast://", "");
        url = url.replace("pcast:", "");

        // soundwaves
        url = url.replace("soundwaves://subscribe/", "");
        url = url.replace("soundwaves://", "");
        url = url.replace("soundwaves:", "");

        url = "http://" + url;

        return url;
    }
}
