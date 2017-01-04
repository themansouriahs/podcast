package org.bottiger.podcast.webservices.directories.itunes.types;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by Mariangela Salcedo (msalcedo047@gmail.com) on 08/10/16.
 * Copyright (c) 2016 m-salcedo. All rights reserved.
 */
public class ImContentType {

    @SerializedName("attributes")
    @Expose
    private Attributes attributes;

    /**
     * No args constructor for use in serialization
     *
     */
    public ImContentType() {
    }

    /**
     *
     * @param attributes
     */
    public ImContentType(Attributes attributes) {
        this.attributes = attributes;
    }

    /**
     *
     * @return
     *     The attributes
     */
    public Attributes getAttributes() {
        return attributes;
    }

    /**
     *
     * @param attributes
     *     The attributes
     */
    public void setAttributes(Attributes attributes) {
        this.attributes = attributes;
    }

}