package org.bottiger.podcast.activities.intro;

import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.RawRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.VideoView;

import com.heinrichreimersoftware.materialintro.app.SlideFragment;

import org.bottiger.podcast.R;

/**
 * Created by aplb on 03-12-2016.
 */

public class VideoIntro extends SlideFragment {

    private static final String TITLE_KEY = "title";
    private static final String DESCRIPTION_KEY = "description";
    private static final String RESOURCE_KEY = "rawres";

    private VideoView mVideoView;
    private TextView mTitleView;
    private TextView mDescriptionView;

    public static VideoIntro newInstance(@NonNull String argTitle, @NonNull String argDescription, @RawRes int argRawRes) {
        VideoIntro fragment = new VideoIntro();
        Bundle bundle = new Bundle();
        bundle.putString(TITLE_KEY, argTitle);
        bundle.putString(DESCRIPTION_KEY, argDescription);
        bundle.putInt(RESOURCE_KEY, argRawRes);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.intro_movie_fragment, container, false);

        mVideoView =  (VideoView) view.findViewById(R.id.intro_video_view);
        mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mp.setLooping(true);
            }
        });

        mTitleView = (TextView) view.findViewById(R.id.mi_title);
        mDescriptionView = (TextView) view.findViewById(R.id.mi_description);

        Bundle arguments = getArguments();

        String title = arguments.getString(TITLE_KEY);
        String description = arguments.getString(DESCRIPTION_KEY);
        @RawRes int videoRes = arguments.getInt(RESOURCE_KEY);

        mTitleView.setText(title);
        mDescriptionView.setText(description);

        mVideoView.setVideoURI(Uri.parse("android.resource://" + getContext().getPackageName() + "/" + videoRes));
        mVideoView.start();
        mVideoView.requestFocus();

        return view;
    }

    @Override
    public void onViewCreated(View v, Bundle savedInstanceState) {
    }

}
