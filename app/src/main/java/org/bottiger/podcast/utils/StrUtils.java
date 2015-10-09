package org.bottiger.podcast.utils;


import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.format.DateUtils;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.IEpisode;

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
	public static String formatTime(final long millis) {
		long seconds = TimeUnit.MILLISECONDS.toSeconds(millis)
				- TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis));
		long minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
				- TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis));
		long hours = TimeUnit.MILLISECONDS.toHours(millis);

		StringBuilder b = new StringBuilder();

		if (hours > 0) {
			b.append(hours < 10 ? String.valueOf("" + hours) : String.valueOf(hours));
			b.append(":");
		}

		b.append(minutes == 0 ? "00" : minutes < 10 ? String.valueOf("0" + minutes) :
				String.valueOf(minutes));
		b.append(":");
		b.append(seconds == 0 ? "00" : seconds < 10 ? String.valueOf("0" + seconds) :
				String.valueOf(seconds));
		return b.toString();
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
}
