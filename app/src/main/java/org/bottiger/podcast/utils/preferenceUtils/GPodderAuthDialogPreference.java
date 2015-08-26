package org.bottiger.podcast.utils.preferenceUtils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.DialogPreference;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import org.bottiger.podcast.R;
import org.bottiger.podcast.provider.Subscription;
import org.bottiger.podcast.provider.SubscriptionLoader;
import org.bottiger.podcast.webservices.datastore.gpodder.GPodderAPI;

/**
 * The OptionDialogPreference will display a dialog, and will persist the
 * <code>true</code> when pressing the positive button and <code>false</code>
 * otherwise. It will persist to the android:key specified in xml-preference.
 */
public class GPodderAuthDialogPreference extends DialogPreference {

    private static final String TAG = "GPodderAuthDialog";

    private EditText mUsernameView;
    private EditText mPasswordView;

    private String mUsernameKey;
    private String mPasswordKey;
    private String mCloudSupportKey;
    private String mGPodderSupportKey;

    public GPodderAuthDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mUsernameKey = getContext().getResources().getString(R.string.pref_gpodder_username_key);
        mPasswordKey = getContext().getResources().getString(R.string.pref_gpodder_password_key);
        mCloudSupportKey = getContext().getResources().getString(R.string.pref_cloud_support_key);
        mGPodderSupportKey = getContext().getResources().getString(R.string.pref_gpodder_support_key);

    }


    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        // the view was created by my custom onCreateDialogView()
        mUsernameView = (EditText)view.findViewById(R.id.gpodder_username);
        mPasswordView = (EditText)view.findViewById(R.id.gpodder_password);

        SharedPreferences sharedPreferences = getSharedPreferences();
        mUsernameView.setText(sharedPreferences.getString(mUsernameKey, ""));
        mPasswordView.setText(sharedPreferences.getString(mPasswordKey, ""));
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        Log.d(TAG, "# onDialogClosed: " + positiveResult);
        if(positiveResult) {

            String username = mUsernameView.getText().toString();
            String password = mPasswordView.getText().toString();

            if (!validateCredentials(username, password))
                return;

            SharedPreferences.Editor editor = getEditor();
            editor.putString(mUsernameKey, username);
            editor.putString(mPasswordKey, password);
            editor.putBoolean(mCloudSupportKey, true);
            editor.putBoolean(mGPodderSupportKey, true);
            editor.commit();
        } else {
            String username = mUsernameView.getText().toString();
            String password = mPasswordView.getText().toString();

            GPodderAPI api = new GPodderAPI(username, password);
            //api.uploadSubscriptions(SubscriptionLoader.asList(getContext().getContentResolver()));
        }
    }

    private boolean validateCredentials(@Nullable String argUsername, @Nullable String argPassword) {
        return !TextUtils.isEmpty(argUsername) || !TextUtils.isEmpty(argPassword);
    }

}