package org.bottiger.podcast.adapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.BitmapImageViewTarget;

import org.bottiger.podcast.R;
import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.adapters.viewholders.ExpandableViewHoldersUtil;
import org.bottiger.podcast.listeners.PaletteListener;
import org.bottiger.podcast.player.exoplayer.ExoPlayerWrapper;
import org.bottiger.podcast.playlist.Playlist;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.provider.ISubscription;
import org.bottiger.podcast.service.PlayerService;
import org.bottiger.podcast.utils.ColorExtractor;
import org.bottiger.podcast.utils.ColorUtils;
import org.bottiger.podcast.utils.PaletteHelper;
import org.bottiger.podcast.utils.SharedAdapterUtils;
import org.bottiger.podcast.utils.StrUtils;
import org.bottiger.podcast.utils.ThemeHelper;
import org.bottiger.podcast.views.PlayPauseImageView;
import org.bottiger.podcast.views.PlaylistViewHolder;

import java.net.URL;
import java.util.Locale;
import java.util.TreeSet;

import static org.bottiger.podcast.player.SoundWavesPlayerBase.STATE_IDLE;

public class PlaylistAdapter extends AbstractPodcastAdapter<PlaylistViewHolder> {

    private static final String TAG = "PlaylistAdapter";

    public static final int TYPE_EXPAND = 1;
	public static final int TYPE_COLLAPSE = 2;

    public static final int PLAYLIST_OFFSET = 1;

    public static ExpandableViewHoldersUtil.KeepOneH<PlaylistViewHolder> keepOne = new ExpandableViewHoldersUtil.KeepOneH<>();

    private Activity mActivity;
    private View mOverlay;

	public static TreeSet<Number> mExpandedItemID = new TreeSet<>();

    public PlaylistAdapter(@NonNull Activity argActivity, View argOverlay) {
        super(argActivity);
        mActivity = argActivity;
        mOverlay = argOverlay;
        mInflater = (LayoutInflater) mActivity
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        mPlaylist = SoundWaves.getAppContext(argActivity).getPlaylist();
        //notifyDataSetChanged();
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
        final Activity activity = mActivity;

        if (item == null) {
            // This should only happen if the playlist only contain 1 item
            return;
        }

        Context context = SoundWaves.getAppContext(mActivity);
        int textColor = item.isMarkedAsListened() ? ColorUtils.getTextColor(context) : ColorUtils.getFadedTextColor(context);

        SharedAdapterUtils.AddPaddingToLastElement(viewHolder.mLayout, 0, dataPosition == getItemCount()-1);

        viewHolder.setArtwork(null);
        ISubscription subscription = item.getSubscription(context);

        String image = item.getArtwork(mActivity);
        if (StrUtils.isValidUrl(image)) {
            Glide.with(mActivity).load(image).asBitmap().centerCrop().into(new BitmapImageViewTarget(viewHolder.mPodcastImage) {
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

        String artwork = item.getArtwork(mActivity);
        PaletteHelper.generate(subscription, activity, viewHolder.mPlayPauseButton);
        PaletteHelper.generate(subscription, activity, new PaletteListener() {
            @Override
            public void onPaletteFound(Palette argChangedPalette) {
                ColorExtractor colorExtractor = new ColorExtractor(mActivity, argChangedPalette);
                viewHolder.setEpisodePrimaryColor(colorExtractor.getPrimary());
            }

            @Override
            public String getPaletteUrl() {
                return null;
            }
        });

        viewHolder.episode = item;
        viewHolder.mAdapter = this;

        viewHolder.mMainTitle.setText(item.getTitle());
        viewHolder.mMainTitle.setTextColor(textColor);

        if (item.getSubscription(mActivity) != null)
            viewHolder.mSecondaryTitle.setText(item.getSubscription(mActivity).getTitle());

        viewHolder.description.setText(item.getDescription());
        bindDuration(viewHolder, item);

        if (item.getPriority() > 0) {
            viewHolder.mPlaylistPosition.setText(String.format(Locale.getDefault(), "%d", dataPosition+1));
            viewHolder.mPlaylistPosition.setVisibility(View.VISIBLE);
        } else {
            viewHolder.mPlaylistPosition.setVisibility(View.GONE);
        }

        viewHolder.mPlayPauseButton.setEpisode(item, PlayPauseImageView.PLAYLIST);
        viewHolder.mPlayPauseButton.setStatus(STATE_IDLE);

        viewHolder.downloadButton.setEpisode(item);

        // needs the downloadButton to be bound in advance
        keepOne.bind(viewHolder, dataPosition);


        viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                                                   @Override
                                                   public void onClick(View v) {
                                                       PlaylistAdapter.toggle(viewHolder, dataPosition);
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
    public void bindExandedPlayer(final Context context, final IEpisode feedItem,
                                  final PlaylistViewHolder holder, final int position) {
        Log.v("PlaylistAdapter", "bindExandedPlayer");


        ThemeHelper themeHelper = new ThemeHelper(context);

        long playerPosition = 0;
        long playerDuration = 0;

        PlayerService ps = PlayerService.getInstance();
        if (ps != null) {
            if (position == 0
                    && ps.isPlaying()) {
                playerPosition = ps
                        .position();
                playerDuration = ps
                        .duration();
            } else {
                if (feedItem instanceof FeedItem) {
                    playerPosition = ((FeedItem)feedItem).offset;
                }
                playerDuration = 0;//feedItem.getDuration();
            }
        }

        holder.currentTime.setText(StrUtils.formatTime(playerPosition));

        holder.seekbar.setEpisode(feedItem);
        holder.seekbar.setOverlay(mOverlay);

        holder.mPlayPauseButton.setEpisode(feedItem, PlayPauseImageView.PLAYLIST);
        holder.downloadButton.setEpisode(feedItem);
        holder.removeButton.setEpisode(feedItem);
        holder.downloadButton.setEpisode(feedItem);

        ISubscription subscription = feedItem.getSubscription(context);
        PaletteHelper.generate(subscription, mActivity, holder.downloadButton);
        PaletteHelper.generate(subscription, mActivity, holder.removeButton);


        holder.removeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PlaylistAdapter.toggle(holder, position);

                if (feedItem instanceof FeedItem) { // FIXME
                    ((FeedItem) feedItem).removeFromPlaylist(context.getContentResolver());
                }

                notifyDataSetChanged();
                mPlaylist.removeItem(position + PLAYLIST_OFFSET);
            }
        });
    }

    @Override
    public int getItemCount() {
        Log.v("PlaylistAdapter", "getItemCount");
        int playlistCount = mPlaylist == null ? 0 : mPlaylist.size();

        return playlistCount-PLAYLIST_OFFSET;
    }

    @Override
    public void onViewRecycled(PlaylistViewHolder viewHolder) {
        Log.v("PlaylistAdapter", "onViewRecycled");

        if (viewHolder == null)
            return;

        PlaylistViewHolder holder = viewHolder;

        if (holder.episode == null) {
            return;
        }

        holder.mPlayPauseButton.unsetEpisodeId();
        holder.removeButton.unsetEpisodeId();
        holder.downloadButton.unsetEpisodeId();
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

	public static void showItem(Long id) {
        Log.v("PlaylistAdapter", "showItem");
		if (!mExpandedItemID.isEmpty())
			mExpandedItemID.remove(mExpandedItemID.first()); // only show
														     // one expanded
															 // at the time
		mExpandedItemID.add(id);
	}

	public static int toggleItem(Long id) {
        Log.v("PlaylistAdapter", "toggleItem");
		if (mExpandedItemID.contains(id)) {
            mExpandedItemID.remove(id);
            return TYPE_COLLAPSE;
        } else {
			showItem(id);
            return TYPE_EXPAND;
		}
	}

    // http://stackoverflow.com/questions/5300962/getviewtypecount-and-getitemviewtype-methods-of-arrayadapter
	@Override
    public int getItemViewType(int position) {

        Long id = itemID(position+PLAYLIST_OFFSET); // The recyclervies does not start with item 1 in the playlist
		boolean isExpanded = mExpandedItemID.contains(id);

        Log.v("PlaylistAdapter", "getItemViewType: " + isExpanded);
		return isExpanded ? TYPE_EXPAND : TYPE_COLLAPSE;
	}

	/**
	 * Returns the ID of the item at the position
	 * 
	 * @param position
	 * @return ID of the FeedItem
	 */
	private static Long itemID(int position) {
        Log.v("PlaylistAdapter", "itemID");

        PlayerService ps = PlayerService.getInstance();
        if (ps == null)
            return -1L;

        Playlist playlist = ps.getPlaylist();
        IEpisode episode = playlist.getItem(position);

        if (episode == null)
            return -1L;

        URL url = episode.getUrl();

        if (url == null)
            return -1L;

        return (long)url.hashCode();
	}

    private void playlistChanged(@NonNull Playlist argPlaylist) {
        mPlaylist = argPlaylist;
        notifyDataSetChanged();
    }

    @Override
    public long getItemId (int position) {
        return itemID(position);
    }

    public static void toggle(PlaylistViewHolder pvh, int pos) {
        keepOne.toggle(pvh);
    }

    private void bindDuration(@NonNull PlaylistViewHolder argHolder, @NonNull IEpisode argFeedItem) {

        int visibility = View.INVISIBLE;
        String strDuration = "";

        long duration = argFeedItem.getDuration();
        if (duration > 0) {
            strDuration = StrUtils.formatTime(duration);
            visibility = View.VISIBLE;
        }

        argHolder.mTimeDuration.setText(strDuration);
        //argHolder.mTimeDurationIcon.setVisibility(visibility);
    }
}
