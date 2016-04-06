package org.bottiger.podcast.activities.pastelog;

import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.Toast;

import org.bottiger.podcast.R;
import org.whispersystems.libpastelog.SubmitLogFragment;

/**
 * Created by aplb on 06-04-2016.
 */
public class LogSubmitActivity extends AppCompatActivity implements SubmitLogFragment.OnLogSubmittedListener {
    private static final String TAG = LogSubmitActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.activity_submit_log);
        //getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        SubmitLogFragment fragment = SubmitLogFragment.newInstance();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();

        initializeScreenshotSecurity();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }

        return false;
    }

    @Override
    public void onSuccess() {
        Toast.makeText(getApplicationContext(), R.string.log_submit_activity__thanks, Toast.LENGTH_LONG).show();
        finish();
    }

    @Override
    public void onFailure() {
        Toast.makeText(getApplicationContext(), R.string.log_submit_activity__log_fetch_failed, Toast.LENGTH_LONG).show();
        finish();
    }

    @Override
    public void onCancel() {
        finish();
    }

    private void initializeScreenshotSecurity() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)
        {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }
    }
}
