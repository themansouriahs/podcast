package org.bottiger.podcast.adapters;

import java.util.HashMap;
import java.util.TreeSet;
import java.util.concurrent.locks.ReentrantLock;

import org.bottiger.podcast.PodcastBaseFragment;
import org.bottiger.podcast.R;

import org.bottiger.podcast.adapters.viewholders.ExpandableViewHoldersUtil;
import org.bottiger.podcast.images.PicassoWrapper;
import org.bottiger.podcast.listeners.DownloadProgressObservable;
import org.bottiger.podcast.listeners.PaletteObservable;
import org.bottiger.podcast.listeners.PlayerStatusObservable;
import org.bottiger.podcast.playlist.Playlist;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.utils.BackgroundTransformation;
import org.bottiger.podcast.utils.PaletteCache;
import org.bottiger.podcast.utils.StrUtils;
import org.bottiger.podcast.utils.ThemeHelper;
import org.bottiger.podcast.views.PlayPauseImageView;
import org.bottiger.podcast.views.PlaylistViewHolder;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

public class PlaylistAdapter extends AbstractPodcastAdapter<PlaylistViewHolder> {

    public static final int TYPE_EXPAND = 1;
	public static final int TYPE_COLLAPSE = 2;

    public static final int PLAYLIST_OFFSET = 1;

    public static ExpandableViewHoldersUtil.KeepOneH<PlaylistViewHolder> keepOne = new ExpandableViewHoldersUtil.KeepOneH<PlaylistViewHolder>();

    private View mOverlay;

    private DownloadProgressObservable mDownloadProgressObservable = null;

	public static TreeSet<Number> mExpandedItemID = new TreeSet<Number>();


	private static DownloadManager mDownloadManager = null;

    private final ReentrantLock mLock = new ReentrantLock();


    // memory leak!!!!
    private static HashMap<String, Drawable> mBitmapCache = new HashMap<String, Drawable>();

    private BackgroundTransformation mImageTransformation;

    public PlaylistAdapter(@NonNull Activity argActivity, View argOverlay, DownloadProgressObservable argDownloadProgressObservable) {
        super(argActivity);
        mOverlay = argOverlay;
        mInflater = (LayoutInflater) mActivity
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mDownloadProgressObservable = argDownloadProgressObservable;

        notifyDataSetChanged();
    }

    @Override
    public PlaylistViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        Log.v("PlaylistAdapter", "onCreateViewHolder");

        View view = mInflater.inflate(R.layout.episode_list, viewGroup, false);
        PlaylistViewHolder holder = new PlaylistViewHolder(view);

        return holder;
    }

    private final int[] mGradientColors = new int[] {0,0xAA000000};
    private GradientDrawable mActionBarGradientDrawable = new GradientDrawable(
            GradientDrawable.Orientation.BOTTOM_TOP, mGradientColors);

    @Override
    public void onBindViewHolder(final PlaylistViewHolder viewHolder, final int position) {
        Log.v("PlaylistAdapter", "onBindViewHolder(pos: " + position + ")");

        final FeedItem item = mPlaylist.getItem(position+PLAYLIST_OFFSET);
        keepOne.bind(viewHolder, position);

        Log.d("ExpanderHelper", "pos: " + position + " episode: " + item.getTitle());

        PaletteObservable.registerListener(viewHolder.mPlayPauseButton);

        int type = getItemViewType(position);
        boolean doExpand = type == TYPE_EXPAND; //  || position < 5

        try {

            viewHolder.episode = item;
            viewHolder.mAdapter = this;

            if (item != null) {

                viewHolder.mMainTitle.setText(item.getShortTitle());
                if (Build.VERSION.SDK_INT >= 16) {
                    viewHolder.mActionBarGradientView.setBackground(mActionBarGradientDrawable);
                }

                viewHolder.mPlayPauseButton.setEpisodeId(item.getId(), PlayPauseImageView.LOCATION.PLAYLIST);
                viewHolder.mPlayPauseButton.setStatus(PlayerStatusObservable.STATUS.PAUSED);
                mDownloadProgressObservable.registerObserver(viewHolder.mPlayPauseButton);

                viewHolder.mItemBackground.setPaletteKey(item.getImageURL(mActivity));

                if (mDownloadManager == null) {
                    mDownloadManager = (DownloadManager) mActivity
                            .getSystemService(Context.DOWNLOAD_SERVICE);
                }

                if (item.sub_title != null) {

                    int stringLength = 150;
                    String displayString = item.content.length() < stringLength ? item.content : item.content.substring(0, stringLength);
                    viewHolder.mSubTitle.setText(displayString);
                }

                viewHolder.mPicassoCallback = new Callback() {

                    @Override
                    public void onSuccess() {
                        String url = item.image;
                        mLock.lock();
                        try {
                            Drawable d = viewHolder.mItemBackground.getDrawable();
                            mBitmapCache.put(url, d);

                            Palette palette = PaletteCache.get(url);
                            if (palette == null) {
                                BitmapDrawable bd = (BitmapDrawable)d;
                                PaletteCache.generate(url, bd.getBitmap());
                            }
                        } finally {
                            mLock.unlock();
                        }
                    }

                    @Override
                    public void onError() {
                        return;
                    }
                };

                if ((item.image != null && !item.image.equals(""))) {
                    String ii = item.image;
                        //PicassoWrapper.load(mContext, ii, playlistViewHolder2.mPlayPauseButton);

                    if (mBitmapCache.containsKey(ii)) {
                        viewHolder.mItemBackground.setImageDrawable(mBitmapCache.get(ii));
                    } else {

                        viewHolder.mItemBackground.setImageResource(0);

                        int h = 1080;
                        com.squareup.picasso.Transformation trans = BackgroundTransformation.getmImageTransformation(mActivity, mImageTransformation, h);
                        PicassoWrapper.load(mActivity, ii, viewHolder.mItemBackground, trans, viewHolder.mPicassoCallback); // playlistViewHolder2.mItemBackground
                        //PicassoWrapper.load(mActivity, ii, target, trans);
                        //int color = item.getSubscription(mActivity).getPrimaryColor();
                        //viewHolder.mItemBackground.setColorFilter(Color.RED, PorterDuff.Mode.LIGHTEN);
                    }

                }

            }

        } catch (IllegalStateException e) {
        }

        bindExandedPlayer(mActivity, item, viewHolder, position);

        Palette palette = PaletteCache.get(item.image);
        if (palette != null) {
            PaletteObservable.updatePalette(item.image, palette);
        }

        if (!doExpand) {
            View v = viewHolder.playerLinearLayout;
            if (v != null) {
                v.setVisibility(View.GONE);
            }
        }
    }

    /**
     * Expands the StubView and creates the expandable extended_player. This is done for
     * the current playing episode and at most one other episode which the user
     * is interacting with
     *
     * @param holder
     * @param position
     */
    public void bindExandedPlayer(final Context context, final FeedItem feedItem,
                                  final PlaylistViewHolder holder, int position) {
        Log.v("PlaylistAdapter", "bindExandedPlayer");

        ThemeHelper themeHelper = new ThemeHelper(context);

        holder.playerLinearLayout.setVisibility(View.VISIBLE);
        holder.timeSlash.setText("/");
        holder.timeSlash.setVisibility(View.VISIBLE);

        long playerPosition = 0;
        long playerDuration = 0;

        if (PodcastBaseFragment.mPlayerServiceBinder != null) {
            if (position == 0
                    && PodcastBaseFragment.mPlayerServiceBinder.isPlaying()) {
                playerPosition = PodcastBaseFragment.mPlayerServiceBinder
                        .position();
                playerDuration = PodcastBaseFragment.mPlayerServiceBinder
                        .duration();
            } else {
                playerPosition = feedItem.offset;
                playerDuration = 0;//feedItem.getDuration();
            }
        }

        holder.currentTime.setText(StrUtils.formatTime(playerPosition));

        //holder.downloadButton.registerListener(paletteObservable);

        holder.seekbar.setEpisode(feedItem);
        holder.seekbar.setOverlay(mOverlay);

        final long id = feedItem.getId();
        holder.mPlayPauseButton.setEpisodeId(id, PlayPauseImageView.LOCATION.PLAYLIST);
        holder.downloadButton.setEpisodeId(id);
        holder.favoriteButton.setEpisodeId(id);
        holder.previousButton.setEpisodeId(id);
        holder.downloadButton.setEpisode(feedItem);


        holder.previousButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (PodcastBaseFragment.mPlayerServiceBinder != null) {
                    PodcastBaseFragment.mPlayerServiceBinder.getPlayer().rewind(feedItem);
                }
            }
        });

        mDownloadProgressObservable.registerObserver(holder.downloadButton);

        // PlayerActivity.setProgressBar(sb, feedItem);
        long secondary = 0;
        if (feedItem.filesize != 0) {
            secondary = feedItem.isDownloaded() ? feedItem.getCurrentFileSize()
                    : (feedItem.chunkFilesize / feedItem.filesize);
        }

        //RecentItemFragment.setProgressBar(holder.seekbar, playerDuration,
        //        playerPosition, secondary);

        if (PodcastBaseFragment.mPlayerServiceBinder != null) {
            boolean isPlaying = false;
            if (PodcastBaseFragment.mPlayerServiceBinder.isInitialized()) {
                if (feedItem.getId() == PodcastBaseFragment.mPlayerServiceBinder
                        .getCurrentItem().id) {
                    if (PodcastBaseFragment.mPlayerServiceBinder.isPlaying()) {
                        isPlaying = true;
                        PodcastBaseFragment.setCurrentTime(holder.currentTime);
                    }
                }
            }

        }
    }

    @Override
    public int getItemCount() {
        Log.v("PlaylistAdapter", "getItemCount");
        //int cursorCount = mCursor == null ? 0 : mCursor.getCount();
        int playlistCount = mPlaylist == null ? 0 : mPlaylist.size();
        //int minCount = Math.min(cursorCount, playlistCount);

        //return minCount-PLAYLIST_OFFSET;
        return playlistCount-PLAYLIST_OFFSET;
    }

    @Override
    public void onViewRecycled(PlaylistViewHolder viewHolder) {
        Log.v("PlaylistAdapter", "onViewRecycled");

        if (viewHolder == null)
            return;

        PlaylistViewHolder holder = (PlaylistViewHolder) viewHolder;

        if (holder.episode == null) {
            return;
        } else {
            if (!TextUtils.isEmpty(holder.episode.image)) {
                mBitmapCache.remove(holder.episode.image);
            }
        }

        //PlayerStatusObservable.unregisterListener(holder.seekbar);

        mDownloadProgressObservable.unregisterObserver(holder.mPlayPauseButton);


        holder.mPlayPauseButton.unsetEpisodeId();
        holder.favoriteButton.unsetEpisodeId();
        holder.previousButton.unsetEpisodeId();
        holder.downloadButton.unsetEpisodeId();

        PaletteObservable.unregisterListener(holder.seekbar);

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
        Playlist playlist = Playlist.getActivePlaylist();
        FeedItem episode = playlist.getItem(position);

        if (episode == null)
            return -1L;

        Long id = episode.getId(); //Long.valueOf(((ReorderCursor) mCursor).getInt(mCursor.getColumnIndex(BaseColumns._ID)));
        return id;
	}

    @Override
    public long getItemId (int position) {
        return itemID(position);
    }

    public static void toggle(PlaylistViewHolder pvh, int pos) {
        Long id = itemID(pos+PLAYLIST_OFFSET);
        //toggleItem(id);
        keepOne.toggle(pvh);
    }
}
