package org.bottiger.podcast.webservices.directories.itunes.types;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by Mariangela Salcedo (msalcedo047@gmail.com) on 08/10/16.
 * Copyright (c) 2016 m-salcedo. All rights reserved.
 */
public class ImReleaseDate {

    @SerializedName("label")
    @Expose
    private String label;
    @SerializedName("attributes")
    @Expose
    private Attributes attributes;

    /**
     * No args constructor for use in serialization
     *
     */
    public ImReleaseDate() {
    }

    /**
     *
     * @param label
     * @param attributes
     */
    public ImReleaseDate(String label, Attributes attributes) {
        this.label = label;
        this.attributes = attributes;
    }

    /**
     *
     * @return
     *     The label
     */
    public String getLabel() {
        return label;
    }

    /**
     *
     * @param label
     *     The label
     */
    public void setLabel(String label) {
        this.label = label;
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
