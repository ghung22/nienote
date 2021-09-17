package com.lexisnguyen.quicknotie.components.notes;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityOptionsCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.lexisnguyen.quicknotie.R;
import com.lexisnguyen.quicknotie.activities.EditorActivity;
import com.lexisnguyen.quicknotie.activities.MainActivity;
import com.lexisnguyen.quicknotie.components.sql.Note;
import com.lexisnguyen.quicknotie.components.sql.Trash;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import static com.lexisnguyen.quicknotie.activities.EditorActivity.initMarkdown;
import static com.lexisnguyen.quicknotie.activities.EditorActivity.isDarkMode;
import static com.lexisnguyen.quicknotie.activities.EditorActivity.markwon;
import static com.lexisnguyen.quicknotie.activities.MainActivity.ACTION_OPEN_NOTE;

@SuppressWarnings("FieldCanBeLocal")
public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.ViewHolder> {
    // Passed data
    private final Context context;
    private final List<Note> notes;
    private final ActivityResultLauncher<Intent> editorLauncher;
    private final RecyclerView recyclerView;

    // Internal data
    private NoteFilter filter;
    private int selectCount = 0;

    // Animation
    private final int quickAni = 150;
    private final int normalAni = 300;

    private final String TAG = "NoteAdapter";

    public NoteAdapter(Context context, List<Note> notes,
                       ActivityResultLauncher<Intent> editorLauncher, RecyclerView recyclerView) {
        this.context = context;
        this.notes = notes;
        this.editorLauncher = editorLauncher;
        this.recyclerView = recyclerView;
        filter = new NoteFilter(this, notes);
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
                textView = holder.textView,
                textViewSavedTime = holder.textViewSavedTime;

        // Set content
        if (note.title.isEmpty()) {
            textViewTitle.setVisibility(View.GONE);
        } else {
            textViewTitle.setVisibility(View.VISIBLE);
        }
        textViewTitle.setText(note.title);
        initMarkdown(context, note.bgColor);
        markwon.setMarkdown(textView, note.text);
        textView.setMovementMethod(null);
        SimpleDateFormat format = new SimpleDateFormat("KK:mm a, dd-MM-yyy", Locale.ROOT);
        String savedDate = format.format(notes.get(position).savedDate);
        textViewSavedTime.setText(savedDate);
        itemView.setCardBackgroundColor(context.getColor(note.bgColor));

        // Update text color based on bgColor
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(R.attr.colorOnSecondary, typedValue, true);
        @ColorRes int color = typedValue.resourceId;
        if (isDarkMode(note.bgColor)) {
            textViewTitle.setTextColor(context.getColor(R.color.white));
            textView.setTextColor(context.getColor(R.color.white));
            textViewSavedTime.setTextColor(context.getColor(R.color.faded_white));
            itemView.setCheckedIconTint(context.getColorStateList(R.color.white));
        } else {
            textViewTitle.setTextColor(context.getColor(color));
            textView.setTextColor(context.getColor(color));
            textViewSavedTime.setTextColor(context.getColor(R.color.faded_black));
            itemView.setCheckedIconTint(context.getColorStateList(color));
        }

        // Set input events
        itemView.setOnClickListener((view) -> {
            if (selectCount == 0) {
                openNote(view, note);
            } else {
                select(view, note);
            }
        });
        itemView.setOnLongClickListener((view) -> select(view, note));
    }

    @Override
    public int getItemCount() {
        return notes.size();
    }

    /**
     * Search a note containing a phrase
     *
     * @param phrase The phrase in question
     */
    public void search(String phrase) {
        filter.filter(phrase);
    }

    /**
     * Open a note in {@link EditorActivity}
     *
     * @param view CardView of the note
     * @param note The note in question
     */
    private void openNote(View view, Note note) {
        ActivityOptionsCompat options = ActivityOptionsCompat
                .makeSceneTransitionAnimation((Activity) context, view, context.getString(R.string.transition_open_note));
        Intent intent = new Intent(context, EditorActivity.class);
        Bundle bundle = new Bundle();
        bundle.putInt("action", ACTION_OPEN_NOTE);
        bundle.putString("folder", note.folder);
        bundle.putLong("noteId", note.getId());
        intent.putExtras(bundle);
        editorLauncher.launch(intent, options);
    }

    /**
     * Select a note in the list
     *
     * @param view CardView of the note
     * @param note The note in question
     * @return True
     */
    private boolean select(View view, Note note) {
        // Set data
        note.isChecked = !note.isChecked;
        ((MaterialCardView) view).setChecked(note.isChecked);
        selectCount += note.isChecked ? 1 : -1;
        selectCount = Integer.max(0, selectCount);
        ((MainActivity) context).selectCount.postValue(selectCount);

        // Update UI
        if (note.isChecked) {
            view.animate().scaleX(.95f).scaleY(.95f).rotationBy(5).setDuration(quickAni)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                        }
                    });
        } else {
            view.animate().scaleX(1).scaleY(1).rotationBy(-5).setDuration(quickAni)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                        }
                    });
        }
        return true;
    }

    /**
     * Select/Deselect everything in note list
     */
    public void selectAll() {
        boolean deselectAll = (selectCount == notes.size());
        for (Note note : notes) {
            if (deselectAll || !note.isChecked) {
                ViewHolder holder = (ViewHolder)
                        recyclerView.findViewHolderForAdapterPosition(notes.indexOf(note));
                if (holder != null) {
                    select(holder.itemView, note);
                }
            }
        }
    }

    /**
     * Deselect everything in note list
     */
    public void clearSelection() {
        for (Note note : notes) {
            if (note.isChecked) {
                ViewHolder holder = (ViewHolder)
                        recyclerView.findViewHolderForAdapterPosition(notes.indexOf(note));
                if (holder != null) {
                    select(holder.itemView, note);
                }
            }
        }
    }

    /**
     * Delete selected notes
     *
     * @param permanently Whether delete it permanently or move it into the trash
     */
    public void delete(boolean permanently) {
        /* FIXME:
        E/AndroidRuntime: FATAL EXCEPTION: main
            Process: com.lexisnguyen.quicknotie, PID: 18652
            java.util.ConcurrentModificationException
            at java.util.ArrayList$Itr.next(ArrayList.java:860)
            at com.lexisnguyen.quicknotie.components.notes.NoteAdapter.delete(NoteAdapter.java:230)
            at com.lexisnguyen.quicknotie.activities.MainActivity.action_delete(MainActivity.java:861)
            at com.lexisnguyen.quicknotie.activities.MainActivity.onMenuItemClick(MainActivity.java:598)
            at com.lexisnguyen.quicknotie.activities.MainActivity.lambda$PaItOa5DqNwajJ0QTZhXAR7cHAE(Unknown Source:0)
            at com.lexisnguyen.quicknotie.activities.-$$Lambda$MainActivity$PaItOa5DqNwajJ0QTZhXAR7cHAE.onMenuItemClick(Unknown Source:2)
            at androidx.appcompat.widget.Toolbar$1.onMenuItemClick(Toolbar.java:208)
            at androidx.appcompat.widget.ActionMenuView$MenuBuilderCallback.onMenuItemSelected(ActionMenuView.java:780)
            at androidx.appcompat.view.menu.MenuBuilder.dispatchMenuItemSelected(MenuBuilder.java:834)
            at androidx.appcompat.view.menu.MenuItemImpl.invoke(MenuItemImpl.java:158)
            at androidx.appcompat.view.menu.MenuBuilder.performItemAction(MenuBuilder.java:985)
            at androidx.appcompat.view.menu.MenuBuilder.performItemAction(MenuBuilder.java:975)
            at androidx.appcompat.widget.ActionMenuView.invokeItem(ActionMenuView.java:624)
            at androidx.appcompat.view.menu.ActionMenuItemView.onClick(ActionMenuItemView.java:151)
            at android.view.View.performClick(View.java:7125)
            at android.view.View.performClickInternal(View.java:7102)
            at android.view.View.access$3500(View.java:801)
            at android.view.View$PerformClick.run(View.java:27336)
            at android.os.Handler.handleCallback(Handler.java:883)
            at android.os.Handler.dispatchMessage(Handler.java:100)
            at android.os.Looper.loop(Looper.java:214)
            at android.app.ActivityThread.main(ActivityThread.java:7356)
            at java.lang.reflect.Method.invoke(Native Method)
            at com.android.internal.os.RuntimeInit$MethodAndArgsCaller.run(RuntimeInit.java:492)
            at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:930)
        I/Process: Sending signal. PID: 18652 SIG: 9
        */
        for (Note note : notes) {
            if (note.isChecked)
                delete(notes.indexOf(note), permanently);
        }
    }

    private void delete(int noteId, boolean permanently) {
        if (!permanently) {
            Trash trash = new Trash(notes.get(noteId));
            trash.save();
            notifyItemRemove(noteId);
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setMessage("This note will be deleted permanently, do you want to continue?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        List<Trash> trashes = Trash.find(Trash.class,
                                "note = ?", String.valueOf(notes.get(noteId).getId()));
                        Trash trash;
                        if (trashes.size() > 0) {
                            trash = trashes.get(0);
                            trash.delete();
                        }
                        if (notes.get(noteId).delete()) {
                            return;
                        }
                        Log.e(TAG, "action_delete: Delete note permanently failed");
                    })
                    .setNegativeButton("No", null)
                    .setCancelable(true)
                    .show();
        }
    }

    // region Update view events

    public void notifyDataSetChanged(List<Note> notes, boolean updateFilter) {
        this.notes.clear();
        this.notes.addAll(notes);
        if (updateFilter) {
            filter = new NoteFilter(this, notes);
        }
        notifyDataSetChanged();
    }

    public void notifyDataSetChanged(List<Note> notes) {
        notifyDataSetChanged(notes, true);
    }

    public void notifyDataSetFiltered(List<Note> notes) {
        notifyDataSetChanged(notes, false);
    }

    public void notifyItemChange(int position, String title, String text, int bgColor) {
        if (position < 0 || position >= notes.size()) {
            Log.w(TAG, "notifyItemChanged: Invalid array position " + position);
            return;
        }
        notes.get(position).title = title;
        notes.get(position).text = text;
        notes.get(position).bgColor = bgColor;
        filter = new NoteFilter(this, notes);
        notifyItemChanged(position);
    }

    public void notifyItemInsert(long noteId) {
        Note note = Note.findById(Note.class, noteId);
        notes.add(0, note);
        notifyItemInserted(0);
        filter = new NoteFilter(this, notes);
    }

    public void notifyItemRemove(int position) {
        if (position < 0 || position >= notes.size()) {
            Log.w(TAG, "notifyItemRemoved: Invalid array position " + position);
            return;
        }
        notes.remove(position);
        notifyItemRemoved(position);
        filter = new NoteFilter(this, notes);
    }

    // endregion

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final MaterialCardView itemView;
        public final TextView textViewTitle, textView, textViewSavedTime;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // Define ViewHolder's View
            this.itemView = itemView.findViewById(R.id.materialCardView);
            textViewTitle = itemView.findViewById(R.id.textViewTitle);
            textView = itemView.findViewById(R.id.textView);
            textViewSavedTime = itemView.findViewById(R.id.textViewSavedTime);
        }
    }
}