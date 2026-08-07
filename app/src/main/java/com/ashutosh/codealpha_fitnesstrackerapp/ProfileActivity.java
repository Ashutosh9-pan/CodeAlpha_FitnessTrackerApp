package com.ashutosh.codealpha_fitnesstrackerapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Locale;

import model.UserProfile;
import utils.ThemeManager;

public class ProfileActivity extends AppCompatActivity {

    private static final String FIREBASE_DATABASE_URL =
            "https://fitnesstrackerapp-cb360-default-rtdb.asia-southeast1.firebasedatabase.app";

    private static final int MIN_AGE = 10;
    private static final int MAX_AGE = 120;

    private static final double MIN_HEIGHT_CM = 50.0;
    private static final double MAX_HEIGHT_CM = 250.0;

    private static final double MIN_WEIGHT_KG = 20.0;
    private static final double MAX_WEIGHT_KG = 400.0;

    private TextInputEditText etName;
    private TextInputEditText etAge;
    private TextInputEditText etHeight;
    private TextInputEditText etWeight;

    private AutoCompleteTextView actGoal;

    private TextView txtBMI;
    private TextView txtBMIStatus;

    private MaterialButton btnSaveProfile;
    private BottomNavigationView bottomNavigationView;

    private FirebaseAuth firebaseAuth;
    private FirebaseUser currentUser;
    private DatabaseReference userReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applySavedTheme(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        initialiseViews();
        applyBottomNavigationInsets();
        setupBottomNavigation();
        initialiseFirebase();
        setupGoalDropdown();
        setupListeners();
        loadProfile();
    }

    private void initialiseViews() {
        etName = findViewById(R.id.etName);
        etAge = findViewById(R.id.etAge);
        etHeight = findViewById(R.id.etHeight);
        etWeight = findViewById(R.id.etWeight);

        actGoal = findViewById(R.id.actGoal);

        txtBMI = findViewById(R.id.txtBMI);
        txtBMIStatus = findViewById(R.id.txtBMIStatus);

        btnSaveProfile =
                findViewById(R.id.btnSaveProfile);

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

            if (itemId == R.id.nav_profile) {
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

            if (itemId == R.id.nav_add_workout) {
                openBottomNavigationDestination(
                        AddWorkoutActivity.class
                );
                return true;
            }

            if (itemId == R.id.nav_history) {
                openBottomNavigationDestination(
                        HistoryActivity.class
                );
                return true;
            }

            return false;
        });

        bottomNavigationView.setSelectedItemId(
                R.id.nav_profile
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

    private void initialiseFirebase() {

        firebaseAuth = FirebaseAuth.getInstance();
        currentUser = firebaseAuth.getCurrentUser();

        FirebaseDatabase firebaseDatabase =
                FirebaseDatabase.getInstance(
                        FIREBASE_DATABASE_URL
                );

        if (currentUser != null) {

            userReference =
                    firebaseDatabase
                            .getReference("users")
                            .child(currentUser.getUid());

        } else {

            userReference =
                    firebaseDatabase
                            .getReference("users")
                            .child("default_user");
        }
    }

    private void setupGoalDropdown() {
        String[] fitnessGoals = {
                "Lose Weight",
                "Gain Muscle",
                "Maintain Weight",
                "Improve Fitness",
                "Improve Endurance",
                "Build Strength"
        };

        ArrayAdapter<String> goalAdapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_dropdown_item_1line,
                        fitnessGoals
                );

        actGoal.setAdapter(goalAdapter);

        actGoal.setOnClickListener(view ->
                actGoal.showDropDown()
        );
    }

    private void setupListeners() {
        btnSaveProfile.setOnClickListener(
                view -> saveProfile()
        );

        etHeight.setOnFocusChangeListener(
                (view, hasFocus) -> {
                    if (!hasFocus) {
                        updateBmiPreview();
                    }
                }
        );

        etWeight.setOnFocusChangeListener(
                (view, hasFocus) -> {
                    if (!hasFocus) {
                        updateBmiPreview();
                    }
                }
        );
    }

    private void loadProfile() {
        if (userReference == null) {
            return;
        }

        btnSaveProfile.setEnabled(false);
        btnSaveProfile.setText("Loading...");

        userReference.addListenerForSingleValueEvent(
                new ValueEventListener() {

                    @Override
                    public void onDataChange(
                            @NonNull DataSnapshot snapshot
                    ) {
                        restoreSaveButton();

                        if (!snapshot.exists()) {
                            resetBmiView();
                            return;
                        }

                        UserProfile profile =
                                snapshot.getValue(
                                        UserProfile.class
                                );

                        if (profile == null) {
                            resetBmiView();
                            return;
                        }

                        populateProfile(profile);
                    }

                    @Override
                    public void onCancelled(
                            @NonNull DatabaseError error
                    ) {
                        restoreSaveButton();

                        Toast.makeText(
                                ProfileActivity.this,
                                "Unable to load profile: "
                                        + error.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    private void populateProfile(
            UserProfile profile
    ) {
        etName.setText(
                safeText(profile.getName())
        );

        if (profile.getAge() > 0) {
            etAge.setText(
                    String.valueOf(
                            profile.getAge()
                    )
            );
        }

        if (profile.getHeight() > 0) {
            etHeight.setText(
                    formatDecimal(
                            profile.getHeight()
                    )
            );
        }

        if (profile.getWeight() > 0) {
            etWeight.setText(
                    formatDecimal(
                            profile.getWeight()
                    )
            );
        }

        actGoal.setText(
                safeText(profile.getGoal()),
                false
        );

        if (profile.getBmi() > 0) {
            displayBmi(
                    profile.getBmi(),
                    profile.getBmiCategory()
            );
        } else {
            resetBmiView();
        }
    }

    private void saveProfile() {
        clearInputErrors();

        String name =
                getText(etName);

        String ageText =
                getText(etAge);

        String heightText =
                getText(etHeight);

        String weightText =
                getText(etWeight);

        String goal =
                actGoal.getText() == null
                        ? ""
                        : actGoal.getText()
                        .toString()
                        .trim();

        if (TextUtils.isEmpty(name)) {
            etName.setError("Enter your full name");
            etName.requestFocus();
            return;
        }

        if (name.length() < 2) {
            etName.setError(
                    "Name must contain at least 2 characters"
            );
            etName.requestFocus();
            return;
        }

        if (name.length() > 50) {
            etName.setError(
                    "Name cannot exceed 50 characters"
            );
            etName.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(ageText)) {
            etAge.setError("Enter your age");
            etAge.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(heightText)) {
            etHeight.setError("Enter your height");
            etHeight.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(weightText)) {
            etWeight.setError("Enter your weight");
            etWeight.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(goal)) {
            actGoal.setError("Select your fitness goal");
            actGoal.requestFocus();
            actGoal.showDropDown();
            return;
        }

        int age;
        double height;
        double weight;

        try {
            age = Integer.parseInt(ageText);
            height = Double.parseDouble(heightText);
            weight = Double.parseDouble(weightText);

        } catch (NumberFormatException exception) {
            Toast.makeText(
                    this,
                    "Enter valid age, height and weight",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (age < MIN_AGE || age > MAX_AGE) {
            etAge.setError(
                    "Enter age between "
                            + MIN_AGE
                            + " and "
                            + MAX_AGE
            );
            etAge.requestFocus();
            return;
        }

        if (height < MIN_HEIGHT_CM
                || height > MAX_HEIGHT_CM) {

            etHeight.setError(
                    "Enter height between "
                            + formatDecimal(MIN_HEIGHT_CM)
                            + " and "
                            + formatDecimal(MAX_HEIGHT_CM)
                            + " cm"
            );

            etHeight.requestFocus();
            return;
        }

        if (weight < MIN_WEIGHT_KG
                || weight > MAX_WEIGHT_KG) {

            etWeight.setError(
                    "Enter weight between "
                            + formatDecimal(MIN_WEIGHT_KG)
                            + " and "
                            + formatDecimal(MAX_WEIGHT_KG)
                            + " kg"
            );

            etWeight.requestFocus();
            return;
        }

        double bmi =
                calculateBmi(
                        height,
                        weight
                );

        String bmiCategory =
                getBmiCategory(bmi);

        displayBmi(
                bmi,
                bmiCategory
        );

        String authenticatedEmail =
                currentUser != null
                        && currentUser.getEmail() != null
                        ? currentUser.getEmail()
                        : "";

        UserProfile userProfile =
                new UserProfile(
                        name,
                        authenticatedEmail,
                        age,
                        height,
                        weight,
                        goal,
                        bmi,
                        bmiCategory
                );

        saveProfileToFirebase(userProfile);
    }

    private void saveProfileToFirebase(
            UserProfile userProfile
    ) {
        if (userReference == null) {
            Toast.makeText(
                    this,
                    "Profile database is unavailable",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        setSavingState();

        userReference
                .setValue(userProfile)
                .addOnSuccessListener(unused -> {
                    restoreSaveButton();

                    Toast.makeText(
                            ProfileActivity.this,
                            "Profile saved successfully",
                            Toast.LENGTH_SHORT
                    ).show();
                })
                .addOnFailureListener(exception -> {
                    restoreSaveButton();

                    Toast.makeText(
                            ProfileActivity.this,
                            "Unable to save profile: "
                                    + exception.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void updateBmiPreview() {
        String heightText =
                getText(etHeight);

        String weightText =
                getText(etWeight);

        if (heightText.isEmpty()
                || weightText.isEmpty()) {

            resetBmiView();
            return;
        }

        try {
            double height =
                    Double.parseDouble(
                            heightText
                    );

            double weight =
                    Double.parseDouble(
                            weightText
                    );

            if (height <= 0 || weight <= 0) {
                resetBmiView();
                return;
            }

            double bmi =
                    calculateBmi(
                            height,
                            weight
                    );

            displayBmi(
                    bmi,
                    getBmiCategory(bmi)
            );

        } catch (NumberFormatException exception) {
            resetBmiView();
        }
    }

    private double calculateBmi(
            double heightCm,
            double weightKg
    ) {
        double heightMetres =
                heightCm / 100.0;

        double bmi =
                weightKg
                        / (heightMetres * heightMetres);

        return Math.round(bmi * 10.0) / 10.0;
    }

    private String getBmiCategory(
            double bmi
    ) {
        if (bmi < 18.5) {
            return "Underweight";
        }

        if (bmi < 25.0) {
            return "Normal";
        }

        if (bmi < 30.0) {
            return "Overweight";
        }

        return "Obese";
    }

    private void displayBmi(
            double bmi,
            String category
    ) {
        txtBMI.setText(
                String.format(
                        Locale.getDefault(),
                        "%.1f",
                        bmi
                )
        );

        if (category == null
                || category.trim().isEmpty()) {

            category = getBmiCategory(bmi);
        }

        txtBMIStatus.setText(category);
    }

    private void resetBmiView() {
        txtBMI.setText("0.0");
        txtBMIStatus.setText(
                "Enter your details"
        );
    }

    private String getText(
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

    private String safeText(
            String value
    ) {
        return value == null
                ? ""
                : value;
    }

    private String formatDecimal(
            double value
    ) {
        if (value == Math.floor(value)) {
            return String.format(
                    Locale.getDefault(),
                    "%.0f",
                    value
            );
        }

        return String.format(
                Locale.getDefault(),
                "%.1f",
                value
        );
    }

    private void clearInputErrors() {
        etName.setError(null);
        etAge.setError(null);
        etHeight.setError(null);
        etWeight.setError(null);
        actGoal.setError(null);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (bottomNavigationView != null
                && bottomNavigationView.getSelectedItemId()
                != R.id.nav_profile) {
            bottomNavigationView.setSelectedItemId(
                    R.id.nav_profile
            );
        }
    }

    private void setSavingState() {
        btnSaveProfile.setEnabled(false);
        btnSaveProfile.setText("Saving...");
    }

    private void restoreSaveButton() {
        btnSaveProfile.setEnabled(true);
        btnSaveProfile.setText("Save Profile");
    }
}