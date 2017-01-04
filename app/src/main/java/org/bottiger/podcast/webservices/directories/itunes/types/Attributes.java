package org.bottiger.podcast.webservices.directories.itunes.types;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by Mariangela Salcedo (msalcedo047@gmail.com) on 08/10/16.
 * Copyright (c) 2016 m-salcedo. All rights reserved.
 */
public class Attributes {

    @SerializedName("height")
    private String height;
    @SerializedName("amount")
    private String amount;
    @SerializedName("currency")
    private String currency;
    @SerializedName("term")
    private String term;
    @SerializedName("label")
    private String label;
    @SerializedName("rel")
    private String rel;
    @SerializedName("type")
    private String type;
    @SerializedName("href")
    private String href;
    @SerializedName("im:id")
    private String imId;
    @SerializedName("im:bundleId")
    @Expose
    private String imBundleId;
    @SerializedName("scheme")
    @Expose
    private String scheme;

    /**
     * No args constructor for use in serialization
     *
     */
    public Attributes() {
    }

    /**
     *
     * @return
     *     The height
     */
    public String getHeight() {
        return height;
    }

    /**
     *
     * @param height
     *     The height
     */
    public void setHeight(String height) {
        this.height = height;
    }

    /**
     *
     * @return
     *     The amount
     */
    public String getAmount() {
        return amount;
    }

    /**
     *
     * @param amount
     *     The amount
     */
    public void setAmount(String amount) {
        this.amount = amount;
    }

    /**
     *
     * @return
     *     The currency
     */
    public String getCurrency() {
        return currency;
    }

    /**
     *
     * @param currency
     *     The currency
     */
    public void setCurrency(String currency) {
        this.currency = currency;
    }


    /**
     *
     * @return
     *     The term
     */
    public String getTerm() {
        return term;
    }

    /**
     *
     * @param term
     *     The term
     */
    public void setTerm(String term) {
        this.term = term;
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
     *     The rel
     */
    public String getRel() {
        return rel;
    }

    /**
     *
     * @param rel
     *     The rel
     */
    public void setRel(String rel) {
        this.rel = rel;
    }

    /**
     *
     * @return
     *     The type
     */
    public String getType() {
        return type;
    }

    /**
     *
     * @param type
     *     The type
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     *
     * @return
     *     The href
     */
    public String getHref() {
        return href;
    }

    /**
     *
     * @param href
     *     The href
     */
    public void setHref(String href) {
        this.href = href;
    }

    /**
     *
     * @return
     *     The imId
     */
    public String getImId() {
        return imId;
    }

    /**
     *
     * @param imId
     *     The im:id
     */
    public void setImId(String imId) {
        this.imId = imId;
    }

    /**
     *
     * @return
     *     The imBundleId
     */
    public String getImBundleId() {
        return imBundleId;
    }

    /**
     *
     * @param imBundleId
     *     The im:bundleId
     */
    public void setImBundleId(String imBundleId) {
        this.imBundleId = imBundleId;
    }

    /**
     *
     * @return
     *     The scheme
     */
    public String getScheme() {
        return scheme;
    }

    /**
     *
     * @param scheme
     *     The scheme
     */
    public void setScheme(String scheme) {
        this.scheme = scheme;
    }
}