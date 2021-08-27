package com.lexisnguyen.quicknotie.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.InputType;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
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
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.MaterialColors;
import com.lexisnguyen.quicknotie.R;
import com.lexisnguyen.quicknotie.components.markdown.AlignTagHandler;
import com.lexisnguyen.quicknotie.components.markdown.ColorTagHandler;
import com.lexisnguyen.quicknotie.components.markdown.NotieGrammarLocator;
import com.lexisnguyen.quicknotie.components.sql.Note;
import com.lexisnguyen.quicknotie.components.sql.Trash;
import com.lexisnguyen.quicknotie.components.undo.UndoAdapter;
import com.lexisnguyen.quicknotie.components.undo.UndoManager;

import org.apache.commons.lang3.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.noties.markwon.AbstractMarkwonPlugin;
import io.noties.markwon.Markwon;
import io.noties.markwon.MarkwonPlugin;
import io.noties.markwon.SoftBreakAddsNewLinePlugin;
import io.noties.markwon.core.MarkwonTheme;
import io.noties.markwon.editor.MarkwonEditor;
import io.noties.markwon.editor.MarkwonEditorTextWatcher;
import io.noties.markwon.ext.tables.TablePlugin;
import io.noties.markwon.html.HtmlPlugin;
import io.noties.markwon.linkify.LinkifyPlugin;
import io.noties.markwon.syntax.Prism4jTheme;
import io.noties.markwon.syntax.Prism4jThemeDarkula;
import io.noties.markwon.syntax.Prism4jThemeDefault;
import io.noties.markwon.syntax.SyntaxHighlightPlugin;
import io.noties.prism4j.Prism4j;

import static com.lexisnguyen.quicknotie.activities.MainActivity.ACTION_ADD_CODEBLOCK;
import static com.lexisnguyen.quicknotie.activities.MainActivity.ACTION_ADD_EMPTY;
import static com.lexisnguyen.quicknotie.activities.MainActivity.ACTION_ADD_IMAGE;

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
    // - SQLite
    private Note note;
    // - Animation
    private final float bounceAmount = 20;
    private final int quickAni = 150;
    private final int normalAni = 300;
    // - Root layout
    @ColorRes
    private int bgColor;
    @ColorRes
    private int oldBgColor;
    private boolean preview = false;
    // - EditText formatting
    private int textSelectionStart = 0, // Start of text selection
            textSelectionEnd = 0; // End of selection
    private final ArrayList<String> textFonts = new ArrayList<>(
            Arrays.asList(
                    "###### Heading 6",
                    "##### Heading 5",
                    "#### Heading 4",
                    "### Heading 3",
                    "## Heading 2",
                    "# Heading 1",
                    "` Monospace `",
                    "Normal"
            )
    );
    // - Markdown
    public static Markwon markwon;
    public static MarkwonEditor markwonEditor;
    // - UndoManager
    private UndoManager undoManager;
    private CountDownTimer textChangedTimer = null;
    private TextWatcher textWatcher;

    // Debugging
    private final String TAG = "EditorActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        initGuiElements();
        initData();
        initMarkdown(this, bgColor);
        initRootLayout();
        initTopToolbar();
        initBottomAppbar();
        initContentLayout();
        initUndoRedo();
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

    // region Init events

    /**
     * Init passed/saved data from other activities/settings
     */
    private void initData() {
        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();

        // From intent
        int action = bundle.getInt("action");
        String folder = bundle.getString("folder");
        switch (action) {
            case ACTION_ADD_EMPTY:
                // Create an empty list
                break;
            case ACTION_ADD_CODEBLOCK:
                // Create a checklist template
                break;
            case ACTION_ADD_IMAGE:
                // Put image into note
                break;
        }

        // From Settings
        bgColor = R.color.white;
        oldBgColor = R.color.white;

        // From SQLite
        boolean queryFailed = true;
        if (bundle.containsKey("noteId")) {
            long id = bundle.getLong("noteId");
            List<Note> queryResult = Note.find(Note.class, "folder = ?", folder);
            note = queryResult.get((int) id);
            editTextTitle.setText(note.title);
            editText.setText(note.text);
            bgColor = note.bgColor;
            oldBgColor = bgColor;
            queryFailed = false;
        }
        if (queryFailed) {
            // Create a new note
            note = new Note(
                    folder,
                    "",
                    "",
                    bgColor);
        }
    }

    /**
     * Init markdown functionalities by using <b>Markwon</b>
     * Plugins used:
     * <ul>
     *   <li><b>SoftBreakAddsNewLinePlugin</b>: Treat user newline as Markdown's newline (it is ignored by default)</li>
     *   <li><b>LinkifyPlugin</b>: Enable Markdown's link display</li>
     *   <li><b>headingTheme</b>: Custom theme for headers</li>
     *   <li><b>Html</b>: Enable HTML support</li>
     *   <li><b>alignTags</b>: Align text using HTML tags</li>
     *   <li><b>colorTags</b>: Change text color using HTML tags</li>
     *   <li><b>tablePlugin</b>: Enable Markdown's table display</li>
     *   <li><b>inlineCodeNoBackground</b>: Disable inline code background (for monospace font functionality)</li>
     *   <li>
     *     <b>SyntaxHighlightPlugin</b>: Syntax highlight support with using Prism4j <br/>
     *     *{@link NotieGrammarLocator#languages() Supported Languages}
     *   </li>
     *   <li><b>Editor</b>: Enable Markdown syntax highlighting</li>
     * </ul>
     *
     * @see <a href="https://github.com/noties/Markwon">Markwon</a>
     */
    public static void initMarkdown(Context context, int bgColor) {
        // Custom plugins
        // - Create abstract plugins
        MarkwonPlugin headingTheme = new AbstractMarkwonPlugin() {
            @Override
            public void configureTheme(@NonNull MarkwonTheme.Builder builder) {
                builder.headingBreakHeight(0);
            }
        }, alignTags = new AbstractMarkwonPlugin() {
            @Override
            public void configure(@NonNull Registry registry) {
                registry.require(HtmlPlugin.class, htmlPlugin ->
                        htmlPlugin.addHandler(new AlignTagHandler()));
            }
        }, colorTags = new AbstractMarkwonPlugin() {
            @Override
            public void configure(@NonNull Registry registry) {
                registry.require(HtmlPlugin.class, htmlPlugin ->
                        htmlPlugin.addHandler(new ColorTagHandler(context)));
            }
        }, tablePlugin = TablePlugin.create(builder ->
                builder.tableBorderWidth(2)
                        .tableCellPadding(16)
                        .tableBorderColor(context.getColor(R.color.faded_black))
                        .tableHeaderRowBackgroundColor(context.getColor(R.color.faded_black))
                        .tableEvenRowBackgroundColor(context.getColor(bgColor))
                        .tableOddRowBackgroundColor(context.getColor(bgColor))
        ), inlineCodeNoBackground = new AbstractMarkwonPlugin() {
            @Override
            public void configureTheme(@NonNull MarkwonTheme.Builder builder) {
                builder.codeTextSize((int) context.getResources().getDimension(R.dimen.content_layout_text_size))
                        .codeBackgroundColor(context.getColor(bgColor));
            }
        };
        // - Syntax highlighting settings
        Prism4j prism4j = new Prism4j(new NotieGrammarLocator());
        Prism4jTheme prism4jTheme = isDarkMode(bgColor) ?
                new Prism4jThemeDefault(context.getColor(R.color.faded_white)) :
                new Prism4jThemeDarkula(context.getColor(R.color.faded_black));

        // Build Markwon
        markwon = Markwon.builder(context)
                .usePlugin(SoftBreakAddsNewLinePlugin.create())
                .usePlugin(LinkifyPlugin.create())
                .usePlugin(headingTheme)
                .usePlugin(HtmlPlugin.create())
                .usePlugin(alignTags)
                .usePlugin(colorTags)
                .usePlugin(tablePlugin)
                .usePlugin(inlineCodeNoBackground)
                .usePlugin(SyntaxHighlightPlugin.create(prism4j, prism4jTheme))
                .build();
        markwonEditor = MarkwonEditor.create(markwon);
    }

    /**
     * Init undo/redo functionalities
     * <ul>
     *   <li><b>history</b>: Name of this history item</li>
     *   <li><b>historyItem</b>: An old version of the text in editText</li>
     * </ul>
     */
    private void initUndoRedo() {
        undoManager = new UndoManager(editText, action_undo, action_redo);
        undoManager.setTextWatcher(textWatcher);
    }

    /**
     * Init (get) all views on the layouts by saving them into a variable
     */
    private void initGuiElements() {
        // Root layout
        window = getWindow();
        layout_root = findViewById(R.id.layout_root);
        // Top Toolbar
        toolbar = findViewById(R.id.toolbar);
        editTextTitle = findViewById(R.id.editTextTitle);
        // Bottom Appbar
        action_add_content = findViewById(R.id.action_add_content);
        action_format_style = findViewById(R.id.action_format_style);
        action_format_color = findViewById(R.id.action_format_color);
        action_format_background = findViewById(R.id.action_format_background);
        action_undo = findViewById(R.id.action_undo);
        action_redo = findViewById(R.id.action_redo);
        // Content Layout
        textView = findViewById(R.id.textView);
        editText = findViewById(R.id.editText);
    }

    /**
     * Init the layout containing everything in this activity
     */
    private void initRootLayout() {
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
    }

    /**
     * Init the top Toolbar consisting the following buttons
     * <ul>
     *   <li><b>BACK</b>: Return to MainActivity</li>
     *   <li><b>EDITTEXT</b>: Show & set note title</li>
     *   <li><b>PREVIEW</b>: Toggle preview/edit mode</li>
     *   <li><b>REMIND</b>: Set a reminder for this note</li>
     *   <li><b>SHARE</b>: Share this note</li>
     *   <li><b>OVERFLOW</b>: Show more options</li>
     * </ul>
     */
    private void initTopToolbar() {
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show back button
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowTitleEnabled(false);
        }
        toolbar.setOnMenuItemClickListener(this::OnMenuItemClick);
    }

    /**
     * Init the BottomAppbar consisting the following buttons
     * <ul>
     *   <li><b>ADD</b>: Add an image/drawing</li>
     *   <li><b>STYLE</b>: Perform text formatting
     *     <ul>
     *       <li>Set text size (heading 1, heading 2, heading 3)</li>
     *       <li>Align left/center/right text</li>
     *       <li>Set line as a bullet/numbered list</li>
     *       <li>Set bold/underline/italic/strikethrough</li>
     *       <li>Set superscript/subscript</li>
     *       <li>Increase/decrease indent</li>
     *     </ul>
     *   </li>
     *   <li><b>COLOR</b>: Set text color</li>
     *   <li><b>BG</b>: Set text background color</li>
     *   <li><b>UNDO</b>: Undo last change (hold for history list)</li>
     *   <li><b>REDO</b>: Redo last change (hold for history list)</li>
     * </ul>
     */
    private void initBottomAppbar() {
        action_add_content.setOnClickListener(this::onClick);
        action_format_style.setOnClickListener(this::onClick);
        action_format_color.setOnClickListener(this::onClick);
        action_format_background.setOnClickListener(this::onClick);
        action_undo.setEnabled(false);
        action_undo.setAlpha(.5f);
        action_undo.setOnClickListener(this::onClick);
        action_undo.setOnLongClickListener(this::onLongClick);
        action_redo.setEnabled(false);
        action_redo.setAlpha(.5f);
        action_redo.setOnClickListener(this::onClick);
        action_redo.setOnLongClickListener(this::onLongClick);
    }

    /**
     * Init the layout consisting of the note body's EditText
     */
    private void initContentLayout() {
        // Init a TextWatcher for UndoManager
        textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (textChangedTimer != null) {
                    textChangedTimer.cancel();
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
                textChangedTimer = new CountDownTimer(500, 500) {
                    public void onTick(long millisUntilFinished) {}

                    public void onFinish() {
                        String textChanged = StringUtils.difference(
                                undoManager.getUndoItems().get(undoManager.getPos()),
                                editText.getText().toString());
                        textChanged = textChanged.replace("\n", " ");
                        if (!textChanged.isEmpty()) {
                            onPostInput("Typed " + textChanged);
                        }
                    }
                }.start();
            }
        };
        editText.addTextChangedListener(MarkwonEditorTextWatcher.withPreRender(
                markwonEditor,
                Executors.newCachedThreadPool(),
                editText
        ));
        editText.addTextChangedListener(textWatcher);
        setBackground(bgColor);
    }

    // endregion

    // region User input events

    /**
     * Perform an action based on which toolbar menu item was clicked.
     * The menu items as specified in
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
                // Lock the note with a password
                break;
            case R.id.action_delete:
                // Move the note into the trash
                action_delete();
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
        int viewId = view.getId();
        String undo = "";
        @DrawableRes int undoDrawable = 0;
        getTextSelection();
        if (textChangedTimer != null) {
            textChangedTimer.onFinish();
        }
        textView.removeTextChangedListener(textWatcher);

        switch (viewId) {
            // Bottom bar
            case R.id.action_add_content:
            case R.id.action_format_style:
            case R.id.action_format_color:
            case R.id.action_format_background:
                view.animate().translationYBy(-bounceAmount).setDuration(quickAni)
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                super.onAnimationEnd(animation);
                                switch (viewId) {
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
                                }
                                view.animate().translationYBy(bounceAmount).setDuration(quickAni)
                                        .setListener(new AnimatorListenerAdapter() {
                                            @Override
                                            public void onAnimationEnd(Animator animation) {
                                                super.onAnimationEnd(animation);
                                            }
                                        });
                            }
                        });
                break;
            case R.id.action_undo:
            case R.id.action_redo:
                float _bounceAmount = (viewId == R.id.action_undo) ?
                        -bounceAmount : bounceAmount;
                view.animate().translationXBy(_bounceAmount).setDuration(quickAni)
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                super.onAnimationEnd(animation);
                                if (viewId == R.id.action_undo) {
                                    undoManager.undo();
                                } else {
                                    undoManager.redo();
                                }
                                view.animate().translationXBy(-_bounceAmount).setDuration(quickAni)
                                        .setListener(new AnimatorListenerAdapter() {
                                            @Override
                                            public void onAnimationEnd(Animator animation) {
                                                super.onAnimationEnd(animation);
                                            }
                                        });
                            }
                        });
                break;

            // Add content dialog
            case R.id.action_add_camera:
            case R.id.action_add_image:
            case R.id.action_add_table:
            case R.id.action_add_codeblock:
            case R.id.action_add_link:
            case R.id.action_add_quote:
            case R.id.action_add_line:
                switch (viewId) {
                    case R.id.action_add_camera:
                        // TODO: Take camera and add to note
                        break;
                    case R.id.action_add_image:
                        // TODO: Upload image to note
                        break;
                    case R.id.action_add_table:
                        action_add_table();
                        undo = "Add table";
                        undoDrawable = R.drawable.action_add_table;
                        break;
                    case R.id.action_add_codeblock:
                        action_add_codeblock();
                        undo = "Add codeblock";
                        undoDrawable = R.drawable.action_add_codeblock;
                        break;
                    case R.id.action_add_link:
                        action_add_link();
                        undo = "Add link";
                        undoDrawable = R.drawable.action_add_link;
                        break;
                    case R.id.action_add_quote:
                        action_add_quote();
                        undo = "Add quote";
                        undoDrawable = R.drawable.action_add_quote;
                        break;
                    case R.id.action_add_line:
                        action_add_line();
                        undo = "Add line";
                        undoDrawable = R.drawable.action_add_line;
                        break;
                }
                break;

            // Format style dialog
            // - Align group
            case R.id.action_align_start:
                action_align("start");
                undo = "Align start";
                undoDrawable = R.drawable.action_align_start;
                break;
            case R.id.action_align_center:
                action_align("center");
                undo = "Align center";
                undoDrawable = R.drawable.action_align_center;
                break;
            case R.id.action_align_end:
                action_align("end");
                undo = "Align end";
                undoDrawable = R.drawable.action_align_end;
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
                action_format_style('b');
                undo = "Format as Bold";
                undoDrawable = R.drawable.action_format_bold;
                break;
            case R.id.action_format_italic:
                action_format_style('i');
                undo = "Format as Italic";
                undoDrawable = R.drawable.action_format_italic;
                break;
            case R.id.action_format_underline:
                action_format_style('u');
                undo = "Format as Underline";
                undoDrawable = R.drawable.action_format_underline;
                break;
            case R.id.action_format_strikethrough:
                action_format_style('s');
                undo = "Format as Strikethrough";
                undoDrawable = R.drawable.action_format_strikethrough;
                break;
            // - Script group
            case R.id.action_format_superscript:
                action_format_style('^');
                undo = "Format as Superscript";
                undoDrawable = R.drawable.action_format_superscript;
                break;
            case R.id.action_format_subscript:
                action_format_style('_');
                undo = "Format as Subscript";
                undoDrawable = R.drawable.action_format_subscript;
                break;
            // - Indent group
            case R.id.action_format_indent_increase:
                action_format_indent(true);
                undo = "Increase indent";
                undoDrawable = R.drawable.action_format_indent_increase;
                break;
            case R.id.action_format_indent_decrease:
                action_format_indent(false);
                undo = "Decrease indent";
                undoDrawable = R.drawable.action_format_indent_decrease;
                break;

            default:
                Log.w(TAG, "OnClick: Unknown item " + view.getId());
                break;
        }
        if (undoDrawable == 0) {
            onPostInput(undo);
        } else {
            onPostInput(undo, undoDrawable);
        }
        getTextSelection();
        textView.addTextChangedListener(textWatcher);
    }


    /**
     * An event triggered when a view (in most times, a button) is held for a while
     *
     * @param view The view in question
     */
    @SuppressLint("NonConstantResourceId")
    private boolean onLongClick(View view) {
        switch (view.getId()) {
            case R.id.action_undo:
                action_undo.animate().translationYBy(-bounceAmount).setDuration(quickAni)
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                super.onAnimationEnd(animation);
                                showBottomDialog(R.layout.layout_undo_redo, "undo");
                                action_undo.animate().translationYBy(bounceAmount).setDuration(quickAni)
                                        .setListener(new AnimatorListenerAdapter() {
                                            @Override
                                            public void onAnimationEnd(Animator animation) {
                                                super.onAnimationEnd(animation);
                                            }
                                        });
                            }
                        });
                break;
            case R.id.action_redo:
                action_redo.animate().translationYBy(-bounceAmount).setDuration(quickAni)
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                super.onAnimationEnd(animation);
                                showBottomDialog(R.layout.layout_undo_redo, "redo");
                                action_redo.animate().translationYBy(bounceAmount).setDuration(quickAni)
                                        .setListener(new AnimatorListenerAdapter() {
                                            @Override
                                            public void onAnimationEnd(Animator animation) {
                                                super.onAnimationEnd(animation);
                                            }
                                        });
                            }
                        });
                break;
        }
        return true;
    }

    /**
     * Run an animation and close the BottomSheetDialog when a button is clicked
     *
     * @param dialog The dialog in question
     * @param view   The button view
     */
    private void onClickAnimation(BottomSheetDialog dialog, View view) {
        view.animate().alpha(.5f).setDuration(quickAni)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        view.animate().alpha(1).setDuration(quickAni)
                                .setListener(new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        super.onAnimationEnd(animation);
                                        onClick(view);
                                        if (dialog != null) {
                                            dialog.onBackPressed();
                                        }
                                    }
                                });
                    }
                });
    }

    /**
     * Run an animation only when a button in the BottomSheetDialog is clicked
     *
     * @param view The button view
     */
    private void onClickAnimation(View view) {
        onClickAnimation(null, view);
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
        if (adapterView.getId() == R.id.action_text_font) {
            action_text_font(i);
        }
    }

    /**
     * An event triggered when the user cancel the spinner popup
     * <i>This event is ignored</i>
     *
     * @param adapterView The adapter of the spinner in question
     */
    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {}

    /**
     * Perform a series of actions after any input event:
     * <ul>
     *   <li>Add a step to the undo history</li>
     *   <li>Save/Update the note to SQLite</li>
     * </ul>
     *
     * @param undo       Name of the undo step
     * @param drawableId The id for the icon of the undo step (-1 for default icon)
     */
    private void onPostInput(String undo, @DrawableRes int drawableId) {
        if (!TextUtils.isEmpty(undo)) {
            if (!editTextTitle.getText().toString().equals(note.title) &&
                    !editText.getText().toString().equals(note.text) &&
                    bgColor != note.bgColor) {
                if (drawableId != -1) {
                    undoManager.add(undo, drawableId);
                } else {
                    undoManager.add(undo);
                }
            }
        }
        saveNote();
    }

    /**
     * Run {@link EditorActivity#onPostInput(String, int)} with the default drawableId
     *
     * @param undo Name of the undo step
     */
    private void onPostInput(String undo) {
        onPostInput(undo, -1);
    }

    // endregion

    // region Handling text selection

    /**
     * Get the position of selected text in editText to be used in format-related functions
     */
    private void getTextSelection() {
        if (!editText.isFocused()) {
            textSelectionStart = editText.length();
            textSelectionEnd = editText.length();
            return;
        } else if (editText.length() == 0) {
            textSelectionStart = 0;
            textSelectionEnd = 0;
            return;
        }

        // Get selection
        textSelectionStart = editText.getSelectionStart();
        textSelectionEnd = editText.getSelectionEnd();
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

    // endregion

    // region Handling soft keyboard

    /**
     * Hide the soft keyboard
     *
     * @see <a href="https://stackoverflow.com/a/17789187">
     * How do you close/hide the Android soft keyboard programmatically? - StackOverflow
     * </a>
     */
    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE);
        View v = this.getCurrentFocus();
        if (v == null) {
            v = new View(this);
        }
        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }

    /**
     * Show the soft keyboard
     *
     * @see <a href="https://stackoverflow.com/a/34306427">
     * Android - show keyboard programmatically - StackOverflow
     * </a>
     */
    private void showKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE);
        View v = this.getCurrentFocus();
        if (v == null) {
            v = new View(this);
        }
        imm.showSoftInput(v, 0);
    }

    // endregion

    /**
     * Shows a BottomSheetDialog with the specified layout
     *
     * @param layoutId The ID of the layout
     * @param data     Additional data if the layout is used by multiple functions
     */
    @SuppressLint("NonConstantResourceId")
    private void showBottomDialog(@LayoutRes int layoutId, String data) {
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
                case R.layout.layout_undo_redo:
                    layout_undo_redo(dialog, data);
                    break;
            }
        } catch (Throwable e) {
            // Don't show the dialog if there is any error
            Log.e(TAG, "showBottomDialog: " + e.getMessage());
            return;
        }

        dialog.show();
    }

    private void showBottomDialog(@LayoutRes int layoutId) {
        showBottomDialog(layoutId, "");
    }

    // region Init dialogs in Bottom Appbar

    /**
     * Init elements in the Add Content dialog
     *
     * @param dialog The dialog in question
     * @throws Throwable Any error occurred
     */
    @SuppressWarnings("DuplicatedCode")
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
        action_add_camera.setOnClickListener(view -> onClickAnimation(dialog, view));
        action_add_image.setOnClickListener(view -> onClickAnimation(dialog, view));
        action_add_table.setOnClickListener(view -> onClickAnimation(dialog, view));
        action_add_codeblock.setOnClickListener(view -> onClickAnimation(dialog, view));
        action_add_link.setOnClickListener(view -> onClickAnimation(dialog, view));
        action_add_quote.setOnClickListener(view -> onClickAnimation(dialog, view));
        action_add_line.setOnClickListener(view -> onClickAnimation(dialog, view));
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
        action_text_font.setSelection(action_text_font_get(), false);
        action_text_font.setOnItemSelectedListener(this);

        // Align group
        ImageView action_align_active = dialog.findViewById(R.id.action_align_active);
        ImageButton action_align_start = dialog.findViewById(R.id.action_align_start),
                action_align_center = dialog.findViewById(R.id.action_align_center),
                action_align_end = dialog.findViewById(R.id.action_align_end);
        if (action_align_active == null || action_align_start == null ||
                action_align_center == null || action_align_end == null) {
            throw new Throwable("Missing button in Align group of the Format Style dialog");
        }
        float iconSize = getResources().getDimension(R.dimen.rounded_layout_icon_size);
        action_align_start.setOnClickListener(view -> {
            int type = action_align_tag_exists();
            if (type == 1 || type == 2) {
                action_align_active.animate()
                        .translationXBy(iconSize * ((type == 1) ? -1 : -2))
                        .setDuration((type == 1) ? quickAni : normalAni)
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                super.onAnimationEnd(animation);
                                onClick(view);
                            }
                        });
            }
        });
        action_align_center.setOnClickListener(view -> {
            int type = action_align_tag_exists();
            if (type != 1) {
                action_align_active.animate()
                        .translationXBy(iconSize * ((type == 2) ? -1 : 1))
                        .setDuration(quickAni)
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                super.onAnimationEnd(animation);
                                onClick(view);
                            }
                        });
            }
        });
        action_align_end.setOnClickListener(view -> {
            int type = action_align_tag_exists();
            if (type != 2) {
                action_align_active.animate()
                        .translationXBy(iconSize * ((type == 1) ? 1 : 2))
                        .setDuration((type == 1) ? quickAni : normalAni)
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                super.onAnimationEnd(animation);
                                onClick(view);
                            }
                        });
            }
        });
        // - Update the active alignment state
        int type = action_align_tag_exists();
        if (type == 1) {
            action_align_active.setX(iconSize);
        } else if (type == 2) {
            action_align_active.setX(iconSize * 2);
        }

        // List Style group
        ImageButton action_bullet = dialog.findViewById(R.id.action_bullet),
                action_number = dialog.findViewById(R.id.action_number);
        if (action_bullet == null || action_number == null) {
            throw new Throwable("Missing button in List Style group of the Format Style dialog");
        }
        action_bullet.setOnClickListener(this::onClickAnimation);
        action_number.setOnClickListener(this::onClickAnimation);

        // Text Style group
        ImageButton action_format_bold = dialog.findViewById(R.id.action_format_bold),
                action_format_italic = dialog.findViewById(R.id.action_format_italic),
                action_format_underline = dialog.findViewById(R.id.action_format_underline),
                action_format_strikethrough = dialog.findViewById(R.id.action_format_strikethrough);
        if (action_format_bold == null || action_format_italic == null || action_format_underline == null || action_format_strikethrough == null) {
            throw new Throwable("Missing button in Text Style group of the Format Style dialog");
        }
        action_format_bold.setOnClickListener(this::onClickAnimation);
        action_format_italic.setOnClickListener(this::onClickAnimation);
        action_format_underline.setOnClickListener(this::onClickAnimation);
        action_format_strikethrough.setOnClickListener(this::onClickAnimation);

        // Script group
        ImageButton action_format_superscript = dialog.findViewById(R.id.action_format_superscript),
                action_format_subscript = dialog.findViewById(R.id.action_format_subscript);
        if (action_format_superscript == null || action_format_subscript == null) {
            throw new Throwable("Missing button in Script group of the Format Style dialog");
        }
        action_format_superscript.setOnClickListener(this::onClickAnimation);
        action_format_subscript.setOnClickListener(this::onClickAnimation);

        // Indent group
        ImageButton action_format_indent_increase = dialog.findViewById(R.id.action_format_indent_increase),
                action_format_indent_decrease = dialog.findViewById(R.id.action_format_indent_decrease);
        if (action_format_indent_increase == null || action_format_indent_decrease == null) {
            throw new Throwable("Missing button in Indent group of the Format Style dialog");
        }
        action_format_indent_increase.setOnClickListener(this::onClickAnimation);
        action_format_indent_decrease.setOnClickListener(this::onClickAnimation);
    }

    /**
     * Init elements in the Text Color dialog
     *
     * @param dialog The dialog in question
     * @throws Throwable Any error occurred
     */
    @SuppressWarnings("DuplicatedCode")
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
        MaterialButton action_reset = dialog.findViewById(R.id.action_reset);
        if (action_color_red == null || action_color_orange == null || action_color_yellow == null ||
                action_color_green == null || action_color_tortoise == null || action_color_cyan == null ||
                action_color_blue == null || action_color_violet == null || action_color_purple == null ||
                action_color_magenta == null || action_color_pink == null || action_color_black == null ||
                action_color_lightgray == null || action_color_light == null || action_color_white == null ||
                action_reset == null) {
            throw new Throwable("Missing button in Format Color dialog");
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
        action_reset.setOnClickListener((view) -> setTextColor(R.color.transparent));
    }

    /**
     * Init elements in the Background dialog
     *
     * @param dialog The dialog in question
     * @throws Throwable Any error occurred
     */
    @SuppressWarnings("DuplicatedCode")
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
        MaterialButton action_reset = dialog.findViewById(R.id.action_reset);
        if (action_color_lightred == null || action_color_lightorange == null || action_color_lightyellow == null ||
                action_color_lightgreen == null || action_color_lighttortoise == null || action_color_lightcyan == null ||
                action_color_lightblue == null || action_color_lightviolet == null || action_color_lightpurple == null ||
                action_color_lightmagenta == null || action_color_lightpink == null || action_color_lightgray == null ||
                action_color_lightergray == null || action_color_light == null || action_color_white == null ||
                action_reset == null) {
            throw new Throwable("Missing button in Format Background dialog");
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

        // Get default bgColor
        if (bgColor == oldBgColor) {
            action_reset.setVisibility(View.GONE);
        } else {
            action_reset.setOnClickListener((view) -> setBackground(dialog, oldBgColor));
        }
    }

    /**
     * Init elements in the Undo/Redo dialog
     *
     * @param dialog The dialog in question
     * @param type   The type of dialog (undo/redo)
     * @throws Throwable Any error occurred
     */
    private void layout_undo_redo(BottomSheetDialog dialog, String type) throws Throwable {
        if (!type.equals("undo") && !type.equals("redo")) {
            throw new Throwable("Invalid dialog type: " + type);
        }

        RecyclerView recyclerView = dialog.findViewById(R.id.recyclerView);
        UndoAdapter adapter = new UndoAdapter(this, dialog, undoManager, type);
        if (recyclerView == null) {
            throw new Throwable("Missing recyclerView in Undo/Redo dialog");
        }
        recyclerView.setAdapter(adapter);
    }

    // endregion

    // region Button actions in Add Content dialog

    /**
     * Put a table after the current line
     */
    private void action_add_table() {
        int endOfLine = getEndOfLine(editText.getText().toString(), textSelectionStart);
        Editable newString = editText.getText();
        String tablePreset =
                "\n\nColumn 1 | Column 2 | Column 3\n" +
                        "--- | --- | ---\n" +
                        "Line 1 |  | \n";
        newString.insert(endOfLine, tablePreset);
        textSelectionStart += 2;
        editText.setSelection(textSelectionStart, textSelectionStart + "Column 1".length());
        showKeyboard();
    }

    /**
     * Put lines in between a new codeblock
     */
    private void action_add_codeblock() {
        int startOfLine = getStartOfLine(editText.getText().toString(), textSelectionStart),
                endOfLine = getEndOfLine(editText.getText().toString(), textSelectionStart);

        // Put ``` before and after content
        // 1. If at the start of line: Insert codeblock above
        // 2. If at the end of line: Append code block below
        // 3. If cursor in somewhere in the line: Surround the line with a codeblock
        Editable newString = editText.getText();
        int contentCase;
        if (textSelectionStart == textSelectionEnd) {
            if (textSelectionStart == startOfLine) {
                contentCase = 1;
            } else if (textSelectionStart == endOfLine) {
                contentCase = 2;
            } else {
                contentCase = 3;
            }
        } else {
            contentCase = 3;
        }
        switch (contentCase) {
            case 1:
                newString.insert(startOfLine, "```\n\n```\n");
                textSelectionStart = textSelectionStart + 4;
                textSelectionEnd = textSelectionStart;
                break;
            case 2:
                newString.insert(endOfLine, "\n```\n\n```");
                textSelectionStart = textSelectionStart + 5;
                textSelectionEnd = textSelectionStart;
                break;
            case 3:
                newString.insert(endOfLine, "\n```");
                newString.insert(startOfLine, "```\n");
                textSelectionStart = startOfLine + 4;
                textSelectionEnd = textSelectionStart + (endOfLine - startOfLine);
                break;
        }

        // Update new cursor position
        // - Retain original cursor position (or Put cursor at the end of selection)
        // - The string "\n```\n" has 5 characters -> offset by 5 chars
        editText.setSelection(textSelectionStart, textSelectionEnd);
//        editText.postDelayed(() -> editText.setSelection(textSelectionStart, textSelectionEnd), 50);
    }

    /**
     * Insert a new link or make a selection as a link
     */
    private void action_add_link() {
        Editable newString = editText.getText();
        if (textSelectionStart == textSelectionEnd) {
            // If nothing is selected, insert a link preset
            newString.insert(textSelectionStart, "[Link name](link)");
            textSelectionStart++;
            textSelectionEnd = textSelectionStart + "Link name".length();
        } else {
            // Use selected text as Link name
            newString.insert(textSelectionEnd, "](link)");
            newString.insert(textSelectionStart, "[");
            textSelectionStart++;
            textSelectionEnd++;
        }
        showKeyboard();
        editText.setSelection(textSelectionStart, textSelectionEnd);
    }

    /**
     * Put a quote symbol at the start of line
     */
    private void action_add_quote() {
        int startOfLine = getStartOfLine(editText.getText().toString(), textSelectionStart);
        Editable newString = editText.getText();
        newString.insert(startOfLine, " > ");
    }

    /**
     * Put a horizontal line after the current line
     */
    private void action_add_line() {
        int endOfLine = getEndOfLine(editText.getText().toString(), textSelectionStart);
        Editable newString = editText.getText();
        newString.insert(endOfLine, "\n***\n");
    }

    // endregion

    // region Button actions in Format Style dialog

    /**
     * Insert/Remove font symbols at the start (and at the end depending on the font)
     *
     * @param fontId The id of the font as specified in
     *               {@link EditorActivity#textFonts}
     */
    private void action_text_font(int fontId) {
        // Skip if the user selected the same font
        int oldFontId = action_text_font_get();
        if (fontId == oldFontId) {
            return;
        }

        String newString = editText.getText().toString();
        int startOfLine = getStartOfLine(newString, textSelectionStart),
                endOfLine = getEndOfLine(newString, textSelectionStart);
        String font = textFonts.get(fontId).split(" ")[0];
        boolean noReset = font.equals("`") ||
                (textFonts.get(oldFontId).contains("`") && font.equals("Normal"));

        // Add font symbols
        CharSequence line;
        Editable lineEditable;
        if (!noReset) {
            lineEditable = action_text_font_reset();
            line = lineEditable;
        } else {
            line = newString.subSequence(startOfLine, endOfLine);
            lineEditable = Editable.Factory.getInstance().newEditable(line);
        }
        switch (font) {
            case "#":
                // Heading 1
            case "##":
                //Heading 2
            case "###":
                //Heading 3
            case "####":
                //Heading 4
            case "#####":
                //Heading 5
            case "######":
                //Heading 6
                lineEditable.insert(0, font + " ");
                textSelectionStart += font.length() + 1;
                textSelectionEnd += font.length() + 1;
                break;
            case "`":
                // Monospace
                if (textSelectionStart == textSelectionEnd) {
                    // If nothing is selected
                    lineEditable.insert(textSelectionStart, "``");
                    textSelectionStart++;
                    textSelectionEnd = textSelectionStart;
                } else {
                    // Put selection between a pair of symbols
                    lineEditable.insert(textSelectionEnd, "`");
                    lineEditable.insert(textSelectionStart, "`");
                    textSelectionStart++;
                    textSelectionEnd++;
                }
                break;
            default:
                // Normal
                // - Remove font symbols if oldFontId is not belong to a line-wide font
                if (noReset) {
                    String oldFont = textFonts.get(oldFontId).split(" ")[0],
                            lineString = line.toString();
                    int oldFontStart = lineString.substring(0, textSelectionStart).lastIndexOf(oldFont),
                            oldFontEnd = lineString.indexOf(oldFont, textSelectionStart);
                    lineEditable.replace(oldFontEnd, oldFontEnd + 1, "");
                    lineEditable.replace(oldFontStart, oldFontStart + 1, "");
                    textSelectionStart--;
                    textSelectionEnd--;
                }
                break;
        }
        newString = newString.substring(0, startOfLine) +
                lineEditable +
                newString.substring(endOfLine);
        editText.setText(newString);

        // Update new cursor position
        editText.setSelection(textSelectionStart, textSelectionEnd);
    }

    /**
     * Remove the line-wide font symbol if it does not match the given symbol
     *
     * @return The new line as an Editable object
     */
    @SuppressWarnings("DuplicatedCode")
    private Editable action_text_font_reset() {
        String newString = editText.getText().toString();
        int startOfLine = getStartOfLine(newString, textSelectionStart),
                endOfLine = getEndOfLine(newString, textSelectionStart);
        ArrayList<String> symbols = new ArrayList<>();
        for (String s : textFonts) {
            symbols.add(s.split(" ")[0]);
        }

        // Get current line (filtering out the leading spaces)
        String line = newString.substring(startOfLine, endOfLine).trim(),
                lineSym = line.split(" ")[0];
        Editable lineEditable = Editable.Factory.getInstance().newEditable(line);

        // Return the new line without the symbol
        if (!symbols.contains(lineSym)) {
            return lineEditable;
        }
        textSelectionStart -= lineSym.length() + 1;
        textSelectionEnd -= lineSym.length() + 1;
        return lineEditable.replace(0, lineSym.length() + 1, "");
    }

    /**
     * Detect the current text font
     *
     * @return The id of the font as specified in
     * {@link EditorActivity#textFonts}
     */
    @SuppressWarnings("DuplicatedCode")
    private int action_text_font_get() {
        String newString = editText.getText().toString();
        int startOfLine = getStartOfLine(newString, textSelectionStart),
                endOfLine = getEndOfLine(newString, textSelectionStart);
        ArrayList<String> symbols = new ArrayList<>();
        for (String s : textFonts) {
            symbols.add(s.split(" ")[0]);
        }

        // Check for line-wide symbols
        String line = newString.substring(startOfLine, endOfLine).trim(),
                lineSym = line.split(" ")[0];
        if (symbols.contains(lineSym)) {
            return symbols.indexOf(lineSym);
        }

        // Check for selection-wide symbols
        if (textSelectionStart == textSelectionEnd) {
            if (action_format_style_tag_exists("`", textSelectionStart, textSelectionStart) > 0) {
                return symbols.indexOf("`");
            }
        } else {
            if (action_format_style_tag_exists("`", textSelectionStart, textSelectionEnd) > 0) {
                return symbols.indexOf("`");
            }
        }

        return textFonts.indexOf("Normal");
    }

    /**
     * Insert/remove a pair of alignment tags surrounding the line with the typing cursor
     *
     * @param type Type of alignment:
     *             <ul>
     *               <li><b>start</b>: Align left</li>
     *               <li><b>center</b>: Align center</li>
     *               <li><b>end</b>: Align end</li>
     *               <li><b>anything else</b>: Treated as Align start</li>
     *             </ul>
     */
    private void action_align(String type) {
        int startOfLine = getStartOfLine(editText.getText().toString(), textSelectionStart),
                endOfLine = getEndOfLine(editText.getText().toString(), textSelectionStart);
        if (type.equals("")) {
            type = "start";
        }
        String openTag = "<align " + type + ">",
                endTag = "</align>";

        // Get current line
        String newString = editText.getText().toString();
        CharSequence line = newString.substring(startOfLine, endOfLine);
        Editable lineEditable = Editable.Factory.getInstance().newEditable(line);

        // Align current line
        // - If already aligned, delete the old tags if type is different
        // - If type is the same, return
        // - Add a pair of align tags only if the type is center or end
        int openTagStart = 0, openTagEnd = 0, endTagStart, endTagEnd = line.length();
        if (action_align_tag_exists(line)) {
            openTagEnd = line.toString().indexOf(">") + 1;
            endTagStart = line.toString().lastIndexOf("</");
            if (line.subSequence(openTagStart, openTagEnd).toString().contains(type)) {
                return;
            } else {
                lineEditable.replace(endTagStart, endTagEnd, "");
                lineEditable.replace(openTagStart, openTagEnd, "");
            }
        }
        if (type.equals("center") || type.equals("end")) {
            lineEditable.insert(0, openTag);
            lineEditable.insert(lineEditable.length(), endTag);
        } else {
            openTag = "";
        }
        newString = newString.substring(0, startOfLine) +
                lineEditable +
                newString.substring(endOfLine);
        editText.setText(newString);

        // Update new cursor position and show keyboard
        textSelectionStart = textSelectionStart - (openTagEnd - openTagStart) + openTag.length();
        textSelectionEnd = textSelectionEnd - (openTagEnd - openTagStart) + openTag.length();
        showKeyboard();
        editText.setSelection(textSelectionStart, textSelectionEnd);
    }

    /**
     * Check if the given line is surrounded by a pair of alignment tags
     *
     * @param line The line is question
     * @return If it is surrounded by said tags
     */
    private boolean action_align_tag_exists(CharSequence line) {
        Pattern pattern = Pattern.compile(getString(R.string.regex_align_tags));
        Matcher matcher = pattern.matcher(line);
        return matcher.matches();
    }

    /**
     * Check which pair of alignment tags are surrounding the current line under the cursor
     *
     * @return The typeId of alignment tags (start: 0, center: 1, end: 2)
     */
    private int action_align_tag_exists() {
        int startOfLine = getStartOfLine(editText.getText().toString(), textSelectionStart),
                endOfLine = getEndOfLine(editText.getText().toString(), textSelectionStart);
        CharSequence line = editText.getText().subSequence(startOfLine, endOfLine);
        if (action_align_tag_exists(line)) {
            int openTagStart = 0, openTagEnd = line.toString().indexOf(">") + 1;
            String lineString = line.subSequence(openTagStart, openTagEnd).toString();
            if (lineString.contains("center")) {
                return 1;
            } else if (lineString.contains("end")) {
                return 2;
            }
        }
        return 0;
    }

    /**
     * Insert/remove a pair of format symbols at the cursor position, or put/remove them between a selection
     *
     * @param type Type of format:
     *             <ul>
     *               <li><b>b</b>: Bold <b>Text</b></li>
     *               <li><b>i</b>: Italic <i>Text</i></li>
     *               <li><b>u</b>: Underline <u>Text</u></li>
     *               <li><b>s</b>: Strikethrough <s>Text</s></li>
     *               <li><b>^</b>: Superscript <sup>Text</sup></li>
     *               <li><b>_</b>: Subscript <sub>Text</sub></li>
     *             </ul>
     */
    private void action_format_style(char type) {// Get the format tags based on type
        String format = "";
        switch (type) {
            case 'b':
            case 'i':
            case 'u':
            case 's':
                format = String.valueOf(type);
                break;
            case '^':
                format = "sup";
                break;
            case '_':
                format = "sub";
                break;
        }
        String openTag = "<" + format + ">",
                endTag = "</" + format + ">";

        // Start formatting
        Editable newString = editText.getText();
        int existingTag;
        if (textSelectionStart == textSelectionEnd) {
            // If nothing is selected
            // - If the cursor is not surrounded by similar format tags, insert them
            // - If it is surrounded, remove them
            existingTag = action_format_style_tag_exists(format, textSelectionStart, textSelectionStart);
            if (existingTag <= 0) {
                newString.insert(textSelectionStart, openTag + endTag);
            } else {
                openTag = "";
                newString.replace(textSelectionStart, textSelectionStart + existingTag + ((existingTag < 3) ? 0 : 1), "");
                newString.replace(textSelectionStart - existingTag, textSelectionStart, "");
                textSelectionStart -= existingTag;
            }
            textSelectionEnd = textSelectionStart;
        } else {
            // Apply format to selection
            // - If the selection is not surrounded by a similar format, insert them
            // - If it is surrounded, remove them
            existingTag = action_format_style_tag_exists(format, textSelectionStart, textSelectionEnd);
            if (existingTag <= 0) {
                newString.insert(textSelectionStart, openTag);
                newString.insert(textSelectionEnd + openTag.length(), endTag);
            } else {
                openTag = "";
                newString.replace(textSelectionEnd, textSelectionEnd + existingTag + ((existingTag < 3) ? 0 : 1), "");
                newString.replace(textSelectionStart - existingTag, textSelectionStart, "");
                textSelectionStart -= existingTag;
                textSelectionEnd -= existingTag;
            }
        }
        int selection = textSelectionEnd - textSelectionStart;
        textSelectionStart += openTag.length();
        textSelectionEnd = textSelectionStart + selection;

        // Update new cursor position and show keyboard
        // - Retain original cursor position (or Put cursor at the end of selection)
        // - Offset by the length of opening format tag
        showKeyboard();
        editText.setSelection(textSelectionStart, textSelectionEnd);
    }

    /**
     * Check if the given range is surrounded by a format tag
     *
     * @param format Format tag type
     * @param start  Start of range
     * @param end    End of range
     * @return The length of the open tag:
     * <ul>
     *   <li><b>0</b>: No tags found</li>
     *   <li><b>< 3</b>: A Markdown tag</li>
     *   <li><b>>= 3</b>: A HTML tag</li>
     * </ul>
     */
    private int action_format_style_tag_exists(String format, int start, int end) {
        String str = editText.getText().toString();
        String openTag = "<" + format + ">",
                endTag = "</" + format + ">",
                mdSymbol = "", mdSymbolAlt = "";
        if (start > end) {
            return 0;
        }
        if (format.chars().allMatch(Character::isLetter)) {
            if (str.startsWith(openTag, start - openTag.length()) &&
                    str.startsWith(endTag, end)) {
                return openTag.length();
            }
        }
        switch (format) {
            case "b":
                mdSymbol = "**";
                mdSymbolAlt = "__";
                break;
            case "i":
                mdSymbol = "*";
                mdSymbolAlt = "_";
                break;
            case "s":
                mdSymbol = "~~";
                mdSymbolAlt = "~~";
                break;
            case "`":
                mdSymbol = "`";
                mdSymbolAlt = "`";
                break;
        }
        if (!mdSymbol.equals("")) {
            if ((str.startsWith(mdSymbol, start - mdSymbol.length()) && str.startsWith(mdSymbol, end)) ||
                    (str.startsWith(mdSymbolAlt, start - mdSymbolAlt.length()) && str.startsWith(mdSymbolAlt, end)))
                return mdSymbol.length();
        }
        return 0;
    }

    /**
     * Increase/decrease 1 level of indent at the start of line
     *
     * @param increasing Will this function increase the indent? (decrease if not)
     */
    private void action_format_indent(boolean increasing) {
        int startOfLine = getStartOfLine(editText.getText().toString(), textSelectionStart),
                endOfLine = getEndOfLine(editText.getText().toString(), textSelectionStart),
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
        editText.setSelection(textSelectionStart + cursorMoveAmount);
    }

    // endregion

    // region Button actions in Format Color dialog

    /**
     * Insert/change a pair of color tags surrounding the selection or cursor
     *
     * @param colorId The id of the color as specified in
     *                {@link R.color#red colors.xml}
     */
    private void setTextColor(@ColorRes int colorId) {
        int startOfLine = getStartOfLine(editText.getText().toString(), textSelectionStart),
                endOfLine = getEndOfLine(editText.getText().toString(), textSelectionStart);
        String color;
        try {
            color = getResources().getResourceName(colorId).split("/")[1];
        } catch (Resources.NotFoundException e) {
            color = "transparent";
        }
        String openTag = "<color " + color + ">",
                endTag = "</color>";

        // Get current line
        String newString = editText.getText().toString();
        CharSequence line = newString.substring(startOfLine, endOfLine);
        Editable lineEditable = Editable.Factory.getInstance().newEditable(line);

        // Apply color to selection, or add a pair of tags at cursor position
        // - If color is transparent, remove tags
        // - If already colored, change the color
        // - If color is the same, return
        int start, end;
        if (textSelectionStart == textSelectionEnd) {
            start = textSelectionStart - startOfLine;
            end = textSelectionStart - startOfLine;
        } else {
            start = textSelectionStart - startOfLine;
            end = textSelectionEnd - startOfLine;
        }
        CharSequence selection = line.subSequence(start, end);
        String oldColor = textColorTagExists(line.toString(), start, end);
        if (oldColor.isEmpty()) {
            if (color.equals("transparent")) {
                return;
            }
            lineEditable.insert(end, endTag);
            lineEditable.insert(start, openTag);
            textSelectionStart = start + openTag.length();
            textSelectionEnd = textSelectionStart + selection.length();
        } else if (!oldColor.equals(color)) {
            String lineString = line.toString();
            int openTagStart = lineString.substring(0, start).lastIndexOf("<"),
                    endTagEnd = lineString.substring(end).indexOf(">") + 1 + end;
            if (color.equals("transparent")) {
                openTag = "";
                endTag = "";
            }
            lineEditable.replace(end, endTagEnd, endTag);
            lineEditable.replace(openTagStart, start, openTag);
            textSelectionStart = start - (start - openTagStart) + openTag.length();
            textSelectionEnd = textSelectionStart + selection.length();
        }
        newString = newString.substring(0, startOfLine) +
                lineEditable +
                newString.substring(endOfLine);
        editText.setText(newString);
        textSelectionStart += startOfLine;
        textSelectionEnd += startOfLine;

        // Update new cursor position and show keyboard
        showKeyboard();
        editText.setSelection(textSelectionStart, textSelectionEnd);
    }

    /**
     * Check if the given range of a given string is surrounded by a pair of color tags
     *
     * @param string The string in question
     * @param start  Start of range
     * @param end    End of range
     * @return The color of these tags (return an empty string if not surrounded)
     */
    private String textColorTagExists(String string, int start, int end) {
        int openTagStart = string.substring(0, start).lastIndexOf("<"),
                endTagEnd = string.substring(end).indexOf(">") + 1 + end;
        if (openTagStart < 0 || start < 0 || end < 0) {
            return "";
        }

        // Check if open and end tag valid
        String openTag = string.substring(openTagStart, start),
                endTag = string.substring(end, endTagEnd);
        Pattern openTagPattern = Pattern.compile(getString(R.string.regex_color_open_tag)),
                endTagPattern = Pattern.compile(getString(R.string.regex_color_end_tag));
        Matcher openTagMatcher = openTagPattern.matcher(openTag),
                endTagMatcher = endTagPattern.matcher(endTag);

        if (openTagMatcher.matches() && endTagMatcher.matches()) {
            return openTag.replace("<", "")
                    .replace(">", "")
                    .replace("color", "")
                    .trim().split(" ")[0];
        }
        return "";
    }

    // endregion

    // region Button actions in Format Background dialog

    /**
     * Set the background of the note
     *
     * @param colorId The id of the note as specified in
     *                {@link com.lexisnguyen.quicknotie.R.color#black values/colors.xml}
     */
    @SuppressWarnings({"ConstantConditions", "deprecation"})
    private void setBackground(@ColorRes int colorId) {
        // Set background color
        bgColor = colorId;
        window.setStatusBarColor(getColor(colorId));
        toolbar.setBackgroundColor(getColor(colorId));
        layout_root.setBackgroundColor(getColor(colorId));

        // Update icon color based on background color
        // - Get color and filter (?colorOnSecondary is the default color for icons)
        int iconColor, statusBarIconColor, hintTextColor;
        if (!isDarkMode(bgColor)) {
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
        // - Rebuild Markdown theme
        initMarkdown(this, bgColor);
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
        saveNote();
    }

    private static boolean isDarkMode(int bgColor) {
        return bgColor != R.color.lightgray && bgColor != R.color.lightergray;
    }

    // endregion

    // region Button actions in Top Toolbar

    /**
     * Toggle between Preview mode and Edit mode
     * <ul>
     *   <li><b>Preview mode</b>: View result of the markdown text</li>
     *   <li><b>Edit mode</b>: Make edits to the markdown text</li>
     * </ul>
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

        hideKeyboard();

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
            if (!isDarkMode(bgColor)) {
                menuItem.getIcon().setTint(getColor(R.color.white));
            } else {
                menuItem.getIcon().setTint(MaterialColors.getColor(layout_root, R.attr.colorOnSecondary));
            }
            menuItem.setTooltipText(getString(R.string.action_edit));
        } else {
            editTextTitle.setInputType(InputType.TYPE_CLASS_TEXT);

            textView.setVisibility(View.INVISIBLE);
            editText.setVisibility(View.VISIBLE);

            editText.setHint(R.string.info_title_hint);
            editTextTitle.setHint(R.string.info_text_hint);

            menuItem.setIcon(R.drawable.action_preview);
            if (!isDarkMode(bgColor)) {
                menuItem.getIcon().setTint(getColor(R.color.white));
            } else {
                menuItem.getIcon().setTint(MaterialColors.getColor(layout_root, R.attr.colorOnSecondary));
            }
            menuItem.setTooltipText(getString(R.string.action_preview));
        }
    }

    private void action_delete() {
        Trash trash = new Trash(note);
        trash.save();
        onBackPressed();
    }

    // endregion

    private void saveNote() {
        String title = editTextTitle.getText().toString(),
                text = editText.getText().toString();
        if (title.isEmpty() && text.isEmpty()) {
            return;
        }
        note.title = title;
        note.text = text;
        note.bgColor = bgColor;
        note.savedDate = Date.from(Instant.now());
        note.save();
    }

    /**
     * Start an action when user presses Back depending on the states of the app.
     */
    @Override
    public void onBackPressed() {
        if (textChangedTimer != null) {
            textChangedTimer.onFinish();
        }
        saveNote();
        super.onBackPressed();
        overridePendingTransition(
                R.anim.anim_null,
                R.anim.anim_slide_down_ease_out
        );
    }
}