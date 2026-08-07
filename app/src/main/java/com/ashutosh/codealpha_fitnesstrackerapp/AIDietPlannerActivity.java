package com.ashutosh.codealpha_fitnesstrackerapp;

import android.os.Bundle;
import android.view.View;
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

public class AIDietPlannerActivity extends AppCompatActivity {

    private static final String DATABASE_URL =
            "https://fitnesstrackerapp-cb360-default-rtdb.asia-southeast1.firebasedatabase.app";

    private TextView txtDietProfile;
    private TextView txtDietResult;

    private TextInputEditText etDietPreference;
    private TextInputEditText etDietRequest;

    private MaterialButton btnGenerateDiet;
    private MaterialCardView cardDietResult;
    private ProgressBar progressDiet;

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
        setContentView(R.layout.activity_ai_diet_planner);

        initialiseViews();
        geminiManager = new GeminiManager();

        initialiseFirebase();

        btnGenerateDiet.setOnClickListener(
                view -> generateDietPlan()
        );
    }

    private void initialiseViews() {
        txtDietProfile =
                findViewById(R.id.txtDietProfile);

        txtDietResult =
                findViewById(R.id.txtDietResult);

        etDietPreference =
                findViewById(R.id.etDietPreference);

        etDietRequest =
                findViewById(R.id.etDietRequest);

        btnGenerateDiet =
                findViewById(R.id.btnGenerateDiet);

        cardDietResult =
                findViewById(R.id.cardDietResult);

        progressDiet =
                findViewById(R.id.progressDiet);
    }

    private void initialiseFirebase() {

        FirebaseUser currentUser =
                FirebaseAuth
                        .getInstance()
                        .getCurrentUser();

        if (currentUser == null) {

            txtDietProfile.setText(
                    "Please sign in to generate a personalized diet plan."
            );

            btnGenerateDiet.setEnabled(false);
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

        txtDietProfile.setText("Loading profile...");

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

                        txtDietProfile.setText(
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

                        txtDietProfile.setText(
                                "Unable to load profile."
                        );

                        Toast.makeText(
                                AIDietPlannerActivity.this,
                                error.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    private void generateDietPlan() {

        if (!profileLoaded) {

            Toast.makeText(
                    this,
                    "Please wait while your profile loads.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String preference =
                getInputText(etDietPreference);

        String request =
                getInputText(etDietRequest);

        if (preference.isEmpty()) {
            preference = "No specific preference provided";
        }

        if (request.isEmpty()) {
            request = "Create a practical balanced Indian meal example";
        }

        String dietRequest =
                "Create a balanced one-day meal example using the "
                        + "food preferences supplied below.\n\n"

                        + "Diet preference: "
                        + preference
                        + "\nAdditional request: "
                        + request
                        + "\n\n"

                        + "The response must contain:\n"
                        + "• Breakfast\n"
                        + "• Mid-morning snack\n"
                        + "• Lunch\n"
                        + "• Evening snack\n"
                        + "• Dinner\n"
                        + "• General hydration reminder\n"
                        + "• Simple substitutions\n\n"

                        + "Use common, affordable Indian foods when possible. "
                        + "Keep portions flexible and encourage regular meals. "
                        + "Do not calculate calorie or macro targets. "
                        + "Do not recommend restrictive eating, weight-loss "
                        + "targets, unsafe supplements or skipping meals. "
                        + "For users under 18, keep all guidance general and "
                        + "age-appropriate. Mention that allergies or medical "
                        + "diet needs should be discussed with a qualified "
                        + "healthcare professional.";

        setLoadingState(true);

        geminiManager.generateResponse(
                dietRequest,
                userContext,
                new GeminiManager.GeminiCallback() {

                    @Override
                    public void onSuccess(String response) {

                        runOnUiThread(
                                () -> {
                                    setLoadingState(false);

                                    txtDietResult.setText(response);

                                    cardDietResult.setVisibility(
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
                                            AIDietPlannerActivity.this,
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

        btnGenerateDiet.setEnabled(!loading);

        btnGenerateDiet.setText(
                loading
                        ? "Generating Diet Plan..."
                        : "✨ Generate Balanced Meal Plan"
        );

        progressDiet.setVisibility(
                loading
                        ? View.VISIBLE
                        : View.GONE
        );

        if (loading) {
            cardDietResult.setVisibility(View.GONE);
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