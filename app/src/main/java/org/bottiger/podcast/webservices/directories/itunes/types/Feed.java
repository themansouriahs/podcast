package org.bottiger.podcast.webservices.directories.itunes.types;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Mariangela Salcedo (msalcedo047@gmail.com) on 08/10/16.
 * Copyright (c) 2016 m-salcedo. All rights reserved.
 */
public class Feed {

    @SerializedName("author")
    @Expose
    private Author author;
    @SerializedName("entry")
    @Expose
    private List<Entry> entry = new ArrayList<Entry>();
    @SerializedName("updated")
    @Expose
    private Updated updated;
    @SerializedName("rights")
    @Expose
    private Rights rights;
    @SerializedName("title")
    @Expose
    private Title title;
    @SerializedName("icon")
    @Expose
    private Icon icon;
    @SerializedName("link")
    @Expose
    private List<Link> link = new ArrayList<Link>();
    @SerializedName("id")
    @Expose
    private Id id;

    /**
     * No args constructor for use in serialization
     *
     */
    public Feed() {
    }

    /**
     *
     * @param id
     * @param icon
     * @param author
     * @param title
     * @param updated
     * @param link
     * @param entry
     * @param rights
     */
    public Feed(Author author, List<Entry> entry, Updated updated, Rights rights, Title title, Icon icon, List<Link> link, Id id) {
        this.author = author;
        this.entry = entry;
        this.updated = updated;
        this.rights = rights;
        this.title = title;
        this.icon = icon;
        this.link = link;
        this.id = id;
    }

    /**
     *
     * @return
     *     The author
     */
    public Author getAuthor() {
        return author;
    }

    /**
     *
     * @param author
     *     The author
     */
    public void setAuthor(Author author) {
        this.author = author;
    }

    /**
     *
     * @return
     *     The entry
     */
    public List<Entry> getEntry() {
        return entry;
    }

    /**
     *
     * @param entry
     *     The entry
     */
    public void setEntry(List<Entry> entry) {
        this.entry = entry;
    }

    /**
     *
     * @return
     *     The updated
     */
    public Updated getUpdated() {
        return updated;
    }

    /**
     *
     * @param updated
     *     The updated
     */
    public void setUpdated(Updated updated) {
        this.updated = updated;
    }

    /**
     *
     * @return
     *     The rights
     */
    public Rights getRights() {
        return rights;
    }

    /**
     *
     * @param rights
     *     The rights
     */
    public void setRights(Rights rights) {
        this.rights = rights;
    }

    /**
     *
     * @return
     *     The title
     */
    public Title getTitle() {
        return title;
    }

    /**
     *
     * @param title
     *     The title
     */
    public void setTitle(Title title) {
        this.title = title;
    }

    /**
     *
     * @return
     *     The icon
     */
    public Icon getIcon() {
        return icon;
    }

    /**
     *
     * @param icon
     *     The icon
     */
    public void setIcon(Icon icon) {
        this.icon = icon;
    }

    /**
     *
     * @return
     *     The link
     */
    public List<Link> getLink() {
        return link;
    }

    /**
     *
     * @param link
     *     The link
     */
    public void setLink(List<Link> link) {
        this.link = link;
    }

    /**
     *
     * @return
     *     The id
     */
    public Id getId() {
        return id;
    }

    /**
     *
     * @param id
     *     The id
     */
    public void setId(Id id) {
        this.id = id;
    }

}
