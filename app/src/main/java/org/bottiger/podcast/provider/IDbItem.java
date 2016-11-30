package org.bottiger.podcast.provider;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by aplb on 30-11-2016.
 */

public interface IDbItem {

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({SUBSCRIPTION, EPISODE})
    @interface DbItemType {}
    int SUBSCRIPTION = 0;
    int EPISODE = 1;

    @DbItemType
    int getType();

}
