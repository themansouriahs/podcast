package org.bottiger.podcast;

import org.bottiger.podcast.adapters.PlaylistAdapter;
import org.bottiger.podcast.flavors.CrashReporter.VendorCrashReporter;
import org.bottiger.podcast.listeners.PaletteListener;
import org.bottiger.podcast.model.events.EpisodeChanged;
import org.bottiger.podcast.player.SoundWavesPlayer;
import org.bottiger.podcast.player.exoplayer.ExoPlayerWrapper;
import org.bottiger.podcast.playlist.Playlist;
import org.bottiger.podcast.playlist.filters.SubscriptionFilter;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.provider.ISubscription;
import org.bottiger.podcast.service.PlayerService;
import org.bottiger.podcast.utils.ColorExtractor;
import org.bottiger.podcast.utils.ImageLoaderUtils;
import org.bottiger.podcast.utils.PaletteHelper;
import org.bottiger.podcast.utils.StrUtils;
import org.bottiger.podcast.utils.UIUtils;
import org.bottiger.podcast.views.CustomLinearLayoutManager;
import org.bottiger.podcast.views.DownloadButtonView;
import org.bottiger.podcast.views.ImageViewTinted;
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
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
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
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.github.ivbaranov.mfb.MaterialFavoriteButton;

import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;

public class PlaylistFragment extends AbstractEpisodeFragment {

    private static final String TAG = "PlaylistFragment";

    private static final String PLAYLIST_WELCOME_DISMISSED = "playlist_welcome_dismissed";
    private static final boolean PLAYLIST_WELCOME_DISMISSED_DEFAULT = false;

    private View mPlaylistFragmentContainer;
    private View mPlaylistWelcomeContainer;

    private View mPlaylistEmptyContainer;
    private RadioButton mPopulateManually;
    private RadioButton mPopulateAutomatically;

    private TopPlayer mTopPlayer;
    private ImageViewTinted mPhoto;

    private TextView mEpisodeTitle;
    private TextView mEpisodeInfo;
    private TextViewObserver mCurrentTime;
    private TextView mTotalTime;
    private PlayPauseImageView mPlayPauseButton;
    private PlayerSeekbar mPlayerSeekbar;
    private DownloadButtonView mPlayerDownloadButton;
    private MaterialFavoriteButton mFavoriteButton;

    private SoundWavesPlayer mPlayer;

    private RecyclerView mRecyclerView;
    private View mOverlay;

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
    private Subscription mRxTopEpisodeChanged;

    private NestedScrollingChildHelper scrollingChildHelper;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        int color = ContextCompat.getColor(getContext(), mSwipeBgColor);
        mSwipePaint.setColor(color);
        mSwipeIcon = BitmapFactory.decodeResource(getResources(), mSwipeIconID);

        mPlayer = SoundWaves.getAppContext(getContext()).getPlayer();

        super.onCreate(savedInstanceState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onDestroyView() {
        if (mPlayer != null) {
            mPlayer.removeListener(mPlayerSeekbar);
            mPlayer.removeListener(mCurrentTime);
            mPlayer.removeListener(mPlayPauseButton);
        }
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

        mTopPlayer =   (TopPlayer) view.findViewById(R.id.top_player);
        mPhoto =            (ImageViewTinted) view.findViewById(R.id.session_photo);

        mPlaylistFragmentContainer = view.findViewById(R.id.top_coordinator_layout); //view.findViewById(R.id.main_content);
        mPlaylistWelcomeContainer = view.findViewById(R.id.playlist_welcome_screen);

        mPlaylistEmptyContainer = view.findViewById(R.id.playlist_empty);
        mPopulateManually       = (RadioButton) view.findViewById(R.id.radioNone);
        mPopulateAutomatically  = (RadioButton) view.findViewById(R.id.radioAll);

        mEpisodeTitle         =    (TextView) view.findViewById(R.id.player_title);
        mEpisodeInfo         =    (TextView) view.findViewById(R.id.player_podcast);
        mFavoriteButton         = (MaterialFavoriteButton) view.findViewById(R.id.favorite);

        mCurrentTime       =    (TextViewObserver) view.findViewById(R.id.current_time);
        mTotalTime         =    (TextView) view.findViewById(R.id.total_time);

        mPlayPauseButton         =    (PlayPauseImageView) view.findViewById(R.id.playpause);
        mPlayerSeekbar          =    (PlayerSeekbar) view.findViewById(R.id.top_player_seekbar);
        mPlayerDownloadButton   =    (DownloadButtonView) view.findViewById(R.id.download);

        mRecyclerView = (RecyclerView) view.findViewById(R.id.my_recycler_view);
        mOverlay = view.findViewById(R.id.playlist_overlay);

        // use a linear layout manager
        mLayoutManager = new CustomLinearLayoutManager(mContext);
        mRecyclerView.setLayoutManager(mLayoutManager);

        // specify an adapter (see also next example)
        mAdapter = new PlaylistAdapter(getActivity(), mOverlay);
        mAdapter.setHasStableIds(true);

        scrollingChildHelper = new NestedScrollingChildHelper(mPlaylistFragmentContainer);
        scrollingChildHelper.setNestedScrollingEnabled(true);

        mPlayer.addListener(mPlayerSeekbar);
        mPlayer.addListener(mCurrentTime);
        mPlayer.addListener(mPlayPauseButton);

        mPlaylist = SoundWaves.getAppContext(getContext()).getPlaylist();
        setPlaylistViewState(mPlaylist);
        mRecyclerView.setAdapter(mAdapter);

        bindHeaderWrapper(mPlaylist);

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

        // init swipe to dismiss logic
        ItemTouchHelper swipeToDismissTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
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

                int color = playlistViewHolder.hasColor() ? playlistViewHolder.getEpisodePrimaryColor() : getResources().getColor(R.color.colorBgPrimary);
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

                UIUtils.disPlayBottomSnackBar(view, R.string.playlist_episode_dismissed, new View.OnClickListener() {
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
            }
        });
        swipeToDismissTouchHelper.attachToRecyclerView(mRecyclerView);
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
        mRxPlaylistSubscription = SoundWaves.getPlaylistObservabel()
                .subscribe(new Action1<Playlist>() {
                    @Override
                    public void call(Playlist playlistChanged) {
                        Log.d(TAG, "mRxPlaylistSubscription event recieved");
                        playlistChanged(playlistChanged);
                        mTopPlayer.togglePlaylistEmpty(playlistChanged.size() == 1);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        VendorCrashReporter.report("subscribeError" , throwable.toString());
                        Log.d(TAG, "error: " + throwable.toString());
                    }
                });
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

            if (item != null) {
                if (mPlayPauseButton != null)
                    mPlayPauseButton.setEpisode(item, PlayPauseImageView.PLAYLIST);

                if (mPlayerDownloadButton != null) {
                    mPlayerDownloadButton.setEpisode(item);
                    mPlayerDownloadButton.enabledProgressListener(true);
                }
            }
            playlistChanged(mPlaylist);
        }

        super.onResume();
    }


    public void bindHeader(final IEpisode item) {

        if (mEpisodeTitle == null)
            return;

        if (mRxTopEpisodeChanged != null && !mRxTopEpisodeChanged.isUnsubscribed()) {
            mRxTopEpisodeChanged.unsubscribe();
        }

        mRxTopEpisodeChanged = SoundWaves.getRxBus()
                .toObserverable()
                .onBackpressureDrop()
                .ofType(EpisodeChanged.class)
                .filter(new Func1<EpisodeChanged, Boolean>() {
                    @Override
                    public Boolean call(EpisodeChanged episodeChanged) {
                        return episodeChanged.getAction() != EpisodeChanged.PROGRESS;
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<EpisodeChanged>() {
                    @Override
                    public void call(EpisodeChanged itemChangedEvent) {
                        long episodeId = itemChangedEvent.getId();
                        IEpisode episode = SoundWaves.getAppContext(getContext()).getLibraryInstance().getEpisode(episodeId);

                        if (episode == null)
                            return;

                        if (episode.equals(mPlaylist.first())) {
                            PlaylistFragment.this.bindHeader(episode);
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        VendorCrashReporter.handleException(throwable);
                        Log.wtf(TAG, "Missing back pressure. Should not happen anymore :(");
                    }
                });

        final String title = item.getTitle();
        final String description = item.getDescription();

        mEpisodeTitle.setText(title);
        mEpisodeInfo.setText(description);

        View.OnClickListener onClickListener = getToast(title, description);
        mEpisodeTitle.setOnClickListener(onClickListener);
        mEpisodeInfo.setOnClickListener(onClickListener);

        if (item instanceof FeedItem) {
            mFavoriteButton.setFavorite(((FeedItem) item).isFavorite());
        }

        final int color = UIUtils.attrColor(R.attr.themeTextColorPrimary, mContext);
        mEpisodeTitle.setTextColor(color);
        mEpisodeInfo.setTextColor(color);

        long duration = item.getDuration();
        if (duration > 0) {
            mTotalTime.setText(StrUtils.formatTime(duration));
        } else {
            mTotalTime.setText("");
        }

        setPlayerProgress(item);

        mPlayPauseButton.setEpisode(item, PlayPauseImageView.PLAYLIST);
        mPlayerDownloadButton.setEpisode(item);

        mPlayPauseButton.setStatus(ExoPlayerWrapper.STATE_READY);

        ISubscription iSubscription = item.getSubscription(getContext());
        if (iSubscription instanceof org.bottiger.podcast.provider.Subscription) {
            org.bottiger.podcast.provider.Subscription subscription = (org.bottiger.podcast.provider.Subscription)iSubscription;
            mTopPlayer.setPlaybackSpeedView(subscription.getPlaybackSpeed());
        }

        mPlayerSeekbar.setEpisode(item);
        mPlayerSeekbar.setOverlay(mOverlay);
        mPlayerSeekbar.getProgressDrawable().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);

        mPlayerDownloadButton.setEpisode(item);

        final Activity activity = getActivity();
        String artworkURL = item.getArtwork(getContext());

        if (iSubscription != null && !TextUtils.isEmpty(iSubscription.getImageURL())) {
            artworkURL = iSubscription.getImageURL();
        }

        if (!TextUtils.isEmpty(artworkURL)) {

            PaletteHelper.generate(artworkURL, activity, mTopPlayer);
            PaletteHelper.generate(artworkURL, activity, new PaletteListener() {
                @Override
                public void onPaletteFound(Palette argChangedPalette) {
                    Palette.Swatch swatch = argChangedPalette.getMutedSwatch();

                    if (swatch == null)
                        return;

                    int colorBackground = swatch.getRgb();

                    ColorExtractor extractor = new ColorExtractor(getActivity(), argChangedPalette);

                    mPlayPauseButton.onPaletteFound(argChangedPalette);

                    int transparentgradientColor;
                    int gradientColor = extractor.getPrimary();

                    int alpha = 0;
                    int red = Color.red(gradientColor);
                    int green = Color.green(gradientColor);
                    int blue = Color.blue(gradientColor);
                    transparentgradientColor = Color.argb(alpha, red, green, blue);

                    GradientDrawable gd = new GradientDrawable(
                            GradientDrawable.Orientation.TOP_BOTTOM,
                            new int[]{transparentgradientColor, gradientColor});
                    Drawable wrapDrawable = DrawableCompat.wrap(gd);
                    DrawableCompat.setTint(wrapDrawable, colorBackground);
                }

                @Override
                public String getPaletteUrl() {
                    return item.getArtwork(getContext());
                }
            });


            Log.v("MissingImage", "Setting image");
            ImageLoaderUtils.loadImageInto(mPhoto, artworkURL, null, false, false, false);

        }

        if (mTopPlayer.isFullscreen()) {
            mTopPlayer.setFullscreen(true, false);
        } else if (mPlaylist.size() == 1) {
            mTopPlayer.togglePlaylistEmpty(true);
        }
    }

    private void setPlayerProgress(@NonNull IEpisode argEpisode) {
        long offset = argEpisode.getOffset();
        if (offset > 0) {
            mCurrentTime.setText(StrUtils.formatTime(offset));
        } else {
            mCurrentTime.setText("00:00");
        }
        mCurrentTime.setEpisode(argEpisode);
    }

    private void setPlaylistViewState(@Nullable Playlist argPlaylist) {

        boolean dismissedWelcomePage = sharedPreferences.getBoolean(PLAYLIST_WELCOME_DISMISSED, PLAYLIST_WELCOME_DISMISSED_DEFAULT);

        if (argPlaylist == null || argPlaylist.isEmpty()) {
            mTopPlayer.setVisibility(View.GONE);

            if (dismissedWelcomePage) {
                mPlaylistEmptyContainer.setVisibility(View.VISIBLE);
                mPlaylistWelcomeContainer.setVisibility(View.GONE);
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
         MenuInflater inflater = getActivity().getMenuInflater();
     }

     @Override
     public void onCreateOptionsMenu(final Menu menu, MenuInflater inflater) {
         inflater.inflate(R.menu.playlist_options_menu, menu);
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
         }
         return super.onOptionsItemSelected(item);
     }

    public void playlistChanged(@NonNull Playlist argPlaylist) {

        mPlaylist = argPlaylist;

        if (mTopPlayer == null)
            return;

        setPlaylistViewState(mPlaylist);

        bindHeaderWrapper(mPlaylist);
    }

    private void bindHeaderWrapper(@Nullable Playlist argPlaylist) {

        if (argPlaylist != null && !argPlaylist.isEmpty()) {
            IEpisode episode = argPlaylist.first();
            bindHeader(episode);
        }
    }

    private View.OnClickListener getToast(@NonNull final String argTitle, @NonNull final String argDescription) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View argView) {
                String msg = argTitle + "\n\n" + argDescription;
                // HACK: ugly, but it does actually work :)
                // double the lifetime of a toast
                for (int i = 0; i < 2; i++) {
                    Toast toast = Toast.makeText(getContext(), msg, Toast.LENGTH_LONG);
                    toast.show();
                }
            }
        };
    }
}
