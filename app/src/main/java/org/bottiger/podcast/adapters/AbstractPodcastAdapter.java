package org.bottiger.podcast.adapters;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.otto.Produce;

import org.bottiger.podcast.playlist.Playlist;
import org.bottiger.podcast.service.PlayerService;
import org.bottiger.podcast.views.PlaylistViewHolder;

public abstract class AbstractPodcastAdapter<PlaylistViewHolder extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<PlaylistViewHolder> {

    protected Playlist mPlaylist;

    protected LayoutInflater mInflater;
    protected Activity mActivity;

    public AbstractPodcastAdapter(@NonNull Activity argActivity) {
        super();
        mActivity = argActivity;
	}
}
