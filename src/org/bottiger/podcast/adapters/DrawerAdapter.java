package org.bottiger.podcast.adapters;

import org.bottiger.podcast.R;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class DrawerAdapter extends ArrayAdapter<String> {
	  private final Context context;
	  private final String[] values;

	  public DrawerAdapter(Context context, String[] values) {
	    super(context, R.layout.drawer_row, values);
	    this.context = context;
	    this.values = values;
	  }

	  @Override
	  public View getView(int position, View convertView, ViewGroup parent) {
	    LayoutInflater inflater = (LayoutInflater) context
	        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	    View rowView = inflater.inflate(R.layout.drawer_row, parent, false);
	    TextView textView = (TextView) rowView.findViewById(R.id.sequence);
	    textView.setText(values[position]);

	    return rowView;
	  }
	} 