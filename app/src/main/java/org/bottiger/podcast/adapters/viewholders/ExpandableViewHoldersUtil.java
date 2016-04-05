package org.bottiger.podcast.adapters.viewholders;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.graphics.Color;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.RecyclerView;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import org.bottiger.podcast.R;
import org.bottiger.podcast.listeners.PaletteListener;
import org.bottiger.podcast.utils.ColorExtractor;
import org.bottiger.podcast.utils.ColorUtils;
import org.bottiger.podcast.utils.PaletteHelper;
import org.bottiger.podcast.utils.UIUtils;
import org.bottiger.podcast.views.PlaylistViewHolder;

/**
 * Created by apl on 20-01-2015.
 */
public class ExpandableViewHoldersUtil {

    private static RelativeLayout.LayoutParams sPlayPauseParams;
    private static RelativeLayout.LayoutParams sTitleParams;
    private static RelativeLayout.LayoutParams sPodcastExpandedLayoutParams;
    private static ViewGroup.LayoutParams sImageParams;


    public static void openH(final PlaylistViewHolder holder, final View expandView, final boolean animate) {
        if (animate) {
            initTransition(holder);
        }
            if (sPodcastExpandedLayoutParams == null) {
                sPodcastExpandedLayoutParams = (RelativeLayout.LayoutParams) holder.mExpandedLayoutControls.getLayoutParams();
                sPlayPauseParams = (RelativeLayout.LayoutParams) holder.mPlayPauseButton.getLayoutParams();
                sTitleParams = (RelativeLayout.LayoutParams) holder.mMainTitle.getLayoutParams();
                sImageParams = holder.mPodcastImage.getLayoutParams();
            }

            int imageSize = (int) holder.mPodcastImage.getContext().getResources().getDimension(R.dimen.playlist_image_size_large);

            sImageParams.width = imageSize;
            sImageParams.height = imageSize;
            holder.mPodcastImage.setLayoutParams(sImageParams);

            holder.downloadButton.enabledProgressListener(true);

            holder.mMainTitle.setLayoutParams(sTitleParams);
            holder.mMainTitle.setSingleLine(false);
            holder.mMainTitle.setTextColor(Color.BLACK);

            sPlayPauseParams.setMargins(sPlayPauseParams.leftMargin, 100, sPlayPauseParams.rightMargin, sPlayPauseParams.bottomMargin);
            sPlayPauseParams.addRule(RelativeLayout.CENTER_VERTICAL, 0);

            holder.mExpandedLayoutBottom.setVisibility(View.VISIBLE);
            expandView.setVisibility(View.VISIBLE);

            holder.buttonLayout.setVisibility(View.VISIBLE);
    }

    @SuppressLint("NewApi")
    public static void closeH(final PlaylistViewHolder holder, final View expandView, final boolean animate) {

        if (animate) {
            initTransition(holder);
        }

        if (sPlayPauseParams != null) {
            sPlayPauseParams.setMargins(sPlayPauseParams.leftMargin, 0, sPlayPauseParams.rightMargin, sPlayPauseParams.bottomMargin);
            sPlayPauseParams.addRule(RelativeLayout.CENTER_VERTICAL);
            holder.mPlayPauseButton.setLayoutParams(sPlayPauseParams);
        }


        if (sImageParams != null) {
            int sizePixels = (int) holder.mPodcastImage.getContext().getResources().getDimension(R.dimen.playlist_image_size_small);
            sImageParams.width = sizePixels;
            sImageParams.height = sizePixels;
            holder.mPodcastImage.setLayoutParams(sImageParams);
        }
        holder.downloadButton.enabledProgressListener(false);

        holder.mExpandedLayoutBottom.setVisibility(View.GONE);
        holder.buttonLayout.setVisibility(View.GONE);
        expandView.setVisibility(View.GONE);

        PaletteHelper.generate(holder.getArtwork(), holder.getActivity(), new PaletteListener() {
            @Override
            public void onPaletteFound(Palette argChangedPalette) {
                int white = ColorUtils.getBackgroundColor(holder.getActivity());
                int black = ColorUtils.getTextColor(holder.getActivity());

                //holder.mLayout.setCardBackgroundColor(white);
                holder.mMainTitle.setTextColor(black);
                holder.description.setTextColor(black);
                holder.currentTime.setTextColor(black);
                holder.mTimeDuration.setTextColor(black);
            }

            @Override
            public String getPaletteUrl() {
                return null;
            }
        });

    }

    public interface Expandable {
        View getExpandView();
    }

    public static class KeepOneH<VH extends PlaylistViewHolder> {
        public int _opened = -1; //private

        public void bind(VH holder, int pos) {
            if (pos == _opened)
                ExpandableViewHoldersUtil.openH(holder, holder.getExpandView(), false);
            else
                ExpandableViewHoldersUtil.closeH(holder, holder.getExpandView(), false);
        }

        @SuppressWarnings("unchecked")
        public void toggle(VH holder) {
            if (_opened == holder.getAdapterPosition()) {
                _opened = -1;
                ExpandableViewHoldersUtil.closeH(holder, holder.getExpandView(), true);
            }
            else {
                int previous = _opened;
                _opened = holder.getAdapterPosition();
                ExpandableViewHoldersUtil.openH(holder, holder.getExpandView(), true);

                final VH oldHolder = (VH) ((RecyclerView) holder.itemView.getParent()).findViewHolderForPosition(previous);
                if (oldHolder != null)
                    ExpandableViewHoldersUtil.closeH(oldHolder, oldHolder.getExpandView(), true);
            }
        }
    }

    @TargetApi(19)
    private static void initTransition(PlaylistViewHolder argPlaylistViewHolder) {
        Log.v("TEst2", "initiTransition");
        if (android.os.Build.VERSION.SDK_INT < 19) {
            return;
        }

        ViewGroup viewGroup = (ViewGroup) argPlaylistViewHolder.mLayout.getParent();
        if (viewGroup == null)
            return;

        TransitionManager.beginDelayedTransition(viewGroup, UIUtils.getDefaultTransition(argPlaylistViewHolder.mLayout.getResources()));
    }

}
