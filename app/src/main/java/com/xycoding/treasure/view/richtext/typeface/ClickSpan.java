package com.xycoding.treasure.view.richtext.typeface;

import android.graphics.Color;
import android.support.annotation.ColorInt;
import android.support.annotation.Nullable;
import android.text.TextPaint;
import android.text.style.CharacterStyle;
import android.text.style.ClickableSpan;
import android.view.View;

/**
 * Created by xuyang on 2017/4/28.
 */
public class ClickSpan extends ClickableSpan implements IStyleSpan {

    int mPressedBackgroundColor;
    int mNormalTextColor;
    int mPressedTextColor;
    CharSequence mPressedText;

    private OnClickListener mClickListener;
    private boolean mPressed;

    public ClickSpan(@ColorInt int normalTextColor,
                     @ColorInt int pressedTextColor,
                     @ColorInt int pressedBackgroundColor,
                     @Nullable OnClickListener listener) {
        mNormalTextColor = normalTextColor;
        mPressedTextColor = pressedTextColor;
        mPressedBackgroundColor = pressedBackgroundColor;
        mClickListener = listener;
    }

    @Override
    public void onClick(View widget) {
        //do nothing.
    }

    public void onClick(float rawX, float rawY) {
        if (mClickListener != null) {
            mClickListener.onClick(mPressedText, rawX, rawY);
        }
    }

    @Override
    public void updateDrawState(TextPaint paint) {
        super.updateDrawState(paint);
        paint.setColor(mPressed ? mPressedTextColor : mNormalTextColor);
        paint.bgColor = mPressed ? mPressedBackgroundColor : Color.TRANSPARENT;
        paint.setUnderlineText(false);
    }

    @Override
    public CharacterStyle getStyleSpan() {
        return new ClickSpan(mNormalTextColor, mPressedTextColor, mPressedBackgroundColor, mClickListener);
    }

    public void setPressed(boolean pressed, CharSequence pressedText) {
        mPressed = pressed;
        mPressedText = pressedText;
    }

    public interface OnClickListener {
        void onClick(CharSequence text, float rawX, float rawY);
    }

}
