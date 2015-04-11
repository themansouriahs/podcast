package org.bottiger.podcast.filesystem;

import android.content.Context;
import android.support.annotation.NonNull;

/**
 * Created by apl on 11-04-2015.
 */
public class FileWriter {

    private static final String TAG = "FileWriter";

    private Context mContext;

    public FileWriter(@NonNull Context argContext) {
        mContext = argContext;
    }
}
