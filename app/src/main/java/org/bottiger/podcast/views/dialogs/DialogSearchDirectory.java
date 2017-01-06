package org.bottiger.podcast.views.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v4.app.DialogFragment;

import org.bottiger.podcast.R;

/**
 * Created by apl on 26-04-2015.
 */
public class DialogSearchDirectory  extends DialogFragment {

    private static final String IDS = "ids";
    private static final String NAMES = "names";

    private DialogInterface.OnClickListener mCallback;
    private String mPrefKey;

    public static DialogSearchDirectory newInstance(int[] argSettingIds, @StringRes int[] argStringRes) {
        DialogSearchDirectory frag = new DialogSearchDirectory();
        Bundle args = new Bundle();

        args.putIntArray(IDS, argSettingIds);
        args.putIntArray(NAMES, argStringRes);

        frag.setArguments(args);
        return frag;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        final Context context = getContext();
        Resources resources = context.getResources();

        mPrefKey = resources.getString(R.string.pref_webservices_discovery_engine_key);

        Bundle bundle = getArguments();
        final int[] ids = bundle.getIntArray(IDS);
        final int[] res = bundle.getIntArray(NAMES);

        assert ids != null;
        assert res != null;

        mCallback = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                int selectedId = ids[which];

                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(mPrefKey, Integer.toString(selectedId));
                editor.apply();
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.discovery_select_backend);

        CharSequence[] names = new CharSequence[res.length];

        for (int i = 0; i < res.length; i++) {
            names[i] = resources.getString(res[i]);
        }

        builder.setItems(names, mCallback);

        return builder.create();
    }

}
