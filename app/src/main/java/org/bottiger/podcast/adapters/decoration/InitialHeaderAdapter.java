package org.bottiger.podcast.adapters.decoration;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.eowise.recyclerview.stickyheaders.StickyHeadersAdapter;

import org.bottiger.podcast.R;

import java.util.List;

/**
 * Created by apl on 19-01-2015.
 */
public class InitialHeaderAdapter implements StickyHeadersAdapter<InitialHeaderAdapter.ViewHolder> {
    private List<String> items;
    public InitialHeaderAdapter(List<String> items) {
        this.items = items;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.episode_list_header, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(ViewHolder headerViewHolder, int position) {
        //headerViewHolder.letter.setText(items.get(position).subSequence(0, 1));
        headerViewHolder.letter.setText("Bob");
    }

    @Override
    public long getHeaderId(int position) {
        return position; //items.get(position).charAt(0);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView letter;
        public ViewHolder(View itemView) {
            super(itemView);
            letter = (TextView) itemView.findViewById(R.id.title);
        }
    }
}