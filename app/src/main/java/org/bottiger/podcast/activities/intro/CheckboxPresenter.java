package org.bottiger.podcast.activities.intro;

import android.content.Context;
import android.support.annotation.BoolRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.util.Log;
import android.view.View;
import android.widget.CheckedTextView;

import org.bottiger.podcast.utils.PreferenceHelper;

/**
 * Created by aplb on 05-12-2016.
 */

public class CheckboxPresenter {

    private static final String TAG = CheckboxPresenter.class.getSimpleName();

    private @StringRes int mKey;
    private boolean mIsChecked = false;

    CheckboxPresenter(@NonNull Context argConText, @StringRes int argKey, @BoolRes int argDefaultValue) {

        mIsChecked = PreferenceHelper.getBooleanPreferenceValue(argConText,
                argKey,
                argDefaultValue);

        mKey = argKey;
    }

    public void onClick(View argView) {

        CheckedTextView checkedTextView = (CheckedTextView) argView;

        boolean isChecked = checkedTextView.isChecked();

        // This value will be inverted
        // http://stackoverflow.com/questions/7531334/androidcheckedtextview-ischecked-returns-incorrect-value
        mIsChecked = !isChecked;

        Log.d(TAG, "Checked: " + mIsChecked);

        checkedTextView.toggle();
        PreferenceHelper.setBooleanPreferenceValue(checkedTextView.getContext(), mKey, mIsChecked);
    }

    public boolean isChecked() {
        return mIsChecked;
    }

}
