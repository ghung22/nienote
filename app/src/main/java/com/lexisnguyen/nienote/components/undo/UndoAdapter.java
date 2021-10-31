package com.lexisnguyen.nienote.components.undo;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.lexisnguyen.nienote.R;

import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class UndoAdapter extends RecyclerView.Adapter<UndoAdapter.ViewHolder> {
    private final Context context;
    private final BottomSheetDialog dialog;
    private final UndoManager undoManager;
    private final String type;

    private final int undoPos;
    private final List<String> undos;
    private final List<Integer> undoDrawables;

    public UndoAdapter(Context context, BottomSheetDialog dialog, UndoManager undoManager, String type) {
        this.context = context;
        this.dialog = dialog;
        this.undoManager = undoManager;
        this.type = type;

        undoPos = undoManager.getPos();
        List<String> _undos = undoManager.getUndos();
        List<Integer> _undoDrawables = undoManager.getUndoDrawables();
        if (type.equals("undo")) {
            undos = _undos.subList(0, undoPos);
            undoDrawables = _undoDrawables.subList(0, undoPos);
        } else {
            undos = _undos.subList(undoPos + 1, _undos.size());
            undoDrawables = _undoDrawables.subList(undoPos + 1, _undoDrawables.size());
        }
    }

    // Create new views (invoked by the layout manager)
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Create a new view, which defines the UI of the list item
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.layout_undo_redo_item, parent, false);
        return new ViewHolder(view);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final int pos = position + ((type.equals("redo")) ? undoPos + 1 : 0);
        Button action_undo_redo = holder.getButton();
        int drawable = undoDrawables.get(position);
        String undo = StringUtils.capitalize(type) + " " + undos.get(position);

        action_undo_redo.setCompoundDrawablesWithIntrinsicBounds(
                AppCompatResources.getDrawable(context, drawable), null, null, null);
        action_undo_redo.setText(undo);
        action_undo_redo.setOnClickListener(view -> {
            undoManager.jump(pos);
            dialog.dismiss();
        });
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return undos.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final Button action_undo_redo;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // Define ViewHolder's View
            action_undo_redo = itemView.findViewById(R.id.action_undo_redo);
        }

        public Button getButton() {
            return action_undo_redo;
        }
    }
}