package com.lexisnguyen.quicknotie.components.sql;

import androidx.annotation.ColorRes;

import com.orm.SugarRecord;
import com.orm.dsl.Unique;

import java.time.Instant;
import java.util.Date;

public class Note extends SugarRecord {
    // Basic info
    @Unique
    public int id;
    public String folder;
    public String title;
    public String text;
    @ColorRes
    public int bgColor;
    public Date savedDate;

    // Private info
    String password = "";

    public Note() {}

    public Note(int id, String folder, String name, String text, @ColorRes int bgColor) {
        this.id = id;
        this.folder = folder;
        this.title = name;
        this.text = text;
        this.bgColor = bgColor;
        savedDate = Date.from(Instant.now());
    }
}