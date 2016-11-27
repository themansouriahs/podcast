package org.bottiger.podcast.views.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.widget.ContentLoadingProgressBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.TextView;

import org.bottiger.podcast.R;
import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.adapters.PlayerChapterAdapter;
import org.bottiger.podcast.flavors.CrashReporter.VendorCrashReporter;
import org.bottiger.podcast.player.GenericMediaPlayerInterface;
import org.bottiger.podcast.playlist.Playlist;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.provider.ISubscription;
import org.bottiger.podcast.provider.Subscription;
import org.bottiger.podcast.service.PlayerService;
import org.bottiger.podcast.utils.PlaybackSpeed;
import org.bottiger.podcast.utils.chapter.Chapter;
import org.bottiger.podcast.utils.chapter.ChapterUtil;
import org.bottiger.podcast.utils.rxbus.RxBasicSubscriber;
import org.bottiger.podcast.utils.rxbus.RxBusSimpleEvents;

import java.util.LinkedList;
import java.util.List;

import io.reactivex.functions.Predicate;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

import static android.view.View.GONE;

/**
 * Created by aplb on 25-11-2016.
 */

public class DialogChapters extends DialogFragment {

    private static final String TAG = DialogFragment.class.getSimpleName();

    private static final String scopeName = "EpisodeID";
    private static final long no_chapters = -1;

    @NonNull private Activity mActivity;

    @NonNull private TextView mTitleView;
    @NonNull private TextView mChaptersMissing;
    @NonNull private RecyclerView mRecyclerView;
    @NonNull private ContentLoadingProgressBar mContentLoadingProgressBar;

    @NonNull private PlayerChapterAdapter mAdapter;
    @NonNull private RecyclerView.LayoutManager mLayoutManager;


    public static DialogChapters newInstance(@NonNull IEpisode argEpisode) {
        DialogChapters frag = new DialogChapters();
        Bundle args = new Bundle();

        long id = no_chapters;
        if (argEpisode instanceof FeedItem) {
            id = ((FeedItem)argEpisode).getId();
        }
        args.putLong(scopeName, id);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mActivity = getActivity();

        long episodeID = getArguments().getLong(scopeName, no_chapters);
        boolean hasEpisode = episodeID > 0;
        final IEpisode episode = hasEpisode ?
                SoundWaves.getAppContext(mActivity).getLibraryInstance().getEpisode(episodeID) :
                null;

        final List<Chapter> chapters = hasEpisode ? episode.getChapters() : new LinkedList<Chapter>();
        final long offset = hasEpisode ? episode.getOffset() : 0;

        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        // Get the layout inflater
        LayoutInflater inflater = mActivity.getLayoutInflater();

        View view = inflater.inflate(R.layout.dialog_chapters, null);

        // bind stuff
        mTitleView = (TextView) view.findViewById(R.id.dialog_chapter_title);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.chapter_recycler_view);
        mContentLoadingProgressBar = (ContentLoadingProgressBar) view.findViewById(R.id.chapter_progress);
        mChaptersMissing = (TextView) view.findViewById(R.id.dialog_chapter_missing);

        if (chapters.isEmpty()) {
            mContentLoadingProgressBar.show();
        } else {
            mContentLoadingProgressBar.setVisibility(GONE);
        }

        mAdapter = new PlayerChapterAdapter(chapters);
        Integer index = ChapterUtil.getCurrentChapterIndex(episode, offset);
        mAdapter.setActive(index);

        mLayoutManager = new LinearLayoutManager(mActivity);

        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mAdapter);

        SoundWaves.getAppContext(mActivity)
                .getChapterObservable()
                .subscribe(new RxBasicSubscriber<Integer>("ChapterChanged") {
                    @Override
                    public void onNext(Integer integer) {

                        if (episode == null)
                            return;

                        Chapter chapter = episode.getChapters().get(integer);
                        mAdapter.setActive(integer);

                        if (chapter != null && chapter.getStart() >= 0) {
                            //SoundWaves.getAppContext(mActivity).getPlayer().seekTo(chapter.getStart());
                            episode.seekTo(chapter.getStart());
                        }
                    }
                });

        if (hasEpisode) {
            ChapterUtil.getChapters(mActivity, episode)
                    .subscribeOn(io.reactivex.schedulers.Schedulers.io())
                    .observeOn(io.reactivex.android.schedulers.AndroidSchedulers.mainThread())
                    .subscribe(new RxBasicSubscriber<List<Chapter>>("ChapterLoaded") {
                        @Override
                        public void onNext(List<Chapter> chapters) {

                            episode.setChapters(chapters);
                            mAdapter.setDataset(chapters);

                            Integer index = ChapterUtil.getCurrentChapterIndex(episode, offset);
                            mAdapter.setActive(index);

                            Log.d(TAG, "chapters: " + chapters.size());
                            mContentLoadingProgressBar.hide();

                            if (!episode.hasChapters()) {
                                mChaptersMissing.setVisibility(View.VISIBLE);
                            }
                        }
                    });
        }


        builder.setView(view);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {

            }
        });

        return builder.create();
    }

}
