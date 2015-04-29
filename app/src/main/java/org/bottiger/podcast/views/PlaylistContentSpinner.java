package org.bottiger.podcast.views;

import android.content.Context;
import android.content.DialogInterface;
import android.util.AttributeSet;
import android.widget.ArrayAdapter;

import org.bottiger.podcast.R;
import org.bottiger.podcast.provider.Subscription;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by apl on 19-02-2015.
 */
public class PlaylistContentSpinner extends MultiSpinner {

    private Context mContext;

    private ArrayAdapter<String> mAdapter;
    private List<Subscription> mSubscriptions;

    private String mSpinnerPrefix;
    private String mShownAll;
    private String mShownSome;

    public PlaylistContentSpinner(Context context) {
        super(context);
        init(context);
    }

    public PlaylistContentSpinner(Context arg0, AttributeSet arg1) {
        super(arg0, arg1);
        init(arg0);
    }

    public PlaylistContentSpinner(Context arg0, AttributeSet arg1, int arg2) {
        super(arg0, arg1, arg2);
        init(arg0);
    }

    private void init(Context argContext) {
        mContext = argContext;

        mSpinnerPrefix = mContext.getResources().getString(R.string.playlist_filter_prefix);
        mShownAll = mContext.getResources().getString(R.string.playlist_filter_all);
        mShownSome = mContext.getResources().getString(R.string.playlist_filter_some);
    }

    @Override
    public void onClick(DialogInterface dialog, int which, boolean isChecked) {

        if (which == 0) {
            for (int i = 0; i < selected.length; i++) {
                selected[i] = false;
                boolean checked = i == 0 ? isChecked : false;
                mAlertDialog.getListView().setItemChecked(i, checked);
            }
            selected[0] = isChecked;
            return;
        } else if (isChecked) {
            selected[0] = false;
            mAlertDialog.getListView().setItemChecked(0, false);
        }

        if (isChecked)
            selected[which] = true;
        else
            selected[which] = false;
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        // refresh text on spinner

        String spinnerText = "";

        if (selected[0]) {
            spinnerText = mSpinnerPrefix + " " + mShownAll;
        } else {
            spinnerText = mSpinnerPrefix + " " + mShownSome;
        }

        mAdapter = new ArrayAdapter<String>(getContext(),
                android.R.layout.simple_spinner_item,
                new String[]{spinnerText});
        setAdapter(mAdapter);

        if (selected[0]) {
            listener.onItemsSelected(new Long[0]);
        }

        LinkedList<Long> ids = new LinkedList<>();
        for (int i = 1; i < selected.length; i++) {
            Subscription sub = mSubscriptions.get(i-1);

            if (selected[i])
                ids.add(sub.getId());

        }
        listener.onItemsSelected(ids.toArray(new Long[ids.size()]));
    }

    public void setSubscriptions(List<Subscription> items, String allText,
                         MultiSpinnerListener listener) {
        List<String> mStrings = new LinkedList<>();

        mStrings.add(mShownAll);
        for (Subscription sub : items) {
            mStrings.add(sub.getTitle());
        }

        int offset = 1;

        this.mSubscriptions = items;
        this.items = mStrings;
        this.defaultText = allText;
        this.listener = listener;

        // all selected by default
        selected = new boolean[items.size()+offset];
        selected[0] = true;
        for (int i = 0+offset; i < selected.length; i++)
            selected[i] = false;

        // all text on the spinner
        mAdapter = new ArrayAdapter<String>(getContext(),
                android.R.layout.simple_spinner_item, new String[]{mSpinnerPrefix + " " + mShownAll});
        setAdapter(mAdapter);
    }
}
