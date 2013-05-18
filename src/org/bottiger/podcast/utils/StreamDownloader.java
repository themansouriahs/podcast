package org.bottiger.podcast.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;

import org.bottiger.podcast.provider.FeedItem;

public class StreamDownloader {
	
	private FeedItem item;
	private OutputStream os;
	
	private File file = new File(SDCardManager.getTmpDir() + "/testfile");
	
	public StreamDownloader(FeedItem item) {
		this.item = item;
	}
	
	public FileOutputStream getFileOutputStream() throws FileNotFoundException {
		return new FileOutputStream(getFile());
	}
	
	public File getFile() {
		return file;
	}

}
