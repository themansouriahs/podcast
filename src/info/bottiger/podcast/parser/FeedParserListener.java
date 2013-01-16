package info.bottiger.podcast.parser;

import info.bottiger.podcast.provider.FeedItem;
import info.bottiger.podcast.utils.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.xml.sax.SAXException;


public class FeedParserListener implements FeedParserListenerInterface {

	public static final int MAX_SIZE = 100;
	public int resultCode = 0;
	private int max_valid_size = 10;

	private String feedTitle;
	private String feedDescription;
	private String feedImage;
	private String feedLink;
	private List<FeedItem> items = new ArrayList<FeedItem>();
	
	private final Log log = Log.getLog(getClass());


	public FeedParserListener(int max_valid_sz) {
		max_valid_size = max_valid_sz;
	}	

	
	public String getFeedTitle() {
		return feedTitle;
	}

	public String getFeedLink() {
		return feedLink;
	}

	public String getFeedDescription() {
		return feedDescription;
	}
	
	public String getFeedImage() {
		return feedImage;
	}
/*
	public FeedItem[] getFeedItems() {
		return items.toArray(new FeedItem[items.size()]);
	}
*/
	public int getFeedItemsSize() {
		return items.size();
	}

	@Override
	public void onFeedDescriptionLoad(String feedDescription) {
		this.feedDescription = feedDescription;
	}
	
	@Override
	public void onFeedImageLoad(String feedImage) {
		this.feedImage = feedImage;
	}

	@Override
	public void onFeedTitleLoad(String feedTitle) {
		this.feedTitle = feedTitle;
	}

	@Override
	public void onFeedLinkLoad(String feedLink) {
		this.feedLink = feedLink;
	}
	
	public FeedItem[] getSortItems() {

		int size = this.items.size()>max_valid_size ? max_valid_size : this.items.size();
		FeedItem[] result = new FeedItem[size]; 
		FeedItem[] item_list = items.toArray(new FeedItem[items.size()]);
		Arrays.sort( item_list,
                new Comparator<FeedItem>() {
                    @Override
					public int compare(FeedItem i1, FeedItem i2) {
                        long d1 = i1.getDate();
                        long d2 = i2.getDate();
                        if (d1==d2)
                            return i1.title.compareTo(i2.title);
                        return d1 > d2 ? (1) : (-1);
                    }
                }); 
		

		System.arraycopy(item_list, (items.size()-size), result, 0, size);
		return result;
	
	}
	

	@Override
	public void onItemLoad(FeedItem item) throws SAXException {
		items.add(item);
		if (items.size() >= MAX_SIZE) {
			throw new SAXException("OverSize!");
		}
	}
}
