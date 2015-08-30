package org.bottiger.podcast.webservices.directories.gpodder;

import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import org.apache.commons.validator.routines.UrlValidator;
import org.bottiger.podcast.flavors.CrashReporter.VendorCrashReporter;
import org.bottiger.podcast.provider.ISubscription;
import org.bottiger.podcast.provider.SlimImplementations.SlimSubscription;
import org.bottiger.podcast.provider.Subscription;
import org.bottiger.podcast.webservices.datastore.IWebservice;
import org.bottiger.podcast.webservices.datastore.gpodder.GPodderAPI;
import org.bottiger.podcast.webservices.datastore.gpodder.datatypes.GSubscription;
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

import retrofit.Call;
import retrofit.Response;

/**
 * Created by apl on 13-04-2015.
 */
public class GPodder extends GenericDirectory {

    private static final String NAME = "gPodder";
    private static final String QUERY_SEPARATOR = " ";

    private UrlValidator mUrlValidator = new UrlValidator();

    private GPodderAPI mGPodderAPI;
    private Call mCall;

    public GPodder() {
        super(NAME);
        mGPodderAPI = new GPodderAPI();
    }

    protected AsyncTask getAsyncTask() {
        return null;
    }

    @Override
    public void search(@NonNull ISearchParameters argParameters, @NonNull final Callback argCallback) {
        String searchTerm = TextUtils.join(QUERY_SEPARATOR, argParameters.getKeywords());
        final GenericSearchResult result = new GenericSearchResult(searchTerm);

        mCall = mGPodderAPI.search(searchTerm, new IWebservice.ICallback<List<GSubscription>>() {
            @Override
            public void onResponse(Response<List<GSubscription>> response) {

                if (response == null || !response.isSuccess())
                    return;

                //return podcasts;
                for (GSubscription podcast : response.body()) {
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

                argCallback.result(result);
            }

            @Override
            public void onFailure(Throwable throwable) {

            }
        });
    }

    public void abortSearch() {
        if (mCall == null)
            return;

        mCall.cancel();
    }
}
