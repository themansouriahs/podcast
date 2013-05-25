package org.bottiger.podcast.provider;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;

public interface WithIcon {
	
	public long getId();
	
	public String getImageURL(Context context);
	
	public abstract String getDriveId();
	
	public abstract void setDriveId(String id);
	
	public abstract long lastModificationDate();
	
	public abstract String getTitle();
	
	public abstract void setTitle(String title);
	
	public abstract String toJSON();
	
	public abstract WithIcon createFromJSON(ContentResolver contentResolver, String json);
	
	public abstract void update(ContentResolver contentResolver);
	
	public abstract ContentProviderOperation update(ContentResolver contentResolver, boolean batchUpdate);

}
