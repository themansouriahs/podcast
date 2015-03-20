package org.bottiger.podcast.listeners;

import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.view.MotionEvent;
import android.view.View;

import org.bottiger.podcast.adapters.PlaylistAdapter;
import org.bottiger.podcast.views.PlaylistViewHolder;

/**
 * Created by apl on 26-01-2015.
 */
public class RecyclerItemTouchListener implements RecyclerView.OnItemTouchListener {

    private MotionEvent fingerDown = null;
    private float fingerdownx = -1;
    private float fingerdowny = -1;

    Rect viewRect = new Rect();

    private boolean mouseDown = false;

    @Override
    public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {

        Rect viewRect = new Rect();

        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                fingerDown = e;
                fingerdownx = e.getRawX();
                fingerdowny = e.getRawY();
                mouseDown = true;
                break;
            case MotionEvent.ACTION_UP:
                if (mouseDown) {
                    if (fingerDown != null) {
                        float thresshold = 10;
                        float diffx = Math.abs(fingerdownx-e.getRawX());
                        float diffy = Math.abs(fingerdowny-e.getRawY());
                        if (diffx < thresshold && diffy < thresshold) {
                            fingerDown=null;
                            onCLick(rv, e);
                        }
                    }
                    mouseDown = false;
                    return true;
                }
                break;
            default:
                mouseDown = false;
        }

        return false;
    }

    // keepOne.toggle(playlistViewHolder2);

    public void onCLick(RecyclerView rv, MotionEvent event) {

        View childView = rv.findChildViewUnder(event.getX(), event.getY());
        RecyclerView.ViewHolder viewHolder = rv.getChildViewHolder(childView);
        final PlaylistViewHolder holder = (PlaylistViewHolder)viewHolder;

        int topHeight = holder.mMainContainer.getHeight();

        if (mouseDown(holder.mPlayPauseButton, holder.mPlayPauseButton, event, topHeight))
            return;

        if (mouseDown(holder.downloadButton, holder.downloadButton, event, topHeight))
            return;

        if (mouseDown(holder.favoriteButton, holder.favoriteButton, event, topHeight))
            return;

        int pos = rv.getChildPosition(childView);
        PlaylistAdapter.toggle(holder, pos);
    }

    @Override
    public void onTouchEvent(RecyclerView rv, MotionEvent e) {
        View childView = rv.findChildViewUnder(e.getX(), e.getY());
        childView.callOnClick();
    }

    /**
     * @return true of view was hit
     */
    private boolean mouseDown(View argView, View.OnClickListener argClick, MotionEvent event, int topHeight) {
        argView.getGlobalVisibleRect(viewRect);
        if (viewRect.contains((int)event.getRawX(), (int)event.getRawY())) {
            argClick.onClick(null);
            resetTouchState();
            return true;
        }

        return false;
    }

    private void resetTouchState() {
        fingerDown = null;
        fingerdownx = -1;
        fingerdowny = -1;
    }
}
