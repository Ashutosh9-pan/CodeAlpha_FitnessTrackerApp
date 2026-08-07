package com.ashutosh.codealpha_fitnesstrackerapp;
import com.google.firebase.auth.FirebaseAuth;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import utils.ThemeManager;

public class StatisticsActivity extends AppCompatActivity {

    private static final String FIREBASE_DATABASE_URL =
            "https://fitnesstrackerapp-cb360-default-rtdb.asia-southeast1.firebasedatabase.app";

    private static final int CALORIE_GOAL = 500;
    private static final int MAX_CHART_ITEMS = 7;

    private TextView txtTotalWorkouts;
    private TextView txtTotalDuration;
    private TextView txtTotalCalories;
    private TextView txtGoal;
    private TextView txtWeekWorkouts;
    private TextView txtWeekCalories;

    private ProgressBar progressGoal;

    private BarChart barChartCalories;
    private LineChart lineChartDuration;

    private View bottomHome;
    private View bottomHistory;
    private View bottomAddWorkout;
    private View bottomStatistics;
    private View bottomProfile;

    private DatabaseReference workoutsReference;
    private ValueEventListener statisticsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applySavedTheme(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        initialiseViews();
        initialiseFirebase();
        configureCharts();
        applyChartTheme();
        setupBottomNavigation();

    }


    private void initialiseViews() {
        txtTotalWorkouts = findViewById(R.id.txtTotalWorkouts);
        txtTotalDuration = findViewById(R.id.txtTotalDuration);
        txtTotalCalories = findViewById(R.id.txtTotalCalories);
        txtGoal = findViewById(R.id.txtGoal);

        txtWeekWorkouts = findViewById(R.id.txtWeekWorkouts);
        txtWeekCalories = findViewById(R.id.txtWeekCalories);

        progressGoal = findViewById(R.id.progressGoal);

        barChartCalories = findViewById(R.id.barChartCalories);
        lineChartDuration = findViewById(R.id.lineChartDuration);

        bottomHome = findViewById(R.id.bottomHome);
        bottomHistory = findViewById(R.id.bottomHistory);
        bottomAddWorkout = findViewById(R.id.bottomAddWorkout);
        bottomStatistics = findViewById(R.id.bottomStatistics);
        bottomProfile = findViewById(R.id.bottomProfile);
    }

    private void setupBottomNavigation() {
        bottomHome.setOnClickListener(view -> openActivity(MainActivity.class));
        bottomHistory.setOnClickListener(view -> openActivity(HistoryActivity.class));
        bottomAddWorkout.setOnClickListener(view -> openActivity(AddWorkoutActivity.class));
        bottomStatistics.setOnClickListener(view -> { });
        bottomProfile.setOnClickListener(view -> openActivity(ProfileActivity.class));
    }

    private void openActivity(Class<?> destinationActivity) {
        Intent intent = new Intent(StatisticsActivity.this, destinationActivity);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
    }

    private void initialiseFirebase() {

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {

            Toast.makeText(
                    this,
                    "Please login again.",
                    Toast.LENGTH_SHORT
            ).show();

            finish();
            return;
        }

        String uid =
                FirebaseAuth.getInstance()
                        .getCurrentUser()
                        .getUid();

        FirebaseDatabase firebaseDatabase =
                FirebaseDatabase.getInstance(
                        FIREBASE_DATABASE_URL
                );

        workoutsReference =
                firebaseDatabase
                        .getReference("users")
                        .child(uid)
                        .child("workouts");
    }

    private void configureCharts() {
        barChartCalories.getDescription().setEnabled(false);
        barChartCalories.getLegend().setEnabled(true);
        barChartCalories.setDrawGridBackground(false);
        barChartCalories.setDrawBarShadow(false);
        barChartCalories.setFitBars(true);
        barChartCalories.setPinchZoom(false);
        barChartCalories.setScaleEnabled(false);
        barChartCalories.setNoDataText(
                "Add workouts to view calories chart"
        );

        barChartCalories.getAxisRight().setEnabled(false);
        barChartCalories.getAxisLeft().setAxisMinimum(0f);

        XAxis caloriesXAxis = barChartCalories.getXAxis();
        caloriesXAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        caloriesXAxis.setGranularity(1f);
        caloriesXAxis.setDrawGridLines(false);
        caloriesXAxis.setAvoidFirstLastClipping(true);

        lineChartDuration.getDescription().setEnabled(false);
        lineChartDuration.getLegend().setEnabled(true);
        lineChartDuration.setDrawGridBackground(false);
        lineChartDuration.setPinchZoom(false);
        lineChartDuration.setScaleEnabled(false);
        lineChartDuration.setNoDataText(
                "Add workouts to view duration chart"
        );

        lineChartDuration.getAxisRight().setEnabled(false);
        lineChartDuration.getAxisLeft().setAxisMinimum(0f);

        XAxis durationXAxis = lineChartDuration.getXAxis();
        durationXAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        durationXAxis.setGranularity(1f);
        durationXAxis.setDrawGridLines(false);
        durationXAxis.setAvoidFirstLastClipping(true);
    }

    private void applyChartTheme() {
        int textColor =
                ContextCompat.getColor(
                        this,
                        R.color.text_primary
                );

        int secondaryTextColor =
                ContextCompat.getColor(
                        this,
                        R.color.text_secondary
                );

        int gridColor =
                ContextCompat.getColor(
                        this,
                        R.color.divider
                );

        barChartCalories.setBackgroundColor(Color.TRANSPARENT);
        barChartCalories.setNoDataTextColor(secondaryTextColor);
        barChartCalories.getLegend().setTextColor(textColor);
        barChartCalories.getXAxis().setTextColor(textColor);
        barChartCalories.getXAxis().setAxisLineColor(gridColor);
        barChartCalories.getAxisLeft().setTextColor(textColor);
        barChartCalories.getAxisLeft().setGridColor(gridColor);
        barChartCalories.getAxisLeft().setAxisLineColor(gridColor);

        lineChartDuration.setBackgroundColor(Color.TRANSPARENT);
        lineChartDuration.setNoDataTextColor(secondaryTextColor);
        lineChartDuration.getLegend().setTextColor(textColor);
        lineChartDuration.getXAxis().setTextColor(textColor);
        lineChartDuration.getXAxis().setAxisLineColor(gridColor);
        lineChartDuration.getAxisLeft().setTextColor(textColor);
        lineChartDuration.getAxisLeft().setGridColor(gridColor);
        lineChartDuration.getAxisLeft().setAxisLineColor(gridColor);
    }

    @Override
    protected void onStart() {
        super.onStart();
        startStatisticsListener();
        if (workoutsReference == null
                || statisticsListener != null) {
            return;
        }
    }

    private void startStatisticsListener() {
        if (statisticsListener != null) {
            return;
        }

        statisticsListener =
                new ValueEventListener() {

                    @Override
                    public void onDataChange(
                            @NonNull DataSnapshot snapshot
                    ) {
                        int totalWorkouts = 0;
                        int totalDuration = 0;
                        int totalCalories = 0;

                        int weeklyWorkouts = 0;
                        int weeklyCalories = 0;

                        ArrayList<BarEntry> calorieEntries =
                                new ArrayList<>();

                        ArrayList<Entry> durationEntries =
                                new ArrayList<>();

                        ArrayList<String> workoutLabels =
                                new ArrayList<>();

                        int chartIndex = 0;

                        for (DataSnapshot workoutSnapshot
                                : snapshot.getChildren()) {

                            String workoutName =
                                    workoutSnapshot
                                            .child("workoutName")
                                            .getValue(String.class);

                            Long durationValue =
                                    workoutSnapshot
                                            .child("duration")
                                            .getValue(Long.class);

                            Long caloriesValue =
                                    workoutSnapshot
                                            .child("calories")
                                            .getValue(Long.class);

                            String workoutDate =
                                    workoutSnapshot
                                            .child("date")
                                            .getValue(String.class);

                            int duration =
                                    durationValue != null
                                            ? durationValue.intValue()
                                            : 0;

                            int calories =
                                    caloriesValue != null
                                            ? caloriesValue.intValue()
                                            : 0;

                            totalDuration += duration;
                            totalCalories += calories;
                            totalWorkouts++;

                            if (isWorkoutFromLastSevenDays(
                                    workoutDate
                            )) {
                                weeklyWorkouts++;
                                weeklyCalories += calories;
                            }

                            if (chartIndex < MAX_CHART_ITEMS) {
                                if (workoutName == null
                                        || workoutName
                                        .trim()
                                        .isEmpty()) {

                                    workoutName =
                                            "Workout "
                                                    + (chartIndex + 1);
                                }

                                workoutLabels.add(
                                        shortenLabel(workoutName)
                                );

                                calorieEntries.add(
                                        new BarEntry(
                                                chartIndex,
                                                calories
                                        )
                                );

                                durationEntries.add(
                                        new Entry(
                                                chartIndex,
                                                duration
                                        )
                                );

                                chartIndex++;
                            }
                        }

                        updateStatisticsViews(
                                totalWorkouts,
                                totalDuration,
                                totalCalories
                        );

                        updateWeeklyViews(
                                weeklyWorkouts,
                                weeklyCalories
                        );

                        if (chartIndex > 0) {
                            displayCaloriesChart(
                                    calorieEntries,
                                    workoutLabels
                            );

                            displayDurationChart(
                                    durationEntries,
                                    workoutLabels
                            );
                        } else {
                            clearCharts();
                        }
                    }

                    @Override
                    public void onCancelled(
                            @NonNull DatabaseError error
                    ) {
                        Toast.makeText(
                                StatisticsActivity.this,
                                "Unable to load statistics: "
                                        + error.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                };

        workoutsReference.addValueEventListener(
                statisticsListener
        );
    }

    private void displayCaloriesChart(
            ArrayList<BarEntry> entries,
            ArrayList<String> labels
    ) {
        int valueTextColor =
                ContextCompat.getColor(
                        this,
                        R.color.text_primary
                );

        BarDataSet dataSet =
                new BarDataSet(
                        entries,
                        "Calories Burned"
                );

        dataSet.setColor(
                Color.rgb(
                        255,
                        152,
                        0
                )
        );

        dataSet.setValueTextColor(valueTextColor);
        dataSet.setValueTextSize(11f);

        BarData barData =
                new BarData(dataSet);

        barData.setBarWidth(0.6f);

        barChartCalories
                .getXAxis()
                .setValueFormatter(
                        new IndexAxisValueFormatter(labels)
                );

        barChartCalories
                .getXAxis()
                .setLabelCount(
                        labels.size(),
                        false
                );

        barChartCalories.setData(barData);
        barChartCalories.setFitBars(true);
        barChartCalories.animateY(900);
        barChartCalories.invalidate();
    }

    private void displayDurationChart(
            ArrayList<Entry> entries,
            ArrayList<String> labels
    ) {
        int primaryColor =
                ContextCompat.getColor(
                        this,
                        R.color.primary
                );

        int valueTextColor =
                ContextCompat.getColor(
                        this,
                        R.color.text_primary
                );

        LineDataSet dataSet =
                new LineDataSet(
                        entries,
                        "Duration in Minutes"
                );

        dataSet.setColor(primaryColor);
        dataSet.setCircleColor(primaryColor);
        dataSet.setFillColor(primaryColor);
        dataSet.setLineWidth(3f);
        dataSet.setCircleRadius(5f);
        dataSet.setValueTextColor(valueTextColor);
        dataSet.setValueTextSize(11f);
        dataSet.setDrawFilled(true);
        dataSet.setFillAlpha(45);
        dataSet.setMode(
                LineDataSet.Mode.CUBIC_BEZIER
        );

        LineData lineData =
                new LineData(dataSet);

        lineChartDuration
                .getXAxis()
                .setValueFormatter(
                        new IndexAxisValueFormatter(labels)
                );

        lineChartDuration
                .getXAxis()
                .setLabelCount(
                        labels.size(),
                        false
                );

        lineChartDuration.setData(lineData);
        lineChartDuration.animateX(900);
        lineChartDuration.invalidate();
    }

    private void updateStatisticsViews(
            int totalWorkouts,
            int totalDuration,
            int totalCalories
    ) {
        txtTotalWorkouts.setText(
                String.valueOf(totalWorkouts)
        );

        txtTotalDuration.setText(
                totalDuration + " min"
        );

        txtTotalCalories.setText(
                totalCalories + " kcal"
        );

        int progress =
                (int) Math.min(
                        100L,
                        ((long) totalCalories * 100L)
                                / CALORIE_GOAL
                );

        progressGoal.setProgress(progress);
        txtGoal.setText(
                progress + "% Completed"
        );
    }

    private void updateWeeklyViews(
            int weeklyWorkouts,
            int weeklyCalories
    ) {
        txtWeekWorkouts.setText(
                String.valueOf(weeklyWorkouts)
        );

        txtWeekCalories.setText(
                weeklyCalories + " kcal"
        );
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

            Calendar today =
                    Calendar.getInstance();

            setCalendarToStartOfDay(today);

            Calendar sevenDaysAgo =
                    Calendar.getInstance();

            setCalendarToStartOfDay(
                    sevenDaysAgo
            );

            sevenDaysAgo.add(
                    Calendar.DAY_OF_YEAR,
                    -6
            );

            Calendar workoutCalendar =
                    Calendar.getInstance();

            workoutCalendar.setTime(
                    parsedWorkoutDate
            );

            setCalendarToStartOfDay(
                    workoutCalendar
            );

            return !workoutCalendar.before(
                    sevenDaysAgo
            ) && !workoutCalendar.after(today);

        } catch (ParseException exception) {
            return false;
        }
    }

    private void setCalendarToStartOfDay(
            Calendar calendar
    ) {
        calendar.set(
                Calendar.HOUR_OF_DAY,
                0
        );

        calendar.set(
                Calendar.MINUTE,
                0
        );

        calendar.set(
                Calendar.SECOND,
                0
        );

        calendar.set(
                Calendar.MILLISECOND,
                0
        );
    }

    private String shortenLabel(
            String label
    ) {
        String cleanedLabel =
                label == null
                        ? "Workout"
                        : label.trim();

        if (cleanedLabel.length() <= 10) {
            return cleanedLabel;
        }

        return cleanedLabel.substring(
                0,
                9
        ) + "…";
    }

    private void clearCharts() {
        barChartCalories.clear();
        barChartCalories.invalidate();

        lineChartDuration.clear();
        lineChartDuration.invalidate();
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (workoutsReference != null
                && statisticsListener != null) {

            workoutsReference.removeEventListener(
                    statisticsListener
            );

            statisticsListener = null;
        }
    }
}