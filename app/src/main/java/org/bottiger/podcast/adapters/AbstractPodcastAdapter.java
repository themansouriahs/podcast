package org.bottiger.podcast.adapters;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;

import org.bottiger.podcast.playlist.Playlist;

public abstract class AbstractPodcastAdapter<PlaylistViewHolder extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<PlaylistViewHolder> {

    protected Playlist mPlaylist;

    protected LayoutInflater mInflater;
    protected Activity mActivity;

    public AbstractPodcastAdapter(@NonNull Activity argActivity) {
        super();
        mActivity = argActivity;
	}
}
