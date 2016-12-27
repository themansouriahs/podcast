package org.bottiger.podcast.utils;

import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.telephony.TelephonyManager;
import android.text.Html;
import android.text.Spannable;
import android.text.Spanned;
import android.text.SpannedString;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Base64;
import android.util.Patterns;

import org.bottiger.podcast.provider.IEpisode;
import org.unbescape.html.HtmlEscape;
import org.unbescape.html.HtmlEscapeLevel;
import org.unbescape.html.HtmlEscapeType;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.zip.Adler32;

public class StrUtils {

    private static DecimalFormat mTimeDecimalFormat = new DecimalFormat("00");

    /*
     * Calculates the current position by parsing the Duration String
     * Kind of a hack
     */
    public static String formatDuration(IEpisode item) {
    	if (item.getDuration() == 0) 
    		return "00:00";

		return DateUtils.formatElapsedTime(item.getDuration());
    }

	public static String formatUrl(@Nullable String argUrl) {
		if (!StrUtils.isValidUrl(argUrl)) {
			return argUrl;
		}

		assert argUrl != null;

		try {
			return new URL(argUrl).getHost();
		} catch (MalformedURLException e) {
			return argUrl.split("/")[0]; //NoI18N
		}
	}

	public static boolean isValidUrl(@Nullable URL argURL) {
		if (argURL == null)
			return false;

		return isValidUrl(argURL.toString());
	}

	public static boolean isValidUrl(@Nullable String argUrl) {
		if (TextUtils.isEmpty(argUrl))
			return false;

		return Patterns.WEB_URL.matcher(argUrl).matches();
	}
    
    
    public static int parseTimeToSeconds(String duration) {
    	int seconds = 0;
    	
    	if (duration.length() == 0) 
    		return 0;
    	else {
    	Date date = null;
    	SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss");
    	SimpleDateFormat sdf2 = new SimpleDateFormat("mm:ss");
        try {
            date = sdf.parse(duration);            
            
        } catch (Exception e) {
            //e.printStackTrace();
        }
        
        if (date == null) {
        	try {
				date = sdf2.parse(duration);
			} catch (ParseException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
        }
        
        seconds = date.getSeconds() + date.getMinutes() * 60 + date.getHours() * 3600;
        
    	}
    	
    	return seconds;
    }
    
    // 0 < progress < 1
    public static String formatTime(float progress, String duration) {
    	
    	int seconds = parseTimeToSeconds(duration);
        float secondsPlayed = seconds*progress;
        return formatTime((long)secondsPlayed*1000);

    }
    
    public static String getTimeFromOffset(int offset, long length, String duration) {
    	long durationInSeconds = parseTimeToSeconds(duration);
    	float progress = (float)offset / (float)length;
    	String currentTime = formatTime((long)(durationInSeconds*progress)*1000);
    	return currentTime;
    }

	/*
	Fast but not so readable.
	From here: http://stackoverflow.com/questions/9027317/how-to-convert-milliseconds-to-hhmmss-format
 	*/
	public static String formatTime(final long argMillis) {

		long millis = Math.abs(argMillis);

		long seconds = TimeUnit.MILLISECONDS.toSeconds(millis)
				- TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis));
		long minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
				- TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis));
		long hours = TimeUnit.MILLISECONDS.toHours(millis);

		StringBuilder stringBuilder = new StringBuilder();

		if (argMillis < 0) {
			stringBuilder.append("-");
		}

		if (hours > 0) {
			stringBuilder.append(hours < 10 ? String.valueOf("" + hours) : String.valueOf(hours));
			stringBuilder.append(":");
		}

		stringBuilder.append(minutes == 0 ? "00" : minutes < 10 ? String.valueOf("0" + minutes) :
				String.valueOf(minutes));
		stringBuilder.append(":");
		stringBuilder.append(seconds == 0 ? "00" : seconds < 10 ? String.valueOf("0" + seconds) :
				String.valueOf(seconds));
		return stringBuilder.toString();
	}

	/*
Fast but not so readable.
From here: http://stackoverflow.com/questions/9027317/how-to-convert-milliseconds-to-hhmmss-format
 */
	public static String formatTimeText(final long argMillis, boolean argIncludeSeconds) {

		long millis = Math.abs(argMillis);

		long seconds = TimeUnit.MILLISECONDS.toSeconds(millis)
				- TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis));
		long minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
				- TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis));
		long hours = TimeUnit.MILLISECONDS.toHours(millis);

		StringBuilder stringBuilder = new StringBuilder();

		if (argMillis < 0) {
			stringBuilder.append("-");
		}

		if (hours > 0) {
			stringBuilder.append(hours < 10 ? String.valueOf("" + hours) : String.valueOf(hours));
			stringBuilder.append("h");
		}

		stringBuilder.append(minutes == 0 ? "00" : minutes < 10 ? String.valueOf("0" + minutes) :
				String.valueOf(minutes));
		stringBuilder.append("m");

		if (argIncludeSeconds) {
			stringBuilder.append(seconds == 0 ? "00" : seconds < 10 ? String.valueOf("0" + seconds) :
					String.valueOf(seconds));
			stringBuilder.append("s");
		}
		return stringBuilder.toString();
	}

	public static String fromHtmlCompat(@Nullable String argString) {
		if (TextUtils.isEmpty(argString)) {
			new SpannedString("");
		}

		//return HtmlEscape.escapeHtml(argString, HtmlEscapeType.DECIMAL_REFERENCES, HtmlEscapeLevel.LEVEL_0_ONLY_MARKUP_SIGNIFICANT_EXCEPT_APOS);

		Spanned spanned;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			spanned = Html.fromHtml(argString, Html.FROM_HTML_MODE_LEGACY);
		} else {
			spanned = Html.fromHtml(argString);
		}

		return spanned.toString();
	}
    
	public static String formatDownloadString(int offset, long length) {
		double d = 100.0 * offset / length;
		
		int status = (int) d;

		String str = "" + status + "% ( " + (formatLength(offset))
				+ " / " + (formatLength((int) length)) + " )";	
		
		return str;
	}
	
	public static int formatDownloadPrecent(int offset, long length) {
		double d = 100.0 * offset / length;
		
		int status = (int) d;
		return status;
	}

	// http://developer.android.com/reference/android/text/format/Formatter.html#formatFileSize%28android.content.Context,%20long%29
	@Deprecated
	public static String formatLength(int length) {

		length /= 1024;

		int i = (length % 1000);
		String s = "";
		if (i < 10) {
			s = "00" + i;
		} else if (i < 100) {
			s = "0" + i;
		} else {
			s += i;
		}

		String str = "" + (length / 1000) + "," + s + " KB";

		return str;
	}



	/**
	 * Get ISO 3166-1 alpha-2 country code for this device (or null if not available)
	 * @param context Context reference to get the TelephonyManager instance from
	 * @return country code or null
	 */
	public static String getUserCountry(Context context) {
		try {
			final TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
			final String simCountry = tm.getSimCountryIso();
			if (simCountry != null && simCountry.length() == 2) { // SIM country code is available
				return simCountry.toLowerCase(Locale.US);
			}
			else if (tm.getPhoneType() != TelephonyManager.PHONE_TYPE_CDMA) { // device is not 3G (would be unreliable)
				String networkCountry = tm.getNetworkCountryIso();
				if (networkCountry != null && networkCountry.length() == 2) { // network country code is available
					return networkCountry.toLowerCase(Locale.US);
				}
			}
		}
		catch (Exception e) { }
		return null;
	}

	/**
	 *
	 * @param argTitle The title of the episode
	 * @return
     */
	public static String formatTitle(@Nullable String argTitle) {
		if (argTitle == null)
			return "";

		String[] parts = argTitle.split("-"); // NoI18N
		String episodeTitle = parts[parts.length-1];

		return episodeTitle.trim();
	}

	public static String toBase64(@NonNull String argString) {
		return Base64.encodeToString(argString.getBytes(), Base64.DEFAULT);
	}

	public static String fromBase64(@NonNull String argString) throws UnsupportedEncodingException {
		return new String(Base64.decode(argString, Base64.DEFAULT), "UTF-8"); // NoI18N
	}

	public static String toBase32(@NonNull String argString) {
		BigInteger bigInt = new BigInteger(argString.getBytes());
		return bigInt.toString(32);
	}

	public static long Adler32(@NonNull String argString) {
		byte[] bytes = argString.getBytes();
		Adler32 adler32 = new Adler32();
		adler32.update(bytes, 0, bytes.length);
		return adler32.getValue();
	}

	public static String greatestCommonPrefix(String a, String b) {
		int minLength = Math.min(a.length(), b.length());
		for (int i = 0; i < minLength; i++) {
			if (a.charAt(i) != b.charAt(i)) {
				return a.substring(0, i);
			}
		}
		return a.substring(0, minLength);
	}

}
