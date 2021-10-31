package com.lexisnguyen.nienote.components.sql;

import com.orm.SugarRecord;

import java.time.Instant;
import java.util.Date;

public class Trash extends SugarRecord {
    // Basic info
    public Date deletedDate = null;

    // Foreign key
    public Note note = null;

    public Trash() {}

    public Trash(Note note) {
        this.note = note;
        deletedDate = Date.from(Instant.now());
    }
}