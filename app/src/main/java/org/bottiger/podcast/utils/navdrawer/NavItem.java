package org.bottiger.podcast.utils.navdrawer;

import android.support.annotation.NonNull;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.bottiger.podcast.R;

/**
 * Created by apl on 25-02-2015.
 */
public class NavItem {

    public enum TYPE { BUTTON, SEPARATOR };

    private TYPE mType;
    private int mText;
    private int mIcon;
    private INavOnClick mCallback;

    public NavItem() {
        mType = TYPE.SEPARATOR;
    }

    public NavItem(@NonNull int argTextResource, @NonNull int argIcon, @NonNull INavOnClick argCallback) {
        mType = TYPE.BUTTON;
        mText = argTextResource;
        mIcon = argIcon;
        mCallback = argCallback;
    }

    public void bindToView(@NonNull View argView) {
        if (mType == TYPE.SEPARATOR)
            return;

        ImageView imageView = (ImageView)argView.findViewById(R.id.drawer_item_icon);
        TextView textView   = (TextView)argView.findViewById(R.id.drawer_item_title);

        imageView.setImageResource(mIcon);

        textView.setTextColor(argView.getResources().getColor(R.color.drawer_text));
        textView.setText(mText);

        argView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCallback.onClick();
            }
        });
    }

    public TYPE getType() {
        return mType;
    }

    public int getLayout() {
        return mType  == TYPE.BUTTON ? R.layout.drawer_item : R.layout.drawer_separator;
    }
}
