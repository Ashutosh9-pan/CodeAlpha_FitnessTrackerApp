package com.ashutosh.codealpha_fitnesstrackerapp;

import android.Manifest;
import android.app.TimePickerDialog;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.Calendar;
import java.util.Locale;

import utils.ThemeManager;

public class ReminderActivity extends AppCompatActivity {

    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE =
            2001;

    private MaterialSwitch switchReminder;
    private MaterialButton btnPickTime;
    private MaterialButton btnSaveReminder;
    private TextView txtSelectedTime;

    private SharedPreferences sharedPreferences;

    private int selectedHour = 6;
    private int selectedMinute = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applySavedTheme(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminder);

        initialiseViews();
        loadSavedSettings();
        setupListeners();
    }

    private void initialiseViews() {
        switchReminder =
                findViewById(R.id.switchReminder);

        btnPickTime =
                findViewById(R.id.btnPickTime);

        btnSaveReminder =
                findViewById(R.id.btnSaveReminder);

        txtSelectedTime =
                findViewById(R.id.txtSelectedTime);

        sharedPreferences =
                getSharedPreferences(
                        ReminderScheduler.PREFERENCES_NAME,
                        MODE_PRIVATE
                );
    }

    private void setupListeners() {
        btnPickTime.setOnClickListener(
                view -> showTimePicker()
        );

        btnSaveReminder.setOnClickListener(
                view -> saveReminder()
        );

        switchReminder.setOnCheckedChangeListener(
                (buttonView, isChecked) -> {

                    btnPickTime.setEnabled(isChecked);
                    txtSelectedTime.setEnabled(isChecked);
                }
        );
    }

    private void showTimePicker() {
        TimePickerDialog timePickerDialog =
                new TimePickerDialog(
                        this,
                        (timePicker, hourOfDay, minute) -> {

                            selectedHour = hourOfDay;
                            selectedMinute = minute;

                            updateTimeText();
                        },
                        selectedHour,
                        selectedMinute,
                        false
                );

        timePickerDialog.show();
    }

    private void updateTimeText() {
        Calendar calendar =
                Calendar.getInstance();

        calendar.set(
                Calendar.HOUR_OF_DAY,
                selectedHour
        );

        calendar.set(
                Calendar.MINUTE,
                selectedMinute
        );

        String amPm =
                calendar.get(
                        Calendar.AM_PM
                ) == Calendar.AM
                        ? "AM"
                        : "PM";

        int displayHour =
                calendar.get(
                        Calendar.HOUR
                );

        if (displayHour == 0) {
            displayHour = 12;
        }

        String formattedTime =
                String.format(
                        Locale.getDefault(),
                        "%02d:%02d %s",
                        displayHour,
                        selectedMinute,
                        amPm
                );

        txtSelectedTime.setText(
                formattedTime
        );

        btnPickTime.setText(
                "Change Time"
        );
    }

    private void saveReminder() {
        boolean reminderEnabled =
                switchReminder.isChecked();

        sharedPreferences
                .edit()
                .putBoolean(
                        ReminderScheduler.KEY_ENABLED,
                        reminderEnabled
                )
                .putInt(
                        ReminderScheduler.KEY_HOUR,
                        selectedHour
                )
                .putInt(
                        ReminderScheduler.KEY_MINUTE,
                        selectedMinute
                )
                .apply();

        if (reminderEnabled) {
            if (needsNotificationPermission()) {
                requestNotificationPermission();
                return;
            }

            ReminderScheduler.scheduleNext(this);

            Toast.makeText(
                    this,
                    "Daily workout reminder scheduled",
                    Toast.LENGTH_SHORT
            ).show();

        } else {
            ReminderScheduler.cancel(this);

            Toast.makeText(
                    this,
                    "Workout reminder disabled",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void loadSavedSettings() {
        boolean reminderEnabled =
                sharedPreferences.getBoolean(
                        ReminderScheduler.KEY_ENABLED,
                        false
                );

        selectedHour =
                sharedPreferences.getInt(
                        ReminderScheduler.KEY_HOUR,
                        6
                );

        selectedMinute =
                sharedPreferences.getInt(
                        ReminderScheduler.KEY_MINUTE,
                        0
                );

        switchReminder.setChecked(
                reminderEnabled
        );

        btnPickTime.setEnabled(
                reminderEnabled
        );

        txtSelectedTime.setEnabled(
                reminderEnabled
        );

        updateTimeText();
    }

    private boolean needsNotificationPermission() {
        return Build.VERSION.SDK_INT
                >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
        ) != PackageManager.PERMISSION_GRANTED;
    }

    private void requestNotificationPermission() {
        ActivityCompat.requestPermissions(
                this,
                new String[]{
                        Manifest.permission.POST_NOTIFICATIONS
                },
                NOTIFICATION_PERMISSION_REQUEST_CODE
        );
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );

        if (requestCode
                != NOTIFICATION_PERMISSION_REQUEST_CODE) {

            return;
        }

        if (grantResults.length > 0
                && grantResults[0]
                == PackageManager.PERMISSION_GRANTED) {

            ReminderScheduler.scheduleNext(this);

            Toast.makeText(
                    this,
                    "Daily workout reminder scheduled",
                    Toast.LENGTH_SHORT
            ).show();

        } else {
            switchReminder.setChecked(false);

            ReminderScheduler.cancel(this);

            sharedPreferences
                    .edit()
                    .putBoolean(
                            ReminderScheduler.KEY_ENABLED,
                            false
                    )
                    .apply();

            Toast.makeText(
                    this,
                    "Notification permission is required for reminders",
                    Toast.LENGTH_LONG
            ).show();
        }
    }
}