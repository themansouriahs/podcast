package org.bottiger.podcast.views.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.util.LongSparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import com.squareup.otto.Subscribe;

import org.bottiger.podcast.ApplicationConfiguration;
import org.bottiger.podcast.MainActivity;
import org.bottiger.podcast.R;
import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.adapters.PlaylistContentSpinnerAdapter;
import org.bottiger.podcast.playlist.Playlist;
import org.bottiger.podcast.playlist.PlaylistData;
import org.bottiger.podcast.playlist.filters.SubscriptionFilter;
import org.bottiger.podcast.provider.Subscription;
import org.bottiger.podcast.provider.SubscriptionLoader;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by Arvid on 8/18/2015.
 */
public class DialogPlaylistFilters extends DialogFragment {

    private Activity mContext;
    private Playlist mPlaylist;

    protected SharedPreferences mSharedPreferences;

    private static boolean modifyingState = false;

    private SubscriptionFilter mSubscriptionFilter;

    private LongSparseArray<Subscription> mSubscriptions = new LongSparseArray<>(50);
    private List<CheckBox> mCheckboxes = new LinkedList<>();

    private RadioGroup mRadioGroup;

    protected android.support.v7.widget.SwitchCompat mPlaylistShowListened;
    protected android.support.v7.widget.SwitchCompat mAutoPlayNext;
    protected android.support.v7.widget.SwitchCompat mOnlyDownloaded;
    protected Spinner mPlaylistOrderSpinner;


    private CompoundButton.OnCheckedChangeListener onCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (modifyingState)
                return;

            modifyingState = true;
            setRadioButtonState();
            modifyingState = false;
        }
    };
    private RadioGroup.OnCheckedChangeListener RadioOnCheckedChangeListener = new RadioGroup.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            if (modifyingState)
                return;

            modifyingState = true;
            switch (checkedId) {
                case R.id.radioNone:
                    checkNone();
                    break;
                case R.id.radioAll:
                    checkAll();
                    break;
                case R.id.radioCustom:
                    checkCustom();
                    break;
            }

            modifyingState = false;
        }
    };

    public static DialogPlaylistFilters newInstance() {
        //mContext = argActivity;
        DialogPlaylistFilters frag = new DialogPlaylistFilters();
        Bundle args = new Bundle();
        args.putInt("title", 2);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public  void onResume() {
        ((SoundWaves)getActivity().getApplicationContext()).getBus().register(this);
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        ((SoundWaves)getActivity().getApplicationContext()).getBus().unregister(this);
    }

        @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        //public Dialog onCreateDialog(@NonNull Playlist argPlaylist) {
        mPlaylist = ((SoundWaves)getActivity().getApplicationContext()).sBoundPlayerService.getPlaylist();
        mContext = getActivity();

            mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);

        setSubscriptions(mPlaylist);


        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        // Get the layout inflater
        LayoutInflater inflater = mContext.getLayoutInflater();

        View view = inflater.inflate(R.layout.dialog_playlist_filters, null);

            mPlaylistOrderSpinner = (Spinner) view.findViewById(R.id.drawer_playlist_sort_order);
            mPlaylistShowListened = (android.support.v7.widget.SwitchCompat) view.findViewById(R.id.slidebar_show_listened);
            mOnlyDownloaded = (android.support.v7.widget.SwitchCompat) view.findViewById(R.id.slidebar_show_downloaded);
            mAutoPlayNext = (android.support.v7.widget.SwitchCompat) view.findViewById(R.id.slidebar_show_continues);

        initSubscriptionFilter(inflater, view);


        mPlaylistOrderSpinner = (Spinner) view.findViewById(R.id.drawer_playlist_sort_order);
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapterSortOrder = ArrayAdapter.createFromResource(mContext,
                R.array.playlist_sort_order, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapterSortOrder.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        mPlaylistOrderSpinner.setAdapter(adapterSortOrder);

        mPlaylistOrderSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                PlaylistData pd = new PlaylistData();
                if (position == 0) {
                    //playlist.setSortOrder(Playlist.SORT.DATE_NEW); // new first
                    pd.sortOrder = Playlist.SORT.DATE_NEW; // new first
                } else {
                    //playlist.setSortOrder(Playlist.SORT.DATE_OLD); // old first
                    pd.sortOrder = Playlist.SORT.DATE_OLD;  // old first
                }
                SoundWaves.getBus().post(pd);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                return;
            }
        });

            initShowListened();

            initOnlyDownloaded();

            initAutoPlayNextSwitch();

        builder.setView(view);
        builder.setPositiveButton(R.string.apply_filters, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                setPlaylistFilters();
            }
        });

        return builder.create();
    }

    private void setPlaylistFilters() {
        switch (mRadioGroup.getCheckedRadioButtonId()) {
            case R.id.radioNone:
                mSubscriptionFilter.setMode(SubscriptionFilter.MODE.SHOW_NONE, getContext());
                break;
            case R.id.radioAll:
                mSubscriptionFilter.setMode(SubscriptionFilter.MODE.SHOW_ALL, getContext());
                break;
            case R.id.radioCustom: {
                mSubscriptionFilter.clear();
                for (int i = 0; i < mCheckboxes.size(); i++) {
                    CheckBox checkbox = mCheckboxes.get(i);
                    if (checkbox.isChecked()) {
                        Subscription subscription = mSubscriptions.valueAt(i);
                        mSubscriptionFilter.add(subscription.getId());
                    }
                }
                mSubscriptionFilter.setMode(SubscriptionFilter.MODE.SHOW_SELECTED, getContext());
                break;
            }
        }

        mPlaylist.populatePlaylist(Playlist.MAX_SIZE, true);
        mPlaylist.notifyPlaylistChanged();
    }

    private void initOnlyDownloaded() {

        // Show only downloaded
        mOnlyDownloaded.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                PlaylistData pd = new PlaylistData();
                pd.onlyDownloaded = isChecked;
                SoundWaves.getBus().post(pd);
            }
        });

        String showOnlyDownloaded = getResources().getString(R.string.pref_only_downloaded_key);
        boolean onlyDownloaded = mSharedPreferences.getBoolean(showOnlyDownloaded, Playlist.SHOW_ONLY_DOWNLOADED);
        if (onlyDownloaded != mOnlyDownloaded.isChecked()) {
            mOnlyDownloaded.setChecked(onlyDownloaded);
        }
    }

    private void initShowListened() {
        // Show listened
        mPlaylistShowListened.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                PlaylistData pd = new PlaylistData();
                pd.showListened = isChecked;
                SoundWaves.getBus().post(pd);
            }
        });

        boolean doShowListened = mSharedPreferences.getBoolean(ApplicationConfiguration.showListenedKey, Playlist.SHOW_LISTENED_DEFAULT);
        if (doShowListened != mPlaylistShowListened.isChecked()) {
            mPlaylistShowListened.setChecked(doShowListened);
        }

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

    private void initSubscriptionFilter(@NonNull LayoutInflater argLayoutInflater, @NonNull View argView) {
        //LinearLayout mPlaylistContentLayout = (LinearLayout) view.findViewById(R.id.playlist_content);
        //bindPlaylistFilter(mPlaylistContentLayout);
        mSubscriptions.clear();
        List<Subscription> list = SubscriptionLoader.allAsList(mContext.getContentResolver());
        for (Subscription s : list) {
            if (s.getStatus() == Subscription.STATUS_SUBSCRIBED)
                mSubscriptions.append(s.getId(), s);
        }

        LinearLayout linearLayout = (LinearLayout) argView.findViewById(R.id.filter_subscription_checkboxes);

        mRadioGroup = (RadioGroup) argView.findViewById(R.id.subscription_selection_type);
        mRadioGroup.setOnCheckedChangeListener(RadioOnCheckedChangeListener);

        mCheckboxes.clear();
        //for (Subscription subscription : mSubscriptions) {
        for (int i = 0, nsize = mSubscriptions.size(); i < nsize; i++) {
            Subscription subscription = mSubscriptions.valueAt(i);

            View itemView = argLayoutInflater.inflate(R.layout.filter_subscriptions_item, null);
            TextView tv = (TextView) itemView.findViewById(R.id.item_text);
            final CheckBox checkBox = (CheckBox) itemView.findViewById(R.id.item_checkbox);
            tv.setText(subscription.getTitle());

            if (mSubscriptionFilter.isShown(subscription.getId()))
                checkBox.setChecked(true);

            checkBox.setOnCheckedChangeListener(onCheckedChangeListener);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    checkBox.setChecked(!checkBox.isChecked());
                }
            });

            linearLayout.addView(itemView);

            mCheckboxes.add(checkBox);
        }

        setRadioButtonState();
    }

    private void bindPlaylistFilter(@NonNull LinearLayout argView) {
        final DialogPlaylistContent mDialogPlaylistContent = new DialogPlaylistContent(mContext, mPlaylist);
        argView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDialogPlaylistContent.performClick();
            }
        });

        //String title = mContext.getResources().getString(R.string.drawer_playlist_content_source);
        //TextView tv = (TextView) argView.findViewById(R.id.drawer_item_title);
        //tv.setText(title);
    }

    private void checkAll() {
        for (CheckBox checkBox : mCheckboxes) {
            checkBox.setChecked(true);
        }
    }

    private void checkNone() {
        for (CheckBox checkBox : mCheckboxes) {
            checkBox.setChecked(false);
        }
    }

    private void checkCustom() {
        for (int i = 0; i < mCheckboxes.size(); i++) {
            CheckBox checkBox = mCheckboxes.get(i);
            Subscription subscription = mSubscriptions.valueAt(i);

            if (mSubscriptionFilter.isShown(subscription.getId()))
                checkBox.setChecked(true);
            else
                checkBox.setChecked(false);
        }
    }

    private void setRadioButtonState() {
        boolean anyChecked = false;
        boolean anyUnchecked = false;
        for (CheckBox checkBox : mCheckboxes) {
            if (checkBox.isChecked()) {
                anyChecked = true;
            } else {
                anyUnchecked = true;
            }
        }

        if (anyChecked && anyUnchecked) {
            mRadioGroup.check(R.id.radioCustom);
            return;
        }

        if (anyChecked) {
            mRadioGroup.check(R.id.radioAll);
            return;
        }

        if (anyUnchecked) {
            mRadioGroup.check(R.id.radioNone);
            return;
        }
    }

    @Subscribe
    public void setSubscriptions(@Nullable Playlist argPlaylist) {
        mPlaylist = argPlaylist;

        if (argPlaylist != null)
            mSubscriptionFilter = argPlaylist.getSubscriptionFilter();
    }
}
