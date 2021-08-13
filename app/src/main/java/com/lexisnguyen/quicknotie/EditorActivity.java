package com.lexisnguyen.quicknotie;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import de.stocard.markdown_to_spanned.Markdown;

public class EditorActivity extends AppCompatActivity {
    //GUI Elements
    TextView textView;
    EditText editTextTitle, editText;
    ImageButton action_add_content, action_format_style,
            action_format_color, action_format_background,
            action_undo, action_redo;

    // Data
    private String title = "";
    private final String text = "";
    private boolean preview = false;

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
        action_add_content.setOnClickListener(this::onClick);
        action_format_style = findViewById(R.id.action_format_style);
        action_format_style.setOnClickListener(this::onClick);
        action_format_color = findViewById(R.id.action_format_color);
        action_format_color.setOnClickListener(this::onClick);
        action_format_background = findViewById(R.id.action_format_background);
        action_format_background.setOnClickListener(this::onClick);
        action_undo = findViewById(R.id.action_undo);
        action_undo.setEnabled(false);
        action_undo.setOnClickListener(this::onClick);
        action_redo = findViewById(R.id.action_redo);
        action_redo.setEnabled(false);
        action_redo.setOnClickListener(this::onClick);
        // - Content Layout
        textView = findViewById(R.id.textView);
        editText = findViewById(R.id.editText);

        /* INIT TOP TOOLBAR
         * [BACK] [EDITTEXT] [PREVIEW] [REMIND] [SHARE] [OVERFLOW]
         * - BACK:      Return to MainActivity
         * - EDITTEXT:  Show & set note title
         * - PREVIEW:   Toggle preview/edit mode
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

    /**
     * An event when the top toolbar is shown
     *
     * @param menu The toolbar that is being created
     * @return a boolean
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_editor_top, menu);
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * An event when a view's focus changes
     *
     * @param view    The view in question
     * @param onFocus The new focus state (true: in focus, false: out of focus)
     */
    private void onFocusChange(View view, boolean onFocus) {
        if (!onFocus) {
            // Save title
            title = editTextTitle.getText().toString();
        }
    }

    /**
     * An event when a view (in most times, a button) is clicked
     *
     * @param view The view in question
     */
    @SuppressLint("NonConstantResourceId")
    private void onClick(View view) {
        switch (view.getId()) {
            case R.id.action_add_content:
                showBottomDialog(R.layout.layout_add_content);
                break;
            case R.id.action_format_style:
                showBottomDialog(R.layout.layout_format_style);
                break;
            case R.id.action_format_color:
                showBottomDialog(R.layout.layout_format_color);
                break;
            case R.id.action_format_background:
                showBottomDialog(R.layout.layout_format_background);
                break;
            case R.id.action_undo:
                break;
            case R.id.action_redo:
                break;
            default:
                break;
        }
    }

    /**
     * <p>Perform an action based on which toolbar menu item was clicked.
     * <p>The menu items are specified in
     * {@link com.lexisnguyen.quicknotie.R.layout#layout_editor_top_toolbar}
     *
     * @param menuItem The selected menu item
     * @return Result of performed action (should be true)
     */
    @SuppressLint("NonConstantResourceId")
    private boolean OnMenuItemClick(MenuItem menuItem) {
        int id = menuItem.getItemId();
        switch (id) {
            case R.id.home:
                // Trigger back key press
                onBackPressed();
                break;
            case R.id.action_preview:
                action_preview(menuItem);
                break;
            case R.id.action_remind:
                // Use Intent to add a new event to calendar
                // TODO: https://stackoverflow.com/a/36947690
                break;
            case R.id.action_share:
                // Save a temporary pdf file and share to external app
                break;
            case R.id.action_export:
                // Save markdown to file using library
                // TODO: https://github.com/Qkyrie/Markdown2Pdf
                break;
            case R.id.action_lock:

                break;
            case R.id.action_delete:

                break;
            default:
                Log.w(TAG, "OnMenuItemClick: Unknown menu item " + menuItem.getTitle());
                break;
        }
        return true;
    }

    private void showBottomDialog(@LayoutRes int layoutId) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(layoutId);

        dialog.show();
    }

    private void action_preview(MenuItem menuItem) {
        preview = !preview;

        // Parse content of EditText into TextView
        Editable text = editText.getText();
        textView.setText(Markdown.fromMarkdown(text.toString()));

        // Hide keyboard
        // https://stackoverflow.com/a/17789187
        InputMethodManager imm = (InputMethodManager) this.getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE);
        View v = this.getCurrentFocus();
        if (v == null) {
            v = new View(this);
        }
        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);

        // Toggle allow interacting with title EditText
        if (preview) {
            editTextTitle.setInputType(InputType.TYPE_NULL);
            editTextTitle.setTextIsSelectable(true);
        } else {
            editTextTitle.setInputType(InputType.TYPE_CLASS_TEXT);
        }

        // Toggle visibility of content EditText and show a TextView
        if (preview) {
            textView.setVisibility(View.VISIBLE);
            editText.setVisibility(View.INVISIBLE);
        } else {
            textView.setVisibility(View.INVISIBLE);
            editText.setVisibility(View.VISIBLE);
        }

        // Toggle editor buttons
        action_add_content.setEnabled(!preview);
        action_format_style.setEnabled(!preview);
        action_format_color.setEnabled(!preview);
        action_format_background.setEnabled(!preview);
        action_undo.setEnabled(!preview);
        action_redo.setEnabled(!preview);

        // Change icon of action_preview
        if (preview) {
            menuItem.setIcon(R.drawable.action_edit);
            menuItem.setTooltipText(getString(R.string.action_edit));
        } else {
            menuItem.setIcon(R.drawable.action_preview);
            menuItem.setTooltipText(getString(R.string.action_preview));
        }
    }

    /**
     * Start an action when user presses Back depending on the states of the app.
     */
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(
                R.anim.anim_null,
                R.anim.anim_slide_down_ease_out
        );
    }
}