package org.bottiger.podcast.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.bottiger.podcast.MainActivity;
import org.bottiger.podcast.R;
import org.bottiger.podcast.listeners.DownloadProgressObservable;
import org.bottiger.podcast.listeners.PlayerStatusObservable;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.Subscription;
import org.bottiger.podcast.utils.PaletteCache;
import org.bottiger.podcast.views.DownloadButtonView;
import org.bottiger.podcast.views.FeedViewQueueButton;
import org.bottiger.podcast.views.PlayPauseImageView;
import org.bottiger.podcast.views.PlayerButtonView;

/**
 * Created by apl on 02-09-2014.
 */
public class FeedViewAdapter extends AbstractEpisodeCursorAdapter<FeedViewAdapter.EpisodeViewHolder> {

    private Subscription mSubscription;

    private DownloadProgressObservable mDownloadProgressObservable;
    private Palette mPalette;
    private boolean mIsExpanded = false;

    public FeedViewAdapter(Context context, Cursor dataset) {
        super(dataset);
        mContext = context;

        mInflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        mDownloadProgressObservable = new DownloadProgressObservable(mContext);
        setDataset(dataset);
    }

    public void setDataset(Cursor c) {
        mCursor = c;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View view = mInflater.inflate(R.layout.feed_view_list_episode, viewGroup, false);
        return new EpisodeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        mCursor.moveToPosition(position);

        final FeedItem item = FeedItem.getByCursor(mCursor);
        final EpisodeViewHolder episodeViewHolder = (EpisodeViewHolder) viewHolder;

        if (mSubscription == null) {
            mSubscription = item.getSubscription(mContext);
        }

        boolean isPlaying = false;
        if (MainActivity.sBoundPlayerService != null && MainActivity.sBoundPlayerService.isInitialized()) {
            if (item.getId() == MainActivity.sBoundPlayerService
                    .getCurrentItem().id) {
                if (MainActivity.sBoundPlayerService.isPlaying()) {
                    isPlaying = true;
                }
            }
        }

        episodeViewHolder.mText.setText(item.getTitle());
        episodeViewHolder.mDescription.setText(item.content);

        if (mIsExpanded != episodeViewHolder.IsExpanded) {
            episodeViewHolder.IsExpanded = mIsExpanded;
            episodeViewHolder.modifyLayout((RelativeLayout)viewHolder.itemView);
        }

        episodeViewHolder.mPlayPauseButton.setEpisodeId(item.getId(), PlayPauseImageView.LOCATION.FEEDVIEW);
        episodeViewHolder.mQueueButton.setEpisodeId(item.getId(), PlayPauseImageView.LOCATION.FEEDVIEW);
        episodeViewHolder.mPlayPauseButton.setStatus(isPlaying ? PlayerStatusObservable.STATUS.PLAYING : PlayerStatusObservable.STATUS.PAUSED);


        episodeViewHolder.mDownloadButton.setEpisode(item);
        mDownloadProgressObservable.registerObserver(episodeViewHolder.mDownloadButton);

        if (mPalette != null) {
            episodeViewHolder.mPlayPauseButton.onPaletteFound(mPalette);
            episodeViewHolder.mQueueButton.onPaletteFound(mPalette);
            episodeViewHolder.mDownloadButton.onPaletteFound(mPalette);
        }

        if (mSubscription.getPaletteUrl() != null) {
            Palette palette = PaletteCache.get(mSubscription);
            if (palette != null)
                mPalette = palette;
        }
    }

    @Override
    public void onViewRecycled(RecyclerView.ViewHolder viewHolder) {
        final EpisodeViewHolder episodeViewHolder = (EpisodeViewHolder) viewHolder;
        mDownloadProgressObservable.unregisterObserver(episodeViewHolder.mDownloadButton);
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

    public class EpisodeViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        public ViewGroup mContainer;
        public TextView mText;
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
            mDescription = (TextView) view.findViewById(R.id.episode_description);
            mPlayPauseButton = (PlayPauseImageView) view.findViewById(R.id.play_pause_button);
            mQueueButton = (FeedViewQueueButton) view.findViewById(R.id.queue_button);
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

            argParent.updateViewLayout(mText, params);
        }
    }

    public void setPalette(@NonNull Palette argPalette) {
        mPalette = argPalette;
        this.notifyDataSetChanged();
    }
}
