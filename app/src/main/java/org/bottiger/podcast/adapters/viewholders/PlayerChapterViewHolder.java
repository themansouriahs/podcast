package org.bottiger.podcast.adapters.viewholders;

import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.Spanned;
import android.view.View;
import android.widget.TextView;

import org.bottiger.podcast.R;
import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.player.GenericMediaPlayerInterface;
import org.bottiger.podcast.utils.StrUtils;
import org.bottiger.podcast.utils.chapter.Chapter;

/**
 * Created by aplb on 24-11-2016.
 */

public class PlayerChapterViewHolder  extends RecyclerView.ViewHolder implements View.OnClickListener {

    @NonNull private View mItemView;
    @NonNull private TextView mTextView;

    @Nullable private Chapter mChapter;

    public PlayerChapterViewHolder(@NonNull View itemView) {
        super(itemView);
        itemView.setOnClickListener(this);

        mItemView = itemView;
        mTextView = (TextView) itemView.findViewById(R.id.chapter_title);
    }

    public void setChapter(@Nullable Chapter argChapter) {
        mChapter = argChapter;

        if (argChapter != null) {
            mTextView.setText(getText());
        }
    }

    private Spanned getText() {
        String input = "";

        if (mChapter != null) {
            String startStr = StrUtils.formatTime(mChapter.getStart());
            input = "<u>" + startStr + "</u> " + mChapter.getTitle();
        }

        Spanned text;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            text = Html.fromHtml(input, Html.FROM_HTML_MODE_LEGACY);
        } else {
            text = Html.fromHtml(input);
        }

        return text;
    }

    @NonNull
    public View getView() {
        return mItemView;
    }

    @Override
    public void onClick(View view) {
        Chapter chapter = mChapter;

        if (mChapter == null)
            return;

        GenericMediaPlayerInterface player = SoundWaves.getAppContext(view.getContext()).getPlayer();
        player.seekTo(chapter.getStart());
    }
}
