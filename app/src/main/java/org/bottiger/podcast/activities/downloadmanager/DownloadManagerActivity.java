package org.bottiger.podcast.activities.downloadmanager;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.widget.TextView;

import org.bottiger.podcast.R;
import org.bottiger.podcast.activities.openopml.OpenOpmlAdapter;

/**
 * Created by aplb on 04-10-2015.
 */
public class DownloadManagerActivity extends AppCompatActivity {

    private static final String TAG = "DownloadManagerActivity";

    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download_manager);

        Toolbar toolbar = (Toolbar) findViewById(R.id.helper_toolbar);
        setSupportActionBar(toolbar);

        TextView toolbarTitle = (TextView) findViewById(R.id.helper_toolbar_title);
        TextView toolbarDescription = (TextView) findViewById(R.id.helper_toolbar_description);

        toolbarTitle.setText(R.string.download_manager_toolbar_title);
        toolbarDescription.setText(R.string.download_manager_toolbar_description);

        mAdapter = new DownloadManagerAdapter();
        mRecyclerView = (RecyclerView) findViewById(R.id.download_queue_list);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setHasFixedSize(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_download_manager, menu);
        return true;
    }
}
