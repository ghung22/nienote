package com.lexisnguyen.quicknotie.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SeekBarPreference;
import androidx.preference.SwitchPreference;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.lexisnguyen.quicknotie.R;
import com.lexisnguyen.quicknotie.components.settings.SettingsManager;

import java.util.Arrays;

@SuppressWarnings("FieldCanBeLocal")
public class SettingsActivity extends AppCompatActivity implements
        PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {
    // GUI Elements
    private TextView action_save;
    private Button action_default;
    private FloatingActionButton fab;

    // Data
    // - Fragments
    private SettingsFragment fragment;
    // - SharedPreferences
    static SettingsManager settingsManager;
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
        action_default = findViewById(R.id.action_default);
        fab = findViewById(R.id.fab);

        action_save.setOnClickListener(this::onClick);
        action_default.setOnClickListener(this::onClick);
        fab.setOnClickListener(this::onClick);

        fragment = new SettingsFragment();
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.frameLayout, fragment)
                .commit();

        settingsManager = new SettingsManager(this);
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

            case R.id.action_default:
                settingsManager.restoreDefault();
                fragment.updateUi();
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
                                                settingsManager.reset();
                                                fragment.updateUi();
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
        // General settings
        private ListPreference app_theme;
        private SwitchPreference auto_save;
        private Preference manage_folders;

        // Editor settings
        private SeekBarPreference note_text_size;
        private ListPreference note_background;
        private SeekBarPreference undo_size;
        private SeekBarPreference undo_delay;

        // Advanced settings
        private SwitchPreference delete_permanently;

        private final String TAG = "SettingsFragment";

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            try {
                setPreferencesFromResource(R.xml.preferences, rootKey);
            } catch (ClassCastException e) {
                settingsManager.clean();
                setPreferencesFromResource(R.xml.preferences, rootKey);
            }
        }

        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            PreferenceManager prefManager = getPreferenceManager();

            // Get settings
            app_theme = prefManager.findPreference("app_theme");
            auto_save = prefManager.findPreference("auto_save");
            manage_folders = prefManager.findPreference("manage_folders");
            note_text_size = prefManager.findPreference("note_text_size");
            note_background = prefManager.findPreference("note_background");
            undo_size = prefManager.findPreference("undo_size");
            undo_delay = prefManager.findPreference("undo_delay");
            delete_permanently = prefManager.findPreference("delete_permanently");

            // Set settings
            set_app_theme();
            set_auto_save();
            set_manage_folders();
            set_note_text_size();
            set_note_background();
            set_undo_size();
            set_undo_delay();
            set_delete_permanently();

            // Update UI
            updateUi();

            return super.onCreateView(inflater, container, savedInstanceState);
        }

        // region Set each single preferences

        private void set_app_theme() {
            app_theme.setOnPreferenceChangeListener((
                    (preference, newValue) -> {
                        settingsManager.app_theme = String.valueOf(newValue);
                        update_app_theme();
                        return true;
                    }));
        }

        private void set_auto_save() {
            auto_save.setOnPreferenceChangeListener((
                    (preference, newValue) -> {
                        String val = String.valueOf(newValue);
                        settingsManager.auto_save = Boolean.parseBoolean(val);
                        update_auto_save();
                        return true;
                    }));
        }

        private void set_manage_folders() {
            manage_folders.setOnPreferenceClickListener((
                    (preference) -> {
                        startActivity(preference.getIntent());
                        return true;
                    }));
        }

        private void set_note_text_size() {
            note_text_size.setOnPreferenceChangeListener((
                    (preference, newValue) -> {
                        String val = String.valueOf(newValue);
                        settingsManager.note_text_size = Integer.parseInt(val);
                        update_note_text_size();
                        return true;
                    }));
        }

        private void set_note_background() {
            note_background.setOnPreferenceChangeListener((
                    (preference, newValue) -> {
                        settingsManager.note_background = String.valueOf(newValue);
                        update_note_background();
                        return true;
                    }));
        }

        private void set_undo_size() {
            undo_size.setOnPreferenceChangeListener((
                    (preference, newValue) -> {
                        String val = String.valueOf(newValue);
                        settingsManager.undo_size = Integer.parseInt(val);
                        update_undo_size();
                        return true;
                    }));
        }

        private void set_undo_delay() {
            undo_delay.setOnPreferenceChangeListener((
                    (preference, newValue) -> {
                        String val = String.valueOf(newValue);
                        settingsManager.undo_delay = Integer.parseInt(val);
                        update_undo_delay();
                        return true;
                    }));
        }

        private void set_delete_permanently() {
            delete_permanently.setOnPreferenceChangeListener((
                    (preference, newValue) -> {
                        String val = String.valueOf(newValue);
                        settingsManager.delete_permanently = Boolean.parseBoolean(val);
                        update_delete_permanently();
                        return true;
                    }));
        }


        // endregion

        public void updateUi() {
            update_app_theme();
            update_auto_save();
            update_note_text_size();
            update_note_background();
            update_undo_size();
            update_undo_delay();
            update_delete_permanently();
        }

        // region Update each single preferences

        private void update_app_theme() {
            int themeId = Arrays.asList(requireContext().getResources()
                    .getStringArray(R.array.app_theme_values)).indexOf(settingsManager.app_theme);
            String theme = requireContext().getResources()
                    .getStringArray(R.array.app_theme_titles)[themeId];
            app_theme.setValue(settingsManager.app_theme);
            app_theme.setSummary(theme);

            switch (settingsManager.app_theme) {
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
                    Log.w(TAG, "update_app_theme: Unknown theme " + settingsManager.app_theme);
                    break;
            }
        }

        private void update_auto_save() {
            auto_save.setChecked(settingsManager.auto_save);
        }

        private void update_note_text_size() {
            note_text_size.setValue(settingsManager.note_text_size);
            note_text_size.setSummary(settingsManager.note_text_size + "sp");
        }

        private void update_note_background() {
            int bgId = Arrays.asList(requireContext().getResources()
                    .getStringArray(R.array.note_background_values)).indexOf(settingsManager.note_background);
            String bg = requireContext().getResources()
                    .getStringArray(R.array.note_background_titles)[bgId];
            note_background.setValue(settingsManager.note_background);
            note_background.setSummary(bg);
        }

        private void update_undo_size() {
            undo_size.setValue(settingsManager.undo_size);
            String size = (settingsManager.undo_size == 0) ?
                    "Disabled" :
                    settingsManager.undo_size + "0 steps";
            undo_size.setSummary(size);
        }

        private void update_undo_delay() {
            undo_delay.setValue(settingsManager.undo_delay);
            undo_delay.setSummary(settingsManager.undo_delay + "00ms");
        }

        private void update_delete_permanently() {
            delete_permanently.setChecked(settingsManager.delete_permanently);
        }

        // endregion
    }
}