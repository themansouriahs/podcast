package org.bottiger.podcast.activities.openopml;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import org.bottiger.podcast.R;
import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.model.Library;
import org.bottiger.podcast.provider.SlimImplementations.SlimSubscription;
import org.bottiger.podcast.provider.Subscription;
import org.bottiger.podcast.utils.ErrorUtils;
import org.bottiger.podcast.utils.OPMLImportExport;

import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by aplb on 21-09-2015.
 */
public class OpenOpmlFromIntentActivity extends AppCompatActivity {

    private static final String TAG = OpenOpmlFromIntentActivity.class.getSimpleName();

    private List<SlimSubscription> mSubscriptions = new LinkedList<>();
    private static final String OPML_SUBS_LIST_EXTRA_CODE = "EXTRACODE_SUBS_LIST";
    private static final String FILE_SCHEME = "file";

    private Library mLibrary;

    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_opml_import);

        Toolbar toolbar = (Toolbar) findViewById(R.id.opml_import_export_toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }
        ActionBar actionbar = getSupportActionBar();
        if (actionbar != null) actionbar.setDisplayHomeAsUpEnabled(true);

        mLibrary = SoundWaves.getAppContext(this).getLibraryInstance();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED || checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    public void requestPermissions(@NonNull String[] permissions, int requestCode)
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for Activity#requestPermissions for more details.
                return;
            }
        }
        OPMLImportExport opmlImportExport = new OPMLImportExport(this);

        try {
            Reader opmlReader;
            Uri uri = getIntent().getData();

            if (FILE_SCHEME.equals(uri.getScheme())) {
                File opmlFile = new File(uri.getPath());
                opmlReader = new FileReader(opmlFile);
            } else {
                InputStream inputStream = getContentResolver().openInputStream(uri);
                opmlReader = new InputStreamReader(inputStream, "UTF-8");
            }

            mSubscriptions = opmlImportExport.readSubscriptionsFromOPML(opmlReader);
        } catch (Exception e) {
            new AlertDialog.Builder(this).setMessage("Cannot open XML - Reason: " + e.getMessage()).show();
            ErrorUtils.handleException(e);
        }

        SlimSubscription subscription;
        for (int i = 0; i < mSubscriptions.size(); i++) {
            subscription = mSubscriptions.get(i);
            if (mLibrary.containsSubscription(subscription)) {
                subscription.setIsSubscribed(true);
            }
        }

        mAdapter = new OpenOpmlAdapter(mSubscriptions);
        //mAdapter.setHasStableIds(true);

        mRecyclerView = (RecyclerView) findViewById(R.id.opml_subscription_list);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    public void import_click(View view) {
        Log.d(TAG, "importing selected"); // NoI18N

        boolean didImport = false;
        Subscription newSubscription;
        SlimSubscription subscription;
        for (int i = 0; i < mSubscriptions.size(); i++) {
            subscription = mSubscriptions.get(i);
            if (subscription.IsDirty()) {
                mLibrary.subscribe(subscription);
                didImport = true;
            }
        }

        if (didImport) {
            finish();
        }
    }

    public void select_all_click(View argView) {
        Log.d(TAG, "Selecting all"); // NoI18N
        setAll(true);
    }

    public void deselect_all_click(View argView) {
        Log.d(TAG, "Deselecting all"); // NoI18N
        setAll(false);
    }

    private void setAll(boolean argSetSubscribed) {
        for (int i = 0; i < mSubscriptions.size(); i++) {
            mSubscriptions.get(i).markForSubscription(argSetSubscribed);
        }
        mAdapter.notifyDataSetChanged();
    }
}
