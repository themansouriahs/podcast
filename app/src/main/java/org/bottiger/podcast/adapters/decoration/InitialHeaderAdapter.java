package org.bottiger.podcast.adapters.decoration;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.eowise.recyclerview.stickyheaders.StickyHeadersAdapter;

import org.bottiger.podcast.ApplicationConfiguration;
import org.bottiger.podcast.R;
import org.bottiger.podcast.adapters.ItemCursorAdapter;
import org.bottiger.podcast.playlist.Playlist;
import org.bottiger.podcast.provider.FeedItem;

import java.util.List;

/**
 * Created by apl on 19-01-2015.
 */
public class InitialHeaderAdapter implements StickyHeadersAdapter<InitialHeaderAdapter.ViewHolder>, DragSortRecycler.OnDragStateChangedListener {

    private Playlist mPlaylist;
    private int mCurrentlyDragging = -1;

    public InitialHeaderAdapter(Playlist argPlaylist) {
        this.mPlaylist = argPlaylist;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.episode_list_header, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(ViewHolder headerViewHolder, int position) {

        FeedItem item = mPlaylist.getItem(position+ ItemCursorAdapter.PLAYLIST_OFFSET);

        headerViewHolder.letter.setVisibility(View.VISIBLE);
        if (position == mCurrentlyDragging) {
            headerViewHolder.letter.setVisibility(View.INVISIBLE);
        }

        if (item.title != null) {
            String title = item.title;
            int priority = item.getPriority();
            long lastUpdate = item.getLastUpdate();

            String preTitle = "";

            if (item.isListened() && ApplicationConfiguration.DEBUGGING) {
                preTitle = "L";
            }


            if (ApplicationConfiguration.DEBUGGING)
                preTitle = preTitle+ "p:" + priority + " t:" + lastUpdate;
            else
                preTitle = preTitle + String.valueOf(priority);

            if (priority > 0 || ApplicationConfiguration.DEBUGGING) {
                title = preTitle + " # " + title;
            }

            headerViewHolder.letter.setText(title);
            //playlistViewHolder2.mMainTitle.setText(title);
        }
    }

    @Override
    public long getHeaderId(int position) {
        return position; //items.get(position).charAt(0);
    }

    @Override
    public void onDragStart(int position) {
        mCurrentlyDragging = position;
    }

    @Override
    public void onDragStop(int position) {
        mCurrentlyDragging = -1;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView letter;
        public ViewHolder(View itemView) {
            super(itemView);
            letter = (TextView) itemView.findViewById(R.id.title);
        }
    }
}