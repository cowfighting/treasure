package com.xycoding.treasure.utils;

import android.animation.Animator;
import android.app.Dialog;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.view.animation.LinearOutSlowInInterpolator;
import android.support.v7.app.AlertDialog;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.OvershootInterpolator;

import com.xycoding.treasure.R;

/**
 * Created by xuyang on 2016/8/1.
 */
public class ViewUtils {

    public static Dialog createLoadingDialog(@NonNull Context context) {
        return createLoadingDialog(context, true);
    }

    public static Dialog createLoadingDialog(@NonNull Context context, boolean cancelable) {
        return new AlertDialog
                .Builder(context, R.style.Dialog_NoTitleAndTransparent)
                .setView(R.layout.layout_loading)
                .setCancelable(cancelable)
                .create();
    }

    public static Dialog createSheetDialog(@NonNull Context context, @NonNull View contentView) {
        Dialog dialog = new AlertDialog.Builder(context, R.style.Dialog_NoTitleAndTransparent_Sheet)
                .setView(contentView)
                .create();
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        Window window = dialog.getWindow();
        if (window != null) {
            layoutParams.copyFrom(window.getAttributes());
        }
        layoutParams.gravity = Gravity.BOTTOM;
        layoutParams.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE;
        layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        dialog.getWindow().setAttributes(layoutParams);
        return dialog;
    }

    public static Dialog createEventDialog(@NonNull Context context, @NonNull View contentView) {
        Dialog dialog = new AlertDialog.Builder(context, R.style.Dialog_NoTitleAndTransparent_Sheet_NotFloating)
                .setView(contentView)
                .create();
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        Window window = dialog.getWindow();
        if (window != null) {
            //不拦截事件
            window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
            //透明
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            layoutParams.copyFrom(window.getAttributes());
        }
        layoutParams.gravity = Gravity.BOTTOM;
        layoutParams.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE;
        layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        dialog.getWindow().setAttributes(layoutParams);
        return dialog;
    }

    public static void zoomInView(@NonNull final View view) {
        view.setVisibility(View.VISIBLE);
        view.setAlpha(0f);
        view.setScaleX(0f);
        view.setScaleY(0f);
        view.animate()
                .alpha(1)
                .scaleX(1)
                .scaleY(1)
                .setDuration(300)
                .setInterpolator(new OvershootInterpolator())
                .setListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        view.animate()
                                .setInterpolator(new LinearOutSlowInInterpolator())
                                .start();
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {

                    }
                })
                .start();
    }

    public static void zoomOutView(@NonNull final View view) {
        view.animate()
                .alpha(0)
                .scaleX(0)
                .scaleY(0)
                .setDuration(300)
                .setInterpolator(new LinearOutSlowInInterpolator())
                .setListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        view.setVisibility(View.GONE);
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        view.setVisibility(View.GONE);
                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {

                    }
                })
                .start();
    }

}
