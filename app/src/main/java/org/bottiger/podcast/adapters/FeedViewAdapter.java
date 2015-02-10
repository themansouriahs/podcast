package org.bottiger.podcast.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.squareup.picasso.Callback;

import org.bottiger.podcast.PodcastBaseFragment;
import org.bottiger.podcast.R;
import org.bottiger.podcast.listeners.DownloadProgressObservable;
import org.bottiger.podcast.listeners.PlayerStatusObservable;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.utils.PaletteCache;
import org.bottiger.podcast.views.DownloadButtonView;
import org.bottiger.podcast.views.PlayPauseImageView;

/**
 * Created by apl on 02-09-2014.
 */
public class FeedViewAdapter extends AbstractEpisodeCursorAdapter<FeedViewAdapter.EpisodeViewHolder> {

    private Fragment mFragment;
    private DownloadProgressObservable mDownloadProgressObservable;
    private Palette mPalette;

    public FeedViewAdapter(Context context, Fragment fragment, Cursor dataset) {
        super(dataset);
        mContext = context;
        mFragment = fragment;

        mInflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        mDownloadProgressObservable = new DownloadProgressObservable(mContext);
        //PodcastDownloadManager.addDownloadProgressObservable(mDownloadProgressObservable);
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
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int i) {
        mCursor.moveToPosition(i);

        final FeedItem item = FeedItem.getByCursor(mCursor);
        final EpisodeViewHolder episodeViewHolder = (EpisodeViewHolder) viewHolder;

        boolean isPlaying = false;
        if (PodcastBaseFragment.mPlayerServiceBinder != null && PodcastBaseFragment.mPlayerServiceBinder.isInitialized()) {
            if (item.getId() == PodcastBaseFragment.mPlayerServiceBinder
                    .getCurrentItem().id) {
                if (PodcastBaseFragment.mPlayerServiceBinder.isPlaying()) {
                    isPlaying = true;
                }
            }
        }

        episodeViewHolder.mText.setText(item.getTitle());

        Palette.Swatch swatch = null;//mPalette.getVibrantSwatch(); // .getDarkMutedColor(R.color.colorPrimaryDark);
        int c = swatch != null ? swatch.getBodyTextColor() : R.color.white_opaque;

        episodeViewHolder.mPlayPauseButton.setEpisodeId(item.getId());
        episodeViewHolder.mPlayPauseButton.setStatus(isPlaying ? PlayerStatusObservable.STATUS.PLAYING : PlayerStatusObservable.STATUS.PAUSED);


        episodeViewHolder.mDownloadButton.setEpisode(item);
        mDownloadProgressObservable.registerObserver(episodeViewHolder.mDownloadButton);

        Palette palette = PaletteCache.get(episodeViewHolder.mPlayPauseButton);
        if (palette != null)
            mPalette = palette;

        if (mPalette != null) {
            episodeViewHolder.mPlayPauseButton.onPaletteFound(mPalette);
            episodeViewHolder.mDownloadButton.onPaletteFound(mPalette);
        }
    }

    @Override
    public void onViewRecycled(RecyclerView.ViewHolder viewHolder) {
        final EpisodeViewHolder episodeViewHolder = (EpisodeViewHolder) viewHolder;
        mDownloadProgressObservable.unregisterObserver(episodeViewHolder.mDownloadButton);

        //PaletteObservable.unregisterListener(episodeViewHolder.mPlayPauseButton);
        //PaletteObservable.unregisterListener(episodeViewHolder.mDownloadButton);
    }

    @Override
    public int getItemCount() {
        return mCursor == null ? 0 : mCursor.getCount();
    }


    public class EpisodeViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        public TextView mText;
        public PlayPauseImageView mPlayPauseButton;
        public DownloadButtonView mDownloadButton;

        public Callback mPicassoCallback;

        @SuppressLint("WrongViewCast")
        public EpisodeViewHolder(View view) {
            super(view);
            view.setOnClickListener(this);

            mText = (TextView) view.findViewById(R.id.title);
            mPlayPauseButton = (PlayPauseImageView) view.findViewById(R.id.play_pause_button);
            mDownloadButton = (DownloadButtonView) view.findViewById(R.id.download_button);
        }


        @Override
        public void onClick(View view) {
        }
    }

    public void setPalette(@NonNull Palette argPalette) {
        mPalette = argPalette;
        this.notifyDataSetChanged();
    }
}
