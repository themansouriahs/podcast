package org.bottiger.podcast.views.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;

import org.bottiger.podcast.R;

/**
 * Created by apl on 26-04-2015.
 */
public class DialogSearchDirectory  extends DialogFragment {

    private DialogInterface.OnClickListener mCallback;
    private String mPrefKey;

    @Override
    public void onAttach(final Context argContext) {
        if (argContext != null) {
            Resources resources = argContext.getResources();
            mPrefKey = resources.getString(R.string.pref_webservices_discovery_engine_key);
        }

        mCallback = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (argContext == null)
                    return;

                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(argContext.getApplicationContext());
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(mPrefKey, Integer.toString(which));
                editor.apply();
            }
        };

        super.onAttach(argContext);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.discovery_select_backend)
                .setItems(R.array.entries_webservices_discovery_engine, mCallback);
        return builder.create();
    }

}
