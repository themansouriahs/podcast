package org.bottiger.podcast.adapters;

import java.util.HashMap;
import java.util.TreeSet;
import java.util.concurrent.locks.ReentrantLock;

import org.bottiger.podcast.ApplicationConfiguration;
import org.bottiger.podcast.PodcastBaseFragment;
import org.bottiger.podcast.R;

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
import org.bottiger.podcast.views.PlayerLinearLayout;
import org.bottiger.podcast.views.PlaylistViewHolder;
import org.bottiger.podcast.views.RelativeLayoutWithBackground;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.DownloadManager;
import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.squareup.picasso.Callback;

public class ItemCursorAdapter extends AbstractEpisodeCursorAdapter<PlaylistViewHolder> implements Playlist.PlaylistChangeListener {

    public static final int TYPE_FIRST = 0;
    public static final int TYPE_EXPAND = 1;
	public static final int TYPE_COLLAPS = 2;
	private static final int TYPE_MAX_COUNT = 3;

    private static final int PLAYLIST_OFFSET = 1;

    private static int mCollapsedHeight = -1;
    private static int mExpandedHeight = 890;

    private View mOverlay;

	private PodcastBaseFragment mFragment = null;
    private DownloadProgressObservable mDownloadProgressObservable = null;

	public static TreeSet<Number> mExpandedItemID = new TreeSet<Number>();
    public static PlaylistViewHolder expandedView = null;
    public static RelativeLayoutWithBackground expandedLayout = null;

	private static DownloadManager mDownloadManager = null;
    private static Playlist mPlaylist = null;

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
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View view = mInflater.inflate(R.layout.episode_list, viewGroup, false);
        PlaylistViewHolder holder = null;

        holder = new PlaylistViewHolder(view);

        if (mCollapsedHeight < 0) {
            mCollapsedHeight = holder.mLayout.getLayoutParams().height;
        }

        return holder;
    }


    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, final int position) {
        Log.d("transformation", "pos: " + position);
        final PlaylistViewHolder playlistViewHolder2 = (PlaylistViewHolder)viewHolder;
        final FeedItem item = mPlaylist.getItem(position+PLAYLIST_OFFSET);
        boolean isPlaying = false;

        playlistViewHolder2.mItemBackground.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playlistViewHolder2.onClick(playlistViewHolder2.mLayout, playlistViewHolder2);
            }
        });


        PaletteObservable.registerListener(playlistViewHolder2.mPlayPauseButton);

        playlistViewHolder2.playerLinearLayout.setVisibility(View.GONE);

        int type = getItemViewType(position+PLAYLIST_OFFSET);
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
                expand(playlistViewHolder2, false, false);
            } else if (type == TYPE_COLLAPS) {
                collapse(playlistViewHolder2, false);
            }

            if (item != null) {

                playlistViewHolder2.mPlayPauseButton.setmEpisodeId(item.getId());
                playlistViewHolder2.mPlayPauseButton.setStatus(isPlaying ? PlayerStatusObservable.STATUS.PLAYING : PlayerStatusObservable.STATUS.PAUSED);
                mDownloadProgressObservable.registerObserver(playlistViewHolder2.mPlayPauseButton);

                if (mDownloadManager == null) {
                    mDownloadManager = (DownloadManager) mContext
                            .getSystemService(Context.DOWNLOAD_SERVICE);
                }


                if (item.title != null) {
                    String title = item.title;
                    int priority = item.getPriority();
                    long lastUpdate = item.getLastUpdate();

                    String preTitle = "";

                    if (item.isListened() && ApplicationConfiguration.DEBUGGING) {
                        preTitle = "L";
                    }


                    if (ApplicationConfiguration.DEBUGGING)
                        preTitle = preTitle+ "p:" + priority + " t:" + lastUpdate;
                    else
                        preTitle = preTitle + String.valueOf(priority);

                    if (priority > 0 || ApplicationConfiguration.DEBUGGING) {
                        title = preTitle + " # " + title;
                    }

                    playlistViewHolder2.mMainTitle.setText(title);
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
                playerDuration = feedItem.getDuration();
            }
        }

        holder.currentTime.setText(StrUtils.formatTime(playerPosition));

        //holder.downloadButton.registerListener(paletteObservable);

        holder.seekbar.setEpisode(feedItem);
        holder.seekbar.setOverlay(mOverlay);

        long id = feedItem.getId();
        holder.mPlayPauseButton.setmEpisodeId(id);
        holder.downloadButton.setEpisodeId(id);
        holder.queueButton.setEpisodeId(id);
        holder.bookmarkButton.setEpisodeId(id);
        holder.previousButton.setEpisodeId(id);
        holder.downloadButton.setEpisode(feedItem);


        mDownloadProgressObservable.registerObserver(holder.downloadButton);

        PaletteObservable.registerListener(holder.mPlayPauseButton);
        PaletteObservable.registerListener(holder.downloadButton);
        PaletteObservable.registerListener(holder.queueButton);
        PaletteObservable.registerListener(holder.bookmarkButton);
        PaletteObservable.registerListener(holder.previousButton);
        PaletteObservable.registerListener(holder.seekbar);

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
        return mCursor == null ? 0 : mCursor.getCount()-PLAYLIST_OFFSET;
    }

    @Override
    public void onViewRecycled(RecyclerView.ViewHolder viewHolder) {

        if (viewHolder == null)
            return;

        PlaylistViewHolder holder = (PlaylistViewHolder) viewHolder;

        if (holder.episode == null) {
            return;
        }

        //PlayerStatusObservable.unregisterListener(holder.seekbar);

        mDownloadProgressObservable.unregisterObserver(holder.mPlayPauseButton);


        PaletteObservable.unregisterListener(holder.mPlayPauseButton);
        PaletteObservable.unregisterListener(holder.previousButton);
        PaletteObservable.unregisterListener(holder.downloadButton);
        PaletteObservable.unregisterListener(holder.queueButton);
        PaletteObservable.unregisterListener(holder.bookmarkButton);
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

	public void showItem(Long id) {
		if (!mExpandedItemID.isEmpty())
			mExpandedItemID.remove(mExpandedItemID.first()); // HACK: only show
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
        boolean isFirst = position == 0;
        if (isFirst) {
            return TYPE_FIRST;
        }
        Long id = itemID(position);
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
        Long id = mPlaylist.getItem(position).getId(); //Long.valueOf(((ReorderCursor) mCursor).getInt(mCursor.getColumnIndex(BaseColumns._ID)));
        return id;
	}

    public void expand(final PlaylistViewHolder viewHolder, final boolean isAnimated, final boolean forceRefresh) {

        View player = viewHolder.playerLinearLayout;
        if (isAnimated) {
            expand(viewHolder);
        } else {

            if (mExpandedHeight < 0) {
                throw new IllegalStateException("mExpandedHeight should not set");
            }

            viewHolder.mLayout.getLayoutParams().height = mExpandedHeight;//imageStartHeight-initialHeight;
            viewHolder.mItemBackground.getLayoutParams().height = mExpandedHeight;

            viewHolder.mForward.setVisibility(View.VISIBLE);
            viewHolder.mBackward.setVisibility(View.VISIBLE);

            player.setVisibility(View.VISIBLE);
            player.measure(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            player.getLayoutParams().height = player.getMeasuredHeight();
            player.requestLayout();

            viewHolder.mLayout.requestLayout();

        }
        //extended_player.requestLayout();
    }

    public void collapse(final PlaylistViewHolder viewHolder, final boolean isAnimated) {
        View player = viewHolder.playerLinearLayout;
        if (isAnimated) {
            collapse(viewHolder);
        } else {

            viewHolder.mBackward.setVisibility(View.INVISIBLE);
            viewHolder.mForward.setVisibility(View.INVISIBLE);

            player.setVisibility(View.GONE);
            viewHolder.mLayout.getLayoutParams().height = mCollapsedHeight;//imageStartHeight-initialHeight;
        }
    }

    private void expand(final PlaylistViewHolder viewHolder) {

        final PlayerLinearLayout player = viewHolder.playerLinearLayout;
        final RelativeLayoutWithBackground layoutWithBackground = viewHolder.mLayout;
        final ImageView layoutBackground = viewHolder.mItemBackground;

        final int imageStartHeight = layoutBackground.getHeight();

        layoutWithBackground.measure(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        int initialMeasuredHeight = layoutWithBackground.getMeasuredHeight();

        player.measure(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        int initialPlayerMeasuredHeight = player.getMeasuredHeight();
        player.requestLayout();
        player.setVisibility(View.VISIBLE);

        viewHolder.mForward.setAlpha(0f);
        viewHolder.mBackward.setAlpha(0f);
        viewHolder.mForward.setVisibility(View.VISIBLE);
        viewHolder.mBackward.setVisibility(View.VISIBLE);

        final int targtetHeight = initialPlayerMeasuredHeight + imageStartHeight; // + initialMeasuredHeight; //player.getMeasuredHeight();
        //extended_player.getLayoutParams().height = 0;

        //layoutWithBackground.getLayoutParams().height = mExpandedHeight;
        layoutBackground.getLayoutParams().height = viewHolder.mMainContainer.getMeasuredHeight();

        final ItemCursorAdapter adapter = this;

        // 1dp/ms
        int duration = (int) (targtetHeight / player.getContext().getResources().getDisplayMetrics().density);

        if (minHeight < 0) {
            minHeight = imageStartHeight;
            maxHeight = targtetHeight; // evt imageStartHeight+initialPlayerMeasureHeig
        }

        class Incrementer {
            private int mHeight = -1;
            public void setHeight(int newHeight) {
                mHeight = newHeight;
            }
            public int getHeight() {return mHeight; }
        }
        final Incrementer inc = new Incrementer();

        ObjectAnimator animator;
        if (minHeight > 0) {
            Log.d("mExpandedHeight", "expand goal => " + maxHeight);
            animator = ObjectAnimator.ofInt(inc, "height", minHeight, maxHeight); // targtetHeight
        } else {
            animator = ObjectAnimator.ofInt(inc, "height", imageStartHeight, initialMeasuredHeight); // targtetHeight
            minHeight = imageStartHeight;
            maxHeight = initialMeasuredHeight;
        }
        animator.setDuration(duration);

        final RecyclerView.LayoutParams paramsT1 = (RecyclerView.LayoutParams)viewHolder.mLayout.getLayoutParams();
        final RelativeLayout.LayoutParams paramsT2 = (RelativeLayout.LayoutParams)viewHolder.mItemBackground.getLayoutParams();

        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                int height = inc.getHeight();
                paramsT1.height = height;
                paramsT2.height = height;

                Log.d("mExpandedHeight", "current height => " + height);
                viewHolder.mLayout.setLayoutParams(paramsT1);
                viewHolder.mItemBackground.setLayoutParams(paramsT2);
       }
        });

        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                mExpandedHeight = maxHeight;
                Log.d("mExpandedHeight", "mExpandedHeight => " + mExpandedHeight);
                adapter.notifyDataSetChanged(); // BUGFIX
            }

            @Override
            public void onAnimationCancel(Animator animator) {
            }

            @Override
            public void onAnimationRepeat(Animator animator) {
            }
        });

        animator.start();

        expandedView = viewHolder;
        expandedLayout = layoutWithBackground;

    }

    private static int minHeight = -1;
    private static int maxHeight = -1;

    private static float dpFromPx(Context context, float px)
    {
        return px / context.getResources().getDisplayMetrics().density;
    }

    private static float pxFromDp(Context context, float dp)
    {
        return dp * context.getResources().getDisplayMetrics().density;
    }

    private static void collapse(final PlaylistViewHolder viewHolder) {
        final PlayerLinearLayout player = viewHolder.playerLinearLayout;
        final RelativeLayoutWithBackground layoutWithBackground = viewHolder.mLayout;
        final RelativeLayout mainContainer = viewHolder.mMainContainer;

        final int initialHeight = layoutWithBackground.getHeight();
        final int targerHeight = mainContainer.getHeight();

        layoutWithBackground.measure(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        // 1dp/ms
        int duration = (int)(initialHeight / player.getContext().getResources().getDisplayMetrics().density);

        class Incrementer {
            private int mHeight = -1;
            public void setHeight(int newHeight) {
                mHeight = newHeight;
            }
            public int getHeight() {return mHeight; }
        }
        final Incrementer inc = new Incrementer();

        //ObjectAnimator animator = ObjectAnimator.ofInt(inc, "height", initialHeight, targerHeight);
        ObjectAnimator animator = ObjectAnimator.ofInt(inc, "height", maxHeight, minHeight);
        animator.setDuration(duration);

        final RecyclerView.LayoutParams paramsT1 = (RecyclerView.LayoutParams)viewHolder.mLayout.getLayoutParams();
        final RelativeLayout.LayoutParams paramsT2 = (RelativeLayout.LayoutParams)viewHolder.mItemBackground.getLayoutParams();


        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                Log.d("extended_player", "new Height:" + inc.getHeight());
                paramsT1.height = inc.getHeight();
                paramsT2.height = inc.getHeight();

                viewHolder.mLayout.setLayoutParams(paramsT1);
                viewHolder.mItemBackground.setLayoutParams(paramsT2);
            }
        });
        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {}

            @Override
            public void onAnimationEnd(Animator animator) {
            }

            @Override
            public void onAnimationCancel(Animator animator) {}

            @Override
            public void onAnimationRepeat(Animator animator) {}
        });
        animator.start();

        expandedView = null;
        expandedLayout = null;

        viewHolder.mBackward.setVisibility(View.INVISIBLE);
        viewHolder.mForward.setVisibility(View.INVISIBLE);
    }

    @Override
    public void notifyPlaylistChanged() {
        this.notifyDataSetChanged();
    }

    @Override
    public void notifyPlaylistRangeChanged(int from, int to) {
        int min = Math.min(from, to);
        int count = 1 + Math.max(from, to) - min;
        this.notifyItemRangeChanged(min, count);
    }
}
