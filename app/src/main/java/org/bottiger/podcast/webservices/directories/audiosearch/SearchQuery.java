package org.bottiger.podcast.webservices.directories.audiosearch;

import com.google.gson.annotations.SerializedName;

/**
 * Created by aplb on 12-12-2016.
 */

class SearchQuery {

    @SerializedName("query")
    String mQuery;

    @SerializedName("sort_by")
    String mSortBy = "_score";

    @SerializedName("sort_order")
    String mSortOrder = "desc";

    @SerializedName("size")
    String mSize = Integer.toString(30);

    @SerializedName("from")
    String mFrom = Integer.toString(0);

    @SerializedName("page")
    String mPage = Integer.toString(1);

    SearchQuery(String argQuery) {
        mQuery = argQuery;
    }

    String getQuery() {
        return mQuery;
    }

    String getSortBy() {
        return mSortBy;
    }

    String getSortOrder() {
        return mSortOrder;
    }

    String getSize() {
        return mSize;
    }

    String getFrom() {
        return mFrom;
    }

    String getPage() {
        return mPage;
    }

}
