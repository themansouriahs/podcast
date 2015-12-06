package org.bottiger.podcast.activities.discovery;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;

import org.bottiger.podcast.R;
import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.activities.feedview.FeedActivity;
import org.bottiger.podcast.provider.Subscription;
import org.bottiger.podcast.utils.PaletteHelper;

import java.net.URL;

/**
 * Created by apl on 23-04-2015.
 */
public class DiscoveryFeedActivity extends FeedActivity {

    private static final String TAG = "DiscoveryFeedActivity";

    protected Button mSubscribeButton;
    protected View mSubscribeContainer;

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

                //URL url = mSubscription.getURL();
                //Subscription subscription = new Subscription(url.toString());
                //subscription.subscribe(DiscoveryFeedActivity.this);
                SoundWaves.getLibraryInstance().subscribe(mSubscription.getURL().toString());

                // if (Build.VERSION.SDK_INT >= 19) {
                //TransitionManager.beginDelayedTransition(mSubscribeContainer);
                mSubscribeContainer.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }
}