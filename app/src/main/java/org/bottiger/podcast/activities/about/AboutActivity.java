package org.bottiger.podcast.activities.about;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.danielstone.materialaboutlibrary.MaterialAboutActivity;
import com.danielstone.materialaboutlibrary.items.MaterialAboutActionItem;
import com.danielstone.materialaboutlibrary.items.MaterialAboutItem;
import com.danielstone.materialaboutlibrary.items.MaterialAboutItemOnClickAction;
import com.danielstone.materialaboutlibrary.items.MaterialAboutTitleItem;
import com.danielstone.materialaboutlibrary.model.MaterialAboutCard;
import com.danielstone.materialaboutlibrary.model.MaterialAboutList;

import org.bottiger.podcast.ApplicationConfiguration;
import org.bottiger.podcast.R;
import org.bottiger.podcast.SoundWavesPreferenceFragment;
import org.bottiger.podcast.utils.SDCardManager;
import org.bottiger.podcast.utils.StorageUtils;
import org.bottiger.podcast.utils.StrUtils;

import java.io.IOException;

import static org.bottiger.podcast.utils.navdrawer.NavigationDrawerMenuGenerator.FEEDBACK;

/**
 * Created by aplb on 26-12-2016.
 */

public class AboutActivity extends MaterialAboutActivity {

    @Override
    protected MaterialAboutList getMaterialAboutList(final Context context) {

        MaterialAboutList.Builder builder = new MaterialAboutList.Builder();

        MaterialAboutCard.Builder aboutCardBuilder = new MaterialAboutCard.Builder();

        MaterialAboutCard.Builder appCardBuilder = new MaterialAboutCard.Builder();
        appCardBuilder.title(R.string.about_app_settings_into);

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
                .setOnClickAction(browserIntent("https://raw.githubusercontent.com/bottiger/SoundWaves/master/LICENSE.txt"))
                .build();

        aboutCardBuilder.addItem(version);
        aboutCardBuilder.addItem(license);

        authorCardBuilder.addItem(new MaterialAboutActionItem.Builder()
                .text(R.string.author_full_name)
                .subText(R.string.author_country)
                .icon(R.drawable.ic_person_black_24dp)
                .setOnClickAction(browserIntent(""))
                .build());

        authorCardBuilder.addItem(new MaterialAboutActionItem.Builder()
                .text(R.string.github)
                .subText(R.string.about_github_subtext)
                .icon(R.drawable.github_circle)
                .setOnClickAction(browserIntent("https://github.com/bottiger/SoundWaves"))
                .build());

        authorCardBuilder.addItem(new MaterialAboutActionItem.Builder()
                .text(R.string.twitter)
                .subText(R.string.about_twitter_subtext)
                .icon(R.drawable.twitter)
                .setOnClickAction(browserIntent("https://twitter.com/arvidbottiger"))
                .build());

        authorCardBuilder.addItem(new MaterialAboutActionItem.Builder()
                .text(R.string.reddit)
                .subText(R.string.about_reddit_subtext)
                .icon(R.drawable.reddit)
                .setOnClickAction(browserIntent("https://www.reddit.com/r/soundwavesapp/"))
                .build());

        authorCardBuilder.addItem(new MaterialAboutActionItem.Builder()
                .text(R.string.email)
                .subText(ApplicationConfiguration.ACRA_MAIL)
                .icon(R.drawable.email)
                .setOnClickAction(new MaterialAboutItemOnClickAction() {
                    @Override
                    public void onClick() {
                        Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                                "mailto", ApplicationConfiguration.ACRA_MAIL, null));
                        emailIntent.putExtra(Intent.EXTRA_SUBJECT, FEEDBACK);
                        startActivity(Intent.createChooser(emailIntent, getString(R.string.feedback_mail_client_picker)));
                    }
                })
                .build());

        String tmpDir;
        String podcastDir;
        boolean hasPermission = false;

        try {
            tmpDir = SDCardManager.getTmpDir(context);
            podcastDir = SDCardManager.getDownloadDir(context).toString();
            hasPermission = true;
        } catch (IOException e) {
            tmpDir = getResources().getString(R.string.error_mising_storage_permission);
            podcastDir = getResources().getString(R.string.error_mising_storage_permission);
        }

        final String tmpDirFinal = tmpDir;
        final String podcastDirFinal = podcastDir;

        MaterialAboutActionItem.Builder fileTmpLocationBuilder = new MaterialAboutActionItem.Builder()
                .text(R.string.about_tmp_dir_title)
                .icon(R.drawable.ic_folder_open_black_24dp)
                .subText(tmpDir);

        MaterialAboutActionItem.Builder filePodcastLocationBuilder = new MaterialAboutActionItem.Builder()
                .text(R.string.about_podcast_dir_title)
                .icon(R.drawable.ic_folder_open_black_24dp)
                .subText(podcastDir);

        if (hasPermission) {
            fileTmpLocationBuilder.setOnClickAction(new MaterialAboutItemOnClickAction() {
                @Override
                public void onClick() {
                    StorageUtils.openFolderIntent(context, tmpDirFinal);
                }
            });

            filePodcastLocationBuilder.setOnClickAction(new MaterialAboutItemOnClickAction() {
                @Override
                public void onClick() {
                    StorageUtils.openFolderIntent(context, podcastDirFinal);
                }
            });
        }

        appCardBuilder.addItem(fileTmpLocationBuilder.build());
        appCardBuilder.addItem(filePodcastLocationBuilder.build());

        builder.addCard(aboutCardBuilder.build());
        builder.addCard(authorCardBuilder.build());
        builder.addCard(appCardBuilder.build());

        return builder.build();
    }

    @Override
    protected CharSequence getActivityTitle() {
        return getString(R.string.mal_title_about);
    }

    private MaterialAboutItemOnClickAction browserIntent(@NonNull final String argUrl) {
        return new MaterialAboutItemOnClickAction() {
            @Override
            public void onClick() {

                if (!StrUtils.isValidUrl(argUrl)) {
                    return;
                }

                Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse(argUrl));
                startActivity(browserIntent);
            }
        };
    }

}