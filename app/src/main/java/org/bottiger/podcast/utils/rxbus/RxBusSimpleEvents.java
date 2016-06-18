package org.bottiger.podcast.utils.rxbus;

/**
 * Created by aplb on 09-10-2015.
 */
public class RxBusSimpleEvents {

    public static class PlaybackEngineChanged {

        private boolean mDoRemoveSilence = false;
        public float speed = 1.0f;

        public PlaybackEngineChanged(float argSpeed, boolean doRemoveSilence) {
            speed = argSpeed;
            mDoRemoveSilence = doRemoveSilence;
        }

        public boolean doRemoveSilence() {
            return mDoRemoveSilence;
        }
    }

}
