package info.bottiger.podcast;

import android.app.ListFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ListView;

public class HackListFragment extends ListFragment {

	public ListView mList;
	boolean mListShown;
	View mProgressContainer;
	View mListContainer;

	public void setListShown(boolean shown, boolean animate){
	    if (mListShown == shown) {
	        return;
	    }
	    mListShown = shown;
	    if (shown) {
	        if (animate) {
	            mProgressContainer.startAnimation(AnimationUtils.loadAnimation(
	                    getActivity(), android.R.anim.fade_out));
	            mListContainer.startAnimation(AnimationUtils.loadAnimation(
	                    getActivity(), android.R.anim.fade_in));
	        }
	        mProgressContainer.setVisibility(View.GONE);
	        mListContainer.setVisibility(View.VISIBLE);
	    } else {
	        if (animate) {
	            mProgressContainer.startAnimation(AnimationUtils.loadAnimation(
	                    getActivity(), android.R.anim.fade_in));
	            mListContainer.startAnimation(AnimationUtils.loadAnimation(
	                    getActivity(), android.R.anim.fade_out));
	        }
	        mProgressContainer.setVisibility(View.VISIBLE);
	        mListContainer.setVisibility(View.INVISIBLE);
	    }
	}
	public void setListShown(boolean shown){
	    setListShown(shown, true);
	}
	public void setListShownNoAnimation(boolean shown) {
	    setListShown(shown, false);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	        Bundle savedInstanceState) {
	    int INTERNAL_EMPTY_ID = 0x00ff0001;
	    View root = inflater.inflate(R.layout.list_content, container, false);
	    (root.findViewById(R.id.internalEmpty)).setId(INTERNAL_EMPTY_ID);
	    mList = (ListView) root.findViewById(android.R.id.list);
	    mListContainer =  root.findViewById(R.id.listContainer);
	    mProgressContainer = root.findViewById(R.id.progressContainer);
	    mListShown = true;
	    return root;
	}
	
}
