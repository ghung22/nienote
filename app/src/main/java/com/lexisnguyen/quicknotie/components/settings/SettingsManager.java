package com.lexisnguyen.quicknotie.components.settings;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

@SuppressWarnings("FieldCanBeLocal")
public class SettingsManager {
    // General settings
    public Integer app_theme = 2;
    public Boolean auto_save = true;

    // Editor settings
    public Integer note_text_size = 15;
    public String note_background = "white";
    public Integer undo_size = 50;
    public Integer undo_delay = 1000;

    // Advanced settings
    public Boolean delete_permanently = false;

    // Data
    private final SharedPreferences preferences;

    public SettingsManager(Context context) {
        this.preferences = PreferenceManager.getDefaultSharedPreferences(context);
        resetSettings();
    }

    public void resetSettings() {
        app_theme = preferences.getInt("app_theme", app_theme);
        auto_save = preferences.getBoolean("auto_save", auto_save);

        note_text_size = preferences.getInt("note_text_size", note_text_size);
        note_background = preferences.getString("note_background", note_background);
        undo_size = preferences.getInt("undo_size", undo_size);
        undo_delay = preferences.getInt("undo_delay", undo_delay);

        delete_permanently = preferences.getBoolean("delete_permanently", delete_permanently);
    }

    public void updateSettings() {
        preferences.edit()
                .putInt("app_theme", app_theme)
                .putBoolean("auto_save", auto_save)

                .putInt("note_text_size", note_text_size)
                .putString("note_background", note_background)
                .putInt("undo_size", undo_size)
                .putInt("undo_delay", undo_delay)

                .putBoolean("delete_permanently", delete_permanently)
                .apply();
    }
}