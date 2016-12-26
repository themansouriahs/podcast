package org.bottiger.podcast.webservices.directories.audiosearch;

import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.text.TextUtils;
import android.util.Base64;

import org.bottiger.podcast.R;
import org.bottiger.podcast.provider.SlimImplementations.SlimSubscription;
import org.bottiger.podcast.utils.ErrorUtils;
import org.bottiger.podcast.utils.HttpUtils;
import org.bottiger.podcast.utils.StrUtils;
import org.bottiger.podcast.utils.rxbus.RxBasicSubscriber;
import org.bottiger.podcast.webservices.directories.ISearchParameters;
import org.bottiger.podcast.webservices.directories.ISearchResult;
import org.bottiger.podcast.webservices.directories.audiosearch.authorization.AuthResult;
import org.bottiger.podcast.webservices.directories.audiosearch.authorization.AuthorizationService;
import org.bottiger.podcast.webservices.directories.audiosearch.types.Chart;
import org.bottiger.podcast.webservices.directories.audiosearch.types.ChartItem;
import org.bottiger.podcast.webservices.directories.audiosearch.types.Show;
import org.bottiger.podcast.webservices.directories.generic.GenericDirectory;
import org.bottiger.podcast.webservices.directories.generic.GenericSearchResult;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static org.bottiger.podcast.ApplicationConfiguration.AUDIOSEARCH_APP_ID;
import static org.bottiger.podcast.ApplicationConfiguration.AUDIOSEARCH_SECRET;

/**
 * Created by aplb on 12-12-2016.
 */

public class AudioSearch extends GenericDirectory {
    private static final String TAG = AudioSearch.class.getSimpleName();

    /** Which action to perform when a track ends */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({AUDIO, VIDEO, ALL})
    @interface Type {}
    static final int AUDIO = 1;
    static final int VIDEO = 2;
    static final int ALL = 3;

    private static final String NAME = "Audiosear.ch";
    private static final String BASE_URL = "https://www.audiosear.ch/";

    private final String GRANT_TYPE = "client_credentials";
    private String AUTH_SIGNATURE;
    @Nullable private String AUTH_TOKEN = null;
    private String AccessToken;

    private final AuthorizationService mAuthorizationService;
    private Retrofit authInstance;
    private AudioSearchEndpoint mService;

    public AudioSearch(@NonNull Context argContext) {
        super(NAME, argContext);

        AUTH_SIGNATURE = getSignature();
        // Initialize AuthorizationClient with Authorization interceptor
        OkHttpClient authClient = new OkHttpClient();
        // Initialize Retrofit for Authentication
        authInstance = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(authClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        mAuthorizationService = authInstance.create(AuthorizationService.class);

        Retrofit mRetrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(getOkHttpClient())
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        mService = mRetrofit.create(AudioSearchEndpoint.class);
    }

    public static @StringRes int getNameRes() {
        return R.string.webservices_discovery_engine_audiosearch;
    }

    @Override
    public boolean isEnabled() {
        return !(TextUtils.isEmpty(AUDIOSEARCH_APP_ID) || TextUtils.isEmpty(AUDIOSEARCH_SECRET));
    }

    @Override
    protected AsyncTask<String, Void, ISearchResult> getAsyncTask() {
        return null;
    }

    @Override
    public void search(@NonNull ISearchParameters argParameters, @NonNull final Callback argCallback) {
        final String searchString = TextUtils.join(" ", argParameters.getKeywords());
        SearchQuery query = getQuery(searchString);
        mService.search(query.getQuery(),
                        query.getSortBy(),
                        query.getSortOrder(),
                        query.getSize(),
                        query.getFrom(),
                        query.getPage()
                )
                .enqueue(new retrofit2.Callback<SearchResult>() {
                    @Override
                    public void onResponse(Call<SearchResult> call, Response<SearchResult> response) {

                        if (!response.isSuccessful()) {
                            AUTH_TOKEN = null;
                            return;
                        }

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
    public void toplist(int amount, @Nullable String argTag, @NonNull final Callback argCallback) {
        String localLanguage = "us"; //Locale.getDefault().getCountry();

        mService.chart_daily(amount, localLanguage).enqueue(new retrofit2.Callback<Chart>() {
            @Override
            public void onResponse(Call<Chart> call, final Response<Chart> response) {
                Flowable.just(response)
                        .subscribeOn(Schedulers.io())
                        .map(new Function<Response<Chart>, ISearchResult>() {
                            @Override
                            public ISearchResult apply(Response<Chart> chartResponse) throws Exception {
                                GenericSearchResult resultReturn = new GenericSearchResult("");

                                Chart chart = response.body();

                                if (chart == null) {
                                    return resultReturn;
                                }

                                Map<String, ChartItem> shows = chart.getShows();

                                if (shows == null) {
                                    return resultReturn;
                                }

                                for (ChartItem entry : shows.values())
                                {
                                    long id = entry.getAudioSearchId();
                                    Response<Show> showResponse = mService.show(id).execute();

                                    if (showResponse.isSuccessful()) {
                                        Show show = showResponse.body();

                                        SlimSubscription subscription = show.toSubscription();
                                        if (subscription != null) {
                                            resultReturn.addResult(subscription);
                                        }
                                    }
                                }

                                return resultReturn;
                            }
                        })
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new RxBasicSubscriber<ISearchResult>() {
                            @Override
                            public void onNext(ISearchResult iSearchResult) {
                                argCallback.result(iSearchResult);
                            }
                });
            }

            @Override
            public void onFailure(Call<Chart> call, Throwable t) {
                ErrorUtils.handleException(t);
            }
        });
    }

    @NonNull
    private SearchQuery getQuery(@Nullable String argQuery) {
        String query = argQuery != null ? argQuery : "";
        return new SearchQuery(query);
    }

    private String Authorize() throws IOException{
        AuthResult authResult = mAuthorizationService.getAccessToken(GRANT_TYPE, AUTH_SIGNATURE).execute().body();
        AccessToken = authResult.accessToken;
        AUTH_TOKEN  = "Bearer" + " " + this.AccessToken;
        return AUTH_TOKEN;
    }

    private static String getSignature() {
        String unencoded = AUDIOSEARCH_APP_ID +":"+ AUDIOSEARCH_SECRET;
        return "Basic " + StrUtils.toBase64(unencoded).replaceAll("(\\r|\\n)", ""); // Base64Encode.encode(unencoded).replaceAll("(\\r|\\n)", "");
    }

    @Override
    protected OkHttpClient createOkHttpClient() {
        return HttpUtils
                .getNewDefaultOkHttpClientBuilder(getContext())
                .addInterceptor(new Interceptor() {
            @Override
            public okhttp3.Response intercept(Chain chain) throws IOException {
                Request original = chain.request();

                if (AUTH_TOKEN == null) {
                    Authorize();
                }

                Request.Builder requestBuilder = original.newBuilder()
                        //.header("Accept", "application/json")
                        .header("Authorization", AUTH_TOKEN)
                        .method(original.method(), original.body());

                Request request = requestBuilder.build();

                return chain.proceed(request);
            }
        }).build();
    }
}
