package org.bottiger.podcast.playlist;

import org.bottiger.podcast.model.Library;

/**
 * Created by apl on 27-05-2015.
 */
public class PlaylistData {
    @Library.SortOrder
    public int sortOrder;
    public Boolean showListened;
    public Boolean onlyDownloaded;
    public Boolean reset;
    public Boolean playlistChanged;
}
