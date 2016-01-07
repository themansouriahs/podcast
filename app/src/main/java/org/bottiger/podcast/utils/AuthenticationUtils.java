package org.bottiger.podcast.utils;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.support.annotation.ColorRes;
import android.support.annotation.MainThread;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.ContentLoadingProgressBar;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.bottiger.podcast.R;

/**
 * Created by aplb on 31-12-2015.
 */
public class AuthenticationUtils {

    public static void disableButton(Button argTestCredentialsButton, boolean argEnabled) {
        if (argTestCredentialsButton == null)
            return;

        argTestCredentialsButton.setEnabled(argEnabled);

        if (argEnabled) {
            argTestCredentialsButton.getBackground().setColorFilter(null);
        } else {
            argTestCredentialsButton.getBackground().setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY);
        }
    }

    public static boolean validateCredentials(@Nullable String argUsername, @Nullable String argPassword) {
        return !TextUtils.isEmpty(argUsername) && !TextUtils.isEmpty(argPassword);
    }

    @MainThread
    public static void initCredentialTest(
                                          ContentLoadingProgressBar argLoadingIndicator,
                                          TextView argTextView) {
        argTextView.setVisibility(View.GONE);
        argLoadingIndicator.show();
    }

    @MainThread
    public static void setState(boolean argIsSuccesfull,
                                Context argContext,
                                ContentLoadingProgressBar argLoadingIndicator,
                                TextView argTextView) {

        @ColorRes int color = R.color.green;

        if (argIsSuccesfull) {
            argLoadingIndicator.hide();
            argTextView.setVisibility(View.VISIBLE);
            argTextView.setText(argContext.getResources().getString(R.string.generic_test_credentials_succes));
        } else {
            argLoadingIndicator.hide();
            argTextView.setVisibility(View.VISIBLE);
            argTextView.setText(argContext.getResources().getString(R.string.generic_test_credentials_failed));
            color = R.color.red;
        }

        argTextView.setTextColor(ContextCompat.getColor(argContext, color));
    }
}
