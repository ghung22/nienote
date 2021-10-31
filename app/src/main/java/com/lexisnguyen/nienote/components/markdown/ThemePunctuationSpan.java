package com.lexisnguyen.nienote.components.markdown;

import android.text.style.ForegroundColorSpan;

import androidx.annotation.ColorInt;

public class ThemePunctuationSpan extends ForegroundColorSpan {
    public ThemePunctuationSpan(@ColorInt int bgColorInt) {
        super(bgColorInt);
    }
}