package com.lexisnguyen.quicknotie;

import android.os.Bundle;
import android.view.Gravity;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ImageButton action_show_menu = findViewById(R.id.action_show_menu),
                action_add_checklist = findViewById(R.id.action_add_checklist),
                action_add_image = findViewById(R.id.action_add_image),
                action_settings = findViewById(R.id.action_settings);
        FloatingActionButton fab = findViewById(R.id.fab);
        drawerLayout = findViewById(R.id.drawerLayout);
        ExpandableListView expandableListView = findViewById(R.id.expandableListView);
        ExpandableListAdapter expandableListAdapter;

        action_show_menu.setOnClickListener(view -> {
            drawerLayout.openDrawer(Gravity.START);
        });
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(Gravity.START)) {
            drawerLayout.closeDrawer(Gravity.START);
        } else {
            super.onBackPressed();
        }
    }
}