package org.bottiger.podcast.activities.downloadmanager;

import android.arch.lifecycle.LifecycleRegistry;
import android.arch.lifecycle.LifecycleRegistryOwner;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.graphics.Canvas;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import org.bottiger.podcast.R;
import org.bottiger.podcast.provider.QueueEpisode;
import org.bottiger.podcast.service.DownloadService;
import org.bottiger.podcast.views.NpaLinearLayoutManager;

import java.util.List;

/**
 * Created by aplb on 04-10-2015.
 */
public class DownloadManagerActivity extends AppCompatActivity implements LifecycleRegistryOwner {

    private static final String TAG = DownloadManagerActivity.class.getSimpleName();

    LifecycleRegistry lifecycleRegistry = new LifecycleRegistry(this);

    private RecyclerView mRecyclerView;
    private DownloadManagerAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download_manager);

        DownloadManagerViewModel viewModel = ViewModelProviders.of(this).get(DownloadManagerViewModel.class);
        viewModel.getData().observe(this, new Observer<List<QueueEpisode>>() {
            @Override
            public void onChanged(@Nullable List<QueueEpisode> queueEpisodes) {
                String queueSize = queueEpisodes != null ? Integer.toString(queueEpisodes.size()) : "null";
                Log.i(TAG, "Livedata changed. Size: " + queueSize);

                mAdapter.setData(queueEpisodes);
            }
        });

        Toolbar toolbar = findViewById(R.id.download_manager_toolbar);
        if (toolbar != null) {
            toolbar.setTitle(R.string.download_manager_toolbar_title);
            setSupportActionBar(toolbar);
        }
        ActionBar actionbar = getSupportActionBar();
        if (actionbar != null) actionbar.setDisplayHomeAsUpEnabled(true);

        TextView emptyText = (TextView) findViewById(R.id.download_empty_text);

        if (emptyText != null)
            mAdapter = new DownloadManagerAdapter(this, emptyText);

        mRecyclerView = (RecyclerView) findViewById(R.id.download_queue_list);
        if (mRecyclerView != null) {
            mRecyclerView.setAdapter(mAdapter);
            mRecyclerView.setLayoutManager(new NpaLinearLayoutManager(this));
            mRecyclerView.setHasFixedSize(true);
        }

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
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        mAdapter.onDetachedFromRecyclerView(mRecyclerView);
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_download_manager, menu);
        return true;
    }

    /**
     * Right corner menu options
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.clear_download_queue: {
                DownloadService.clearQueue();
                return true;
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public LifecycleRegistry getLifecycle() {
        return lifecycleRegistry;
    }
}
