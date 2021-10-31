package com.lexisnguyen.nienote.components.sql;

import androidx.annotation.ColorRes;

import com.lexisnguyen.nienote.R;
import com.orm.SugarRecord;
import com.orm.dsl.Ignore;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Objects;

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

    // Ignored info
    @Ignore
    public boolean isChecked = false;

    public Note() {}

    public Note(String folder, String name, String text, @ColorRes int bgColor) {
        this.folder = folder;
        this.title = name;
        this.text = text;
        this.bgColor = bgColor;
        savedDate = Date.from(Instant.now());
    }

    public Note(String folder, String name, String text, @ColorRes int bgColor, Date savedDate) {
        this.folder = folder;
        this.title = name;
        this.text = text;
        this.bgColor = bgColor;
        this.savedDate = savedDate;
    }

    public boolean delete(boolean permanently) {
        if (!permanently) {
            Trash trash = new Trash(this);
            trash.save();
        } else {
            List<Trash> trashes = Trash.find(Trash.class,
                    "note = ?", String.valueOf(getId()));
            Trash trash;
            if (trashes.size() > 0) {
                trash = trashes.get(0);
                trash.delete();
            }
            return delete();
        }
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Note note = (Note) o;
        return bgColor == note.bgColor && folder.equals(note.folder) && title.equals(note.title) && text.equals(note.text) && savedDate.equals(note.savedDate) && Objects.equals(password, note.password);
    }

    @Override
    public int hashCode() {
        return Objects.hash(folder, title, text, bgColor, savedDate, password);
    }
}