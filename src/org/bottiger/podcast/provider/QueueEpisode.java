package org.bottiger.podcast.provider;

import java.util.Comparator;

/**
 * A Thin wrapper for episodes in the download queue.
 * 
 * @author Arvid Böttiger
 *
 */
public class QueueEpisode implements Comparator<QueueEpisode> {

	private String title;
	private long episodeID;
	private Integer queuePriority;

	QueueEpisode(FeedItem episode) {
		title = episode.getTitle();
		episodeID = episode.getId();
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
	public int compare(QueueEpisode lhs, QueueEpisode rhs) {
		if (lhs.getPriority() < rhs.getPriority())
			return -1;

		if (lhs.getPriority() > rhs.getPriority())
			return 1;

		return 0;
	}

}