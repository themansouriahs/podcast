package org.bottiger.podcast.listeners;

import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import org.bottiger.podcast.ApplicationConfiguration;

import java.util.WeakHashMap;

/**
 * Created by apl on 23-01-2015.
 */
public class PlaylistScrollListener extends RecyclerView.OnScrollListener {

    private WeakHashMap<PostScrollListener, Boolean> mPostScrollListeners = new WeakHashMap<>();

    public interface PostScrollListener {
        public void hasScrolledY(int dy);
    }

    public PlaylistScrollListener(@Nullable PostScrollListener argPostScrollListener) {
        if (argPostScrollListener != null) {
            mPostScrollListeners.put(argPostScrollListener, true);
        }
    }

    public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
        super.onScrollStateChanged(recyclerView, newState);
    }

    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
        super.onScrolled(recyclerView, dx, dy);

        if (ApplicationConfiguration.DEBUGGING)
            Log.d("PlaylistScrollListener", "Scrolled: " + dx + " " + dy);

        for(PostScrollListener listener : mPostScrollListeners.keySet()) {
            listener.hasScrolledY(dy);
        }
    }
}
