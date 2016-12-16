package org.bottiger.podcast.service.Downloader.engines;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.net.ConnectivityManager;
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
import org.bottiger.podcast.utils.HttpUtils;
import org.bottiger.podcast.utils.StrUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;

/**
 * Created by apl on 17-09-2014.
 */
public class OkHttpDownloader extends DownloadEngineBase {

    private static final String TAG = OkHttpDownloader.class.getSimpleName();

    private static final int BUFFER_SIZE = 2048;

    private final OkHttpClient mOkHttpClient = new OkHttpClient();;
    private final SparseArray<Callback> mExternalCallback = new SparseArray<>();

    private volatile boolean mAborted = false;

    @NonNull private Context mContext;
    @Nullable private final URL mURL;

    private double mProgress = 0;

    @WorkerThread
    public OkHttpDownloader(@NonNull Context argContext, @NonNull IEpisode argEpisode) {
        super(argEpisode);
        mContext = argContext;
        mURL = argEpisode.getUrl();
    }

    @WorkerThread
    @Override
    @RequiresPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    public void startDownload() throws SecurityException {
        mProgress = 0;

        if (!StrUtils.isValidUrl(mURL)) {
            Log.d(TAG, "no URL, return");
            onFailure(new MalformedURLException());
            return;
        }

        if (!(mEpisode instanceof FeedItem)) {
            Log.d(TAG, "Trying to download slim episode - abort");
            onFailure(new IllegalArgumentException("Argument must be of type FeedItem"));
            return;
        }

        FeedItem episode = (FeedItem) mEpisode;
        String extension = MimeTypeMap.getFileExtensionFromUrl(mURL.toString());
        Log.d(TAG, "File extension: " + extension);

        String filename = Integer.toString(episode.getEpisodeNumber()) + episode.getTitle().replace(' ', '_'); //Integer.toString(item.getEpisodeNumber()) + "_"
        episode.setFilename(filename + "." + extension);

        File tmpFile;
        File finalFile;
        try {
            tmpFile = new File(episode.getAbsoluteTmpPath(mContext));
            finalFile = new File(episode.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        BufferedSource source = null;
        BufferedSink sink = null;

        try {
            Log.d(TAG, "startDownload");

            Request request = new Request.Builder()
                    .url(mURL.toString())
                    .header("User-Agent", HttpUtils.getUserAgent(mContext))
                    .build();

            Response response = mOkHttpClient.newCall(request).execute();
            ResponseBody body = response.body();
            long contentLength = body.contentLength();
            source = body.source();

            mEpisode.setFilesize(contentLength);

            sink = Okio.buffer(Okio.sink(tmpFile));
            Buffer sinkBuffer = sink.buffer();

            long totalBytesRead = 0;
            int bufferSize = BUFFER_SIZE;

            Log.d(TAG, "starting file transfer");

            for (long bytesRead; (bytesRead = source.read(sinkBuffer, bufferSize)) != -1; ) {

                if (mAborted) {
                    Log.d(TAG, "Transfer abort");
                    closeConnection(source, sink);
                    onFailure(new InterruptedIOException());
                    return;
                }

                sink.emit();
                totalBytesRead += bytesRead;
                mProgress = ((totalBytesRead * 100) / contentLength);
                getEpisode().setProgress(mProgress);
            }

            Log.d(TAG, "filetransfer done");

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
            closeConnection(source, sink);
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
    public float getProgress() {
        return (float)mProgress;
    }

    @Override
    public void addCallback(@NonNull Callback argCallback) {
        int newKey = mExternalCallback.size() == 0 ? 0 : mExternalCallback.keyAt(mExternalCallback.size()-1) + 1;
        mExternalCallback.append(newKey, argCallback);
    }

    @Override
    public void abort() {
        mAborted = true;
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
        Log.w("Download", "Download Failed: " + e.getMessage());

        for(int i = 0; i < mExternalCallback.size(); i++) {
            int key = mExternalCallback.keyAt(i);
            Callback callback = mExternalCallback.valueAt(key);
            callback.downloadInterrupted(mEpisode);
        }
    }
}
