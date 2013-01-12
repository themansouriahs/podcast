package info.bottiger.podcast.provider;

import info.bottiger.podcast.PlayerActivity;
import info.bottiger.podcast.service.PodcastDownloadManager;
import info.bottiger.podcast.service.PodcastService;
import info.bottiger.podcast.utils.FileUtils;
import info.bottiger.podcast.utils.Log;
import info.bottiger.podcast.utils.SDCardManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Handler;
import android.support.v4.util.LruCache;
import android.widget.Toast;

public class FeedItem implements Comparable<FeedItem> {
	
	public static final int MAX_DOWNLOAD_FAIL = 5;
	
	private final Log log = Log.getLog(getClass());
	private static ItemLruCache cache = null;

	public String url;
	public String title;
	public String author;
	public String date;
	public String content;
	public String resource;
	public String duration;
	public String image;

	public long id;

	public long sub_id;
	public long filesize;
	public long chunkFilesize;

	private String pathname;
	public int offset;
	public int status;
	public long failcount;
            //failcount is currently used for two purposes:
            //1. counts the number of times we fail to download, and
            //   when we exceed a predefined max, we pause the download.
            //2. when an item is in the player, failcount is used as
            //   the order of the item in the list.
	public int keep;	//1 if we should not expire this item
	
	public long length;

	public long update;

	public String uri;

	public String sub_title;
	public long created;

	public String type;
	
	private long m_date;

	static String[] DATE_FORMATS = { 
		"EEE, dd MMM yyyy HH:mm:ss Z",
		"EEE, d MMM yy HH:mm z", 
		"EEE, d MMM yyyy HH:mm:ss z",
		"EEE, d MMM yyyy HH:mm z", 
		"d MMM yy HH:mm z",
		"d MMM yy HH:mm:ss z", 
		"d MMM yyyy HH:mm z",
		"d MMM yyyy HH:mm:ss z", 
		"yyyy-MM-dd HH:mm", 
		"yyyy-MM-dd HH:mm:ss", 
		"EEE,dd MMM yyyy HH:mm:ss Z",};
	
	static String default_format = "EEE, dd MMM yyyy HH:mm:ss Z";

	public static void view(Activity act, long item_id) {
		Uri uri = ContentUris.withAppendedId(ItemColumns.URI, item_id);
		FeedItem item = FeedItem.getById(act.getContentResolver(), item_id);
		if ((item != null)
				&& (item.status == ItemColumns.ITEM_STATUS_UNREAD)) {
			item.status = ItemColumns.ITEM_STATUS_READ;
			item.update(act.getContentResolver());
		}    			
		act.startActivity(new Intent(Intent.ACTION_EDIT, uri));   
	}
	
	/*
	public static void viewChannel(Activity act, long item_id) {
		FeedItem item = FeedItem.getById(act.getContentResolver(), item_id);
		item.viewChannel(act);
	}
	*/
	public static void play(Activity act, long item_id) {
		FeedItem feeditem = FeedItem.getById(act.getContentResolver(), item_id);
		if (feeditem == null)
			return;
		feeditem.play(act);
	}
	
	//True if we found the item and added it to the playlist
	public static boolean addToPlaylist(Activity act, long item_id) {
		FeedItem feeditem = FeedItem.getById(act.getContentResolver(), item_id);
		if (feeditem != null) {
			feeditem.addtoPlaylist(act.getContentResolver());
			return true;
		} else {
			return false;
		}
	}

	public static FeedItem getBySQL(ContentResolver context,String where,String order) 
	{
		FeedItem item = null;
		Cursor cursor = null;
	
		try {
			cursor = context.query(
					ItemColumns.URI,
					ItemColumns.ALL_COLUMNS,
					where,
					null,
					order);
			if (cursor.moveToFirst()) {
				item = FeedItem.getByCursor(cursor);
			}
		}finally {
			if (cursor != null)
				cursor.close();
		}		
		return item;
						
	}
	
	public static FeedItem getById(ContentResolver contentResolver, long id) {
		Cursor cursor = null;
		FeedItem item = null;
		
		initCache();
		
		// Return item directly if cached
		synchronized (cache) {
			item = cache.get(id);
			if (item != null) {
				return item;
			}
		}
		
		try {
			String where = ItemColumns._ID + " = " + id;

			cursor = contentResolver.query(ItemColumns.URI, ItemColumns.ALL_COLUMNS,
					where, null, null);
			if (cursor.moveToFirst()) {
				item = fetchFromCursor(cursor);

				cursor.close();
			}
		} catch (Exception e) {
			e.printStackTrace();

		} finally {

			if (cursor != null)
				cursor.close();

		}

		return item;
	}

	public static FeedItem getByCursor(Cursor cursor) {
		//if (cursor.moveToFirst() == false)
		//	return null;
		FeedItem item = fetchFromCursor(cursor);
		return item;
	}

	public FeedItem() {
		url = null;
		title = null;
		author = null;
		date = null;
		content = null;
		resource = null;
		duration = null;
		pathname = null;
		uri = null;
		type = null;
		image = null;

		id = -1;
		offset = -1;
		status = -1;
		failcount = -1;
		length = -1;
		update = -1;
		keep = -1;
		filesize = -1;
		chunkFilesize = -1;

		created = -1;
		sub_title = null;
		sub_id = -1;
		
		m_date = -1;

	}
	
	public void updateOffset(ContentResolver context, long i)
	{
		offset = (int)i;
		update = -1;
		update(context);
		
	}
	
	public void playingOrPaused(boolean isPlaying, ContentResolver context)
	{
		if (isPlaying)
			playing(context);
		else
			paused(context);				
	}
	
	public void playing(ContentResolver context)
	{
		if(status != ItemColumns.ITEM_STATUS_PLAYING_NOW) {
			status = ItemColumns.ITEM_STATUS_PLAYING_NOW;
			update = Long.valueOf(System.currentTimeMillis());
		}else{
			update = -1;
		}
		update(context);		
	}
	
	public void paused(ContentResolver context)
	{
		if(status == ItemColumns.ITEM_STATUS_PLAYING_NOW) {
			status = ItemColumns.ITEM_STATUS_PLAY_PAUSE;
			update = Long.valueOf(System.currentTimeMillis());
		}else{
			update = -1;
		}
		update(context);		
	}
	
	public void played(ContentResolver context)
	{
		offset = 0;
		if(status == ItemColumns.ITEM_STATUS_NO_PLAY ||
           status == ItemColumns.ITEM_STATUS_PLAYING_NOW) {
			status = ItemColumns.ITEM_STATUS_PLAYED;
			update = Long.valueOf(System.currentTimeMillis());
		}else{
			update = -1;
		}
		update(context);		
	}
	
	public void addtoPlaylistByOrder(ContentResolver context, long order)
	{
		failcount = order;
		if(status == ItemColumns.ITEM_STATUS_NO_PLAY) {
			status = ItemColumns.ITEM_STATUS_PLAY_READY;
		}
		update = -1;
		update(context);
		
	}
	
	public void addtoPlaylist(ContentResolver context)
	{
		addtoPlaylistByOrder(context,Long.valueOf(System.currentTimeMillis()));		
	}		

	public void removeFromPlaylist(ContentResolver context)
	{
		failcount = 0;
		if(status == ItemColumns.ITEM_STATUS_PLAY_READY) {
			status = ItemColumns.ITEM_STATUS_NO_PLAY;
		}
		update = -1;
		update(context);		
	}

	public void markKeep(ContentResolver context) {
		if (this.keep <= 1) {
			this.keep = 1;
			this.update(context);
		}
	}
	public void markUnkeep(ContentResolver context) {
		if (this.keep > 0) {
			this.keep = 0;
			this.update(context);
		}
	}
	public void markNew(ContentResolver context) {
		if (this.status > ItemColumns.ITEM_STATUS_NO_PLAY &&
				this.status!=ItemColumns.ITEM_STATUS_PLAYING_NOW) {
			this.status = ItemColumns.ITEM_STATUS_NO_PLAY;
			this.failcount = 0;
			this.updateOffset(context,0);
		}
	}
	
	public void update(ContentResolver context) {
		log.debug("item update start");
		
		initCache();
		if (this.id > 0)
			cache.remove(this.id);
		
		try {

			ContentValues cv = new ContentValues();
			if (pathname != null)
				cv.put(ItemColumns.PATHNAME, pathname);
			if (filesize >= 0)
				cv.put(ItemColumns.FILESIZE, filesize);
			if (chunkFilesize >= 0)
				cv.put(ItemColumns.CHUNK_FILESIZE, chunkFilesize);
			if (offset >= 0)
				cv.put(ItemColumns.OFFSET, offset);
			if (status >= 0)
				cv.put(ItemColumns.STATUS, status);
			if (failcount >= 0)
				cv.put(ItemColumns.FAIL_COUNT, failcount);
			
			if(update >= 0){
				update = Long.valueOf(System.currentTimeMillis());
				cv.put(ItemColumns.LAST_UPDATE, update);
			}
			if (created >= 0)
				cv.put(ItemColumns.CREATED, created);
			if (length >= 0)
				cv.put(ItemColumns.LENGTH, length);
			if (uri != null)
				cv.put(ItemColumns.MEDIA_URI, uri);
			if (type != null)
				cv.put(ItemColumns.TYPE, type);
			if (keep>=0)
				cv.put(ItemColumns.KEEP, keep);

			context.update(ItemColumns.URI, cv, ItemColumns._ID + "=" + id,
					null);

			log.debug("update OK");
		} finally {
		}
	}

	public Uri insert(ContentResolver context) {
		log.debug("item insert start");
		try {

			ContentValues cv = new ContentValues();
			if (pathname != null)
				cv.put(ItemColumns.PATHNAME, pathname);
			if (offset >= 0)
				cv.put(ItemColumns.OFFSET, offset);
			if (status >= 0)
				cv.put(ItemColumns.STATUS, status);
			if (failcount >= 0)
				cv.put(ItemColumns.FAIL_COUNT, failcount);
			if (update >= 0)
				cv.put(ItemColumns.LAST_UPDATE, update);
			if (length >= 0)
				cv.put(ItemColumns.LENGTH, length);

			if (sub_id >= 0)
				cv.put(ItemColumns.SUBS_ID, sub_id);
			if (url != null)
				cv.put(ItemColumns.URL, url);
			if (image != null)
				cv.put(ItemColumns.IMAGE_URL, image);
			if (title != null)
				cv.put(ItemColumns.TITLE, title);

			if (author != null)
				cv.put(ItemColumns.AUTHOR, author);
			if (date != null)
				cv.put(ItemColumns.DATE, date);
			if (content != null)
				cv.put(ItemColumns.CONTENT, content);
			if (resource != null)
				cv.put(ItemColumns.RESOURCE, resource);
			if (filesize >= 0)
				cv.put(ItemColumns.FILESIZE, filesize);
			if (duration != null) {
				// Log.w("ITEM","  duration: " + duration);
				cv.put(ItemColumns.DURATION, duration);
			}
			if (sub_title != null) {
				cv.put(ItemColumns.SUB_TITLE, sub_title);
			}
			if (uri != null)
				cv.put(ItemColumns.MEDIA_URI, uri);
			if (keep >= 0)
				cv.put(ItemColumns.KEEP, keep);

			return context.insert(ItemColumns.URI, cv);

		} finally {
		}
	}

	/*
	public void viewChannel(Activity act) {
		//Subscription sub = Subscription.getSubbyId(getContentResolver(), item.sub_id);
		Uri chUri = ContentUris.withAppendedId(SubscriptionColumns.URI, this.sub_id);
		if (ChannelActivity.channelExists(act,chUri))
			act.startActivity(new Intent(Intent.ACTION_EDIT, chUri));
		else {
			String subTitle = this.sub_title;
			if (subTitle==null || subTitle.equals(""))
				subTitle = "(no channel title)";
			String tstr = String.format("Channel not found: '%s'", subTitle);
			Toast.makeText(act, tstr, Toast.LENGTH_SHORT).show();
		}
	}
	*/
	public void playedBy(Activity act) {
		Intent intent = new Intent(android.content.Intent.ACTION_VIEW); 
	    Uri data = Uri.parse("file://"+this.pathname); 
		log.error(this.pathname);
	 
	    intent.setDataAndType(data,"audio/mp3"); 
	    try { 
	         act.startActivity(intent); 
	    } catch (Exception e) { 
	         e.printStackTrace();
	    }
	}

	public void export(Activity act) {
		String filename = FileUtils.get_export_file_name(this.title, this.id);
		filename = SDCardManager.getExportDir()+"/"+filename;
		log.error(filename);   			
			 Toast.makeText(act, "Please wait... ", 
				 Toast.LENGTH_LONG).show();  
			 
		boolean b  = FileUtils.copy_file(getPathname(),filename);
		if(b)
		 Toast.makeText(act, "Exported audio file to : "+ filename, 
				 Toast.LENGTH_LONG).show();
		else
			 Toast.makeText(act, "Export failed ", 
				 Toast.LENGTH_LONG).show();    				
	}
	
	public long getDate() {
		//log.debug(" getDate() start");
		
		if(m_date<0){
			m_date  = parse();
			//log.debug(" getDate() end " + default_format);
			
			
		}
			
		return m_date;

	}

	private long parse() {
		long l = 0;
		try{
			return  new SimpleDateFormat(default_format, Locale.US).parse(date)
			.getTime();
		} catch (ParseException e) {
			log.debug(" first fail");
		}


		

		for (String format : DATE_FORMATS) {
			try {
				l = new SimpleDateFormat(format, Locale.US).parse(date)
						.getTime();
				default_format = format;
				return l;
			} catch (ParseException e) {
			}
		}
		log.warn("cannot parser date: " + date);
		return 0L;
	}

	private static FeedItem fetchFromCursor(Cursor cursor) {
		//assert cursor.moveToFirst();
		//cursor.moveToFirst();
		FeedItem item = new FeedItem();
		
		item.id = cursor.getLong(cursor.getColumnIndex(ItemColumns._ID));
		
		// Return item directly if cached
		initCache();
		synchronized (cache) {
			FeedItem cacheItem = cache.get(item.id);
			if (cacheItem != null && cacheItem.title != "") { // FIXME cacheItem.title != ""
				item = cacheItem;
				return item;
			}
		}
		
		int idx = cursor
				.getColumnIndex(ItemColumns.RESOURCE);
		item.resource = cursor.getString(idx);
		item.pathname = cursor.getString(cursor
				.getColumnIndex(ItemColumns.PATHNAME));
		item.offset = cursor.getInt(cursor.getColumnIndex(ItemColumns.OFFSET));
		item.status = cursor.getInt(cursor.getColumnIndex(ItemColumns.STATUS));
		item.failcount = cursor.getLong(cursor
				.getColumnIndex(ItemColumns.FAIL_COUNT));

		item.length = cursor.getLong(cursor.getColumnIndex(ItemColumns.LENGTH));

		item.url = cursor.getString(cursor.getColumnIndex(ItemColumns.URL));
		item.image = cursor.getString(cursor.getColumnIndex(ItemColumns.IMAGE_URL));
		item.title = cursor.getString(cursor.getColumnIndex(ItemColumns.TITLE));
		item.author = cursor.getString(cursor
				.getColumnIndex(ItemColumns.AUTHOR));
		item.date = cursor.getString(cursor.getColumnIndex(ItemColumns.DATE));
		item.content = cursor.getString(cursor
				.getColumnIndex(ItemColumns.CONTENT));
		item.filesize = cursor.getLong(cursor
				.getColumnIndex(ItemColumns.FILESIZE));
		item.chunkFilesize = cursor.getLong(cursor
				.getColumnIndex(ItemColumns.CHUNK_FILESIZE));
		item.duration = cursor.getString(cursor
				.getColumnIndex(ItemColumns.DURATION));
		item.uri = cursor.getString(cursor
				.getColumnIndex(ItemColumns.MEDIA_URI));

		item.created = cursor.getLong(cursor
				.getColumnIndex(ItemColumns.CREATED));
		item.update = cursor.getLong(cursor
				.getColumnIndex(ItemColumns.LAST_UPDATE));		
		item.sub_title = cursor.getString(cursor
				.getColumnIndex(ItemColumns.SUB_TITLE));
		item.sub_id = cursor.getLong(cursor.getColumnIndex(ItemColumns.SUBS_ID));
		item.type = cursor.getString(cursor.getColumnIndex(ItemColumns.TYPE));
		item.keep = cursor.getInt(cursor.getColumnIndex(ItemColumns.KEEP));
		
		// if item was not cached we put it in the cache
		synchronized (cache) {
			cache.put(item.id, item);
		}
		
		return item;
	}

	@Override
	public String toString() {
		return "Feed: " + title;
	}

	public String getType() {
		if (type == null) {
			return "audio/mpeg";
		}

		if (!type.equalsIgnoreCase("")) {
			return type;
		}

		return "audio/mpeg";
	}
	
	public void play(Activity act){
		
		//item.play(ReadActivity.this);
		Intent intent = new Intent(act, PlayerActivity.class);
		intent.putExtra("item_id", id);
		act.startActivity(intent);
		
		return;

	}
	
	public void delFile(ContentResolver contentResolver){
		if(status<ItemColumns.ITEM_STATUS_DELETE){
			status = ItemColumns.ITEM_STATUS_DELETE;
			update(contentResolver);	
		}

		if (SDCardManager.getSDCardStatus()) {
			try {
				File file = new File(getPathname());
				
				boolean deleted = true;
				if(file.exists())
				{
					deleted = file.delete();					
				}
				if(deleted){
					if(status<ItemColumns.ITEM_STATUS_DELETED){
						status = ItemColumns.ITEM_STATUS_DELETED;
						update(contentResolver);	
					}						
				}
			} catch (Exception e) {
				log.warn("del file failed : " + getPathname() + "  " + e);

			}
		}		

	}
	
	private String getMailBody(){
		
		String text;
		text = "audio title: "+title+" \n";
		text +="download address: "+resource;
		
		text +="\n-------------------------------------------------------------\n";
		text +="from Hapi Podcast http://market.android.com/search?q=pname:info.xuluan.podcast";
		
		return text;
		
	}
	
	public void prepareDownload(ContentResolver context)
	{
		if (getPathname().equals("") || getPathname().equals("0")) {
			//String fileName = new File(pathname)FeedItem getName();
			String filename = resource.substring(resource.lastIndexOf("/")+1);  
			
			//pathname = SDCardMgr.getDownloadDir() + "/podcast_" + id + ".mp3";
			pathname = this.sub_id + "_" + filename + ".mp3";
		}
		status = ItemColumns.ITEM_STATUS_DOWNLOADING_NOW;
		update(context);
	}

	public void downloadSuccess(ContentResolver contentResolver)
	{
		status = ItemColumns.ITEM_STATUS_NO_PLAY;
		filesize = getCurrentFileSize();
		update(contentResolver);
	}	
	
	public void endDownload(ContentResolver context)
	{
		if (status == ItemColumns.ITEM_STATUS_DOWNLOAD_PENDING)
			status = ItemColumns.ITEM_STATUS_NO_PLAY;
		
		if (status == ItemColumns.ITEM_STATUS_NO_PLAY) {
			update = Long.valueOf(System.currentTimeMillis());
			failcount = 0;
			offset = 0;

		} else {
			status = ItemColumns.ITEM_STATUS_DOWNLOAD_QUEUE;
			failcount++;
			if (failcount > MAX_DOWNLOAD_FAIL) {
				status = ItemColumns.ITEM_STATUS_DOWNLOAD_PAUSE;
				failcount = 0;
			}
		}

		update(context);		
	}	
	
	public long getCurrentFileSize() {
		String filename = getPathname();
		if (filename != null)
			return new File(filename).length();
		return 0;
	}
	
	public String getPathname() {
		return SDCardManager.pathFromFilename(this.pathname);
	}

	public void sendMail(Activity act){
	
		final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND); 
		emailIntent .setType("plain/text"); 
		//emailIntent .putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{"xuluan.android@gmail.com"}); 
		emailIntent .putExtra(android.content.Intent.EXTRA_SUBJECT, "please listen..."); 
		emailIntent .putExtra(android.content.Intent.EXTRA_TEXT, getMailBody()); 
		act.startActivity(Intent.createChooser(emailIntent, "Send mail..."));
	}
	
	public void setPosition(ContentResolver contentResolver, long pos) {
		this.offset = (int) pos;
		update(contentResolver);
	}

	@Override
	public int compareTo(FeedItem another) {
		if (this.update > another.update)
			return 1;
		else if (this.update < another.update)
			return -1;
		
		return 0;
	}
	
	public String getURL() {
		String itemURL = "";
		if (resource.length() > 1)
			itemURL = resource;
		else if (url.length() > 1)
			itemURL = url;
		return itemURL;
	}
	
	public boolean isDownloaded() {
		/* refactor when it works */
		String path = getPathname();
		if (path == null || path == "") return false;
		
		File localFile = new File(path);
		long localFilesize = localFile.length();
		
		if (filesize == 0)
			return localFilesize > 0;
		else
			return localFilesize >= filesize && localFilesize > 0;
	}
	
	/*
	 * @return the duration of the mp3 (or whatever) in milliseconds.
	 */
	public long getDuration() {
		if (isDownloaded()) {
			MediaMetadataRetriever retriever = new MediaMetadataRetriever();
			try {
				retriever.setDataSource(getPathname());
			} catch(RuntimeException e) {
				e.printStackTrace();
				return this.length;
			}
			return Long.parseLong(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
		} else {
			return this.length;
		}
	}
	
	/*
	 * @return The path to the Items icons
	 */
	public String getThumbnail() {

		/* Calculate the imageOath */
		String imageURL;
		String fullPath = SDCardManager.pathFromFilename(pathname);
		PodcastDownloadManager.DownloadStatus ds = PodcastDownloadManager.getStatus(this);
		
		if (pathname != null && pathname.length() > 0 && new File(fullPath).exists() && ds == PodcastDownloadManager.DownloadStatus.DONE) {
			//holder.imageView.setImageBitmap(cover);
			FileOutputStream out;
			String thumbnailPath = thumbnailCacheURL(id);
			File thumbnail = new File(thumbnailPath);
			String urlPrefix = "file://";
			if (!thumbnail.exists()) {
			try {
				MediaMetadataRetriever mmr = new MediaMetadataRetriever();
				mmr.setDataSource(fullPath);
				byte[] ba = mmr.getEmbeddedPicture();
				Bitmap cover = BitmapFactory.decodeByteArray(ba, 0, ba.length);
				out = new FileOutputStream(thumbnailPath);
				cover.compress(Bitmap.CompressFormat.PNG, 90, out);
				imageURL = urlPrefix + thumbnailPath;
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				imageURL = urlPrefix + image;
			} catch (RuntimeException e) {
				e.printStackTrace();
				imageURL = image;
			}
			} else {
				imageURL = urlPrefix + thumbnailPath;
			}
		} else {
			//this.setViewImage3(holder.imageView, cursor.getString(imageIndex));
			imageURL = image;
		}
		
		return imageURL;
	}
	
	@SuppressLint("UseValueOf")
	private String thumbnailCacheURL(long id) {
		String StringID = new Long(id).toString();
		String thumbURL = SDCardManager.getThumbnailCacheDir() + "/" + StringID + ".png";
		return thumbURL;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (id ^ (id >>> 32));
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof FeedItem))
			return false;
		FeedItem other = (FeedItem) obj;
		if (id != other.id)
			return false;
		return true;
	}
	
	private static void initCache() {
		if (cache == null) {
			int memoryClass = 16 * 1024 * 1024; //FIXME use getMemoryClass()
	    	cache = new ItemLruCache(memoryClass);		
		}
	}
	
	private static class ItemLruCache extends LruCache<Long, FeedItem> {

	    public ItemLruCache(int maxSize) {
	        super(maxSize);
	    }

	}
}
