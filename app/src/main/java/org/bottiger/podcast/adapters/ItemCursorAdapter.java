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
import org.bottiger.podcast.views.PlaylistViewHolder;
import org.bottiger.podcast.views.utils.PlaylistViewHolderExpanderHelper;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.squareup.picasso.Callback;

public class ItemCursorAdapter extends AbstractEpisodeCursorAdapter<PlaylistViewHolder> implements Playlist.PlaylistChangeListener {

    public static final int TYPE_FIRST = 0;
    public static final int TYPE_EXPAND = 1;
	public static final int TYPE_COLLAPS = 2;
	private static final int TYPE_MAX_COUNT = 3;

    public static final int PLAYLIST_OFFSET = 1;

    public static int mCollapsedHeight = -1;
    private static int mExpandedHeight = -1; //890;

    public static ExpandableViewHoldersUtil.KeepOneH<PlaylistViewHolder> keepOne = new ExpandableViewHoldersUtil.KeepOneH<PlaylistViewHolder>();

    private View mOverlay;

	private PodcastBaseFragment mFragment = null;
    private DownloadProgressObservable mDownloadProgressObservable = null;

	public static TreeSet<Number> mExpandedItemID = new TreeSet<Number>();


	private static DownloadManager mDownloadManager = null;
    private static Playlist mPlaylist = null;

    private PlaylistViewHolderExpanderHelper mExpanderHelper;

    private final ReentrantLock mLock = new ReentrantLock();


    // memory leak!!!!
    private static HashMap<String, Drawable> mBitmapCache = new HashMap<String, Drawable>();

    private BackgroundTransformation mImageTransformation;

    public ItemCursorAdapter(Context context, PodcastBaseFragment fragment, View argOverlay, Playlist argPlaylist, Cursor dataset, DownloadProgressObservable argDownloadProgressObservable) {
        super(dataset);
        mContext = context;
        mFragment = fragment;
        mOverlay = argOverlay;
        mPlaylist = argPlaylist;
        mInflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mDownloadProgressObservable = argDownloadProgressObservable;

        setDataset(dataset);
        Playlist.setActivePlaylist(mPlaylist);
        mPlaylist.registerPlaylistChangeListener(this);

        mExpanderHelper = new PlaylistViewHolderExpanderHelper(mContext, this, mExpandedHeight);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View view = null;
        view = mInflater.inflate(R.layout.episode_list, viewGroup, false);
        PlaylistViewHolder holder = null;

        holder = new PlaylistViewHolder(view);

        if (mCollapsedHeight < 0) {
            mCollapsedHeight = holder.mLayout.getLayoutParams().height;
        }

        return holder;
    }


    private MotionEvent fingerDown = null;
    private float fingerdownx = -1;
    private float fingerdowny = -1;
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, final int position) {
        final PlaylistViewHolder playlistViewHolder2 = (PlaylistViewHolder)viewHolder;
        final FeedItem item = mPlaylist.getItem(position+PLAYLIST_OFFSET);
        boolean isPlaying = false;
        keepOne.bind(playlistViewHolder2, position);

        Log.d("ExpanderHelper", "pos: " + position + " episode: " + item.getTitle());

        playlistViewHolder2.mItemBackground.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                keepOne.toggle(playlistViewHolder2);
            }
        });


        playlistViewHolder2.mItemBackground.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN){
                    Rect viewRect = new Rect();
                    playlistViewHolder2.mPlayPauseButton.getHitRect(viewRect);
                    if (viewRect.contains((int)event.getX(), (int)event.getY())) {
                        playlistViewHolder2.mPlayPauseButton.onClick(null);
                    } else {
                        fingerDown = event;
                        fingerdownx = event.getRawX();
                        fingerdowny = event.getRawY();

                    }
                }

                if (event.getAction() == MotionEvent.ACTION_UP){
                    if (fingerDown != null) {
                        float thresshold = 10;
                        float diffx = Math.abs(fingerdownx-event.getRawX());
                        float diffy = Math.abs(fingerdowny-event.getRawY());
                        if (diffx < thresshold && diffy < thresshold) {
                           //playlistViewHolder2.onClick(playlistViewHolder2.mLayout, playlistViewHolder2);
                           keepOne.toggle(playlistViewHolder2);
                            fingerDown=null;
                        }
                    }
                }

                if (event.getAction() == MotionEvent.ACTION_CANCEL || event.getAction() == MotionEvent.ACTION_HOVER_EXIT){
                    fingerDown=null;
                }

                return true;
            }
        });


        PaletteObservable.registerListener(playlistViewHolder2.mPlayPauseButton);

        playlistViewHolder2.playerLinearLayout.setVisibility(View.GONE);

        int type = getTrueItemViewType(position);
        //type = TYPE_FIRST;
        boolean doExpand = type == TYPE_EXPAND; //  || position < 5

        try {

            playlistViewHolder2.episode = item;
            playlistViewHolder2.mAdapter = this;

            // The first item should never be recycled
            /*
            if (position == 0 && !playlistViewHolder2.isRecyclable()) {
                playlistViewHolder2.setIsRecyclable(false);
            }*/

            if (PodcastBaseFragment.mPlayerServiceBinder != null && PodcastBaseFragment.mPlayerServiceBinder.isInitialized()) {
                if (item.getId() == PodcastBaseFragment.mPlayerServiceBinder
                        .getCurrentItem().id) {
                    if (PodcastBaseFragment.mPlayerServiceBinder.isPlaying()) {
                        isPlaying = true;
                    }
                }
            }


            if (doExpand) {
                mExpanderHelper.expand(playlistViewHolder2, false, false);
            } else if (type == TYPE_COLLAPS) {
                mExpanderHelper.collapse(playlistViewHolder2, false);
            }

            if (item != null) {

                playlistViewHolder2.mPlayPauseButton.setEpisodeId(item.getId());
                playlistViewHolder2.mPlayPauseButton.setStatus(isPlaying ? PlayerStatusObservable.STATUS.PLAYING : PlayerStatusObservable.STATUS.PAUSED);
                mDownloadProgressObservable.registerObserver(playlistViewHolder2.mPlayPauseButton);

                if (mDownloadManager == null) {
                    mDownloadManager = (DownloadManager) mContext
                            .getSystemService(Context.DOWNLOAD_SERVICE);
                }

                if (item.sub_title != null) {

                    int stringLength = 150;
                    String displayString = item.content.length() < stringLength ? item.content : item.content.substring(0, stringLength);
                    playlistViewHolder2.mSubTitle.setText(displayString);
                }

                playlistViewHolder2.mPicassoCallback = new Callback() {

                    @Override
                    public void onSuccess() {
                        String url = item.image;
                        mLock.lock();
                        try {
                            Drawable d = playlistViewHolder2.mItemBackground.getDrawable();
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
                        playlistViewHolder2.mItemBackground.setImageDrawable(mBitmapCache.get(ii));
                    } else {

                        playlistViewHolder2.mItemBackground.setImageResource(0);

                        int h = 1080;
                        //com.squareup.picasso.Transformation trans = BackgroundTransformation.getmImageTransformation(mContext, mImageTransformation, playlistViewHolder2.mItemBackground);
                        com.squareup.picasso.Transformation trans = BackgroundTransformation.getmImageTransformation(mContext, mImageTransformation, h);
                        PicassoWrapper.load(mContext, ii, playlistViewHolder2.mItemBackground, trans, playlistViewHolder2.mPicassoCallback); // playlistViewHolder2.mItemBackground
                    }

                }

            }

        } catch (IllegalStateException e) {
        }

        bindExandedPlayer(mContext, item, playlistViewHolder2, position);

        Palette palette = PaletteCache.get(item.image);
        if (palette != null) {
            PaletteObservable.updatePalette(item.image, palette);
        }

        if (!doExpand) {
            View v = playlistViewHolder2.playerLinearLayout;
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

        long id = feedItem.getId();
        holder.mPlayPauseButton.setEpisodeId(id);
        holder.downloadButton.setEpisodeId(id);
        holder.favoriteButton.setEpisodeId(id);
        holder.previousButton.setEpisodeId(id);
        holder.downloadButton.setEpisode(feedItem);



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
        int cursorCount = mCursor == null ? 0 : mCursor.getCount();
        int playlistCount = mPlaylist == null ? 0 : mPlaylist.size();
        int minCount = Math.min(cursorCount, playlistCount);

        return minCount-PLAYLIST_OFFSET;
    }

    @Override
    public void onViewRecycled(RecyclerView.ViewHolder viewHolder) {

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
        super.registerAdapterDataObserver(observer);
    }

    @Override
    public void unregisterAdapterDataObserver (RecyclerView.AdapterDataObserver observer) {
        super.unregisterAdapterDataObserver(observer);
    }

    public PlaylistViewHolderExpanderHelper getExpanderHelper() {
        return mExpanderHelper;
    }

	public void showItem(Long id) {
		if (!mExpandedItemID.isEmpty())
			mExpandedItemID.remove(mExpandedItemID.first()); // only show
														     // one expanded
															 // at the time
		mExpandedItemID.add(id);
	}

	public int toggleItem(Long id) {
		if (mExpandedItemID.contains(id)) {
            mExpandedItemID.remove(id);
            return TYPE_COLLAPS;
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
		return isExpanded ? TYPE_EXPAND : TYPE_COLLAPS;
        //return TYPE_COLLAPS;
	}

    public int getTrueItemViewType(int position) {
        Long id = itemID(position+PLAYLIST_OFFSET); // The recyclervies does not start with item 1 in the playlist
        boolean isExpanded = mExpandedItemID.contains(id);
        return isExpanded ? TYPE_EXPAND : TYPE_COLLAPS;
    }

	/**
	 * Returns the ID of the item at the position
	 * 
	 * @param position
	 * @return ID of the FeedItem
	 */
	private Long itemID(int position) {
        FeedItem episode = mPlaylist.getItem(position);

        if (episode == null)
            return -1L;

        Long id = episode.getId(); //Long.valueOf(((ReorderCursor) mCursor).getInt(mCursor.getColumnIndex(BaseColumns._ID)));
        return id;
	}



    @Override
    public void notifyPlaylistChanged() {
        ((Activity)mOverlay.getContext()).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ItemCursorAdapter.this.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void notifyPlaylistRangeChanged(int from, int to) {
        final int tmpmin = Math.min(from, to);
        final int min = tmpmin > this.getItemCount() ? this.getItemCount() : tmpmin;

        final int tmpcount = 1 + Math.max(from, to) - min;
        final int count = tmpcount > this.getItemCount() ? this.getItemCount() : tmpcount;
        try {
            ((Activity)mOverlay.getContext()).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ItemCursorAdapter.this.notifyItemRangeChanged(min, count);
                }
            });
        } catch (IndexOutOfBoundsException iob) {
            int count2 = 5;
            int min2 = count;
            return;
        }
    }
}
