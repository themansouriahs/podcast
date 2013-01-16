package info.bottiger.podcast;

import android.os.Bundle;
import info.bottiger.podcast.provider.ItemColumns;

public class FeedFragment extends RecentItemFragment {

	// FIXME should not be static
	private static long subId = 14;
	
	public static FeedFragment newInstance(long subID) {
	    FeedFragment fragment = new FeedFragment();

	    Bundle args = new Bundle();
	    args.putLong("subID", subID);
	    fragment.setArguments(args);
	    
	    FeedFragment.subId = subID;

	    return fragment;
	}


	@Override
	public String getWhere() {		
		String where = ItemColumns.SUBS_ID + "=" + subId;
		return where;
	}

}
