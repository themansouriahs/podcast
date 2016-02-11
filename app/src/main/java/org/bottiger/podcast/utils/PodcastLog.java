package org.bottiger.podcast.utils;

import android.os.Environment;
import android.support.design.widget.CoordinatorLayout;

import org.bottiger.podcast.BuildConfig;
import org.bottiger.podcast.SoundWaves;

import java.io.File;
import java.io.IOException;

public class PodcastLog {

	public final static int  VERBOSE = 0;
	
	public final static int  DEBUG = 1;
	public final static int  INFO = 2;
	public final static int  WARN = 3;
	public final static int  ERROR = 4;
	
	public final static int  DEFAULT_LEVEL = 3;
	
	private final String clazz;
	
	private int  level;
	private static final String TAG = "PODCAST";

	public static void initFileLog(SoundWaves argSoundWavesApplication) {
		if (!BuildConfig.DEBUG) {
			return;
		}

		if ( isExternalStorageWritable() ) {

			File appDirectory = new File( Environment.getExternalStorageDirectory() + "/MyPersonalAppFolder" );
			File logDirectory = new File( appDirectory + "/log" );
			File logFile = new File( logDirectory, "logcat_wlog.txt" );

			// create app folder
			if ( !appDirectory.exists() ) {
				appDirectory.mkdir();
			}

			// create log folder
			if ( !logDirectory.exists() ) {
				logDirectory.mkdir();
			}

			// clear the previous logcat and then write the new one to the file
			try {
				//Process process = Runtime.getRuntime().exec( "logcat -c");
				//process = Runtime.getRuntime().exec( "logcat -f " + logFile + " *:S MyActivity:D MyActivity2:D");
				Process process = Runtime.getRuntime().exec( "logcat -f " + logFile + " Unsubscribing:E *:S");
			} catch ( IOException e ) {
				e.printStackTrace();
			}

		} else if ( isExternalStorageReadable() ) {
			// only readable
		} else {
			// not accessible
		}
	}

	/* Checks if external storage is available for read and write */
	private static boolean isExternalStorageWritable() {
		String state = Environment.getExternalStorageState();
		if ( Environment.MEDIA_MOUNTED.equals( state ) ) {
			return true;
		}
		return false;
	}

	/* Checks if external storage is available to at least read */
	private static boolean isExternalStorageReadable() {
		String state = Environment.getExternalStorageState();
		if ( Environment.MEDIA_MOUNTED.equals( state ) ||
				Environment.MEDIA_MOUNTED_READ_ONLY.equals( state ) ) {
			return true;
		}
		return false;
	}

	@Deprecated
	public static PodcastLog getDebugLog(Class<?> clazz, int l) {
		PodcastLog log = new PodcastLog(clazz);
		log.level = l;
		return log;
	}

	@Deprecated
	public static PodcastLog getLog(Class<?> clazz) {
		return new PodcastLog(clazz);
	}

	@Deprecated
	public PodcastLog(Class<?> clazz) {
		this.clazz = "[" + clazz.getSimpleName() + "] ";
		level = DEFAULT_LEVEL;
	}

	@Deprecated
	public void debug(String message) {
		debug(message, null);
	}

	@Deprecated
	public void info(String message) {
		info(message, null);
	}

	@Deprecated
	public void error(String message) {
		error(message, null);
	}

	@Deprecated
	public void debug(String message, Throwable t) {
		if(DEBUG<level)
			return;		
		if (message != null)
			android.util.Log.d(TAG, clazz + message);
		if (t != null)
			android.util.Log.d(TAG, clazz + t.toString());		
	}

	@Deprecated
	public void info(String message, Throwable t) {
		if(INFO<level)
			return;			
		if (message != null)
			android.util.Log.i(TAG, clazz + message);
		if (t != null)
			android.util.Log.i(TAG, clazz + t.toString());
	}

	@Deprecated
	public void warn(String message, Throwable t) {
		if(WARN<level)
			return;			
		if (message != null)
			android.util.Log.w(TAG, clazz + message);
		if (t != null)
			android.util.Log.w(TAG, clazz + t.toString());
	}

	@Deprecated
	public void error(String message, Throwable t) {
		if(ERROR<level)
			return;			
		if (message != null)
			android.util.Log.e(TAG, clazz + message);
		if (t != null)
			android.util.Log.e(TAG, clazz + t.toString());
	}
}
