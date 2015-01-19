package org.bottiger.podcast.cloud.drive;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
* Service for the DriveSyncAdapter.
*/
public class DriveSyncService extends Service {

  private static final Object sSyncAdapterLock = new Object();

  private static DriveSyncAdapter sSyncAdapter = null;

  @Override
  public void onCreate() {
    synchronized (sSyncAdapterLock) {
      if (sSyncAdapter == null) {
        sSyncAdapter = new DriveSyncAdapter(getApplicationContext(), true);
      }
    }
  }

  @Override
  public IBinder onBind(Intent intent) {
    return sSyncAdapter.getSyncAdapterBinder();
  }
}
