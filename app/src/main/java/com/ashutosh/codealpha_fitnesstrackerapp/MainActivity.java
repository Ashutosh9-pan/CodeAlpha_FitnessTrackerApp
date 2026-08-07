package com.ashutosh.codealpha_fitnesstrackerapp;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

import utils.ThemeManager;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final String FIREBASE_DATABASE_URL =
            "https://fitnesstrackerapp-cb360-default-rtdb.asia-southeast1.firebasedatabase.app";

    private static final int DAILY_CALORIE_GOAL = 500;
    private static final int WEEKLY_WORKOUT_GOAL = 5;

    private TextView txtTitle;
    private TextView txtWelcome;
    private TextView txtDate;

    private TextView txtCalories;
    private TextView txtDuration;
    private TextView txtWorkouts;
    private TextView txtProgress;
    private TextView txtWeeklyGoal;
    private TextView txtInsight;
    private TextView txtQuote;
    private TextView txtRecentWorkout;

    private TextView txtNavName;
    private TextView txtNavGoal;
    private TextView txtNavBMI;

    private ProgressBar progressGoal;
    private ProgressBar progressWeeklyGoal;

    private Button btnAddWorkout;
    private Button btnHistory;
    private Button btnStatistics;

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private MaterialToolbar toolbar;
    private BottomNavigationView bottomNavigationView;

    private DatabaseReference userReference;
    private DatabaseReference workoutsReference;
    private ValueEventListener dashboardListener;

    private final Random random = new Random();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applySavedTheme(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initialiseViews();
        applyBottomNavigationInsets();
        showDashboardLoadingState();
        setupNavigationDrawer();
        setupBottomNavigation();

        if (!initialiseFirebase()) {
            return;
        }

        setupButtonListeners();
        setupGreetingAndDate();
        setupQuoteOfTheDay();
        loadNavigationHeader();
        animateDashboardEntrance();
    }

    private void initialiseViews() {
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        toolbar = findViewById(R.id.toolbar);
        bottomNavigationView = findViewById(R.id.bottomNavigationView);

        View headerView = navigationView.getHeaderView(0);
        txtNavName = headerView.findViewById(R.id.txtNavName);
        txtNavGoal = headerView.findViewById(R.id.txtNavGoal);
        txtNavBMI = headerView.findViewById(R.id.txtNavBMI);

        txtTitle = findViewById(R.id.txtTitle);
        txtWelcome = findViewById(R.id.txtWelcome);
        txtDate = findViewById(R.id.txtDate);

        txtCalories = findViewById(R.id.txtCalories);
        txtDuration = findViewById(R.id.txtDuration);
        txtWorkouts = findViewById(R.id.txtWorkouts);
        txtProgress = findViewById(R.id.txtProgress);
        txtWeeklyGoal = findViewById(R.id.txtWeeklyGoal);
        txtInsight = findViewById(R.id.txtInsight);
        txtQuote = findViewById(R.id.txtQuote);
        txtRecentWorkout = findViewById(R.id.txtRecentWorkout);

        progressGoal = findViewById(R.id.progressGoal);
        progressWeeklyGoal = findViewById(R.id.progressWeeklyGoal);

        btnAddWorkout = findViewById(R.id.btnAddWorkout);
        btnHistory = findViewById(R.id.btnHistory);
        btnStatistics = findViewById(R.id.btnStatistics);
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

    private void setupNavigationDrawer() {
        setSupportActionBar(toolbar);

        ActionBarDrawerToggle drawerToggle =
                new ActionBarDrawerToggle(
                        this,
                        drawerLayout,
                        toolbar,
                        R.string.navigation_drawer_open,
                        R.string.navigation_drawer_close
                );

        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();

        navigationView.setNavigationItemSelectedListener(this);
        navigationView.setCheckedItem(R.id.nav_dashboard);
    }

    private void setupBottomNavigation() {
        if (bottomNavigationView == null) {
            return;
        }

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_dashboard) {
                return true;
            }

            if (itemId == R.id.nav_statistics) {
                openActivity(StatisticsActivity.class);
                return true;
            }

            if (itemId == R.id.nav_add_workout) {
                openActivity(AddWorkoutActivity.class);
                return true;
            }

            if (itemId == R.id.nav_history) {
                openActivity(HistoryActivity.class);
                return true;
            }

            if (itemId == R.id.nav_profile) {
                openActivity(ProfileActivity.class);
                return true;
            }

            return false;
        });

        bottomNavigationView.setSelectedItemId(
                R.id.nav_dashboard
        );
    }

    private boolean initialiseFirebase() {
        FirebaseUser currentUser =
                FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(
                    this,
                    "Your session has expired. Please sign in again.",
                    Toast.LENGTH_LONG
            ).show();

            Intent intent =
                    new Intent(
                            MainActivity.this,
                            LoginActivity.class
                    );

            intent.setFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TASK
            );

            startActivity(intent);
            finish();
            return false;
        }

        FirebaseDatabase firebaseDatabase =
                FirebaseDatabase.getInstance(
                        FIREBASE_DATABASE_URL
                );

        userReference =
                firebaseDatabase
                        .getReference("users")
                        .child(currentUser.getUid());

        workoutsReference =
                userReference.child("workouts");

        return true;
    }

    private void setupGreetingAndDate() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);

        String greeting;

        if (hour < 12) {
            greeting = "Good Morning";
        } else if (hour < 17) {
            greeting = "Good Afternoon";
        } else {
            greeting = "Good Evening";
        }

        txtTitle.setText("👋 " + greeting);
        txtWelcome.setText("Ready to crush your fitness goals today? 💪");

        SimpleDateFormat dateFormat =
                new SimpleDateFormat(
                        "EEEE, dd MMMM yyyy",
                        Locale.getDefault()
                );

        txtDate.setText(dateFormat.format(new Date()));

        if (userReference == null) {
            return;
        }

        userReference
                .child("name")
                .get()
                .addOnSuccessListener(snapshot -> {
                    String name = snapshot.getValue(String.class);

                    if (name != null && !name.trim().isEmpty()) {
                        txtTitle.setText(
                                "👋 "
                                        + greeting
                                        + ", "
                                        + getFirstName(name)
                        );
                    }
                });
    }

    private String getFirstName(String fullName) {
        String cleanedName = fullName.trim();
        int firstSpace = cleanedName.indexOf(" ");

        if (firstSpace > 0) {
            return cleanedName.substring(0, firstSpace);
        }

        return cleanedName;
    }

    private void loadNavigationHeader() {
        if (userReference == null) {
            return;
        }

        userReference.get().addOnSuccessListener(snapshot -> {
            String name = snapshot.child("name").getValue(String.class);
            String goal = snapshot.child("goal").getValue(String.class);
            Double bmi = snapshot.child("bmi").getValue(Double.class);
            String bmiCategory =
                    snapshot.child("bmiCategory").getValue(String.class);

            txtNavName.setText(
                    name != null && !name.trim().isEmpty()
                            ? name
                            : "VitaFit User"
            );

            txtNavGoal.setText(
                    goal != null && !goal.trim().isEmpty()
                            ? "🎯 Goal: " + goal
                            : "🎯 Goal: Not Set"
            );

            if (bmi != null && bmi > 0) {
                String category =
                        bmiCategory == null || bmiCategory.trim().isEmpty()
                                ? ""
                                : " (" + bmiCategory + ")";

                txtNavBMI.setText(
                        "⚖ BMI: "
                                + String.format(
                                Locale.getDefault(),
                                "%.1f",
                                bmi
                        )
                                + category
                );
            } else {
                txtNavBMI.setText("⚖ BMI: Not Calculated");
            }
        });
    }

    private void setupQuoteOfTheDay() {
        String[] quotes = {
                "“Consistency beats motivation.”",
                "“Small progress is still progress.”",
                "“One workout today is better than none.”",
                "“Strong habits build a stronger you.”",
                "“Every workout moves you forward.”",
                "“Success starts with showing up.”",
                "“Your future self will thank you.”",
                "“Progress is built one session at a time.”"
        };

        txtQuote.setText(
                quotes[random.nextInt(quotes.length)]
        );
    }

    private void setupButtonListeners() {
        btnAddWorkout.setOnClickListener(view ->
                animateButtonAndOpenActivity(
                        view,
                        AddWorkoutActivity.class
                )
        );

        btnHistory.setOnClickListener(view ->
                animateButtonAndOpenActivity(
                        view,
                        HistoryActivity.class
                )
        );

        btnStatistics.setOnClickListener(view ->
                animateButtonAndOpenActivity(
                        view,
                        StatisticsActivity.class
                )
        );
    }

    private void animateButtonAndOpenActivity(
            View view,
            Class<?> destinationActivity
    ) {
        view.setEnabled(false);

        view.animate()
                .scaleX(0.96f)
                .scaleY(0.96f)
                .setDuration(90)
                .withEndAction(() ->
                        view.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(90)
                                .withEndAction(() -> {
                                    openActivity(destinationActivity);
                                    view.setEnabled(true);
                                })
                                .start()
                )
                .start();
    }

    private void openActivity(Class<?> destinationActivity) {
        Intent intent = new Intent(this, destinationActivity);
        startActivity(intent);

        overridePendingTransition(
                R.anim.slide_in_right,
                R.anim.slide_out_left
        );
    }

    private void animateDashboardEntrance() {
        animateViewEntrance(txtTitle, 50);
        animateViewEntrance(txtWelcome, 100);
        animateViewEntrance(txtDate, 150);
        animateViewEntrance(progressGoal, 220);
        animateViewEntrance(btnAddWorkout, 300);
        animateViewEntrance(btnHistory, 380);
        animateViewEntrance(btnStatistics, 460);
    }

    private void animateViewEntrance(
            View view,
            long startDelay
    ) {
        if (view == null) {
            return;
        }

        view.setAlpha(0f);
        view.setTranslationY(40f);

        view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(450)
                .setStartDelay(startDelay)
                .start();
    }

    private void showDashboardLoadingState() {
        txtCalories.setText("Loading...");
        txtDuration.setText("Loading...");
        txtWorkouts.setText("...");
        txtProgress.setText("...");
        txtWeeklyGoal.setText("Loading weekly progress...");
        txtRecentWorkout.setText("Loading recent activity...");

        progressGoal.setIndeterminate(true);
        progressWeeklyGoal.setProgress(0);

        setDashboardTextAlpha(0.6f);
    }

    private void setDashboardTextAlpha(float alpha) {
        txtCalories.setAlpha(alpha);
        txtDuration.setAlpha(alpha);
        txtWorkouts.setAlpha(alpha);
        txtProgress.setAlpha(alpha);
    }

    private void restoreDashboardTextOpacity() {
        txtCalories.animate().alpha(1f).setDuration(300).start();
        txtDuration.animate().alpha(1f).setDuration(300).start();
        txtWorkouts.animate().alpha(1f).setDuration(300).start();
        txtProgress.animate().alpha(1f).setDuration(300).start();
    }

    @Override
    protected void onStart() {
        super.onStart();
        startDashboardListener();
    }

    private void startDashboardListener() {
        if (workoutsReference == null || dashboardListener != null) {
            return;
        }

        dashboardListener = new ValueEventListener() {
            @Override
            public void onDataChange(
                    @NonNull DataSnapshot snapshot
            ) {
                int totalCalories = 0;
                int totalDuration = 0;
                int totalWorkouts = 0;
                int weeklyWorkouts = 0;

                String recentWorkoutName = "No workouts yet";
                long latestCreatedAt = Long.MIN_VALUE;

                for (DataSnapshot workoutSnapshot
                        : snapshot.getChildren()) {

                    int calories = convertToInt(
                            workoutSnapshot
                                    .child("calories")
                                    .getValue()
                    );

                    int duration = convertToInt(
                            workoutSnapshot
                                    .child("duration")
                                    .getValue()
                    );

                    String workoutDate =
                            workoutSnapshot
                                    .child("date")
                                    .getValue(String.class);

                    String workoutName =
                            workoutSnapshot
                                    .child("workoutName")
                                    .getValue(String.class);

                    long createdAt = convertToLong(
                            workoutSnapshot
                                    .child("createdAt")
                                    .getValue()
                    );

                    totalCalories += calories;
                    totalDuration += duration;
                    totalWorkouts++;

                    if (isWorkoutFromLastSevenDays(workoutDate)) {
                        weeklyWorkouts++;
                    }

                    if (createdAt >= latestCreatedAt) {
                        latestCreatedAt = createdAt;

                        if (workoutName != null
                                && !workoutName.trim().isEmpty()) {
                            recentWorkoutName = workoutName;
                        }
                    }
                }

                updateDashboardViews(
                        totalCalories,
                        totalDuration,
                        totalWorkouts,
                        weeklyWorkouts,
                        recentWorkoutName
                );
            }

            @Override
            public void onCancelled(
                    @NonNull DatabaseError error
            ) {
                progressGoal.setIndeterminate(false);
                progressGoal.setProgress(0);
                progressWeeklyGoal.setProgress(0);

                txtCalories.setText("0 kcal");
                txtDuration.setText("0 min");
                txtWorkouts.setText("0");
                txtProgress.setText("0%");
                txtWeeklyGoal.setText("0% Weekly Goal Completed");
                txtRecentWorkout.setText("Unable to load recent activity");

                restoreDashboardTextOpacity();

                Toast.makeText(
                        MainActivity.this,
                        "Unable to load dashboard: "
                                + error.getMessage(),
                        Toast.LENGTH_LONG
                ).show();
            }
        };

        workoutsReference.addValueEventListener(
                dashboardListener
        );
    }

    private void updateDashboardViews(
            int totalCalories,
            int totalDuration,
            int totalWorkouts,
            int weeklyWorkouts,
            String recentWorkoutName
    ) {
        progressGoal.setIndeterminate(false);

        int dailyProgress =
                (int) Math.min(
                        100L,
                        ((long) totalCalories * 100L)
                                / DAILY_CALORIE_GOAL
                );

        restoreDashboardTextOpacity();

        animateNumber(txtCalories, totalCalories, " kcal");
        animateNumber(txtDuration, totalDuration, " min");
        animateNumber(txtWorkouts, totalWorkouts, "");
        animateNumber(txtProgress, dailyProgress, "%");

        animateProgressBar(progressGoal, dailyProgress);
        updateWeeklyGoal(weeklyWorkouts);
        txtRecentWorkout.setText(recentWorkoutName);

        updateFitnessInsight(
                totalCalories,
                totalDuration,
                totalWorkouts,
                dailyProgress
        );
    }

    private void animateNumber(
            TextView textView,
            int finalValue,
            String suffix
    ) {
        ValueAnimator animator =
                ValueAnimator.ofInt(0, finalValue);

        animator.setDuration(700);
        animator.addUpdateListener(animation -> {
            int animatedValue =
                    (int) animation.getAnimatedValue();

            textView.setText(
                    animatedValue + suffix
            );
        });

        animator.start();
    }

    private void animateProgressBar(
            ProgressBar progressBar,
            int targetProgress
    ) {
        ObjectAnimator progressAnimator =
                ObjectAnimator.ofInt(
                        progressBar,
                        "progress",
                        progressBar.getProgress(),
                        targetProgress
                );

        progressAnimator.setDuration(700);
        progressAnimator.setInterpolator(
                new AccelerateDecelerateInterpolator()
        );
        progressAnimator.start();
    }

    private void updateWeeklyGoal(int weeklyWorkouts) {
        int weeklyProgress =
                Math.min(
                        100,
                        (weeklyWorkouts * 100)
                                / WEEKLY_WORKOUT_GOAL
                );

        animateProgressBar(
                progressWeeklyGoal,
                weeklyProgress
        );

        if (weeklyProgress >= 100) {
            txtWeeklyGoal.setText(
                    "100% Weekly Goal Completed 🎉"
            );
        } else {
            txtWeeklyGoal.setText(
                    weeklyProgress
                            + "% Weekly Goal Completed • "
                            + weeklyWorkouts
                            + "/"
                            + WEEKLY_WORKOUT_GOAL
                            + " workouts"
            );
        }
    }

    private boolean isWorkoutFromLastSevenDays(
            String workoutDate
    ) {
        if (workoutDate == null
                || workoutDate.trim().isEmpty()) {
            return false;
        }

        SimpleDateFormat dateFormat =
                new SimpleDateFormat(
                        "dd-MM-yyyy",
                        Locale.getDefault()
                );

        dateFormat.setLenient(false);

        try {
            Date parsedWorkoutDate =
                    dateFormat.parse(
                            workoutDate.trim()
                    );

            if (parsedWorkoutDate == null) {
                return false;
            }

            Calendar today = Calendar.getInstance();
            resetTime(today);

            Calendar sevenDaysAgo = Calendar.getInstance();
            resetTime(sevenDaysAgo);
            sevenDaysAgo.add(
                    Calendar.DAY_OF_YEAR,
                    -6
            );

            Calendar workoutCalendar = Calendar.getInstance();
            workoutCalendar.setTime(parsedWorkoutDate);
            resetTime(workoutCalendar);

            return !workoutCalendar.before(sevenDaysAgo)
                    && !workoutCalendar.after(today);

        } catch (ParseException exception) {
            return false;
        }
    }

    private void resetTime(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
    }

    private int convertToInt(Object value) {
        if (value == null) {
            return 0;
        }

        if (value instanceof Number) {
            return ((Number) value).intValue();
        }

        try {
            return (int) Double.parseDouble(value.toString());
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private long convertToLong(Object value) {
        if (value == null) {
            return 0L;
        }

        if (value instanceof Number) {
            return ((Number) value).longValue();
        }

        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException exception) {
            return 0L;
        }
    }

    private void updateFitnessInsight(
            int totalCalories,
            int totalDuration,
            int totalWorkouts,
            int progress
    ) {
        if (totalWorkouts == 0) {
            txtInsight.setText(
                    "Welcome to VitaFit! 🏋️\n\n"
                            + "Start your first workout today. "
                            + "Your personalised fitness insight will appear here."
            );
            return;
        }

        String suggestion;

        if (progress >= 100) {
            suggestion =
                    "🏆 Excellent work! You reached your calorie goal. "
                            + "Stay consistent and recover well.";
        } else if (totalWorkouts >= 5
                || totalCalories >= 1000) {
            suggestion =
                    "🔥 Great consistency! Keep balancing training, hydration and recovery.";
        } else if (totalDuration < 90) {
            suggestion =
                    "💡 Try adding 10–15 minutes to one workout this week for steady progress.";
        } else {
            suggestion =
                    "💪 You are building momentum. Keep showing up and progress gradually.";
        }

        String insight =
                "🔥 Total Calories: "
                        + totalCalories
                        + " kcal"
                        + "\n🏋 Total Workouts: "
                        + totalWorkouts
                        + "\n⏱ Total Duration: "
                        + totalDuration
                        + " min"
                        + "\n🎯 Goal Progress: "
                        + progress
                        + "%"
                        + "\n\n"
                        + suggestion;

        txtInsight.setText(insight);
    }

    @Override
    public boolean onNavigationItemSelected(
            @NonNull MenuItem item
    ) {
        int itemId = item.getItemId();

        if (itemId == R.id.nav_dashboard) {
            drawerLayout.closeDrawer(GravityCompat.START);

        } else if (itemId == R.id.nav_add_workout) {
            openActivity(AddWorkoutActivity.class);

        } else if (itemId == R.id.nav_history) {
            openActivity(HistoryActivity.class);

        } else if (itemId == R.id.nav_statistics) {
            openActivity(StatisticsActivity.class);

        } else if (itemId == R.id.nav_profile) {
            openActivity(ProfileActivity.class);

        } else if (itemId == R.id.nav_ai_hub) {
            openActivity(AIHubActivity.class);

        } else if (itemId == R.id.nav_reminder) {
            openActivity(ReminderActivity.class);

        } else if (itemId == R.id.nav_dark_mode) {
            boolean darkModeEnabled =
                    ThemeManager.isDarkModeEnabled(this);

            ThemeManager.setDarkMode(
                    this,
                    !darkModeEnabled
            );

            drawerLayout.closeDrawer(GravityCompat.START);
            recreate();
            return true;

        } else if (itemId == R.id.nav_about) {
            showAboutDialog();

        } else if (itemId == R.id.nav_logout) {
            showLogoutDialog();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void showAboutDialog() {
        String aboutMessage =
                "Your Personal AI Fitness Coach\n\n"
                        + "VitaFit helps users track workouts, monitor progress, "
                        + "receive personalised fitness recommendations, and stay "
                        + "consistent with daily reminders.\n\n"
                        + "Key Features\n\n"
                        + "✓ Firebase Authentication\n"
                        + "✓ User-Specific Workout Tracking\n"
                        + "✓ Workout History and Editing\n"
                        + "✓ Statistics and Progress Charts\n"
                        + "✓ AI Workout Recommendation\n"
                        + "✓ Daily Workout Reminder\n"
                        + "✓ BMI and Fitness Profile\n"
                        + "✓ PDF and CSV Export\n"
                        + "✓ Dark Mode\n\n"
                        + "Developed by\n"
                        + "Ashutosh Panwar\n\n"
                        + "Version 1.0\n"
                        + "© 2026 VitaFit";

        new MaterialAlertDialogBuilder(this)
                .setIcon(R.mipmap.ic_launcher_round)
                .setTitle("VitaFit")
                .setMessage(aboutMessage)
                .setPositiveButton("Close", null)
                .show();
    }

    private void showLogoutDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Logout from VitaFit?")
                .setMessage(
                        "Are you sure you want to sign out of your account?"
                )
                .setNegativeButton("Cancel", null)
                .setPositiveButton(
                        "Logout",
                        (dialog, which) -> logoutUser()
                )
                .show();
    }

    private void logoutUser() {
        FirebaseAuth.getInstance().signOut();

        Intent intent =
                new Intent(
                        MainActivity.this,
                        LoginActivity.class
                );

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();

        overridePendingTransition(
                R.anim.slide_in_left,
                R.anim.slide_out_right
        );
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        btnAddWorkout.setEnabled(true);
        btnHistory.setEnabled(true);
        btnStatistics.setEnabled(true);

        setupGreetingAndDate();
        loadNavigationHeader();

        if (navigationView != null) {
            navigationView.setCheckedItem(R.id.nav_dashboard);
        }

        if (bottomNavigationView != null
                && bottomNavigationView.getSelectedItemId()
                != R.id.nav_dashboard) {
            bottomNavigationView.setSelectedItemId(
                    R.id.nav_dashboard
            );
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (workoutsReference != null
                && dashboardListener != null) {
            workoutsReference.removeEventListener(
                    dashboardListener
            );
            dashboardListener = null;
        }
    }
}