package org.bottiger.podcast.adapters.viewholders;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.graphics.Color;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.RecyclerView;
import android.transition.TransitionManager;
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

    private static TransitionManager sTransitionManager = null;

    private static RelativeLayout.LayoutParams sPlayPauseParams;
    private static RelativeLayout.LayoutParams sTitleParams;
    private static RelativeLayout.LayoutParams sPodcastExpandedLayoutParams;
    private static RelativeLayout.LayoutParams sDurationParams;
    private static ViewGroup.LayoutParams sImageParams;


    public static void openH(final PlaylistViewHolder holder, final View expandView, final boolean animate) {
        if (animate) {
            initTransition(holder);
        }
            if (sPodcastExpandedLayoutParams == null) {
                sPodcastExpandedLayoutParams = (RelativeLayout.LayoutParams) holder.mExpandedLayoutControls.getLayoutParams();
                sPlayPauseParams = (RelativeLayout.LayoutParams) holder.mPlayPauseButton.getLayoutParams();
                sTitleParams = (RelativeLayout.LayoutParams) holder.mMainTitle.getLayoutParams();
                sDurationParams = (RelativeLayout.LayoutParams) holder.mTimeDuration.getLayoutParams();
                sImageParams = (ViewGroup.LayoutParams) holder.mPodcastImage.getLayoutParams();
            }

            int roundingRadius = (int) holder.mPodcastImage.getContext().getResources().getDimension(R.dimen.playlist_image_radius_large);
            int imageSize = (int) holder.mPodcastImage.getContext().getResources().getDimension(R.dimen.playlist_image_size_large);

            sImageParams.width = imageSize;
            sImageParams.height = imageSize;
            holder.mPodcastImage.setLayoutParams(sImageParams);

            /*
                GenericDraweeHierarchy hierarchy = holder.mPodcastImage.getHierarchy();
                RoundingParams roundingParams = hierarchy.getRoundingParams();
                roundingParams.setCornersRadius(roundingRadius);
                holder.mPodcastImage.getHierarchy().setRoundingParams(roundingParams);
            */

            holder.mMainTitle.setLayoutParams(sTitleParams);
            holder.mMainTitle.setSingleLine(false);
            holder.mMainTitle.setTextColor(Color.BLACK);

            sPlayPauseParams.setMargins(sPlayPauseParams.leftMargin, 100, sPlayPauseParams.rightMargin, sPlayPauseParams.bottomMargin);
            sPlayPauseParams.addRule(RelativeLayout.CENTER_VERTICAL, 0);

            holder.mExpandedLayoutBottom.setVisibility(View.VISIBLE);
            expandView.setVisibility(View.VISIBLE);

            holder.buttonLayout.setVisibility(View.VISIBLE);

            PaletteHelper.generate(holder.getArtwork(), holder.getActivity(), new PaletteListener() {
                @Override
                public void onPaletteFound(Palette argChangedPalette) {
                    int white = holder.getActivity().getResources().getColor(R.color.white_opaque);

                    ColorExtractor colorExtractor = new ColorExtractor(holder.getActivity(), argChangedPalette);
                    //holder.mLayout.setCardBackgroundColor(colorExtractor.getPrimary());
                    //holder.mMainTitle.setTextColor(colorExtractor.getTextColor());
                    ////viewHolder.buttonLayout.setBackgroundColor(colorExtractor.getPrimary());
                    //holder.description.setTextColor(colorExtractor.getTextColor());
                    //holder.currentTime.setTextColor(colorExtractor.getTextColor());
                    //holder.mTimeDuration.setTextColor(colorExtractor.getTextColor());
                }

                @Override
                public String getPaletteUrl() {
                    return null;
                }
            });

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

        /*
        GenericDraweeHierarchy hierarchy = holder.mPodcastImage.getHierarchy();
        RoundingParams roundingParams = hierarchy.getRoundingParams();
        //roundingParams.setRoundAsCircle(true);
        holder.mPodcastImage.getHierarchy().setRoundingParams(roundingParams);
        */

        holder.mExpandedLayoutBottom.setVisibility(View.GONE);
        holder.buttonLayout.setVisibility(View.GONE);
        expandView.setVisibility(View.GONE);

        PaletteHelper.generate(holder.getArtwork(), holder.getActivity(), new PaletteListener() {
            @Override
            public void onPaletteFound(Palette argChangedPalette) {
                int white = ColorUtils.getBackgroundColor(holder.getActivity()); //holder.getActivity().getResources().getColor(R.color.white_opaque);
                int black = ColorUtils.getTextColor(holder.getActivity()); //holder.getActivity().getResources().getColor(R.color.black);

                ColorExtractor colorExtractor = new ColorExtractor(holder.getActivity(), argChangedPalette);
                holder.mLayout.setCardBackgroundColor(white);
                holder.mMainTitle.setTextColor(black);
                ////viewHolder.buttonLayout.setBackgroundColor(colorExtractor.getPrimary());
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


    public static interface Expandable {
        public View getExpandView();
    }

    public static class KeepOneH<VH extends PlaylistViewHolder & Expandable> {
        public int _opened = -1; //private

        public void bind(VH holder, int pos) {
            if (pos == _opened)
                ExpandableViewHoldersUtil.openH(holder, holder.getExpandView(), false);
            else
                ExpandableViewHoldersUtil.closeH(holder, holder.getExpandView(), false);
        }

        @SuppressWarnings("unchecked")
        public void toggle(VH holder) {
            if (_opened == holder.getPosition()) {
                _opened = -1;
                ExpandableViewHoldersUtil.closeH(holder, holder.getExpandView(), true);
            }
            else {
                int previous = _opened;
                _opened = holder.getPosition();
                ExpandableViewHoldersUtil.openH(holder, holder.getExpandView(), true);

                final VH oldHolder = (VH) ((RecyclerView) holder.itemView.getParent()).findViewHolderForPosition(previous);
                if (oldHolder != null)
                    ExpandableViewHoldersUtil.closeH(oldHolder, oldHolder.getExpandView(), true);
            }
        }
    }

    @TargetApi(19)
    private static void initTransition(PlaylistViewHolder argPlaylistViewHolder) {
        if (android.os.Build.VERSION.SDK_INT < 19) {
            return;
        }

        if (sTransitionManager == null) {
            sTransitionManager = new TransitionManager();
        }

        ViewGroup viewGroup = (ViewGroup) argPlaylistViewHolder.mLayout.getParent();
        if (viewGroup == null)
            return;

        sTransitionManager.beginDelayedTransition(viewGroup, UIUtils.getDefaultTransition(argPlaylistViewHolder.mLayout.getResources()));
    }

}
