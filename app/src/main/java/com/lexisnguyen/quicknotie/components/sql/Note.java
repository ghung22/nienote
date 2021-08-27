package com.lexisnguyen.quicknotie.components.sql;

import androidx.annotation.ColorRes;

import com.lexisnguyen.quicknotie.R;
import com.orm.SugarRecord;

import java.time.Instant;
import java.util.Date;

public class Note extends SugarRecord {
    // Basic info
    public String folder = "/";
    public String title = "";
    public String text = "";
    @ColorRes
    public int bgColor = R.color.white;
    public Date savedDate = null;

    // Private info
    String password = "";

    public Note() {}

    public Note(String folder, String name, String text, @ColorRes int bgColor) {
        this.folder = folder;
        this.title = name;
        this.text = text;
        this.bgColor = bgColor;
        savedDate = Date.from(Instant.now());
    }
}