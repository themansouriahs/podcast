package org.bottiger.podcast.service.Downloader.engines;

import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.SparseArray;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.OkUrlFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;

import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.service.Downloader.EpisodeDownloadManager;

/**
 * Created by apl on 17-09-2014.
 */
public class OkHttpDownloader extends DownloadEngineBase {

    private static final int BUFFER_SIZE = 4096;

    private final OkHttpClient mOkHttpClient = new OkHttpClient();
    private HttpURLConnection mConnection;
    private final SparseArray<Callback> mExternalCallback = new SparseArray<>();

    private URL mURL;

    private double mProgress = 0;

    public OkHttpDownloader(@NonNull IEpisode argEpisode) {
        super(argEpisode);

        mURL = argEpisode.getUrl();
    }

    @Override
    public void startDownload() {
        mProgress = 0;
        mConnection = new OkUrlFactory(mOkHttpClient).open(mURL);
        EpisodeDownloadManager.mDownloadingItem = mEpisode;

        Thread downloadingThread = new Thread() {
            public void run() {
                try {
                    if (mEpisode instanceof FeedItem) {
                        FeedItem episode = (FeedItem) mEpisode;

                        String filename = Integer.toString(episode.getEpisodeNumber()) + episode.title.replace(' ', '_'); //Integer.toString(item.getEpisodeNumber()) + "_"
                        episode.setFilename(filename + ".mp3"); // .replaceAll("[^a-zA-Z0-9_-]", "") +

                        double contentLength = mConnection.getContentLength();
                        InputStream inputStream = mConnection.getInputStream();
                        FileOutputStream outputStream = new FileOutputStream(episode.getAbsoluteTmpPath());


                        int bytesRead = -1;
                        byte[] buffer = new byte[BUFFER_SIZE];
                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, bytesRead);
                            mProgress += bytesRead / contentLength;
                        }

                        outputStream.close();
                        inputStream.close();

                        File tmpFIle = new File(episode.getAbsoluteTmpPath());
                        File finalFIle = new File(episode.getAbsolutePath());

                        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                        intent.setData(Uri.fromFile(finalFIle));
                        SoundWaves.getAppContext().sendBroadcast(intent);

                        EpisodeDownloadManager.mDownloadingItem = null;

                        // If download was succesfull
                        if (tmpFIle.exists() && tmpFIle.length() == contentLength) {
                            tmpFIle.renameTo(finalFIle);
                            onSucces(finalFIle);
                        } else {
                            onFailure(null);
                        }
                    }

                }catch(IOException e){
                    onFailure(e);
                }finally{
                    mConnection.disconnect();
                }
            }
        };
        downloadingThread.start();
    }

    @Override
    public float getProgress() {
        return (float)mProgress;
    }

    @Override
    public void addCallback(@NonNull Callback argCallback) {
        if (argCallback == null) {
            throw new IllegalArgumentException("Callback may not be null");
        }

        int newKey = mExternalCallback.size() == 0 ? 0 : mExternalCallback.keyAt(mExternalCallback.size()-1) + 1;
        mExternalCallback.append(newKey, argCallback);
    }

    private void onSucces(File response)  throws IOException {
        Log.d("Download", "Download succeeded");

        for(int i = 0; i < mExternalCallback.size(); i++) {
            int key = mExternalCallback.keyAt(i);
            Callback callback = mExternalCallback.valueAt(key);
            callback.downloadCompleted(mEpisode);
        }
    }

    public void onFailure(IOException e) {
        Log.w("Download", "Download Failed: " + e.getMessage());

        for(int i = 0; i < mExternalCallback.size(); i++) {
            int key = mExternalCallback.keyAt(i);
            Callback callback = mExternalCallback.valueAt(key);
            callback.downloadInterrupted(mEpisode);
        }
    }
}
