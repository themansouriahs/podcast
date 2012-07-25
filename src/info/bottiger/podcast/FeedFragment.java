package info.bottiger.podcast;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import info.bottiger.podcast.provider.ItemColumns;

public class FeedFragment extends RecentItemFragment {
	
	private View v;
	private int subId = 0;
	
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
        
        this.subId = savedInstanceState != null ? savedInstanceState.getInt("subID") : subId;
        v = super.onCreateView(inflater, container, savedInstanceState);
        
        return v;
    }
	
    @Override
	public String getWhere() {
		String where = ItemColumns.SUBS_ID + "=" + subId;
		return where;
	}

}
