package org.bottiger.podcast.utils;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Created by aplb on 19-09-2015.
 */
public class DateUtils {

    private static final Map<Pattern, String> DATE_FORMAT_REGEXPS = new HashMap<Pattern, String>() {{
        put(Pattern.compile("^\\d{8}$"), "yyyyMMdd");
        put(Pattern.compile("^\\d{1,2}-\\d{1,2}-\\d{4}$"), "dd-MM-yyyy");
        put(Pattern.compile("^\\d{4}-\\d{1,2}-\\d{1,2}$"), "yyyy-MM-dd");
        put(Pattern.compile("^\\d{1,2}/\\d{1,2}/\\d{4}$"), "MM/dd/yyyy");
        put(Pattern.compile("^\\d{4}/\\d{1,2}/\\d{1,2}$"), "yyyy/MM/dd");
        put(Pattern.compile("^\\d{1,2}\\s[a-z]{3}\\s\\d{4}$"), "dd MMM yyyy");
        put(Pattern.compile("^\\d{1,2}\\s[a-z]{4,}\\s\\d{4}$"), "dd MMMM yyyy");
        put(Pattern.compile("^\\d{12}$"), "yyyyMMddHHmm");
        put(Pattern.compile("^\\d{8}\\s\\d{4}$"), "yyyyMMdd HHmm");
        put(Pattern.compile("^\\d{1,2}-\\d{1,2}-\\d{4}\\s\\d{1,2}:\\d{2}$"), "dd-MM-yyyy HH:mm");
        put(Pattern.compile("^\\d{4}-\\d{1,2}-\\d{1,2}\\s\\d{1,2}:\\d{2}$"), "yyyy-MM-dd HH:mm");
        put(Pattern.compile("^\\d{1,2}/\\d{1,2}/\\d{4}\\s\\d{1,2}:\\d{2}$"), "MM/dd/yyyy HH:mm");
        put(Pattern.compile("^\\d{4}/\\d{1,2}/\\d{1,2}\\s\\d{1,2}:\\d{2}$"), "yyyy/MM/dd HH:mm");
        put(Pattern.compile("^\\d{1,2}\\s[a-z]{3}\\s\\d{4}\\s\\d{1,2}:\\d{2}$"), "dd MMM yyyy HH:mm");
        put(Pattern.compile("^\\d{1,2}\\s[a-z]{4,}\\s\\d{4}\\s\\d{1,2}:\\d{2}$"), "dd MMMM yyyy HH:mm");
        put(Pattern.compile("^\\d{14}$"), "yyyyMMddHHmmss");
        put(Pattern.compile("^\\d{8}\\s\\d{6}$"), "yyyyMMdd HHmmss");
        put(Pattern.compile("^\\d{4}-\\d{1,2}-\\d{1,2}T\\d{1,2}:\\d{2}:\\d{2}$", Pattern.CASE_INSENSITIVE), "yyyy-MM-dd'T'HH:mm:ss"); // 2016-11-26T00:00:00
        put(Pattern.compile("^\\d{1,2}-\\d{1,2}-\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}$"), "dd-MM-yyyy HH:mm:ss");
        put(Pattern.compile("^\\d{4}-\\d{1,2}-\\d{1,2}\\s\\d{1,2}:\\d{2}:\\d{2}$"), "yyyy-MM-dd HH:mm:ss");
        put(Pattern.compile("^\\d{1,2}/\\d{1,2}/\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}$"), "MM/dd/yyyy HH:mm:ss");
        put(Pattern.compile("^\\d{1,2}/\\d{1,2}/\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2} [PMApma]{2}$"), "MM/dd/yyyy HH:mm:ss a"); // am/pm
        put(Pattern.compile("^\\d{1,2}/\\d{1,2}/\\d{4}\\s\\d{1,2}:\\d{2} [PMApma]{2}$"), "MM/dd/yyyy HH:mm a"); // am/pm
        put(Pattern.compile("^\\d{4}/\\d{1,2}/\\d{1,2}\\s\\d{1,2}:\\d{2}:\\d{2}$"), "yyyy/MM/dd HH:mm:ss");
        put(Pattern.compile("^\\d{1,2}\\s[A-Za-z]{3}\\s\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}$"), "dd MMM yyyy HH:mm:ss");
        put(Pattern.compile("^\\d{1,2}\\s[A-Za-z]{4,}\\s\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}$"), "dd MMMM yyyy HH:mm:ss");
        put(Pattern.compile("^\\d{1,2}\\s[A-Za-z]{3}\\s\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}\\s[0-9+-]+$"), "dd MMM yyyy HH:mm:ss Z");
        put(Pattern.compile("^\\d{1,2}\\s[A-Za-z]{4,}\\s\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}\\s[A-Za-z0-9+-]+$"), "dd MMMM yyyy HH:mm:ss z");
        put(Pattern.compile("^[A-Za-z]{3}\\s\\d{1,2}\\s[A-Za-z]{3,9}\\s\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}\\s[0-9+-]+$"), "EEE dd MMM yyyy HH:mm:ss Z"); // missing ,
        put(Pattern.compile("^[A-Za-z]{3}\\s\\d{1,2}\\s[A-Za-z]{3,9}\\s\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}\\s[A-Za-z]+[0-9+-]{0,5}$"), "EEE dd MMM yyyy HH:mm:ss z"); // missing ,
        put(Pattern.compile("^[A-Za-z]{3},\\s\\d{1,2}\\s[A-Za-z]{3,9}\\s\\d{4}\\s\\d{1,2}:\\d{2}\\s[0-9+-]+$"), "EEE, dd MMM yyyy HH:mm Z"); // no seconds
        put(Pattern.compile("^[A-Za-z]{3},\\s\\d{1,2}\\s[A-Za-z]{3,9}\\s\\d{4}\\s\\d{1,2}:\\d{2}\\s[A-Za-z]+[0-9+-]{0,5}$"), "EEE, dd MMM yyyy HH:mm z"); // no seconds
        put(Pattern.compile("^[A-Za-z]{3},\\s\\d{1,2}\\s[A-Za-z]{3,9}\\s\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}\\s[0-9+-]+$"), "EEE, dd MMM yyyy HH:mm:ss Z");
        put(Pattern.compile("^[A-Za-z]{3},\\s\\d{1,2}\\s[A-Za-z]{3,9}\\s\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}\\s[A-Za-z]+[0-9+-]{0,5}$"), "EEE, dd MMM yyyy HH:mm:ss z");
    }};

    private static final Map<String, String> UNSUPPORTED_TIME_ZONE = new HashMap<String, String>() {{
        put("BST", "GMT+0100");
        put("PST", "GMT-0800");
        put("PDT", "GMT-0700");
        put("EST", "GMT-0500");
        put("EDT", "GMT-0400");
    }};

    private static final Map<String, SimpleDateFormat> SIMPLE_DATE_FORMATS_LUT = new HashMap<>(DATE_FORMAT_REGEXPS.size());
    private static Pattern[] sDateFormatKeys = null;

    public interface Hint {
        Pattern get();
    }

    // Converters ---------------------------------------------------------------------------------

    /**
     * Parse the given date string to date object and return a date instance based on the given
     * date string. This makes use of the {@link DateUtils#determineDateFormat(String, Hint)} to determine
     * the SimpleDateFormat pattern to be used for parsing.
     *
     * @param dateString The date string to be parsed to date object.
     * @return The parsed date object.
     * @throws ParseException If the date format pattern of the given date string is unknown, or if
     * the given date string or its actual date is invalid based on the date format pattern.
     */
    public static Pair<Date, Hint> parse(@NonNull String dateString, @Nullable Hint argHint) throws ParseException {
        Pair<String, Hint> dateFormat = determineDateFormat(dateString, argHint);
        if (dateFormat == null) {
            throw new ParseException("Unknown date format: " + dateString, 0);
        }

        Date returnDate = parse(dateString, dateFormat.first);
        return new Pair<>(returnDate, dateFormat.second);
    }

    /**
     * Validate the actual date of the given date string based on the given date format pattern and
     * return a date instance based on the given date string.
     *
     * @param dateString The date string.
     * @param dateFormat The date format pattern which should respect the SimpleDateFormat rules.
     * @return The parsed date object.
     * @throws ParseException If the given date string or its actual date is invalid based on the
     * given date format pattern.
     * @see SimpleDateFormat
     */
    public static synchronized Date parse(@NonNull String dateString, @NonNull String dateFormat) throws ParseException {

        dateString = fixUnsupportedTimeZones(dateString);

        try {
            SimpleDateFormat simpleDateFormat = getSimpleDateFormat(dateFormat, Locale.getDefault());
            return simpleDateFormat.parse(dateString);
        } catch (ParseException pe) {
            ParseException pe2 = pe;
            return simpleDateFormat.parse(dateString);
        }
    }

    private static String fixUnsupportedTimeZones(@NonNull String dateString) {
        String result = dateString;
        // This is a hack to deal with time zones not known to Java
        Iterator it = UNSUPPORTED_TIME_ZONE.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            result = result.replace(pair.getKey().toString(), pair.getValue().toString());
        }
        return result;
    }

    // Validators ---------------------------------------------------------------------------------

    /**
     * Checks whether the actual date of the given date string is valid. This makes use of the
     * {@link DateUtils#determineDateFormat(String, Hint)} to determine the SimpleDateFormat pattern to be
     * used for parsing.
     * @param dateString The date string.
     * @return True if the actual date of the given date string is valid.
     */
    public static boolean isValidDate(String dateString) {
        try {
            parse(dateString, getHint(null));
            return true;
        } catch (ParseException e) {
            return false;
        }
    }

    /**
     * Checks whether the actual date of the given date string is valid based on the given date
     * format pattern.
     * @param dateString The date string.
     * @param dateFormat The date format pattern which should respect the SimpleDateFormat rules.
     * @return True if the actual date of the given date string is valid based on the given date
     * format pattern.
     * @see SimpleDateFormat
     */
    public static boolean isValidDate(String dateString, String dateFormat) {
        try {
            parse(dateString, dateFormat);
            return true;
        } catch (ParseException e) {
            return false;
        }
    }

    // Checkers -----------------------------------------------------------------------------------

    /**
     * Determine SimpleDateFormat pattern matching with the given date string. Returns null if
     * format is unknown. You can simply extend DateUtil with more formats if needed.
     *
     * Optimization: This method has proven to be very slow. Therefore we return a Pair where the
     * second value it a hint of the format for caching higher up.
     *
     * @param dateString The date string to determine the SimpleDateFormat pattern for.
     * @return The matching SimpleDateFormat pattern, or null if format is unknown.
     * @see SimpleDateFormat
     */
    @Nullable
    private static Pair<String, Hint> determineDateFormat(String dateString, @Nullable Hint argKeyHint) {

        String dateStringLowerCase = dateString.toLowerCase();

        // Test the hint first
        if (argKeyHint != null) {
            if (argKeyHint.get().matcher(dateStringLowerCase).matches()) { // dateStringLowerCase.matches(argKeyHint.get())
                return new Pair<>(DATE_FORMAT_REGEXPS.get(argKeyHint.get()), argKeyHint);
            }
        }

        for (final Pattern regexp : getDateFormatKeys()) {

            if (argKeyHint != null && regexp.equals(argKeyHint.get()))
                continue;

            if (regexp.matcher(dateStringLowerCase).matches()) { // dateStringLowerCase.matches(regexp)
                return new Pair<>(DATE_FORMAT_REGEXPS.get(regexp), getHint(regexp));
            }
        }
        return null; // Unknown format.
    }

    @Nullable
    private static Hint getHint(@Nullable final Pattern argKey) {
        if (argKey == null)
            return null;

        return new Hint() {
            @Override
            public Pattern get() {
                return argKey;
            }
        };
    }

    public static Date preventDateInTheFutre(@NonNull Date argDate) {
        Date now = new Date();
        Date newDate = argDate.before(now) ? argDate : now;

        return newDate;
    }

    private static synchronized Pattern[] getDateFormatKeys() {
        if (sDateFormatKeys == null) {
            Set<Pattern> keySet = DATE_FORMAT_REGEXPS.keySet();
            sDateFormatKeys = keySet.toArray(new Pattern[keySet.size()]);
            Collections.reverse(Arrays.asList(sDateFormatKeys));
        }

        return sDateFormatKeys;
    }

    private static SimpleDateFormat getSimpleDateFormat(@NonNull String argDateFormat, Locale locale) {
        String lutKey = argDateFormat + locale.toString();
        SimpleDateFormat simpleDateFormat = SIMPLE_DATE_FORMATS_LUT.get(lutKey);

        if (simpleDateFormat == null) {
            synchronized (SIMPLE_DATE_FORMATS_LUT) {
                simpleDateFormat = new SimpleDateFormat(argDateFormat, locale);
                simpleDateFormat.setLenient(false); // Don't automatically convert invalid date.

                SIMPLE_DATE_FORMATS_LUT.put(lutKey, simpleDateFormat);
            }
        }

        return simpleDateFormat;
    }
}
