package com.lexisnguyen.nienote.activities;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.lexisnguyen.nienote.R;

/**
 * Introduction screen to the app
 */
public class SplashActivity extends AppCompatActivity {
    private final static int SPLASH_DURATION = 1000;
    private Intent main;

    private ActivityResultLauncher<String>
            storagePermissionLauncher,
            cameraPermissionLauncher;

    /**
     * Opens a screen with the icon of this app. {@link SplashActivity} will also check for permissions and ask for them
     * from the user if necessary. Only when mandatory permissions are granted will {@link SplashActivity} switch to
     * {@link MainActivity} and let the user use the app.
     */
    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_splash);

        // Set custom splash theme
        getWindow().setStatusBarColor(getColor(R.color.gray));
        //noinspection deprecation
        getWindow().getDecorView().setSystemUiVisibility(0);

        // Check for app permission
        storagePermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                result -> {
                    if (!result) {
                        retryPermission(Manifest.permission.READ_EXTERNAL_STORAGE, storagePermissionLauncher);
                        return;
                    }
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
                });
        cameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                result -> {
                    if (!result) {
                        retryPermission(Manifest.permission.CAMERA, cameraPermissionLauncher);
                        return;
                    }
                    startActivity(main);
                    finish();
                });
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            main = new Intent(SplashActivity.this, MainActivity.class);
            storagePermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
        }, SPLASH_DURATION);
    }

    /**
     * Show a dialog to explain the reason for permission request and a way to retry the request
     *
     * @param permission Name of the permission
     * @param launcher   The ActivityResultLauncher to request for the permission
     */
    private void retryPermission(String permission, ActivityResultLauncher<String> launcher) {
        // Get message string to show on the dialog
        @StringRes int messageId = R.string.app_name;
        if (permission.equals(Manifest.permission.READ_EXTERNAL_STORAGE)) {
            messageId = R.string.alert_permission_denied_message_storage;
        } else if (permission.equals(Manifest.permission.CAMERA)) {
            messageId = R.string.alert_permission_denied_message_camera;
        }

        // Create a dialog with a custom title, a message and a close/retry button
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.alert_permission_denied_title)
          .setMessage(getString(messageId, getString(R.string.app_name)))
                .setPositiveButton(getString(R.string.action_retry), (dialog, i) -> {
                    dialog.dismiss();
                    launcher.launch(permission);
                })
                .setNegativeButton(getString(R.string.action_close), (dialog, i) -> {
                    dialog.dismiss();
                    finishAffinity();
                })
                .setCancelable(true)
                .setOnCancelListener(dialog -> {
                    dialog.dismiss();
                    finishAffinity();
                })
                .show();
    }
}