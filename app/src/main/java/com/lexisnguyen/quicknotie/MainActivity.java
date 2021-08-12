package com.lexisnguyen.quicknotie;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.customview.widget.ViewDragHelper;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    // GUI Elements
    private DrawerLayout drawerLayout;

    // Data
    private String currentFolder;

    // Fragment data
    private final ArrayList<String> fragNames = new ArrayList<>(Arrays.asList("notes", "settings"));
    private final ArrayList<Integer> fragId = new ArrayList<>();
    private String currentFrag;

    // Constants
    private final int ACTION_ADD_EMPTY = 0;
    private final int ACTION_ADD_CHECKLIST = 1;
    private final int ACTION_ADD_IMAGE = 2;

    private final int SWIPE_TRIGGER_MULTIPLIER = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // INIT GUI ELEMENTS
        // - Bottom Appbar
        ImageButton action_show_menu = findViewById(R.id.action_show_menu),
                action_add_checklist = findViewById(R.id.action_add_checklist),
                action_add_image = findViewById(R.id.action_add_image),
                action_settings = findViewById(R.id.action_settings);
        FloatingActionButton fab = findViewById(R.id.fab);
        // - Drawer
        drawerLayout = findViewById(R.id.drawerLayout);
        ExpandableListView expandableListView = findViewById(R.id.expandableListView);
        ExpandableListAdapter expandableListAdapter;
        expandDrawerTrigger(SWIPE_TRIGGER_MULTIPLIER);
        // - Content Layout
        FrameLayout frameLayout = findViewById(R.id.frameLayout);

        // INIT DATA
        currentFolder = "all";

        // INIT FRAGMENT DATA
        fragId.add(R.id.action_show_menu);
        fragId.add(R.id.action_settings);
        currentFrag = fragNames.get(0);

        /* BOTTOM APPBAR BUTTONS:
         * [MENU] [CHECKLIST] (ADD) [IMAGE] [SETTINGS]
         * - MENU:      Show drawer menu on the left/Go back to Notes
         * - CHECKLIST: Add a note with an empty checklist
         * - ADD:       Add an empty note
         * - IMAGE:     Add a note with an image
         * - SETTINGS:  Show Settings screen
         */
        action_show_menu.setOnClickListener(view -> action_show_menu());
        action_add_checklist.setOnClickListener(view -> action_add(ACTION_ADD_CHECKLIST));
        fab.setOnClickListener(view -> action_add(ACTION_ADD_EMPTY));
        action_add_image.setOnClickListener(view -> action_add(ACTION_ADD_IMAGE));
    }

    /**
     * Expand the area that the user can swipe to open the drawer.
     *
     * @param multiplier How much the trigger zone is expanded.
     * @see <a href="https://stackoverflow.com/a/19764654">
     * Touch anywhere to slide open menu for navigation drawer - StackOverflow
     * </a>
     */
    private void expandDrawerTrigger(int multiplier) {
        try {
            Field dragField = drawerLayout.getClass().getDeclaredField("mLeftDragger");
            dragField.setAccessible(true);
            ViewDragHelper dragHelper = (ViewDragHelper) dragField.get(drawerLayout);
            Field edgeSizeField = Objects.requireNonNull(dragHelper).getClass().getDeclaredField("mEdgeSize");
            edgeSizeField.setAccessible(true);
            int edge = edgeSizeField.getInt(dragHelper);
            edgeSizeField.setInt(dragHelper, edge * multiplier);
        } catch (Exception ignored) {
        }
    }

    /**
     * Show/hide drawer, or go back to note list fragment if current fragment is settings
     */
    private void action_show_menu() {
        if (currentFrag.equals(fragNames.get(0))) {
            drawerLayout.openDrawer(Gravity.START);
        } else if (currentFrag.equals(fragNames.get(1))) {
            // Back to notes
        }
    }

    /**
     * Create a new note, starting {@link EditorActivity} with a note preset, specified by the following action
     * codes:
     * <ul>
     * <li>{@link MainActivity#ACTION_ADD_EMPTY}: An empty note.
     * <li>{@link MainActivity#ACTION_ADD_CHECKLIST}: A note with an empty checklist.
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

        if (currentFrag.equals(fragNames.get(1))) {
            // Back to notes
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