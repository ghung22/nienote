package com.lexisnguyen.quicknotie.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.customview.widget.ViewDragHelper;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.lexisnguyen.quicknotie.R;
import com.lexisnguyen.quicknotie.components.notes.NoteAdapter;
import com.lexisnguyen.quicknotie.components.sql.Note;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Objects;

@SuppressWarnings("FieldCanBeLocal")
public class MainActivity extends AppCompatActivity {
    // GUI Elements
    private DrawerLayout drawerLayout;
    // - BottomAppBar
    private BottomAppBar bottomAppBar;
    private ImageButton action_show_menu, action_add_codeblock, action_add_image, action_settings;
    private FloatingActionButton fab;
    // - Content view
    private SearchView searchView;
    private RecyclerView recyclerView;

    // Data
    // - SQLite
    private String currentFolder;
    private ArrayList<Note> notes;
    // - Animation
    private final float bounceAmount = 20;
    private final int quickAni = 150;
    private final int normalAni = 300;

    // Constants
    // - New note
    public static final int ACTION_ADD_EMPTY = 0;
    public static final int ACTION_ADD_CODEBLOCK = 1;
    public static final int ACTION_ADD_IMAGE = 2;
    // - Open note
    public static final int ACTION_OPEN_NOTE = 10;

    // Debugging
    private final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /* INIT GUI ELEMENTS */
        // - Drawer
        drawerLayout = findViewById(R.id.drawerLayout);
        ExpandableListView expandableListView = findViewById(R.id.expandableListView);
        ExpandableListAdapter expandableListAdapter;
        expandDrawerTrigger();
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

        /* INIT DATA */
        currentFolder = "/";

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
        notes = (ArrayList<Note>) Note.listAll(Note.class);
        NoteAdapter adapter = new NoteAdapter(this, notes);
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
            Field dragField = drawerLayout.getClass().getDeclaredField("mLeftDragger");
            dragField.setAccessible(true);
            ViewDragHelper dragHelper = (ViewDragHelper) dragField.get(drawerLayout);
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
        view.setClickable(false);
        switch (viewId) {
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
        }
        view.setClickable(true);
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