package com.lexisnguyen.quicknotie.components.settings;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

/**
 * Handles saving user settings for the app
 */
@SuppressWarnings("FieldCanBeLocal")
public class SettingsManager {
    // General settings
    public String app_theme,
            old_app_theme,
            default_app_theme = "system";
    public Boolean auto_save,
            old_auto_save,
            default_auto_save = true;

    // Editor settings
    public Integer note_text_size,
            old_note_text_size,
            default_note_text_size = 15; // Unit: sp
    public String note_background,
            old_note_background,
            default_note_background = "white"; // Values in colors.xml
    public Integer undo_size,
            old_undo_size,
            default_undo_size = 5; // This value will be multiplied -> 50 steps
    public Integer undo_delay,
            old_undo_delay,
            default_undo_delay = 10; // This value will be multiplied -> 1000ms

    // Advanced settings
    public Boolean delete_permanently,
            old_delete_permanently,
            default_delete_permanently = false;

    // Data
    private final SharedPreferences preferences;

    /**
     * Init a SettingsManager object
     *
     * @param context The activity context
     */
    public SettingsManager(Context context) {
        this.preferences = PreferenceManager.getDefaultSharedPreferences(context);
        get();
    }

    /**
     * Get settings saved in SharedPreferences
     */
    public void get() {
        try {
            app_theme = preferences.getString("app_theme", default_app_theme);
            auto_save = preferences.getBoolean("auto_save", default_auto_save);

            note_text_size = preferences.getInt("note_text_size", default_note_text_size);
            note_background = preferences.getString("note_background", default_note_background);
            undo_size = preferences.getInt("undo_size", default_undo_size);
            undo_delay = preferences.getInt("undo_delay", default_undo_delay);

            delete_permanently = preferences.getBoolean("delete_permanently", default_delete_permanently);
        } catch (ClassCastException e) {
            clean();
        }
        old_app_theme = app_theme;
        old_auto_save = auto_save;
        old_note_text_size = note_text_size;
        old_note_background = note_background;
        old_undo_size = undo_size;
    }

    /**
     * Recreate SharedPreferences data in events of ClassCastException
     */
    public void clean() {
        preferences.edit()
                .remove("app_theme")
                .remove("auto_save")

                .remove("note_text_size")
                .remove("note_background")
                .remove("undo_size")
                .remove("undo_delay")

                .remove("delete_permanently")
                .apply();
        update();
    }

    /**
     * Save local settings into SharedPreferences
     */
    public void update() {
        preferences.edit()
                .putString("app_theme", app_theme)
                .putBoolean("auto_save", auto_save)

                .putInt("note_text_size", note_text_size)
                .putString("note_background", note_background)
                .putInt("undo_size", undo_size)
                .putInt("undo_delay", undo_delay)

                .putBoolean("delete_permanently", delete_permanently)
                .apply();
    }

    /**
     * Restore the default settings
     */
    public void restoreDefault() {
        app_theme = default_app_theme;
        auto_save = default_auto_save;
        note_text_size = default_note_text_size;
        note_background = default_note_background;
        undo_size = default_undo_size;
    }

    /**
     * Reset to the previous settings
     */
    public void reset() {
        app_theme = old_app_theme;
        auto_save = old_auto_save;
        note_text_size = old_note_text_size;
        note_background = old_note_background;
        undo_size = old_undo_size;
    }
}