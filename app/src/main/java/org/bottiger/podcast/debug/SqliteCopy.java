package org.bottiger.podcast.debug;

import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class SqliteCopy {

	/*
	 * Unrooted phones can use this method to copy the SQlite database to the SD card
	 */
	public static void backupDatabase() throws IOException {
		// Open your local db as the input stream
		String inFileName = "/data/data/org.bottiger.podcast/databases/podcast.db";
		File dbFile = new File(inFileName);
		FileInputStream fis = new FileInputStream(dbFile);

		String outFileName = Environment.getExternalStorageDirectory()
				+ "/podcast.db";
		// Open the empty db as the output stream
		OutputStream output = new FileOutputStream(outFileName);
		// transfer bytes from the inputfile to the outputfile
		byte[] buffer = new byte[1024];
		int length;
		while ((length = fis.read(buffer)) > 0) {
			output.write(buffer, 0, length);
		}
		// Close the streams
		output.flush();
		output.close();
		fis.close();
	}

}
