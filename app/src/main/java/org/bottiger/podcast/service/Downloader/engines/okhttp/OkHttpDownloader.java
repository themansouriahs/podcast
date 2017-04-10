package org.bottiger.podcast.service.Downloader.engines.okhttp;

import android.Manifest;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresPermission;
import android.support.annotation.WorkerThread;
import android.util.Log;
import android.util.SparseArray;
import android.webkit.MimeTypeMap;

import org.bottiger.podcast.flavors.CrashReporter.VendorCrashReporter;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.service.Downloader.engines.DownloadEngineBase;
import org.bottiger.podcast.service.Downloader.engines.ProgressListener;
import org.bottiger.podcast.utils.ErrorUtils;
import org.bottiger.podcast.utils.HttpUtils;
import org.bottiger.podcast.utils.StrUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.MalformedURLException;
import java.net.URL;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.ForwardingSource;
import okio.Okio;
import okio.Source;

/**
 * Created by apl on 17-09-2014.
 */
public class OkHttpDownloader extends DownloadEngineBase {

    private static final String TAG = OkHttpDownloader.class.getSimpleName();

    private static final int BUFFER_SIZE = 2048;

    private final OkHttpClient mOkHttpClient;
    private final SparseArray<Callback> mExternalCallback = new SparseArray<>();

    @Nullable
    private volatile BufferedSink sink;

    @Nullable private final URL mURL;

    @WorkerThread
    public OkHttpDownloader(@NonNull Context argContext, @NonNull IEpisode argEpisode) {
        super(argContext, argEpisode);
        mURL = argEpisode.getUrl();

        final ProgressListener progressListener = new ProgressListener() {
            @Override public void update(long bytesRead, long contentLength, boolean done, long startTime) {
                long nowTime = System.currentTimeMillis();
                long timeS = (nowTime-startTime)/1000;
                if (timeS > 0) {
                    setSpeed(bytesRead / timeS);
                }

                if (bytesRead != contentLength) {
                    float progress = (100 * bytesRead) / contentLength;
                    setProgress(progress);
                }
            }
        };

        mOkHttpClient = new OkHttpClient.Builder()
                .addNetworkInterceptor(new Interceptor() {
                    @Override public Response intercept(Interceptor.Chain chain) throws IOException {
                        Response originalResponse = chain.proceed(chain.request());
                        return originalResponse.newBuilder()
                                .body(new ProgressResponseBody(originalResponse.body(), progressListener))
                                .build();
                    }
                })
                .build();
    }

    @WorkerThread
    @Override
    @RequiresPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    public void startDownload(boolean argIsLast) throws SecurityException {
        setProgress(0);

        if (!StrUtils.isValidUrl(mURL)) {
            Log.d(TAG, "no URL, return");
            onFailure(new MalformedURLException());
            return;
        }

        boolean isFeedItem = mEpisode instanceof FeedItem;

        FeedItem feedItem = isFeedItem ? (FeedItem)mEpisode : null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(mURL.toString());
        Log.d(TAG, "File extension: " + extension);

        String filename;
        if (isFeedItem) {
            filename = Integer.toString(feedItem.getEpisodeNumber()) + feedItem.getTitle().replace(' ', '_'); //Integer.toString(item.getEpisodeNumber()) + "_"
            feedItem.setFilename(filename + "." + extension);
        }

        File tmpFile;
        File finalFile;
        try {
            tmpFile = new File(mEpisode.getAbsoluteTmpPath(getContext()));
            finalFile = new File(mEpisode.getAbsolutePath(getContext()));
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        try {
            Log.d(TAG, "startDownload");

            Request request = new Request.Builder()
                    .url(mURL.toString())
                    .header("User-Agent", HttpUtils.getUserAgent(getContext()))
                    .build();

            Response response = mOkHttpClient.newCall(request).execute();

            ResponseBody body = response.body();
            long contentLength = body.contentLength();
            mEpisode.setFilesize(contentLength);

            Log.d(TAG, "starting file transfer");


            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            } else {
                Log.d(TAG, "filetransfer done");
            }

            sink = Okio.buffer(Okio.sink(tmpFile));

            try {
                sink.writeAll(response.body().source());
                sink.close();
                sink = null;
            } catch (NullPointerException npe) {
                String msg = "unable to aquire downl";
                Log.d(TAG, msg);
                onFailure(new FileNotFoundException(msg));
                return;
            }

            // If download was succesfull
            boolean movedFileSuccesfully = false;
            if (tmpFile.exists() && tmpFile.length() == contentLength) {
                Log.d(TAG, "Renaming file");
                movedFileSuccesfully = tmpFile.renameTo(finalFile);
                Log.d(TAG, "File renamed");

                if (movedFileSuccesfully) {
                    onSucces(finalFile);
                    Log.d(TAG, "post onSucces");
                }
            }

            if (!movedFileSuccesfully) {
                Log.d(TAG, "File already exists");
                String msg = "Wrong file size. Expected: " + tmpFile.length() + ", got: " + contentLength; // NoI18N
                onFailure(new FileNotFoundException(msg));
                Log.d(TAG, "post onfailure");
            }
        } catch (IOException e){
            Log.d(TAG, "IOException: " + e.toString());
            onFailure(e);
            String[] keys = {"DownloadUrl"};
            String[] values = {mURL.toString()};
            VendorCrashReporter.handleException(e, keys, values);
        } finally{
            if (argIsLast) {
                removeNotification();
            }
        }
    }

    private void closeConnection(@Nullable BufferedSource source, @Nullable BufferedSink sink) {
        Log.d(TAG, "disconnecting");
        try {
            if (sink != null) {
                sink.flush();
                sink.close();
            }
            if (source != null) {
                source.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void addCallback(@NonNull Callback argCallback) {
        int newKey = mExternalCallback.size() == 0 ? 0 : mExternalCallback.keyAt(mExternalCallback.size()-1) + 1;
        mExternalCallback.append(newKey, argCallback);
    }

    @Override
    public void abort() {
        try {
            BufferedSink sinkHolder = sink;
            if (sinkHolder != null) {
                sinkHolder.close();

                Log.d(TAG, "Transfer abort");
                onFailure(new InterruptedIOException());
            }
        } catch (IOException e) {
            ErrorUtils.handleException(e);
        }
    }

    private void onSucces(File response)  throws IOException {
        Log.d("Download", "Download succeeded");

        for(int i = 0; i < mExternalCallback.size(); i++) {
            int key = mExternalCallback.keyAt(i);
            Callback callback = mExternalCallback.valueAt(key);
            callback.downloadCompleted(mEpisode);
        }
    }

    public void onFailure(Exception e) {
        Log.w(TAG, "Download Failed: " + e.getMessage());

        for(int i = 0; i < mExternalCallback.size(); i++) {
            int key = mExternalCallback.keyAt(i);
            Callback callback = mExternalCallback.valueAt(key);
            callback.downloadInterrupted(mEpisode);
        }
    }


    private static class ProgressResponseBody extends ResponseBody {

        private final ResponseBody responseBody;
        private final ProgressListener progressListener;
        private BufferedSource bufferedSource;

        private long startTime;

        public ProgressResponseBody(ResponseBody responseBody, ProgressListener progressListener) {
            this.responseBody = responseBody;
            this.progressListener = progressListener;
            this.startTime = System.currentTimeMillis();
        }

        @Override public MediaType contentType() {
            return responseBody.contentType();
        }

        @Override public long contentLength() {
            return responseBody.contentLength();
        }

        @Override public BufferedSource source() {
            if (bufferedSource == null) {
                bufferedSource = Okio.buffer(source(responseBody.source()));
            }
            return bufferedSource;
        }

        private Source source(Source source) {
            return new ForwardingSource(source) {
                long totalBytesRead = 0L;

                @Override
                public long read(Buffer sink, long byteCount) throws IOException {
                    long bytesRead = super.read(sink, byteCount);
                    // read() returns the number of bytes read, or -1 if this source is exhausted.
                    totalBytesRead += bytesRead != -1 ? bytesRead : 0;
                    progressListener.update(totalBytesRead, responseBody.contentLength(), bytesRead == -1, startTime);
                    return bytesRead;
                }

                @Override
                public void close() throws IOException {
                    progressListener.update(responseBody.contentLength(), responseBody.contentLength(), true, startTime);
                }
            };
        }
    }
}
