package org.bottiger.podcast.utils.rxbus;

/**
 * Created by aplb on 09-10-2015.
 */
public class RxBusSimpleEvents {

    public static class PlaybackEngineChanged {

        private boolean mDoRemoveSilence = false;
        private boolean mAutomaticGainControl = false;
        public float speed = 1.0f;

        public PlaybackEngineChanged(float argSpeed, boolean doRemoveSilence, boolean doAutomaticGainControl) {
            speed = argSpeed;
            mDoRemoveSilence = doRemoveSilence;
            mAutomaticGainControl = doAutomaticGainControl;
        }

        public boolean doRemoveSilence() {
            return mDoRemoveSilence;
        }
        public boolean doAutomaticGainControl() {
            return mAutomaticGainControl;
        }
    }

}
