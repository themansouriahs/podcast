package org.bottiger.podcast.utils;

import java.util.List;

/**
 * Created by aplb on 13-12-2016.
 */

public class IntUtils {

    public static int[] toIntArray(List<Integer> list)  {
        int[] ret = new int[list.size()];
        int i = 0;
        for (Integer e : list)
            ret[i++] = e;
        return ret;
    }

}
