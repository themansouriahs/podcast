package org.bottiger.podcast.activities.downloadmanager;

import android.graphics.Canvas;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
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
    private DownloadManagerAdapter mAdapter;

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

        mAdapter = new DownloadManagerAdapter(this);
        mRecyclerView = (RecyclerView) findViewById(R.id.download_queue_list);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setHasFixedSize(true);

        ItemTouchHelper.Callback callback = new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                mAdapter.onItemMove(viewHolder.getAdapterPosition(), target.getAdapterPosition());
                return true;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
            }

            /**
             * FIXME: Unfortunately this doesn't work
             */
            @Override
            public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);

                if (Build.VERSION.SDK_INT >= 21 && isCurrentlyActive) {
                    final float newElevation = 5f + ViewCompat.getElevation(viewHolder.itemView);
                    ViewCompat.setElevation(viewHolder.itemView, newElevation);
                }
            }

            @Override
            public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                ViewCompat.setElevation(viewHolder.itemView, 0);
            }
        };
        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(mRecyclerView);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_download_manager, menu);
        return true;
    }
}
