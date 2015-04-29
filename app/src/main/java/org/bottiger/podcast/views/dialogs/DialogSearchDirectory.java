package org.bottiger.podcast.views.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;

import org.bottiger.podcast.R;

/**
 * Created by apl on 26-04-2015.
 */
public class DialogSearchDirectory  extends DialogFragment {

    private DialogInterface.OnClickListener mCallback;
    private String[] mValues;
    private String mPrefKey;

    @Override
    public void onAttach(final Activity activity) {
        if (activity != null) {
            Resources resources = activity.getResources();
            mValues = resources.getStringArray(R.array.entries_webservices_discovery_engine);
            mPrefKey = resources.getString(R.string.pref_webservices_discovery_engine_key);
        }

        mCallback = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext());
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(mPrefKey, Integer.toString(which));
                editor.commit();
            }
        };

        super.onAttach(activity);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.discovery_select_backend)
                .setItems(R.array.entries_webservices_discovery_engine, mCallback);
        return builder.create();
    }

}
