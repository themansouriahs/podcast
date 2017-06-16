package org.bottiger.podcast.utils.preferenceUtils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.DialogPreference;
import android.support.v4.widget.ContentLoadingProgressBar;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.jakewharton.rxbinding2.InitialValueObservable;
import com.jakewharton.rxbinding2.widget.RxTextView;

import org.bottiger.podcast.R;
import org.bottiger.podcast.utils.AuthenticationUtils;
import org.bottiger.podcast.utils.StrUtils;
import org.bottiger.podcast.webservices.datastore.gpodder.GPodderAPI;
import org.bottiger.podcast.webservices.datastore.gpodder.GPodderUtils;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.SingleObserver;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function4;
import io.reactivex.observers.DisposableObserver;
import retrofit2.Callback;

import static android.text.TextUtils.isEmpty;

/**
 * The OptionDialogPreference will display a dialog, and will persist the
 * <code>true</code> when pressing the positive button and <code>false</code>
 * otherwise. It will persist to the android:key specified in xml-preference.
 */
public class GPodderAuthDialogPreference extends DialogPreference {

    private static final String TAG = "GPodderAuthDialog";

    private InitialValueObservable<CharSequence> _usernameChangeObservable;
    private InitialValueObservable<CharSequence> _passwordChangeObservable;
    private InitialValueObservable<CharSequence> _serverChangeObservable;
    private InitialValueObservable<CharSequence> _deviceNameChangeObservable;

    private Disposable _subscription = null;

    private EditText mUsernameView;
    private EditText mPasswordView;

    private EditText mServerView;
    private EditText mDeviceNameView;

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

        setPositiveButtonText(R.string.gpodder_dialog_positive_button);
    }


    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        // the view was created by my custom onCreateDialogView()
        mUsernameView = (EditText)view.findViewById(R.id.gpodder_username);
        mPasswordView = (EditText)view.findViewById(R.id.gpodder_password);
        mServerView = (EditText)view.findViewById(R.id.gpodder_server);
        mDeviceNameView = (EditText)view.findViewById(R.id.gpodder_device);

        _usernameChangeObservable   = RxTextView.textChanges(mUsernameView);//.skip(1);
        _passwordChangeObservable   = RxTextView.textChanges(mPasswordView);//.skip(1);
        _serverChangeObservable     = RxTextView.textChanges(mServerView);//.skip(1);
        _deviceNameChangeObservable = RxTextView.textChanges(mDeviceNameView);//.skip(1);

        mTestCredentials = (Button)view.findViewById(R.id.test_credentials_button);
        mTestResult = (TextView)view.findViewById(R.id.test_credentials_result);
        mTestLoading = (ContentLoadingProgressBar)view.findViewById(R.id.test_credentials_loading);

        mTestLoading.hide();

        SharedPreferences sharedPreferences = getSharedPreferences();

        String username = sharedPreferences.getString(mUsernameKey, "");
        String password = sharedPreferences.getString(mPasswordKey, "");

        mUsernameView.setText(username);
        mPasswordView.setText(password);
        mServerView.setText(GPodderUtils.getServer(sharedPreferences));
        mDeviceNameView.setText(GPodderUtils.getDeviceCaption(getContext()));

        AuthenticationUtils.disableButton(mTestCredentials, AuthenticationUtils.validateCredentials(username, password));

        mTestCredentials.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //mTestResult.setVisibility(View.GONE);
                //mTestLoading.show();
                AuthenticationUtils.initCredentialTest(mTestLoading, mTestResult);

                String username = mUsernameView.getText().toString();
                String password = mPasswordView.getText().toString();
                String server = mServerView.getText().toString();

                if (!StrUtils.isValidUrl(server)) {
                    AuthenticationUtils.setState(false, getContext(), mTestLoading, mTestResult, null);
                    return;
                }

                try {
                    GPodderAPI api = new GPodderAPI(getContext(), server, username, password, new Callback() {
                        @Override
                        public void onResponse(retrofit2.Call call, retrofit2.Response response) {
                            AuthenticationUtils.setState(response.isSuccessful(), getContext(), mTestLoading, mTestResult, null);
                        }

                        @Override
                        public void onFailure(retrofit2.Call call, Throwable t) {
                            AuthenticationUtils.setState(false, getContext(), mTestLoading, mTestResult, t);
                        }
                    });
                } catch (IllegalArgumentException iae) {
                    AuthenticationUtils.setState(false, getContext(), mTestLoading, mTestResult, iae);
                    return;
                }
            }
        });

        _combineLatestEvents();
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        Log.d(TAG, "# onDialogClosed: " + positiveResult);
        if(positiveResult) {

            String username = mUsernameView.getText().toString();
            String password = mPasswordView.getText().toString();
            String server   = mServerView.getText().toString();
            String device   = mDeviceNameView.getText().toString();

            SharedPreferences.Editor editor = getEditor();

            boolean validates = AuthenticationUtils.validateCredentials(username, password);
            editor.putBoolean(mGPodderSupportKey, validates);
            if (validates) {
                editor.putString(mUsernameKey, username);
                editor.putString(mPasswordKey, password);
                editor.putString(GPodderUtils.serverNameKey, server);
                editor.putString(GPodderUtils.deviceNameKey, device);
                editor.putBoolean(mCloudSupportKey, true);
            }

            editor.commit();
        }

        if (_subscription != null && !_subscription.isDisposed()) {
            _subscription.dispose();
        }
    }

    private void _combineLatestEvents() {
        _subscription = Observable.combineLatest(
                _usernameChangeObservable,
                _passwordChangeObservable,
                _serverChangeObservable,
                _deviceNameChangeObservable,
                new Function4<CharSequence, CharSequence, CharSequence, CharSequence, Boolean>() {

                    @Override
                    public Boolean apply(CharSequence newEmail, CharSequence newPassword, CharSequence newServer, CharSequence newDevice) throws Exception {
                        boolean serverValid = !isEmpty(newServer) && StrUtils.isValidUrl(newServer.toString());
                        if (!serverValid) {
                            mServerView.setError("Invalid url");
                        }

                        boolean deviceNameValid = !isEmpty(newDevice);
                        if (!serverValid) {
                            mDeviceNameView.setError("Invalid name");
                        }

                        boolean credentialsValid = AuthenticationUtils.validateCredentials(newEmail.toString(), newPassword.toString());

                        return serverValid && deviceNameValid && credentialsValid;
                    }
                }).subscribe(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean formValid) throws Exception {
                        AuthenticationUtils.disableButton(mTestCredentials, formValid);
                    }
                });
    }

}