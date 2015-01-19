package org.bottiger.podcast.service;

import android.os.SystemClock;

public class CloudSyncService extends AbstractAlarmService {

	private long lastUpdatedAt;
	
	public CloudSyncService() {
		super("CloudSync Service");
	}

	@Override
	protected void fireAlarm() {
		lastUpdatedAt = SystemClock.elapsedRealtime();
		// TODO Auto-generated method stub
	}

	@Override
	protected long nextUpdateMs() {
		// 10 minutes
		return 10*60*1000;
	}
	
}

