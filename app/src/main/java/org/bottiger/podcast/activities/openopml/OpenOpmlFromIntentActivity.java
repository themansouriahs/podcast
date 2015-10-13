package org.bottiger.podcast.activities.openopml;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import org.bottiger.podcast.R;
import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.provider.SlimImplementations.SlimSubscription;
import org.bottiger.podcast.provider.Subscription;
import org.bottiger.podcast.utils.OPMLImportExport;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by aplb on 21-09-2015.
 */
public class OpenOpmlFromIntentActivity extends AppCompatActivity {
    private static final String TAG = "OpenOpmlFromIntentActivity";

    private List<SlimSubscription> mSubscriptions = new LinkedList<>();

    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_opml_import);

        TextView toolbarTitle = (TextView) findViewById(R.id.helper_toolbar_title);
        TextView toolbarDescription = (TextView) findViewById(R.id.helper_toolbar_description);

        toolbarTitle.setText(R.string.opml_import_toolbar_title);
        toolbarDescription.setText(R.string.opml_import_toolbar_description);

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
            Uri uri = getIntent().getData();

            File opmlFile = new File(uri.getPath());

            mSubscriptions = opmlImportExport.readSubscriptionsFromOPML(opmlFile);
        } catch (Exception e) {
            new AlertDialog.Builder(this).setMessage("Cannot open XML - Reason: " + e.getMessage()).show();
        }

        mAdapter = new OpenOpmlAdapter(mSubscriptions);
        mRecyclerView = (RecyclerView) findViewById(R.id.opml_subscription_list);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    public void import_click(View view) {
        boolean didImport = false;
        Subscription newSubscription;
        SlimSubscription subscription;
        for (int i = 0; i < mSubscriptions.size(); i++) {
            subscription = mSubscriptions.get(i);
            if (subscription.IsDirty()) {
                //newSubscription = new Subscription(subscription.getURLString());
                //newSubscription.subscribe(this);
                SoundWaves.getLibraryInstance().subscribe(subscription.getURLString());
                didImport = true;
            }
        }

        if (didImport) {
            finish();
        }
    }
}
