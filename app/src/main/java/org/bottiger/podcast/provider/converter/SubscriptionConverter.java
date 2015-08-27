package org.bottiger.podcast.provider.converter;

import android.content.ContentResolver;
import android.support.annotation.NonNull;

import org.bottiger.podcast.provider.Subscription;
import org.bottiger.podcast.provider.SubscriptionLoader;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

/**
 * Created by apl on 22-05-2015.
 */
public class SubscriptionConverter {

    public static String toJSON(@NonNull Subscription argSubscription) {
        JSONObject json = new JSONObject();
        json.put("url", argSubscription.getURL().toString());
        json.put("last_update", argSubscription.getLastUpdate());
        json.put("title", argSubscription.getTitle());

        return json.toJSONString();
    }

    public static Subscription fromJSON(ContentResolver contentResolver,
                                        String json) {
        Subscription subscription = null;
        boolean updateItem = false;
        if (json != null) {
            String url = null;
            String title = null;
            String remote_id = null;
            Number lastUpdate = -1;

            Object rootObject = JSONValue.parse(json);
            JSONObject mainObject = (JSONObject) rootObject;
            if (mainObject != null) {
                url = mainObject.get("url").toString();
                if (mainObject.get("title") != null)
                    title = mainObject.get("title").toString();
                if (mainObject.get("last_update") != null)
                    lastUpdate = (Number) mainObject.get("last_update");
                if (mainObject.get("remote_id") != null)
                    remote_id = mainObject.get("remote_id").toString();
            }

            subscription = SubscriptionLoader.getByUrl(contentResolver, url);
            if (subscription == null) {
                subscription = new Subscription();
                updateItem = true;
            } else {
                updateItem = subscription.getLastUpdate() < lastUpdate
                        .longValue();
            }

            if (url != null)
                subscription.url = url;
            if (title != null)
                subscription.title = title;
            if (remote_id != null)
                subscription.sync_id = remote_id;
            if (lastUpdate.longValue() > -1)
                subscription.lastUpdated = lastUpdate.longValue();

            if (updateItem)
                subscription.update(contentResolver);
        }

        return subscription;
    }
}
