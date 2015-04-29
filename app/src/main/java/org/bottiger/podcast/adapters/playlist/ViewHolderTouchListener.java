package org.bottiger.podcast.adapters.playlist;

import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;

import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.views.PlaylistViewHolder;

/**
 * Handles touch event on a single ViewHolder
 *
 * Created by apl on 18-03-2015.
 */
public class ViewHolderTouchListener implements View.OnTouchListener {

    private final PlaylistViewHolder holder;
    private final FeedItem item;

    Rect viewRect = new Rect();

    private MotionEvent fingerDown = null;
    private float fingerdownx = -1;
    private float fingerdowny = -1;

    public ViewHolderTouchListener(PlaylistViewHolder argPlaylistViewHolder, FeedItem argFeedItem) {
        holder = argPlaylistViewHolder;
        item = argFeedItem;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN){

            if (mouseDown(holder.mPlayPauseButton, event))
                return true;

            if (mouseDown(holder.downloadButton, event))
                return true;

            if (mouseDown(holder.removeButton, event))
                return true;

            if (mouseDown(holder.favoriteButton, event))
                return true;

            if (mouseDown(holder.mForward, event))
                return true;

            fingerDown = event;
            fingerdownx = event.getRawX();
            fingerdowny = event.getRawY();
        }

        if (event.getAction() == MotionEvent.ACTION_UP){
            if (fingerDown != null) {
                float thresshold = 10;
                float diffx = Math.abs(fingerdownx-event.getRawX());
                float diffy = Math.abs(fingerdowny-event.getRawY());
                if (diffx < thresshold && diffy < thresshold) {
                    //holder.onClick(holder.mLayout, holder);
                    //PlaylistAdapter.toggleItem(item.getId());
                    //PlaylistAdapter.keepOne.toggle(holder);
                    resetTouchState();
                }
            }
        }

        if (event.getAction() == MotionEvent.ACTION_CANCEL || event.getAction() == MotionEvent.ACTION_HOVER_EXIT){
            resetTouchState();
        }

        return true;
    }

    /**
     * @return true of view was hit
     */
    private boolean mouseDown(View argView, MotionEvent event) { // View.OnClickListener
        argView.getHitRect(viewRect);
        if (viewRect.contains((int)event.getX(), (int)event.getY())) {
            //argClick.onClick(null);
            argView.callOnClick();
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
