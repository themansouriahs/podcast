package org.bottiger.podcast.widgets;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import org.bottiger.podcast.R;

import java.util.ArrayList;

/**
 * Created by aplb on 26-05-2016.
 */

public class SoundWavesWidgetConfigure extends Activity {

    static final String TAG = "SoundWavesWidgetCfg";

    private static final String PREFS_NAME = "SWWidgetConfig";
    private static final String PREF_PREFIX_KEY = "prefix_";

    int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    TextView mAppWidgetPrefix;

    public SoundWavesWidgetConfigure() {
        super();
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        Log.e(TAG, "oncreate");

        // Set the view layout resource to use.
        setContentView(R.layout.widget_configure);

        // Find the EditText
        mAppWidgetPrefix = (TextView)findViewById(R.id.textView3);

        // Bind the action for the save button.
        findViewById(R.id.main_ok).setOnClickListener(mOnClickListener);
        mAppWidgetPrefix.setText(loadTitlePref(SoundWavesWidgetConfigure.this, mAppWidgetId));

        // Find the widget id from the intent.
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        // If they gave us an intent without the widget id, just bail.
        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            Log.e(TAG, "oncreate - finish");
            finish();
        }
    }

    View.OnClickListener mOnClickListener = new View.OnClickListener() {
        public void onClick(View v) {
            final Context context = SoundWavesWidgetConfigure.this;

            // When the button is clicked, save the string in our prefs and return that they
            // clicked OK.
            String titlePrefix = mAppWidgetPrefix.getText().toString();
            saveTitlePref(context, mAppWidgetId, titlePrefix);

            // Push widget update to surface with newly set prefix
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            //SoundWavesWidgetProvider.updateAppWidget(context, appWidgetManager,
            //        mAppWidgetId, titlePrefix);
            //SoundWavesWidgetProvider.updateAppWidget(context, mAppWidgetId, false);

            // Make sure we pass back the original appWidgetId
            Intent resultValue = new Intent();
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
            setResult(RESULT_OK, resultValue);
            finish();
        }
    };

    // Write the prefix to the SharedPreferences object for this widget
    static void saveTitlePref(Context context, int appWidgetId, String text) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.putString(PREF_PREFIX_KEY + appWidgetId, text);
        prefs.commit();
    }

    // Read the prefix from the SharedPreferences object for this widget.
    // If there is no preference saved, get the default from a resource
    static String loadTitlePref(Context context, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        String prefix = prefs.getString(PREF_PREFIX_KEY + appWidgetId, null);
        if (prefix != null) {
            return prefix;
        } else {
            //return context.getString(R.string.appwidget_prefix_default);
            return "Hello";
        }
    }

    static void deleteTitlePref(Context context, int appWidgetId) {
    }

    static void loadAllTitlePrefs(Context context,
                                  ArrayList<Integer> appWidgetIds,
                                  ArrayList<String> texts) {
    }
}

