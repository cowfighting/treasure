package com.xycoding.treasure.view.richtext.style;

import android.text.SpannableStringBuilder;

import com.xycoding.treasure.view.richtext.TagBlock;
import com.xycoding.treasure.view.richtext.typeface.IStyleSpan;

import java.util.Arrays;
import java.util.List;

public abstract class BaseTagStyle {

    protected List<String> mTags;
    protected IStyleSpan mStyleSpan;

    public BaseTagStyle(IStyleSpan span, String... tags) {
        mStyleSpan = span;
        mTags = Arrays.asList(tags);
    }

    abstract public void start(TagBlock block, SpannableStringBuilder builder);

    abstract public void end(TagBlock block, SpannableStringBuilder builder);

    abstract public boolean match(String tagName);

}
