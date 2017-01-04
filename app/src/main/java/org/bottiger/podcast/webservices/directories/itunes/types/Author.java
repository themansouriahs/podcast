package org.bottiger.podcast.webservices.directories.itunes.types;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by Mariangela Salcedo (msalcedo047@gmail.com) on 08/10/16.
 * Copyright (c) 2016 m-salcedo. All rights reserved.
 */
public class Author {

    @SerializedName("name")
    @Expose
    private Name name;
    @SerializedName("uri")
    @Expose
    private Uri uri;

    /**
     * No args constructor for use in serialization
     *
     */
    public Author() {
    }

    /**
     *
     * @param name
     * @param uri
     */
    public Author(Name name, Uri uri) {
        this.name = name;
        this.uri = uri;
    }

    /**
     *
     * @return
     *     The name
     */
    public Name getName() {
        return name;
    }

    /**
     *
     * @param name
     *     The name
     */
    public void setName(Name name) {
        this.name = name;
    }

    /**
     *
     * @return
     *     The uri
     */
    public Uri getUri() {
        return uri;
    }

    /**
     *
     * @param uri
     *     The uri
     */
    public void setUri(Uri uri) {
        this.uri = uri;
    }

}