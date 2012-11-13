package info.bottiger.podcast.utils;

import info.bottiger.podcast.provider.FeedItem;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.text.format.DateFormat;

public class StrUtils {

    private static DecimalFormat mTimeDecimalFormat = new DecimalFormat("00");

    /*
     * Calculates the current position by parsing the Duration String
     * Kind of a hack
     */
    public static String formatTime(FeedItem item) {
    	float progress = item.offset / item.length;
    	return formatTime(progress, item.duration);
    }
    
    public static String formatTime(float progress, String duration) {
    	if (duration.length() == 0) 
    		return "";
    	else {
    	Date date = null;
    	SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss");
    	SimpleDateFormat sdf2 = new SimpleDateFormat("mm:ss");
        try {
            date = sdf.parse(duration);            
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        if (date == null) {
        	try {
				date = sdf2.parse(duration);
			} catch (ParseException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
        }
        
        int seconds = date.getSeconds() + date.getMinutes() * 60 + date.getHours() * 3600;
        float secondsPlayed = seconds*progress;
        return formatTime((long)secondsPlayed*1000);
    	}

    }
    
    public static String formatTime(long ms) {
    	long s = ms / 1000;
    	long m = s / 60;
    	s = s % 60;
    	long h = m / 60;
    	m = m % 60;
    	String m_s = mTimeDecimalFormat.format(m) + ":" 
    		+ mTimeDecimalFormat.format(s);
    	if (h > 0) {
    		// show also hour
    		return "" + h + ":" + m_s;
    	} else {
    		// Show only minute:second
    		return m_s;
    	}
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
