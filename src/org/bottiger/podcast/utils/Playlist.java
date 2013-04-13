package org.bottiger.podcast.utils;

import java.util.HashMap;

import org.bottiger.podcast.R.id;
import org.bottiger.podcast.provider.FeedItem;

public class Playlist {
	
	private static int from;
	private static int to;
	
	public static int getItemAt(int position) {
		
		if (Playlist.to == -1)
			return -1;
		
		if (position == Playlist.to)
			return from;
		
		if (position < Playlist.to)
			return position;
		
		if (position > Playlist.to) {
			Playlist.resert();
			return position+1;
		}
		
		return -1;
	}
	
	private static void resert() {
		Playlist.setFrom(-1);
		Playlist.setTo(-1);
	}
	
	public static void setFrom(int fromId) {
		Playlist.from = fromId;
	}
	
	public static void setTo(int toId) {
		Playlist.to = toId;
	}

}
