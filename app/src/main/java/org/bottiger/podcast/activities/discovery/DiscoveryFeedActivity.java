package org.bottiger.podcast.activities.discovery;

import android.app.ProgressDialog;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.util.Log;
import android.view.Menu;
import android.view.View;

import org.bottiger.podcast.R;
import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.activities.feedview.FeedActivity;
import org.bottiger.podcast.activities.feedview.FeedViewAdapter;
import org.bottiger.podcast.model.Library;
import org.bottiger.podcast.provider.Subscription;
import org.bottiger.podcast.utils.PaletteHelper;
import org.bottiger.podcast.utils.UIUtils;

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
        PaletteHelper.generate(mSubscription.getImageURL(), this, mFloatingButton);

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

    @NonNull
    @Override
    protected FeedViewAdapter getAdapter() {
        FeedViewAdapter adapter;

        mProgress = new ProgressDialog(this);
        mProgress.setMessage(getString(R.string.discovery_progress_loading_podcast_content));
        mProgress.show();
        adapter = new FeedViewDiscoveryAdapter(this, mSubscription);
        SoundWaves.getAppContext(this).getRefreshManager().refresh(mSubscription, mRefreshCompleteCallback);

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
}
