package org.bottiger.podcast.activities.feedview;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.ColorInt;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.bottiger.podcast.R;
import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.TopActivity;
import org.bottiger.podcast.flavors.CrashReporter.VendorCrashReporter;
import org.bottiger.podcast.model.datastructures.EpisodeFilter;
import org.bottiger.podcast.model.datastructures.EpisodeList;
import org.bottiger.podcast.player.SoundWavesPlayerBase;
import org.bottiger.podcast.player.exoplayer.ExoPlayerWrapper;
import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.provider.ISubscription;
import org.bottiger.podcast.provider.base.BaseSubscription;
import org.bottiger.podcast.utils.ColorExtractor;
import org.bottiger.podcast.utils.ColorUtils;
import org.bottiger.podcast.utils.PaletteHelper;
import org.bottiger.podcast.utils.SharedAdapterUtils;
import org.bottiger.podcast.utils.StrUtils;
import org.bottiger.podcast.views.PlayPauseImageView;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;

import static org.bottiger.podcast.player.SoundWavesPlayerBase.STATE_IDLE;
import static org.bottiger.podcast.player.SoundWavesPlayerBase.STATE_READY;

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
    private EpisodeList<IEpisode> mEpisodeList;
    private LinkedList<IEpisode> mFilteredEpisodeList = new LinkedList<>();

    protected TopActivity mActivity;
    protected LayoutInflater mInflater;

    protected ColorExtractor mPalette;

    public static boolean mIsExpanded = false;
    protected @Order int mSortOrder = RECENT_FIRST;

    private ArrayList<Integer> mExpanededItems = new ArrayList<>();

    private StringBuilder mStringBuilder = new StringBuilder(100);

    public FeedViewAdapter(@NonNull TopActivity activity, @NonNull ISubscription argSubscription) {
        mActivity = activity;
        setDataset(argSubscription);

        mSortOrder = calcOrder();

        mInflater = (LayoutInflater) activity
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public void setDataset(@NonNull ISubscription argSubscription) {
        mSubscription = argSubscription;
        notifyEpisodesChanged();
    }

    public void notifyEpisodesChanged() {
        mEpisodeList = mSubscription.getEpisodes();
        mFilteredEpisodeList = mEpisodeList.getFilteredList();
        notifyDataSetChanged();
    }

    public @Order int getOrder() {
        return mSortOrder;
    }

    public void setOrder(@Order int argSortOrder) {
        mSortOrder = argSortOrder;
        notifyDataSetChanged();
    }

    public void search(@Nullable String argSearchQuery) {
        EpisodeFilter filter = mEpisodeList.getFilter();
        filter.setSearchQuery(argSearchQuery);

        mFilteredEpisodeList = mEpisodeList.getFilteredList();

        notifyDataSetChanged();
    }

    @Override
    public EpisodeViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View view = mInflater.inflate(R.layout.feed_view_list_episode, viewGroup, false);
        return new EpisodeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(EpisodeViewHolder viewHolder, int position) {
        final int dataPosition = viewHolder.getAdapterPosition();
        final IEpisode item = getItemForPosition(dataPosition);
        final EpisodeViewHolder episodeViewHolder = viewHolder;

        SharedAdapterUtils.AddPaddingToLastElement((viewHolder).mContainer, 0, dataPosition == getItemCount()-1);

        if (item == null) {
            VendorCrashReporter.report("FeedViewAdapter", "item is null for: " + mSubscription);
            return;
        }

        episodeViewHolder.mPrimaryColor = ColorUtils.getTextColor(mActivity);
        episodeViewHolder.mFadedColor = ColorUtils.getFadedTextColor(mActivity);

        if (item.getTitle() != null)
            episodeViewHolder.mTitle.setText(item.getTitle());

        episodeViewHolder.mDescription.setText(item.getDescription());
        episodeViewHolder.IsMarkedAsListened = item.isMarkedAsListened();
        episodeViewHolder.DisplayDescription = mIsExpanded;

        episodeViewHolder.mTextSecondary.setText(getSecondaryText(item));

        @EpisodeViewHolder.DisplayState int state = EpisodeViewHolder.COLLAPSED;
        if (mExpanededItems.contains(dataPosition)) {
            state = EpisodeViewHolder.EXPANDED;
        } else if (item.isMarkedAsListened()) {
            state = EpisodeViewHolder.COLLAPSED_LISTENED;
        } else if (mIsExpanded) {
            state = EpisodeViewHolder.COLLAPSED_WITH_DESCRIPTION;
        }

        final boolean canDownload = item.canDownload();
        episodeViewHolder.setState(state, canDownload);

        episodeViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FeedViewAdapter.this.onClick(episodeViewHolder, dataPosition, canDownload);
            }
        });

        @SoundWavesPlayerBase.PlayerState int playerStatus = STATE_IDLE;

        if (mActivity.getPlayerHelper().isPlaying(item)) {
            playerStatus = STATE_READY;
        }

        episodeViewHolder.mPlayPauseButton.setEpisode(item, PlayPauseImageView.FEEDVIEW);
        episodeViewHolder.mQueueButton.setEpisode(item, PlayPauseImageView.FEEDVIEW);

        episodeViewHolder.mDownloadButton.setEpisode(item);

        SoundWaves.getAppContext(mActivity).getPlayer().addListener(episodeViewHolder.mPlayPauseButton);
        episodeViewHolder.mDownloadButton.enabledProgressListener(true);

        getPalette(viewHolder);

        episodeViewHolder.mPlayPauseButton.setStatus(playerStatus);

    }

    public void onClick(EpisodeViewHolder episodeViewHolder, int dataPosition, boolean argCanDownload) {
        @EpisodeViewHolder.DisplayState int newState = episodeViewHolder.toggleState(argCanDownload);
        if (newState == EpisodeViewHolder.EXPANDED) {
            mExpanededItems.add(dataPosition);
        } else {
            mExpanededItems.remove(Integer.valueOf(dataPosition));
        }
    }

    @Override
    public void onViewAttachedToWindow (EpisodeViewHolder holder) {
        super.onViewAttachedToWindow(holder);
    }

    @Override
    public void  onViewDetachedFromWindow(EpisodeViewHolder holder) {
        SoundWaves.getAppContext(mActivity).getPlayer().removeListener(holder.mPlayPauseButton);
        holder.mDownloadButton.enabledProgressListener(false);
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
        return mFilteredEpisodeList.size();
    }

    public void setExpanded(boolean expanded) {
        mIsExpanded = expanded;
        notifyDataSetChanged();
    }

    protected IEpisode getItemForPosition(int argPosition) {
        if (mSubscription.isListOldestFirst(mActivity.getResources())) {
            argPosition = getDatasetPosition(argPosition);
        }
        return mFilteredEpisodeList.get(argPosition);
    }

    protected int getDatasetPosition(int argPosition) {
        if (mSortOrder == RECENT_FIRST)
            return argPosition;

        return getItemCount() - argPosition -1;
    }

    public void setPalette(@NonNull ColorExtractor argExtractor) {
        mPalette = argExtractor;
        this.notifyDataSetChanged();
    }

    private String getSecondaryText(@NonNull IEpisode argItem) {
        mStringBuilder.setLength(0);

        Date date = argItem.getDateTime();
        if (date != null) {
            int flags = DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_NO_MIDNIGHT;
            //mStringBuilder.append(DateUtils.formatDateTime(mActivity, argItem.getDateTime().getTime(), flags));
            mStringBuilder.append(DateUtils.getRelativeTimeSpanString(argItem.getDateTime().getTime()));
            mStringBuilder.append(", ");
        }

        if (argItem.getDuration() > 0) {
            //mStringBuilder.append(DateUtils.formatElapsedTime(argItem.getDuration() / 1000));
            mStringBuilder.append(StrUtils.formatTimeText(argItem.getDuration(), true));
            mStringBuilder.append(", ");
        }

        long filesize = argItem.getFilesize();
        if (filesize > 0) {
            mStringBuilder.append(Formatter.formatShortFileSize(mActivity, argItem.getFilesize()));
        }

        return mStringBuilder.toString();
    }

    protected void getPalette(@NonNull final EpisodeViewHolder episodeViewHolder) {
        mSubscription.getColors(mActivity)
                .subscribeOn(io.reactivex.schedulers.Schedulers.io())
                .observeOn(io.reactivex.android.schedulers.AndroidSchedulers.mainThread())
                .subscribe(new BaseSubscription.BasicColorExtractorObserver<ColorExtractor>() {

                    @Override
                    public void onSuccess(ColorExtractor value) {
                        episodeViewHolder.mPlayPauseButton.setColor(value);
                        episodeViewHolder.mQueueButton.onPaletteFound(value);
                        episodeViewHolder.mDownloadButton.onPaletteFound(value);
                    }
                });
    }

    public @Order int calcOrder() {
        return mSubscription.isListOldestFirst(mActivity.getResources()) ? OLDEST_FIRST : RECENT_FIRST;
    }
}
