package org.bottiger.podcast;

import org.bottiger.podcast.adapters.PlaylistAdapter;
import org.bottiger.podcast.adapters.decoration.DragSortRecycler;
import org.bottiger.podcast.adapters.decoration.InitialHeaderAdapter;
import org.bottiger.podcast.listeners.DownloadProgressObservable;
import org.bottiger.podcast.listeners.PaletteObservable;
import org.bottiger.podcast.listeners.PlayerStatusObservable;
import org.bottiger.podcast.listeners.RecentItemsRecyclerListener;
import org.bottiger.podcast.listeners.RecyclerItemTouchListener;
import org.bottiger.podcast.playlist.Playlist;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.service.IDownloadCompleteCallback;
import org.bottiger.podcast.service.Downloader.EpisodeDownloadManager;
import org.bottiger.podcast.service.PlayerService;
import org.bottiger.podcast.utils.BackgroundTransformation;
import org.bottiger.podcast.utils.PaletteCache;
import org.bottiger.podcast.utils.StrUtils;
import org.bottiger.podcast.utils.UIUtils;
import org.bottiger.podcast.views.DownloadButtonView;
import org.bottiger.podcast.views.PlayPauseImageView;
import org.bottiger.podcast.views.PlayerButtonView;
import org.bottiger.podcast.views.PlayerSeekbar;
import org.bottiger.podcast.views.TextViewObserver;
import org.bottiger.podcast.views.TopPlayer;
import org.bottiger.podcast.views.MultiShrink.playlist.MultiShrinkScroller;
import org.bottiger.podcast.views.dialogs.DialogBulkDownload;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TextView;

import com.eowise.recyclerview.stickyheaders.StickyHeadersBuilder;
import com.eowise.recyclerview.stickyheaders.StickyHeadersItemDecoration;
import com.facebook.drawee.view.SimpleDraweeView;
import com.squareup.picasso.Callback;

import jp.wasabeef.recyclerview.animators.SlideInLeftAnimator;

public class PlaylistFragment extends GeastureFragment implements
		OnSharedPreferenceChangeListener, Playlist.PlaylistChangeListener, IDownloadCompleteCallback
         {

    private static int CONTEXT_MENU = 0;
	private static Fragment CONTEXT_FRAGMENT = null;

	public final static int PLAYLIST_CONTEXT_MENU = 0;
	public final static int SUBSCRIPTION_CONTEXT_MENU = 1;

    private boolean mHasPhoto = false;
    private static final float PHOTO_ASPECT_RATIO = 1.7777777f;

    private View mPlaylistContainer;
    private View mEmptyPlaylistContainer;

    private MultiShrinkScroller mMultiShrinkScroller;

    private TopPlayer mTopPlayer;
    private SimpleDraweeView mPhoto;

    private TextView mEpisodeTitle;
    private TextView mEpisodeInfo;
    private TextViewObserver mCurrentTime;
    private TextViewObserver mTotalTime;
    private PlayPauseImageView mPlayPauseButton;
    private PlayerSeekbar mPlayerSeekbar;
    private DownloadButtonView mPlayerDownloadButton;
    private PlayerButtonView mForwardButton;
    private PlayerButtonView mBackButton;
    private PlayerButtonView mDownloadButton;
    private PlayerButtonView mFavoriteButton;

    private int mHeaderTopClearance;
    private int mPhotoHeightPixels;

	private SharedPreferences.OnSharedPreferenceChangeListener spChanged;

	/** ID of the current expanded episode */
	private long mExpandedEpisodeId = -1;
	private String mExpandedEpisodeKey = "currentExpanded";

    DownloadProgressObservable mDownloadProgressObservable = null;

    private Playlist mPlaylist;
    private Activity mActivity;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mDownloadProgressObservable = new DownloadProgressObservable(mActivity);
        mPlaylist = PlayerService.getPlaylist(this);

        super.onCreate(savedInstanceState);
    }

    @Override
    public void onDestroyView() {
        EpisodeDownloadManager.resetDownloadProgressObservable();
        super.onDestroyView();
    }

	// Read here:
	// http://developer.android.com/reference/android/app/Fragment.html#Layout
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

        mDownloadProgressObservable = EpisodeDownloadManager.getDownloadProgressObservable(mActivity);

		TopActivity.getPreferences().registerOnSharedPreferenceChangeListener(
				spChanged);

		if (savedInstanceState != null) {
			// Restore last state for checked position.
			mExpandedEpisodeId = savedInstanceState.getLong(
					mExpandedEpisodeKey, mExpandedEpisodeId);
		}

	}

    @Override
    public void onRefresh() {
        Log.d("PlaylistRefresh", "starting");
        mSwipeRefreshView.setRefreshing(true);
        SoundWaves.sSubscriptionRefreshManager.refresh(null, this);
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

        mMultiShrinkScroller = (MultiShrinkScroller) mSwipeRefreshView.findViewById(R.id.playlist_container);

        mTopPlayer =   (TopPlayer) mSwipeRefreshView.findViewById(R.id.session_photo_container);
        mPhoto =            (SimpleDraweeView) mSwipeRefreshView.findViewById(R.id.session_photo);

        mPlaylistContainer = mSwipeRefreshView.findViewById(R.id.playlist_container);
        mEmptyPlaylistContainer = mSwipeRefreshView.findViewById(R.id.playlist_empty);

        mEpisodeTitle         =    (TextView) mSwipeRefreshView.findViewById(R.id.episode_title);
        mEpisodeInfo         =    (TextView) mSwipeRefreshView.findViewById(R.id.episode_info);

        mCurrentTime       =    (TextViewObserver) mSwipeRefreshView.findViewById(R.id.current_time);
        mTotalTime         =    (TextViewObserver) mSwipeRefreshView.findViewById(R.id.total_time);

        mPlayPauseButton         =    (PlayPauseImageView) mSwipeRefreshView.findViewById(R.id.play_pause_button);
        mPlayerSeekbar          =    (PlayerSeekbar) mSwipeRefreshView.findViewById(R.id.player_progress);
        mPlayerDownloadButton   =    (DownloadButtonView) mSwipeRefreshView.findViewById(R.id.download);
        mBackButton = (PlayerButtonView)mSwipeRefreshView.findViewById(R.id.previous);
        mForwardButton = (PlayerButtonView)mSwipeRefreshView.findViewById(R.id.fast_forward_button);
        mDownloadButton = (PlayerButtonView)mSwipeRefreshView.findViewById(R.id.download);
        mFavoriteButton = (PlayerButtonView)mSwipeRefreshView.findViewById(R.id.favorite);

        setPlaylistViewState(mPlaylist);

        mMultiShrinkScroller.initialize(new MultiShrinkScroller.MultiShrinkScrollerListener() {
            @Override
            public void onScrolledOffBottom() {

            }

            @Override
            public void onStartScrollOffBottom() {

            }

            @Override
            public void onTransparentViewHeightChange(float ratio) {

            }

            @Override
            public void onEntranceAnimationDone() {

            }

            @Override
            public void onEnterFullscreen() {

            }

            @Override
            public void onExitFullscreen() {

            }
        }, true);

        // use a linear layout manager
        //mLayoutManager = new ExpandableLayoutManager(mActivity, mSwipeRefreshView, mTopPlayer, mRecyclerView, mPhoto);
        mLayoutManager = new LinearLayoutManager(mActivity);
        mRecyclerView.setLayoutManager(mLayoutManager);

        //mRecyclerView.setOnScrollListener(this);

        mTopPlayer.setRecyclerView(mRecyclerView);

        // specify an adapter (see also next example)
        mAdapter = new PlaylistAdapter(mActivity, mOverlay, mDownloadProgressObservable);
        mAdapter.setHasStableIds(true);

        mRecyclerView.setAdapter(mAdapter);

        //RecentItemsRecyclerListener l = new RecentItemsRecyclerListener(mAdapter);
        //mRecyclerView.setRecyclerListener(l);

        // The bug where the player doesn't show up happens because the playlist is empty.
        // The playlist is empty if the context is null
        // The context is set by the PlayerService
        // I need to make sure that playerservice has been started at this point,
        // or more realisticly that the header is fixed when the context is ready
        mPlaylist.setContext(getActivity());
        mPlaylist.populatePlaylistIfEmpty();

        if (!mPlaylist.isEmpty()) {
            bindHeader(mPlaylist.first());
        }

        mSwipeRefreshView.fragment = this;

        // Build item decoration and add it to the RecyclerView
        /*
        InitialHeaderAdapter initialHeaderAdapter = new InitialHeaderAdapter(mPlaylist);
        StickyHeadersItemDecoration decoration = new StickyHeadersBuilder()
                .setAdapter(mAdapter)
                .setRecyclerView(mRecyclerView)
                .setStickyHeadersAdapter(
                        initialHeaderAdapter, // Class that implements StickyHeadersAdapter
                        true)     // Decoration position relative to a item
                .build();

        mRecyclerView.addItemDecoration(decoration);
        */

        //////

        mRecyclerView.addOnItemTouchListener(new RecyclerItemTouchListener());


        //////
        /*
        DragSortRecycler dragSortRecycler = new DragSortRecycler();
        dragSortRecycler.setViewHandleId(R.id.drag_handle); //View you wish to use as the handle

        dragSortRecycler.setOnDragStateChangedListener(initialHeaderAdapter);
        dragSortRecycler.setOnDragStateChangedListener(mPlaylist);
        dragSortRecycler.setOnDragStateChangedListener(mRecyclerView);

        dragSortRecycler.setNavigationHeight(UIUtils.NavigationBarHeight(mActivity));


        mRecyclerView.addItemDecoration(dragSortRecycler);
        mRecyclerView.addOnItemTouchListener(dragSortRecycler);
        */
        //mRecyclerView.setOnScrollListener(dragSortRecycler.getScrollListener());
        //////

        mRecyclerView.setItemAnimator(new SlideInLeftAnimator());
    }

    @Override
    public void onAttach(Activity activity) {
        mActivity = activity;
        super.onAttach(activity);
    }

    @Override
    public void onPause() {
        PaletteObservable.unregisterListener(mPlayPauseButton);
        PaletteObservable.unregisterListener(mBackButton);
        PaletteObservable.unregisterListener(mForwardButton);
        PaletteObservable.unregisterListener(mDownloadButton);
        PaletteObservable.unregisterListener(mFavoriteButton);
        super.onPause();
    }

    @Override
    public void onResume() {
        if (mPlaylist != null) {
            FeedItem item = mPlaylist.getItem(0);

            if (item != null) {
                mPlayPauseButton.setEpisodeId(item.getId(), PlayPauseImageView.LOCATION.PLAYLIST);
                mBackButton.setEpisodeId(item.getId());
                mForwardButton.setEpisodeId(item.getId());
                mDownloadButton.setEpisodeId(item.getId());
                mFavoriteButton.setEpisodeId(item.getId());
            }
        }

        super.onResume();
    }


    public void bindHeader(final FeedItem item) {
        Callback cb = new Callback() {

            @Override
            public void onSuccess() {
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

        mEpisodeTitle.setText(item.getTitle());
        mEpisodeInfo.setText("");

        long duration = item.getDuration();
        if (duration > 0) {
            mTotalTime.setText(StrUtils.formatTime(duration));
        } else {
            mTotalTime.setText("");
        }

        long offset = item.offset;
        if (offset > 0) {
            mCurrentTime.setText(StrUtils.formatTime(offset));
        } else {
            mCurrentTime.setText("");
        }
        mCurrentTime.setEpisode(item);
        mTotalTime.setEpisode(item);

        mPlayPauseButton.setEpisodeId(item.getId(), PlayPauseImageView.LOCATION.PLAYLIST);
        mBackButton.setEpisodeId(item.getId());
        mForwardButton.setEpisodeId(item.getId());
        mDownloadButton.setEpisodeId(item.getId());
        mFavoriteButton.setEpisodeId(item.getId());

        mPlayPauseButton.setStatus(PlayerStatusObservable.STATUS.PAUSED); // FIXME: This should not be static
        mDownloadProgressObservable.registerObserver(mDownloadButton);

        mBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (MainActivity.sBoundPlayerService != null) {
                    MainActivity.sBoundPlayerService.getPlayer().rewind(item);
                }
            }
        });

        mForwardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (MainActivity.sBoundPlayerService != null) {
                    MainActivity.sBoundPlayerService.getPlayer().fastForward(item);
                }
            }
        });

        mPlayerSeekbar.setEpisode(item);
        mPlayerSeekbar.setOverlay(mOverlay);

        mPlayerDownloadButton.setEpisode(item);

        mDownloadProgressObservable.registerObserver(mPlayerDownloadButton);

        mTopPlayer.setEpisodeId(item);
        PaletteObservable.registerListener(mTopPlayer);

        PlayerStatusObservable.registerListener(mPlayerSeekbar);


        Palette palette = PaletteCache.get(item.image);
        if (palette != null) {
            mTopPlayer.onPaletteFound(palette);
            mPlayPauseButton.onPaletteFound(palette);
            mBackButton.onPaletteFound(palette);
            mForwardButton.onPaletteFound(palette);
            mDownloadButton.onPaletteFound(palette);
            mFavoriteButton.onPaletteFound(palette);
        } else {
            PaletteCache.generate(item.image, getActivity());
        }

        if (item != null) {
            Uri uri = Uri.parse(item.image);
            mPhoto.setImageURI(uri);
        }

        if (mTopPlayer.getVisibleHeight() == 0) {
            mTopPlayer.setPlayerHeight(1000);
        }
    }

    private void recomputePhotoAndScrollingMetrics() {
        final int actionBarSize = UIUtils.calculateActionBarSize(mActivity);
        mHeaderTopClearance = actionBarSize - mTopPlayer.getPaddingTop();

        mPhotoHeightPixels = mHeaderTopClearance;
        if (mHasPhoto) {
            mPhotoHeightPixels = (int) (mPhoto.getWidth() / PHOTO_ASPECT_RATIO);
            mPhotoHeightPixels = Math.min(mPhotoHeightPixels, mSwipeRefreshView.getHeight() * 2 / 3);
        }

        ViewGroup.LayoutParams lp;
        lp = mTopPlayer.getLayoutParams();
        if (lp.height != mPhotoHeightPixels) {
            lp.height = mPhotoHeightPixels;
            mTopPlayer.setLayoutParams(lp);
        }
    }

    private void setPlaylistViewState(@NonNull Playlist argPlaylist) {
        if (argPlaylist.isEmpty()) {
            mPlaylistContainer.setVisibility(View.GONE);
            mEmptyPlaylistContainer.setVisibility(View.VISIBLE);
        } else {
            mPlaylistContainer.setVisibility(View.VISIBLE);
            mEmptyPlaylistContainer.setVisibility(View.GONE);
        }
    }

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putLong(mExpandedEpisodeKey, mExpandedEpisodeId);
	}

    @Override
    public void onStart () {
        super.onStart();
        mPlaylist.registerPlaylistChangeListener(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        mPlaylist.unregisterPlaylistChangeListener(this);
    }


     @Override
     public void onCreateContextMenu(ContextMenu menu, View v,
                                     ContextMenuInfo menuInfo) {
         super.onCreateContextMenu(menu, v, menuInfo);
         MenuInflater inflater = mActivity.getMenuInflater();
         //inflater.inflate(R.menu.podcast_context, menu);
         PlaylistFragment.setContextMenu(PLAYLIST_CONTEXT_MENU, this);
     }

     @Override
     public void onCreateOptionsMenu(final Menu menu, MenuInflater inflater) {
         inflater.inflate(R.menu.playlist_actionbar, menu);
         super.onCreateOptionsMenu(menu, inflater);
         return;
     }

     @Override
     public boolean onOptionsItemSelected(MenuItem item) {
         switch (item.getItemId()) {
             case R.id.menu_bulk_download: {
                 DialogBulkDownload dialogBulkDownload = new DialogBulkDownload();
                 Dialog dialog = dialogBulkDownload.onCreateDialog(getActivity(), mPlaylist);
                 dialog.show();
                 return true;
             }
         }
         return super.onOptionsItemSelected(item);
     }

	public boolean playlistContextMenu(MenuItem item) {
		AdapterView.AdapterContextMenuInfo cmi = (AdapterView.AdapterContextMenuInfo) item
				.getMenuInfo();
        return true;
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
        mActivity.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                mPlaylist.populatePlaylistIfEmpty();

                setPlaylistViewState(mPlaylist);

                if (!mPlaylist.isEmpty()) {
                    bindHeader(mPlaylist.first());
                }

            }
        });
    }

    @Override
    public void notifyPlaylistRangeChanged(int from, int to) {
        if (!mPlaylist.isEmpty()) {

            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    bindHeader(mPlaylist.first());

                    setPlaylistViewState(mPlaylist);
                }
            });
        }
    }
}
