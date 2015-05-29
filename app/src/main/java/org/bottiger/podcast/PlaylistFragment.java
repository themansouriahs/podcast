package org.bottiger.podcast;

import org.bottiger.podcast.adapters.PlaylistAdapter;
import org.bottiger.podcast.listeners.DownloadProgressObservable;
import org.bottiger.podcast.listeners.PaletteListener;
import org.bottiger.podcast.listeners.PlayerStatusObservable;
import org.bottiger.podcast.listeners.RecyclerItemTouchListener;
import org.bottiger.podcast.playlist.Playlist;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.provider.ISubscription;
import org.bottiger.podcast.service.IDownloadCompleteCallback;
import org.bottiger.podcast.service.Downloader.EpisodeDownloadManager;
import org.bottiger.podcast.service.PlayerService;
import org.bottiger.podcast.utils.PaletteHelper;
import org.bottiger.podcast.utils.StrUtils;
import org.bottiger.podcast.utils.UIUtils;
import org.bottiger.podcast.views.DownloadButtonView;
import org.bottiger.podcast.views.PlayPauseImageView;
import org.bottiger.podcast.views.PlayerButtonView;
import org.bottiger.podcast.views.PlayerSeekbar;
import org.bottiger.podcast.views.SwipeDismissRecyclerViewTouchListener;
import org.bottiger.podcast.views.TextViewObserver;
import org.bottiger.podcast.views.TopPlayer;
import org.bottiger.podcast.views.MultiShrink.playlist.MultiShrinkScroller;
import org.bottiger.podcast.views.dialogs.DialogBulkDownload;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;

import com.cocosw.undobar.UndoBarController;
import com.cocosw.undobar.UndoBarStyle;
import com.facebook.drawee.view.SimpleDraweeView;
import com.squareup.otto.Produce;
import com.squareup.otto.Subscribe;

import jp.wasabeef.recyclerview.animators.SlideInLeftAnimator;

public class PlaylistFragment extends GeastureFragment implements
		OnSharedPreferenceChangeListener, IDownloadCompleteCallback
         {

	public final static int SUBSCRIPTION_CONTEXT_MENU = 1;

    private static final String PLAYLIST_WELCOME_DISMISSED = "playlist_welcome_dismissed";
    private static final boolean PLAYLIST_WELCOME_DISMISSED_DEFAULT = false;

    private View mPlaylistContainer;
    private View mPlaylistWelcomeContainer;
    private View mPlaylistEmptyContainer;

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

	private SharedPreferences.OnSharedPreferenceChangeListener spChanged;

	/** ID of the current expanded episode */
	private long mExpandedEpisodeId = -1;
	private String mExpandedEpisodeKey = "currentExpanded";

    DownloadProgressObservable mDownloadProgressObservable = null;

    private Playlist mPlaylist;
    private Activity mActivity;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mDownloadProgressObservable = new DownloadProgressObservable((SoundWaves)mActivity.getApplicationContext());
        super.onCreate(savedInstanceState);
        //SoundWaves.getBus().register(this);
    }

    @Override
    public void onDestroy() {
        //SoundWaves.getBus().unregister(this);
        super.onDestroy();
    }

    @Override
    public void onDestroyView() {
        EpisodeDownloadManager.resetDownloadProgressObservable();
        SoundWaves.getBus().unregister(mAdapter);
        SoundWaves.getBus().unregister(mPlayPauseButton);
        SoundWaves.getBus().unregister(mPlayerSeekbar);
        SoundWaves.getBus().unregister(mCurrentTime);
        super.onDestroyView();
    }

    @Produce
    public Playlist producePlaylist() {
        // Assuming 'lastAnswer' exists.
        return new Playlist(mActivity);
    }

	// Read here:
	// http://developer.android.com/reference/android/app/Fragment.html#Layout
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

        mDownloadProgressObservable = EpisodeDownloadManager.getDownloadProgressObservable((SoundWaves)mActivity.getApplicationContext());

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
    public void complete(boolean succes, ISubscription argSubscription) {
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
        mPlaylistWelcomeContainer = mSwipeRefreshView.findViewById(R.id.playlist_welcome_screen);
        mPlaylistEmptyContainer = mSwipeRefreshView.findViewById(R.id.playlist_empty);

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

        SoundWaves.getBus().register(mAdapter);
        SoundWaves.getBus().register(mPlayPauseButton);
        SoundWaves.getBus().register(mPlayerSeekbar);
        SoundWaves.getBus().register(mCurrentTime);

        mRecyclerView.setAdapter(mAdapter);

        //RecentItemsRecyclerListener l = new RecentItemsRecyclerListener(mAdapter);
        //mRecyclerView.setRecyclerListener(l);

        if (mPlaylist != null && !mPlaylist.isEmpty()) {
            IEpisode episode = mPlaylist.first();
            bindHeader(episode);
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

        ///////

        SwipeDismissRecyclerViewTouchListener touchListener =
                new SwipeDismissRecyclerViewTouchListener(
                        mRecyclerView,
                        new SwipeDismissRecyclerViewTouchListener.DismissCallbacks() {
                            @Override
                            public boolean canDismiss(int position) {
                                return true;
                            }

                            @Override
                            public void onDismiss(RecyclerView recyclerView, int[] reverseSortedPositions) {

                                final int itemPosition = reverseSortedPositions[0]+1;
                                final IEpisode episode = mPlaylist.getItem(itemPosition);
                                final int currentPriority = episode.getPriority();
                                final ContentResolver contentResolver = getActivity().getContentResolver();

                                episode.setPriority(-1);

                                if (episode instanceof FeedItem) {
                                    FeedItem item = (FeedItem) episode;
                                    item.markAsListened();
                                }

                                episode.update(contentResolver);
                                mPlaylist.removeItem(itemPosition);
                                // do not call notifyItemRemoved for every item, it will cause gaps on deleting items
                                mAdapter.notifyDataSetChanged();
                                String episodeRemoved = getResources().getString(R.string.playlist_episode_dismissed);

                                new UndoBarController.UndoBar(getActivity())
                                        .message(episodeRemoved)
                                        .listener(new UndoBarController.AdvancedUndoListener() {
                                            @Override
                                            public void onHide(Parcelable parcelable) {

                                            }

                                            @Override
                                            public void onClear(@NonNull Parcelable[] parcelables) {

                                            }

                                            @Override
                                            public void onUndo(Parcelable parcelable) {
                                                episode.setPriority(currentPriority);
                                                mPlaylist.setItem(itemPosition, episode);
                                                mAdapter.notifyDataSetChanged();

                                                if (episode instanceof FeedItem) {
                                                    FeedItem item = (FeedItem) episode;
                                                    item.markAsListened(0);
                                                }

                                                episode.update(contentResolver);
                                            }
                                        }).show();
                            }
                        });
        mRecyclerView.setOnTouchListener(touchListener);


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

        //mRecyclerView.setItemAnimator(new SlideInLeftAnimator());
    }

    @Override
    public void onAttach(Activity activity) {
        mActivity = activity;
        super.onAttach(activity);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        if (mPlaylist != null) {
            IEpisode item = mPlaylist.getItem(0);

            if (item != null) {
                mPlayPauseButton.setEpisode(item, PlayPauseImageView.LOCATION.PLAYLIST);
                mBackButton.setEpisode(item);
                mForwardButton.setEpisode(item);
                mDownloadButton.setEpisode(item);
                mFavoriteButton.setEpisode(item);
            }
            playlistChanged(mPlaylist);
        }
        super.onResume();
    }


    public void bindHeader(final IEpisode item) {

        if (mEpisodeTitle == null)
            return;

        mEpisodeTitle.setText(item.getTitle());
        mEpisodeInfo.setText(item.getDescription());

        final int color =getResources().getColor(R.color.white_opaque);
        mEpisodeTitle.setTextColor(color);
        mEpisodeInfo.setTextColor(color);

        long duration = item.getDuration();
        if (duration > 0) {
            mTotalTime.setText(StrUtils.formatTime(duration));
        } else {
            mTotalTime.setText("");
        }

        long offset = item.getOffset();
        if (offset > 0) {
            mCurrentTime.setText(StrUtils.formatTime(offset));
        } else {
            mCurrentTime.setText("");
        }
        mCurrentTime.setEpisode(item);
        mTotalTime.setEpisode(item);

        mPlayPauseButton.setEpisode(item, PlayPauseImageView.LOCATION.PLAYLIST);
        mBackButton.setEpisode(item);
        mForwardButton.setEpisode(item);
        mDownloadButton.setEpisode(item);
        mFavoriteButton.setEpisode(item);

        if (MainActivity.sBoundPlayerService != null &&
                MainActivity.sBoundPlayerService.getCurrentItem() != null &&
                MainActivity.sBoundPlayerService.getCurrentItem().equals(item) &&
                MainActivity.sBoundPlayerService.isPlaying()) {
            mPlayPauseButton.setStatus(PlayerStatusObservable.STATUS.PLAYING);
        } else {
            mPlayPauseButton.setStatus(PlayerStatusObservable.STATUS.PAUSED);
        }
        //mDownloadProgressObservable.registerObserver(mDownloadButton);

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

        //mDownloadProgressObservable.registerObserver(mPlayerDownloadButton);

        final Activity activity = getActivity();

        mTopPlayer.setEpisodeId(item);

        String artworkURL = item.getArtwork(activity);
        if (!TextUtils.isEmpty(artworkURL)) {
            PaletteHelper.generate(artworkURL, activity, mTopPlayer);
            PaletteHelper.generate(artworkURL, activity, mPlayPauseButton);
            PaletteHelper.generate(artworkURL, activity, mBackButton);
            PaletteHelper.generate(artworkURL, activity, mForwardButton);
            PaletteHelper.generate(artworkURL, activity, mDownloadButton);
            PaletteHelper.generate(artworkURL, activity, mFavoriteButton);

            PaletteHelper.generate(artworkURL, activity, new PaletteListener() {
                @Override
                public void onPaletteFound(Palette argChangedPalette) {
                    Palette.Swatch swatch = argChangedPalette.getMutedSwatch();

                    if (swatch == null)
                        return;

                    int colorText = swatch.getTitleTextColor();
                    mEpisodeTitle.setTextColor(color);
                    mEpisodeInfo.setTextColor(color);
                }

                @Override
                public String getPaletteUrl() {
                    return item.getArtwork(activity);
                }
            });
        }

        if (item != null && item.getArtwork(activity) != null) {
            Uri uri = Uri.parse(item.getArtwork(activity));
            mPhoto.setImageURI(uri);
        }

        if (mTopPlayer.getVisibleHeight() == 0) {
            mTopPlayer.setPlayerHeight(mTopPlayer.getMaximumSize());
        }
    }

    private void setPlaylistViewState(@Nullable Playlist argPlaylist) {

        boolean dismissedWelcomePage = sharedPreferences.getBoolean(PLAYLIST_WELCOME_DISMISSED, PLAYLIST_WELCOME_DISMISSED_DEFAULT);

        if (argPlaylist == null || argPlaylist.isEmpty()) {
            mPlaylistContainer.setVisibility(View.GONE);

            if (dismissedWelcomePage) {
                mPlaylistEmptyContainer.setVisibility(View.VISIBLE);
                mPlaylistWelcomeContainer.setVisibility(View.GONE);
            } else {
                mPlaylistWelcomeContainer.setVisibility(View.VISIBLE);
                mPlaylistEmptyContainer.setVisibility(View.GONE);
            }
        } else {
            if (!dismissedWelcomePage) {
                sharedPreferences.edit().putBoolean(PLAYLIST_WELCOME_DISMISSED, true).commit();
            }

            if (mPlaylistContainer == null)
                return;

            mPlaylistContainer.setVisibility(View.VISIBLE);
            mPlaylistWelcomeContainer.setVisibility(View.GONE);
            mPlaylistEmptyContainer.setVisibility(View.GONE);
        }
    }

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
        outState.putLong(mExpandedEpisodeKey, mExpandedEpisodeId);
	}

    @Override
    public boolean onContextItemSelected(MenuItem item) {

        // Handle item selection
        switch (item.getItemId()) {
            case R.id.menu_playlist_context_play_next:
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }


     @Override
     public void onCreateContextMenu(ContextMenu menu, View v,
                                     ContextMenuInfo menuInfo) {
         super.onCreateContextMenu(menu, v, menuInfo);
         MenuInflater inflater = mActivity.getMenuInflater();
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

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		if (key.equals(ApplicationConfiguration.showListenedKey)) {
			//RecentItemFragment.this.getAdapter().notifyDataSetChanged();
		}
	}

    @Subscribe
    public void playlistChanged(@NonNull Playlist argPlaylist) {
        mPlaylist = argPlaylist;

        setPlaylistViewState(mPlaylist);

        if (!mPlaylist.isEmpty()) {
            IEpisode episode = mPlaylist.first();
            bindHeader(episode);
        }
    }
}
