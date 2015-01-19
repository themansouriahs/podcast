package org.bottiger.podcast;

import org.bottiger.podcast.adapters.ItemCursorAdapter;
import org.bottiger.podcast.images.PicassoWrapper;
import org.bottiger.podcast.listeners.DownloadProgressObservable;
import org.bottiger.podcast.listeners.PaletteListener;
import org.bottiger.podcast.listeners.PaletteObservable;
import org.bottiger.podcast.listeners.PlayerStatusObservable;
import org.bottiger.podcast.listeners.RecentItemsRecyclerListener;
import org.bottiger.podcast.playlist.Playlist;
import org.bottiger.podcast.playlist.PlaylistCursorLoader;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.service.DownloadCompleteCallback;
import org.bottiger.podcast.service.PodcastDownloadManager;
import org.bottiger.podcast.utils.BackgroundTransformation;
import org.bottiger.podcast.utils.ControlButtons;
import org.bottiger.podcast.utils.PaletteCache;
import org.bottiger.podcast.utils.UIUtils;
import org.bottiger.podcast.views.DownloadButtonView;
import org.bottiger.podcast.views.ExpandableLayoutManager;
import org.bottiger.podcast.views.PlayPauseImageView;
import org.bottiger.podcast.views.PlayerButtonView;
import org.bottiger.podcast.views.PlayerSeekbar;
import org.bottiger.podcast.views.SwipeRefreshExpandableLayout;
import org.bottiger.podcast.views.TopPlayer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.graphics.Palette;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.GestureDetector;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.OverScroller;
import android.widget.RelativeLayout;
import android.widget.Scroller;

import com.squareup.picasso.Callback;

public class PlaylistFragment extends GeastureFragment implements
		OnSharedPreferenceChangeListener, Playlist.PlaylistChangeListener, DownloadCompleteCallback
         { // , SwipeRefreshLayout.OnRefreshListener // RecyclerView.OnScrollListener

    private static final int DEFAULT_IMAGE_SIZE = 1080;
    private int maxRecyclerViewOffset = -1;

    private static int CONTEXT_MENU = 0;
	private static Fragment CONTEXT_FRAGMENT = null;

	public final static int PLAYLIST_CONTEXT_MENU = 0;
	public final static int SUBSCRIPTION_CONTEXT_MENU = 1;

    private boolean mHasPhoto = false;
    private static final float PHOTO_ASPECT_RATIO = 1.7777777f;

    private TopPlayer mPhotoContainer;
    private ImageView mPhoto;

    private PlayPauseImageView mPlayPauseButton;
    private PlayerSeekbar mPlayerSeekbar;
    private DownloadButtonView mPlayerDownloadButton;
    private PlayerButtonView mBackButton;
    private PlayerButtonView mDownloadButton;
    private PlayerButtonView mQueueButton;
    private PlayerButtonView mFavoriteButton;

    private int mHeaderTopClearance;
    private int mPhotoHeightPixels;
    private int mHeaderHeightPixels;
    private int mAddScheduleButtonHeightPixels;

    private int mHeaderStartSize = -1;

	private SharedPreferences.OnSharedPreferenceChangeListener spChanged;

	/** ID of the current expanded episode */
	private long mExpandedEpisodeId = -1;
	private String mExpandedEpisodeKey = "currentExpanded";

    DownloadProgressObservable mDownloadProgressObservable = null;

    private PlaylistCursorLoader playlistCursor;

    private Playlist mPlaylist;
    private Activity mActivity;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDownloadProgressObservable = new DownloadProgressObservable(mActivity);
    }

    @Override
    public void onDestroyView() {
        PodcastDownloadManager.resetDownloadProgressObservable();
        super.onDestroyView();
    }

	// Read here:
	// http://developer.android.com/reference/android/app/Fragment.html#Layout
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

        mDownloadProgressObservable = PodcastDownloadManager.getDownloadProgressObservable(mActivity);

		mPlaylist = getPlaylist();
        mPlaylist.registerPlaylistChangeListener(this);
		playlistCursor = new PlaylistCursorLoader(mPlaylist, this, (ItemCursorAdapter)mAdapter, mCursor);


		spChanged = new SharedPreferences.OnSharedPreferenceChangeListener() {

			@Override
			public void onSharedPreferenceChanged(
					SharedPreferences sharedPreferences, String key) {
				if (key.equals(ApplicationConfiguration.showListenedKey)) {
					playlistCursor.requery();
				}
			}
		};

		TopActivity.getPreferences().registerOnSharedPreferenceChangeListener(
				spChanged);

		if (savedInstanceState != null) {
			// Restore last state for checked position.
			mExpandedEpisodeId = savedInstanceState.getLong(
					mExpandedEpisodeKey, mExpandedEpisodeId);
		}

	}

    @Override
    public void onStart () {
        super.onStart();

        // Does this even work?
        for (FeedItem item : mPlaylist.getPlaylist()) {
            if (!TextUtils.isEmpty(item.image)) {
                PicassoWrapper.fetch(mActivity, item.image, null);
            }
        }
    }

    @Override
    public void onRefresh() {
        Log.d("PlaylistRefresh", "starting");
        mSwipeRefreshView.setRefreshing(true);
        PodcastDownloadManager.start_update(mActivity, null, this);
    }

    @Override
    public void complete(boolean succes) {
        Log.d("PlaylistRefresh", "ending");
        mSwipeRefreshView.setRefreshing(false);
    }

    @SuppressLint("WrongViewCast")
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view,savedInstanceState);

        mSwipeRefreshView.setGestureListener(mListener);
        mSwipeRefreshView.fragment = this;

        mPhotoContainer =   (TopPlayer) mSwipeRefreshView.findViewById(R.id.session_photo_container);
        mPhoto =            (ImageView) mSwipeRefreshView.findViewById(R.id.session_photo);

        mPlayPauseButton         =    (PlayPauseImageView) mSwipeRefreshView.findViewById(R.id.play_pause_button);
        mPlayerSeekbar          =    (PlayerSeekbar) mSwipeRefreshView.findViewById(R.id.player_progress);
        mPlayerDownloadButton   =    (DownloadButtonView) mSwipeRefreshView.findViewById(R.id.download);
        mBackButton = (PlayerButtonView)mSwipeRefreshView.findViewById(R.id.previous);
        mDownloadButton = (PlayerButtonView)mSwipeRefreshView.findViewById(R.id.download);
        mQueueButton = (PlayerButtonView)mSwipeRefreshView.findViewById(R.id.queue);
        mFavoriteButton = (PlayerButtonView)mSwipeRefreshView.findViewById(R.id.bookmark);

        // use a linear layout manager
        mLayoutManager = new ExpandableLayoutManager(mActivity);
        mRecyclerView.setLayoutManager(mLayoutManager);

        //mRecyclerView.setOnScrollListener(this);

        if (mPlaylist == null) {
            mPlaylist = new Playlist(mActivity);
            mPlaylist.populatePlaylistIfEmpty();
        }

        // specify an adapter (see also next example)
        mAdapter = new ItemCursorAdapter(mActivity, this, mOverlay, mPlaylist, mCursor, mDownloadProgressObservable);
        mRecyclerView.setAdapter(mAdapter);
        RecentItemsRecyclerListener l = new RecentItemsRecyclerListener(mAdapter);
        mRecyclerView.setRecyclerListener(l);

        mPhotoContainer.bringToFront();

        if (!mPlaylist.isEmpty()) {
            bindHeader(mPlaylist.first());
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mActivity = activity;
    }

    public void bindHeader(final FeedItem item) {
        Callback cb = new Callback() {

            @Override
            public void onSuccess() {
                mHasPhoto = true;
                recomputePhotoAndScrollingMetrics();
                int h =mPhotoContainer.getHeight();
                maxRecyclerViewOffset = h;
                mPhotoContainer.getLayoutParams().height = (h); // +actionBarSize
                mPhotoContainer.requestLayout();
                mRecyclerView.scrollToPosition(0);
                Log.d("RecyclerPadding", "padding: " + h);



                Bitmap bitmap = ((BitmapDrawable)mPhoto.getDrawable()).getBitmap();
                if (bitmap != null) {
                    PaletteCache.generate(item.image, bitmap);
                }
            }

            @Override
            public void onError() {
                return;
            }
        };

        mPlayPauseButton.setmEpisodeId(item.getId());
        mBackButton.setEpisodeId(item.getId());
        mDownloadButton.setEpisodeId(item.getId());
        mQueueButton.setEpisodeId(item.getId());
        mFavoriteButton.setEpisodeId(item.getId());

        mPlayPauseButton.setStatus(PlayerStatusObservable.STATUS.PAUSED); // FIXME: This should not be static
        mDownloadProgressObservable.registerObserver(mPlayPauseButton);

        mPlayerSeekbar.setEpisode(item);
        mPlayerSeekbar.setOverlay(mOverlay);

        mPlayerDownloadButton.setEpisode(item);

        mDownloadProgressObservable.registerObserver(mPlayerDownloadButton);

        PlayerStatusObservable.registerListener(mPlayerSeekbar);

        PaletteObservable.registerListener(mPlayPauseButton);
        PaletteObservable.registerListener(mBackButton);
        PaletteObservable.registerListener(mDownloadButton);
        PaletteObservable.registerListener(mQueueButton);
        PaletteObservable.registerListener(mFavoriteButton);

        Palette palette = PaletteCache.get(item.image);
        if (palette != null) {
            mPlayPauseButton.onPaletteFound(palette);
            mBackButton.onPaletteFound(palette);
            mDownloadButton.onPaletteFound(palette);
            mQueueButton.onPaletteFound(palette);
            mFavoriteButton.onPaletteFound(palette);
        }

        BackgroundTransformation mImageTransformation = null;
        int height = TopPlayer.sizeLarge;//1080;//(int)(mPhoto.getHeight()*5.6);
        trans = BackgroundTransformation.getmImageTransformation(mActivity, mImageTransformation, height);
        PicassoWrapper.load(mActivity, item.image, mPhoto, trans, cb);
        mPhotoContainer.getLayoutParams().height = (height); // +actionBarSize
        mPhotoContainer.requestLayout();
    }
    com.squareup.picasso.Transformation trans = null;

    private void recomputePhotoAndScrollingMetrics() {
        final int actionBarSize = UIUtils.calculateActionBarSize(mActivity);
        mHeaderTopClearance = actionBarSize - mPhotoContainer.getPaddingTop();
        mHeaderHeightPixels = mPhotoContainer.getHeight();

        mPhotoHeightPixels = mHeaderTopClearance;
        if (mHasPhoto) {
            mPhotoHeightPixels = (int) (mPhoto.getWidth() / PHOTO_ASPECT_RATIO);
            mPhotoHeightPixels = Math.min(mPhotoHeightPixels, mSwipeRefreshView.getHeight() * 2 / 3);
        }

        ViewGroup.LayoutParams lp;
        lp = mPhotoContainer.getLayoutParams();
        if (lp.height != mPhotoHeightPixels) {
            lp.height = mPhotoHeightPixels;
            mPhotoContainer.setLayoutParams(lp);
        }

        //onScrolled(0, 0); // trigger scroll handling
    }

    public int playerHeight = getMaxPlayerSize();

    public static boolean usingLargeLayout = true;
    private GestureDetector.OnGestureListener mListener = new GestureDetector.SimpleOnGestureListener() {

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            Log.d("GeatureDetector", "onScroll: e1y -> " + e1.getY() + " e2y -> " + e2.getY() + " dy -> " + distanceY);
            mSwipeRefreshView.getOverScroller().startScroll((int)e2.getX(),
                                                            (int)e2.getY(),
                                                            (int)distanceX,
                                                            (int)distanceY);
            return scrollLayout(distanceY);
        }

        // http://stackoverflow.com/questions/4951142/smooth-scrolling-in-android
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            Log.d("GeatureDetector", "Fling : e1y -> " + e1.getY() + " e2y -> " + e2.getY() + " vy -> " + velocityY);
            //mSwipeRefreshView.getOverScroller().
            mSwipeRefreshView.getOverScroller().fling((int)e1.getX(), (int)e1.getY(),
                    (int)velocityX, (int)velocityY, 0, (int)10000, 0, (int)10000);

            return true;
        }
    };

    public boolean scrollLayout( float distanceY) {
        Log.d("GeatureDetector", "scrollLayout: distanceY -> " + distanceY);

        ExpandableLayoutManager elm = ((ExpandableLayoutManager) mRecyclerView.getLayoutManager());
        if (mPhotoContainer.isMinimumSize()) {
            elm.SetCanScrollVertically(true);
        } else {
            elm.SetCanScrollVertically(false);
        }

        mSwipeRefreshView.mDownGeastureInProgress = true;
        // distanceY > 0 => scroll up
        // distanceY < 0 => scroll down

        if (mPhotoContainer.isMinimumSize() && distanceY > 0) {
            mSwipeRefreshView.mDownGeastureInProgress = false;
            mSwipeRefreshView.setCurrentScrollState(SwipeRefreshExpandableLayout.ScrollState.MINIMAIL_PLAYER_AND_SCROLLED_LIST);
            return false;
        }

        int containerHeight = mPhotoContainer.getHeight();
        float containerTranslationY = mPhotoContainer.getTranslationY();
        //float currentHeight = !mPhotoContainer.isMinimumSize() ? containerHeight+containerTranslationY-distanceY : mPhotoContainer.getHeight()-distanceY;
        float currentHeight = containerHeight+containerTranslationY-distanceY;

        Log.d("setTranslationXYZ", "h -> " + containerHeight + " t -> " + containerTranslationY + " dy -> " + distanceY + " curH -> " + currentHeight);



        float newShrinkAmount = mPhotoContainer.getTranslationY()-distanceY;
        Log.d("setTranslationXYZ", "newShrinkAmount: " +  newShrinkAmount + " trans: " + mPhotoContainer.getTranslationY() + " distY: " + distanceY);

        float newVisibleHeight = -1;

        // Prevent the user from scrolling too far down. (i.e. more down than the maximum size player)
        if (mPhotoContainer.isMaximumSize() && currentHeight >= 0 && newShrinkAmount > 0) {
            mPhoto.setTranslationY(0);
            mRecyclerView.setTranslationY(mPhotoContainer.getHeight());
            mSwipeRefreshView.setCurrentScrollState(SwipeRefreshExpandableLayout.ScrollState.FULL_PLAYER);
            //mPhotoContainer.setPlayerHeight(mPhotoContainer.get, newOffset);
            return false;
        }

        if ( (!mPhotoContainer.isMinimumSize() && distanceY > 0) || (!mPhotoContainer.isMaximumSize() && distanceY < 0) ) {
            newVisibleHeight = mPhotoContainer.setPlayerHeight(currentHeight, newShrinkAmount);
            Log.d("photoOffset", "newVisibleHeight -> " + newVisibleHeight);

            if (newShrinkAmount == 111120) {
                //mPhotoContainer.setTranslationY(newOffset);
                float photoOffset = -newShrinkAmount / 2; // < 0 ? 0 : -newOffset / 2;
                Log.d("photoOffset", "photoOffset -> " + photoOffset);
                float photoOffset2 = photoOffset < 0 ? 0 : photoOffset;
                Log.d("photoOffset", "photoOffset2 -> " + photoOffset2);
                mPhoto.setTranslationY(photoOffset2); //mPhoto.getTranslationY()+distanceY/2);
            }
            //mPlayPauseButton.setTranslationY(-photoOffset2);
        }

        if (mPhotoContainer.isMinimumSize()) {
            if (usingLargeLayout) {
                Log.d("photoOffset", "mPhotoContainer.setTranslationY(0)");
                //mPhotoContainer.setTranslationY(0);
                mPhoto.setTranslationY(0);
                mSwipeRefreshView.setCurrentScrollState(SwipeRefreshExpandableLayout.ScrollState.MINIMAIL_PLAYER);
                mPhotoContainer.bringToFront();
                usingLargeLayout = false;
            }
        } else {
            if (!usingLargeLayout) {
                mRecyclerView.bringToFront();
                mSwipeRefreshView.setCurrentScrollState(SwipeRefreshExpandableLayout.ScrollState.PARTIAL_PLAYER);
            }
            usingLargeLayout = true;
        }

        boolean isMinimumSize = mPhotoContainer.isMinimumSize();

        mSwipeRefreshView.setCanScrollRecyclerView(isMinimumSize);
        mRecyclerView.setCanScrollRecyclerView(isMinimumSize);

        if (newVisibleHeight < 0)
            newVisibleHeight = mPhotoContainer.getHeight();


        if (isMinimumSize) {
            Log.d("TopPlayerIOut", "To the top!");
            //mRecyclerView.scrollToPosition(0);
        }

        int[] location = new int[2];
        mRecyclerView.getChildAt(0).getLocationOnScreen(location);

        int[] location2 = new int[2];
        mRecyclerView.getLocationOnScreen(location2);

        Log.d("TopPlayerIOut", "Translation =>" + newVisibleHeight + " mRecyclerView.child.Top => " + location[0] + " & " + location[1]);
        Log.d("TopPlayerIOut", "diffx =>" + (location[0]-location2[0]) + " diffy => " + (location[1]-location2[1]));
        mRecyclerView.setTranslationY(newVisibleHeight);

        Log.d("TopPlayerInputk", "mPhotoContainer translation ->" + mPhotoContainer.getTranslationY());
        return true;
    }


	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putLong(mExpandedEpisodeKey, mExpandedEpisodeId);
	}

	@Override
	public void onResume() {
		super.onResume();

		ControlButtons.fragment = this;
	}


	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = mActivity.getMenuInflater();
		inflater.inflate(R.menu.podcast_context, menu);
		PlaylistFragment.setContextMenu(PLAYLIST_CONTEXT_MENU, this);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {

		if (!AdapterView.AdapterContextMenuInfo.class.isInstance(item
				.getMenuInfo()))
			return false;

		if (CONTEXT_MENU == PLAYLIST_CONTEXT_MENU) {
			return playlistContextMenu(item);
		} else if (CONTEXT_MENU == SUBSCRIPTION_CONTEXT_MENU) {
			if (CONTEXT_FRAGMENT != null)
				return ((SubscriptionsFragment) CONTEXT_FRAGMENT)
						.subscriptionContextMenu(item);
		}

		return false;

	}

	public boolean playlistContextMenu(MenuItem item) {
		AdapterView.AdapterContextMenuInfo cmi = (AdapterView.AdapterContextMenuInfo) item
				.getMenuInfo();
        return true;
	}

    private int getMaxPlayerSize() {
        return DEFAULT_IMAGE_SIZE;
    }

	@Override
	View getPullView() {
		return getListView();
	}

	public static void setContextMenu(int menu, Fragment fragment) {
		CONTEXT_MENU = menu;
		CONTEXT_FRAGMENT = fragment;
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		if (key.equals(ApplicationConfiguration.showListenedKey)) {
			//RecentItemFragment.this.getAdapter().notifyDataSetChanged();
		}
	}

             @Override
             public void notifyPlaylistChanged() {
                 Playlist activePlaylist = Playlist.getActivePlaylist();
                 if (mPlaylist != activePlaylist)
                     mPlaylist = activePlaylist;

                 mPlaylist.populatePlaylistIfEmpty();
                 if (!mPlaylist.isEmpty()) {
                     bindHeader(mPlaylist.first());
                 }
             }

             @Override
             public void notifyPlaylistRangeChanged(int from, int to) {

                 Playlist activePlaylist = Playlist.getActivePlaylist();
                 if (mPlaylist != activePlaylist)
                     mPlaylist = activePlaylist;

                 mPlaylist.populatePlaylistIfEmpty();
                 if (!mPlaylist.isEmpty()) {

                     mActivity.runOnUiThread(new Runnable() {
                         @Override
                         public void run() {
                             bindHeader(mPlaylist.first());
                         }
                     });
                 }
             }
         }
