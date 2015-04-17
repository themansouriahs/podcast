package org.bottiger.podcast.adapters;

import org.bottiger.podcast.FeedActivity;
import org.bottiger.podcast.R;
import org.bottiger.podcast.adapters.viewholders.subscription.SubscriptionViewHolder;
import org.bottiger.podcast.provider.Subscription;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


public class SubscriptionGridCursorAdapter extends CursorRecyclerAdapter {

    private static final String TAG = "SubscriptionGridAdapter";

    private final LayoutInflater mInflater;
    private Activity mActivity;
    private int mItemLayout;

    public SubscriptionGridCursorAdapter(Activity argActivity, Cursor cursor, int argItemLayout) {
        super(cursor);
        mActivity = argActivity;
        mItemLayout = argItemLayout;
        mInflater = (LayoutInflater) argActivity
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Log.v(TAG, "onCreateViewHolder");

        View view = mInflater.inflate(R.layout.subscription_grid_item, parent, false);
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
                FeedActivity.start(mActivity, subscription.getId());
            }
        });
    }

    private boolean isListView() {
        return mItemLayout == R.layout.subscription_list_item;
    }

}
