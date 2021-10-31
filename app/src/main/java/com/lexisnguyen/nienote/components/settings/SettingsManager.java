package com.lexisnguyen.nienote.components.settings;

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
    public Boolean show_preview,
            old_show_preview,
            default_show_preview = false;
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
    public Boolean debugging,
            old_debugging,
            default_debugging = false;

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
    @SuppressWarnings("DuplicatedCode")
    public void get() {
        try {
            app_theme = preferences.getString("app_theme", default_app_theme);
            auto_save = preferences.getBoolean("auto_save", default_auto_save);

            show_preview = preferences.getBoolean("show_preview", default_show_preview);
            note_text_size = preferences.getInt("note_text_size", default_note_text_size);
            note_background = preferences.getString("note_background", default_note_background);
            undo_size = preferences.getInt("undo_size", default_undo_size);
            undo_delay = preferences.getInt("undo_delay", default_undo_delay);

            delete_permanently = preferences.getBoolean("delete_permanently", default_delete_permanently);
            debugging = preferences.getBoolean("debugging", default_debugging);
        } catch (ClassCastException e) {
            clean();
        }
        old_app_theme = app_theme;
        old_auto_save = auto_save;

        old_show_preview = show_preview;
        old_note_text_size = note_text_size;
        old_note_background = note_background;
        old_undo_size = undo_size;
        old_undo_delay = undo_delay;

        old_delete_permanently = delete_permanently;
        old_debugging = debugging;
    }

    /**
     * Recreate SharedPreferences data in events of ClassCastException
     */
    public void clean() {
        preferences.edit()
                .remove("app_theme")
                .remove("auto_save")

                .remove("show_preview")
                .remove("note_text_size")
                .remove("note_background")
                .remove("undo_size")
                .remove("undo_delay")

                .remove("delete_permanently")
                .remove("debugging")
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

                .putBoolean("show_preview", show_preview)
                .putInt("note_text_size", note_text_size)
                .putString("note_background", note_background)
                .putInt("undo_size", undo_size)
                .putInt("undo_delay", undo_delay)

                .putBoolean("delete_permanently", delete_permanently)
                .putBoolean("debugging", debugging)
                .apply();
    }

    /**
     * Restore the default settings
     */
    @SuppressWarnings("DuplicatedCode")
    public void restoreDefault() {
        app_theme = default_app_theme;
        auto_save = default_auto_save;

        show_preview = default_show_preview;
        note_text_size = default_note_text_size;
        note_background = default_note_background;
        undo_size = default_undo_size;
        undo_delay = default_undo_delay;

        delete_permanently = default_delete_permanently;
        debugging = default_debugging;
    }

    /**
     * Reset to the previous settings
     */
    @SuppressWarnings("DuplicatedCode")
    public void reset() {
        app_theme = old_app_theme;
        auto_save = old_auto_save;

        show_preview = old_show_preview;
        note_text_size = old_note_text_size;
        note_background = old_note_background;
        undo_size = old_undo_size;
        undo_delay = old_undo_delay;

        delete_permanently = old_delete_permanently;
        debugging = old_debugging;
    }
}