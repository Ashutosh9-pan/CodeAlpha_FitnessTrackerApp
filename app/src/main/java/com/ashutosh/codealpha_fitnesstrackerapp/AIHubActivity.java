package com.ashutosh.codealpha_fitnesstrackerapp;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import utils.ThemeManager;

public class AIHubActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applySavedTheme(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_hub);

        MaterialButton btnRecommendation =
                findViewById(R.id.btnAiRecommendation);

        MaterialButton btnCoachChat =
                findViewById(R.id.btnAiCoachChat);

        MaterialButton btnDietPlanner =
                findViewById(R.id.btnAiDietPlanner);

        MaterialButton btnWorkoutPlanner =
                findViewById(R.id.btnAiWorkoutPlanner);

        MaterialButton btnProgressAnalyzer =
                findViewById(R.id.btnAiProgressAnalyzer);

        btnRecommendation.setOnClickListener(
                view -> openActivity(RecommendationActivity.class)
        );

        btnCoachChat.setOnClickListener(
                view -> openActivity(AIChatActivity.class)
        );

        btnDietPlanner.setOnClickListener(
                view -> openActivity(AIDietPlannerActivity.class)
        );

        btnWorkoutPlanner.setOnClickListener(
                view -> openActivity(AIWorkoutPlannerActivity.class)
        );

        btnProgressAnalyzer.setOnClickListener(
                view -> openActivity(AIProgressActivity.class)
        );
    }

    private void openActivity(Class<?> activityClass) {
        startActivity(new Intent(this, activityClass));
    }
}