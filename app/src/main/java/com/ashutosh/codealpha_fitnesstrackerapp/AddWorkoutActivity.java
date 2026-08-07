package com.ashutosh.codealpha_fitnesstrackerapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import database.WorkoutDBHelper;
import utils.ThemeManager;

public class AddWorkoutActivity extends AppCompatActivity {

    private static final String FIREBASE_DATABASE_URL =
            "https://fitnesstrackerapp-cb360-default-rtdb.asia-southeast1.firebasedatabase.app";

    private static final int MIN_DURATION_MINUTES = 1;
    private static final int MAX_DURATION_MINUTES = 600;

    private static final int MIN_CALORIES = 1;
    private static final int MAX_CALORIES = 5000;

    private EditText etWorkoutName;
    private EditText etDuration;
    private EditText etCalories;
    private Button btnSaveWorkout;
    private BottomNavigationView bottomNavigationView;

    private WorkoutDBHelper dbHelper;
    private DatabaseReference workoutsReference;

    private FirebaseAuth firebaseAuth;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applySavedTheme(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_workout);

        initialiseViews();
        applyBottomNavigationInsets();
        setupBottomNavigation();

        if (!initialiseDatabase()) {
            return;
        }

        setupSaveButton();
    }

    private void initialiseViews() {
        etWorkoutName = findViewById(R.id.etWorkoutName);
        etDuration = findViewById(R.id.etDuration);
        etCalories = findViewById(R.id.etCalories);
        btnSaveWorkout = findViewById(R.id.btnSaveWorkout);
        bottomNavigationView =
                findViewById(R.id.bottomNavigationView);
    }

    private void applyBottomNavigationInsets() {
        if (bottomNavigationView == null) {
            return;
        }

        final int normalNavigationHeight =
                Math.round(
                        92f
                                * getResources()
                                .getDisplayMetrics()
                                .density
                );

        final int initialPaddingLeft =
                bottomNavigationView.getPaddingLeft();
        final int initialPaddingTop =
                bottomNavigationView.getPaddingTop();
        final int initialPaddingRight =
                bottomNavigationView.getPaddingRight();

        ViewCompat.setOnApplyWindowInsetsListener(
                bottomNavigationView,
                (view, windowInsets) -> {
                    Insets navigationInsets =
                            windowInsets.getInsets(
                                    WindowInsetsCompat.Type.navigationBars()
                            );

                    view.setPadding(
                            initialPaddingLeft,
                            initialPaddingTop,
                            initialPaddingRight,
                            navigationInsets.bottom
                    );

                    ViewGroup.LayoutParams layoutParams =
                            view.getLayoutParams();

                    int requiredHeight =
                            normalNavigationHeight
                                    + navigationInsets.bottom;

                    if (layoutParams.height != requiredHeight) {
                        layoutParams.height = requiredHeight;
                        view.setLayoutParams(layoutParams);
                    }

                    return windowInsets;
                }
        );

        ViewCompat.requestApplyInsets(
                bottomNavigationView
        );
    }

    private void setupBottomNavigation() {
        if (bottomNavigationView == null) {
            return;
        }

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_add_workout) {
                return true;
            }

            if (itemId == R.id.nav_dashboard) {
                openBottomNavigationDestination(
                        MainActivity.class
                );
                return true;
            }

            if (itemId == R.id.nav_statistics) {
                openBottomNavigationDestination(
                        StatisticsActivity.class
                );
                return true;
            }

            if (itemId == R.id.nav_history) {
                openBottomNavigationDestination(
                        HistoryActivity.class
                );
                return true;
            }

            if (itemId == R.id.nav_profile) {
                openBottomNavigationDestination(
                        ProfileActivity.class
                );
                return true;
            }

            return false;
        });

        bottomNavigationView.setSelectedItemId(
                R.id.nav_add_workout
        );
    }

    private void openBottomNavigationDestination(
            Class<?> destinationActivity
    ) {
        Intent intent =
                new Intent(
                        this,
                        destinationActivity
                );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        );

        startActivity(intent);

        overridePendingTransition(
                R.anim.slide_in_right,
                R.anim.slide_out_left
        );
    }

    private boolean initialiseDatabase() {
        dbHelper = new WorkoutDBHelper(this);

        firebaseAuth = FirebaseAuth.getInstance();
        currentUser = firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(
                    this,
                    "Your session has expired. Please sign in again.",
                    Toast.LENGTH_LONG
            ).show();

            Intent loginIntent =
                    new Intent(
                            AddWorkoutActivity.this,
                            LoginActivity.class
                    );

            loginIntent.setFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TASK
            );

            startActivity(loginIntent);
            finish();

            return false;
        }

        FirebaseDatabase firebaseDatabase =
                FirebaseDatabase.getInstance(
                        FIREBASE_DATABASE_URL
                );

        workoutsReference =
                firebaseDatabase
                        .getReference("users")
                        .child(currentUser.getUid())
                        .child("workouts");

        return true;
    }

    private void setupSaveButton() {
        btnSaveWorkout.setOnClickListener(
                view -> saveWorkout()
        );
    }

    private void saveWorkout() {
        clearInputErrors();

        String workoutName =
                etWorkoutName
                        .getText()
                        .toString()
                        .trim();

        String durationText =
                etDuration
                        .getText()
                        .toString()
                        .trim();

        String caloriesText =
                etCalories
                        .getText()
                        .toString()
                        .trim();

        if (TextUtils.isEmpty(workoutName)) {
            etWorkoutName.setError("Enter workout name");
            etWorkoutName.requestFocus();
            return;
        }

        if (workoutName.length() < 2) {
            etWorkoutName.setError(
                    "Workout name must contain at least 2 characters"
            );
            etWorkoutName.requestFocus();
            return;
        }

        if (workoutName.length() > 40) {
            etWorkoutName.setError(
                    "Workout name cannot exceed 40 characters"
            );
            etWorkoutName.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(durationText)) {
            etDuration.setError("Enter workout duration");
            etDuration.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(caloriesText)) {
            etCalories.setError("Enter calories burned");
            etCalories.requestFocus();
            return;
        }

        int duration;
        int calories;

        try {
            duration = Integer.parseInt(durationText);
            calories = Integer.parseInt(caloriesText);
        } catch (NumberFormatException exception) {
            Toast.makeText(
                    this,
                    "Enter valid whole numbers",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        if (duration < MIN_DURATION_MINUTES
                || duration > MAX_DURATION_MINUTES) {

            etDuration.setError(
                    "Enter duration between "
                            + MIN_DURATION_MINUTES
                            + " and "
                            + MAX_DURATION_MINUTES
                            + " minutes"
            );

            etDuration.requestFocus();
            return;
        }

        if (calories < MIN_CALORIES
                || calories > MAX_CALORIES) {

            etCalories.setError(
                    "Enter calories between "
                            + MIN_CALORIES
                            + " and "
                            + MAX_CALORIES
                            + " kcal"
            );

            etCalories.requestFocus();
            return;
        }

        if (currentUser == null
                || workoutsReference == null) {

            Toast.makeText(
                    this,
                    "Unable to save workout. Please sign in again.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        String currentDate =
                new SimpleDateFormat(
                        "dd-MM-yyyy",
                        Locale.getDefault()
                ).format(new Date());

        saveWorkoutLocallyAndOnline(
                workoutName,
                duration,
                calories,
                currentDate
        );
    }

    private void saveWorkoutLocallyAndOnline(
            String workoutName,
            int duration,
            int calories,
            String currentDate
    ) {
        setSavingState();

        boolean insertedLocally =
                dbHelper.insertWorkout(
                        workoutName,
                        duration,
                        calories,
                        currentDate
                );

        if (!insertedLocally) {
            restoreSaveButton();

            Toast.makeText(
                    this,
                    "Unable to save workout locally",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        String firebaseId =
                workoutsReference.push().getKey();

        if (firebaseId == null) {
            restoreSaveButton();

            Toast.makeText(
                    this,
                    "Saved locally, but Firebase ID was not created",
                    Toast.LENGTH_LONG
            ).show();

            clearFields();
            finish();
            return;
        }

        Map<String, Object> workoutData =
                new HashMap<>();

        workoutData.put("firebaseId", firebaseId);
        workoutData.put("userId", currentUser.getUid());
        workoutData.put("workoutName", workoutName);
        workoutData.put("duration", duration);
        workoutData.put("calories", calories);
        workoutData.put("date", currentDate);
        workoutData.put(
                "createdAt",
                System.currentTimeMillis()
        );

        workoutsReference
                .child(firebaseId)
                .setValue(workoutData)
                .addOnSuccessListener(unused -> {
                    restoreSaveButton();
                    showSuccessDialog();
                })
                .addOnFailureListener(exception -> {
                    restoreSaveButton();

                    Toast.makeText(
                            AddWorkoutActivity.this,
                            "Saved locally, but Firebase failed:\n"
                                    + exception.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();

                    clearFields();
                    finish();
                });
    }

    private void setSavingState() {
        btnSaveWorkout.setEnabled(false);
        btnSaveWorkout.setText("Saving...");
    }

    private void clearInputErrors() {
        etWorkoutName.setError(null);
        etDuration.setError(null);
        etCalories.setError(null);
    }

    private void showSuccessDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("🎉 Workout Saved")
                .setMessage(
                        "Your workout has been saved successfully.\n\n"
                                + "Keep pushing towards your fitness goals! 💪"
                )
                .setCancelable(false)
                .setPositiveButton(
                        "Awesome!",
                        (dialog, which) -> {
                            dialog.dismiss();
                            clearFields();
                            finish();
                        }
                )
                .show();
    }

    private void clearFields() {
        etWorkoutName.setText("");
        etDuration.setText("");
        etCalories.setText("");
    }

    private void restoreSaveButton() {
        btnSaveWorkout.setEnabled(true);
        btnSaveWorkout.setText("Save Workout");
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (bottomNavigationView != null
                && bottomNavigationView.getSelectedItemId()
                != R.id.nav_add_workout) {
            bottomNavigationView.setSelectedItemId(
                    R.id.nav_add_workout
            );
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (dbHelper != null) {
            dbHelper.close();
        }
    }
}