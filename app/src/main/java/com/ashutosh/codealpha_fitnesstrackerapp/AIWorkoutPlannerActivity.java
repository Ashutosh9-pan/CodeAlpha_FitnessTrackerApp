package com.ashutosh.codealpha_fitnesstrackerapp;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.ashutosh.codealpha_fitnesstrackerapp.ai.GeminiManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import utils.ThemeManager;

public class AIWorkoutPlannerActivity extends AppCompatActivity {

    private static final String DATABASE_URL =
            "https://fitnesstrackerapp-cb360-default-rtdb.asia-southeast1.firebasedatabase.app";

    private TextView txtWorkoutProfile;
    private TextView txtWorkoutResult;

    private AutoCompleteTextView dropdownWorkoutGoal;
    private AutoCompleteTextView dropdownWorkoutType;
    private AutoCompleteTextView dropdownExperience;
    private AutoCompleteTextView dropdownDuration;

    private TextInputEditText etWorkoutRequest;

    private MaterialButton btnGenerateWorkout;
    private MaterialCardView cardWorkoutResult;
    private ProgressBar progressWorkout;

    private GeminiManager geminiManager;
    private DatabaseReference userReference;

    private boolean profileLoaded = false;

    private String userContext =
            "Name: Not available\n"
                    + "Age: Not available\n"
                    + "Fitness goal: General Fitness";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applySavedTheme(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_workout_planner);

        initialiseViews();
        setupDropdowns();

        geminiManager = new GeminiManager();

        initialiseFirebase();

        btnGenerateWorkout.setOnClickListener(
                view -> generateWorkoutPlan()
        );
    }

    private void initialiseViews() {
        txtWorkoutProfile =
                findViewById(R.id.txtWorkoutProfile);

        txtWorkoutResult =
                findViewById(R.id.txtWorkoutResult);

        dropdownWorkoutGoal =
                findViewById(R.id.dropdownWorkoutGoal);

        dropdownWorkoutType =
                findViewById(R.id.dropdownWorkoutType);

        dropdownExperience =
                findViewById(R.id.dropdownExperience);

        dropdownDuration =
                findViewById(R.id.dropdownDuration);

        etWorkoutRequest =
                findViewById(R.id.etWorkoutRequest);

        btnGenerateWorkout =
                findViewById(R.id.btnGenerateWorkout);

        cardWorkoutResult =
                findViewById(R.id.cardWorkoutResult);

        progressWorkout =
                findViewById(R.id.progressWorkout);
    }

    private void setupDropdowns() {
        setDropdownOptions(
                dropdownWorkoutGoal,
                new String[]{
                        "Improve Fitness",
                        "Muscle Gain",
                        "Healthy Activity",
                        "Strength",
                        "Endurance",
                        "Mobility"
                }
        );

        setDropdownOptions(
                dropdownWorkoutType,
                new String[]{
                        "Full Body",
                        "Chest",
                        "Back",
                        "Legs",
                        "Shoulders",
                        "Arms",
                        "Push",
                        "Pull",
                        "Core",
                        "Cardio"
                }
        );

        setDropdownOptions(
                dropdownExperience,
                new String[]{
                        "Beginner",
                        "Intermediate",
                        "Advanced"
                }
        );

        setDropdownOptions(
                dropdownDuration,
                new String[]{
                        "20 Minutes",
                        "30 Minutes",
                        "45 Minutes",
                        "60 Minutes"
                }
        );

        dropdownWorkoutGoal.setText(
                "Improve Fitness",
                false
        );

        dropdownWorkoutType.setText(
                "Full Body",
                false
        );

        dropdownExperience.setText(
                "Beginner",
                false
        );

        dropdownDuration.setText(
                "45 Minutes",
                false
        );
    }

    private void setDropdownOptions(
            AutoCompleteTextView dropdown,
            String[] options
    ) {
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_dropdown_item_1line,
                        options
                );

        dropdown.setAdapter(adapter);
    }

    private void initialiseFirebase() {
        FirebaseUser currentUser =
                FirebaseAuth
                        .getInstance()
                        .getCurrentUser();

        if (currentUser == null) {
            txtWorkoutProfile.setText(
                    "Please sign in to create a personalized workout."
            );

            btnGenerateWorkout.setEnabled(false);
            return;
        }

        userReference =
                FirebaseDatabase
                        .getInstance(DATABASE_URL)
                        .getReference("users")
                        .child(currentUser.getUid());

        loadProfile(currentUser);
    }

    private void loadProfile(
            @NonNull FirebaseUser currentUser
    ) {
        txtWorkoutProfile.setText("Loading profile...");

        userReference.addListenerForSingleValueEvent(
                new ValueEventListener() {

                    @Override
                    public void onDataChange(
                            @NonNull DataSnapshot snapshot
                    ) {
                        String name =
                                getString(
                                        snapshot,
                                        "name"
                                );

                        if (isBlank(name)) {
                            name = currentUser.getDisplayName();
                        }

                        if (isBlank(name)) {
                            name = "VitaFit User";
                        }

                        long age =
                                getLong(
                                        snapshot.child("age")
                                );

                        String goal =
                                getString(
                                        snapshot,
                                        "goal"
                                );

                        if (isBlank(goal)) {
                            goal = "General Fitness";
                        }

                        String ageText =
                                age > 0
                                        ? String.valueOf(age)
                                        : "Not available";

                        userContext =
                                "Name: " + name
                                        + "\nAge: " + ageText
                                        + "\nFitness goal: " + goal;

                        txtWorkoutProfile.setText(
                                "Name: " + name
                                        + "\nAge: " + ageText
                                        + "\nGoal: " + goal
                        );

                        profileLoaded = true;
                    }

                    @Override
                    public void onCancelled(
                            @NonNull DatabaseError error
                    ) {
                        txtWorkoutProfile.setText(
                                "Unable to load profile."
                        );

                        Toast.makeText(
                                AIWorkoutPlannerActivity.this,
                                error.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    private void generateWorkoutPlan() {
        if (!profileLoaded) {
            Toast.makeText(
                    this,
                    "Please wait while your profile loads.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String selectedGoal =
                dropdownWorkoutGoal
                        .getText()
                        .toString()
                        .trim();

        String workoutType =
                dropdownWorkoutType
                        .getText()
                        .toString()
                        .trim();

        String experience =
                dropdownExperience
                        .getText()
                        .toString()
                        .trim();

        String duration =
                dropdownDuration
                        .getText()
                        .toString()
                        .trim();

        String specialRequest =
                getInputText(etWorkoutRequest);

        if (specialRequest.isEmpty()) {
            specialRequest =
                    "No special restriction or equipment request";
        }

        String workoutRequest =
                "Create a personalized workout session using the "
                        + "user profile supplied in the fitness context.\n\n"

                        + "Selected workout goal: "
                        + selectedGoal
                        + "\nWorkout type: "
                        + workoutType
                        + "\nExperience level: "
                        + experience
                        + "\nAvailable duration: "
                        + duration
                        + "\nSpecial request: "
                        + specialRequest
                        + "\n\n"

                        + "The response must include:\n"
                        + "• Workout title\n"
                        + "• Warm-up with duration\n"
                        + "• Main exercises in correct order\n"
                        + "• Sets and repetitions or time\n"
                        + "• Rest periods\n"
                        + "• Cool-down\n"
                        + "• Approximate total duration\n"
                        + "• One technique or safety tip\n\n"

                        + "Match the difficulty to the selected experience "
                        + "level. Respect equipment or physical restrictions "
                        + "mentioned by the user. Do not encourage unsafe form, "
                        + "extreme volume, maximum-load attempts, over-exercise, "
                        + "or training through pain. Keep the plan age-appropriate "
                        + "for a teen, focus on health and safe skill progression, "
                        + "and do not frame the workout around weight loss, body "
                        + "shape or appearance. Include easier alternatives and "
                        + "a reminder for recovery and rest days.";

        setLoadingState(true);

        geminiManager.generateResponse(
                workoutRequest,
                userContext,
                new GeminiManager.GeminiCallback() {

                    @Override
                    public void onSuccess(String response) {
                        runOnUiThread(
                                () -> {
                                    setLoadingState(false);

                                    txtWorkoutResult.setText(response);

                                    cardWorkoutResult.setVisibility(
                                            View.VISIBLE
                                    );
                                }
                        );
                    }

                    @Override
                    public void onError(String errorMessage) {
                        runOnUiThread(
                                () -> {
                                    setLoadingState(false);

                                    Toast.makeText(
                                            AIWorkoutPlannerActivity.this,
                                            errorMessage,
                                            Toast.LENGTH_LONG
                                    ).show();
                                }
                        );
                    }
                }
        );
    }

    private void setLoadingState(boolean loading) {
        btnGenerateWorkout.setEnabled(!loading);

        btnGenerateWorkout.setText(
                loading
                        ? "Generating Workout..."
                        : "✨ Generate Workout Plan"
        );

        progressWorkout.setVisibility(
                loading
                        ? View.VISIBLE
                        : View.GONE
        );

        if (loading) {
            cardWorkoutResult.setVisibility(View.GONE);
        }
    }

    private String getInputText(
            TextInputEditText editText
    ) {
        if (editText.getText() == null) {
            return "";
        }

        return editText
                .getText()
                .toString()
                .trim();
    }

    private String getString(
            DataSnapshot snapshot,
            String field
    ) {
        String value =
                snapshot
                        .child(field)
                        .getValue(String.class);

        return value == null
                ? ""
                : value.trim();
    }

    private long getLong(
            DataSnapshot snapshot
    ) {
        Object value = snapshot.getValue();

        if (value instanceof Number) {
            return ((Number) value).longValue();
        }

        if (value instanceof String) {
            try {
                return Long.parseLong(
                        ((String) value).trim()
                );
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }

        return 0;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}