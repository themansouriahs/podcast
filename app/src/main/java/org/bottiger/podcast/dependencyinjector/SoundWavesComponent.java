package org.bottiger.podcast.dependencyinjector;

import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;

import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.SubscriptionsFragment;
import org.bottiger.podcast.activities.feedview.FeedActivity;
import org.bottiger.podcast.dependencyinjector.SoundWavesModule;
import org.bottiger.podcast.service.PlayerService;
import org.bottiger.podcast.views.dialogs.DialogPlaylistFilters;

import javax.inject.Singleton;

import dagger.Component;

/**
 * Created by aplb on 01-06-2017.
 */

@Singleton
@Component(modules = {SoundWavesModule.class})
public interface SoundWavesComponent {
    void inject(@NonNull SoundWaves argApp);
    void inject(@NonNull PlayerService argPlayerservice);
    void inject(@NonNull DialogPlaylistFilters argPlaylistFilters);
    void inject(@NonNull FeedActivity argFeedActivity);
    void inject(@NonNull SubscriptionsFragment argSubscriptionFragment);
}
