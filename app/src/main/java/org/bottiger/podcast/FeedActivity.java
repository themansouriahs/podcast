package org.bottiger.podcast;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Trace;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toolbar;

import org.bottiger.podcast.views.MultiShrink.ExpandingEntryCardView;
import org.bottiger.podcast.views.MultiShrink.MultiShrinkScroller;
import org.bottiger.podcast.views.MultiShrink.SchedulingUtils;

/**
 * Created by apl on 14-02-2015.
 */
public class FeedActivity extends Activity {

    public static final int MODE_FULLY_EXPANDED = 4;

    private static final String TAG = "QuickContact";

    private static final String KEY_THEME_COLOR = "theme_color";

    private static final int ANIMATION_STATUS_BAR_COLOR_CHANGE_DURATION = 150;
    private static final int REQUEST_CODE_CONTACT_EDITOR_ACTIVITY = 1;
    private static final int DEFAULT_SCRIM_ALPHA = 0xC8;
    private static final int SCRIM_COLOR = Color.argb(DEFAULT_SCRIM_ALPHA, 0, 0, 0);
    private static final int REQUEST_CODE_CONTACT_SELECTION_ACTIVITY = 2;
    private static final String MIMETYPE_SMS = "vnd.android-dir/mms-sms";

    /** This is the Intent action to install a shortcut in the launcher. */
    private static final String ACTION_INSTALL_SHORTCUT =
            "com.android.launcher.action.INSTALL_SHORTCUT";

    @SuppressWarnings("deprecation")
    private static final String LEGACY_AUTHORITY = android.provider.Contacts.AUTHORITY;

    private static final String MIMETYPE_GPLUS_PROFILE =
            "vnd.android.cursor.item/vnd.googleplus.profile";
    private static final String INTENT_DATA_GPLUS_PROFILE_ADD_TO_CIRCLE = "Add to circle";
    private static final String MIMETYPE_HANGOUTS =
            "vnd.android.cursor.item/vnd.googleplus.profile.comm";
    private static final String INTENT_DATA_HANGOUTS_VIDEO = "Start video call";
    private static final String CALL_ORIGIN_QUICK_CONTACTS_ACTIVITY =
            "com.android.contacts.quickcontact.QuickContactActivity";

    private int mStatusBarColor;
    private int mExtraMode;
    private boolean mHasAlreadyBeenOpened;

    private MultiShrinkScroller mScroller;

    /**
     *  This scrim's opacity is controlled in two different ways. 1) Before the initial entrance
     *  animation finishes, the opacity is animated by a value animator. This is designed to
     *  distract the user from the length of the initial loading time. 2) After the initial
     *  entrance animation, the opacity is directly related to scroll position.
     */
    private ColorDrawable mWindowScrim;
    private boolean mIsEntranceAnimationFinished;
    //private MaterialColorMapUtils mMaterialColorMapUtils;
    private boolean mIsExitAnimationInProgress;
    private boolean mHasComputedThemeColor;

    final MultiShrinkScroller.MultiShrinkScrollerListener mMultiShrinkScrollerListener
            = new MultiShrinkScroller.MultiShrinkScrollerListener() {
        @Override
        public void onScrolledOffBottom() {
            finish();
        }

        @Override
        public void onEnterFullscreen() {
            updateStatusBarColor();
        }

        @Override
        public void onExitFullscreen() {
            updateStatusBarColor();
        }

        @Override
        public void onStartScrollOffBottom() {
            mIsExitAnimationInProgress = true;
        }

        @Override
        public void onEntranceAnimationDone() {
            mIsEntranceAnimationFinished = true;
        }

        @Override
        public void onTransparentViewHeightChange(float ratio) {
            if (mIsEntranceAnimationFinished) {
                mWindowScrim.setAlpha((int) (0xFF * ratio));
            }
        }
    };

    @Override
    @TargetApi(21)
    protected void onCreate(Bundle savedInstanceState) {
        //Trace.beginSection("onCreate()");
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(Color.TRANSPARENT);

        //processIntent(getIntent());

        // Show QuickContact in front of soft input
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,
                WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);

        setContentView(R.layout.feed_activity);

        //mMaterialColorMapUtils = new MaterialColorMapUtils(getResources());

        mScroller = (MultiShrinkScroller) findViewById(R.id.multiscroller);

        /*
        mContactCard = (ExpandingEntryCardView) findViewById(R.id.communication_card);
        mNoContactDetailsCard = (ExpandingEntryCardView) findViewById(R.id.no_contact_data_card);
        mRecentCard = (ExpandingEntryCardView) findViewById(R.id.recent_card);
        mAboutCard = (ExpandingEntryCardView) findViewById(R.id.about_card);

        mNoContactDetailsCard.setOnClickListener(mEntryClickHandler);
        mContactCard.setOnClickListener(mEntryClickHandler);
        mContactCard.setExpandButtonText(
                getResources().getString(R.string.expanding_entry_card_view_see_all));
        mContactCard.setOnCreateContextMenuListener(mEntryContextMenuListener);

        mRecentCard.setOnClickListener(mEntryClickHandler);
        mRecentCard.setTitle(getResources().getString(R.string.recent_card_title));

        mAboutCard.setOnClickListener(mEntryClickHandler);
        mAboutCard.setOnCreateContextMenuListener(mEntryContextMenuListener);

        mPhotoView = (QuickContactImageView) findViewById(R.id.photo);
        final View transparentView = findViewById(R.id.transparent_view);
        if (mScroller != null) {
            transparentView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mScroller.scrollOffBottom();
                }
            });
        }*/

        // Allow a shadow to be shown under the toolbar.
        //ViewUtil.addRectangularOutlineProvider(findViewById(R.id.toolbar_parent), getResources());

        /*
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setActionBar(toolbar);
        getActionBar().setTitle(null);
        // Put a TextView with a known resource id into the ActionBar. This allows us to easily
        // find the correct TextView location & size later.
        toolbar.addView(getLayoutInflater().inflate(R.layout.quickcontact_title_placeholder, null));

        mHasAlreadyBeenOpened = savedInstanceState != null;
        mIsEntranceAnimationFinished = mHasAlreadyBeenOpened;
        mWindowScrim = new ColorDrawable(SCRIM_COLOR);
        mWindowScrim.setAlpha(0);
        getWindow().setBackgroundDrawable(mWindowScrim);
        */
        mScroller.initialize(mMultiShrinkScrollerListener, mExtraMode == MODE_FULLY_EXPANDED);
        // mScroller needs to perform asynchronous measurements after initalize(), therefore
        // we can't mark this as GONE.
        //mScroller.setVisibility(View.INVISIBLE);

        /*
        setHeaderNameText(R.string.missing_name);

        mSelectAccountFragmentListener= (SelectAccountDialogFragmentListener) getFragmentManager()
                .findFragmentByTag(FRAGMENT_TAG_SELECT_ACCOUNT);
        if (mSelectAccountFragmentListener == null) {
            mSelectAccountFragmentListener = new SelectAccountDialogFragmentListener();
            getFragmentManager().beginTransaction().add(0, mSelectAccountFragmentListener,
                    FRAGMENT_TAG_SELECT_ACCOUNT).commit();
            mSelectAccountFragmentListener.setRetainInstance(true);
        }
        mSelectAccountFragmentListener.setQuickContactActivity(this);
        */
        SchedulingUtils.doOnPreDraw(mScroller, /* drawNextFrame = */ true,
                new Runnable() {
                    @Override
                    public void run() {
                        if (!mHasAlreadyBeenOpened) {
                            // The initial scrim opacity must match the scrim opacity that would be
                            // achieved by scrolling to the starting position.
                            final float alphaRatio = mExtraMode == MODE_FULLY_EXPANDED ?
                                    1 : mScroller.getStartingTransparentHeightRatio();
                            final int duration = getResources().getInteger(
                                    android.R.integer.config_shortAnimTime);
                            final int desiredAlpha = (int) (0xFF * alphaRatio);
                            ObjectAnimator o = ObjectAnimator.ofInt(mWindowScrim, "alpha", 0,
                                    desiredAlpha).setDuration(duration);

                            o.start();
                        }
                    }
                });

        if (savedInstanceState != null) {
            final int color = savedInstanceState.getInt(KEY_THEME_COLOR, 0);
            SchedulingUtils.doOnPreDraw(mScroller, /* drawNextFrame = */ false,
                    new Runnable() {
                        @Override
                        public void run() {
                            // Need to wait for the pre draw before setting the initial scroll
                            // value. Prior to pre draw all scroll values are invalid.
                            if (mHasAlreadyBeenOpened) {
                                mScroller.setVisibility(View.VISIBLE);
                                mScroller.setScroll(mScroller.getScrollNeededToBeFullScreen());
                            }
                            // Need to wait for pre draw for setting the theme color. Setting the
                            // header tint before the MultiShrinkScroller has been measured will
                            // cause incorrect tinting calculations.
                            if (color != 0) {
                                /*
                                setThemeColor(mMaterialColorMapUtils
                                        .calculatePrimaryAndSecondaryColor(color));
                                        */
                            }
                        }
                    });
        }

        //Trace.endSection();
    }

    @TargetApi(21)
    private void updateStatusBarColor() {
        if (mScroller == null) {
            return;
        }
        final int desiredStatusBarColor;
        // Only use a custom status bar color if QuickContacts touches the top of the viewport.
        if (mScroller.getScrollNeededToBeFullScreen() <= 0) {
            desiredStatusBarColor = mStatusBarColor;
        } else {
            desiredStatusBarColor = Color.TRANSPARENT;
        }
        // Animate to the new color.
        final ObjectAnimator animation = ObjectAnimator.ofInt(getWindow(), "statusBarColor",
                getWindow().getStatusBarColor(), desiredStatusBarColor);
        animation.setDuration(ANIMATION_STATUS_BAR_COLOR_CHANGE_DURATION);
        animation.setEvaluator(new ArgbEvaluator());
        animation.start();
    }
}
