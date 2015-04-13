package org.bottiger.podcast.adapters;

import java.util.HashMap;
import java.util.TreeSet;
import java.util.concurrent.locks.ReentrantLock;

import org.bottiger.podcast.MainActivity;
import org.bottiger.podcast.PodcastBaseFragment;
import org.bottiger.podcast.R;

import org.bottiger.podcast.adapters.viewholders.ExpandableViewHoldersUtil;
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
import android.graphics.drawable.Animatable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.controller.BaseControllerListener;
import com.facebook.drawee.controller.ControllerListener;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.imagepipeline.image.ImageInfo;
import com.facebook.imagepipeline.image.QualityInfo;

public class PlaylistAdapter extends AbstractPodcastAdapter<PlaylistViewHolder> {

    private static final String TAG = "PlaylistAdapter";

    public static final int TYPE_EXPAND = 1;
	public static final int TYPE_COLLAPSE = 2;

    public static final int PLAYLIST_OFFSET = 1;

    public static ExpandableViewHoldersUtil.KeepOneH<PlaylistViewHolder> keepOne = new ExpandableViewHoldersUtil.KeepOneH<>();

    private View mOverlay;

    private DownloadProgressObservable mDownloadProgressObservable = null;

	public static TreeSet<Number> mExpandedItemID = new TreeSet<Number>();


	private static DownloadManager mDownloadManager = null;

    private final ReentrantLock mLock = new ReentrantLock();
    StringBuilder mStringBuilder = new StringBuilder();

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

    private final int[] mGradientColors = new int[] {0,0xDD000000};
    private GradientDrawable mActionBarGradientDrawable = new GradientDrawable(
            GradientDrawable.Orientation.BOTTOM_TOP, mGradientColors);

    @Override
    public void onBindViewHolder(final PlaylistViewHolder viewHolder, final int position) {
        Log.v("PlaylistAdapter", "onBindViewHolder(pos: " + position + ")");

        final FeedItem item = mPlaylist.getItem(position+PLAYLIST_OFFSET);

        if (item == null) {
            // This should only happen if the playlist only contain 1 item
            return;
        }

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
                viewHolder.mSubTitle.setText(getSubTitle(mActivity, item));
                bindDuration(viewHolder, item);

                if (Build.VERSION.SDK_INT >= 16) {
                    viewHolder.mActionBarGradientView.setBackground(mActionBarGradientDrawable);
                }

                viewHolder.mPlayPauseButton.setEpisodeId(item.getId(), PlayPauseImageView.LOCATION.PLAYLIST);
                viewHolder.mPlayPauseButton.setStatus(PlayerStatusObservable.STATUS.PAUSED);
                mDownloadProgressObservable.registerObserver(viewHolder.downloadButton);

                String imageUrl = item.getImageURL(mActivity);
                if (!TextUtils.isEmpty(imageUrl)) {
                    viewHolder.mItemBackground.setPaletteKey(imageUrl);
                }

                if (mDownloadManager == null) {
                    mDownloadManager = (DownloadManager) mActivity
                            .getSystemService(Context.DOWNLOAD_SERVICE);
                }

                if (item.sub_title != null) {

                    int stringLength = 150;
                    String displayString = item.content.length() < stringLength ? item.content : item.content.substring(0, stringLength);
                    viewHolder.mSubTitle.setText(displayString);
                }


                // http://frescolib.org/docs/getting-started.html#_
                if (!TextUtils.isEmpty(item.image)) {
                    ControllerListener controllerListener = new BaseControllerListener<ImageInfo>() {
                        @Override
                        public void onFinalImageSet(
                                String id,
                                @Nullable ImageInfo imageInfo,
                                @Nullable Animatable anim) {
                            if (imageInfo == null) {
                                return;
                            }
                            QualityInfo qualityInfo = imageInfo.getQualityInfo();
                            Log.d(TAG, "Final image received! "
                                            + "Size %d x %d"
                                            + "Quality level %d, good enough: %s, full quality: %s"
                                            + imageInfo.getWidth()
                                            + imageInfo.getHeight()
                                            + qualityInfo.getQuality()
                                            + qualityInfo.isOfGoodEnoughQuality()
                                            + qualityInfo.isOfFullQuality());



                            // legacy stuff :)
                            String url = item.image;
                            mLock.lock();
                            try {
                                Drawable d = viewHolder.mItemBackground.getDrawable();

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
                        public void onIntermediateImageSet(String id, @Nullable ImageInfo imageInfo) {
                            Log.d(TAG, "Intermediate image received");
                        }

                        @Override
                        public void onFailure(String id, Throwable throwable) {
                            Log.e(TAG, "Error loading %s" + id);
                        }
                    };

                    Uri frescoImageUrl = Uri.parse(item.image);

                    DraweeController controller = Fresco.newDraweeControllerBuilder()
                            .setControllerListener(controllerListener)
                            .setUri(frescoImageUrl).build();

                    viewHolder.mItemBackground.setController(controller);
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
            View v = viewHolder.playerRelativeLayout;
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
                                  final PlaylistViewHolder holder, final int position) {
        Log.v("PlaylistAdapter", "bindExandedPlayer");

        ThemeHelper themeHelper = new ThemeHelper(context);

        holder.playerRelativeLayout.setVisibility(View.VISIBLE);
        holder.timeSlash.setText("/");
        holder.timeSlash.setVisibility(View.VISIBLE);

        long playerPosition = 0;
        long playerDuration = 0;

        if (MainActivity.sBoundPlayerService != null) {
            if (position == 0
                    && MainActivity.sBoundPlayerService.isPlaying()) {
                playerPosition = MainActivity.sBoundPlayerService
                        .position();
                playerDuration = MainActivity.sBoundPlayerService
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
        holder.removeButton.setEpisodeId(id);
        holder.downloadButton.setEpisode(feedItem);


        holder.removeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PlaylistAdapter.toggle(holder, position);
                feedItem.removeFromPlaylist(context.getContentResolver());
                PlaylistAdapter.this.notifyItemRemoved(position);
                mPlaylist.removeItem(position+PLAYLIST_OFFSET);
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

        if (MainActivity.sBoundPlayerService != null) {
            boolean isPlaying = false;
            if (MainActivity.sBoundPlayerService.isInitialized()) {
                if (feedItem.getId() == MainActivity.sBoundPlayerService
                        .getCurrentItem().id) {
                    if (MainActivity.sBoundPlayerService.isPlaying()) {
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

        PlaylistViewHolder holder = viewHolder;

        if (holder.episode == null) {
            return;
        }

        mDownloadProgressObservable.unregisterObserver(holder.downloadButton);


        holder.mPlayPauseButton.unsetEpisodeId();
        holder.favoriteButton.unsetEpisodeId();
        holder.removeButton.unsetEpisodeId();
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

    private String getSubTitle(@NonNull Context argContext, @NonNull FeedItem argFeedItem) {
        mStringBuilder = new StringBuilder();
        boolean needSeparator = false;

        String date = argFeedItem.getDate(argContext);

        if (!TextUtils.isEmpty(date)) {
            mStringBuilder.append(date);
            needSeparator = true;
        }

        return mStringBuilder.toString();
    }

    private void bindDuration(@NonNull PlaylistViewHolder argHolder, @NonNull FeedItem argFeedItem) {

        int visibility = View.INVISIBLE;
        String strDuration = "";

        long duration = argFeedItem.getDuration();
        if (duration > 0) {
            strDuration = StrUtils.formatTime(duration);
            visibility = View.VISIBLE;
        }

        argHolder.mTimeDuration.setText(strDuration);
        argHolder.mTimeDurationIcon.setVisibility(visibility);
    }
}
