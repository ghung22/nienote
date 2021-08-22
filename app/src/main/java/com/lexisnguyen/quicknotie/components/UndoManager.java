package com.lexisnguyen.quicknotie.components;

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
            undos = new ArrayList<>(),
            undoItems = new ArrayList<>();
    private final ArrayList<Integer>
            undoDrawables = new ArrayList<>(),
            undoSelectionStart = new ArrayList<>(),
            undoSelectionEnd = new ArrayList<>();

    // Internal data
    private int size;
    private int pos = 0;
    private TextWatcher textWatcher = null;
    private static final int DEFAULT_HISTORY_SIZE = 50;

    public UndoManager(EditText editText, ImageButton undoBtn, ImageButton redoBtn) {
        this(DEFAULT_HISTORY_SIZE, editText, undoBtn, redoBtn);
    }

    public UndoManager(int size, EditText editText, ImageButton undoBtn, ImageButton redoBtn) {
        this.editText = editText;
        this.size = size;
        this.undoBtn = undoBtn;
        this.redoBtn = redoBtn;
        undos.add("Original text");
        undoItems.add(editText.getText().toString());
        undoDrawables.add(R.drawable.action_edit);
        undoSelectionStart.add(editText.getSelectionStart());
        undoSelectionEnd.add(editText.getSelectionEnd());
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

    public void add(String name) {
        add(name, R.drawable.action_edit);
    }

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

    public void undo() {
        jump(pos - 1);
    }

    public void redo() {
        jump(pos + 1);
    }

    public void capPos() {
        // FIXME: java.lang.IllegalArgumentException: fromIndex(6) > toIndex(4) ????
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