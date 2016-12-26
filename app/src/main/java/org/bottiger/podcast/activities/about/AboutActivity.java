package org.bottiger.podcast.activities.about;

import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.danielstone.materialaboutlibrary.MaterialAboutActivity;
import com.danielstone.materialaboutlibrary.model.MaterialAboutActionItem;
import com.danielstone.materialaboutlibrary.model.MaterialAboutCard;
import com.danielstone.materialaboutlibrary.model.MaterialAboutItem;
import com.danielstone.materialaboutlibrary.model.MaterialAboutList;
import com.danielstone.materialaboutlibrary.model.MaterialAboutTitleItem;

import org.bottiger.podcast.ApplicationConfiguration;
import org.bottiger.podcast.R;
import org.bottiger.podcast.SoundWavesPreferenceFragment;

import static org.bottiger.podcast.utils.navdrawer.NavigationDrawerMenuGenerator.FEEDBACK;

/**
 * Created by aplb on 26-12-2016.
 */

public class AboutActivity extends MaterialAboutActivity {

    @Override
    protected MaterialAboutList getMaterialAboutList() {

        MaterialAboutList.Builder builder = new MaterialAboutList.Builder();

        MaterialAboutCard.Builder aboutCardBuilder = new MaterialAboutCard.Builder();

        MaterialAboutCard.Builder authorCardBuilder = new MaterialAboutCard.Builder();
        authorCardBuilder.title(R.string.about_author);

        aboutCardBuilder.addItem(new MaterialAboutTitleItem.Builder()
                .text(R.string.app_name)
                .icon(R.drawable.ic_launcher_sw)
                .build());

        MaterialAboutItem version = new MaterialAboutActionItem.Builder()
                .text(R.string.pref_current_version_title)
                .subText(SoundWavesPreferenceFragment.getVersion(this))
                .icon(R.drawable.ic_info_outline_24dp)
                .build();

        MaterialAboutItem license = new MaterialAboutActionItem.Builder()
                .text(R.string.pref_license_title)
                .icon(R.drawable.ic_insert_drive_file_black_24dp)
                .setOnClickListener(browserIntent("https://raw.githubusercontent.com/bottiger/SoundWaves/master/LICENSE.txt"))
                .build();

        aboutCardBuilder.addItem(version);
        aboutCardBuilder.addItem(license);

        authorCardBuilder.addItem(new MaterialAboutActionItem.Builder()
                .text(R.string.author_full_name)
                .subText(R.string.author_country)
                .icon(R.drawable.ic_person_black_24dp)
                .setOnClickListener(browserIntent(""))
                .build());

        authorCardBuilder.addItem(new MaterialAboutActionItem.Builder()
                .text(R.string.github)
                .subText(R.string.about_github_subtext)
                .icon(R.drawable.github_circle)
                .setOnClickListener(browserIntent("https://github.com/bottiger/SoundWaves"))
                .build());

        authorCardBuilder.addItem(new MaterialAboutActionItem.Builder()
                .text(R.string.twitter)
                .subText(R.string.about_twitter_subtext)
                .icon(R.drawable.twitter)
                .setOnClickListener(browserIntent("https://twitter.com/arvidbottiger"))
                .build());

        authorCardBuilder.addItem(new MaterialAboutActionItem.Builder()
                .text(R.string.reddit)
                .subText(R.string.about_reddit_subtext)
                .icon(R.drawable.reddit)
                .setOnClickListener(browserIntent("https://www.reddit.com/r/soundwavesapp/"))
                .build());

        authorCardBuilder.addItem(new MaterialAboutActionItem.Builder()
                .text(R.string.email)
                .subText(ApplicationConfiguration.ACRA_MAIL)
                .icon(R.drawable.email)
                .setOnClickListener(new MaterialAboutActionItem.OnClickListener() {
                    @Override
                    public void onClick() {
                        Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                                "mailto", ApplicationConfiguration.ACRA_MAIL, null));
                        emailIntent.putExtra(Intent.EXTRA_SUBJECT, FEEDBACK);
                        startActivity(Intent.createChooser(emailIntent, getString(R.string.feedback_mail_client_picker)));
                    }
                })
                .build());

        builder.addCard(aboutCardBuilder.build());
        builder.addCard(authorCardBuilder.build());

        return builder.build();
    }

    @Override
    protected CharSequence getActivityTitle() {
        return getString(R.string.mal_title_about);
    }

    private MaterialAboutActionItem.OnClickListener browserIntent(@NonNull final String argUrl) {
        return new MaterialAboutActionItem.OnClickListener() {
            @Override
            public void onClick() {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse(argUrl));
                startActivity(browserIntent);
            }
        };
    }

}