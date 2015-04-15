package org.bottiger.podcast.webservices.directories.gpodder;

import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.dragontek.mygpoclient.pub.PublicClient;
import com.dragontek.mygpoclient.simple.IPodcast;

import org.apache.commons.validator.routines.UrlValidator;
import org.bottiger.podcast.flavors.CrashReporter.VendorCrashReporter;
import org.bottiger.podcast.provider.ISubscription;
import org.bottiger.podcast.provider.SlimImplementations.SlimSubscription;
import org.bottiger.podcast.provider.Subscription;
import org.bottiger.podcast.webservices.directories.IDirectoryProvider;
import org.bottiger.podcast.webservices.directories.ISearchParameters;
import org.bottiger.podcast.webservices.directories.ISearchResult;
import org.bottiger.podcast.webservices.directories.generic.GenericDirectory;
import org.bottiger.podcast.webservices.directories.generic.GenericSearchResult;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by apl on 13-04-2015.
 */
public class GPodder extends GenericDirectory {

    private static final String QUERY_SEPARATOR = " ";

    private UrlValidator mUrlValidator = new UrlValidator();
    private QueryGpodder asyncTask = null;

    @Override
    public void search(@NonNull ISearchParameters argParameters, @NonNull Callback argCallback) {
        asyncTask = new QueryGpodder(argCallback);

        String result = TextUtils.join(QUERY_SEPARATOR, argParameters.getKeywords());
        asyncTask.execute(result);
    }

    private class QueryGpodder extends AsyncTask<String, Void, ISearchResult> {

        private Callback mCallback;

        public QueryGpodder(@NonNull Callback argCallback) {
            mCallback = argCallback;
        }

        protected ISearchResult doInBackground(String... string) {
            PublicClient gpodderClient = new PublicClient();
            List<IPodcast> podcasts;
            GenericSearchResult result = new GenericSearchResult();

            try {
                podcasts = gpodderClient.searchPodcast(string[0]);
            } catch (IOException e) {
                e.printStackTrace();
                mCallback.error(e);
                return null;
            }

            //return podcasts;
            for (IPodcast podcast : podcasts) {
                String title = podcast.getTitle();
                String urlString = podcast.getUrl();
                String imageUrl = podcast.getLogoUrl();

                if (TextUtils.isEmpty(title)) {
                    continue;
                }

                if (!mUrlValidator.isValid(urlString)) {
                    continue;
                }

                URL url;
                try {
                    url = new URL(urlString);
                } catch (MalformedURLException e) {
                    VendorCrashReporter.report("URL invalid", urlString);
                    continue;
                }

                ISubscription subscription = new SlimSubscription(title, url, imageUrl);
                result.addResult(subscription);
            }


            return result;
        }

        protected void onPostExecute(ISearchResult argResult) {
            if (argResult != null) {
                mCallback.result(argResult);
            }
        }
    }
}
