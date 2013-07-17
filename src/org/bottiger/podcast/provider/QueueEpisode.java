package org.bottiger.podcast.provider;

import java.util.Comparator;

/**
 * A Thin wrapper for episodes in the download queue.
 * 
 * @author Arvid Böttiger
 *
 */
public class QueueEpisode implements Comparable {

	private String title;
	private long episodeID;
	private Integer queuePriority;

	public QueueEpisode(FeedItem episode) {
		title = episode.getTitle();
		episodeID = episode.getId();
		
		int priority = episode.getPriority();
		if (priority > 0)
			queuePriority = priority;
		else
			queuePriority = (int) episodeID; 
	}
	
	public long getId() {
		return episodeID;
	}
	
	public String getTitle() {
		return title;
	}

	public Integer getPriority() {
		return queuePriority;
	}

	public void setPriority(Integer priority) {
		queuePriority = priority;
	}

	@Override
	public int compareTo(Object other) {
		Integer otherPriority = ((QueueEpisode)other).getPriority();
	    if(this.getPriority() == otherPriority)
	        return 0;
	    else if (this.getPriority() < otherPriority)
	        return 1;
	    else 
	        return -1;
	}
	
	@Override
	public String toString() {
		return "QueueEpisode (" + String.valueOf(this.getPriority()) + "): " + this.getTitle();
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (episodeID ^ (episodeID >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		QueueEpisode other = (QueueEpisode) obj;
		if (episodeID != other.episodeID)
			return false;
		return true;
	}

}