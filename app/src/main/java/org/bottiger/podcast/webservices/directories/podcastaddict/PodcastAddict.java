package org.bottiger.podcast.webservices.directories.podcastaddict;

import android.os.AsyncTask;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.bottiger.podcast.provider.SlimImplementations.SlimSubscription;
import org.bottiger.podcast.utils.ErrorUtils;
import org.bottiger.podcast.webservices.directories.IDirectoryProvider;
import org.bottiger.podcast.webservices.directories.ISearchParameters;
import org.bottiger.podcast.webservices.directories.ISearchResult;
import org.bottiger.podcast.webservices.directories.generic.GenericDirectory;
import org.bottiger.podcast.webservices.directories.generic.GenericSearchResult;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by aplb on 06-12-2016.
 */
public class PodcastAddict extends GenericDirectory {

    private static final String TAG = PodcastAddict.class.getSimpleName();

    /** Which action to perform when a track ends */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({AUDIO, VIDEO, ALL})
    @interface Type {}
    static final int AUDIO = 1;
    static final int VIDEO = 2;
    static final int ALL = 3;

    private static final String NAME = "PodcastAddict";
    private static final String BASE_URL = "http://addictpodcast.com/";

    private PodcastAddictEndpoint mService;

    private static String sLanguage = null;

    public PodcastAddict() {
        super(NAME);

        OkHttpClient client = new OkHttpClient.Builder().build();

        Retrofit mRetrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        mService = mRetrofit.create(PodcastAddictEndpoint.class);
    }

    @Override
    protected AsyncTask<String, Void, ISearchResult> getAsyncTask() {
        return null;
    }

    @Override
    public void search(@NonNull ISearchParameters argParameters, @NonNull final Callback argCallback) {
        final String searchString = TextUtils.join(" ", argParameters.getKeywords());
        SearchQuery query = getQuery(searchString);
        mService.search(query.mQuery,
                getLanguage(),
                getDateFilter(),
                getExplicitFilter(),
                getType(AUDIO))
                .enqueue(new retrofit2.Callback<SearchResult>() {
            @Override
            public void onResponse(Call<SearchResult> call, Response<SearchResult> response) {
                GenericSearchResult resultReturn = new GenericSearchResult(searchString);
                SearchResult results = response.body();

                List<SlimSubscription> result = results.toSubscriptions();

                for (int i = 0; i < result.size(); i++) {
                    resultReturn.addResult(result.get(i));
                }

                argCallback.result(resultReturn);
            }

            @Override
            public void onFailure(Call<SearchResult> call, Throwable t) {
                ErrorUtils.handleException(t);
            }
        });
    }

    @Override
    public void toplist(@NonNull Callback argCallback) {

    }

    @Override
    public void toplist(int amount, @Nullable String argTag, @NonNull Callback argCallback) {

    }

    @NonNull
    private SearchQuery getQuery(@Nullable String argQuery) {
        String query = argQuery != null ? argQuery : "";
        return new SearchQuery(query);
    }

    @NonNull
    private String getLanguage() {
        if (sLanguage != null)
            return sLanguage;

        sLanguage = "";

        String english = Locale.ENGLISH.getLanguage();
        String localLanguage = Locale.getDefault().getLanguage();

        if (english.equals(localLanguage)) {
            sLanguage = "'" + localLanguage + "', ";
        }

        sLanguage += "' " + english + "'";

        return sLanguage;
    }

    @NonNull
    private String getDateFilter() {
        return "0";
    }

    @NonNull
    private String getExplicitFilter() {
        return "0";
    }

    @Nullable
    private String getType(@Type int argType) {
        switch (argType) {
            case AUDIO:
                return "AUDIO"; // NoI18N
            case VIDEO:
                return "VIDEO"; // NoI18N
            case ALL:
                break;
        }

        return null;
    }
}
