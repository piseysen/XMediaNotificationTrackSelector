package de.Maxr1998.xposed.mnts;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;

import java.lang.reflect.Method;

import de.Maxr1998.xposed.mnts.view.CustomTrackAdapter;
import de.Maxr1998.xposed.mnts.view.IntentView;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static android.widget.RelativeLayout.TRUE;
import static de.robv.android.xposed.XposedBridge.log;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;

public class TrackSelector {

    public static final int INTENT_VIEW_ID = 0x7f0f0200;
    public static final String SEEK_COUNT_EXTRA = "new_queue_position";
    public static final String CURRENT_PLAYING_POSITION_EXTRA = "current_queue_position";
    public static final String TRACK_INFO_EXTRA = "track_data";
    public static final String REPLY_INTENT_EXTRA = "reply";

    public static void initUI(final XC_LoadPackage.LoadPackageParam lPParam) {
        try {
            // Edit notification view
            findAndHookMethod("android.widget.RemoteViews", lPParam.classLoader, "performApply", View.class, ViewGroup.class, findClass("android.widget.RemoteViews.OnClickHandler", lPParam.classLoader), new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                    View v = (View) param.args[0];
                    if (v instanceof RelativeLayout) {
                        final RelativeLayout root = (RelativeLayout) v;
                        if ((root.getChildCount() == 4
                                && (root.getChildAt(0) instanceof FrameLayout || root.getChildAt(0) instanceof ImageView)
                                && root.getChildAt(1) instanceof LinearLayout
                                && root.getChildAt(2) instanceof LinearLayout
                                && root.getChildAt(3) instanceof ImageView)
                                || root.getTag().toString().matches("bigMedia(Narrow)?")) {
                            root.setTag("xgpmBigMedia");
                            final Resources res = root.getResources();
                            final float density = res.getDisplayMetrics().density;
                            final ViewGroup.LayoutParams rootParams = root.getLayoutParams();
                            final ImageButton queueButton = new ImageButton(root.getContext());
                            final ListView queueLayout = new ListView(root.getContext());
                            final View.OnClickListener close = new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    // Close
                                    Animator anim = ViewAnimationUtils.createCircularReveal(queueLayout, (int) (queueButton.getX() + queueButton.getWidth() / 2), (int) (queueButton.getY() + queueButton.getHeight() / 2), density * 416, density * 24);
                                    anim.addListener(new AnimatorListenerAdapter() {
                                        @Override
                                        public void onAnimationEnd(Animator animation) {
                                            super.onAnimationEnd(animation);
                                            queueLayout.setVisibility(View.GONE);
                                            queueButton.setImageDrawable(res.getDrawable(res.getIdentifier("ic_queue_music", "drawable", BuildConfig.APPLICATION_ID), null));
                                        }
                                    });
                                    anim.start();
                                    Animation collapse = new Animation() {
                                        @Override
                                        protected void applyTransformation(float interpolatedTime, Transformation t) {
                                            rootParams.height = (int) (density * 128 * (2f - interpolatedTime));
                                            root.requestLayout();
                                        }

                                        @Override
                                        public boolean willChangeBounds() {
                                            return true;
                                        }
                                    };
                                    collapse.setDuration(300);
                                    root.startAnimation(collapse);
                                }
                            };
                            final View.OnClickListener toggle = new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    if (queueLayout.getVisibility() == View.VISIBLE) {
                                        close.onClick(v);
                                        return;
                                    }
                                    Animation expand = new Animation() {
                                        @Override
                                        protected void applyTransformation(float interpolatedTime, Transformation t) {
                                            rootParams.height = (int) (density * 128 * (1f + interpolatedTime));
                                            root.requestLayout();
                                        }

                                        @Override
                                        public boolean willChangeBounds() {
                                            return true;
                                        }
                                    };
                                    expand.setDuration(300);
                                    root.startAnimation(expand);
                                    queueLayout.setVisibility(View.VISIBLE);
                                    Animator reveal = ViewAnimationUtils.createCircularReveal(queueLayout, (int) (v.getX() + v.getWidth() / 2), (int) (v.getY() + v.getHeight() / 2), density * 24, density * 416);
                                    reveal.addListener(new AnimatorListenerAdapter() {
                                        @Override
                                        public void onAnimationEnd(Animator animation) {
                                            super.onAnimationEnd(animation);
                                            queueButton.setImageDrawable(res.getDrawable(res.getIdentifier("ic_close", "drawable", BuildConfig.APPLICATION_ID), null));
                                        }
                                    });
                                    reveal.start();
                                }
                            };
                            // Queue button
                            queueButton.setImageDrawable(res.getDrawable(res.getIdentifier("ic_queue_music", "drawable", BuildConfig.APPLICATION_ID), null));
                            queueButton.setColorFilter(Color.WHITE);
                            queueButton.setBackground(null);
                            RelativeLayout.LayoutParams buttonParams = new RelativeLayout.LayoutParams((int) (density * 48), (int) (density * 48));
                            buttonParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, TRUE);
                            buttonParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, TRUE);
                            buttonParams.addRule(RelativeLayout.ALIGN_PARENT_END, TRUE);
                            queueButton.setLayoutParams(buttonParams);
                            queueButton.setOnClickListener(toggle);
                            // Prevent text overlapping queue button
                            ViewGroup.MarginLayoutParams titleContainerParams = (ViewGroup.MarginLayoutParams) root.getChildAt(1).getLayoutParams();
                            titleContainerParams.rightMargin = (int) (density * 48);
                            titleContainerParams.setMarginEnd(titleContainerParams.rightMargin);
                            // Track container
                            queueLayout.setBackgroundColor(Color.WHITE);
                            queueLayout.setClickable(true);
                            queueLayout.setVisibility(View.GONE);
                            queueLayout.setOnTouchListener(new View.OnTouchListener() {
                                @Override
                                public boolean onTouch(View v, MotionEvent event) {
                                    ViewParent mScrollLayout = v.getParent().getParent().getParent().getParent();
                                    mScrollLayout.requestDisallowInterceptTouchEvent(true);
                                    callMethod(mScrollLayout, "removeLongPressCallback");
                                    return false;
                                }
                            });
                            queueLayout.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                @Override
                                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                    int mCurrentPosition = ((CustomTrackAdapter) parent.getAdapter()).getCurrentPosition();
                                    if (position != mCurrentPosition) {
                                        Intent intent = new Intent();
                                        intent.putExtra(SEEK_COUNT_EXTRA, position - mCurrentPosition);
                                        try {
                                            ((CustomTrackAdapter) parent.getAdapter()).reply().send(view.getContext(), 0, intent);
                                        } catch (PendingIntent.CanceledException e) {
                                            log(e);
                                        }
                                    }
                                    close.onClick(view);
                                }
                            });
                            IntentView intent = new IntentView(root.getContext());
                            intent.setChildList(queueLayout);
                            intent.setVisibility(View.GONE);
                            //noinspection ResourceType
                            intent.setId(INTENT_VIEW_ID);
                            root.addView(intent);
                            root.addView(queueLayout, root.getChildCount(), new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                            root.addView(queueButton, root.getChildCount());
                        }
                    }
                }
            });
            // Allow resolveIntent to be called as RemoteView
            findAndHookMethod("java.lang.reflect.Method", lPParam.classLoader, "isAnnotationPresent", Class.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (((Method) param.thisObject).getName().equals("resolveIntent") && ((Class) param.args[0]).getName().equals("android.view.RemotableViewMethod")) {
                        param.setResult(true);
                    }
                }
            });
        } catch (Throwable t) {
            log(t);
        }
    }
}