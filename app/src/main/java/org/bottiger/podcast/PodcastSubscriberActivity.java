package org.bottiger.podcast;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import org.bottiger.podcast.activities.feedview.FeedActivity;
import org.bottiger.podcast.provider.ISubscription;
import org.bottiger.podcast.provider.SlimImplementations.SlimSubscription;
import org.bottiger.podcast.provider.Subscription;
import org.bottiger.podcast.service.IDownloadCompleteCallback;

import java.net.MalformedURLException;
import java.net.URL;

public class PodcastSubscriberActivity extends Activity {

    private static final String TAG = "PodcastSubscriberAct";

    private ProgressDialog mProgress;
    private URL url;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "Starting PodcastSubscriberActivity"); // NoI18N

        Intent intent = getIntent();

        try {
            if (Intent.ACTION_SEND.equals(intent.getAction())) {
                String urlstr = intent.getStringExtra(Intent.EXTRA_TEXT);
                url = new URL(urlstr);
            } else {
                Uri data = intent.getData();
                url = new URL(data.toString());
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
            finish();
        }

        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onStart() {
        super.onStart();  // Always call the superclass method first

        IDownloadCompleteCallback mRefreshCompleteCallback = new IDownloadCompleteCallback() {
            @Override
            public void complete(boolean argSucces, ISubscription argSubscription) {

                if (!argSucces) {
                    finish();
                }

                mProgress.dismiss();

                if (!argSucces)
                    return;

                // FIXME why do I get Subscriptions here?
                if (argSubscription instanceof Subscription)
                    return;

                SlimSubscription slimSubscription = (SlimSubscription)argSubscription;
                FeedActivity.startSlim(PodcastSubscriberActivity.this, slimSubscription.getURLString(), slimSubscription);
            }
        };

        ISubscription subscription = new SlimSubscription("", url, "");

        mProgress = new ProgressDialog(this);
        mProgress.setMessage(getString(R.string.discovery_progress_loading_podcast_content));
        mProgress.show();
        SoundWaves.getAppContext(this).getRefreshManager().refresh(subscription, mRefreshCompleteCallback);
    }


}
