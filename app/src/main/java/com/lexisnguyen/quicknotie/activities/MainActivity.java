package com.lexisnguyen.quicknotie.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageButton;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.ActivityOptionsCompat;
import androidx.customview.widget.ViewDragHelper;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.lexisnguyen.quicknotie.R;
import com.lexisnguyen.quicknotie.components.notes.NoteAdapter;
import com.lexisnguyen.quicknotie.components.sql.Note;
import com.lexisnguyen.quicknotie.components.sql.Trash;
import com.orm.SugarDb;
import com.orm.SugarRecord;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@SuppressWarnings("FieldCanBeLocal")
public class MainActivity extends AppCompatActivity {
    // GUI Elements
    // - DrawerLayout
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Button action_drawer_all, action_drawer_favorites, action_drawer_locked, action_drawer_trash;
    // - BottomAppBar
    private BottomAppBar bottomAppBar;
    private ImageButton action_show_menu, action_add_codeblock, action_add_image, action_settings;
    private FloatingActionButton fab;
    // - Content view
    private MaterialCardView materialCardView;
    private SearchView searchView;
    private RecyclerView recyclerView;

    // Data
    // - Activity results
    private ActivityResultLauncher<Intent> editorLauncher;
    private ActivityResultLauncher<Intent> settingsLauncher;
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

    // Constants
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
    // - New note action
    public static final int ACTION_ADD_EMPTY = 0;
    public static final int ACTION_ADD_CODEBLOCK = 1;
    public static final int ACTION_ADD_IMAGE = 2;
    // - Open note action
    public static final int ACTION_OPEN_NOTE = 10;

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
        sort(SORT_ID, DESCENDING);
    }

    // region Init events

    /**
     * Init data from many sources
     */
    private void initData() {
        // From SQLite
        db = new SugarDb(this);
        db.onCreate(db.getDB());
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
        searchView = findViewById(R.id.searchView);
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
        action_add_image.setOnClickListener(this::onClick);
        action_settings.setOnClickListener(this::onClick);
    }

    /**
     * Init the layout consisting the Search bar and the Note list
     */
    private void initContentView() {
        adapter = new NoteAdapter(this, notes, editorLauncher);
        recyclerView.setAdapter(adapter);
    }

    private void initActivityResults() {
        editorLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
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
        );
    }

    // endregion

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
                action_add(ACTION_ADD_EMPTY);
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
                                            }
                                        });
                            }
                        });
                break;

            // Content view
            case R.id.searchView:
                materialCardView.performClick();
                break;

            default:
                Log.w(TAG, "OnClick: Unknown item " + view.getId());
                break;
        }
    }

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

        // Get new note list based on new folder
        switch (currentFolder) {
            case "/" + FOLDER_FAVORITES:
                view = action_drawer_favorites;
                break;
            case "/" + FOLDER_LOCKED:
                view = action_drawer_locked;
                break;
            case "/" + FOLDER_TRASH:
                view = action_drawer_trash;
                notes = Note.listAll(Note.class);
                notes.removeIf(note ->
                        !trash.stream().map(t -> t.note.getId())
                                .collect(Collectors.toList())
                                .contains(note.getId()));
                break;
            default:
                view = action_drawer_all;
                notes = Note.find(Note.class, "folder = ?", currentFolder);
                notes.removeIf(note ->
                        trash.stream().map(t -> t.note.getId())
                                .collect(Collectors.toList())
                                .contains(note.getId()));
                break;
        }

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
     * @param mode The type of attribute to sort
     * @param asc  The order ({@link #ASCENDING} or {@link #DESCENDING})
     */
    private void sort(int mode, boolean asc) {
        Comparator<Note> comparator;
        switch (mode) {
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
    }

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