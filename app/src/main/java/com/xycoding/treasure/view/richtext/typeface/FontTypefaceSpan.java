package com.xycoding.treasure.view.richtext.typeface;

import android.graphics.Paint;
import android.graphics.Typeface;
import android.renderscript.Matrix2f;
import android.support.annotation.NonNull;
import android.text.TextPaint;
import android.text.style.CharacterStyle;
import android.text.style.TypefaceSpan;

/**
 * Created by xuyang on 2017/4/28.
 */
public class FontTypefaceSpan extends TypefaceSpan implements IStyleSpan {

    private final String mFamily;
    private final Typeface mTypeface;

    public FontTypefaceSpan(String family, @NonNull Typeface typeface) {
        super(family);
        mFamily = family;
        mTypeface = typeface;
    }

    @Override
    public void updateDrawState(TextPaint ds) {
        apply(ds, mTypeface);
    }

    @Override
    public void updateMeasureState(TextPaint paint) {
        apply(paint, mTypeface);
    }

    private static void apply(Paint paint, Typeface typeface) {
        int oldStyle;

        Typeface old = paint.getTypeface();
        if (old == null) {
            oldStyle = 0;
        } else {
            oldStyle = old.getStyle();
        }
        Typeface tf = Typeface.create(typeface, oldStyle);
        int fake = oldStyle & ~tf.getStyle();

        if ((fake & Typeface.BOLD) != 0) {
            paint.setFakeBoldText(true);
        }

        if ((fake & Typeface.ITALIC) != 0) {
            paint.setTextSkewX(-0.25f);
        }

        paint.setTypeface(tf);
    }

    @Override
    public CharacterStyle getStyleSpan() {
        return new FontTypefaceSpan(mFamily, mTypeface);
    }

}
