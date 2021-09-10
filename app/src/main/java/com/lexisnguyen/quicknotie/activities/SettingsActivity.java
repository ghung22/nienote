package com.lexisnguyen.quicknotie.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.lexisnguyen.quicknotie.R;

@SuppressWarnings("FieldCanBeLocal")
public class SettingsActivity extends AppCompatActivity implements
        PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {
    // GUI Elements
    private TextView action_save;
    private FloatingActionButton fab;

    // Data
    // - Shared Preferences
    // - Animation
    private final float bounceAmount = 20;
    private final float rotateAmount = 45;
    private final int quickAni = 150;
    private final int normalAni = 300;

    // Debugging
    private final String TAG = "SettingsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        action_save = findViewById(R.id.action_save);
        fab = findViewById(R.id.fab);

        action_save.setOnClickListener(this::onClick);
        fab.setOnClickListener(this::onClick);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.frameLayout, new SettingsFragment())
                .commit();
    }

    @Override
    public boolean onPreferenceStartFragment(PreferenceFragmentCompat caller, Preference pref) {
        // Instantiate the new Fragment
        final Bundle args = pref.getExtras();
        final FragmentManager supportFragmentManager = getSupportFragmentManager();
        final Fragment fragment = supportFragmentManager.getFragmentFactory().instantiate(
                getClassLoader(),
                pref.getFragment());
        fragment.setArguments(args);

        // Replace the existing Fragment with the new Fragment
        supportFragmentManager.beginTransaction()
                .replace(R.id.frameLayout, fragment)
                .addToBackStack(null)
                .commit();
        supportFragmentManager.setFragmentResultListener(
                "requestKey",
                caller.getViewLifecycleOwner(),
                (requestKey, result) -> {
                    // Get result from bundle
                });
        return true;
    }

    @SuppressLint("NonConstantResourceId")
    private void onClick(View view) {
        int viewId = view.getId();
        switch (viewId) {
            case R.id.action_save:
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
                                                onBackPressed();
                                            }
                                        });
                            }
                        });
                break;

            case R.id.fab:
                view.animate().rotationBy(-rotateAmount).setDuration(quickAni)
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                super.onAnimationEnd(animation);
                                view.animate().rotationBy(rotateAmount).setDuration(quickAni)
                                        .setListener(new AnimatorListenerAdapter() {
                                            @Override
                                            public void onAnimationEnd(Animator animation) {
                                                super.onAnimationEnd(animation);
                                                // TODO: Reset settings
                                            }
                                        });
                            }
                        });
                break;

            default:
                Log.w(TAG, "OnClick: Unknown item " + viewId);
                break;
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.anim_slide_right_enter, R.anim.anim_slide_right_leave);
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences, rootKey);
        }
    }
}