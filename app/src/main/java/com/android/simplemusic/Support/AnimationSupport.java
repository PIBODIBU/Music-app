package com.android.simplemusic.Support;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import com.android.simplemusic.R;

import io.codetail.animation.SupportAnimator;
import io.codetail.animation.ViewAnimationUtils;

/**
 * Created by root on 3/19/16.
 */
public class AnimationSupport {
    public static final String TAG = "AnimationSupport";


    public static class Reveal {
        private static final String TAG = "Reveal";

        public static int animDuration = 400;

        /* Center of View
        int cx = (view.getLeft() + view.getRight()) / 2;
        int cy = (view.getTop() + view.getBottom()) / 2;
         */

        public static void openFromLeft(final View view) {
            int cx = view.getLeft();
            int cy = view.getLeft();

            // get the final radius for the clipping circle
            int dx = Math.max(cx, view.getWidth());
            int dy = Math.max(cy, view.getHeight());
            float finalRadius = (float) Math.hypot(dx, dy);

            SupportAnimator animator = ViewAnimationUtils.createCircularReveal(view, cx, cy, 0, finalRadius);
            animator.setInterpolator(new AccelerateDecelerateInterpolator());
            animator.setDuration(animDuration);
            view.setVisibility(View.VISIBLE);

            try {
                animator.start();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        public static void openFromLeft(final View view, final AnimationAction animationAction) {
            int cx = view.getLeft();
            int cy = view.getLeft();

            // get the final radius for the clipping circle
            int dx = Math.max(cx, view.getWidth());
            int dy = Math.max(cy, view.getHeight());
            float finalRadius = (float) Math.hypot(dx, dy);

            SupportAnimator animator = ViewAnimationUtils.createCircularReveal(view, cx, cy, 0, finalRadius);
            animator.setInterpolator(new AccelerateDecelerateInterpolator());
            animator.setDuration(animDuration);
            view.setVisibility(View.VISIBLE);

            if (animationAction != null) {
                animationAction.onPrepare();
            }

            try {
                animator.start();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        public static void closeToLeft(final View view) {
            int cx = view.getLeft();
            int cy = view.getLeft();

            // get the final radius for the clipping circle
            int dx = Math.max(cx, view.getWidth() - cx);
            int dy = Math.max(cy, view.getHeight() - cy);
            float finalRadius = (float) Math.hypot(dx, dy);

            SupportAnimator animator = ViewAnimationUtils.createCircularReveal(view, cx, cy, 0, finalRadius);
            animator.setInterpolator(new AccelerateDecelerateInterpolator());
            animator.setDuration(animDuration);

            animator = animator.reverse();

            try {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        view.setVisibility(View.GONE);
                    }
                }, animDuration - 100);

                animator.start();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        public static void closeToLeft(final View view, final AnimationAction animationAction) {
            int cx = view.getLeft();
            int cy = view.getLeft();

            // get the final radius for the clipping circle
            int dx = Math.max(cx, view.getWidth() - cx);
            int dy = Math.max(cy, view.getHeight() - cy);
            float finalRadius = (float) Math.hypot(dx, dy);

            SupportAnimator animator = ViewAnimationUtils.createCircularReveal(view, cx, cy, 0, finalRadius);
            animator.setInterpolator(new AccelerateDecelerateInterpolator());
            animator.setDuration(animDuration);

            animator = animator.reverse();

            try {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (animationAction != null)
                            animationAction.onStart();
                        view.setVisibility(View.INVISIBLE);
                    }
                }, animDuration - 100);

                animator.start();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        public interface AnimationAction {
            void onPrepare();

            void onStart();
        }
    }

    public static class Fade {
        private static final String TAG = "Fade";

        public static void fadeIn(Context context, final View view) {
            final Animation fadeIn = AnimationUtils.loadAnimation(context, R.anim.fade_in);
            fadeIn.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    Log.d(AnimationSupport.TAG, TAG + " -> fadeIn() ->onAnimationStart()");
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    Log.d(AnimationSupport.TAG, TAG + " -> fadeIn() -> onAnimationEnd()");
                    view.setVisibility(View.VISIBLE);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });

            view.startAnimation(fadeIn);
        }

        public static void fadeOut(Context context, final View view) {
            final Animation fadeOut = AnimationUtils.loadAnimation(context, R.anim.fade_out);

            fadeOut.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    Log.d(AnimationSupport.TAG, TAG + " -> fadeOut() -> onAnimationStart()");
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    Log.d(AnimationSupport.TAG, TAG + " -> fadeOut() -> onAnimationEnd()");
                    view.setVisibility(View.INVISIBLE);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });

            view.startAnimation(fadeOut);
        }
    }
}
