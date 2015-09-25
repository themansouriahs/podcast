package org.bottiger.podcast.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewTreeObserver;

import org.bottiger.podcast.R;

/**
 * Created by aplb on 25-09-2015.
 */
public class ViewBackground extends View {

    private int mDictatingViewId = -1;
    private View mDictatingView;

    private ViewTreeObserver mVto;

    public ViewBackground(Context context) {
        super(context);
        init(context, null);
    }

    public ViewBackground(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public ViewBackground(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    @TargetApi(21)
    public ViewBackground(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    private void init(@NonNull Context context, @Nullable AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.DictatingView);

        final int N = a.getIndexCount();
        for (int i = 0; i < N; ++i)
        {
            int attr = a.getIndex(i);
            switch (attr)
            {
                case R.styleable.DictatingView_dictatingView:
                    mDictatingViewId = a.getResourceId(attr, 0);
                    break;
            }
        }
        a.recycle();

        /*
        mDictatingViewId =
        if (dictatingViewResourceID > 0) {
            mDictatingView = findViewById(dictatingViewResourceID);
        }
        */
    }

    @Override
    protected void onAttachedToWindow () {
        super.onAttachedToWindow();
        if (mDictatingViewId > 0) {
            mDictatingView = ((View)getParent().getParent()).findViewById(mDictatingViewId);
            observeToolbarHeight(mDictatingView, this);
        }
    }

    @Override
    protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec) {
        if (mDictatingView != null) {
            boolean canReadValues = mDictatingView.getLayoutParams().width > 0; // FIXME: find better solution
            if (canReadValues) {
                widthMeasureSpec = mDictatingView.getMeasuredWidth();
                heightMeasureSpec = mDictatingView.getMeasuredHeight();
            }
        }

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        super.setMeasuredDimension(widthMeasureSpec, heightMeasureSpec);
    }

    private void observeToolbarHeight(@NonNull final View argToolbar, @NonNull final View argToolbarBackground) {

        mVto = argToolbar.getViewTreeObserver();

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            mVto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                @TargetApi(16)
                public void onGlobalLayout() {
                    argToolbar.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    setToolbarBackgroundHeight(argToolbar, argToolbarBackground);

                }
            });
        } else {
            mVto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    argToolbar.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                    setToolbarBackgroundHeight(argToolbar, argToolbarBackground);

                }
            });
        }
    }

    private void setToolbarBackgroundHeight(@NonNull final View argToolbar, @NonNull final View argToolbarBackground) {
        argToolbarBackground.getLayoutParams().width  = argToolbar.getMeasuredWidth();
        argToolbarBackground.getLayoutParams().height = argToolbar.getMeasuredHeight();
    }
}
