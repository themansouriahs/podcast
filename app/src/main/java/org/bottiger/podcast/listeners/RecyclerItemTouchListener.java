package org.bottiger.podcast.listeners;

import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.view.MotionEvent;
import android.view.View;

import org.bottiger.podcast.adapters.ItemCursorAdapter;
import org.bottiger.podcast.views.PlaylistViewHolder;

/**
 * Created by apl on 26-01-2015.
 */
public class RecyclerItemTouchListener implements RecyclerView.OnItemTouchListener {

    private MotionEvent fingerDown = null;
    private float fingerdownx = -1;
    private float fingerdowny = -1;

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

    public void onCLick(RecyclerView rv, MotionEvent e) {
        View childView = rv.findChildViewUnder(e.getX(), e.getY());
        RecyclerView.ViewHolder viewHolder = rv.getChildViewHolder(childView);
        final PlaylistViewHolder playlistViewHolder2 = (PlaylistViewHolder)viewHolder;

        Rect viewRect = new Rect();

        playlistViewHolder2.mPlayPauseButton.getHitRect(viewRect);
        if (viewRect.contains((int)e.getX(), (int)e.getY())) {
            playlistViewHolder2.mPlayPauseButton.onClick(null);
        } else {
            ItemCursorAdapter.keepOne.toggle(playlistViewHolder2);
        }

    }

    @Override
    public void onTouchEvent(RecyclerView rv, MotionEvent e) {
        View childView = rv.findChildViewUnder(e.getX(), e.getY());
        childView.callOnClick();
    }
}
