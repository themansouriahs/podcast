package org.bottiger.podcast.adapters;

import org.apache.commons.validator.routines.UrlValidator;
import org.bottiger.podcast.FeedActivity;
import org.bottiger.podcast.R;
import org.bottiger.podcast.ToolbarActivity;
import org.bottiger.podcast.adapters.viewholders.FooterViewHolder;
import org.bottiger.podcast.adapters.viewholders.subscription.SubscriptionViewHolder;
import org.bottiger.podcast.images.FrescoHelper;
import org.bottiger.podcast.provider.Subscription;
import org.bottiger.podcast.provider.SubscriptionLoader;
import org.bottiger.podcast.utils.SharedAdapterUtils;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.BitmapImageViewTarget;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;


public class SubscriptionCursorAdapter extends CursorRecyclerAdapter {

    private static final String TAG = "SubscriptionAdapter";

    private static final int GRID_TYPE = 1;
    private static final int LIST_TYPE = 2;
    private static final int FOOTER_TYPE = 3;

    private final LayoutInflater mInflater;
    private Activity mActivity;

    private int numberOfColumns = 2;
    private int position = -1;

    private OnSubscriptionCountChanged mOnSubscriptionCountChanged = null;

    public SubscriptionCursorAdapter(Activity argActivity, Cursor cursor, int argColumnsCount) {
        super(cursor);
        mActivity = argActivity;
        mInflater = (LayoutInflater) argActivity
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        numberOfColumns = argColumnsCount;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Log.v(TAG, "onCreateViewHolder");

        RecyclerView.ViewHolder holder = null;

        switch (viewType)
        {
            case FOOTER_TYPE: {
                View view = mInflater.inflate(R.layout.recycler_item_empty_footer, parent, false);
                holder = new FooterViewHolder(view);
                break;
            }
            default: {
                View view = mInflater.inflate(getGridItemLayout(), parent, false);
                holder = new SubscriptionViewHolder(view);
                break;
            }
        }

        return holder;
    }

    @Override
    public void onBindViewHolderCursor(RecyclerView.ViewHolder argHolder, Cursor cursor, int position) {

        // In case we are dealing with the footer
        if (isLastItem(position)) {
            FooterViewHolder footer = (FooterViewHolder)argHolder;
            //footer.getFooter().getLayoutParams().height = ToolbarActivity.getNavigationBarHeight(mActivity.getResources());

            android.support.v7.widget.GridLayoutManager.LayoutParams params = (android.support.v7.widget.GridLayoutManager.LayoutParams)footer.getFooter().getLayoutParams();
            params.height = ToolbarActivity.getNavigationBarHeight(mActivity.getResources());
            //params.columnSpec = GridLayout.spec(0, numberOfColumns-1);
            //params.s
            footer.getFooter().setLayoutParams(params);
            footer.getFooter().requestLayout();

            return;
        }

        SubscriptionViewHolder holder = (SubscriptionViewHolder)argHolder;

        Subscription sub = null;
        try {
            sub = SubscriptionLoader.getByCursor(cursor);
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }

        if (null == sub) {
            return;
        }

        // Add some padding to the last element in order to compensate for the transparent navigationbar
        //int itemsLeft = cursor.getCount()-cursor.getPosition();
        //boolean isLastRow = cursor.getPosition()
        //SharedAdapterUtils.AddPaddingToLastElement(holder.container, 0, cursor.getPosition() == getItemCount()-1);
        /*
        int left = argHolder.itemView.getPaddingLeft();
        int right = argHolder.itemView.getPaddingRight();
        int top = argHolder.itemView.getPaddingTop();
        int bottom = argHolder.itemView.getPaddingTop();
        Resources resources = argHolder.itemView.getResources();
        int newBottomPadding = position == getItemCount() ? ToolbarActivity.getNavigationBarHeight(resources) : 0;
        argHolder.itemView.setPadding(left, top, right, newBottomPadding);
        */

        final Subscription subscription = sub;

        String title = sub.title;
        final String logo = sub.imageURL;

        if (isListView()) {
            holder.gradient.setVisibility(View.GONE);
        } else {
            holder.gradient.setVisibility(View.VISIBLE);
        }


        if (title != null && !title.equals(""))
            holder.title.setText(title);
        else
            holder.title.setText(R.string.subscription_no_title);

        if (subscription.getLastUpdate() > 0 && holder.subTitle != null) {
            String reportDate = DateUtils.getRelativeTimeSpanString(subscription.getLastItemUpdated()).toString(); //df.format(date);

            String updatedAt = mActivity.getResources().getString(R.string.subscription_subtitle_updated_at);
            holder.subTitle.setText(updatedAt + " " + reportDate);
        }


        Uri uri = null;
        if (holder.image != null && !TextUtils.isEmpty(logo)) {
            uri = Uri.parse(logo);
        } else {
            //uri = Uri.parse("android.resource://" + mActivity.getPackageName() + "/" + R.drawable.generic_podcast);
            //uri = ResourceToUri(mActivity, R.drawable.generic_podcast);
        }

        UrlValidator urlValidator = new UrlValidator();

        if (uri != null) {
            String image = uri.toString();
            if (!TextUtils.isEmpty(image) && urlValidator.isValid(image)) {

                //FrescoHelper.PalettePostProcessor postProcessor = new FrescoHelper.PalettePostProcessor(mActivity, image);
                //FrescoHelper.loadImageInto(holder.image, image, postProcessor);
                Glide.with(mActivity).load(image).centerCrop().placeholder(R.drawable.generic_podcast).into(holder.image);

            }
        } else {
            Glide.with(mActivity).load(R.drawable.generic_podcast).centerCrop().into(holder.image);
        }

        /*
        if (uri != null)
            holder.image.setImageURI(uri);
            */

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



    public static Uri ResourceToUri (Context context, @DrawableRes int resID) {
        return Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" +
                context.getResources().getResourcePackageName(resID) + '/' +
                context.getResources().getResourceTypeName(resID) + '/' +
                context.getResources().getResourceEntryName(resID) );
    }



    @Override
    public void onViewRecycled(RecyclerView.ViewHolder holder) {
        holder.itemView.setOnLongClickListener(null);
        super.onViewRecycled(holder);
    }

    @Override
    public int getItemViewType(int position) {
        if (isLastItem(position))
            return FOOTER_TYPE;

        return numberOfColumns == 1 ? LIST_TYPE : GRID_TYPE;
    }

    private boolean isLastItem(int argPosition) {
        return argPosition +1 == getItemCount();
    }

    private boolean isListView() {
        return numberOfColumns == 1;
    }

    private int getGridItemLayout() {
        return isListView() ? R.layout.subscription_list_item : R.layout.subscription_grid_item ;
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
    public int getItemCount() {
        int count = super.getItemCount();

        if (count == 0) // If there are 0 subscriptions we do not want to return 1
            return count;

        return count +1; // one footer please
    }


    @Override
    public void changeCursor(Cursor cursor) {
        super.changeCursor(cursor);
        mOnSubscriptionCountChanged.newSubscriptionCount(super.getItemCount());

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
        mOnSubscriptionCountChanged.newSubscriptionCount(super.getItemCount());
        return cursor;
    }

    protected void onContentChanged() {
        if (mOnSubscriptionCountChanged != null) {
            mOnSubscriptionCountChanged.newSubscriptionCount(super.getItemCount());
        }
    }

    public void setOnSubscriptionCountChangedListener(@Nullable OnSubscriptionCountChanged argOnSubscriptionCountChanged) {
        mOnSubscriptionCountChanged = argOnSubscriptionCountChanged;
    }

    public interface OnSubscriptionCountChanged {
        public void newSubscriptionCount(int argCount);
    }

}
