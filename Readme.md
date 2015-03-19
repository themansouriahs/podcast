# SoundWaves

SoundWaves is a free, libre and open source app for fetching and listening to podcasts.

[![Download from Google Play](http://www.android.com/images/brand/android_app_on_play_large.png "Download from Google Play")](https://play.google.com/store/apps/details?id=org.bottiger.podcast)
[![SoundWaves on fdroid.org](https://camo.githubusercontent.com/7df0eafa4433fa4919a56f87c3d99cf81b68d01c/68747470733a2f2f662d64726f69642e6f72672f77696b692f696d616765732f632f63342f462d44726f69642d627574746f6e5f617661696c61626c652d6f6e2e706e67 "Download from fdroid.org")](https://f-droid.org/repository/browse/?fdcategory=Multimedia&fdid=org.bottiger.podcast&fdpage=1)

# Help out

If you feel like helping out we are more than happy to accept any kind of help. There are a lot of programming - and non programming - tasks which needs to be done .  

## Translations
If you want to help translate SoundWaves into your language please head over to:
https://www.transifex.com/projects/p/soundwaves/

Unfortunately a (free) account is required.

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