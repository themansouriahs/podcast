# SoundWaves

SoundWaves is a free, libre and open source app for fetching and listening to podcasts.

# Privacy

SoundWaves takes privacy very seriously, and I try to do whatever I can do always behave in the users best interest.

If you are getting SoundWaves from source or the F-droid it will never share any data with anyone without your explicit consent.
However, if you are getting SoundWaves from a proprietary app store, like the Play store, it will collect some anonymous information - like crash reports - in order for me to improve the quality.

# Permissions

SoundWaves aims at not requesting any permissions it doesn't need. I am not quite there yet - but almost.

Basic internet access. SoundWaves also need to know the state of the current network connection in order know if 
the device is using a cellular connection.
* INTERNET
* ACCESS_WIFI_STATE
* ACCESS_NETWORK_STATE

A wakelock is required in order to keep the device alive while downloading new episodes in the background.
* WAKE_LOCK

Write access to the external storage is required in order to store episodes.
* WRITE_EXTERNAL_STORAGE

Knowlegde of the phone state is required in order to pause an episode if there is an incomming phone call.
* READ_PHONE_STATE

In order to start fetching new episodes in the background without starting the app first we need to get notified when the device has booted up.
* RECEIVE_BOOT_COMPLETED

In order to synchronize the database to the cloud we use Android build in sync framework.
* READ_SYNC_STATS
* READ_SYNC_SETTINGS
* WRITE_SYNC_SETTINGS

[Play store only] On the play store we can request access to Google Drive in order to provide free cross device synchronization.
* GET_ACCOUNTS
* USE_CREDENTIALS

Required in order to control the media player.
* MEDIA_CONTENT_CONTROL

Legacy permission which will be removed in the near future.
* VIBRATE
* DOWNLOAD_WITHOUT_NOTIFICATION