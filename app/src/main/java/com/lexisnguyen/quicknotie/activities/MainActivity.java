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

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.customview.widget.ViewDragHelper;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.lexisnguyen.quicknotie.R;
import com.lexisnguyen.quicknotie.components.notes.NoteAdapter;
import com.lexisnguyen.quicknotie.components.sql.Note;
import com.lexisnguyen.quicknotie.components.sql.Trash;
import com.orm.SugarDb;

import java.lang.reflect.Field;
import java.util.ArrayList;
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
    private SearchView searchView;
    private RecyclerView recyclerView;

    // Data
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

        /* INIT DATA */
        // - SQLite
        db = new SugarDb(this);
        db.onCreate(db.getDB());
        adapter = new NoteAdapter(this, notes);
        cd("/");

        /* INIT GUI ELEMENTS */
        // - DrawerLayout
        drawerLayout = findViewById(R.id.drawerLayout);
        ExpandableListView expandableListView = findViewById(R.id.expandableListView);
        navigationView = findViewById(R.id.navigationView);
        action_drawer_all = findViewById(R.id.action_drawer_all);
        action_drawer_favorites = findViewById(R.id.action_drawer_favorites);
        action_drawer_locked = findViewById(R.id.action_drawer_locked);
        action_drawer_trash = findViewById(R.id.action_drawer_trash);
        // - Bottom Appbar
        bottomAppBar = findViewById(R.id.bottomAppBar);
        action_show_menu = findViewById(R.id.action_show_menu);
        action_add_codeblock = findViewById(R.id.action_add_codeblock);
        action_add_image = findViewById(R.id.action_add_image);
        action_settings = findViewById(R.id.action_settings);
        fab = findViewById(R.id.fab);
        // - Content view
        searchView = findViewById(R.id.searchView);
        recyclerView = findViewById(R.id.recyclerView);

        /* INIT DRAWER:
         * [ALL] [FAVORITES] [LOCKED] [TRASH]
         * - ALL:       Show all notes that are not in trash
         * - FAVORITES:  Show liked notes
         * - LOCKED:      Show locked notes
         * - TRASH:     Show trashed notes
         */
        ExpandableListAdapter expandableListAdapter;
        expandDrawerTrigger();
        action_drawer_all.setBackgroundColor(MaterialColors.getColor(drawerLayout, R.attr.colorPrimary));
        action_drawer_all.setOnClickListener(this::onClick);
        action_drawer_favorites.setOnClickListener(this::onClick);
        action_drawer_locked.setOnClickListener(this::onClick);
        action_drawer_trash.setOnClickListener(this::onClick);

        /* INIT BOTTOM APPBAR BUTTONS:
         * [MENU] [CHECKLIST] (ADD) [IMAGE] [SETTINGS]
         * - MENU:      Show drawer menu on the left/Go back to Notes
         * - CHECKLIST: Add a note with an empty checklist
         * - ADD:       Add an empty note
         * - IMAGE:     Add a note with an image
         * - SETTINGS:  Show Settings screen
         */
        action_show_menu.setOnClickListener(this::onClick);
        action_add_codeblock.setOnClickListener(this::onClick);
        fab.setOnClickListener(this::onClick);
        action_add_image.setOnClickListener(this::onClick);
        action_settings.setOnClickListener(this::onClick);

        /* INIT CONTENT VIEW */
        recyclerView.setAdapter(adapter);
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

    @SuppressLint("NonConstantResourceId")
    private void onClick(View view) {
        int viewId = view.getId();
        switch (viewId) {
            // DrawerLayout
            case R.id.action_drawer_all:
            case R.id.action_drawer_favorites:
            case R.id.action_drawer_locked:
            case R.id.action_drawer_trash:
                action_drawer_all.setBackgroundColor(getColor(R.color.transparent));
                action_drawer_favorites.setBackgroundColor(getColor(R.color.transparent));
                action_drawer_locked.setBackgroundColor(getColor(R.color.transparent));
                action_drawer_trash.setBackgroundColor(getColor(R.color.transparent));
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
                view.setBackgroundColor(MaterialColors.getColor(drawerLayout, R.attr.colorPrimary));
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
            case R.id.action_add_codeblock:
            case R.id.fab:
            case R.id.action_add_image:
                view.animate().translationYBy(-bounceAmount).setDuration(quickAni)
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                super.onAnimationEnd(animation);
                                if (viewId == R.id.action_add_codeblock) {
                                    action_add(ACTION_ADD_CODEBLOCK);
                                } else if (viewId == R.id.fab) {
                                    action_add(ACTION_ADD_EMPTY);
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

            default:
                Log.w(TAG, "OnClick: Unknown item " + view.getId());
                break;
        }
    }

    /**
     * Create a new note, starting {@link EditorActivity} with a note preset, specified by the following action
     * codes:
     * <ul>
     * <li>{@link MainActivity#ACTION_ADD_EMPTY}: An empty note.
     * <li>{@link MainActivity#ACTION_ADD_CODEBLOCK}: A note with an empty checklist.
     * <li>{@link MainActivity#ACTION_ADD_IMAGE}: A note with an image/drawing.
     * <li><b>Unknown</b>: Fallback to {@link MainActivity#ACTION_ADD_EMPTY}.
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
        startActivity(intent);
        overridePendingTransition(
                R.anim.anim_slide_up_ease_in,
                R.anim.anim_null
        );
    }

    private void cd(String folder) {
        if (currentFolder.equals(folder)) {
            return;
        }
        currentFolder = folder;
        trash = Trash.listAll(Trash.class);
        notes.clear();
        switch (currentFolder) {
            case "/" + FOLDER_FAVORITES:
                break;
            case "/" + FOLDER_LOCKED:
                break;
            case "/" + FOLDER_TRASH:
                notes = Note.listAll(Note.class);
                notes.removeIf(note ->
                        trash.stream().map(Trash::getId)
                                .collect(Collectors.toList())
                                .contains(note.getId()));
                break;
            default:
                notes = Note.find(Note.class, "folder = ?", currentFolder);
                notes.removeIf(note ->
                        !trash.stream().map(Trash::getId)
                                .collect(Collectors.toList())
                                .contains(note.getId()));
                break;
        }
        new Handler(Looper.getMainLooper()).post(() -> adapter.notifyDataSetChanged(notes));
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