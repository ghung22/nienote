package com.lexisnguyen.nienote.components.notes;

import android.widget.Filter;

import com.lexisnguyen.nienote.components.sql.Note;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A filter to search for notes in a list
 *
 * @see <a href="https://stackoverflow.com/a/38099888">SearchView filter with RecyclerView - Stack Overflow</a>
 */
public class NoteFilter extends Filter {
    private final NoteAdapter adapter;
    private final List<Note> notes;

    private final List<Note> filteredNotes = new ArrayList<>();

    public NoteFilter(NoteAdapter adapter, List<Note> notes) {
        this.adapter = adapter;
        this.notes = notes;
    }

    @Override
    protected FilterResults performFiltering(CharSequence constraint) {
        filteredNotes.clear();
        final FilterResults results = new FilterResults();
        String title, text, folder, savedDate, createdDate,
                phrase = constraint.toString().toLowerCase(Locale.ROOT);

        for (final Note note : notes) {
            title = note.title.toLowerCase(Locale.ROOT);
            text = note.text.toLowerCase(Locale.ROOT);
            folder = note.folder.toLowerCase(Locale.ROOT);
            savedDate = note.savedDate.toString();
            if (title.contains(phrase) || text.contains(phrase) ||
                    folder.contains(phrase) || savedDate.contains(phrase)) {
                filteredNotes.add(note);
            }
        }

        results.values = filteredNotes;
        results.count = filteredNotes.size();
        return results;
    }

    @Override
    protected void publishResults(CharSequence constraint, FilterResults results) {
        adapter.notifyDataSetFiltered(filteredNotes);
    }
}