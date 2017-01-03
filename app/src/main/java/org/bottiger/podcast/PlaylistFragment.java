package org.bottiger.podcast;

import org.bottiger.podcast.adapters.PlaylistAdapter;
import org.bottiger.podcast.flavors.CrashReporter.VendorCrashReporter;
import org.bottiger.podcast.listeners.NewPlayerEvent;
import org.bottiger.podcast.listeners.PaletteListener;
import org.bottiger.podcast.model.events.EpisodeChanged;
import org.bottiger.podcast.player.GenericMediaPlayerInterface;
import org.bottiger.podcast.player.SoundWavesPlayer;
import org.bottiger.podcast.player.exoplayer.ExoPlayerWrapper;
import org.bottiger.podcast.playlist.Playlist;
import org.bottiger.podcast.playlist.filters.SubscriptionFilter;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.provider.ISubscription;
import org.bottiger.podcast.provider.base.BaseSubscription;
import org.bottiger.podcast.utils.ColorExtractor;
import org.bottiger.podcast.utils.ImageLoaderUtils;
import org.bottiger.podcast.utils.PaletteHelper;
import org.bottiger.podcast.utils.PlayerHelper;
import org.bottiger.podcast.utils.StrUtils;
import org.bottiger.podcast.utils.UIUtils;
import org.bottiger.podcast.utils.chapter.Chapter;
import org.bottiger.podcast.utils.chapter.ChapterReader;
import org.bottiger.podcast.utils.id3reader.ID3ReaderException;
import org.bottiger.podcast.views.CustomLinearLayoutManager;
import org.bottiger.podcast.views.DownloadButtonView;
import org.bottiger.podcast.views.ImageViewTinted;
import org.bottiger.podcast.views.Overlay;
import org.bottiger.podcast.views.PlayPauseImageView;
import org.bottiger.podcast.views.PlayerSeekbar;
import org.bottiger.podcast.views.PlaylistViewHolder;
import org.bottiger.podcast.views.TextViewObserver;
import org.bottiger.podcast.views.TopPlayer;
import org.bottiger.podcast.views.dialogs.DialogBulkDownload;
import org.bottiger.podcast.views.dialogs.DialogPlaylistFilters;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.NestedScrollingChildHelper;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.github.ivbaranov.mfb.MaterialFavoriteButton;

import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;

import static org.bottiger.podcast.player.SoundWavesPlayerBase.STATE_READY;

public class PlaylistFragment extends AbstractEpisodeFragment {

    private static final String TAG = PlaylistFragment.class.getSimpleName();

    private static final String PLAYLIST_WELCOME_DISMISSED = "playlist_welcome_dismissed";
    private static final boolean PLAYLIST_WELCOME_DISMISSED_DEFAULT = false;

    private View mPlaylistFragmentContainer;
    private View mPlaylistWelcomeContainer;

    private View mPlaylistEmptyContainer;
    private RadioButton mPopulateManually;
    private RadioButton mPopulateAutomatically;

    private ViewStub mTopPlayerStub;
    private ViewStub mRecyclerStub;

    @Nullable private TopPlayer mTopPlayer;
    @Nullable private ImageView mPhoto;

    private GenericMediaPlayerInterface mPlayer;

    private RecyclerView mRecyclerView;
    private Overlay mOverlay;

	/** ID of the current expanded episode */
	private long mExpandedEpisodeId = -1;
	private String mExpandedEpisodeKey = "currentExpanded";

	// ItemTouchHelperResources
    private Paint mSwipePaint = new Paint();
    private Paint mSwipeIconPaint = new Paint();
    private Bitmap mSwipeIcon;
    private int mSwipeBgColor = R.color.colorBgPrimaryDark;
    private int mSwipeIconID = R.drawable.ic_hearing_white;

    private Playlist mPlaylist;
    private Context mContext;

    private Subscription mRxPlaylistSubscription;

    private NestedScrollingChildHelper scrollingChildHelper;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        int color = ContextCompat.getColor(getContext(), mSwipeBgColor);
        mSwipePaint.setColor(color);
        mSwipeIcon = BitmapFactory.decodeResource(getResources(), mSwipeIconID);

        mRxPlaylistSubscription = getPlaylistChangedSubscription();

        super.onCreate(savedInstanceState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mTopPlayer != null) {
            mTopPlayer.onDestroyView();
        }

        if (mRxPlaylistSubscription != null && !mRxPlaylistSubscription.isUnsubscribed()) {
            mRxPlaylistSubscription.unsubscribe();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

	// Read here:
	// http://developer.android.com/reference/android/app/Fragment.html#Layout
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

		if (savedInstanceState != null) {
			// Restore last state for checked position.
			mExpandedEpisodeId = savedInstanceState.getLong(
					mExpandedEpisodeKey, mExpandedEpisodeId);
		}
	}

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        super.onViewCreated(view,savedInstanceState);

        mTopPlayerStub = (ViewStub) view.findViewById(R.id.stub_top_player);
        mRecyclerStub = (ViewStub) view.findViewById(R.id.stub_playlist);

        mPlaylistFragmentContainer = view.findViewById(R.id.top_coordinator_layout); //view.findViewById(R.id.main_content);
        mPlaylistWelcomeContainer = view.findViewById(R.id.playlist_welcome_screen);

        mPlaylistEmptyContainer = view.findViewById(R.id.playlist_empty);
        mPopulateManually       = (RadioButton) view.findViewById(R.id.radioNone);
        mPopulateAutomatically  = (RadioButton) view.findViewById(R.id.radioAll);

        mOverlay = (Overlay) view.findViewById(R.id.playlist_overlay);

        scrollingChildHelper = new NestedScrollingChildHelper(mPlaylistFragmentContainer);
        scrollingChildHelper.setNestedScrollingEnabled(true);

        mPlaylist = SoundWaves.getAppContext(getContext()).getPlaylist();

        setPlaylistViewState(mPlaylist);

        mPopulateManually.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    Playlist.changePlaylistFilter(getContext(), mPlaylist, SubscriptionFilter.SHOW_NONE);
                }
            }
        });

        mPopulateAutomatically.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    Playlist.changePlaylistFilter(getContext(), mPlaylist, SubscriptionFilter.SHOW_ALL);
                }
            }
        });
    }

    @Override
    public void onAttach(Context context) {
        mContext = context;
        super.onAttach(context);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        Log.v(TAG, "inflatetime: " + System.currentTimeMillis());
        View view = inflater.inflate(R.layout.playlist_fragment_main, container, false);
        Log.v(TAG, "inflatetime2: " + System.currentTimeMillis());
        return view;
    }

    @Override
    public void onStart() {
        Log.d(TAG, "onStart");
        super.onStart();

        Log.d(TAG, "mRxPlaylistSubscription isUnsubscribed: " + mRxPlaylistSubscription.isUnsubscribed());
    }

    @Override
    public void onStop() {
        Log.d(TAG, "onStop");
        super.onStop();
    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();
    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume");

        if (mPlaylist != null) {
            IEpisode item = mPlaylist.getItem(0);

            if (mTopPlayer != null) {
                mTopPlayer.bind(item);
            }
            playlistChanged(mPlaylist);
        }

        super.onResume();
    }


    private void bindHeader(@NonNull TopPlayer argTopPlayer, @Nullable final IEpisode item) {

        argTopPlayer.bind(item);

        if (item == null)
            return;

        if (argTopPlayer.isFullscreen()) {
            argTopPlayer.setFullscreen(true, false);
        } else  {
            /* If the player is not fullscreen, but the playlist is empty */
            argTopPlayer.setPlaylistEmpty(mPlaylist.size() == 1);
        }
    }

    private void setPlaylistViewState(@NonNull Playlist argPlaylist) {

        boolean dismissedWelcomePage = sharedPreferences.getBoolean(PLAYLIST_WELCOME_DISMISSED, PLAYLIST_WELCOME_DISMISSED_DEFAULT);

        if (argPlaylist.isEmpty()) {
            if (mTopPlayer != null)
                mTopPlayer.setVisibility(View.GONE);

            if (dismissedWelcomePage) {
                if (argPlaylist.isLoaded()) {
                    mPlaylistEmptyContainer.setVisibility(View.VISIBLE);
                    mPlaylistWelcomeContainer.setVisibility(View.GONE);
                }
            } else {
                mPlaylistWelcomeContainer.setVisibility(View.VISIBLE);
                mPlaylistEmptyContainer.setVisibility(View.GONE);
            }
        } else {
            if (!dismissedWelcomePage) {
                sharedPreferences.edit().putBoolean(PLAYLIST_WELCOME_DISMISSED, true).apply();
            }

            if (mTopPlayer == null)
                return;

            mTopPlayer.setVisibility(View.VISIBLE);
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
        switch (item.getItemId()) {
            case R.id.menu_playlist_context_play_next:
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

     @Override
     public void onCreateOptionsMenu(final Menu menu, MenuInflater inflater) {
         inflater.inflate(R.menu.playlist_options_menu, menu);

         if (mTopPlayer != null && mTopPlayer.isFullscreen()) {
             MenuItem menuItem = menu.findItem(R.id.action_fullscreen_player);
             menuItem.setIcon(R.drawable.ic_fullscreen_exit_white_24px);
             menuItem.setTitle(R.string.action_exit_fullscreen);
         }

         super.onCreateOptionsMenu(menu, inflater);
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
             case R.id.action_filter_playlist: {
                 DialogPlaylistFilters dialogPlaylistFilters = DialogPlaylistFilters.newInstance();
                 dialogPlaylistFilters.show(getFragmentManager(), getTag());
                 return true;
             }
             case R.id.action_fullscreen_player: {
                 boolean shouldBeFullscreen = false;
                 if (mTopPlayer != null) {
                     shouldBeFullscreen = !mTopPlayer.isFullscreen();
                     mTopPlayer.setFullscreen(shouldBeFullscreen, true);
                 }

                 if (!shouldBeFullscreen) {
                     inflatePlaylist(mPlaylistFragmentContainer);
                 }
                 getActivity().invalidateOptionsMenu();
                 return true;
             }
         }
         return super.onOptionsItemSelected(item);
     }

    public synchronized void playlistChanged(@NonNull Playlist argPlaylist) {

        mPlaylist = argPlaylist;

        setPlaylistViewState(mPlaylist);

        if (showTopPlayer(mPlaylist)) {
            if (mTopPlayerStub != null) {
                inflateTopPlayer(getView());
            }

            bindHeaderWrapper(mPlaylist);
        }

        if (showPlaylist(mPlaylist)) {
            if (mRecyclerStub != null) {
                inflatePlaylist(getView());
            }

            mAdapter.notifyDataSetChanged();
        }
    }

    private boolean showTopPlayer(@NonNull Playlist argPlaylist) {
        return argPlaylist.size() > 0;
    }

    private boolean showPlaylist(@NonNull Playlist argPlaylist) {
        boolean hasContent = argPlaylist.size() > 1;
        boolean inFullscreenMode = mTopPlayer != null && mTopPlayer.isFullscreen();
        return hasContent && !inFullscreenMode;
    }

    private void bindHeaderWrapper(@Nullable Playlist argPlaylist) {
        if (argPlaylist != null && mTopPlayer != null) {
            IEpisode episode = argPlaylist.first();
            bindHeader(mTopPlayer, episode);
        }
    }

    private synchronized void inflateTopPlayer(@NonNull View view) {

        if (mTopPlayerStub == null)
            return;

        mTopPlayer = (TopPlayer) mTopPlayerStub.inflate();
        mPhoto =            (ImageView) view.findViewById(R.id.session_photo);

        assert mTopPlayer != null;
        mTopPlayer.setOverlay(mOverlay);

        bindHeaderWrapper(mPlaylist);

        mTopPlayerStub = null;

        if (mTopPlayer.isFullscreen()) {
            getActivity().invalidateOptionsMenu();
        }
    }

    private synchronized void inflatePlaylist(@NonNull View view) {

        if (mRecyclerStub == null)
            return;

        mRecyclerView = (RecyclerView) mRecyclerStub.inflate();

        // use a linear layout manager
        mLayoutManager = new CustomLinearLayoutManager(mContext);
        mRecyclerView.setLayoutManager(mLayoutManager);

        mRecyclerView.setPadding(0, 0, 0, UIUtils.NavigationBarHeight(getContext()));

        // specify an adapter (see also next example)
        mAdapter = new PlaylistAdapter(getActivity(), mOverlay);
        mAdapter.setHasStableIds(true);

        mRecyclerView.setAdapter(mAdapter);

        // init swipe to dismiss logic
        ItemTouchHelper swipeToDismissTouchHelper = getItemTouchHelper(view);
        swipeToDismissTouchHelper.attachToRecyclerView(mRecyclerView);

        mRecyclerStub = null;
    }

    private Subscription getPlaylistChangedSubscription() {
        return SoundWaves
                .getRxBus()
                .toObserverable()
                .onBackpressureLatest()
                .observeOn(AndroidSchedulers.mainThread())
                .ofType(Playlist.class)
                .subscribe(new Action1<Playlist>() {
                    @Override
                    public void call(Playlist playlistChanged) {
                        Log.d(TAG, "notifyPlaylistChanged: mRxPlaylistSubscription event recieved");
                        playlistChanged(playlistChanged);
                        mTopPlayer.setPlaylistEmpty(playlistChanged.size() == 1);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Log.wtf(TAG, "ERROR: notifyPlaylistChanged: mRxPlaylistSubscription event recieved");
                        VendorCrashReporter.report("subscribeError" , throwable.toString());
                        Log.wtf(TAG, "error: " + throwable.toString());
                    }
                });
    }

    @NonNull
     private ItemTouchHelper getItemTouchHelper(@NonNull final View argView) {
         return new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                 ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT, ItemTouchHelper.RIGHT) {

             @Override
             public void onChildDraw(Canvas c,
                                     RecyclerView recyclerView,
                                     RecyclerView.ViewHolder viewHolder,
                                     float dX,
                                     float dY,
                                     int actionState,
                                     boolean isCurrentlyActive) {

                 if (actionState != ItemTouchHelper.ACTION_STATE_SWIPE)
                     return;

                 PlaylistViewHolder playlistViewHolder = null;
                 if (viewHolder instanceof PlaylistViewHolder) {
                     playlistViewHolder = (PlaylistViewHolder) viewHolder;
                 }

                 if (playlistViewHolder == null) {
                     Log.wtf(TAG, "playlistViewHolder should never be null"); // NoI18N
                     return;
                 }

                 // http://stackoverflow.com/questions/30820806/adding-a-colored-background-with-text-icon-under-swiped-row-when-using-androids
                 View itemView = viewHolder.itemView;

                 @ColorInt int color = playlistViewHolder.hasColor() ? playlistViewHolder.getEpisodePrimaryColor() : ContextCompat.getColor(mContext, R.color.colorBgPrimary);
                 mSwipePaint.setColor(color);

                 c.drawRect((float) itemView.getLeft(), (float) itemView.getTop(), dX,
                         (float) itemView.getBottom(), mSwipePaint);

                 int height2 = mSwipeIcon.getHeight()/2;
                 int heightView = itemView.getHeight();
                 int bitmapTopPos = heightView/2-height2+itemView.getTop();

                 int bitmapLeftPos = (int)UIUtils.convertDpToPixel(25, getContext());

                 c.drawBitmap(mSwipeIcon, bitmapLeftPos, bitmapTopPos, mSwipeIconPaint);

                 super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);

             }

             @Override
             public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                 // callback for drag-n-drop, false to skip this feature
                 return false;
             }

             @Override
             public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                 // callback for swipe to dismiss, removing item from data and adapter
                 //items.remove(viewHolder.getAdapterPosition());

                 PlaylistViewHolder playlistViewHolder = null;
                 if (viewHolder instanceof PlaylistViewHolder) {
                     playlistViewHolder = (PlaylistViewHolder) viewHolder;
                 }

                 if (playlistViewHolder == null) {
                     Log.wtf(TAG, "playlistViewHolder should never be null"); // NoI18N
                     return;
                 }

                 final int itemPosition = playlistViewHolder.getAdapterPosition()+1;
                 final IEpisode episode = mPlaylist.getItem(itemPosition);
                 final int currentPriority = episode.getPriority();

                 episode.setPriority(-1);

                 if (episode instanceof FeedItem) {
                     FeedItem item = (FeedItem) episode;
                     item.markAsListened();
                 }

                 mPlaylist.removeItem(itemPosition, true);

                 UIUtils.disPlayBottomSnackBar(argView, R.string.playlist_episode_dismissed, new View.OnClickListener() {
                     @Override
                     public void onClick(View v) {
                         episode.setPriority(currentPriority);
                         mPlaylist.setItem(itemPosition, episode);
                         mAdapter.notifyDataSetChanged();

                         if (episode instanceof FeedItem) {
                             FeedItem item = (FeedItem) episode;
                             item.markAsListened(0);
                         }

                         SoundWaves.getAppContext(getContext()).getLibraryInstance().updateEpisode(episode);
                     }
                 }, false);

                 mAdapter.notifyItemRemoved(itemPosition-1);

                 // This doesn't work as intended
                 if (mPlaylist.size() == 1) {
                     mTopPlayer.setPlaylistEmpty(true);
                 }
             }
         });
     }
}
