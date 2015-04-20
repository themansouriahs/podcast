package org.bottiger.podcast.adapters;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.v7.graphics.Palette;

import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.provider.ISubscription;
import org.bottiger.podcast.provider.Subscription;
import org.bottiger.podcast.utils.PaletteCache;

import java.util.ArrayList;

/**
 * Created by apl on 21-04-2015.
 */
public class FeedViewDiscoveryAdapter extends FeedViewAdapter {

    private ArrayList<IEpisode> mEpisodes = new ArrayList<>();

    public FeedViewDiscoveryAdapter(Context context, Cursor dataset) {
        super(context, dataset);
    }

    @Override
    public void setDataset(Cursor c) {
        return;
    }

    @Override
    public int getItemCount() {
        return mEpisodes.size();
    }

    public void setDataset(@NonNull ArrayList<IEpisode> argEpisodes) {
        mEpisodes = argEpisodes;
    }

    @Override
    protected void getPalette(ISubscription argSubscription) {
        if (argSubscription.getImageURL() != null) {
            Palette palette = PaletteCache.get(argSubscription.getImageURL());
            if (palette != null)
                mPalette = palette;
        }
    }
}
