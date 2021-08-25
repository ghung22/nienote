package com.lexisnguyen.quicknotie.components.undo;

import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.annotation.DrawableRes;

import com.lexisnguyen.quicknotie.R;

import java.util.ArrayList;

/**
 * Handles undo and redo for an EditText
 */
@SuppressWarnings("FieldCanBeLocal")
public class UndoManager {
    // External data
    private EditText editText;
    private ImageButton undoBtn, redoBtn;
    private final ArrayList<String>
            undos = new ArrayList<>(), // Undo step names
            undoItems = new ArrayList<>(); // Versions of EditText
    private final ArrayList<Integer>
            undoDrawables = new ArrayList<>(), // Icons of each step
            undoSelectionStart = new ArrayList<>(), // textSelectionStart in each step
            undoSelectionEnd = new ArrayList<>(); // textSelectionEnd in each step

    // Internal data
    private int size;
    private int pos = 0; // Current active step
    private TextWatcher textWatcher = null;
    private static final int DEFAULT_HISTORY_SIZE = 50;

    /**
     * Init an UndoManager object
     *
     * @param size     Max size of undo history
     * @param editText The EditText associated with this UndoManager
     * @param undoBtn  The Undo button associated with this UndoManager
     * @param redoBtn  The Redo button associated with this UndoManager
     */
    public UndoManager(int size, EditText editText, ImageButton undoBtn, ImageButton redoBtn) {
        this.editText = editText;
        this.size = size;
        this.undoBtn = undoBtn;
        this.redoBtn = redoBtn;

        // Add an "Original text" step which saves the note state before any editing
        undos.add("Original text");
        undoItems.add(editText.getText().toString());
        undoDrawables.add(R.drawable.action_edit);
        undoSelectionStart.add(editText.getSelectionStart());
        undoSelectionEnd.add(editText.getSelectionEnd());
    }

    /**
     * Init an UndoManager object, with the default size as {@link UndoManager#DEFAULT_HISTORY_SIZE}
     *
     * @param editText The EditText associated with this UndoManager
     * @param undoBtn  The Undo button associated with this UndoManager
     * @param redoBtn  The Redo button associated with this UndoManager
     */
    public UndoManager(EditText editText, ImageButton undoBtn, ImageButton redoBtn) {
        this(DEFAULT_HISTORY_SIZE, editText, undoBtn, redoBtn);
    }

    // region Getters & Setters

    public EditText getEditText() {
        return editText;
    }

    public void setEditText(EditText editText) {
        this.editText = editText;
    }

    public ImageButton getUndoBtn() {
        return undoBtn;
    }

    public void setUndoBtn(ImageButton undoBtn) {
        this.undoBtn = undoBtn;
    }

    public ImageButton getRedoBtn() {
        return redoBtn;
    }

    public void setRedoBtn(ImageButton redoBtn) {
        this.redoBtn = redoBtn;
    }

    public ArrayList<String> getUndos() {
        return undos;
    }

    public ArrayList<String> getUndoItems() {
        return undoItems;
    }

    public ArrayList<Integer> getUndoDrawables() {
        return undoDrawables;
    }

    public ArrayList<Integer> getUndoSelectionStart() {
        return undoSelectionStart;
    }

    public ArrayList<Integer> getUndoSelectionEnd() {
        return undoSelectionEnd;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getPos() {
        return pos;
    }

    public TextWatcher getTextWatcher() {
        return textWatcher;
    }

    public void setTextWatcher(TextWatcher textWatcher) {
        this.textWatcher = textWatcher;
    }

    // endregion

    /**
     * Add a step to undo history
     * <ul>
     *   <li>Cut off any steps behind this current step</li>
     *   <li>Save data of the new step</li>
     *   <li>Set current step to the new one</li>
     *   <li>Cut off a part of the head so that the list will not exceed the max size</li>
     *   <li>Update Undo and Redo button</li>
     * </ul>
     *
     * @param name       Name of the new undo step
     * @param drawableId Icon of the new undo step
     */
    public void add(String name, @DrawableRes int drawableId) {
        capPos();
        undos.add(name);
        undoItems.add(editText.getText().toString());
        undoDrawables.add(drawableId);
        undoSelectionStart.add(editText.getSelectionStart());
        undoSelectionEnd.add(editText.getSelectionEnd());
        pos = undos.size() - 1;
        capSize();
        updateBtn();
    }

    /**
     * Add a step to undo history, with the default icon as
     * {@link com.lexisnguyen.quicknotie.R.drawable#action_edit action_edit}
     *
     * @param name Name of the new undo step
     */
    public void add(String name) {
        add(name, R.drawable.action_edit);
    }

    /**
     * Clear undo history, leaving only the <i>Original text</i> step
     */
    public void clear() {
        undos.removeAll(
                undos.subList(1, undos.size()));
        undoItems.removeAll(
                undoItems.subList(1, undos.size()));
        undoDrawables.removeAll(
                undoDrawables.subList(1, undos.size()));
        undoSelectionStart.removeAll(
                undoSelectionStart.subList(1, undos.size()));
        undoSelectionEnd.removeAll(
                undoSelectionEnd.subList(1, undos.size()));
        pos = 0;
        updateBtn();
    }

    /**
     * Jump to a step in undo history
     *
     * @param pos The jump position
     */
    public void jump(int pos) {
        if (pos < 0 || pos >= undos.size()) {
            return;
        }
        this.pos = pos;
        String undoItem = undoItems.get(pos);
        if (textWatcher != null) {
            editText.removeTextChangedListener(textWatcher);
        }
        editText.setText(undoItem);
        editText.setSelection(undoSelectionStart.get(pos), undoSelectionEnd.get(pos));
        if (textWatcher != null) {
            editText.addTextChangedListener(textWatcher);
        }
        updateBtn();
    }

    /**
     * Jump to the previous step in undo history
     */
    public void undo() {
        jump(pos - 1);
    }

    /**
     * Jump to the next step in undo history
     */
    public void redo() {
        jump(pos + 1);
    }

    /**
     * Cut off any steps behind this current step
     */
    public void capPos() {
        if (pos + 1 < undos.size()) {
            undos.removeAll(
                    undos.subList(pos + 1, undos.size()));
            undoItems.removeAll(
                    undoItems.subList(pos + 1, undos.size()));
            undoDrawables.removeAll(
                    undoDrawables.subList(pos + 1, undos.size()));
            undoSelectionStart.removeAll(
                    undoSelectionStart.subList(pos + 1, undos.size()));
            undoSelectionEnd.removeAll(
                    undoSelectionEnd.subList(pos + 1, undos.size()));
        }
    }

    /**
     * Cut off a part of the head so that the list will not exceed the max size,
     * with the <i>Original text</i> step at the start
     */
    public void capSize() {
        if (undos.size() > size) {
            undos.removeAll(
                    undos.subList(1, undos.size() - size + 1));
            undoItems.removeAll(
                    undoItems.subList(1, undos.size() - size + 1));
            undoDrawables.removeAll(
                    undoDrawables.subList(1, undos.size() - size + 1));
            undoSelectionStart.removeAll(
                    undoSelectionStart.subList(1, undos.size() - size + 1));
            undoSelectionEnd.removeAll(
                    undoSelectionEnd.subList(1, undos.size() - size + 1));
        }
    }

    /**
     * Update the Undo and Redo button
     * <ul>
     *   <li>If there is any step before the current step, enable the Undo button</li>
     *   <li>If there is any step after the current step, enable the Redo button</li>
     * </ul>
     */
    public void updateBtn() {
        if (pos > 0) {
            undoBtn.setEnabled(true);
            undoBtn.setAlpha(1f);
        } else {
            undoBtn.setEnabled(false);
            undoBtn.setAlpha(.5f);
        }
        if (pos + 1 < undos.size()) {
            redoBtn.setEnabled(true);
            redoBtn.setAlpha(1f);
        } else {
            redoBtn.setEnabled(false);
            redoBtn.setAlpha(.5f);
        }
    }
}