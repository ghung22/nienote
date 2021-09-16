package com.lexisnguyen.quicknotie.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageButton;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.ColorRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.widget.NestedScrollView;
import androidx.customview.widget.ViewDragHelper;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.lexisnguyen.quicknotie.R;
import com.lexisnguyen.quicknotie.components.notes.NoteAdapter;
import com.lexisnguyen.quicknotie.components.settings.SettingsManager;
import com.lexisnguyen.quicknotie.components.sql.Note;
import com.lexisnguyen.quicknotie.components.sql.Trash;
import com.orm.SugarDb;
import com.orm.SugarRecord;
import com.thedeanda.lorem.Lorem;
import com.thedeanda.lorem.LoremIpsum;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;

@SuppressWarnings({"FieldCanBeLocal", "unused"})
public class MainActivity extends AppCompatActivity {
    // region Variables: GUI Elements
    // - DrawerLayout
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Button action_drawer_all, action_drawer_favorites, action_drawer_locked, action_drawer_trash;
    // - BottomAppBar
    private BottomAppBar bottomAppBar;
    private ImageButton action_show_menu, action_add_codeblock, action_add_image, action_settings;
    private FloatingActionButton fab;
    // - Content view
    private MaterialCardView materialCardView, materialCardViewSelection;
    private SearchView searchView;
    private ImageButton action_sort, action_order;
    private MaterialToolbar toolbar;
    private RecyclerView recyclerView;
    // endregion

    // region Variables: Data
    // - Activity results
    private ActivityResultLauncher<Intent> editorLauncher;
    private ActivityResultLauncher<Intent> settingsLauncher;
    private ActivityResult result;
    // - Settings
    private String app_theme;
    private boolean delete_permanently;
    // - SQLite
    public static SugarDb db;
    private List<Note> notes = new ArrayList<>();
    // private List<Favorite> favorite = new ArrayList<>();
    private List<Trash> trash = new ArrayList<>();
    private String currentFolder = "";
    private NoteAdapter adapter;
    // - Animation
    private final float bounceAmount = 20;
    private final int quickAni = 150;
    private final int normalAni = 300;
    // - Handling DataSet
    private int sort_type = SORT_DEFAULT;
    private boolean sort_order = DESCENDING;
    private int selectCountBefore = 0;
    public MutableLiveData<Integer> selectCount = new MutableLiveData<>(0);
    // endregion

    // region Variables: Constants
    // - Built-in folders
    public static final String FOLDER_FAVORITES = "favorites";
    public static final String FOLDER_LOCKED = "locked";
    public static final String FOLDER_TRASH = "trash";
    // - Sort order
    public static final int SORT_ID = 0;
    public static final int SORT_NAME = 1;
    public static final int SORT_COLOR = 2;
    public static final int SORT_SAVED_DATE = 3;
    public static final boolean ASCENDING = true;
    public static final boolean DESCENDING = false;
    public static final int SORT_DEFAULT = SORT_SAVED_DATE;
    public static final boolean ORDER_DEFAULT = DESCENDING;
    // - New note action
    public static final int ACTION_ADD_EMPTY = 0;
    public static final int ACTION_ADD_CODEBLOCK = 1;
    public static final int ACTION_ADD_IMAGE = 2;
    // - Open note action
    public static final int ACTION_OPEN_NOTE = 10;
    public static final int ACTION_OPEN_WITH = 11;
    // endregion

    // Debugging
    private final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initData();
        initActivityResults();
        initGuiElements();
        initDrawer();
        initBottomAppBar();
        initContentView();

        cd("/");
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (result != null) {
            updateDataSet();
            result = null;
        }
    }

    // region Init events

    /**
     * Init data from many sources
     */
    private void initData() {
        // From SQLite
        db = new SugarDb(this);
        db.onCreate(db.getDB());

        // From Settings
        // - Get settings
        SettingsManager settingsManager = new SettingsManager(this);
        app_theme = settingsManager.app_theme;
        delete_permanently = settingsManager.delete_permanently;
        // - Set theme
        switch (app_theme) {
            case "light":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case "dark":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case "system":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
            default:
                Log.w(TAG, "update_app_theme: Unknown theme " + app_theme);
                break;
        }

        // Handling DataSet
        selectCount.observe(this, integer -> {
            if (selectCountBefore == integer) {
                return;
            }
            int height = getActionBarHeight();

            // Hide search bar and show selection bar
            if (selectCountBefore == 0) {
                materialCardView.animate().translationYBy(-height).setDuration(quickAni);
                materialCardViewSelection.animate().translationYBy(height).setDuration(quickAni);
            }

            // Update toolbar content
            toolbar.setSubtitle(getString(R.string.info_selection, integer));

            // Hide selection bar and show search bar
            if (integer == 0) {
                materialCardViewSelection.animate().translationYBy(-height).setDuration(quickAni);
                materialCardView.animate().translationYBy(height).setDuration(quickAni);
            }

            selectCountBefore = integer;
        });
    }

    /**
     * Init (get) all views on the layouts by saving them into a variable
     */
    private void initGuiElements() {
        // DrawerLayout
        drawerLayout = findViewById(R.id.drawerLayout);
        ExpandableListView expandableListView = findViewById(R.id.expandableListView);
        navigationView = findViewById(R.id.navigationView);
        action_drawer_all = findViewById(R.id.action_drawer_all);
        action_drawer_favorites = findViewById(R.id.action_drawer_favorites);
        action_drawer_locked = findViewById(R.id.action_drawer_locked);
        action_drawer_trash = findViewById(R.id.action_drawer_trash);
        // Bottom Appbar
        bottomAppBar = findViewById(R.id.bottomAppBar);
        action_show_menu = findViewById(R.id.action_show_menu);
        action_add_codeblock = findViewById(R.id.action_add_codeblock);
        action_add_image = findViewById(R.id.action_add_image);
        action_settings = findViewById(R.id.action_settings);
        fab = findViewById(R.id.fab);
        // Content view
        materialCardView = findViewById(R.id.materialCardView);
        materialCardViewSelection = findViewById(R.id.materialCardViewSelection);
        searchView = findViewById(R.id.searchView);
        action_sort = findViewById(R.id.action_sort);
        action_order = findViewById(R.id.action_order);
        toolbar = findViewById(R.id.toolbar);
        recyclerView = findViewById(R.id.recyclerView);
    }

    /**
     * Init the side Drawer consisting the following menus
     * <ul>
     *   <li><b>ALL</b>: Show all notes according to a directory tree (excluding deleted notes)</li>
     *   <li><b>FAVORITES</b>: Show all liked notes</li>
     *   <li><b>LOCKED</b>: Show all notes that has a protective password</li>
     *   <li><b>TRASH</b>: Show all deleted notes</li>
     *   <br/>
     *   <li><b>FOLDERS</b>: List of folders in the root directory</li>
     *   <li><b>MANAGE FOLDERS</b>: Change the said folder list</li>
     * </ul>
     */
    private void initDrawer() {
        ExpandableListAdapter expandableListAdapter;
        expandDrawerTrigger();
        action_drawer_all.setBackgroundColor(MaterialColors.getColor(drawerLayout, R.attr.colorPrimary));
        action_drawer_all.setOnClickListener(this::onClick);
        action_drawer_favorites.setOnClickListener(this::onClick);
        action_drawer_locked.setOnClickListener(this::onClick);
        action_drawer_trash.setOnClickListener(this::onClick);
    }

    /**
     * Expand the area that the user can swipe to open the drawer.
     *
     * @see <a href="https://stackoverflow.com/a/19764654">
     * Touch anywhere to slide open menu for navigation drawer - StackOverflow
     * </a>
     */
    private void expandDrawerTrigger() {
        try {
            Field dragField = navigationView.getClass().getDeclaredField("mLeftDragger");
            dragField.setAccessible(true);
            ViewDragHelper dragHelper = (ViewDragHelper) dragField.get(navigationView);
            Field edgeSizeField = Objects.requireNonNull(dragHelper).getClass().getDeclaredField("mEdgeSize");
            edgeSizeField.setAccessible(true);
            int edge = edgeSizeField.getInt(dragHelper);
            edgeSizeField.setInt(dragHelper, edge * 2);
        } catch (Exception ignored) {
        }
    }

    /**
     * Init the BottomAppBar consisting the following buttons
     * <ul>
     *   <li><b>MENU</b>: Show drawer menu on the left/Go back to Notes</li>
     *   <li><b>CHECKLIST</b>: Add a note with an empty checklist</li>
     *   <li><b>ADD</b>: Add an empty note</li>
     *   <li><b>IMAGE</b>: Add a note with an image</li>
     *   <li><b>SETTINGS</b>: Show Settings screen</li>
     * </ul>
     */
    private void initBottomAppBar() {
        action_show_menu.setOnClickListener(this::onClick);
        action_add_codeblock.setOnClickListener(this::onClick);
        fab.setOnClickListener(this::onClick);
        fab.setOnLongClickListener((view) -> action_add_generate());
        action_add_image.setOnClickListener(this::onClick);
        action_settings.setOnClickListener(this::onClick);
    }

    /**
     * Init the layout consisting the Search bar and the Note list
     */
    private void initContentView() {
        adapter = new NoteAdapter(this, notes, editorLauncher, recyclerView);
        recyclerView.setAdapter(adapter);

        // Make search bar background transparent
        // REF: https://stackoverflow.com/a/41259257
        AppBarLayout appBarLayout = findViewById(R.id.appBarLayout);
        NestedScrollView scrollView = findViewById(R.id.nestedScrollView);
        scrollView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                scrollView.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                final int appBarHeight = appBarLayout.getHeight();
                scrollView.setTranslationY(-appBarHeight);
                scrollView.getLayoutParams().height = scrollView.getHeight() + appBarHeight;
            }
        });

        // Search
        materialCardView.setOnClickListener(this::onClick);
        searchView.setOnClickListener(this::onClick);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.search(newText);
                return true;
            }
        });

        // Sort
        action_sort.setOnClickListener(this::onClick);
        action_order.setOnClickListener(this::onClick);

        // Selection
        int height = getActionBarHeight();
        materialCardViewSelection.animate().translationYBy(-height).setDuration(0);
        toolbar.setNavigationOnClickListener(view -> {
            selectCount.setValue(0);
            adapter.clearSelection();
        });
        toolbar.getMenu().findItem(R.id.action_create_folder).getIcon()
                .setTint(MaterialColors.getColor(this, R.attr.colorPrimaryVariant, Color.WHITE));
        toolbar.getMenu().findItem(R.id.action_delete).getIcon()
                .setTint(MaterialColors.getColor(this, R.attr.colorPrimaryVariant, Color.WHITE));
        toolbar.getMenu().findItem(R.id.action_select_all).getIcon()
                .setTint(MaterialColors.getColor(this, R.attr.colorPrimaryVariant, Color.WHITE));
        toolbar.setOnMenuItemClickListener(this::onMenuItemClick);
    }

    private int getActionBarHeight() {
        TypedValue typedValue = new TypedValue();
        int actionBarHeight = 0, actionBarMargin = 0;
        if (getTheme().resolveAttribute(android.R.attr.actionBarSize, typedValue, true)) {
            actionBarHeight = TypedValue.complexToDimensionPixelSize(
                    typedValue.data, getResources().getDisplayMetrics());
            actionBarMargin = (int) (getResources().getDimension(R.dimen.card_margin)
                    / getResources().getDisplayMetrics().density);
        }
        return actionBarHeight + actionBarMargin * 4;
    }

    @SuppressWarnings("SameParameterValue")
    private void setToolbarVisibility(boolean action_delete,
                                      boolean action_create_folder,
                                      boolean action_select_all) {
        toolbar.getMenu().findItem(R.id.action_delete).setVisible(action_delete);
        toolbar.getMenu().findItem(R.id.action_create_folder).setVisible(action_create_folder);
        toolbar.getMenu().findItem(R.id.action_select_all).setVisible(action_select_all);
    }

    private void setAppBarVisibility(boolean action_add_codeblock,
                                     boolean fab,
                                     boolean action_add_image) {
        this.action_add_codeblock.setEnabled(action_add_codeblock);
        this.action_add_codeblock.setVisibility(action_add_codeblock ? View.VISIBLE : View.INVISIBLE);
        this.fab.setAlpha(fab ? 1 : .2f);
        this.action_add_image.setEnabled(action_add_image);
        this.action_add_image.setVisibility(action_add_image ? View.VISIBLE : View.INVISIBLE);
    }

    /**
     * Init the actions that will be performed after a child activity finishes
     * <p><i>(And after a transition animation is done)</i></p>
     *
     * @see <a href="https://stackoverflow.com/a/56265197">Calling startActivity with shared element transition
     * from onActivityResult - Stack Overflow</a>
     */
    private void initActivityResults() {
        // Save updated data and perform actions on them in onResume()
        editorLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> this.result = result
        );
    }

    // endregion

    // region Input events

    @SuppressLint("NonConstantResourceId")
    private void onClick(View view) {
        int viewId = view.getId();
        switch (viewId) {
            // DrawerLayout
            case R.id.action_drawer_all:
            case R.id.action_drawer_favorites:
            case R.id.action_drawer_locked:
            case R.id.action_drawer_trash:
                switch (viewId) {
                    case R.id.action_drawer_all:
                        cd("/");
                        break;
                    case R.id.action_drawer_favorites:
                        cd("/" + FOLDER_FAVORITES);
                        break;
                    case R.id.action_drawer_locked:
                        cd("/" + FOLDER_LOCKED);
                        break;
                    case R.id.action_drawer_trash:
                        cd("/" + FOLDER_TRASH);
                        break;
                }
                drawerLayout.closeDrawer(Gravity.START);
                break;

            // Bottom Appbar
            case R.id.action_show_menu:
                view.animate().translationXBy(bounceAmount).setDuration(quickAni)
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                super.onAnimationEnd(animation);
                                view.animate().translationXBy(-bounceAmount).setDuration(quickAni)
                                        .setListener(new AnimatorListenerAdapter() {
                                            @Override
                                            public void onAnimationEnd(Animator animation) {
                                                super.onAnimationEnd(animation);
                                                drawerLayout.openDrawer(Gravity.START);
                                            }
                                        });
                            }
                        });
                break;
            case R.id.fab:
                if (view.getAlpha() >= 1) {
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
                                                    action_add(ACTION_ADD_EMPTY);
                                                }
                                            });
                                }
                            });
                }
                break;
            case R.id.action_add_codeblock:
            case R.id.action_add_image:
                view.animate().translationYBy(-bounceAmount).setDuration(quickAni)
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                super.onAnimationEnd(animation);
                                if (viewId == R.id.action_add_codeblock) {
                                    action_add(ACTION_ADD_CODEBLOCK);
                                } else {
                                    action_add(ACTION_ADD_IMAGE);
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
            case R.id.action_settings:
                view.animate().translationXBy(-bounceAmount).setDuration(quickAni)
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                super.onAnimationEnd(animation);
                                view.animate().translationXBy(bounceAmount).setDuration(quickAni)
                                        .setListener(new AnimatorListenerAdapter() {
                                            @Override
                                            public void onAnimationEnd(Animator animation) {
                                                super.onAnimationEnd(animation);
                                                // Show settings
                                                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                                                startActivity(intent);
                                                overridePendingTransition(
                                                        R.anim.anim_slide_left_enter,
                                                        R.anim.anim_slide_left_leave
                                                );
                                            }
                                        });
                            }
                        });
                break;

            // Content view
            case R.id.materialCardView:
                searchView.requestFocus();
                break;
            case R.id.action_sort:
            case R.id.action_order:
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
                                                switch (viewId) {
                                                    case R.id.action_sort:
                                                        action_sort();
                                                        break;
                                                    case R.id.action_order:
                                                        action_order();
                                                        break;
                                                }
                                            }
                                        });
                            }
                        });
                break;

            default:
                Log.w(TAG, "OnClick: Unknown item " + view.getId());
                break;
        }
    }

    @SuppressLint("NonConstantResourceId")
    private boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_create_folder:
                break;
            case R.id.action_delete:
                break;
            case R.id.action_select_all:
                adapter.selectAll();
                break;
            default:
                Log.w(TAG, "OnMenuItemClick: Unknown item " + item.getTitle());
                return false;
        }
        return true;
    }

    // endregion

    // region Button actions in BottomAppBar

    /**
     * Create a new note, starting {@link EditorActivity} with a note preset, specified by the following action
     * codes:
     * <ul>
     * <li>{@link #ACTION_ADD_EMPTY}: An empty note.
     * <li>{@link #ACTION_ADD_CODEBLOCK}: A note with an empty checklist.
     * <li>{@link #ACTION_ADD_IMAGE}: A note with an image/drawing.
     * <li><b>Unknown</b>: Fallback to {@link #ACTION_ADD_EMPTY}.
     * </ul>
     *
     * @param action Which type of note the user wants to add.
     */
    private void action_add(int action) {
        if (action < ACTION_ADD_EMPTY || action > ACTION_ADD_IMAGE) {
            action = ACTION_ADD_EMPTY;
        }

        Intent intent = new Intent(this, EditorActivity.class);
        Bundle bundle = new Bundle();
        bundle.putInt("action", action);
        bundle.putString("folder", currentFolder);
        intent.putExtras(bundle);


        ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                this, findViewById(R.id.bottomAppBar), getString(R.string.transition_new_note));
        editorLauncher.launch(intent, options);
    }

    private boolean action_add_generate() {
        EditText input = new EditText(this);
        DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> {
            switch (which) {
                case DialogInterface.BUTTON_NEGATIVE:
                    // Clear first button
                    if (currentFolder.equals("/" + FOLDER_FAVORITES)) {
                        // Favorite.deleteAll(Favorite.class);
                    }
                    if (currentFolder.equals("/" + FOLDER_LOCKED)) {
                        // Locked.deleteAll(Locked.class);
                    }
                    if (currentFolder.equals("/" + FOLDER_TRASH)) {
                        Trash.deleteAll(Trash.class);
                    }
                    Note.deleteInTx(notes);
                    notes.clear();
                    adapter.notifyDataSetChanged(notes);
                case DialogInterface.BUTTON_POSITIVE:
                    // Generate button
                    int generateCount = 0;
                    if (!input.getText().toString().isEmpty()) {
                        generateCount = Integer.parseInt(input.getText().toString());
                    }
                    action_add_generate_start(Math.abs(generateCount));
                    break;
                default:
                    // Cancel button
                    break;
            }
        };

        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_NUMBER_FLAG_SIGNED);
        input.setHint("Note count");
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.action_add_generate)
                .setMessage("[DEBUGGING] Generate a specified number of notes")
                .setView(input)
                .setPositiveButton("Generate", dialogClickListener)
                .setNegativeButton("Clear first", dialogClickListener)
                .setNeutralButton("Cancel", dialogClickListener)
                .setCancelable(true)
                .show();
        input.requestFocus();
        return true;
    }

    private void action_add_generate_start(int generateCount) {
        for (int i = 0; i < generateCount; ++i) {
            Lorem lorem = LoremIpsum.getInstance();
            Random random = new Random();
            String title, text = "", folder = currentFolder;
            @ColorRes int bgColor;
            Date savedDate;

            // Generate content
            title = lorem.getWords(0, 5);
            if (random.nextBoolean()) {
                text += "# " + lorem.getWords(4) + "\n" +
                        "## " + lorem.getWords(4) + "\n";
            }
            text += lorem.getWords(8, 20) + "\n";
            if (random.nextBoolean()) {
                text += "Contact (<b>" + lorem.getName() + "</b>): <i>" + lorem.getPhone() + "</i>\n";
            }
            text += lorem.getParagraphs(0, 3) + "\n";
            if (random.nextBoolean()) {
                text += "<u>Important</u>: Visit " + lorem.getCity() + ", " +
                        lorem.getStateFull() + " before" + " <s>July</s> September!\n";
            }
            if (random.nextBoolean()) {
                text += "[Videos](https://youtu.be)\n" +
                        "[Games](https://y8.com)\n";
            }
            if (random.nextBoolean()) {
                text += "> 2<sup>2</sup> = 4\n" +
                        "> > 3<sup>2</sup> = 9\n\n" +
                        "(x<sub>1</sub> + x<sub>2</sub>)<sup>2</sup> = " +
                        "x<sub>1</sub><sup>2</sup> + x<sub>1</sub>x<sub>2</sub> + x<sub>2</sub><sup>2</sup>\n";
            }
            if (random.nextBoolean()) {
                text += "\n***\n" +
                        "# <align center>Time schedule</align>\n\n" +
                        "Monday | Tuesday | Wednesday\n" +
                        ":--- | :---: | ---:\n" +
                        lorem.getFirstName() + " | " + lorem.getFirstName() + " | " + lorem.getFirstName() + "\n" +
                        "15:00 | 21:00 | 09:00\n\n";
            }
            text += lorem.getParagraphs(0, 2) + "\n";
            if (random.nextBoolean()) {
                text += "\n***\n" +
                        "#### <align end>Python `print` code:</align>\n" +
                        "```python\n" +
                        "s = \"Python syntax highlighting\"\n" +
                        "print s\n" +
                        "```\n";
            }
            if (random.nextBoolean()) {
                text += "### Shopping list:\n" +
                        "1. Eggs\n" +
                        "2. Vegetables\n" +
                        "- Lettuce\n" +
                        "- Pumpkin\n" +
                        "- Onions\n";
            }

            // Generate bgColor
            List<String> colors = Arrays.asList(getResources().getStringArray(R.array.note_background_values));
            bgColor = getResources().getIdentifier(
                    colors.get(random.nextInt(colors.size())), "color", getPackageName());

            // Generate date between 1940 and 2021
            // REF: https://stackoverflow.com/a/3985467
            long ms = -946771200000L + (Math.abs(random.nextLong()) % (80L * 365 * 24 * 60 * 60 * 1000));
            savedDate = new Date(ms);
            if (folder.equals("/" + FOLDER_FAVORITES) ||
                    folder.equals("/" + FOLDER_LOCKED) ||
                    folder.equals("/" + FOLDER_TRASH)) {
                folder = "/";
            }
            Note note = new Note(folder, title, text, bgColor, savedDate);
            note.save();
            notes.add(note);

            // Handle special folders
            if (currentFolder.equals("/" + FOLDER_FAVORITES)) {
                // TODO: Add favorite entry
            }
            if (currentFolder.equals("/" + FOLDER_LOCKED)) {
                // TODO: Add locked entry
            }
            if (currentFolder.equals("/" + FOLDER_TRASH)) {
                Trash t = new Trash(note);
                t.save();
                trash.add(t);
            }
        }
        sort(sort_type, sort_order);
        adapter.notifyDataSetChanged(notes);
    }

    // endregion

    // region Button actions in Top Toolbar

    /**
     * Show a sort dialog letting the user chose one of these sort types
     * <ul>
     *   <li>By note ID</li>
     *   <li>By name</li>
     *   <li>By color</li>
     *   <li>By saved date</li>
     * </ul>
     */
    private void action_sort() {
        DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> {
            switch (which) {
                case DialogInterface.BUTTON_NEGATIVE:
                    // Default button
                    sort_type = SORT_DEFAULT;
                case DialogInterface.BUTTON_POSITIVE:
                    // Sort button
                    sort(sort_type, sort_order);
                    break;
                case DialogInterface.BUTTON_NEUTRAL:
                    // Cancel button
                    break;
                default:
                    // Option selected
                    sort_type = which;
                    break;
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        CharSequence[] choices = {
                getString(R.string.info_sort_id),
                getString(R.string.info_sort_name),
                getString(R.string.info_sort_color),
                getString(R.string.info_sort_saved_date)
        };
        builder.setTitle(R.string.info_sort_type)
                .setPositiveButton("Sort", dialogClickListener)
                .setNegativeButton("Default", dialogClickListener)
                .setNeutralButton("Cancel", dialogClickListener)
                .setCancelable(true)
                .setSingleChoiceItems(choices, sort_type, dialogClickListener)
                .show();
    }

    /**
     * Flip between these sort orders
     * <ul>
     *   <li>Ascending</li>
     *   <li>Descending</li>
     * </ul>
     */
    private void action_order() {
        action_order.animate().rotationXBy(180).setDuration(quickAni)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        sort_order = !sort_order;
                        sort(sort_type, sort_order);
                    }
                });
    }

    // endregion

    // region Handling DataSet

    /**
     * Change directory to a folder. Some built-in folders:
     * <ul>
     *   <li><b>/{@value #FOLDER_FAVORITES}</b>: A folder containing liked notes</li>
     *   <li><b>/{@value #FOLDER_LOCKED}</b>: A folder contains all notes with password</li>
     *   <li><b>/{@value #FOLDER_TRASH}</b>: A list of notes in trash</li>
     * </ul>
     *
     * @param folder The folder to change to
     */
    private void cd(String folder) {
        // Abort if already in folder
        if (currentFolder.equals(folder)) {
            return;
        }

        // Reset drawer active folder state
        View view;
        switch (currentFolder) {
            case "/" + FOLDER_FAVORITES:
                view = action_drawer_favorites;
                break;
            case "/" + FOLDER_LOCKED:
                view = action_drawer_locked;
                break;
            case "/" + FOLDER_TRASH:
                view = action_drawer_trash;
                break;
            default:
                view = action_drawer_all;
                break;
        }
        view.setBackgroundColor(getColor(R.color.transparent));

        // Set current folder and clear current note list
        currentFolder = folder;
        trash = Trash.listAll(Trash.class);
        notes.clear();

        // Get new note list & update UI based on new folder
        switch (currentFolder) {
            case "/" + FOLDER_FAVORITES:
                view = action_drawer_favorites;
                setToolbarVisibility(true, false, true);
                setAppBarVisibility(false, false, false);
                break;
            case "/" + FOLDER_LOCKED:
                view = action_drawer_locked;
                setToolbarVisibility(true, false, true);
                setAppBarVisibility(false, false, false);
                break;
            case "/" + FOLDER_TRASH:
                view = action_drawer_trash;
                notes = Note.listAll(Note.class);
                notes.removeIf(note ->
                        !trash.stream().map(t -> t.note.getId())
                                .collect(Collectors.toList())
                                .contains(note.getId()));
                setToolbarVisibility(true, false, true);
                setAppBarVisibility(false, false, false);
                break;
            default:
                view = action_drawer_all;
                notes = Note.find(Note.class, "folder = ?", currentFolder);
                notes.removeIf(note ->
                        trash.stream().map(t -> t.note.getId())
                                .collect(Collectors.toList())
                                .contains(note.getId()));
                setToolbarVisibility(true, true, true);
                setAppBarVisibility(true, true, true);
                break;
        }

        // Sort the new list
        sort(sort_type, sort_order);

        // Update drawer and note list view
        view.setBackgroundColor(MaterialColors.getColor(drawerLayout, R.attr.colorPrimary));
        new Handler(Looper.getMainLooper()).post(() -> adapter.notifyDataSetChanged(notes));
    }

    /**
     * Sort the note list according to a given order and attribute. Some supported attributes:
     * <ul>
     *   <li>{@link #SORT_ID}: By the default order in the database</li>
     *   <li>{@link #SORT_NAME}: Sort by name (alphabetical order)</li>
     *   <li>{@link #SORT_COLOR}: By color codes</li>
     *   <li>{@link #SORT_SAVED_DATE} (default): By how recent the note has been edited</li>
     * </ul>
     *
     * @param type The type of attribute to sort
     * @param asc  The order ({@link #ASCENDING} or {@link #DESCENDING})
     */
    private void sort(int type, boolean asc) {
        Comparator<Note> comparator;
        switch (type) {
            case SORT_ID:
                comparator = Comparator.comparing(SugarRecord::getId);
                break;
            case SORT_NAME:
                comparator = Comparator.comparing(n -> n.title);
                break;
            case SORT_COLOR:
                comparator = Comparator.comparing(n -> n.bgColor);
                break;
            case SORT_SAVED_DATE:
                comparator = Comparator.comparing(n -> n.savedDate);
                break;
            default:
                return;
        }
        if (!asc) {
            comparator = comparator.reversed();
        }
        notes.sort(comparator);
        new Handler(Looper.getMainLooper()).post(() -> adapter.notifyDataSetChanged(notes));
    }

    /**
     * Update the note list after editing
     */
    private void updateDataSet() {
        // Get updated note data
        if (result.getResultCode() != RESULT_OK) {
            return;
        }
        Intent data = result.getData();
        Bundle extras;
        if (data == null) {
            return;
        }
        extras = data.getExtras();
        if (!extras.containsKey("noteId")) {
            return;
        }

        // Update view
        long noteId = extras.getLong("noteId");
        String title = extras.getString("title"),
                text = extras.getString("text");
        int bgColor = extras.getInt("bgColor");
        // - Delete or change this note from view (if exists)
        for (Note note : notes) {
            if (note.getId() != noteId) {
                continue;
            }
            if (extras.containsKey("trashed")) {
                if (extras.getBoolean("trashed")) {
                    // Delete from view
                    adapter.notifyItemRemove(notes.indexOf(note));
                    // Delete from local data
                    if (currentFolder.equals("/" + FOLDER_TRASH)) {
                        notes.remove(note);
                    }
                    // Move to a folder
                    if (extras.containsKey("moved")) {
                        String moved = extras.getString("moved");
                        cd(moved);
                    }
                    return;
                }
            }
            adapter.notifyItemChange(notes.indexOf(note), title, text, bgColor);
            return;
        }
        // - Add a new note (if not exists)
        Note note = Note.findById(Note.class, noteId);
        notes.add(0, note);
        adapter.notifyItemInsert(noteId);
    }

    // endregion

    /**
     * Start an action when user presses Back depending on the states of the app.
     */
    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(Gravity.START)) {
            drawerLayout.closeDrawer(Gravity.START);
        } else {
            super.onBackPressed();
        }
    }
}