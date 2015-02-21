package org.bottiger.podcast.adapters;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.bottiger.podcast.playlist.Playlist;
import org.bottiger.podcast.service.PlayerService;
import org.bottiger.podcast.views.PlaylistViewHolder;

public abstract class AbstractPodcastAdapter<PlaylistViewHolder extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<PlaylistViewHolder> implements Playlist.PlaylistChangeListener {

    protected Playlist mPlaylist;

    protected LayoutInflater mInflater;
    protected Activity mActivity;

    public AbstractPodcastAdapter(@NonNull Activity argActivity, @NonNull Playlist argPlaylist) {
        super();
        mActivity = argActivity;
        mPlaylist = argPlaylist;
    }

    public AbstractPodcastAdapter(@NonNull Activity argActivity) {
        super();
        mActivity = argActivity;
        mPlaylist = PlayerService.getPlaylist(this);
	}

    @Override
    public void notifyPlaylistChanged() {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AbstractPodcastAdapter.this.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void notifyPlaylistRangeChanged(int from, int to) {
        final int tmpmin = Math.min(from, to);
        final int min = tmpmin > this.getItemCount() ? this.getItemCount() : tmpmin;

        final int tmpcount = 1 + Math.max(from, to) - min;
        final int count = tmpcount > this.getItemCount() ? this.getItemCount() : tmpcount;
        try {
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    AbstractPodcastAdapter.this.notifyItemRangeChanged(min, count);
                }
            });
        } catch (IndexOutOfBoundsException iob) {
            int count2 = 5;
            int min2 = count;
            return;
        }
    }

}
