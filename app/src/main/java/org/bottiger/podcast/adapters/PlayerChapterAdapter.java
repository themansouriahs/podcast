package org.bottiger.podcast.adapters;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.bottiger.podcast.R;
import org.bottiger.podcast.adapters.viewholders.PlayerChapterViewHolder;
import org.bottiger.podcast.utils.chapter.Chapter;

import java.util.List;

/**
 * Created by aplb on 24-11-2016.
 */

public class PlayerChapterAdapter extends RecyclerView.Adapter<PlayerChapterViewHolder> {

    @NonNull private List<Chapter> mChapters;

    public PlayerChapterAdapter(@NonNull List<Chapter> argChapters) {
        super();
        mChapters = argChapters;
    }

    @Override
    public PlayerChapterViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.player_chapter_list_item, parent, false);

        return new PlayerChapterViewHolder(view);
    }

    @Override
    public void onBindViewHolder(PlayerChapterViewHolder holder, int position) {
        holder.setChapter(mChapters.get(position));
    }

    @Override
    public int getItemCount() {
        return mChapters.size();
    }
}
