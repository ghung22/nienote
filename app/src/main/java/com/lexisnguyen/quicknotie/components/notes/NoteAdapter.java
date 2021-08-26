package com.lexisnguyen.quicknotie.components.notes;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.lexisnguyen.quicknotie.R;
import com.lexisnguyen.quicknotie.components.sql.Note;

import java.util.ArrayList;

import static com.lexisnguyen.quicknotie.activities.EditorActivity.initMarkdown;
import static com.lexisnguyen.quicknotie.activities.EditorActivity.markwon;

public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.ViewHolder> {
    private final Context context;
    private final ArrayList<Note> notes;

    public NoteAdapter(Context context, ArrayList<Note> notes) {
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
        MaterialCardView itemView = holder.getItemView();
        TextView textViewTitle = holder.getTextViewTitle(),
                textView = holder.getTextView();
        textViewTitle.setText(note.title);
        initMarkdown(context, note.bgColor);
        markwon.setMarkdown(textView, note.text);
        itemView.setCardBackgroundColor(context.getColor(note.bgColor));
    }

    @Override
    public int getItemCount() {
        return notes.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView itemView;
        private final TextView textViewTitle, textView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // Define ViewHolder's View
            this.itemView = (MaterialCardView) itemView;
            textViewTitle = itemView.findViewById(R.id.textViewTitle);
            textView = itemView.findViewById(R.id.textView);
        }

        public MaterialCardView getItemView() {
            return itemView;
        }

        public TextView getTextViewTitle() {
            return textViewTitle;
        }

        public TextView getTextView() {
            return textView;
        }
    }
}