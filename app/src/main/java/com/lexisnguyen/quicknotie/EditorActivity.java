package com.lexisnguyen.quicknotie;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class EditorActivity extends AppCompatActivity {
    //GUI Elements
    EditText editTextTitle, editText;
    ImageButton action_add_content, action_format_style,
            action_format_color, action_format_background,
            action_undo, action_redo;

    // Data
    private String title = "";
    private final String text = "";
    private boolean readonly = false;

    // Debugging
    private final String TAG = "EditorActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        // INIT GUI ELEMENTS
        // - Top Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        editTextTitle = findViewById(R.id.editTextTitle);
        // - Bottom Appbar
        action_add_content = findViewById(R.id.action_add_content);
        action_format_style = findViewById(R.id.action_format_style);
        action_format_color = findViewById(R.id.action_format_color);
        action_format_background = findViewById(R.id.action_format_background);
        action_undo = findViewById(R.id.action_undo);
        action_redo = findViewById(R.id.action_redo);
        // - Content Layout
        editText = findViewById(R.id.editText);

        /* INIT TOP TOOLBAR
         * [BACK] [EDITTEXT] [READONLY] [REMIND] [SHARE] [OVERFLOW]
         * - BACK:      Return to MainActivity
         * - EDITTEXT:  Show & set note title
         * - READONLY:  Toggle readonly mode
         * - REMIND:    Set a reminder for this note
         * - SHARE:     Share this note
         * - OVERFLOW:  Show more options
         */
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show back button
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowTitleEnabled(false);
        }
        editText.setOnFocusChangeListener(this::onFocusChange);
        toolbar.setOnMenuItemClickListener(this::OnMenuItemClick);

        /* INIT BOTTOM APPBAR
         * [ADD] [STYLE] [COLOR] [BG] [UNDO] [REDO]
         * - ADD:   Add an image/drawing
         * - STYLE: Set line as a bullet/numbered list
         *          Align left/center/right text
         *          Set bold/underline/italic/strikethrough
         *          Increase/decrease indent
         *          Set text size (heading 1, heading 2, heading 3)
         * - COLOR: Set text color
         * - BG:    Set text background color
         * - UNDO:  Undo last change (hold for history list)
         * - REDO:  Redo last change (hold for history list)
         */
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_editor_top, menu);
        return super.onCreateOptionsMenu(menu);
    }

    private void onFocusChange(View view, boolean onFocus) {
        if (!onFocus) {
            // Save title
            title = editTextTitle.getText().toString();
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    private boolean OnMenuItemClick(MenuItem menuItem) {
        int id = menuItem.getItemId();
        if (id == R.id.home) {
            // Trigger back key press
            onBackPressed();
        } else if (id == R.id.action_readonly) {
            readonly = !readonly;
            // Disable interacting with EditText fields
            editTextTitle.setFocusable(readonly);
            editText.setFocusable(readonly);
            // Disable editor buttons
            action_add_content.setEnabled(readonly);
            action_format_style.setEnabled(readonly);
            action_format_color.setEnabled(readonly);
            action_format_background.setEnabled(readonly);
            action_undo.setEnabled(readonly);
            action_redo.setEnabled(readonly);
        } else if (id == R.id.action_remind) {
            // Use Intent to add a new event to calendar
            // TODO: https://stackoverflow.com/a/36947690
        } else if (id == R.id.action_share) {
            // Save a temporary pdf file and share to external app
        } else if (id == R.id.action_export) {
            // Save markdown to file using library
            // TODO: https://github.com/Qkyrie/Markdown2Pdf
        } else if (id == R.id.action_lock) {

        } else if (id == R.id.action_delete) {

        } else {
            Log.w(TAG, "OnMenuItemClick: Unknown menu item " + menuItem.getTitle());
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}