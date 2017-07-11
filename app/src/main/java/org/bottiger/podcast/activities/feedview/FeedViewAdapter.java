package org.bottiger.podcast.activities.feedview;

import android.content.Context;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.provider.ISubscription;
import org.bottiger.podcast.provider.base.BaseSubscription;
import org.bottiger.podcast.utils.ColorExtractor;
import org.bottiger.podcast.utils.ColorUtils;
import org.bottiger.podcast.utils.SharedAdapterUtils;
import org.bottiger.podcast.utils.StrUtils;
import org.bottiger.podcast.views.PlayPauseButton;

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
public class FeedViewAdapter extends RecyclerView.Adapter<FeedViewHolder> {

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({RECENT_FIRST, OLDEST_FIRST})
    public @interface Order {}
    public static final int RECENT_FIRST = 0;
    public static final int OLDEST_FIRST = 1;

    private static final int EPISODE_TYPE   = 1;
    private static final int FOOTER_TYPE    = 2;

    private static final boolean includeFooter = true;

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
    public FeedViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View view;
        if (viewType == EPISODE_TYPE) {
            view = mInflater.inflate(R.layout.feed_view_list_episode, parent, false);
            return new EpisodeViewHolder(view);
        } else if (viewType == FOOTER_TYPE) {
            view = mInflater.inflate(R.layout.feed_view_list_footer, parent, false);
            return new FooterViewHolder(view);
        }
        throw new RuntimeException("there is no type that matches the type " + viewType + " + make sure your using types correctly");
    }

    private void onBindEpisodeViewHolder(EpisodeViewHolder episodeViewHolder, int position) {
        final int dataPosition = episodeViewHolder.getAdapterPosition();
        final IEpisode item = getItemForPosition(dataPosition);

        if (item == null) {
            VendorCrashReporter.report("FeedViewAdapter", "item is null for: " + mSubscription);
            return;
        }

        SharedAdapterUtils.AddPaddingToLastElement(episodeViewHolder.mContainer, 0, dataPosition == getItemCount()-1);

        final boolean canDownload = item.canDownload();
        @SoundWavesPlayerBase.PlayerState int playerStatus = STATE_IDLE;

        String title = item.getTitle();
        String description = item.getDescription();
        item.setIsNew(false);

        if (!TextUtils.isEmpty(title)) {
            episodeViewHolder.mTitle.setText(item.getTitle());
        }

        episodeViewHolder.mPrimaryColor = ColorUtils.getTextColor(mActivity);
        episodeViewHolder.mFadedColor = ColorUtils.getFadedTextColor(mActivity);
        episodeViewHolder.mTextSecondary.setText(getSecondaryText(item));

        episodeViewHolder.mDescription.setText(description);
        episodeViewHolder.IsMarkedAsListened = item.isMarkedAsListened();
        episodeViewHolder.DisplayDescription = mIsExpanded;

        @EpisodeViewHolder.DisplayState int state = EpisodeViewHolder.COLLAPSED;
        if (mExpanededItems.contains(dataPosition)) {
            state = EpisodeViewHolder.EXPANDED;
        } else if (item.isMarkedAsListened()) {
            state = EpisodeViewHolder.COLLAPSED_LISTENED;
        } else if (mIsExpanded) {
            state = EpisodeViewHolder.COLLAPSED_WITH_DESCRIPTION;
        }

        episodeViewHolder.setState(state, canDownload);

        final EpisodeViewHolder finalEpisodeViewHolder = episodeViewHolder;
        episodeViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FeedViewAdapter.this.onClick(finalEpisodeViewHolder, dataPosition, canDownload);
            }
        });

        if (mActivity.getPlayerHelper().isPlaying(item)) {
            playerStatus = STATE_READY;
        }

        episodeViewHolder.mPlayPauseButton.setEpisode(item, PlayPauseButton.FEEDVIEW);
        episodeViewHolder.mQueueButton.setEpisode(item, PlayPauseButton.FEEDVIEW);
        episodeViewHolder.mDownloadButton.setEpisode(item);

        SoundWaves.getAppContext(mActivity).getPlayer().addListener(episodeViewHolder.mPlayPauseButton);
        episodeViewHolder.mDownloadButton.enabledProgressListener(true);

        getPalette(episodeViewHolder);

        episodeViewHolder.mPlayPauseButton.setStatus(playerStatus);
    }

    private void onBindFooterViewHolder(FooterViewHolder footerViewHolder, int position) {
    }

    @Override
    public void onBindViewHolder(FeedViewHolder feedViewHolder, int position) {

        if (feedViewHolder instanceof FooterViewHolder) {
            onBindFooterViewHolder((FooterViewHolder) feedViewHolder, position);
            return;
        }

        if (feedViewHolder instanceof EpisodeViewHolder) {
            onBindEpisodeViewHolder((EpisodeViewHolder) feedViewHolder, position);
            return;
        }

        throw new RuntimeException("Missing feedViewHolder instanceof check");
    }

    private void onClickEpisode(EpisodeViewHolder episodeViewHolder, int dataPosition, boolean argCanDownload) {
        @EpisodeViewHolder.DisplayState int newState = episodeViewHolder.toggleState(argCanDownload);
        if (newState == EpisodeViewHolder.EXPANDED) {
            mExpanededItems.add(dataPosition);
        } else {
            mExpanededItems.remove(Integer.valueOf(dataPosition));
        }
    }

    private void onClickFooter(FooterViewHolder footerViewHolder, int dataPosition, boolean argCanDownload) {
    }

    public void onClick(FeedViewHolder feedViewHolder, int dataPosition, boolean argCanDownload) {

        if (feedViewHolder instanceof FooterViewHolder) {
            onClickFooter((FooterViewHolder) feedViewHolder, dataPosition, argCanDownload);
            return;
        }

        if (feedViewHolder instanceof EpisodeViewHolder) {
            onClickEpisode((EpisodeViewHolder) feedViewHolder, dataPosition, argCanDownload);
            return;
        }

        throw new RuntimeException("Missing feedViewHolder instanceof check");
    }

    @Override
    public void onViewAttachedToWindow (FeedViewHolder holder) {
        super.onViewAttachedToWindow(holder);
    }

    private void onEpisodeViewDetachedFromWindow(EpisodeViewHolder episodeViewHolder) {
        SoundWaves.getAppContext(mActivity).getPlayer().removeListener(episodeViewHolder.mPlayPauseButton);
        episodeViewHolder.mDownloadButton.enabledProgressListener(false);
    }

    private void onFooterViewDetachedFromWindow(FooterViewHolder footerViewHolder) {
    }

    @Override
    public void onViewDetachedFromWindow(FeedViewHolder feedViewHolder) {

        if (feedViewHolder instanceof FooterViewHolder) {
            onFooterViewDetachedFromWindow((FooterViewHolder) feedViewHolder);
            super.onViewDetachedFromWindow(feedViewHolder);
            return;
        }

        if (feedViewHolder instanceof EpisodeViewHolder) {
            onEpisodeViewDetachedFromWindow((EpisodeViewHolder) feedViewHolder);
            super.onViewDetachedFromWindow(feedViewHolder);
            return;
        }

        throw new RuntimeException("Missing feedViewHolder instanceof check");
    }

    private void onEpisodeViewRecycled(EpisodeViewHolder episodeViewHolder) {
        episodeViewHolder.mDownloadButton.unsetEpisodeId();
        episodeViewHolder.mPlayPauseButton.unsetEpisodeId();
        episodeViewHolder.mQueueButton.unsetEpisodeId();
    }

    private void onFooterViewRecycled(FooterViewHolder footerViewHolder) {
    }

    @Override
    public void onViewRecycled(FeedViewHolder feedViewHolder) {

        if (feedViewHolder instanceof FooterViewHolder) {
            onFooterViewRecycled((FooterViewHolder) feedViewHolder);
            return;
        }

        if (feedViewHolder instanceof EpisodeViewHolder) {
            onEpisodeViewRecycled((EpisodeViewHolder) feedViewHolder);
            return;
        }

        throw new RuntimeException("Missing feedViewHolder instanceof check");
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

    @Override
    public int getItemViewType(int position) {
        if (isPositionFooter(position))
            return FOOTER_TYPE;

        return EPISODE_TYPE;
    }

    @Override
    public int getItemCount() {
        int count = mFilteredEpisodeList.size();
        int addition = includeFooter ? 1 : 0;
        return count + addition;
    }

    private boolean isPositionFooter(int position) {
        return position == getItemCount() - 1;
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

    private void getPaletteEpisode(@NonNull final EpisodeViewHolder episodeViewHolder) {
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

    private void getPaletteFooter(@NonNull final FooterViewHolder footerViewHolder) {
    }

    protected void getPalette(@NonNull final FeedViewHolder feedViewHolder) {

        if (feedViewHolder instanceof FooterViewHolder) {
            getPaletteFooter((FooterViewHolder) feedViewHolder);
            return;
        }

        if (feedViewHolder instanceof EpisodeViewHolder) {
            getPaletteEpisode((EpisodeViewHolder) feedViewHolder);
            return;
        }

        throw new RuntimeException("Missing feedViewHolder instanceof check");
    }

    public @Order int calcOrder() {
        return mSubscription.isListOldestFirst(mActivity.getResources()) ? OLDEST_FIRST : RECENT_FIRST;
    }
}
