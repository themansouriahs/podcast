package org.bottiger.podcast.utils;

/**
 * Created by aplb on 15-09-2015.
 */
public class BitMaskUtils {

    /**
     * We require that the mask is positive in order to make sure it has been set
     * @param argIntegerMask
     * @param argBit
     * @return True if the bit has been set
     */
    public static boolean IsBitSet(int argIntegerMask, int argBit) {
        return IsBitmaskInitialized(argIntegerMask) && ((argBit & argIntegerMask) != 0);
    }

    public static boolean IsBitmaskInitialized(int argIntegerMask) {
        return argIntegerMask >= 0;
    }
}
