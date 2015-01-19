package org.bottiger.podcast.adapters;

import java.util.ArrayList;

import org.bottiger.podcast.ApplicationConfiguration;
import org.bottiger.podcast.DrawerActivity;
import org.bottiger.podcast.FragmentContainerActivity;
import org.bottiger.podcast.TopActivity;
import org.bottiger.podcast.FragmentContainerActivity.SectionsPagerAdapter;
import org.bottiger.podcast.R;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.CheckedTextView;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class DrawerAdapter extends BaseExpandableListAdapter implements
		ExpandableListAdapter {

	private DrawerActivity activity;
	private ArrayList<Object> childtems;
	private LayoutInflater inflater;
	private ArrayList<String> parentItems, child;

	private SharedPreferences prefs;
	private boolean showListened;
	private String showListenedKey = ApplicationConfiguration.showListenedKey;

	public DrawerAdapter(ArrayList<String> parents, ArrayList<Object> childern) {
		this.parentItems = parents;
		this.childtems = childern;
	}

	public void setInflater(LayoutInflater inflater, DrawerActivity activity) {
		this.inflater = inflater;
		this.activity = activity;

		prefs = TopActivity.getPreferences();
		showListened = prefs.getBoolean(showListenedKey, true);
	}

	@Override
	public View getChildView(int groupPosition, final int childPosition,
			boolean isLastChild, View convertView, ViewGroup parent) {

		child = (ArrayList<String>) childtems.get(groupPosition);

		TextView textView = null;

		if (groupPosition == 0) {
			convertView = inflater.inflate(R.layout.drawer_filter, null);
		} else {

			if (convertView == null) {
				convertView = inflater.inflate(R.layout.drawer_group, null);
			}
		}

		if (groupPosition == 0) {
			Switch toggleSwitch = (Switch) convertView
					.findViewById(R.id.slidebar_show_listened);
			toggleSwitch.setChecked(showListened);
			CompoundButton.OnCheckedChangeListener l = new OnCheckedChangeListener() {
				public void onCheckedChanged(CompoundButton buttonView,
						boolean isChecked) {
					prefs.edit().putBoolean(showListenedKey, isChecked)
							.commit();
				}
			};
			toggleSwitch.setOnCheckedChangeListener(l);
		} else {

			textView = (TextView) convertView.findViewById(R.id.textView1);
			textView.setText(child.get(childPosition));

			convertView.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View view) {
					Toast.makeText(activity, child.get(childPosition),
							Toast.LENGTH_SHORT).show();
				}
			});
		}

		return convertView;
	}

	@Override
	public View getGroupView(final int groupPosition, boolean isExpanded,
			View convertView, final ViewGroup parent) {

		if (convertView == null) {
			convertView = inflater.inflate(R.layout.drawer_row, null);
		}

		((CheckedTextView) convertView).setText(parentItems.get(groupPosition));
		((CheckedTextView) convertView).setChecked(isExpanded);

		convertView.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View view) {
				int childCount = getChildrenCount(groupPosition);

				if (childCount == 0)
					activity.selectItem(groupPosition);
				else {
					ExpandableListView eLV = (ExpandableListView) parent;
					if (eLV.isGroupExpanded(groupPosition))
						eLV.collapseGroup(groupPosition);
					else
						eLV.expandGroup(groupPosition, true);
				}
			}
		});

		return convertView;
	}

	@Override
	public Object getChild(int groupPosition, int childPosition) {
		ArrayList<String> child = ((ArrayList<String>) childtems
				.get(groupPosition));
		return child.get(childPosition);
	}

	@Override
	public long getChildId(int groupPosition, int childPosition) {
		return (long) (groupPosition * 100 + childPosition); // FIXME
	}

	@Override
	public int getChildrenCount(int groupPosition) {
		if (childtems.size() > groupPosition) {
			ArrayList<String> children = (ArrayList<String>) childtems
					.get(groupPosition);
			return children.size();
		} else
			return 0;
	}

	@Override
	public Object getGroup(int groupPosition) {
		return parentItems.get(groupPosition);
	}

	@Override
	public int getGroupCount() {
		return parentItems.size();
	}

	@Override
	public void onGroupCollapsed(int groupPosition) {
		super.onGroupCollapsed(groupPosition);
	}

	@Override
	public void onGroupExpanded(int groupPosition) {
		super.onGroupExpanded(groupPosition);
	}

	@Override
	public long getGroupId(int groupPosition) {
		return groupPosition;
	}

	@Override
	public boolean hasStableIds() {
		return true;
	}

	@Override
	public boolean isChildSelectable(int groupPosition, int childPosition) {
		return true;
	}

}