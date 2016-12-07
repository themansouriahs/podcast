package org.bottiger.podcast.flavors.Analytics.database;

import android.support.annotation.NonNull;
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.bottiger.podcast.utils.StrUtils;

import java.util.HashMap;

/**
 * Created by aplb on 07-12-2016.
 */

public class FirebaseDatabaseLog {

    private static final String TAG = FirebaseDatabaseLog.class.getSimpleName();

    private static final String SUBSCRIPTION_NODE = "subscriptions";

    public static void logFeed(@NonNull String argUrl, final boolean argDidSubscribe) {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();

        String key = StrUtils.toBase32(argUrl);

        final DatabaseReference subscriptionReference = databaseReference.child(SUBSCRIPTION_NODE).child(key);

        subscriptionReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                HashMap<String, Object> values = (HashMap<String, Object>) snapshot.getValue();
                Long counter = 0L;
                boolean hasRecord = values != null;

                if (values != null) {
                    counter = (Long)values.get("counter");
                }

                if (argDidSubscribe) {
                    counter = counter+1;
                }

                if (!hasRecord || argDidSubscribe) {
                    DatabaseSubscription subscriptionNode = new DatabaseSubscription(counter);
                    subscriptionReference.setValue(subscriptionNode);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.wtf(TAG, "Database Error");
            }
        });
    }

}
