package com.lexisnguyen.quicknotie.components.markdown.tags;

import android.content.Context;
import android.text.style.ForegroundColorSpan;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.lexisnguyen.quicknotie.R;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import io.noties.markwon.MarkwonConfiguration;
import io.noties.markwon.RenderProps;
import io.noties.markwon.html.HtmlTag;
import io.noties.markwon.html.tag.SimpleTagHandler;

public class ColorTagHandler extends SimpleTagHandler {
    private final Context context;

    public ColorTagHandler(Context context) {
        this.context = context;
    }

    @Nullable
    @Override
    public Object getSpans(
            @NonNull MarkwonConfiguration configuration,
            @NonNull RenderProps renderProps,
            @NonNull HtmlTag tag) {
        @ColorInt int color = 0;
        String key;
        try {
            key = (String) tag.attributes().keySet().toArray()[0];
        } catch (ArrayIndexOutOfBoundsException e) {
            return null;
        }

        switch (key) {
            case "red":
                color = context.getColor(R.color.red);
                break;
            case "orange":
                color = context.getColor(R.color.orange);
                break;
            case "yellow":
                color = context.getColor(R.color.yellow);
                break;
            case "green":
                color = context.getColor(R.color.green);
                break;
            case "tortoise":
                color = context.getColor(R.color.tortoise);
                break;
            case "cyan":
                color = context.getColor(R.color.cyan);
                break;
            case "blue":
                color = context.getColor(R.color.blue);
                break;
            case "violet":
                color = context.getColor(R.color.violet);
                break;
            case "purple":
                color = context.getColor(R.color.purple);
                break;
            case "magenta":
                color = context.getColor(R.color.magenta);
                break;
            case "pink":
                color = context.getColor(R.color.pink);
                break;
            case "black":
                color = context.getColor(R.color.black);
                break;
            case "lightgray":
                color = context.getColor(R.color.lightgray);
                break;
            case "light":
                color = context.getColor(R.color.light);
                break;
            case "white":
                color = context.getColor(R.color.white);
                break;
        }

        ArrayList<Object> spans = new ArrayList<>();
        if (color != 0) {
            spans.add(new ForegroundColorSpan(color));
        }
        return spans.toArray();
    }

    @NonNull
    @NotNull
    @Override
    public Collection<String> supportedTags() {
        return Collections.singleton("color");
    }
}