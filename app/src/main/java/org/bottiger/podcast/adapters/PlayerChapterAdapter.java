package org.bottiger.podcast.adapters;

import android.graphics.Color;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
    @Nullable private Integer mActive = null;

    public PlayerChapterAdapter(@NonNull List<Chapter> argChapters) {
        super();
        mChapters = argChapters;
    }

    public void setDataset(@NonNull List<Chapter> argChapters) {
        mChapters = argChapters;
        notifyDataSetChanged();
    }

    public void setActive(Integer argIndex) {
        if ((argIndex == null || mActive == null) && argIndex == mActive)
            return;

        if (argIndex != null && argIndex.equals(mActive))
            return;

        mActive = argIndex;
        notifyDataSetChanged();
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

        boolean isActive = mActive != null && mActive.equals(position);
        @ColorInt int backgroundColor = isActive ? Color.GRAY : Color.TRANSPARENT;

        holder.getView().setBackgroundColor(backgroundColor);
    }

    @Override
    public int getItemCount() {
        return mChapters.size();
    }
}
