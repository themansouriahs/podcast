package org.bottiger.podcast.adapters.viewholders.subscription;

import android.support.annotation.NonNull;

/**
 * Created by aplb on 14-12-2016.
 */

public interface ISubscriptionViewHolder {

    void setIsPinned(boolean argIsPinned);
    void setImagePlaceholderText(@NonNull String argTitle);
    void setImagePlaceholderVisibility(int argVisibility);
}
