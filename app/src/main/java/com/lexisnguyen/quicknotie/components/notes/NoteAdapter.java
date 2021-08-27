package com.lexisnguyen.quicknotie.components.notes;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityOptionsCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.lexisnguyen.quicknotie.R;
import com.lexisnguyen.quicknotie.activities.EditorActivity;
import com.lexisnguyen.quicknotie.components.sql.Note;

import java.util.List;

import static com.lexisnguyen.quicknotie.activities.EditorActivity.initMarkdown;
import static com.lexisnguyen.quicknotie.activities.EditorActivity.markwon;
import static com.lexisnguyen.quicknotie.activities.MainActivity.ACTION_OPEN_NOTE;

public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.ViewHolder> {
    private final Context context;
    private final List<Note> notes;

    public NoteAdapter(Context context, List<Note> notes) {
        this.context = context;
        this.notes = notes;
    }

    @NonNull
    @Override
    public NoteAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Create a new view, which defines the UI of the list item
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.layout_note_item, parent, false);
        return new NoteAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteAdapter.ViewHolder holder, int position) {
        Note note = notes.get(position);
        MaterialCardView itemView = holder.itemView;
        TextView textViewTitle = holder.textViewTitle,
                textView = holder.textView;

        // Set content
        textViewTitle.setText(note.title);
        initMarkdown(context, note.bgColor);
        markwon.setMarkdown(textView, note.text);
        textView.setMovementMethod(null);
        itemView.setCardBackgroundColor(context.getColor(note.bgColor));
        itemView.setOnClickListener((view) -> openNote(view, note, position));
    }

    @Override
    public int getItemCount() {
        return notes.size();
    }

    public void notifyDataSetChanged(List<Note> notes) {
        this.notes.clear();
        this.notes.addAll(notes);
        notifyDataSetChanged();
    }

    private void openNote(View view, Note note, long id) {
        ActivityOptionsCompat options = ActivityOptionsCompat
                .makeSceneTransitionAnimation((Activity) context, view, context.getString(R.string.transition_open_note));
        Intent intent = new Intent(context, EditorActivity.class);
        Bundle bundle = new Bundle();
        bundle.putInt("action", ACTION_OPEN_NOTE);
        bundle.putString("folder", note.folder);
        bundle.putLong("noteId", id);
        intent.putExtras(bundle);
        context.startActivity(intent, options.toBundle());
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final MaterialCardView itemView;
        public final TextView textViewTitle, textView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // Define ViewHolder's View
            this.itemView = itemView.findViewById(R.id.materialCardView);
            textViewTitle = itemView.findViewById(R.id.textViewTitle);
            textView = itemView.findViewById(R.id.textView);
        }
    }
}