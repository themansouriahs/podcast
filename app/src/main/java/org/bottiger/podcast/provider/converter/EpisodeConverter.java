package org.bottiger.podcast.provider.converter;

import android.content.ContentResolver;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.SlimImplementations.SlimEpisode;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.net.URL;

/**
 * Created by apl on 22-04-2015.
 */
public class EpisodeConverter {

    @Nullable
    public static SlimEpisode toSlim(@NonNull FeedItem argEpisode) {

        String title = argEpisode.getTitle();
        String description = argEpisode.getDescription();
        URL url = argEpisode.getUrl();

        if (title == null || description == null || url == null)
            return null;

        SlimEpisode slimEpisode = new SlimEpisode(title, url, description);

        if (argEpisode.getFilesize() > 0)
            slimEpisode.setFilesize(argEpisode.getFilesize());

        if (argEpisode.getDuration() > 0)
            slimEpisode.setDuration(argEpisode.getDuration());

        if (!TextUtils.isEmpty(argEpisode.getDescription())) {
            slimEpisode.setDescription(argEpisode.getDescription());
        }

        return slimEpisode;
    }

    public static String toJson(@NonNull FeedItem argEpisode) {
        JSONObject json = new JSONObject();
        // strings
        json.put("url", argEpisode.getURL().toString());
        json.put("remote_id", argEpisode.remote_id);
        json.put("title", argEpisode.title);
        json.put("author", argEpisode.author);
        json.put("date", argEpisode.date);
        json.put("content", argEpisode.content);
        json.put("resource", argEpisode.resource);
        json.put("duration_string", argEpisode.duration_string);
        json.put("image", argEpisode.image);
        json.put("filename", argEpisode.getFilename());
        json.put("uri", argEpisode.uri);
        json.put("subtitle", argEpisode.sub_title);

        // long/int
        json.put("duration_ms", argEpisode.duration_ms);
        json.put("sub_id", argEpisode.sub_id);
        json.put("filesize", argEpisode.filesize);
        json.put("episodeNumber", argEpisode.episodeNumber);
        json.put("offset", argEpisode.offset);
        json.put("status", argEpisode.status);
        json.put("listened", argEpisode.listened);
        json.put("priority", argEpisode.priority);
        json.put("length", argEpisode.length);
        json.put("lastUpdate", argEpisode.lastUpdate);

        return json.toJSONString();
    }

    public static FeedItem fromJson(@NonNull String argjson, @NonNull ContentResolver argContentResolver) {
        FeedItem item = null;
        boolean updateItem = false;
        if (argjson != null) {
            String url = null;
            String remote_id = null;
            String title = null;
            String author = null;
            String date = null;
            String content = null;
            String duration_string = null;
            String image = null;
            String filename = null;
            String subtitle = null;

            Number duration_ms = -1;
            Number sub_id = -1;
            Number filesize = -1;
            Number episodeNumber = -1;
            Number offset = -1;
            Number status = -1;
            Number listened = -1;
            Number priority = -1;
            Number length = -1;
            Number lastUpdate = -1;

            Object rootObject = JSONValue.parse(argjson);
            JSONObject mainObject = (JSONObject) rootObject;
            if (mainObject != null) {
                url = mainObject.get("url").toString();

                Object remoteIdObject = mainObject.get("remote_id");
                if (remoteIdObject != null)
                    remote_id = mainObject.get("remote_id").toString();

                title = mainObject.get("title").toString();
                author = mainObject.get("author").toString();
                date = mainObject.get("date").toString();
                content = mainObject.get("content").toString();
                duration_string = mainObject.get("duration_string").toString();
                image = mainObject.get("image").toString();
                filename = mainObject.get("filename").toString();
                subtitle = mainObject.get("subtitle").toString();

                duration_ms = (Number) mainObject.get("duration_ms");
                sub_id = (Number) mainObject.get("sub_id");
                filesize = (Number) mainObject.get("filesize");
                episodeNumber = (Number) mainObject.get("episodeNumber");
                offset = (Number) mainObject.get("offset");
                status = (Number) mainObject.get("status");
                listened = (Number) mainObject.get("listened");
                priority = (Number) mainObject.get("priority");
                length = (Number) mainObject.get("length");
                lastUpdate = (Number) mainObject.get("lastUpdate");

                item = FeedItem.getByURL(argContentResolver, url);
                if (item == null) {
                    item = new FeedItem();
                    updateItem = true;
                } else {
                    updateItem = item.getLastUpdate() < lastUpdate.longValue();
                }
            }

            if (url != null)
                item.url = url;
            if (remote_id != null)
                item.remote_id = title;
            if (title != null)
                item.title = title;
            if (author != null)
                item.author = author;
            if (date != null)
                item.date = date;
            if (content != null)
                item.content = content;
            if (duration_string != null)
                item.duration_string = duration_string;
            if (image != null)
                item.image = image;
            if (filename != null)
                item.setFilename(filename);
            if (subtitle != null)
                item.sub_title = subtitle;

            if (duration_ms.longValue() > -1)
                item.duration_ms = duration_ms.longValue();
            if (sub_id.longValue() > -1)
                item.sub_id = sub_id.longValue();
            if (filesize.longValue() > -1)
                item.filesize = filesize.longValue();
            if (episodeNumber.longValue() > -1)
                item.episodeNumber = episodeNumber.intValue();
            if (offset.longValue() > -1)
                item.offset = offset.intValue();
            if (status.longValue() > -1)
                item.status = status.intValue();
            if (listened.longValue() > -1)
                item.listened = listened.intValue();
            if (priority.longValue() > -1)
                item.priority = priority.intValue();
            if (length.longValue() > -1)
                item.length = length.longValue();
            if (lastUpdate.longValue() > -1)
                item.lastUpdate = lastUpdate.longValue();

            if (updateItem)
                item.update(argContentResolver);

        }

        return item;
    }
}
