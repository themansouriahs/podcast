package info.bottiger.podcast.receiver;

import info.bottiger.podcast.PodcastBaseFragment;
import info.bottiger.podcast.service.PlayerService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.view.KeyEvent;

public class RemoteControlReceiver extends BroadcastReceiver {

	private PlayerService mPlayerServiceBinder = PodcastBaseFragment.mPlayerServiceBinder;

	@Override
	public void onReceive(Context context, Intent intent) {
		if (Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {
			KeyEvent event = (KeyEvent) intent
					.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
			if (KeyEvent.KEYCODE_HEADSETHOOK == event.getKeyCode()) {
				if (KeyEvent.ACTION_DOWN == event.getAction()) {
					if (mPlayerServiceBinder.isPlaying())
						mPlayerServiceBinder.pause();
					else
						mPlayerServiceBinder.start();
				}
			}
		}
	}
}
