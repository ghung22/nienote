package com.lexisnguyen.quicknotie.activities;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.lexisnguyen.quicknotie.R;

/**
 * Introduction screen to the app
 */
public class SplashActivity extends AppCompatActivity {
    private final static int STORAGE_PERMISSION_REQUEST_CODE = 101;
    private final static int SPLASH_DURATION = 1000;
    private final static String TAG = "SPLASH";
    Intent main;

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
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            main = new Intent(SplashActivity.this, MainActivity.class);
            checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE, STORAGE_PERMISSION_REQUEST_CODE);
        }, SPLASH_DURATION);
    }

    /**
     * If a permission has granted, close {@link SplashActivity} and start {@link MainActivity}.
     *
     * @param permission  Permission code taken from {@link Manifest.permission}.
     * @param requestCode Unique request code to check for request permission results in
     *                    {@link #onRequestPermissionsResult(int, String[], int[])}.
     */
    public void checkPermission(String permission, int requestCode) {
        if (ContextCompat.checkSelfPermission(SplashActivity.this, permission) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(SplashActivity.this, new String[]{permission}, requestCode);
        } else {
            startActivity(main);
            finish();
        }
    }

    /**
     * <p>Check for a permission request result…</p>
     * <p>If <b>Granted</b>: Start {@link MainActivity} and close {@link SplashActivity}.</p>
     * <p>If <b>Denied</b>: Notify user why these permissions are needed.</p>
     *
     * @param requestCode  Unique request code to identify which permission is asked .
     * @param permissions  Permission code taken from {@link Manifest.permission}.
     * @param grantResults The results got from asking for permission.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "onRequestPermissionsResult: " + permissions[0] + " is granted, switching to MainActivity…");
                startActivity(main);
                finish();
            } else {
                Log.w(TAG, "onRequestPermissionsResult: " + permissions[0] + " is denied, asking the user to retry…");

                // Get message string to show on the dialog
                @StringRes int messageId = R.string.app_name;
                if (requestCode == STORAGE_PERMISSION_REQUEST_CODE) {
                    messageId = R.string.alert_permission_denied_message_storage;
                }

                // Create a dialog with a custom title, a message and a close/retry button
                MaterialAlertDialogBuilder dialogBuilder =
                        new MaterialAlertDialogBuilder(this)
                                .setTitle(R.string.alert_permission_denied_title)
                                .setMessage(getString(messageId, getString(R.string.app_name)))
                                .setPositiveButton(getString(R.string.action_retry), (dialogInterface, i) -> {
                                    dialogInterface.dismiss();
                                    checkPermission(permissions[0], requestCode);
                                })
                                .setNegativeButton(getString(R.string.action_close), (dialogInterface, i) -> {
                                    dialogInterface.dismiss();
                                    finishAffinity();
                                });
                AlertDialog dialog = dialogBuilder.create();
                dialog.show();

                // Align the close button to middle
                final Button button = dialog.getButton(DialogInterface.BUTTON_NEUTRAL);
                LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) button.getLayoutParams();
                layoutParams.gravity = Gravity.CENTER;
                button.setLayoutParams(layoutParams);
            }
        } else {
            Log.e(TAG, "onRequestPermissionsResult: Grant result array is empty");
        }
    }
}