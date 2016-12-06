package org.bottiger.podcast;

import android.content.Context;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by apl on 13-02-2015.
 *
 * HACK: http://stackoverflow.com/a/27422401/911646
 */
public class ToolbarPreference extends Preference {

    public ToolbarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        super.onCreateView(parent);
        parent.setPadding(0, 0, 0, 0);

        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.preference_screen_toolbar, parent, false);

        Toolbar toolbar = (Toolbar) layout.findViewById(R.id.soundwaves_toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white);
        toolbar.setTitle(getTitle());
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PreferenceScreen prefScreen = (PreferenceScreen) getPreferenceManager().findPreference(getKey());
                prefScreen.getDialog().dismiss();
            }
        });

        return layout;
    }

}
