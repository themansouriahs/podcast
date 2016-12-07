package org.bottiger.podcast.flavors.Analytics.database;

import com.google.firebase.database.IgnoreExtraProperties;

/**
 * Created by aplb on 07-12-2016.
 */
@IgnoreExtraProperties
public class DatabaseSubscription {

    public Long counter;

    public DatabaseSubscription() {
        // Default constructor required for calls to DataSnapshot.getValue(DatabaseSubscription.class)
    }

    public DatabaseSubscription(Long argCounter) {
        this.counter = argCounter;
    }

}
