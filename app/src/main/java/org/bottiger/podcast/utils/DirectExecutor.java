package org.bottiger.podcast.utils;

import java.util.concurrent.Executor;

/**
 * Created by apl on 14-04-2015.
 */
public class DirectExecutor implements Executor {
    public void execute(Runnable r) {
        r.run();

    }
}
