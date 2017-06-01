package org.bottiger.podcast.dependencyinjector;

import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.dependencyinjector.SoundWavesModule;
import org.bottiger.podcast.service.PlayerService;

import javax.inject.Singleton;

import dagger.Component;

/**
 * Created by aplb on 01-06-2017.
 */

@Singleton
@Component(modules = {SoundWavesModule.class})
public interface SoundWavesComponent {
    void inject(SoundWaves argApp);
    void inject(PlayerService argPlayerservice);
}
