package com.ashutosh.codealpha_fitnesstrackerapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Locale;
import java.util.Random;

import utils.ThemeManager;

public class RecommendationActivity extends AppCompatActivity {

    private static final String DATABASE_URL =
            "https://fitnesstrackerapp-cb360-default-rtdb.asia-southeast1.firebasedatabase.app";

    private static final long GENERATION_DELAY = 1200;

    private TextView txtProfileSummary;
    private TextView txtFitnessLevel;
    private TextView txtCalories;
    private TextView txtProtein;
    private TextView txtWater;
    private TextView txtSleep;
    private TextView txtWorkoutDuration;
    private TextView txtWeeklyPlan;
    private TextView txtNutritionTip;
    private TextView txtMotivation;

    private MaterialButton btnGeneratePlan;

    private FirebaseAuth firebaseAuth;
    private FirebaseUser currentUser;
    private DatabaseReference profileReference;

    private final Random random = new Random();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applySavedTheme(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recommendation);

        initialiseViews();

        if (!initialiseFirebase()) {
            return;
        }

        setupListeners();
        loadProfileAndGeneratePlan();
    }

    private void initialiseViews() {
        txtProfileSummary =
                findViewById(R.id.txtProfileSummary);

        txtFitnessLevel =
                findViewById(R.id.txtFitnessLevel);

        txtCalories =
                findViewById(R.id.txtCalories);

        txtProtein =
                findViewById(R.id.txtProtein);

        txtWater =
                findViewById(R.id.txtWater);

        txtSleep =
                findViewById(R.id.txtSleep);

        txtWorkoutDuration =
                findViewById(R.id.txtWorkoutDuration);

        txtWeeklyPlan =
                findViewById(R.id.txtWeeklyPlan);

        txtNutritionTip =
                findViewById(R.id.txtNutritionTip);

        txtMotivation =
                findViewById(R.id.txtMotivation);

        btnGeneratePlan =
                findViewById(R.id.btnGeneratePlan);
    }

    private boolean initialiseFirebase() {
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
                            RecommendationActivity.this,
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

        profileReference =
                FirebaseDatabase
                        .getInstance(DATABASE_URL)
                        .getReference("users")
                        .child(currentUser.getUid());

        return true;
    }

    private void setupListeners() {
        btnGeneratePlan.setOnClickListener(view ->
                generatePlanWithLoading()
        );
    }

    private void generatePlanWithLoading() {
        setGeneratingState(true);

        txtFitnessLevel.setText(
                "👤 Fitness Level\nAnalysing..."
        );

        txtCalories.setText(
                "🔥 Daily Calories\nCalculating..."
        );

        txtProtein.setText(
                "🍗 Protein Target\nCalculating..."
        );

        txtWater.setText(
                "💧 Water Intake\nCalculating..."
        );

        txtSleep.setText(
                "😴 Sleep\nAnalysing..."
        );

        txtWorkoutDuration.setText(
                "⏱ Workout Duration\nCalculating..."
        );

        txtWeeklyPlan.setText(
                "📅 Weekly Workout Plan\nGenerating..."
        );

        txtNutritionTip.setText(
                "🥗 Nutrition Tip\nGenerating..."
        );

        txtMotivation.setText(
                "💡 Motivation\nGenerating..."
        );

        new Handler(
                Looper.getMainLooper()
        ).postDelayed(
                this::loadProfileAndGeneratePlan,
                GENERATION_DELAY
        );
    }

    private void loadProfileAndGeneratePlan() {
        if (profileReference == null) {
            setGeneratingState(false);
            return;
        }

        profileReference
                .get()
                .addOnSuccessListener(snapshot -> {

                    Object heightValue =
                            snapshot
                                    .child("height")
                                    .getValue();

                    Object weightValue =
                            snapshot
                                    .child("weight")
                                    .getValue();

                    Object ageValue =
                            snapshot
                                    .child("age")
                                    .getValue();

                    String goal =
                            snapshot
                                    .child("goal")
                                    .getValue(String.class);

                    double height =
                            convertToDouble(heightValue);

                    double weight =
                            convertToDouble(weightValue);

                    int age =
                            convertToInt(ageValue);

                    if (goal == null
                            || goal.trim().isEmpty()) {

                        goal = "Improve Fitness";
                    }

                    if (height <= 0
                            || weight <= 0
                            || age <= 0) {

                        showIncompleteProfileMessage();
                        setGeneratingState(false);
                        return;
                    }

                    double bmi =
                            calculateBmi(
                                    height,
                                    weight
                            );

                    updateProfileSummary(
                            age,
                            height,
                            weight,
                            bmi,
                            goal
                    );

                    generateRecommendation(
                            age,
                            weight,
                            bmi,
                            goal
                    );

                    setGeneratingState(false);
                })
                .addOnFailureListener(exception -> {
                    setGeneratingState(false);

                    Toast.makeText(
                            RecommendationActivity.this,
                            "Unable to load profile: "
                                    + exception.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private double convertToDouble(
            Object value
    ) {
        if (value == null) {
            return 0.0;
        }

        if (value instanceof Double) {
            return (Double) value;
        }

        if (value instanceof Long) {
            return ((Long) value).doubleValue();
        }

        if (value instanceof Integer) {
            return ((Integer) value).doubleValue();
        }

        if (value instanceof Float) {
            return ((Float) value).doubleValue();
        }

        try {
            return Double.parseDouble(
                    value.toString()
            );

        } catch (NumberFormatException exception) {
            return 0.0;
        }
    }

    private int convertToInt(
            Object value
    ) {
        if (value == null) {
            return 0;
        }

        if (value instanceof Long) {
            return ((Long) value).intValue();
        }

        if (value instanceof Integer) {
            return (Integer) value;
        }

        if (value instanceof Double) {
            return ((Double) value).intValue();
        }

        if (value instanceof Float) {
            return ((Float) value).intValue();
        }

        try {
            return Integer.parseInt(
                    value.toString()
            );

        } catch (NumberFormatException exception) {
            try {
                return (int) Double.parseDouble(
                        value.toString()
                );

            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
    }

    private void updateProfileSummary(
            int age,
            double height,
            double weight,
            double bmi,
            String goal
    ) {
        String profileSummary =
                "Age: "
                        + age
                        + " years"
                        + "\nHeight: "
                        + formatNumber(height)
                        + " cm"
                        + "\nWeight: "
                        + formatNumber(weight)
                        + " kg"
                        + "\nBMI: "
                        + String.format(
                        Locale.getDefault(),
                        "%.1f",
                        bmi
                )
                        + " ("
                        + getBmiCategory(bmi)
                        + ")"
                        + "\nGoal: "
                        + goal;

        txtProfileSummary.setText(
                profileSummary
        );
    }

    private void generateRecommendation(
            int age,
            double weight,
            double bmi,
            String goal
    ) {
        String safeGoal =
                goal == null
                        ? "Improve Fitness"
                        : goal.trim();

        String fitnessLevel =
                getFitnessLevel(
                        age,
                        bmi
                );

        int dailyCalories =
                calculateDailyCalories(
                        weight,
                        safeGoal
                );

        int proteinTarget =
                calculateProteinTarget(
                        weight,
                        safeGoal
                );

        double waterTarget =
                calculateWaterTarget(
                        weight
                );

        String sleepRecommendation =
                getSleepRecommendation(age);

        String workoutDuration =
                getWorkoutDuration(
                        fitnessLevel,
                        safeGoal
                );

        String weeklyPlan =
                getRandomWorkoutPlan(
                        safeGoal
                );

        String nutritionTip =
                getRandomNutritionTip(
                        safeGoal
                );

        String motivation =
                getRandomMotivation();

        txtFitnessLevel.setText(
                "👤 Fitness Level\n"
                        + fitnessLevel
        );

        txtCalories.setText(
                "🔥 Daily Calories\n"
                        + dailyCalories
                        + " kcal/day"
        );

        txtProtein.setText(
                "🍗 Protein Target\n"
                        + proteinTarget
                        + " g/day"
        );

        txtWater.setText(
                "💧 Water Intake\n"
                        + String.format(
                        Locale.getDefault(),
                        "%.1f",
                        waterTarget
                )
                        + " L/day"
        );

        txtSleep.setText(
                "😴 Sleep\n"
                        + sleepRecommendation
        );

        txtWorkoutDuration.setText(
                "⏱ Workout Duration\n"
                        + workoutDuration
        );

        txtWeeklyPlan.setText(
                "📅 Weekly Workout Plan\n\n"
                        + weeklyPlan
        );

        txtNutritionTip.setText(
                "🥗 Nutrition Tip\n"
                        + nutritionTip
        );

        txtMotivation.setText(
                "💡 Motivation\n“"
                        + motivation
                        + "”"
        );
    }

    private double calculateBmi(
            double heightCm,
            double weightKg
    ) {
        double heightMetres =
                heightCm / 100.0;

        if (heightMetres <= 0) {
            return 0.0;
        }

        return weightKg
                / (heightMetres * heightMetres);
    }

    private String getBmiCategory(
            double bmi
    ) {
        if (bmi < 18.5) {
            return "Below reference range";
        }

        if (bmi < 25.0) {
            return "Within reference range";
        }

        if (bmi < 30.0) {
            return "Above reference range";
        }

        return "High reference range";
    }

    private String getFitnessLevel(
            int age,
            double bmi
    ) {
        if (age < 18 || age >= 50) {
            return "Beginner";
        }

        if (bmi < 18.5 || bmi >= 30.0) {
            return "Beginner";
        }

        if (bmi < 27.5) {
            return "Intermediate";
        }

        return "Beginner to Intermediate";
    }

    private int calculateDailyCalories(
            double weight,
            String goal
    ) {
        if (isWeightLossGoal(goal)) {
            return (int) Math.round(
                    weight * 27
            );
        }

        if (isMuscleGoal(goal)) {
            return (int) Math.round(
                    weight * 35
            );
        }

        if (goal.equalsIgnoreCase(
                "Maintain Weight"
        )) {
            return (int) Math.round(
                    weight * 31
            );
        }

        return (int) Math.round(
                weight * 30
        );
    }

    private int calculateProteinTarget(
            double weight,
            String goal
    ) {
        double proteinMultiplier;

        if (isMuscleGoal(goal)) {
            proteinMultiplier = 1.8;

        } else if (isWeightLossGoal(goal)) {
            proteinMultiplier = 1.6;

        } else {
            proteinMultiplier = 1.4;
        }

        return (int) Math.round(
                weight * proteinMultiplier
        );
    }

    private double calculateWaterTarget(
            double weight
    ) {
        double litres =
                weight * 0.035;

        return Math.max(
                2.0,
                Math.round(litres * 10.0)
                        / 10.0
        );
    }

    private String getSleepRecommendation(
            int age
    ) {
        if (age < 18) {
            return "8–10 hours/night";
        }

        return "7–9 hours/night";
    }

    private String getWorkoutDuration(
            String fitnessLevel,
            String goal
    ) {
        if (fitnessLevel.equalsIgnoreCase(
                "Beginner"
        )) {
            return "30–45 minutes";
        }

        if (isMuscleGoal(goal)) {
            return "50–70 minutes";
        }

        if (isWeightLossGoal(goal)) {
            return "40–60 minutes";
        }

        return "45–60 minutes";
    }

    private String getRandomWorkoutPlan(
            String goal
    ) {
        String[] plans;

        if (isWeightLossGoal(goal)) {

            plans = new String[]{

                    "Monday: Brisk Walk + Core\n"
                            + "Tuesday: Full Body Strength\n"
                            + "Wednesday: Cycling or Jogging\n"
                            + "Thursday: Upper Body + Core\n"
                            + "Friday: Lower Body Strength\n"
                            + "Saturday: Low-Impact Cardio\n"
                            + "Sunday: Rest and Stretching",

                    "Monday: Full Body Circuit\n"
                            + "Tuesday: Walking + Mobility\n"
                            + "Wednesday: Lower Body\n"
                            + "Thursday: Cardio + Core\n"
                            + "Friday: Upper Body\n"
                            + "Saturday: Long Walk\n"
                            + "Sunday: Rest",

                    "Monday: Cardio Intervals\n"
                            + "Tuesday: Push Workout\n"
                            + "Wednesday: Lower Body\n"
                            + "Thursday: Light Cardio\n"
                            + "Friday: Pull Workout\n"
                            + "Saturday: Full Body Circuit\n"
                            + "Sunday: Recovery"
            };

        } else if (isMuscleGoal(goal)) {

            plans = new String[]{

                    "Monday: Chest + Triceps\n"
                            + "Tuesday: Back + Biceps\n"
                            + "Wednesday: Legs\n"
                            + "Thursday: Shoulders + Core\n"
                            + "Friday: Upper Body\n"
                            + "Saturday: Light Cardio\n"
                            + "Sunday: Rest",

                    "Monday: Push\n"
                            + "Tuesday: Pull\n"
                            + "Wednesday: Legs\n"
                            + "Thursday: Rest or Mobility\n"
                            + "Friday: Upper Body\n"
                            + "Saturday: Lower Body\n"
                            + "Sunday: Rest",

                    "Monday: Upper Body Strength\n"
                            + "Tuesday: Lower Body Strength\n"
                            + "Wednesday: Rest\n"
                            + "Thursday: Push Hypertrophy\n"
                            + "Friday: Pull Hypertrophy\n"
                            + "Saturday: Legs + Core\n"
                            + "Sunday: Rest"
            };

        } else {

            plans = new String[]{

                    "Monday: Upper Body\n"
                            + "Tuesday: Lower Body\n"
                            + "Wednesday: Cardio\n"
                            + "Thursday: Push\n"
                            + "Friday: Pull\n"
                            + "Saturday: Full Body\n"
                            + "Sunday: Rest",

                    "Monday: Full Body Strength\n"
                            + "Tuesday: Walking or Cycling\n"
                            + "Wednesday: Upper Body\n"
                            + "Thursday: Mobility + Core\n"
                            + "Friday: Lower Body\n"
                            + "Saturday: Light Cardio\n"
                            + "Sunday: Rest",

                    "Monday: Push\n"
                            + "Tuesday: Cardio\n"
                            + "Wednesday: Pull\n"
                            + "Thursday: Core + Mobility\n"
                            + "Friday: Legs\n"
                            + "Saturday: Full Body\n"
                            + "Sunday: Recovery"
            };
        }

        return plans[
                random.nextInt(
                        plans.length
                )
                ];
    }

    private String getRandomNutritionTip(
            String goal
    ) {
        String[] tips;

        if (isWeightLossGoal(goal)) {

            tips = new String[]{

                    "Build meals around vegetables, whole grains and lean protein.",

                    "Use a moderate calorie deficit instead of extreme restriction.",

                    "Choose filling foods with protein and fibre at most meals.",

                    "Limit sugary drinks and prefer water or unsweetened beverages."
            };

        } else if (isMuscleGoal(goal)) {

            tips = new String[]{

                    "Include a protein source with every main meal.",

                    "Eat enough carbohydrates to support strength training.",

                    "Spread protein intake across the day for better recovery.",

                    "Use a small calorie surplus rather than overeating."
            };

        } else {

            tips = new String[]{

                    "Choose balanced meals with protein, carbohydrates and healthy fats.",

                    "Eat a variety of fruits and vegetables throughout the week.",

                    "Stay consistent with meal timing and hydration.",

                    "Prefer minimally processed foods for most meals."
            };
        }

        return tips[
                random.nextInt(
                        tips.length
                )
                ];
    }

    private String getRandomMotivation() {
        String[] quotes = {

                "Consistency beats perfection.",

                "Small improvements create big results.",

                "Strong today, stronger tomorrow.",

                "Success starts with showing up.",

                "Every workout is progress.",

                "Your future self will thank you.",

                "Progress is built one session at a time."
        };

        return quotes[
                random.nextInt(
                        quotes.length
                )
                ];
    }

    private boolean isWeightLossGoal(
            String goal
    ) {
        if (goal == null) {
            return false;
        }

        return goal.equalsIgnoreCase(
                "Lose Weight"
        ) || goal.equalsIgnoreCase(
                "Weight Loss"
        );
    }

    private boolean isMuscleGoal(
            String goal
    ) {
        if (goal == null) {
            return false;
        }

        return goal.equalsIgnoreCase(
                "Gain Muscle"
        ) || goal.equalsIgnoreCase(
                "Muscle Gain"
        ) || goal.equalsIgnoreCase(
                "Build Strength"
        );
    }

    private String formatNumber(
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

    private void showIncompleteProfileMessage() {
        txtProfileSummary.setText(
                "Please complete your age, height, weight and fitness goal in Profile."
        );

        txtFitnessLevel.setText(
                "👤 Fitness Level\nUnavailable"
        );

        txtCalories.setText(
                "🔥 Daily Calories\nUnavailable"
        );

        txtProtein.setText(
                "🍗 Protein Target\nUnavailable"
        );

        txtWater.setText(
                "💧 Water Intake\nUnavailable"
        );

        txtSleep.setText(
                "😴 Sleep\nUnavailable"
        );

        txtWorkoutDuration.setText(
                "⏱ Workout Duration\nUnavailable"
        );

        txtWeeklyPlan.setText(
                "📅 Weekly Workout Plan\nComplete your profile to generate a plan."
        );

        txtNutritionTip.setText(
                "🥗 Nutrition Tip\nComplete your profile first."
        );

        txtMotivation.setText(
                "💡 Motivation\n“Complete your profile and start your journey.”"
        );
    }

    private void setGeneratingState(
            boolean isGenerating
    ) {
        btnGeneratePlan.setEnabled(
                !isGenerating
        );

        btnGeneratePlan.setText(
                isGenerating
                        ? "Generating..."
                        : "Generate New AI Plan"
        );
    }
}