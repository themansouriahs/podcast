package org.bottiger.podcast.webservices.directories.itunes.types;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Mariangela Salcedo (msalcedo047@gmail.com) on 08/10/16.
 * Copyright (c) 2016 m-salcedo. All rights reserved.
 */
public class Entry {

    @SerializedName("im:name")
    @Expose
    private ImName imName;
    @SerializedName("im:image")
    @Expose
    private List<ImImage> imImage = new ArrayList<ImImage>();
    @SerializedName("summary")
    @Expose
    private Summary summary;
    @SerializedName("im:price")
    @Expose
    private ImPrice imPrice;
    @SerializedName("im:contentType")
    @Expose
    private ImContentType imContentType;
    @SerializedName("rights")
    @Expose
    private Rights rights;
    @SerializedName("title")
    @Expose
    private Title title;
    @SerializedName("link")
    @Expose
    private Link link;
    @SerializedName("id")
    @Expose
    private Id id;
    @SerializedName("im:artist")
    @Expose
    private ImArtist imArtist;
    @SerializedName("category")
    @Expose
    private Category category;
    @SerializedName("im:releaseDate")
    @Expose
    private ImReleaseDate imReleaseDate;

    /**
     * No args constructor for use in serialization
     */
    public Entry() {
    }

    /**
     * @param id
     * @param summary
     * @param imContentType
     * @param category
     * @param title
     * @param imArtist
     * @param imReleaseDate
     * @param link
     * @param rights
     * @param imPrice
     * @param imImage
     * @param imName
     */
    public Entry(ImName imName, List<ImImage> imImage, Summary summary, ImPrice imPrice, ImContentType imContentType, Rights rights, Title title, Link link, Id id, ImArtist imArtist, Category category, ImReleaseDate imReleaseDate) {
        this.imName = imName;
        this.imImage = imImage;
        this.summary = summary;
        this.imPrice = imPrice;
        this.imContentType = imContentType;
        this.rights = rights;
        this.title = title;
        this.link = link;
        this.id = id;
        this.imArtist = imArtist;
        this.category = category;
        this.imReleaseDate = imReleaseDate;
    }

    /**
     * @return The imName
     */
    public ImName getImName() {
        return imName;
    }

    /**
     * @param imName The im:name
     */
    public void setImName(ImName imName) {
        this.imName = imName;
    }

    /**
     * @return The imImage
     */
    public List<ImImage> getImageList() {
        return imImage;
    }

    public ImImage getImage() {
        return imImage.get(imImage.size() - 1);
    }

    /**
     * @param imImage The im:image
     */
    public void setImImage(List<ImImage> imImage) {
        this.imImage = imImage;
    }

    /**
     * @return The summary
     */
    public Summary getSummary() {
        return summary;
    }

    /**
     * @param summary The summary
     */
    public void setSummary(Summary summary) {
        this.summary = summary;
    }

    /**
     * @return The imPrice
     */
    public ImPrice getImPrice() {
        return imPrice;
    }

    /**
     * @param imPrice The im:price
     */
    public void setImPrice(ImPrice imPrice) {
        this.imPrice = imPrice;
    }

    /**
     * @return The imContentType
     */
    public ImContentType getImContentType() {
        return imContentType;
    }

    /**
     * @param imContentType The im:contentType
     */
    public void setImContentType(ImContentType imContentType) {
        this.imContentType = imContentType;
    }

    /**
     * @return The rights
     */
    public Rights getRights() {
        return rights;
    }

    /**
     * @param rights The rights
     */
    public void setRights(Rights rights) {
        this.rights = rights;
    }

    /**
     * @return The title
     */
    public Title getTitle() {
        return title;
    }

    /**
     * @param title The title
     */
    public void setTitle(Title title) {
        this.title = title;
    }

    /**
     * @return The link
     */
    public Link getLink() {
        return link;
    }

    /**
     * @param link The link
     */
    public void setLink(Link link) {
        this.link = link;
    }

    /**
     * @return The id
     */
    public Id getId() {
        return id;
    }

    /**
     * @param id The id
     */
    public void setId(Id id) {
        this.id = id;
    }

    /**
     * @return The imArtist
     */
    public ImArtist getImArtist() {
        return imArtist;
    }

    /**
     * @param imArtist The im:artist
     */
    public void setImArtist(ImArtist imArtist) {
        this.imArtist = imArtist;
    }

    /**
     * @return The category
     */
    public Category getCategory() {
        return category;
    }

    /**
     * @param category The category
     */
    public void setCategory(Category category) {
        this.category = category;
    }

    /**
     * @return The imReleaseDate
     */
    public ImReleaseDate getImReleaseDate() {
        return imReleaseDate;
    }

    /**
     * @param imReleaseDate The im:releaseDate
     */
    public void setImReleaseDate(ImReleaseDate imReleaseDate) {
        this.imReleaseDate = imReleaseDate;
    }

    public static Entry valueOf(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, Entry.class);
    }

}