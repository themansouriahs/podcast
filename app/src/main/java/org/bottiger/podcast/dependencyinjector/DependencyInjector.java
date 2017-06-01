package org.bottiger.podcast.dependencyinjector;

import android.content.Context;

/**
 * Created by aplb on 01-06-2017.
 */
public class DependencyInjector {

    private static SoundWavesComponent applicationComponent;

    public static void initialize(Context diApplication) {
        applicationComponent = DaggerSoundWavesComponent.builder()
                .soundWavesModule(new SoundWavesModule(diApplication))
                .build();
    }

    public static SoundWavesComponent applicationComponent() {
        if (applicationComponent == null) {
            throw new IllegalStateException("DependencyInjector not initialized!"); // NoI18N
        }

        return applicationComponent;
    }

    private DependencyInjector(){}
}
