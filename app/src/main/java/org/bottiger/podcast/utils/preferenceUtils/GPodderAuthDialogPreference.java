package org.bottiger.podcast.utils.preferenceUtils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.DialogPreference;
import android.support.annotation.ColorRes;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.ContentLoadingProgressBar;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.jakewharton.rxbinding.widget.RxTextView;

import org.bottiger.podcast.R;
import org.bottiger.podcast.utils.AuthenticationUtils;
import org.bottiger.podcast.utils.StrUtils;
import org.bottiger.podcast.webservices.datastore.gpodder.GPodderAPI;
import org.bottiger.podcast.webservices.datastore.gpodder.GPodderUtils;

import retrofit.Callback;
import retrofit.Response;
import retrofit.Retrofit;
import rx.Observable;
import rx.Observer;
import rx.functions.Func4;

import static android.text.TextUtils.isEmpty;

/**
 * The OptionDialogPreference will display a dialog, and will persist the
 * <code>true</code> when pressing the positive button and <code>false</code>
 * otherwise. It will persist to the android:key specified in xml-preference.
 */
public class GPodderAuthDialogPreference extends DialogPreference {

    private static final String TAG = "GPodderAuthDialog";

    private Observable<CharSequence> _usernameChangeObservable;
    private Observable<CharSequence> _passwordChangeObservable;
    private Observable<CharSequence> _serverChangeObservable;
    private Observable<CharSequence> _deviceNameChangeObservable;

    private rx.Subscription _subscription = null;

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
                    AuthenticationUtils.setState(false, getContext(), mTestLoading, mTestResult);
                    return;
                }

                try {
                    GPodderAPI api = new GPodderAPI(server, username, password, new Callback() {
                        @Override
                        public void onResponse(Response response, Retrofit argRetrofit) {
                            AuthenticationUtils.setState(response.isSuccess(), getContext(), mTestLoading, mTestResult);
                        }

                        @Override
                        public void onFailure(Throwable t) {
                            AuthenticationUtils.setState(false, getContext(), mTestLoading, mTestResult);
                        }
                    });
                } catch (IllegalArgumentException iae) {
                    AuthenticationUtils.setState(false, getContext(), mTestLoading, mTestResult);
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
        /*else {
            String username = mUsernameView.getText().toString();
            String password = mPasswordView.getText().toString();

            GPodderAPI api = new GPodderAPI(username, password);
            //api.uploadSubscriptions(SubscriptionLoader.asList(getContext().getContentResolver()));
        }
        */

        if (_subscription != null && !_subscription.isUnsubscribed()) {
            _subscription.unsubscribe();
        }
    }

    private void _combineLatestEvents() {
        _subscription = Observable.combineLatest(
                _usernameChangeObservable,
                _passwordChangeObservable,
                _serverChangeObservable,
                _deviceNameChangeObservable,
                new Func4<CharSequence, CharSequence, CharSequence, CharSequence, Boolean>() {
                    @Override
                    public Boolean call(CharSequence newEmail,
                                        CharSequence newPassword,
                                        CharSequence newServer,
                                        CharSequence newDevice) {

                        boolean serverValid = !isEmpty(newServer) && StrUtils.isValidUrl(newServer.toString());
                        if (!serverValid) {
                            mServerView.setError("Invalid url");
                        }

                        boolean deviceNameValid = !isEmpty(newDevice);
                        if (!serverValid) {
                            mDeviceNameView.setError("Invalid name");
                        }

                        /*
                        boolean emailValid = !isEmpty(newEmail) &&
                                EMAIL_ADDRESS.matcher(newEmail).matches();
                        if (!emailValid) {
                            _email.setError("Invalid Email!");
                        }

                        boolean passValid = !isEmpty(newPassword) && newPassword.length() > 8;
                        if (!passValid) {
                            _password.setError("Invalid Password!");
                        }

                        boolean numValid = !isEmpty(newNumber);
                        if (numValid) {
                            int num = Integer.parseInt(newNumber.toString());
                            numValid = num > 0 && num <= 100;
                        }
                        if (!numValid) {
                            _number.setError("Invalid Number!");
                        }
                        */

                        boolean credentialsValid = AuthenticationUtils.validateCredentials(newEmail.toString(), newPassword.toString());

                        return serverValid && deviceNameValid && credentialsValid;

                    }
                })//
                .subscribe(new Observer<Boolean>() {
                    @Override
                    public void onCompleted() {
                        Log.d(TAG, "completed");
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG, "there was an error");
                    }

                    @Override
                    public void onNext(Boolean formValid) {
                        AuthenticationUtils.disableButton(mTestCredentials, formValid);
                        /*
                        if (formValid) {
                            _btnValidIndicator.setBackgroundColor(getResources().getColor(R.color.blue));
                        } else {
                            _btnValidIndicator.setBackgroundColor(getResources().getColor(R.color.gray));
                        }
                        */
                    }
                });
    }

}