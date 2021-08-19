package com.lexisnguyen.quicknotie.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.Spanned;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.LayoutRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.color.MaterialColors;
import com.lexisnguyen.quicknotie.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.Executors;

import io.noties.markwon.Markwon;
import io.noties.markwon.SoftBreakAddsNewLinePlugin;
import io.noties.markwon.editor.MarkwonEditor;
import io.noties.markwon.editor.MarkwonEditorTextWatcher;
import io.noties.markwon.ext.tables.TablePlugin;
import io.noties.markwon.linkify.LinkifyPlugin;

@SuppressWarnings("FieldCanBeLocal")
public class EditorActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
    //GUI Elements
    private Window window;
    private RelativeLayout layout_root;
    private Toolbar toolbar;
    private TextView textView;
    private EditText editTextTitle, editText;
    private ImageButton action_add_content, action_format_style,
            action_format_color, action_format_background,
            action_undo, action_redo;

    // Data
    @ColorRes
    private int bgColor;
    private boolean preview = false;
    private int textSelectionStart = 0, // Start of text selection
            textSelectionEnd = 0, // End of selection
            textSelectionPoint = 0; // Original cursor position
    private final ArrayList<String> textFonts = new ArrayList<>(
            Arrays.asList(
                    "###### Heading 6",
                    "##### Heading 5",
                    "#### Heading 4",
                    "### Heading 3",
                    "## Heading 2",
                    "# Heading 1",
                    "Normal"
            )
    );
    private Markwon markwon;
    private MarkwonEditor markwonEditor;

    // Debugging
    private final String TAG = "EditorActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        /* INIT DATA */
        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        if (bundle.containsKey("bgColor")) {
            bgColor = bundle.getInt("bgColor");
        } else {
            bgColor = R.color.white;
        }

        /* INIT MARKDOWN BACKEND */
        markwon = Markwon.builder(this)
                .usePlugin(SoftBreakAddsNewLinePlugin.create())
                .usePlugin(TablePlugin.create(this))
                .usePlugin(LinkifyPlugin.create())
                .build();
        markwonEditor = MarkwonEditor.create(markwon);

        /* INIT GUI ELEMENTS */
        // - Root layout
        window = getWindow();
        layout_root = findViewById(R.id.layout_root);
        // - Top Toolbar
        toolbar = findViewById(R.id.toolbar);
        editTextTitle = findViewById(R.id.editTextTitle);
        // - Bottom Appbar
        action_add_content = findViewById(R.id.action_add_content);
        action_format_style = findViewById(R.id.action_format_style);
        action_format_color = findViewById(R.id.action_format_color);
        action_format_background = findViewById(R.id.action_format_background);
        action_undo = findViewById(R.id.action_undo);
        action_redo = findViewById(R.id.action_redo);
        // - Content Layout
        textView = findViewById(R.id.textView);
        editText = findViewById(R.id.editText);

        /* INIT ROOT LAYOUT */
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

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
        action_add_content.setOnClickListener(this::onClick);
        action_format_style.setOnClickListener(this::onClick);
        action_format_color.setOnClickListener(this::onClick);
        action_format_background.setOnClickListener(this::onClick);
        action_undo.setEnabled(false);
        action_undo.setOnClickListener(this::onClick);
        action_redo.setEnabled(false);
        action_redo.setOnClickListener(this::onClick);

        /* INIT CONTENT LAYOUT */
        editText.addTextChangedListener(
                MarkwonEditorTextWatcher.withPreRender(
                        markwonEditor,
                        Executors.newCachedThreadPool(),
                        editText
                )
        );
        setBackground(bgColor);
    }

    /**
     * An event triggered when the top toolbar is shown
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
     * <p>Perform an action based on which toolbar menu item was clicked.
     * <p>The menu items as specified in
     * {@link com.lexisnguyen.quicknotie.R.layout#layout_editor_top_toolbar layout_editor_top_toolbar}
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

    /**
     * An event triggered when a view (in most times, a button) is clicked
     *
     * @param view The view in question
     */
    @SuppressLint("NonConstantResourceId")
    private void onClick(View view) {
        getTextSelection();
        switch (view.getId()) {
            // Bottom bar
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

            // Add content dialog
            case R.id.action_add_camera:
                // TODO: Take camera and add to note
                break;
            case R.id.action_add_image:
                // TODO: Upload image to note
                break;
            case R.id.action_add_table:
                action_add_table();
                break;
            case R.id.action_add_codeblock:
                action_add_codeblock();
                break;
            case R.id.action_add_link:
                action_add_link();
                break;
            case R.id.action_add_quote:
                action_add_quote();
                break;
            case R.id.action_add_line:
                action_add_line();
                break;

            // Format style dialog
            // - Align group
            case R.id.action_table_align_left:
                // TODO: Add 3x1 table and put text on the 1st cell
                break;
            case R.id.action_table_align_center:
                // TODO: Add 3x1 table and put text on the 2nd cell
                break;
            case R.id.action_table_align_right:
                // TODO: Add 3x1 table and put text on the 3rd cell
                break;
            // - List style group
            case R.id.action_bullet:
                // TODO: Add/remove bulleted list symbol at the start of line
                break;
            case R.id.action_number:
                // TODO: Add/remove numbered list symbol at the start of line
                break;
            // - Text style group
            case R.id.action_format_bold:
                // TODO: Add/remove bold symbol at current position or surrounding selection
                break;
            case R.id.action_format_italic:
                // TODO: Add/remove italic symbol at current position or surrounding selection
                break;
            case R.id.action_format_underline:
                // TODO: Add/remove underline symbol at current position or surrounding selection
                break;
            case R.id.action_format_strikethrough:
                // TODO: Add/remove strikethrough symbol at current position or surrounding selection
                break;
            // - Script group
            case R.id.action_format_superscript:
                // TODO: Add/remove sup tag at current position or surrounding selection
                break;
            case R.id.action_format_subscript:
                // TODO: Add/remove sub tag at current position or surrounding selection
                break;
            // - Indent group
            case R.id.action_format_indent_increase:
                action_format_indent(true);
                break;
            case R.id.action_format_indent_decrease:
                action_format_indent(false);
                break;
        }
    }

    /**
     * An event triggered when an option of a spinner is clicked
     *
     * @param adapterView The adapter of the spinner in question
     * @param view        The view of the option that was clicked
     * @param i           The position of the view in the adapter
     * @param l           The row id of the item that is selected
     */
    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        // TODO: Insert heading item or remove it
    }

    /**
     * <p>An event triggered when the user cancel the spinner popup</p>
     * <p><i>This event is ignored</i></p>
     *
     * @param adapterView The adapter of the spinner in question
     */
    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {}

    /**
     * Get the position of selected text in editText to be used in format-related functions
     */
    private void getTextSelection() {
        if (!editText.isFocused()) {
            textSelectionStart = editText.length();
            textSelectionEnd = editText.length();
            textSelectionPoint = editText.length();
            return;
        } else if (editText.length() == 0) {
            textSelectionStart = 0;
            textSelectionEnd = 0;
            textSelectionPoint = 0;
            return;
        }

        // Get selection
        textSelectionStart = editText.getSelectionStart();
        textSelectionEnd = editText.getSelectionEnd();
        textSelectionPoint = textSelectionStart;

        // Select whole line or everything if nothing is selected
        if (textSelectionStart == textSelectionEnd) {
            String str = editText.getText().toString();
            textSelectionStart = getStartOfLine(str, textSelectionPoint);
            textSelectionEnd = getEndOfLine(str, textSelectionPoint);
        }
    }

    /**
     * Get cursor position for the start of current line
     *
     * @param str       The whole text
     * @param cursorPos Current cursor position
     * @return The start position of current line
     */
    private int getStartOfLine(String str, int cursorPos) {
        str = str.substring(0, cursorPos);
        int start = str.lastIndexOf("\n");
        return (start == -1) ? 0 : start + 1;
    }

    /**
     * Get cursor position for the end of current line
     *
     * @param str       The whole text
     * @param cursorPos Current cursor position
     * @return The end position of current line
     */
    private int getEndOfLine(String str, int cursorPos) {
        int end = str.indexOf("\n", cursorPos);
        return (end == -1) ? str.length() : end;
    }

    /**
     * Shows a BottomSheetDialog with the specified layout
     *
     * @param layoutId The ID of the layout
     */
    @SuppressLint("NonConstantResourceId")
    private void showBottomDialog(@LayoutRes int layoutId) {
        // Put layout view into dialog
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(layoutId);

        // Init buttons based on layout
        try {
            switch (layoutId) {
                case R.layout.layout_add_content:
                    layout_add_content(dialog);
                    break;
                case R.layout.layout_format_style:
                    layout_format_style(dialog);
                    break;
                case R.layout.layout_format_color:
                    layout_format_color(dialog);
                    break;
                case R.layout.layout_format_background:
                    layout_format_background(dialog);
                    break;
            }
        } catch (Throwable e) {
            // Don't show the dialog if there is any error
            Log.e(TAG, "showBottomDialog: " + e.getMessage());
            return;
        }

        dialog.show();
    }

    /**
     * Init elements in the Add Content dialog
     *
     * @param dialog The dialog in question
     * @throws Throwable Any error occurred
     */
    private void layout_add_content(BottomSheetDialog dialog) throws Throwable {
        Button action_add_camera = dialog.findViewById(R.id.action_add_camera),
                action_add_image = dialog.findViewById(R.id.action_add_image),
                action_add_table = dialog.findViewById(R.id.action_add_table),
                action_add_codeblock = dialog.findViewById(R.id.action_add_codeblock),
                action_add_link = dialog.findViewById(R.id.action_add_link),
                action_add_quote = dialog.findViewById(R.id.action_add_quote),
                action_add_line = dialog.findViewById(R.id.action_add_line);
        if (action_add_camera == null || action_add_image == null || action_add_table == null || action_add_codeblock == null
                || action_add_link == null || action_add_quote == null || action_add_line == null) {
            throw new Throwable("Missing button in Add Content dialog");
        }
        action_add_camera.setOnClickListener(view -> {
            onClick(view);
            dialog.onBackPressed();
        });
        action_add_image.setOnClickListener(view -> {
            onClick(view);
            dialog.onBackPressed();
        });
        action_add_table.setOnClickListener(view -> {
            onClick(view);
            dialog.onBackPressed();
        });
        action_add_codeblock.setOnClickListener(view -> {
            onClick(view);
            dialog.onBackPressed();
        });
        action_add_link.setOnClickListener(view -> {
            onClick(view);
            dialog.onBackPressed();
        });
        action_add_quote.setOnClickListener(view -> {
            onClick(view);
            dialog.onBackPressed();
        });
        action_add_line.setOnClickListener(view -> {
            onClick(view);
            dialog.onBackPressed();
        });
    }

    /**
     * Init elements in the Format Style dialog
     *
     * @param dialog The dialog in question
     * @throws Throwable Any error occurred
     */
    private void layout_format_style(BottomSheetDialog dialog) throws Throwable {
        // Font spinner
        Spinner action_text_font = dialog.findViewById(R.id.action_text_font);
        ArrayList<Spanned> textFontsSpanned = new ArrayList<>();
        for (String font : textFonts) {
            textFontsSpanned.add(markwon.toMarkdown(font));
        }
        ArrayAdapter<Spanned> textFontAdapter =
                new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, textFontsSpanned);
        if (action_text_font == null) {
            throw new Throwable("Missing font spinner in Format Style dialog");
        }
        action_text_font.setAdapter(textFontAdapter);
        action_text_font.setOnItemSelectedListener(this);
        action_text_font.setSelection(textFonts.size() - 1);

        // Align group
        ImageButton action_table_align_left = dialog.findViewById(R.id.action_table_align_left),
                action_table_align_center = dialog.findViewById(R.id.action_table_align_center),
                action_table_align_right = dialog.findViewById(R.id.action_table_align_right);
        if (action_table_align_left == null || action_table_align_center == null || action_table_align_right == null) {
            throw new Throwable("Missing button in Align group of the Format Style dialog");
        }
        action_table_align_left.setOnClickListener(this::onClick);
        action_table_align_center.setOnClickListener(this::onClick);
        action_table_align_right.setOnClickListener(this::onClick);

        // List Style group
        ImageButton action_bullet = dialog.findViewById(R.id.action_bullet),
                action_number = dialog.findViewById(R.id.action_number);
        if (action_bullet == null || action_number == null) {
            throw new Throwable("Missing button in List Style group of the Format Style dialog");
        }
        action_bullet.setOnClickListener(this::onClick);
        action_number.setOnClickListener(this::onClick);

        // Text Style group
        ImageButton action_format_bold = dialog.findViewById(R.id.action_format_bold),
                action_format_italic = dialog.findViewById(R.id.action_format_italic),
                action_format_underline = dialog.findViewById(R.id.action_format_underline),
                action_format_strikethrough = dialog.findViewById(R.id.action_format_strikethrough);
        if (action_format_bold == null || action_format_italic == null || action_format_underline == null || action_format_strikethrough == null) {
            throw new Throwable("Missing button in Text Style group of the Format Style dialog");
        }
        action_format_bold.setOnClickListener(this::onClick);
        action_format_italic.setOnClickListener(this::onClick);
        action_format_underline.setOnClickListener(this::onClick);
        action_format_strikethrough.setOnClickListener(this::onClick);

        // Script group
        ImageButton action_format_superscript = dialog.findViewById(R.id.action_format_superscript),
                action_format_subscript = dialog.findViewById(R.id.action_format_subscript);
        if (action_format_superscript == null || action_format_subscript == null) {
            throw new Throwable("Missing button in Script group of the Format Style dialog");
        }
        action_format_superscript.setOnClickListener(this::onClick);
        action_format_subscript.setOnClickListener(this::onClick);

        // Indent group
        ImageButton action_format_indent_increase = dialog.findViewById(R.id.action_format_indent_increase),
                action_format_indent_decrease = dialog.findViewById(R.id.action_format_indent_decrease);
        if (action_format_indent_increase == null || action_format_indent_decrease == null) {
            throw new Throwable("Missing button in Indent group of the Format Style dialog");
        }
        action_format_indent_increase.setOnClickListener(this::onClick);
        action_format_indent_decrease.setOnClickListener(this::onClick);

    }

    /**
     * Init elements in the Text Color dialog
     *
     * @param dialog The dialog in question
     * @throws Throwable Any error occurred
     */
    private void layout_format_color(BottomSheetDialog dialog) throws Throwable {
        ImageButton action_color_red = dialog.findViewById(R.id.action_color_red),
                action_color_orange = dialog.findViewById(R.id.action_color_orange),
                action_color_yellow = dialog.findViewById(R.id.action_color_yellow),
                action_color_green = dialog.findViewById(R.id.action_color_green),
                action_color_tortoise = dialog.findViewById(R.id.action_color_tortoise),
                action_color_cyan = dialog.findViewById(R.id.action_color_cyan),
                action_color_blue = dialog.findViewById(R.id.action_color_blue),
                action_color_violet = dialog.findViewById(R.id.action_color_violet),
                action_color_purple = dialog.findViewById(R.id.action_color_purple),
                action_color_magenta = dialog.findViewById(R.id.action_color_magenta),
                action_color_pink = dialog.findViewById(R.id.action_color_pink),
                action_color_black = dialog.findViewById(R.id.action_color_black),
                action_color_lightgray = dialog.findViewById(R.id.action_color_lightgray),
                action_color_light = dialog.findViewById(R.id.action_color_light),
                action_color_white = dialog.findViewById(R.id.action_color_white);
        if (action_color_red == null || action_color_orange == null || action_color_yellow == null ||
                action_color_green == null || action_color_tortoise == null || action_color_cyan == null ||
                action_color_blue == null || action_color_violet == null || action_color_purple == null ||
                action_color_magenta == null || action_color_pink == null || action_color_black == null ||
                action_color_lightgray == null || action_color_light == null || action_color_white == null) {
            throw new Throwable("Missing button in Text Color dialog");
        }
        action_color_red.setOnClickListener((view) -> setTextColor(R.color.red));
        action_color_orange.setOnClickListener((view) -> setTextColor(R.color.orange));
        action_color_yellow.setOnClickListener((view) -> setTextColor(R.color.yellow));
        action_color_green.setOnClickListener((view) -> setTextColor(R.color.green));
        action_color_tortoise.setOnClickListener((view) -> setTextColor(R.color.tortoise));
        action_color_cyan.setOnClickListener((view) -> setTextColor(R.color.cyan));
        action_color_blue.setOnClickListener((view) -> setTextColor(R.color.blue));
        action_color_violet.setOnClickListener((view) -> setTextColor(R.color.violet));
        action_color_purple.setOnClickListener((view) -> setTextColor(R.color.purple));
        action_color_magenta.setOnClickListener((view) -> setTextColor(R.color.magenta));
        action_color_pink.setOnClickListener((view) -> setTextColor(R.color.pink));
        action_color_black.setOnClickListener((view) -> setTextColor(R.color.black));
        action_color_lightgray.setOnClickListener((view) -> setTextColor(R.color.lightgray));
        action_color_light.setOnClickListener((view) -> setTextColor(R.color.light));
        action_color_white.setOnClickListener((view) -> setTextColor(R.color.white));
    }

    /**
     * Init elements in the Background dialog
     *
     * @param dialog The dialog in question
     * @throws Throwable Any error occurred
     */
    private void layout_format_background(BottomSheetDialog dialog) throws Throwable {
        ImageButton action_color_lightred = dialog.findViewById(R.id.action_color_lightred),
                action_color_lightorange = dialog.findViewById(R.id.action_color_lightorange),
                action_color_lightyellow = dialog.findViewById(R.id.action_color_lightyellow),
                action_color_lightgreen = dialog.findViewById(R.id.action_color_lightgreen),
                action_color_lighttortoise = dialog.findViewById(R.id.action_color_lighttortoise),
                action_color_lightcyan = dialog.findViewById(R.id.action_color_lightcyan),
                action_color_lightblue = dialog.findViewById(R.id.action_color_lightblue),
                action_color_lightviolet = dialog.findViewById(R.id.action_color_lightviolet),
                action_color_lightpurple = dialog.findViewById(R.id.action_color_lightpurple),
                action_color_lightmagenta = dialog.findViewById(R.id.action_color_lightmagenta),
                action_color_lightpink = dialog.findViewById(R.id.action_color_lightpink),
                action_color_lightgray = dialog.findViewById(R.id.action_color_lightgray),
                action_color_lightergray = dialog.findViewById(R.id.action_color_lightergray),
                action_color_light = dialog.findViewById(R.id.action_color_light),
                action_color_white = dialog.findViewById(R.id.action_color_white);
        if (action_color_lightred == null || action_color_lightorange == null || action_color_lightyellow == null ||
                action_color_lightgreen == null || action_color_lighttortoise == null || action_color_lightcyan == null ||
                action_color_lightblue == null || action_color_lightviolet == null || action_color_lightpurple == null ||
                action_color_lightmagenta == null || action_color_lightpink == null || action_color_lightgray == null ||
                action_color_lightergray == null || action_color_light == null || action_color_white == null) {
            throw new Throwable("Missing button in Background dialog");
        }
        action_color_lightred.setOnClickListener((view) -> setBackground(dialog, R.color.lightred));
        action_color_lightorange.setOnClickListener((view) -> setBackground(dialog, R.color.lightorange));
        action_color_lightyellow.setOnClickListener((view) -> setBackground(dialog, R.color.lightyellow));
        action_color_lightgreen.setOnClickListener((view) -> setBackground(dialog, R.color.lightgreen));
        action_color_lighttortoise.setOnClickListener((view) -> setBackground(dialog, R.color.lighttortoise));
        action_color_lightcyan.setOnClickListener((view) -> setBackground(dialog, R.color.lightcyan));
        action_color_lightblue.setOnClickListener((view) -> setBackground(dialog, R.color.lightblue));
        action_color_lightviolet.setOnClickListener((view) -> setBackground(dialog, R.color.lightviolet));
        action_color_lightpurple.setOnClickListener((view) -> setBackground(dialog, R.color.lightpurple));
        action_color_lightmagenta.setOnClickListener((view) -> setBackground(dialog, R.color.lightmagenta));
        action_color_lightpink.setOnClickListener((view) -> setBackground(dialog, R.color.lightpink));
        action_color_lightgray.setOnClickListener((view) -> setBackground(dialog, R.color.lightgray));
        action_color_lightergray.setOnClickListener((view) -> setBackground(dialog, R.color.lightergray));
        action_color_light.setOnClickListener((view) -> setBackground(dialog, R.color.light));
        action_color_white.setOnClickListener((view) -> setBackground(dialog, R.color.white));
    }

    private void setTextColor(@ColorRes int colorId) {
        // TODO: Add color tag to cursor position or surrounding selection
    }

    /**
     * Set the background of the note
     *
     * @param colorId The id of the note as specified in
     *                {@link com.lexisnguyen.quicknotie.R.color#black values/colors.xml}
     */
    @SuppressWarnings({"ConstantConditions", "deprecation"})
    private void setBackground(@ColorRes int colorId) {
        // Set background color
        window.setStatusBarColor(getColor(colorId));
        toolbar.setBackgroundColor(getColor(colorId));
        layout_root.setBackgroundColor(getColor(colorId));

        // Update icon color based on background color
        // - Get color and filter (?colorOnSecondary is the default color for icons)
        int iconColor, statusBarIconColor, hintTextColor;
        if (colorId == R.color.lightgray || colorId == R.color.lightergray) {
            iconColor = getColor(R.color.white);
            statusBarIconColor = 0;
            hintTextColor = getColor(R.color.faded_white);
        } else {
            iconColor = MaterialColors.getColor(layout_root, R.attr.colorOnSecondary);
            statusBarIconColor = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            hintTextColor = getColor(R.color.faded_black);
        }
        // - Root layout
        getWindow().getDecorView().setSystemUiVisibility(statusBarIconColor);
        // - Top Toolbar
        Menu menu = toolbar.getMenu();
        MenuItem action_preview = menu.findItem(R.id.action_preview),
                action_remind = menu.findItem(R.id.action_remind),
                action_share = menu.findItem(R.id.action_share);
        toolbar.getNavigationIcon().setTint(iconColor);
        action_preview.getIcon().setTint(iconColor);
        action_remind.getIcon().setTint(iconColor);
        action_share.getIcon().setTint(iconColor);
        toolbar.getOverflowIcon().setTint(iconColor);
        // - Content Layout
        editTextTitle.setTextColor(iconColor);
        editTextTitle.setHintTextColor(hintTextColor);
        editText.setTextColor(iconColor);
        editText.setHintTextColor(hintTextColor);
        textView.setTextColor(iconColor);
        textView.setHintTextColor(hintTextColor);
    }

    /**
     * Set note background and dismiss the current showing BottomSheetDialog
     *
     * @param dialog  The dialog in question
     * @param colorId The id of the note as specified in
     *                {@link com.lexisnguyen.quicknotie.R.color#black values/colors.xml}
     */
    private void setBackground(BottomSheetDialog dialog, @ColorRes int colorId) {
        setBackground(colorId);
        dialog.onBackPressed();
    }

    /**
     * TODO: Add table on the line under typing cursor
     */
    private void action_add_table() {

    }

    /**
     * Put selection/line in between a new codeblock
     */
    private void action_add_codeblock() {
        // Put ``` before and after content
        CharSequence newString = editText.getText();
        if (textSelectionStart == textSelectionEnd) {
            newString = "```\n\n```\n";
            textSelectionEnd--;
        } else {
            newString = newString.subSequence(0, textSelectionStart) +
                    "\n```\n" +
                    newString.subSequence(textSelectionStart, textSelectionEnd) +
                    "\n```\n" +
                    newString.subSequence(textSelectionEnd, newString.length());
        }
        editText.setText(newString);

        // Update new cursor position
        // - Retain original cursor position (or Put cursor at the end of selection)
        // - The string "\n```\n" has 5 characters -> offset by 5 chars
        editText.setSelection(textSelectionPoint
                + ((textSelectionPoint != textSelectionStart) ? 0 : textSelectionEnd - textSelectionStart)
                + 5);
    }

    private void action_add_link() {
        // TODO: action_add_link

    }

    /**
     * Put a quote symbol at the start of line
     */
    private void action_add_quote() {
        int startOfLine = getStartOfLine(editText.getText().toString(), textSelectionPoint);
        Editable newString = editText.getText();
        newString.insert(startOfLine, " > ");
    }

    /**
     * Put a horizontal line after the current line
     */
    private void action_add_line() {
        int endOfLine = getEndOfLine(editText.getText().toString(), textSelectionPoint);
        Editable newString = editText.getText();
        newString.insert(endOfLine, "\n***\n");
    }


    /**
     * Increase/decrease 1 level of indent at the start of line
     *
     * @param increasing Will this function increase the indent? (decrease if not)
     */
    private void action_format_indent(boolean increasing) {
        int startOfLine = getStartOfLine(editText.getText().toString(), textSelectionPoint),
                endOfLine = getEndOfLine(editText.getText().toString(), textSelectionPoint),
                cursorMoveAmount = 0;
        String newString = editText.getText().toString();

        // Get current line
        CharSequence line = newString.subSequence(startOfLine, endOfLine);
        Editable lineEditable = Editable.Factory.getInstance().newEditable(line);

        // Scanning for an indent indicator and perform appropriate actions
        int charPos = -1;
        char curChar = '\0';
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == ' ' || c == '>') {
                continue;
            } else if (cursorMoveAmount != 0) {
                break;
            }
            switch (c) {
                case '-':
                case '+':
                case '*':
                    // Bullet indent -> Keep going until reaching end of space sequence
                    if (curChar == '\0') {
                        charPos = i;
                        curChar = c;
                        continue;
                    }
                    break;
                case '&':
                    if (line.subSequence(i, i + 6).equals("&ensp;")) {
                        // Normal indent -> Add/remove 1 normal indent and end loop
                        if (increasing) {
                            lineEditable.insert(i, "&ensp;");
                            cursorMoveAmount += 6;
                        } else {
                            lineEditable.replace(i, i + 6, "");
                            cursorMoveAmount -= 6;
                        }
                        continue;
                    }
                    break;
            }
            if (curChar == '\0') {
                // No special indent -> Add normal indent only
                // TODO: Check for numbered list indent
                if (increasing) {
                    lineEditable.insert(i, "&ensp;");
                    cursorMoveAmount += 6;
                }
            } else {
                switch (curChar) {
                    case '-':
                    case '+':
                    case '*':
                        // Bullet list -> Add/remove 2 spaces to increase/decrease 1 indent level
                        if (i > charPos + 1) {
                            if (increasing) {
                                lineEditable.insert(charPos, "  ");
                                cursorMoveAmount += 2;
                            } else if (charPos >= 2) {
                                if (line.subSequence(charPos - 2, charPos).equals("  ")) {
                                    lineEditable.replace(charPos - 2, charPos, "");
                                    cursorMoveAmount -= 2;
                                }
                            }
                        } else {
                            // Actually not a bullet -> Add normal indent
                            if (increasing) {
                                lineEditable.insert(charPos, "&ensp;");
                                cursorMoveAmount += 6;
                            }
                        }
                        break;
                }
            }
            break;
        }
        newString = newString.substring(0, startOfLine) +
                lineEditable.toString() +
                newString.substring(endOfLine);
        editText.setText(newString);
        editText.setSelection(textSelectionPoint + cursorMoveAmount);
    }

    /**
     * <p>Toggle between Preview mode and Edit mode</p>
     * <p>- Preview mode: View result of the markdown text</p>
     * <p>- Edit mode: Make edits to the markdown text</p>
     *
     * @param menuItem The
     *                 {@link com.lexisnguyen.quicknotie.R.id#action_preview action_preview}
     *                 button to update its icon
     */
    private void action_preview(MenuItem menuItem) {
        preview = !preview;

        // Parse content of EditText into TextView
        Editable text = editText.getText();
        markwon.setMarkdown(textView, text.toString());

        // Hide keyboard
        // https://stackoverflow.com/a/17789187
        InputMethodManager imm = (InputMethodManager) this.getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE);
        View v = this.getCurrentFocus();
        if (v == null) {
            v = new View(this);
        }
        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);

        // Toggle editor buttons
        action_add_content.setEnabled(!preview);
        action_format_style.setEnabled(!preview);
        action_format_color.setEnabled(!preview);
        action_format_background.setEnabled(!preview);
        action_undo.setEnabled(!preview);
        action_redo.setEnabled(!preview);

        // Switching functions and UI based on current mode:
        // - Interacting with title EditText
        // - Visibility of content EditText or TextView
        // - Visibility of hint text of both EditText
        // - Icon of action_preview
        if (preview) {
            editTextTitle.setInputType(InputType.TYPE_NULL);
            editTextTitle.setTextIsSelectable(true);

            textView.setVisibility(View.VISIBLE);
            editText.setVisibility(View.INVISIBLE);

            editText.setHint("");
            editTextTitle.setHint("");

            menuItem.setIcon(R.drawable.action_edit);
            menuItem.setTooltipText(getString(R.string.action_edit));
        } else {
            editTextTitle.setInputType(InputType.TYPE_CLASS_TEXT);

            textView.setVisibility(View.INVISIBLE);
            editText.setVisibility(View.VISIBLE);

            editText.setHint(R.string.info_title_hint);
            editTextTitle.setHint(R.string.info_text_hint);

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