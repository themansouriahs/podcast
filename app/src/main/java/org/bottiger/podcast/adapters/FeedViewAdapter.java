package org.bottiger.podcast.adapters;

import android.animation.LayoutTransition;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.bottiger.podcast.MainActivity;
import org.bottiger.podcast.R;
import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.listeners.DownloadProgressObservable;
import org.bottiger.podcast.listeners.PaletteListener;
import org.bottiger.podcast.listeners.PlayerStatusObservable;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.provider.ISubscription;
import org.bottiger.podcast.utils.PaletteHelper;
import org.bottiger.podcast.views.DownloadButtonView;
import org.bottiger.podcast.views.FeedViewQueueButton;
import org.bottiger.podcast.views.PlayPauseImageView;

/**
 * Created by apl on 02-09-2014.
 */
public class FeedViewAdapter extends RecyclerView.Adapter {

    public enum ORDER { RECENT_FIRST, OLDEST_FIRST}

    protected ISubscription mSubscription;

    protected Cursor mCursor;
    protected Activity mActivity;
    protected LayoutInflater mInflater;

    protected Palette mPalette;

    private DownloadProgressObservable mDownloadProgressObservable;
    private boolean mIsExpanded = false;
    protected ORDER mSortOrder = ORDER.RECENT_FIRST;

    public FeedViewAdapter(@NonNull Activity activity, @NonNull ISubscription argSubscription, @Nullable Cursor dataset) {
        mActivity = activity;
        mSubscription = argSubscription;

        mInflater = (LayoutInflater) activity
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        mDownloadProgressObservable = new DownloadProgressObservable((SoundWaves)mActivity.getApplicationContext());
        setDataset(dataset);
    }

    public void setDataset(Cursor c) {
        mCursor = c;
    }

    public ORDER getOrder() {
        return mSortOrder;
    }

    public void setOrder(ORDER argSortOrder) {
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


        episodeViewHolder.mText.setText(item.getTitle());
        episodeViewHolder.mDescription.setText(item.getDescription());

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
        if (MainActivity.sBoundPlayerService != null && MainActivity.sBoundPlayerService.isInitialized()) {
            if (item.getURL().equals(MainActivity.sBoundPlayerService
                    .getCurrentItem().getUrl().toString())) {
                if (MainActivity.sBoundPlayerService.isPlaying()) {
                    isPlaying = true;
                }
            }
        }

        episodeViewHolder.mPlayPauseButton.setEpisode(item, PlayPauseImageView.LOCATION.FEEDVIEW);
        episodeViewHolder.mQueueButton.setEpisode(item, PlayPauseImageView.LOCATION.FEEDVIEW);
        episodeViewHolder.mPlayPauseButton.setStatus(isPlaying ? PlayerStatusObservable.STATUS.PLAYING : PlayerStatusObservable.STATUS.PAUSED);

        getPalette(episodeViewHolder);

        episodeViewHolder.mDownloadButton.setEpisode(item);
    }

    protected void getPalette(@NonNull final EpisodeViewHolder episodeViewHolder) {
        PaletteHelper.generate(mSubscription.getImageURL(), mActivity, episodeViewHolder.mDownloadButton);
        PaletteHelper.generate(mSubscription.getImageURL(), mActivity, episodeViewHolder.mQueueButton);
        PaletteHelper.generate(mSubscription.getImageURL(), mActivity, episodeViewHolder.mPlayPauseButton);
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
        if (mSortOrder == ORDER.RECENT_FIRST)
            return argPosition;

        int position = argPosition;
        return getItemCount() - position -1;
    }

    public class EpisodeViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        public ViewGroup mContainer;
        public TextView mText;
        public TextView mDescription;
        public PlayPauseImageView mPlayPauseButton;
        public FeedViewQueueButton mQueueButton;
        public DownloadButtonView mDownloadButton;
        public com.andexert.library.RippleView mQueueRipple;

        public boolean IsExpanded = false;

        @SuppressLint("WrongViewCast")
        public EpisodeViewHolder(View view) {
            super(view);
            view.setOnClickListener(this);

            mContainer = (ViewGroup) view.findViewById(R.id.group);
            mText = (TextView) view.findViewById(R.id.title);
            mDescription = (TextView) view.findViewById(R.id.episode_description);
            mPlayPauseButton = (PlayPauseImageView) view.findViewById(R.id.play_pause_button);
            mQueueButton = (FeedViewQueueButton) view.findViewById(R.id.queue_button);
            mQueueRipple = (com.andexert.library.RippleView) view.findViewById(R.id.queue_button_ripple);
            mDownloadButton = (DownloadButtonView) view.findViewById(R.id.feedview_download_button);
        }


        @Override
        public void onClick(View view) {
        }

        public void modifyLayout(@NonNull ViewGroup argParent) {

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
            mDescription.setVisibility(visibility);
            mQueueButton.setVisibility(visibility);
            mQueueRipple.setVisibility(visibility);

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
