package org.bottiger.podcast.adapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.BitmapImageViewTarget;

import org.bottiger.podcast.R;
import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.adapters.viewholders.ExpandableViewHoldersUtil;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.provider.ISubscription;
import org.bottiger.podcast.provider.base.BaseSubscription;
import org.bottiger.podcast.utils.ColorExtractor;
import org.bottiger.podcast.utils.ColorUtils;
import org.bottiger.podcast.utils.ImageLoaderUtils;
import org.bottiger.podcast.utils.StrUtils;
import org.bottiger.podcast.views.Overlay;
import org.bottiger.podcast.views.PlayPauseButton;
import org.bottiger.podcast.views.PlaylistViewHolder;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.URL;
import java.util.Locale;

import static org.bottiger.podcast.player.SoundWavesPlayerBase.STATE_IDLE;

public class PlaylistAdapter extends AbstractPodcastAdapter<PlaylistViewHolder> {

    private static final String TAG = PlaylistAdapter.class.getSimpleName();

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({TYPE_EXPAND, TYPE_COLLAPSE})
    @interface EpisodeViewState {}
    private static final int TYPE_EXPAND = 1;
	private static final int TYPE_COLLAPSE = 2;

    private static final int PLAYLIST_OFFSET = 1;

    private ExpandableViewHoldersUtil.KeepOneH<PlaylistViewHolder> keepOne = new ExpandableViewHoldersUtil.KeepOneH<>();

    private Activity mActivity;
    private Overlay mOverlay;

    public PlaylistAdapter(@NonNull Activity argActivity, Overlay argOverlay) {
        super(argActivity);
        mActivity = argActivity;
        mOverlay = argOverlay;
        mInflater = (LayoutInflater) mActivity
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        mPlaylist = SoundWaves.getAppContext(argActivity).getPlaylist();
    }

    @Override
    public PlaylistViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        Log.v(TAG, "onCreateViewHolder");

        View view = mInflater.inflate(R.layout.episode_list, viewGroup, false);
        return new PlaylistViewHolder(view, mActivity);
    }

    @Override
    public void onBindViewHolder(final PlaylistViewHolder viewHolder, int position) {
        Log.v(TAG, "onBindViewHolder(pos: " + position + ")");

        final int dataPosition = viewHolder.getAdapterPosition();
        final IEpisode item = mPlaylist.getItem(dataPosition+PLAYLIST_OFFSET);

        if (item == null) {
            // This should only happen if the playlist only contain 1 item
            return;
        }

        Context context = SoundWaves.getAppContext(mActivity);
        int textColor = item.isMarkedAsListened() ? ColorUtils.getFadedTextColor(context) : ColorUtils.getTextColor(context);

        viewHolder.setArtwork(null);
        ISubscription subscription = item.getSubscription(context);

        String image = item.getArtwork(mActivity);
        if (StrUtils.isValidUrl(image)) {
            assert image != null;

            RequestOptions options = new RequestOptions();
            options.centerCrop();
            RequestBuilder<Bitmap> builder = ImageLoaderUtils.getGlide(mActivity, image);
            builder.apply(options);
            builder.into(new BitmapImageViewTarget(viewHolder.mPodcastImage) {
                @Override
                protected void setResource(Bitmap resource) {
                    RoundedBitmapDrawable circularBitmapDrawable =
                            RoundedBitmapDrawableFactory.create(mActivity.getResources(), resource);
                    float radius = mActivity.getResources().getDimension(R.dimen.playlist_image_radius_small);
                    circularBitmapDrawable.setCornerRadius(radius);
                    viewHolder.mPodcastImage.setImageDrawable(circularBitmapDrawable);
                }
            });

            viewHolder.setArtwork(subscription);
        }

        Log.d("ExpanderHelper", "pos: " + dataPosition + " episode: " + item.getTitle());

        viewHolder.mPlayPauseButton.setIconColor(Color.WHITE);

        subscription.getColors(mActivity)
                .subscribeOn(io.reactivex.schedulers.Schedulers.io())
                .observeOn(io.reactivex.android.schedulers.AndroidSchedulers.mainThread())
                .subscribe(new BaseSubscription.BasicColorExtractorObserver<ColorExtractor>() {

                    @Override
                    public void onSuccess(ColorExtractor value) {
                        viewHolder.mPlayPauseButton.setColor(value);
                        viewHolder.setEpisodePrimaryColor(value.getPrimary());
                    }
                });

        viewHolder.setEpisode(item);
        viewHolder.mAdapter = this;

        viewHolder.mMainTitle.setText(item.getTitle());
        viewHolder.mMainTitle.setTextColor(textColor);
        viewHolder.mSecondaryTitle.setText(item.getSubscription(mActivity).getTitle());

        viewHolder.description.setText(item.getDescription());
        bindDuration(viewHolder, item);

        if (item.getPriority() > 0) {
            viewHolder.mPlaylistPosition.setText(String.format(Locale.getDefault(), "%d", dataPosition+1));
            viewHolder.mPlaylistPosition.setVisibility(View.VISIBLE);
        } else {
            viewHolder.mPlaylistPosition.setVisibility(View.GONE);
        }

        viewHolder.mPlayPauseButton.setEpisode(item, PlayPauseButton.PLAYLIST);
        viewHolder.mPlayPauseButton.setStatus(STATE_IDLE);
        viewHolder.downloadButton.setEpisode(item);

        // needs the downloadButton to be bound in advance
        keepOne.bind(viewHolder, dataPosition);


        viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                                                   @Override
                                                   public void onClick(View v) {
                                                       PlaylistAdapter.this.toggle(viewHolder);
                                                   }
                                               });



        bindExandedPlayer(mActivity, item, viewHolder, dataPosition);
    }

    /**
     * Expands the StubView and creates the expandable extended_player. This is done for
     * the current playing episode and at most one other episode which the user
     * is interacting with
     *
     * @param holder
     * @param position
     */
    private void bindExandedPlayer(final Context context, final IEpisode argEpisode,
                                  final PlaylistViewHolder holder, final int position) {
        Log.v("PlaylistAdapter", "bindExandedPlayer");

        final boolean isFeedItem = (argEpisode instanceof FeedItem);
        final FeedItem feedItem = isFeedItem ? (FeedItem) argEpisode : null;
        long playerPosition = isFeedItem ? feedItem.getOffset() : 0;

        holder.currentTime.setText(StrUtils.formatTime(playerPosition));

        holder.seekbar.setEpisode(argEpisode);
        holder.seekbar.setOverlay(mOverlay);

        holder.mPlayPauseButton.setEpisode(argEpisode, PlayPauseButton.PLAYLIST);
        holder.downloadButton.setEpisode(argEpisode);

        ISubscription subscription = argEpisode.getSubscription(context);
        subscription.getColors(context)
                .subscribeOn(io.reactivex.schedulers.Schedulers.io())
                .observeOn(io.reactivex.android.schedulers.AndroidSchedulers.mainThread())
                .subscribe(new BaseSubscription.BasicColorExtractorObserver<ColorExtractor>() {

                    @Override
                    public void onSuccess(ColorExtractor value) {
                        holder.downloadButton.onPaletteFound(value);
                    }
                });


        holder.getRemoveButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PlaylistAdapter.this.toggle(holder);

                if (isFeedItem) { // FIXME
                    feedItem.removeFromPlaylist(context.getContentResolver());
                }

                notifyDataSetChanged();
                mPlaylist.removeItem(position + PLAYLIST_OFFSET);
            }
        });
    }

    @Override
    public int getItemCount() {

        if (!mPlaylist.isLoaded()) {
            Log.v(TAG, "playlistCount: 0. Not loaded");
            return 0;
        }

        int playlistCount = mPlaylist == null ? 0 : mPlaylist.size();
        int countAdjusted = playlistCount-PLAYLIST_OFFSET;

        Log.v(TAG, "playlistCount: " + countAdjusted);

        return countAdjusted;
    }

    @Override
    public void onViewRecycled(PlaylistViewHolder viewHolder) {
        Log.v("PlaylistAdapter", "onViewRecycled");

        if (viewHolder == null)
            return;

        if (viewHolder.getEpisode() == null) {
            return;
        }

        viewHolder.mPlayPauseButton.unsetEpisodeId();
        viewHolder.downloadButton.unsetEpisodeId();
    }

    /**
     1.) Observable is a Class and Observer is an Interface
     2.) Observable class maintain a list of observers
     3.) When an Observable object is updated it invokes the update() method of each of its observers to notify that, it is changed

     * @param observer
     */
    @Override
    public void registerAdapterDataObserver (RecyclerView.AdapterDataObserver observer) {
        Log.v("PlaylistAdapter", "registerAdapterDataObserver");
        super.registerAdapterDataObserver(observer);
    }

    @Override
    public void unregisterAdapterDataObserver (RecyclerView.AdapterDataObserver observer) {
        Log.v("PlaylistAdapter", "unregisterAdapterDataObserver");
        super.unregisterAdapterDataObserver(observer);
    }

    // http://stackoverflow.com/questions/5300962/getviewtypecount-and-getitemviewtype-methods-of-arrayadapter
	@Override
    public @EpisodeViewState int getItemViewType(int position) {
		return TYPE_COLLAPSE;
	}

	/**
	 * Returns the ID of the item at the position
	 * 
	 * @param position
	 * @return ID of the FeedItem
	 */
	private Long itemID(int position) {
        Log.d("PlaylistAdapter", "itemID");

        IEpisode episode = mPlaylist.getItem(position);

        if (episode == null)
            return -1L;

        URL url = episode.getUrl();

        if (url == null)
            return -1L;

        return (long)url.hashCode();
	}

    @Override
    public long getItemId (int position) {
        return itemID(position);
    }

    public void toggle(PlaylistViewHolder pvh) {
        keepOne.toggle(pvh);
    }

    private void bindDuration(@NonNull PlaylistViewHolder argHolder, @NonNull IEpisode argFeedItem) {
        String strDuration = "";

        long duration = argFeedItem.getDuration();
        if (duration >= 0) {
            strDuration = StrUtils.formatTime(duration);
        }

        argHolder.mTimeDuration.setText(strDuration);
    }
}
