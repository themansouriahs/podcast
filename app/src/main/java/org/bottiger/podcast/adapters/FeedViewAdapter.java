package org.bottiger.podcast.adapters;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.os.Build;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.bottiger.podcast.R;
import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.listeners.PlayerStatusObservable;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.provider.ISubscription;
import org.bottiger.podcast.utils.PaletteHelper;
import org.bottiger.podcast.utils.SharedAdapterUtils;
import org.bottiger.podcast.utils.StrUtils;
import org.bottiger.podcast.utils.UIUtils;
import org.bottiger.podcast.views.DownloadButtonView;
import org.bottiger.podcast.views.FeedViewQueueButton;
import org.bottiger.podcast.views.PlayPauseImageView;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Date;
import java.util.Formatter;

/**
 * Created by apl on 02-09-2014.
 */
public class FeedViewAdapter extends RecyclerView.Adapter {

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({RECENT_FIRST, OLDEST_FIRST})
    public @interface Order {}
    public static final int RECENT_FIRST = 0;
    public static final int OLDEST_FIRST = 1;

    protected ISubscription mSubscription;

    protected Cursor mCursor;
    protected Activity mActivity;
    protected LayoutInflater mInflater;

    protected Palette mPalette;

    public static boolean mIsExpanded = false;
    protected @Order int mSortOrder = RECENT_FIRST;

    private StringBuilder mStringBuilder = new StringBuilder(100);

    public FeedViewAdapter(@NonNull Activity activity, @NonNull ISubscription argSubscription, @Nullable Cursor dataset) {
        mActivity = activity;
        mSubscription = argSubscription;

        mInflater = (LayoutInflater) activity
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        setDataset(dataset);
    }

    public void setDataset(Cursor c) {
        mCursor = c;
    }

    public @Order int getOrder() {
        return mSortOrder;
    }

    public void setOrder(@Order int argSortOrder) {
        mSortOrder = argSortOrder;
        notifyDataSetChanged();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View view = mInflater.inflate(R.layout.feed_view_list_episode, viewGroup, false);
        return new EpisodeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        int dataPosition = getDatasetPosition(position);
        final IEpisode item = getItemForPosition(dataPosition);
        final EpisodeViewHolder episodeViewHolder = (EpisodeViewHolder) viewHolder;

        SharedAdapterUtils.AddPaddingToLastElement(((EpisodeViewHolder) viewHolder).mContainer, 0, position == getItemCount()-1);


        episodeViewHolder.mText.setText(item.getTitle());
        episodeViewHolder.mDescription.setText(item.getDescription());

        android.text.format.Formatter formatter = new android.text.format.Formatter();
        mStringBuilder.setLength(0);
        if (item.getDuration() > 0)
            mStringBuilder.append(DateUtils.formatElapsedTime(item.getDuration()/1000));
        else
            mStringBuilder.append("--:--");
        mStringBuilder.append(", ");
        mStringBuilder.append(formatter.formatShortFileSize(mActivity, item.getFilesize()));
        mStringBuilder.append(", ");

        Date date = item.getDateTime();
        if (date != null)
            mStringBuilder.append(DateUtils.formatDateTime(mActivity, item.getDateTime().getTime(), 0));

        episodeViewHolder.mTextSecondary.setText(mStringBuilder.toString());

        if (mIsExpanded != episodeViewHolder.IsExpanded) {
            episodeViewHolder.IsExpanded = mIsExpanded;
            episodeViewHolder.modifyLayout((RelativeLayout)viewHolder.itemView);
        }

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
        if (SoundWaves.sBoundPlayerService != null && SoundWaves.sBoundPlayerService.isInitialized()) {
            if (item.getURL().equals(SoundWaves.sBoundPlayerService
                    .getCurrentItem().getUrl().toString())) {
                if (SoundWaves.sBoundPlayerService.isPlaying()) {
                    isPlaying = true;
                }
                playerStatus = SoundWaves.sBoundPlayerService.getPlayer().getStatus();
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
    public void onViewAttachedToWindow (RecyclerView.ViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        final EpisodeViewHolder episodeViewHolder = (EpisodeViewHolder) holder;
        SoundWaves.getBus().register(episodeViewHolder.mDownloadButton);
    }

    @Override
    public void  onViewDetachedFromWindow(RecyclerView.ViewHolder holder) {
        final EpisodeViewHolder episodeViewHolder = (EpisodeViewHolder) holder;
        SoundWaves.getBus().unregister(episodeViewHolder.mDownloadButton);
        super.onViewDetachedFromWindow(holder);
    }

    @Override
    public void onViewRecycled(RecyclerView.ViewHolder viewHolder) {
        final EpisodeViewHolder episodeViewHolder = (EpisodeViewHolder) viewHolder;
        //mDownloadProgressObservable.unregisterObserver(episodeViewHolder.mDownloadButton);
        episodeViewHolder.mDownloadButton.unsetEpisodeId();
        episodeViewHolder.mPlayPauseButton.unsetEpisodeId();
        episodeViewHolder.mQueueButton.unsetEpisodeId();
    }

    @Override
    public int getItemCount() {
        return mCursor == null ? 0 : mCursor.getCount();
    }

    public void setExpanded(boolean expanded) {
        this.mIsExpanded = expanded;
        notifyDataSetChanged();
    }

    protected IEpisode getItemForPosition(int argPosition) {
        mCursor.moveToPosition(argPosition);
        return FeedItem.getByCursor(mCursor);
    }

    protected int getDatasetPosition(int argPosition) {
        if (mSortOrder == RECENT_FIRST)
            return argPosition;

        int position = argPosition;
        return getItemCount() - position -1;
    }

    public class EpisodeViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        public ViewGroup mContainer;
        public TextView mText;
        public TextView mTextSecondary;
        public TextView mDescription;
        public PlayPauseImageView mPlayPauseButton;
        public FeedViewQueueButton mQueueButton;
        public DownloadButtonView mDownloadButton;

        public boolean IsExpanded = false;

        @SuppressLint("WrongViewCast")
        public EpisodeViewHolder(View view) {
            super(view);
            view.setOnClickListener(this);

            mContainer = (ViewGroup) view.findViewById(R.id.group);
            mText = (TextView) view.findViewById(R.id.title);
            mTextSecondary = (TextView) view.findViewById(R.id.subtitle);
            mDescription = (TextView) view.findViewById(R.id.episode_description);
            mPlayPauseButton = (PlayPauseImageView) view.findViewById(R.id.play_pause_button);
            mQueueButton = (FeedViewQueueButton) view.findViewById(R.id.queue_button);
            mDownloadButton = (DownloadButtonView) view.findViewById(R.id.feedview_download_button);
        }


        @Override
        public void onClick(View view) {
        }

        public void modifyLayout(@NonNull ViewGroup argParent) {


            //if (Build.VERSION.SDK_INT >= 19) {
            //    TransitionManager.beginDelayedTransition(argParent);
            //}

            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mText.getLayoutParams();

            if (IsExpanded) {
                if (Build.VERSION.SDK_INT >= 17) {
                    params.removeRule(RelativeLayout.CENTER_VERTICAL);
                } else {
                    params.addRule(RelativeLayout.CENTER_VERTICAL, 0);
                }
            } else {
                params.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
            }

            //mText.setLayoutParams(params);

            int visibility = IsExpanded ? View.VISIBLE : View.GONE;
            float alphaStart = IsExpanded ? 0.0f : 1.0f;
            float alphaEnd = IsExpanded ? 1.0f : 0.0f;

            mDescription.setVisibility(visibility);
            mQueueButton.setVisibility(visibility);

            mDescription.setAlpha(alphaStart);
            //mDescription.animate().alpha(alphaEnd).start();

            ViewCompat.animate(mDescription).alpha(alphaEnd).start();

            argParent.updateViewLayout(mText, params);

            //if (Build.VERSION.SDK_INT >= 17) {
            //    LayoutTransition transition = argParent.getLayoutTransition();
            //    transition.enableTransitionType(LayoutTransition.CHANGING);
            //}
        }
    }

    public void setPalette(@NonNull Palette argPalette) {
        mPalette = argPalette;
        this.notifyDataSetChanged();
    }
}
