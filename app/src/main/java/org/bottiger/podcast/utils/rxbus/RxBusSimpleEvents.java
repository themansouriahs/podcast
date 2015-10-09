package org.bottiger.podcast.utils.rxbus;

/**
 * Created by aplb on 09-10-2015.
 */
public class RxBusSimpleEvents {

    public static class PlaybackSpeedChanged {

        public float speed = 1.0f;

        public PlaybackSpeedChanged(float argSpeed) {
            speed = argSpeed;
        }
    }

}
