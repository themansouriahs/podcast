package org.bottiger.podcast.parser;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringDef;
import android.support.v4.util.Pair;
import android.text.TextUtils;
import android.util.Log;
import android.util.Xml;

import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.flavors.CrashReporter.VendorCrashReporter;
import org.bottiger.podcast.model.Library;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.provider.ISubscription;
import org.bottiger.podcast.provider.SlimImplementations.SlimEpisode;
import org.bottiger.podcast.provider.SlimImplementations.SlimSubscription;
import org.bottiger.podcast.provider.Subscription;
import org.bottiger.podcast.service.Downloader.SoundWavesDownloadManager;
import org.bottiger.podcast.utils.DateUtils;
import org.bottiger.podcast.utils.StorageUtils;
import org.bottiger.podcast.utils.StrUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import static org.bottiger.podcast.utils.StorageUtils.VIDEO;

/**
 * Created by aplb on 19-09-2015.
 */
public class FeedParser {

    private static final String TAG = "FeedParser";

    private String[] DURATION_FORMATS = {"HH:mm:ss", "mm:ss"};

    @Nullable
    private DateUtils.Hint mDateFormatHint = null;

    // We don't use namespaces
    private static final String ns = null;

    private static final String topTag = "rss";
    private static final String startTag = "channel";
    private static final String EPISODE_ITEM_TAG = "item";

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({ FEED_TYPE_RSS, FEED_TYPE_ATOM, FEED_TYPE_ITUNES})
    public @interface FeedType {}
    private static final String FEED_TYPE_RSS = "rss";
    private static final String FEED_TYPE_ATOM = "atom";
    private static final String FEED_TYPE_ITUNES = "itunes";

    /**
     * RSS optional tags
     * https://validator.w3.org/feed/docs/rss2.html#optionalChannelElements
     */
    private static final String SUBSCRIPTION_TITLE_TAG = "title";
    private static final String SUBSCRIPTION_LINK_TAG = "link";
    private static final String SUBSCRIPTION_DESCRIPTION_TAG = "summary";
    private static final String SUBSCRIPTION_LANGUAGE_TAG = "language";
    private static final String SUBSCRIPTION_COPYRIGHT_TAG = "copyright";
    private static final String SUBSCRIPTION_MANAGING_EDITOR_TAG = "managingEditor";
    private static final String SUBSCRIPTION_WEBMASTER_TAG = "webMaster";
    private static final String SUBSCRIPTION_PUB_DATE_TAG = "pubDate";
    private static final String SUBSCRIPTION_LAST_BUILD_DATE_TAG = "lastBuildDate";
    private static final String SUBSCRIPTION_CATEGORY_TAG = "category";
    private static final String SUBSCRIPTION_GENERATOR_TAG = "generator";
    private static final String SUBSCRIPTION_DOCS_TAG = "docs";
    private static final String SUBSCRIPTION_CLOUD_TAG = "cloud";
    private static final String SUBSCRIPTION_TTL_TAG = "ttl";
    private static final String SUBSCRIPTION_IMAGE_TAG = "image";
    private static final String SUBSCRIPTION_TEXT_INPUT_TAG = "textInput";
    private static final String SUBSCRIPTION_SKIP_HOURS_TAG = "skipHours";
    private static final String SUBSCRIPTION_SKIP_DAYS_TAG = "skipDays";

    private static final String SUBSCRIPTION_IMAGE_URL_TAG = "url";
    private static final String SUBSCRIPTION_IMAGE_URL_ATTRIBUTE = "href";
    private static final String SUBSCRIPTION_IMAGE_TITLE_TAG = "title";
    private static final String SUBSCRIPTION_IMAGE_LINK_TAG = "link";


    @Retention(RetentionPolicy.SOURCE)
    @StringDef({
            EPISODE_TITLE_TAG, EPISODE_LINK_TAG, EPISODE_DESCRIPTION_TAG,
            EPISODE_AUTHOR_TAG, EPISODE_CATEGORY_TAG, EPISODE_COMMENTS_TAG, EPISODE_ENCLOSURE_TAG, EPISODE_GUID_TAG,
            EPISODE_PUB_DATE_TAG, EPISODE_SOURCE_TAG
    })
    public @interface RssItemTag {}

    private static final String EPISODE_TITLE_TAG = "title";
    private static final String EPISODE_LINK_TAG = "link";
    private static final String EPISODE_DESCRIPTION_TAG = "description";
    private static final String EPISODE_AUTHOR_TAG = "author";
    private static final String EPISODE_CATEGORY_TAG = "category";
    private static final String EPISODE_COMMENTS_TAG = "comments";
    private static final String EPISODE_ENCLOSURE_TAG = "enclosure";
    private static final String EPISODE_GUID_TAG = "guid";
    private static final String EPISODE_PUB_DATE_TAG = "pubDate";
    private static final String EPISODE_SOURCE_TAG = "source";

    final String EPISODE_ENCLOSURE_URL = "url"; // in bytes
    final String EPISODE_ENCLOSURE_FILESISZE = "length"; // in bytes
    final String EPISODE_ENCLOSURE_MIMETYPE = "type";
    final String ITUNES_IMAGE_HREF = "href";

    /**
     * Not complete
     */
    @Retention(RetentionPolicy.SOURCE)
    @StringDef({
            ITUNES_DURATION_TAG, ITUNES_IMAGE_TAG, ITUNES_SUMMARY_TAG
    })
    public @interface ItunesItemTag {}
    private static final String ITUNES_DURATION_TAG = "itunes:duration";
    private static final String ITUNES_IMAGE_TAG = "itunes:image";
    private static final String ITUNES_SUMMARY_TAG = "itunes:summary";

    private static class EpisodeEnclosure {
        public String url;
        public String mimeType;
        public long filesize;
    }

    private boolean mParsingSlim = false;

    /**
     * Parse a ISubscription by reading the stream. The parser will update the subscriptions metadata
     * and add new episodes to the subscription.
     *
     * The parser will disable all notification during the parsing and emit a single notification at
     * the end if required.
     *
     * The parser will not persist the subscription.
     *
     * @param argSubscription The subscription which will be updated
     * @param in The inputstream wit the datat
     * @param argContext A Context
     * @return The same subscription which was given as input
     * @throws XmlPullParserException
     * @throws IOException
     */
    public ISubscription parse(@NonNull ISubscription argSubscription,
                               @NonNull InputStream in,
                               @NonNull Context argContext) throws XmlPullParserException, IOException {
        argSubscription.setIsRefreshing(true);
        mParsingSlim = argSubscription instanceof SlimSubscription;
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            parser.nextTag();
            return readFeed(parser, argSubscription, argContext);
        } finally {
            argSubscription.setIsRefreshing(false);
            in.close();
        }
    }

    private ISubscription readFeed(@NonNull XmlPullParser parser,
                                   @NonNull ISubscription argSubscription,
                                   @NonNull Context argContext) throws XmlPullParserException, IOException {


        boolean addedEpisodes = false;

        parser.require(XmlPullParser.START_TAG, ns, topTag);
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            // Starts by looking for the entry tag
            switch (name) {
                case startTag:
                    addedEpisodes = readChannel(parser, argSubscription, argContext);
                    break;
                default:
                    skip(parser);
                    break;
            }
        }

        if (addedEpisodes && !isParsingSlimSubscription()) {
            Subscription sub = ((Subscription) argSubscription);
            SoundWaves.getAppContext(argContext).getLibraryInstance().addEpisodes(sub);

            sub.setLastItemUpdated(System.currentTimeMillis());
            sub.notifyEpisodeAdded(false);
        }

        return argSubscription;
    }

    private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }


    /////////////////////////////

    // Parses the contents of an entry. If it encounters a title, summary, or link tag, hands them off
    // to their respective "read" methods for processing. Otherwise, skips the tag.
    private boolean readChannel(@NonNull XmlPullParser parser,
                                @NonNull ISubscription argSubscription,
                                @NonNull Context argContext) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, startTag);
        boolean addedEpisodes = false;
        Library library = SoundWaves.getAppContext(argContext).getLibraryInstance();

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

            String name = parser.getName();

            if (name.startsWith(FEED_TYPE_ITUNES)) {
                readSubscriptionItunesTag(parser, name, argSubscription);
                continue;
            }

            switch (name) {
                case SUBSCRIPTION_TITLE_TAG: {
                    argSubscription.setTitle(readTitle(parser));
                    break;
                }
                case SUBSCRIPTION_LINK_TAG: {
                    argSubscription.setLink(readLink(parser));
                    break;
                }
                case SUBSCRIPTION_DESCRIPTION_TAG: {
                    argSubscription.setDescription(readSummary(parser));
                    break;
                }
                case EPISODE_ITEM_TAG: {
                    IEpisode episode = readEpisode(parser, argSubscription);

                    // Traditional approach
                    //if (isParsingSlimSubscription()) {
                    //    argSubscription.addEpisode(episode);
                    //}
                    //addedEpisodes = addedEpisodes || !library.addEpisode(episode);

                    // Bulk insert.
                    addedEpisodes = argSubscription.addEpisode(episode) || addedEpisodes;

                    break;
                }
                case SUBSCRIPTION_IMAGE_TAG: {
                    String image = readSubscriptionImage(parser);
                    argSubscription.setImageURL(image);
                    break;
                }
                case SUBSCRIPTION_PUB_DATE_TAG:
                case SUBSCRIPTION_LANGUAGE_TAG:
                case SUBSCRIPTION_COPYRIGHT_TAG:
                case SUBSCRIPTION_MANAGING_EDITOR_TAG:
                case SUBSCRIPTION_WEBMASTER_TAG:
                case SUBSCRIPTION_LAST_BUILD_DATE_TAG:
                case SUBSCRIPTION_CATEGORY_TAG:
                case SUBSCRIPTION_GENERATOR_TAG:
                case SUBSCRIPTION_DOCS_TAG:
                case SUBSCRIPTION_CLOUD_TAG:
                case SUBSCRIPTION_TTL_TAG:
                case SUBSCRIPTION_TEXT_INPUT_TAG:
                case SUBSCRIPTION_SKIP_HOURS_TAG:
                case SUBSCRIPTION_SKIP_DAYS_TAG:
                default: {
                    skip(parser);
                }
            }
        }

        return addedEpisodes;
    }

    private void readSubscriptionItunesTag(XmlPullParser parser, String name, ISubscription argSubscription) throws XmlPullParserException, IOException {
        switch (name) {
            case ITUNES_IMAGE_TAG: {
                String image = parser.getAttributeValue(null, ITUNES_IMAGE_HREF);
                argSubscription.setImageURL(image);
                parser.nextTag();
                break;
            }
            default: {
                skip(parser);
            }
        }
    }

    // Parses the contents of an entry. If it encounters a title, summary, or link tag, hands them off
    // to their respective "read" methods for processing. Otherwise, skips the tag.
    private IEpisode readEpisode(@NonNull XmlPullParser parser,
                                 @NonNull ISubscription argSubscription) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, EPISODE_ITEM_TAG);

        IEpisode episode;

        if (isParsingSlimSubscription()) {
            episode = new SlimEpisode();
        } else {
            FeedItem item = new FeedItem(true);
            item.setIsParsing(true);
            item.setFeed((Subscription)argSubscription);
            episode = item;
        }

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();

            if (name.startsWith(FEED_TYPE_ITUNES)) { // FIXME FEED_TYPE_ITUNES.equals(prefix)
                readEpisodeItunesTag(parser, name, episode);
                continue;
            }

            switch (name) {
                case EPISODE_TITLE_TAG: {
                    episode.setTitle(readTitle(parser));
                    break;
                }
                case EPISODE_LINK_TAG: {
                    episode.setPageLink(readSimpleTag(parser, EPISODE_LINK_TAG));
                    break;
                }
                case EPISODE_DESCRIPTION_TAG: {
                    String description = readSimpleTag(parser, EPISODE_DESCRIPTION_TAG);
                    if (TextUtils.isEmpty(episode.getDescription())) {
                        episode.setDescription(description);
                    }
                    break;
                }
                case EPISODE_PUB_DATE_TAG: {
                    try {
                        Date date = readDate(parser);
                        if (date != null) {
                            episode.setPubDate(DateUtils.preventDateInTheFutre(date));
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "Could not parse date from subscription: " + argSubscription + " e: " + e.toString());
                        String[] keys = new String[1];
                        String[] values = new String[1];

                        keys[0] = "Date parse error";
                        values[0]  = argSubscription.getURLString();

                        VendorCrashReporter.handleException(e, keys, values);
                    }
                    break;
                }
                case EPISODE_AUTHOR_TAG: {
                    episode.setAuthor(readSimpleTag(parser, EPISODE_AUTHOR_TAG));
                    break;
                }
                case EPISODE_ENCLOSURE_TAG: {
                    EpisodeEnclosure enclosure = readEnclosure(parser);
                    episode.setURL(enclosure.url);

                    // Planet money seems to tag all files with filesize 0
                    if (enclosure.filesize > 0)
                        episode.setFilesize(enclosure.filesize);

                    episode.setIsVideo(StorageUtils.getFileType(enclosure.mimeType) == VIDEO);
                    break;
                }
                case EPISODE_CATEGORY_TAG:
                case EPISODE_COMMENTS_TAG:
                case EPISODE_GUID_TAG:
                case EPISODE_SOURCE_TAG:
                default: {
                    skip(parser);
                }
            }
        }

        episode.setIsParsing(false, false);
        return episode;
    }

    private void readEpisodeItunesTag(@NonNull XmlPullParser parser, @NonNull String name, @NonNull IEpisode episode) throws IOException, XmlPullParserException {
        switch (name) {
            case ITUNES_DURATION_TAG: {
                // The duration should be of the format hh:mm:ss, or hh:mm
                // However, planet money seems to use an int as the number of minutes
                String unparsedDuration = readSimpleTag(ITUNES_DURATION_TAG, parser);
                long duration = -1;

                if (unparsedDuration.contains(":")) {

                    Date date = null;
                    SimpleDateFormat sdf = null;
                    for (String formatString : DURATION_FORMATS)
                    {
                        try
                        {
                            sdf = new SimpleDateFormat(formatString);
                            sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
                            date = sdf.parse(unparsedDuration);
                            Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
                            cal.setTime(date);
                            duration = cal.getTimeInMillis();
                            break;
                        }
                        catch (ParseException e) {
                        }
                    }
                } else {
                    // We assume it's the number of minutes
                    try {
                        duration = Long.parseLong(unparsedDuration) * 60 * 1000; // to ms
                    } catch (NumberFormatException nfe) {
                        // I have observed feeds with malform duration
                        duration = -1;
                    } catch (NullPointerException npe) {
                        // I have observed feeds with malform duration
                        duration = -1;
                    }
                }

                episode.setDuration(duration);
                break;
            }
            case ITUNES_IMAGE_TAG: {
                String image = parser.getAttributeValue(null, ITUNES_IMAGE_HREF);
                if (StrUtils.isValidUrl(image)) {
                    episode.setArtwork(image);
                }
                parser.nextTag();
                break;
            }
            case ITUNES_SUMMARY_TAG: {
                episode.setDescription(readSimpleTag(ITUNES_SUMMARY_TAG, parser));
                break;
            }
            default: {
                skip(parser);
            }
        }
    }

    /**
     * A hack
     */
    private String readSimpleTag(@ItunesItemTag String argTag, XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, argTag);
        String value = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, argTag);
        return value;
    }

    private String readSimpleTag(XmlPullParser parser, @RssItemTag String argTag) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, argTag);
        String value = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, argTag);
        return value;
    }

    // Processes title tags of an item in the feed.
    private String readTitle(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, EPISODE_TITLE_TAG);
        String title = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, EPISODE_TITLE_TAG);
        return title;
    }

    // Processes pubdate tag of an item in the feed.
    @Nullable
    private Date readDate(XmlPullParser parser) throws IOException, XmlPullParserException, ParseException {
        parser.require(XmlPullParser.START_TAG, ns, EPISODE_PUB_DATE_TAG);
        String pubDate = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, EPISODE_PUB_DATE_TAG);

        return cacheDateFormat(pubDate);
    }

    // Processes pubdate tag of an item in the feed.
    private Date readSubscriptionPubDate(XmlPullParser parser) throws IOException, XmlPullParserException, ParseException {
        parser.require(XmlPullParser.START_TAG, ns, SUBSCRIPTION_PUB_DATE_TAG);
        String pubDate = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, SUBSCRIPTION_PUB_DATE_TAG);

        return cacheDateFormat(pubDate);
    }

    private Date cacheDateFormat(@NonNull String argDate) throws IOException, XmlPullParserException, ParseException {
        Pair<Date, DateUtils.Hint> parsedDate = DateUtils.parse(argDate, mDateFormatHint);
        mDateFormatHint = parsedDate.second;
        return parsedDate.first;
    }

    // Processes link tags in the feed.
    private EpisodeEnclosure readEnclosure(XmlPullParser parser) throws IOException, XmlPullParserException {
        EpisodeEnclosure enclosure = new EpisodeEnclosure();

        parser.require(XmlPullParser.START_TAG, ns, EPISODE_ENCLOSURE_TAG);
        String tag = parser.getName();

        String url = parser.getAttributeValue(null, EPISODE_ENCLOSURE_URL);
        String fileSize = parser.getAttributeValue(null, EPISODE_ENCLOSURE_FILESISZE);
        String mimeType = parser.getAttributeValue(null, EPISODE_ENCLOSURE_MIMETYPE);

        enclosure.url = url;
        try {
            enclosure.filesize = Long.parseLong(fileSize);
        }
        catch (Exception e) {
            enclosure.filesize = -1;
        }
        enclosure.mimeType = mimeType;

        readText(parser);

        parser.require(XmlPullParser.END_TAG, ns, EPISODE_ENCLOSURE_TAG);
        return enclosure;
    }

    private String readSubscriptionImage(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, SUBSCRIPTION_IMAGE_TAG);

        String url = "";

        if (parser.getAttributeCount() > 0) {
            if (SUBSCRIPTION_IMAGE_URL_ATTRIBUTE.equals(parser.getAttributeName(0))) {
                String potentialUrl = parser.getAttributeValue(0);
                if (StrUtils.isValidUrl(potentialUrl)) {
                    url = potentialUrl;
                }
            }
        }

        while (!(parser.next() == XmlPullParser.END_TAG && parser.getName().equals(SUBSCRIPTION_IMAGE_TAG))) {
            if (SUBSCRIPTION_IMAGE_URL_TAG.equals(parser.getName())) {
                url = readText(parser);
            }
        }

        String image = url;
        parser.require(XmlPullParser.END_TAG, ns, SUBSCRIPTION_IMAGE_TAG);
        return image;
    }

    private String readLink(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, EPISODE_LINK_TAG);
        String title = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, EPISODE_LINK_TAG);
        return title;
    }

    // Processes summary tags in the feed.
    private String readSummary(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, EPISODE_DESCRIPTION_TAG);
        String summary = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, EPISODE_DESCRIPTION_TAG);
        return summary;
    }

    // For the tags title and summary, extracts their text values.
    private String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
        if (parser.next() == XmlPullParser.TEXT) {
            String result = parser.getText();
            parser.nextTag();
            return result;
        }

        return "";
    }

    private boolean isParsingSlimSubscription() {
        return mParsingSlim;
    }

}
