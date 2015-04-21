package org.bottiger.podcast.utils.navdrawer;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;

import org.bottiger.podcast.ApplicationConfiguration;
import org.bottiger.podcast.R;
import org.bottiger.podcast.utils.TransitionUtils;

import java.util.LinkedList;

/**
 * Created by apl on 25-02-2015.
 */
public class NavigationDrawerMenuGenerator {

    private static final String FEEDBACK = "[Feedback]"; // NoI18N

    private final LinkedList<NavItem> mItems = new LinkedList<>();

    private Activity mActivity;

    public NavigationDrawerMenuGenerator(@NonNull final Activity argContext) {
        mActivity = argContext;

        /*
        // Settings
        mItems.add(new NavItem());
        mItems.add(new NavItem(R.string.menu_settings, R.drawable.ic_settings_white, new INavOnClick() {
            @Override
            public void onClick() {
                TransitionUtils.openSettings(mActivity);
            }
        }));
        */
        mItems.add(new NavItem());
        mItems.add(new NavItem(R.string.menu_feedback, R.drawable.ic_messenger_white, new INavOnClick() {
            @Override
            public void onClick() {
                Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                        "mailto", ApplicationConfiguration.ACRA_MAIL, null));
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, FEEDBACK);
                argContext.startActivity(Intent.createChooser(emailIntent, argContext.getString(R.string.feedback_mail_client_picker)));
            }
        }));
        mItems.add(new NavItem());
    }

    public void generate(@NonNull ViewGroup argContainer) {
        for(NavItem item : mItems)
            generateView(item, argContainer);
    }

    private void generateView(@NonNull NavItem argItem, @NonNull ViewGroup argContainer) {
        int layoutResource = argItem.getLayout();
        View view = mActivity.getLayoutInflater().inflate(layoutResource, argContainer, false);

        argItem.bindToView(view);

        argContainer.addView(view);
    }
}
