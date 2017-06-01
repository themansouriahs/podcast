package org.bottiger.podcast.dependencyinjector;

import android.content.Context;
import android.support.annotation.NonNull;

import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.model.Library;
import org.bottiger.podcast.player.GenericMediaPlayerInterface;
import org.bottiger.podcast.playlist.Playlist;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * Created by aplb on 01-06-2017.
 */
@Module
public class SoundWavesModule {
    private final SoundWaves mSoundWaves;

    public SoundWavesModule(@NonNull Context argContext) {
        mSoundWaves = SoundWaves.getAppContext(argContext);
    }

    @Provides
    @Singleton
    public GenericMediaPlayerInterface providePlayer() {
        return mSoundWaves.getPlayer();
    }

    @Provides
    @Singleton
    public Library provideLibrary() {
        return mSoundWaves.getLibraryInstance();
    }

    @Provides
    @Singleton
    public Playlist providePlaylist() {
        return mSoundWaves.getPlaylist();
    }

}
