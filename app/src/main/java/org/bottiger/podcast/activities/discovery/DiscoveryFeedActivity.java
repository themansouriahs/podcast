package org.bottiger.podcast.activities.discovery;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

import org.bottiger.podcast.R;
import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.activities.feedview.FeedActivity;
import org.bottiger.podcast.activities.feedview.FeedViewAdapter;
import org.bottiger.podcast.provider.SlimImplementations.SlimSubscription;
import org.bottiger.podcast.provider.Subscription;
import org.bottiger.podcast.utils.PaletteHelper;

import java.net.URL;

import static android.view.View.GONE;

/**
 * Created by apl on 23-04-2015.
 */
public class DiscoveryFeedActivity extends FeedActivity {

    private static final String TAG = "DiscoveryFeedActivity";

    private Button mSubscribeButton;
    private View mSubscribeContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "Starting DiscoveryFeedActivity"); // NoI18N
        super.onCreate(savedInstanceState);
        PaletteHelper.generate(mSubscription.getImageURL(), this, mFloatingButton);

        mSubscribeContainer = findViewById(R.id.feed_subscribe_layout);
        mSubscribeButton = (Button) findViewById(R.id.feed_subscribe_button);

        mSubscribeContainer.setVisibility(View.VISIBLE);

        mSubscribeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (mSubscription instanceof SlimSubscription) {
                    SoundWaves.getAppContext(DiscoveryFeedActivity.this).getLibraryInstance().subscribe(mSubscription);
                } else {
                    SoundWaves.getAppContext(DiscoveryFeedActivity.this).getLibraryInstance().subscribe(mSubscription.getURL().toString());
                }

                mSubscribeContainer.setVisibility(GONE);
            }
        });
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
}
