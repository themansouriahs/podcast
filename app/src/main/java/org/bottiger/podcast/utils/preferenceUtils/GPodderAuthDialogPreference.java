package org.bottiger.podcast.utils.preferenceUtils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.DialogPreference;
import android.support.annotation.Nullable;
import android.support.v4.widget.ContentLoadingProgressBar;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.bottiger.podcast.R;
import org.bottiger.podcast.provider.Subscription;
import org.bottiger.podcast.provider.SubscriptionLoader;
import org.bottiger.podcast.utils.ThemeHelper;
import org.bottiger.podcast.webservices.datastore.gpodder.GPodderAPI;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * The OptionDialogPreference will display a dialog, and will persist the
 * <code>true</code> when pressing the positive button and <code>false</code>
 * otherwise. It will persist to the android:key specified in xml-preference.
 */
public class GPodderAuthDialogPreference extends DialogPreference {

    private static final String TAG = "GPodderAuthDialog";

    private EditText mUsernameView;
    private EditText mPasswordView;

    private Button mTestCredentials;
    private TextView mTestResult;
    private ContentLoadingProgressBar mTestLoading;

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

        mTestCredentials = (Button)view.findViewById(R.id.test_credentials_button);
        mTestResult = (TextView)view.findViewById(R.id.test_credentials_result);
        mTestLoading = (ContentLoadingProgressBar)view.findViewById(R.id.test_credentials_loading);

        mTestLoading.hide();

        SharedPreferences sharedPreferences = getSharedPreferences();

        String username = sharedPreferences.getString(mUsernameKey, "");
        String password = sharedPreferences.getString(mPasswordKey, "");

        mUsernameView.setText(username);
        mPasswordView.setText(password);

        mTestCredentials.setEnabled(validateCredentials(username, password));

        mTestCredentials.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mTestResult.setVisibility(View.GONE);
                mTestLoading.show();
                String username = mUsernameView.getText().toString();
                String password = mPasswordView.getText().toString();
                GPodderAPI api = new GPodderAPI(username, password, new Callback() {
                    @Override
                    public void success(Object o, Response response) {
                        mTestLoading.hide();
                        mTestResult.setVisibility(View.VISIBLE);
                        mTestResult.setText(getContext().getResources().getString(R.string.generic_test_credentials_succes));
                        mTestResult.setTextColor(getContext().getResources().getColor(R.color.green));
                    }

                    @Override
                    public void failure(RetrofitError error) {
                        mTestLoading.hide();
                        mTestResult.setVisibility(View.VISIBLE);
                        mTestResult.setText(getContext().getResources().getString(R.string.generic_test_credentials_failed));
                        mTestResult.setTextColor(getContext().getResources().getColor(R.color.red));
                    }
                });
            }
        });
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