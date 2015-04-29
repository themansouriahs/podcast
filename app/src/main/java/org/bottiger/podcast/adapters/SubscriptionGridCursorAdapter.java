package org.bottiger.podcast.adapters;

import org.bottiger.podcast.FeedActivity;
import org.bottiger.podcast.R;
import org.bottiger.podcast.adapters.viewholders.subscription.SubscriptionViewHolder;
import org.bottiger.podcast.provider.Subscription;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


public class SubscriptionGridCursorAdapter extends CursorRecyclerAdapter {

    private static final String TAG = "SubscriptionGridAdapter";

    private static final int GRID_TYPE = 1;
    private static final int LIST_TYPE = 2;

    private final LayoutInflater mInflater;
    private Activity mActivity;

    private int numberOfColumns = 2;
    private int position = -1;

    private OnSubscriptionCountChanged mOnSubscriptionCountChanged = null;

    public SubscriptionGridCursorAdapter(Activity argActivity, Cursor cursor) {
        super(cursor);
        mActivity = argActivity;
        mInflater = (LayoutInflater) argActivity
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Log.v(TAG, "onCreateViewHolder");

        View view = mInflater.inflate(getGridItemLayout(), parent, false);
        SubscriptionViewHolder holder = new SubscriptionViewHolder(view);

        return holder;
    }

    @Override
    public void onBindViewHolderCursor(RecyclerView.ViewHolder argHolder, Cursor cursor) {
        SubscriptionViewHolder holder = (SubscriptionViewHolder)argHolder;

        Subscription sub = null;
        try {
            sub = Subscription.getByCursor(cursor);
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }

        if (null == sub) {
            return;
        }

        final Subscription subscription = sub;

        String title = sub.title;
        final String logo = sub.imageURL;

        if (isListView()) {
            holder.gradient.setVisibility(View.GONE);

            boolean missingColor = sub.getPrimaryColor() == -1;

            /*
            if (missingColor) {

                TintedFramelayout tview = (TintedFramelayout) view;
                tview.unsetSubscription();
                tview.setSubscription(sub);

                UrlValidator validator = new UrlValidator();
                if (validator.isValid(logo)) {

                try {

                    Uri uri = Uri.parse(logo);
                    ImageRequest request = ImageRequestBuilder
                            .newBuilderWithSource(uri)
                            .build();


                    ImagePipeline imagePipeline = Fresco.getImagePipeline();
                    DataSource<CloseableReference<CloseableImage>> dataSource = imagePipeline.fetchDecodedImage(request, mActivity); //

                    dataSource.subscribe(new BaseBitmapDataSubscriber() {
                        @Override
                        public void onNewResultImpl(@Nullable Bitmap bitmap) {
                            PaletteCache.generate(logo, bitmap);
                            // You can use the bitmap in only limited ways
                            // No need to do any cleanup.
                        }

                        @Override
                        public void onFailureImpl(DataSource dataSource) {
                            // No cleanup required here.
                        }
                    }, null);
                } catch (NullPointerException npe) {
                    // Uri.parse probably failed because logo==null
                }
            }


            } else {
                view.setBackgroundColor(sub.getPrimaryColor());
            }*/

        } else {
            holder.gradient.setVisibility(View.VISIBLE);
        }


        if (title != null && !title.equals(""))
            holder.title.setText(title);
        else
            holder.title.setText(R.string.subscription_no_title);


        if (holder.image != null && !TextUtils.isEmpty(logo)) {
            Uri uri = Uri.parse(logo);
            holder.image.setImageURI(uri);
        }

        argHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FeedActivity.start(mActivity, subscription.getUrl());
            }
        });

        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                setPosition((int)subscription.getId());
                return false;
            }
        });
    }

    @Override
    public void onViewRecycled(RecyclerView.ViewHolder holder) {
        holder.itemView.setOnLongClickListener(null);
        super.onViewRecycled(holder);
    }

    @Override
    public int getItemViewType(int position) {
        return numberOfColumns == 1 ? LIST_TYPE : GRID_TYPE;
    }

    private boolean isListView() {
        return numberOfColumns == 1;
    }

    private int getGridItemLayout() {
        return numberOfColumns == 1 ? R.layout.subscription_list_item : R.layout.subscription_grid_item ;
    }

    public void setNumberOfColumns(int argNumber) {
        if (numberOfColumns != argNumber) {
            numberOfColumns = argNumber;
            notifyDataSetChanged();
        }
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }


    @Override
    public void changeCursor(Cursor cursor) {
        super.changeCursor(cursor);
        mOnSubscriptionCountChanged.newSubscriptionCount(getItemCount());
    }

    /**
     * Swap in a new Cursor, returning the old Cursor.  Unlike
     * {@link #changeCursor(Cursor)}, the returned old Cursor is <em>not</em>
     * closed.
     *
     * @param newCursor The new cursor to be used.
     * @return Returns the previously set Cursor, or null if there wasa not one.
     * If the given new Cursor is the same instance is the previously set
     * Cursor, null is also returned.
     */
    @Override
    public Cursor swapCursor(Cursor newCursor) {
        Cursor cursor = super.swapCursor(newCursor);
        mOnSubscriptionCountChanged.newSubscriptionCount(getItemCount());
        return cursor;
    }

    protected void onContentChanged() {
        if (mOnSubscriptionCountChanged != null) {
            mOnSubscriptionCountChanged.newSubscriptionCount(getItemCount());
        }
    }

    public void setOnSubscriptionCountChangedListener(@Nullable OnSubscriptionCountChanged argOnSubscriptionCountChanged) {
        mOnSubscriptionCountChanged = argOnSubscriptionCountChanged;
    }

    public interface OnSubscriptionCountChanged {
        public void newSubscriptionCount(int argCount);
    }

}
