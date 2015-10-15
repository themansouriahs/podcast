package org.bottiger.podcast.activities.feedview;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import org.bottiger.podcast.R;
import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.flavors.CrashReporter.VendorCrashReporter;
import org.bottiger.podcast.listeners.PlayerStatusObservable;
import org.bottiger.podcast.model.Library;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.provider.ISubscription;
import org.bottiger.podcast.service.PlayerService;
import org.bottiger.podcast.utils.ColorUtils;
import org.bottiger.podcast.utils.PaletteHelper;
import org.bottiger.podcast.utils.SharedAdapterUtils;
import org.bottiger.podcast.views.PlayPauseImageView;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by apl on 02-09-2014.
 */
public class FeedViewAdapter extends RecyclerView.Adapter<EpisodeViewHolder> {

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({RECENT_FIRST, OLDEST_FIRST})
    public @interface Order {}
    public static final int RECENT_FIRST = 0;
    public static final int OLDEST_FIRST = 1;

    protected ISubscription mSubscription;

    protected Activity mActivity;
    protected LayoutInflater mInflater;

    protected Palette mPalette;

    public static boolean mIsExpanded = false;
    protected @Order int mSortOrder = RECENT_FIRST;

    private ArrayList<Integer> mExpanededItems = new ArrayList<>();

    private static android.text.format.Formatter sFormatter = new android.text.format.Formatter();
    private StringBuilder mStringBuilder = new StringBuilder(100);

    public FeedViewAdapter(@NonNull Activity activity, @NonNull ISubscription argSubscription) {
        mActivity = activity;
        mSubscription = argSubscription;

        mInflater = (LayoutInflater) activity
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public void setDataset(@NonNull ISubscription argSubscription) {
        mSubscription = argSubscription;
        notifyDataSetChanged();
    }

    public @Order int getOrder() {
        return mSortOrder;
    }

    public void setOrder(@Order int argSortOrder) {
        mSortOrder = argSortOrder;
        notifyDataSetChanged();
    }

    @Override
    public EpisodeViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View view = mInflater.inflate(R.layout.feed_view_list_episode, viewGroup, false);
        return new EpisodeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(EpisodeViewHolder viewHolder, final int position) {
        int dataPosition = getDatasetPosition(position);
        final IEpisode item = getItemForPosition(dataPosition);
        final EpisodeViewHolder episodeViewHolder = viewHolder;

        SharedAdapterUtils.AddPaddingToLastElement((viewHolder).mContainer, 0, position == getItemCount()-1);

        if (item == null) {
            VendorCrashReporter.report("FeedViewAdapter", "item is null for: " + mSubscription);
            return;
        }

        Context context = SoundWaves.getAppContext();
        episodeViewHolder.mPrimaryColor = ColorUtils.getTextColor(context);
        episodeViewHolder.mFadedColor = ColorUtils.getFadedTextColor(context);

        if (item.getTitle() != null)
            episodeViewHolder.mTitle.setText(item.getTitle());

        if (item.getDescription() != null)
            episodeViewHolder.mDescription.setText(item.getDescription());

        episodeViewHolder.IsMarkedAsListened = item.isMarkedAsListened();
        episodeViewHolder.DisplayDescription = mIsExpanded;

        episodeViewHolder.mTextSecondary.setText(getSecondaryText(item));

        @EpisodeViewHolder.DisplayState int state = EpisodeViewHolder.COLLAPSED;
        if (mExpanededItems.contains(position)) {
            state = EpisodeViewHolder.EXPANDED;
        } else if (item.isMarkedAsListened()) {
            state = EpisodeViewHolder.COLLAPSED_LISTENED;
        } else if (mIsExpanded) {
            state = EpisodeViewHolder.COLLAPSED_WITH_DESCRIPTION;
        }

        episodeViewHolder.setState(state);

        episodeViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                @EpisodeViewHolder.DisplayState int newState = episodeViewHolder.toggleState();
                if (newState == EpisodeViewHolder.EXPANDED) {
                    mExpanededItems.add(position);
                } else {
                    mExpanededItems.remove(Integer.valueOf(position));
                }
            }
        });

        bindButtons(episodeViewHolder, item);


        episodeViewHolder.mDownloadButton.setEpisode(item);
        //mDownloadProgressObservable.registerObserver(episodeViewHolder.mDownloadButton);

        if (mPalette != null) {
            episodeViewHolder.mPlayPauseButton.onPaletteFound(mPalette);
            episodeViewHolder.mQueueButton.onPaletteFound(mPalette);
            episodeViewHolder.mDownloadButton.onPaletteFound(mPalette);
        }

    }

    protected void bindButtons(@NonNull EpisodeViewHolder episodeViewHolder, @NonNull IEpisode argEpisode) {
        FeedItem item = (FeedItem) argEpisode;

        if (mSubscription == null) {
            mSubscription = item.getSubscription(mActivity);
        }

        boolean isPlaying = false;
        @PlayerStatusObservable.PlayerStatus int playerStatus = PlayerStatusObservable.STOPPED;
        PlayerService ps = PlayerService.getInstance();
        if (ps != null && ps.isInitialized()) {
            if (item.getURL().equals(ps.getCurrentItem().getUrl().toString())) {
                if (ps.isPlaying()) {
                    isPlaying = true;
                }
                playerStatus = ps.getPlayer().getStatus();
            }
        }

        episodeViewHolder.mPlayPauseButton.setEpisode(item, PlayPauseImageView.FEEDVIEW);
        episodeViewHolder.mQueueButton.setEpisode(item, PlayPauseImageView.FEEDVIEW);
        //episodeViewHolder.mPlayPauseButton.setStatus(isPlaying ? PlayerStatusObservable.PLAYING : PlayerStatusObservable.PAUSED);
        episodeViewHolder.mPlayPauseButton.setStatus(playerStatus);

        getPalette(episodeViewHolder);

        episodeViewHolder.mDownloadButton.setEpisode(item);
    }

    protected void getPalette(@NonNull final EpisodeViewHolder episodeViewHolder) {
        PaletteHelper.generate(mSubscription.getImageURL(), mActivity, episodeViewHolder.mDownloadButton);
        PaletteHelper.generate(mSubscription.getImageURL(), mActivity, episodeViewHolder.mQueueButton);
        PaletteHelper.generate(mSubscription.getImageURL(), mActivity, episodeViewHolder.mPlayPauseButton);
    }

    @Override
    public void onViewAttachedToWindow (EpisodeViewHolder holder) {
        SoundWaves.getBus().register(holder.mDownloadButton);
        super.onViewAttachedToWindow(holder);
    }

    @Override
    public void  onViewDetachedFromWindow(EpisodeViewHolder holder) {
        SoundWaves.getBus().unregister(holder.mDownloadButton);
        super.onViewDetachedFromWindow(holder);
    }

    @Override
    public void onViewRecycled(EpisodeViewHolder viewHolder) {
        viewHolder.mDownloadButton.unsetEpisodeId();
        viewHolder.mPlayPauseButton.unsetEpisodeId();
        viewHolder.mQueueButton.unsetEpisodeId();
    }

    @Override
    public int getItemCount() {
        return mSubscription.getEpisodes().size();
    }

    public void setExpanded(boolean expanded) {
        this.mIsExpanded = expanded;
        notifyDataSetChanged();
    }

    protected IEpisode getItemForPosition(int argPosition) {
        return mSubscription.getEpisodes().get(argPosition);
    }

    protected int getDatasetPosition(int argPosition) {
        if (mSortOrder == RECENT_FIRST)
            return argPosition;

        int position = argPosition;
        return getItemCount() - position -1;
    }

    public void setPalette(@NonNull Palette argPalette) {
        mPalette = argPalette;
        this.notifyDataSetChanged();
    }

    public String getSecondaryText(@NonNull IEpisode argItem) {
        mStringBuilder.setLength(0);
        if (argItem.getDuration() > 0) {
            mStringBuilder.append(DateUtils.formatElapsedTime(argItem.getDuration() / 1000));
            mStringBuilder.append(", ");
        }
        //else
        //    mStringBuilder.append("--:--");


        long filesize = argItem.getFilesize();
        if (filesize > 0) {
            mStringBuilder.append(sFormatter.formatShortFileSize(mActivity, argItem.getFilesize()));
            mStringBuilder.append(", ");
        }

        Date date = argItem.getDateTime();
        if (date != null)
            mStringBuilder.append(DateUtils.formatDateTime(mActivity, argItem.getDateTime().getTime(), 0));

        return mStringBuilder.toString();
    }
}
