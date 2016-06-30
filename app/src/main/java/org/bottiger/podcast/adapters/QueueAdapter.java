package org.bottiger.podcast.adapters;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.bottiger.podcast.provider.QueueEpisode;

import java.util.ArrayList;
import java.util.PriorityQueue;

public class QueueAdapter extends ArrayAdapter<QueueEpisode> {
	
	Context context;
    int layoutResourceId;   
    int textResourceId;   
    ArrayList<QueueEpisode> mDownloadQueue = null;
    private static PriorityQueue<QueueEpisode> mDownloadQueue2 = new PriorityQueue<QueueEpisode>();
	
    public QueueAdapter(Context context, int layoutViewResourceId, int textViewResourceId,
    		ArrayList<QueueEpisode> objects) {
		super(context, textViewResourceId, objects);
		this.context = context;
		this.layoutResourceId = layoutViewResourceId;
		this.textResourceId = textViewResourceId;
		this.mDownloadQueue = objects;
	}
    
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        EpisodeHolder holder = null;
       
        if(row == null)
        {
            LayoutInflater inflater = ((Activity)context).getLayoutInflater();
            row = inflater.inflate(layoutResourceId, parent, false);
           
            holder = new EpisodeHolder();
            holder.txtTitle = (TextView)row.findViewById(textResourceId);
           
            row.setTag(holder);
        }
        else
        {
            holder = (EpisodeHolder)row.getTag();
        }
       
        QueueEpisode episode = mDownloadQueue.get(position);
        holder.txtTitle.setText(episode.getTitle());
       
        return row;
    }
   
    static class EpisodeHolder
    {
        TextView txtTitle;
    }
}
