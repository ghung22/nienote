package com.lexisnguyen.quicknotie;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
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

import java.util.ArrayList;
import java.util.Arrays;

import de.stocard.markdown_to_spanned.Markdown;

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
    private @ColorRes
    int bgColor;
    private boolean preview = false;
    private final ArrayList<String> textFonts = new ArrayList<>(
            Arrays.asList("Normal", "Heading 1", "Heading 2", "Heading 3", "Heading 4", "Heading 5", "Heading 6")
    );

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
     * An event triggered when a view (in most times, a button) is clicked
     *
     * @param view The view in question
     */
    @SuppressLint("NonConstantResourceId")
    private void onClick(View view) {
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
                // TODO: Add table on the line under typing cursor
                break;
            case R.id.action_add_checklist:
                // TODO: Add checklist symbol at the start of line
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
                // TODO: Add 1 tab character at the start of line
                break;
            case R.id.action_format_indent_decrease:
                // TODO: Remove 1 tab character at the start of line
                break;
        }
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
        LinearLayout action_add_camera = dialog.findViewById(R.id.action_add_camera),
                action_add_image = dialog.findViewById(R.id.action_add_image),
                action_add_table = dialog.findViewById(R.id.action_add_table),
                action_add_checklist = dialog.findViewById(R.id.action_add_checklist);
        if (action_add_camera == null || action_add_image == null || action_add_table == null || action_add_checklist == null) {
            throw new Throwable("Missing button in Add Content dialog");
        }
        action_add_camera.setOnClickListener(this::onClick);
        action_add_image.setOnClickListener(this::onClick);
        action_add_table.setOnClickListener(this::onClick);
        action_add_checklist.setOnClickListener(this::onClick);
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
        ArrayAdapter<String> textFontAdapter =
                new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, textFonts);
        if (action_text_font == null) {
            throw new Throwable("Missing font spinner in Format Style dialog");
        }
        action_text_font.setAdapter(textFontAdapter);
        action_text_font.setOnItemSelectedListener(this);

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