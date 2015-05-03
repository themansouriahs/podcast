package org.bottiger.podcast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.bottiger.podcast.adapters.PlaylistContentSpinnerAdapter;
import org.bottiger.podcast.playlist.Playlist;
import org.bottiger.podcast.provider.Subscription;
import org.bottiger.podcast.service.PlayerService;
import org.bottiger.podcast.utils.navdrawer.NavigationDrawerMenuGenerator;
import org.bottiger.podcast.views.MultiSpinner;
import org.bottiger.podcast.views.PlaylistContentSpinner;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.TypeEvaluator;
import android.app.Fragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ExpandableListView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TableLayout;

import com.android.volley.toolbox.ImageLoader;

/**
 * This example illustrates a common usage of the DrawerLayout widget in the
 * Android support library.
 * <p/>
 * <p>
 * When a navigation (left) drawer is present, the host activity should detect
 * presses of the action bar's Up affordance as a signal to open and close the
 * navigation drawer. The ActionBarDrawerToggle facilitates this behavior. Items
 * within the drawer should fall into one of two categories:
 * </p>
 * <p/>
 * <ul>
 * <li><strong>View switches</strong>. A view switch follows the same basic
 * policies as list or tab navigation in that a view switch does not create
 * navigation history. This pattern should only be used at the root activity of
 * a task, leaving some form of Up navigation active for activities further down
 * the navigation hierarchy.</li>
 * <li><strong>Selective Up</strong>. The drawer allows the user to choose an
 * alternate parent for Up navigation. This allows a user to jump across an
 * app's navigation hierarchy at will. The application should treat this as it
 * treats Up navigation from a different task, replacing the current task stack
 * using TaskStackBuilder or similar. This is the only form of navigation drawer
 * that should be used outside of the root activity of a task.</li>
 * </ul>
 * <p/>
 * <p>
 * Right side drawers should be used for actions, not navigation. This follows
 * the pattern established by the Action Bar that navigation should be to the
 * left and actions to the right. An action should be an operation performed on
 * the current contents of the window, for example enabling or disabling a data
 * overlay on top of the current content.
 * </p>
 */
public abstract class DrawerActivity extends MediaRouterPlaybackActivity {

    protected SharedPreferences mSharedPreferences;

    protected DrawerLayout mDrawerLayout;
    protected RelativeLayout mDrawerMainContent;
	protected ExpandableListView mDrawerList;
	protected FrameLayout mDrawerContainer;
    protected TableLayout mDrawerTable;

    protected Switch mPlaylistShowListened;
    protected Switch mAutoPlayNext;
    protected Spinner mPlaylistOrderSpinner;
    protected PlaylistContentSpinner mPlaylistContentSpinner;
    protected PlaylistContentSpinnerAdapter mPlaylistContentSpinnerAdapter;

	protected ActionBarDrawerToggle mDrawerToggle;
	protected String[] mListItems;

	private CharSequence mDrawerTitle;
	private CharSequence mTitle;

    // variables that control the Action Bar auto hide behavior (aka "quick recall")
    private boolean mActionBarAutoHideEnabled = true;
    private int mActionBarAutoHideSensivity = 0;
    private int mActionBarAutoHideMinY = 0;
    private int mActionBarAutoHideSignal = 0;
    private boolean mActionBarShown = true;

    // Durations for certain animations we use:
    private static final int HEADER_HIDE_ANIM_DURATION = 300;
    private static final int ACCOUNT_BOX_EXPAND_ANIM_DURATION = 200;

    private ObjectAnimator mStatusBarColorAnimator;
    private LinearLayout mAccountListContainer;
    private ViewGroup mDrawerItemsListContainer;

    // When set, these components will be shown/hidden in sync with the action bar
    // to implement the "quick recall" effect (the Action Bar and the header views disappear
    // when you scroll down a list, and reappear quickly when you scroll up).
    private ArrayList<View> mHideableHeaderViews = new ArrayList<View>();

    // SwipeRefreshLayout allows the user to swipe the screen down to trigger a manual refresh
    private SwipeRefreshLayout mSwipeRefreshLayout;

    private int mThemedStatusBarColor;
    private int mProgressBarTopWhenActionBarShown;
    private static final TypeEvaluator ARGB_EVALUATOR = new ArgbEvaluator();
    private ImageLoader mImageLoader;

	private ArrayList<String> parentItems = new ArrayList<String>();
	private ArrayList<Object> childItems = new ArrayList<Object>();


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

		mTitle = mDrawerTitle = getTitle();
		mListItems = getResources().getStringArray(R.array.drawer_menu);
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerMainContent = (RelativeLayout) findViewById(R.id.outer_container);
		mDrawerContainer = (FrameLayout) findViewById(R.id.drawer_container);
        mDrawerTable = (TableLayout) findViewById(R.id.drawer_table);

        // if we can use windowTranslucentNavigation=true
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            FrameLayout.MarginLayoutParams params = (FrameLayout.MarginLayoutParams) mDrawerTable.getLayoutParams();
            params.topMargin = getStatusBarHeight(getResources());

            RelativeLayout.MarginLayoutParams params2 = (RelativeLayout.MarginLayoutParams) mDrawerMainContent.getLayoutParams();
            params2.topMargin = getStatusBarHeight(getResources());

            mDrawerMainContent.setLayoutParams(params2);
            mDrawerTable.setLayoutParams(params);
        }

        final Playlist playlist = PlayerService.getPlaylist();
        playlist.setContext(this);

        mPlaylistContentSpinner = (PlaylistContentSpinner) findViewById(R.id.drawer_playlist_source);
        mPlaylistOrderSpinner = (Spinner) findViewById(R.id.drawer_playlist_sort_order);
        mPlaylistShowListened = (Switch) findViewById(R.id.slidebar_show_listened);
        mAutoPlayNext = (Switch) findViewById(R.id.slidebar_show_continues);

        mPlaylistContentSpinner.setSubscriptions(playlist);

        parentItems = new ArrayList<String>(Arrays.asList(mListItems));
		// setGroupParents();
		setChildData();

        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapterSortOrder = ArrayAdapter.createFromResource(this,
                R.array.playlist_sort_order, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapterSortOrder.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        mPlaylistOrderSpinner.setAdapter(adapterSortOrder);
        mPlaylistOrderSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    playlist.setSortOrder(Playlist.SORT.DATE_NEW); // new first
                } else {
                    playlist.setSortOrder(Playlist.SORT.DATE_OLD); // old first
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                return;
            }
        });

        // Show listened
        mPlaylistShowListened.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                playlist.setShowListened(isChecked);
            }
        });
        boolean doShowListened = mSharedPreferences.getBoolean(ApplicationConfiguration.showListenedKey, Playlist.SHOW_LISTENED_DEFAULT);
        if (doShowListened != mPlaylistShowListened.isChecked()) {
            mPlaylistShowListened.setChecked(doShowListened);
        }

        initAutoPlayNextSwitch();

        LinearLayout layout = (LinearLayout) findViewById(R.id.drawer_items);
        NavigationDrawerMenuGenerator navigationDrawerMenuGenerator = new NavigationDrawerMenuGenerator(this);
        navigationDrawerMenuGenerator.generate(layout);


        // enable ActionBar app icon to behave as action to toggle nav drawer
        //getActionBar().setDisplayHomeAsUpEnabled(true);
        //getActionBar().setHomeButtonEnabled(true);

        // ActionBarDrawerToggle ties together the the proper interactions
        // between the sliding drawer and the action bar app icon
        mDrawerToggle = new ActionBarDrawerToggle(this, /* host Activity */
                mDrawerLayout, /* DrawerLayout object */
                mToolbar, //R.drawable.ic_drawer, /* nav drawer image to replace 'Up' caret */
                R.string.drawer_open, /* "open drawer" description for accessibility */
                R.string.drawer_close /* "close drawer" description for accessibility */
        ) {
            public void onDrawerClosed(View view) {
                //getActionBar().setTitle(mTitle);
                invalidateOptionsMenu(); // creates call to
                // onPrepareOptionsMenu()
            }

            public void onDrawerOpened(View drawerView) {
                //getActionBar().setTitle(mDrawerTitle);
                invalidateOptionsMenu(); // creates call to
                // onPrepareOptionsMenu()
            }
        };

        mDrawerLayout.setDrawerListener(mDrawerToggle);
        mDrawerLayout.setScrimColor(Color.TRANSPARENT);


		/*
		 * if (savedInstanceState == null) { selectItem(0); }
		 */
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		/*
		 * MenuInflater inflater = getMenuInflater();
		 * inflater.inflate(R.menu.main, menu);
		 */
		return super.onCreateOptionsMenu(menu);
	}

	/* Called whenever we call invalidateOptionsMenu() */
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		// If the nav drawer is open, hide action items related to the content
		// view
	    boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerContainer);
		/*
		 * menu.findItem(R.id.action_websearch).setVisible(!drawerOpen);
		 */
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// The action bar home/up action should open or close the drawer.
		// ActionBarDrawerToggle will take care of this.
		if (mDrawerToggle.onOptionsItemSelected(item)) {
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	/* The click listner for ListView in the navigation drawer */
	private class DrawerItemClickListener implements
			ListView.OnItemClickListener {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			selectItem(position);
		}
	}

	public void selectItem(int position) {
		setTitle(mListItems[position]);
		mDrawerLayout.closeDrawer(mDrawerContainer);

		if (position == 1) {
			//Intent intent = new Intent(this, DownloadActivity.class);
			//startActivity(intent);
		}
	}

	/*
	 * public abstract void selectItem(int position);
	 * 
	 * private void selectItem(int position) { // update the main content by
	 * replacing fragments Fragment fragment = new PlanetFragment(); Bundle args
	 * = new Bundle(); args.putInt(PlanetFragment.ARG_PLANET_NUMBER, position);
	 * fragment.setArguments(args);
	 * 
	 * FragmentManager fragmentManager = getFragmentManager();
	 * 
	 * 
	 * //fragmentManager.beginTransaction() // .replace(R.id.content_frame,
	 * fragment).commit();
	 * 
	 * 
	 * // update selected item and title, then close the drawer
	 * mDrawerList.setItemChecked(position, true);
	 * setTitle(mListItems[position]);
	 * mDrawerLayout.closeDrawer(mDrawerContainer); }
	 */

	@Override
	public void setTitle(CharSequence title) {
		mTitle = title;
		//getActionBar().setTitle(mTitle);
	}

	/**
	 * When using the ActionBarDrawerToggle, you must call it during
	 * onPostCreate() and onConfigurationChanged()...
	 */

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		// Sync the toggle state after onRestoreInstanceState has occurred.
		mDrawerToggle.syncState();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		// Pass any configuration change to the drawer toggls
		mDrawerToggle.onConfigurationChanged(newConfig);
	}
    @Override
    public void onResume() {
        initAutoPlayNextSwitch();
        super.onResume();
    }

	/**
	 * Fragment that appears in the "content_frame", shows a planet
	 */
	public static class PlanetFragment extends Fragment {
		public static final String ARG_PLANET_NUMBER = "planet_number";

		public PlanetFragment() {
			// Empty constructor required for fragment subclasses
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			/*
			 * View rootView = inflater.inflate(R.layout.fragment_planet,
			 * container, false); int i =
			 * getArguments().getInt(ARG_PLANET_NUMBER); String planet =
			 * getResources() .getStringArray(R.array.planets_array)[i];
			 * 
			 * int imageId = getResources().getIdentifier(
			 * planet.toLowerCase(Locale.getDefault()), "drawable",
			 * getActivity().getPackageName()); ((ImageView)
			 * rootView.findViewById(R.id.image)) .setImageResource(imageId);
			 * getActivity().setTitle(planet); return rootView;
			 */
			return null;
		}
	}

	public void setChildData() {

		// Android
		ArrayList<String> child = new ArrayList<String>();
		child.add("Refactor this");
		childItems.add(child);

		// Core Java child = new ArrayList<String>(); child.add("Apache");
		//child.add("Applet");
		//child.add("AspectJ");
		//child.add("Beans");
		//child.add("Crypto");
		//childItems.add(child);
		/*
		 * // Desktop Java child = new ArrayList<String>();
		 * child.add("Accessibility"); child.add("AWT"); child.add("ImageIO");
		 * child.add("Print"); childItems.add(child);
		 * 
		 * // Enterprise Java child = new ArrayList<String>();
		 * child.add("EJB3"); child.add("GWT"); child.add("Hibernate");
		 * child.add("JSP"); childItems.add(child);
		 */
	}






    // Google IO

    /**
     * Initializes the Action Bar auto-hide (aka Quick Recall) effect.
     */
    private void initActionBarAutoHide() {
        mActionBarAutoHideEnabled = true;
        mActionBarAutoHideMinY = getResources().getDimensionPixelSize(
                R.dimen.action_bar_auto_hide_min_y);
        mActionBarAutoHideSensivity = getResources().getDimensionPixelSize(
                R.dimen.action_bar_auto_hide_sensivity);
    }

    protected void enableActionBarAutoHide(final ListView listView) {
        initActionBarAutoHide();
        listView.setOnScrollListener(new AbsListView.OnScrollListener() {
            final static int ITEMS_THRESHOLD = 3;
            int lastFvi = 0;

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                onMainContentScrolled(firstVisibleItem <= ITEMS_THRESHOLD ? 0 : Integer.MAX_VALUE,
                        lastFvi - firstVisibleItem > 0 ? Integer.MIN_VALUE :
                                lastFvi == firstVisibleItem ? 0 : Integer.MAX_VALUE
                );
                lastFvi = firstVisibleItem;
            }
        });
    }

    /**
     * Indicates that the main content has scrolled (for the purposes of showing/hiding
     * the action bar for the "action bar auto hide" effect). currentY and deltaY may be exact
     * (if the underlying view supports it) or may be approximate indications:
     * deltaY may be INT_MAX to mean "scrolled forward indeterminately" and INT_MIN to mean
     * "scrolled backward indeterminately".  currentY may be 0 to mean "somewhere close to the
     * start of the list" and INT_MAX to mean "we don't know, but not at the start of the list"
     */
    private void onMainContentScrolled(int currentY, int deltaY) {
        if (deltaY > mActionBarAutoHideSensivity) {
            deltaY = mActionBarAutoHideSensivity;
        } else if (deltaY < -mActionBarAutoHideSensivity) {
            deltaY = -mActionBarAutoHideSensivity;
        }

        if (Math.signum(deltaY) * Math.signum(mActionBarAutoHideSignal) < 0) {
            // deltaY is a motion opposite to the accumulated signal, so reset signal
            mActionBarAutoHideSignal = deltaY;
        } else {
            // add to accumulated signal
            mActionBarAutoHideSignal += deltaY;
        }

        boolean shouldShow = currentY < mActionBarAutoHideMinY ||
                (mActionBarAutoHideSignal <= -mActionBarAutoHideSensivity);
        autoShowOrHideActionBar(shouldShow);
    }

    // Subclasses can override this for custom behavior
    protected void onNavDrawerStateChanged(boolean isOpen, boolean isAnimating) {
        if (mActionBarAutoHideEnabled && isOpen) {
            autoShowOrHideActionBar(true);
        }
    }

    protected void autoShowOrHideActionBar(boolean show) {
        if (show == mActionBarShown) {
            return;
        }

        mActionBarShown = show;
        //getLPreviewUtils().showHideActionBarIfPartOfDecor(show);
        onActionBarAutoShowOrHide(show);
    }

    protected void onActionBarAutoShowOrHide(boolean shown) {
        /*
        if (mStatusBarColorAnimator != null) {
            mStatusBarColorAnimator.cancel();
        }*/

        /*
        mStatusBarColorAnimator = ObjectAnimator.ofInt(mLPreviewUtils, "statusBarColor",
                shown ? mThemedStatusBarColor : Color.BLACK).setDuration(250);
                */

        //mStatusBarColorAnimator.setEvaluator(ARGB_EVALUATOR);
        //mStatusBarColorAnimator.start();

        updateSwipeRefreshProgressBarTop();

        for (View view : mHideableHeaderViews) {
            if (shown) {
                view.animate()
                        .translationY(0)
                        .alpha(1)
                        .setDuration(HEADER_HIDE_ANIM_DURATION)
                        .setInterpolator(new DecelerateInterpolator());
            } else {
                view.animate()
                        .translationY(-view.getBottom())
                        .alpha(0)
                        .setDuration(HEADER_HIDE_ANIM_DURATION)
                        .setInterpolator(new DecelerateInterpolator());
            }
        }
    }

    private void updateSwipeRefreshProgressBarTop() {
        if (mSwipeRefreshLayout == null) {
            return;
        }

        /*
        if (mActionBarShown) {
            mSwipeRefreshLayout.setProgressBarTop(mProgressBarTopWhenActionBarShown);
        } else {
            mSwipeRefreshLayout.setProgressBarTop(0);
        }
        */
    }

    private void initAutoPlayNextSwitch() {
        // Auto play next
        final String playNextKey = getResources().getString(R.string.pref_continuously_playing_key);

        mAutoPlayNext.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mSharedPreferences.edit().putBoolean(playNextKey, isChecked).commit();
            }
        });
        mSharedPreferences.registerOnSharedPreferenceChangeListener(new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if (key == playNextKey) {
                    mAutoPlayNext.setChecked(sharedPreferences.getBoolean(playNextKey, Playlist.PLAY_NEXT_DEFAULT));
                }
            }
        });

        boolean doPlayNext = mSharedPreferences.getBoolean(playNextKey, Playlist.PLAY_NEXT_DEFAULT);
        if (doPlayNext != mAutoPlayNext.isChecked()) {
            mAutoPlayNext.setChecked(doPlayNext);
        }
    }

    class MultiSpinnerListener implements MultiSpinner.MultiSpinnerListener {

        Playlist mPlaylist;

        public MultiSpinnerListener(Playlist argPlaylist) {
            mPlaylist = argPlaylist;
        }

        @Override
        public void onItemsSelected(Long[] selected) {
            int len = selected.length;

            mPlaylist.clearSubscriptionID();

            for (int i = 0; i < len; i++) {
                mPlaylist.addSubscriptionID(selected[i]);
            }

            mPlaylist.notifyDatabaseChanged();
        }
    }

}