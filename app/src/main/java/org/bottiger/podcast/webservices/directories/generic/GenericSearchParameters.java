package org.bottiger.podcast.webservices.directories.generic;

import android.text.TextUtils;

import org.bottiger.podcast.webservices.directories.ISearchParameters;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by apl on 15-04-2015.
 */
public class GenericSearchParameters implements ISearchParameters {

    private final List<String> mKeywords = new LinkedList<>();

    @Override
    public void addSearchTerm(String argKeyword) {
        String[] keywords = TextUtils.split(argKeyword, " ");

        for (String keyword : keywords) {
            if (!TextUtils.isEmpty(keyword)) {
                mKeywords.add(keyword);
            }
        }
    }

    @Override
    public List<String> getKeywords() {
        return mKeywords;
    }
}
