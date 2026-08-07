package com.ashutosh.codealpha_fitnesstrackerapp;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ashutosh.codealpha_fitnesstrackerapp.ai.GeminiManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import utils.ThemeManager;

public class AIChatActivity extends AppCompatActivity {

    private static final String DATABASE_URL =
            "https://fitnesstrackerapp-cb360-default-rtdb.asia-southeast1.firebasedatabase.app";

    private RecyclerView recyclerChat;
    private EditText etMessage;
    private ImageButton btnSend;

    private final List<ChatMessage> chatList = new ArrayList<>();

    private ChatAdapter chatAdapter;
    private GeminiManager geminiManager;

    private DatabaseReference userReference;

    private final Handler mainHandler =
            new Handler(Looper.getMainLooper());

    private boolean requestInProgress = false;
    private boolean contextLoaded = false;

    /*
     * Default context is used temporarily while Firebase data is loading.
     */
    private String userFitnessContext =
            "Name: Not available\n"
                    + "Age: Not available\n"
                    + "Height: Not available\n"
                    + "Weight: Not available\n"
                    + "BMI: Not available\n"
                    + "Fitness goal: Not available\n"
                    + "Total workouts: 0\n"
                    + "Total workout duration: 0 minutes\n"
                    + "Total calories burned: 0 kcal\n"
                    + "Recent workout: No workout recorded";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applySavedTheme(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_chat);

        initialiseViews();
        setupRecyclerView();
        setupGemini();
        setupListeners();
        initialiseFirebase();
        showWelcomeMessage();
    }

    private void initialiseViews() {
        recyclerChat = findViewById(R.id.recyclerChat);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
    }

    private void setupRecyclerView() {
        chatAdapter = new ChatAdapter(chatList);

        LinearLayoutManager layoutManager =
                new LinearLayoutManager(this);

        recyclerChat.setLayoutManager(layoutManager);
        recyclerChat.setAdapter(chatAdapter);
    }

    private void setupGemini() {
        geminiManager = new GeminiManager();
    }

    private void setupListeners() {
        btnSend.setOnClickListener(view -> sendMessage());

        etMessage.setOnEditorActionListener(
                (
                        TextView textView,
                        int actionId,
                        KeyEvent event
                ) -> {

                    if (actionId == EditorInfo.IME_ACTION_SEND) {
                        sendMessage();
                        return true;
                    }

                    return false;
                }
        );
    }

    private void initialiseFirebase() {

        FirebaseUser currentUser =
                FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            contextLoaded = true;

            Toast.makeText(
                    this,
                    "Profile personalization unavailable. Please sign in again.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        userReference =
                FirebaseDatabase
                        .getInstance(DATABASE_URL)
                        .getReference("users")
                        .child(currentUser.getUid());

        loadPersonalizedContext(currentUser);
    }

    private void loadPersonalizedContext(
            @NonNull FirebaseUser currentUser
    ) {

        userReference.addListenerForSingleValueEvent(
                new ValueEventListener() {

                    @Override
                    public void onDataChange(
                            @NonNull DataSnapshot userSnapshot
                    ) {

                        String name =
                                firstAvailableString(
                                        userSnapshot,
                                        "name",
                                        "fullName",
                                        "username"
                                );

                        if (isBlank(name)) {
                            name = currentUser.getDisplayName();
                        }

                        if (isBlank(name)
                                && currentUser.getEmail() != null) {

                            String email = currentUser.getEmail();
                            int atIndex = email.indexOf("@");

                            name = atIndex > 0
                                    ? email.substring(0, atIndex)
                                    : email;
                        }

                        if (isBlank(name)) {
                            name = "VitaFit User";
                        }

                        long age =
                                getLongValue(
                                        userSnapshot.child("age")
                                );

                        double height =
                                getDoubleValue(
                                        userSnapshot.child("height")
                                );

                        double weight =
                                getDoubleValue(
                                        userSnapshot.child("weight")
                                );

                        String goal =
                                userSnapshot
                                        .child("goal")
                                        .getValue(String.class);

                        if (isBlank(goal)) {
                            goal = "General Fitness";
                        }

                        double bmi = 0;

                        if (height > 0 && weight > 0) {
                            bmi =
                                    weight
                                            / Math.pow(
                                            height / 100.0,
                                            2
                                    );
                        }

                        WorkoutSummary workoutSummary =
                                calculateWorkoutSummary(
                                        userSnapshot.child("workouts")
                                );

                        userFitnessContext =
                                buildUserContext(
                                        name,
                                        age,
                                        height,
                                        weight,
                                        bmi,
                                        goal,
                                        workoutSummary
                                );

                        contextLoaded = true;
                    }

                    @Override
                    public void onCancelled(
                            @NonNull DatabaseError error
                    ) {

                        contextLoaded = true;

                        Toast.makeText(
                                AIChatActivity.this,
                                "AI profile context could not be loaded.",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }
        );
    }

    private WorkoutSummary calculateWorkoutSummary(
            @NonNull DataSnapshot workoutsSnapshot
    ) {

        int totalWorkouts = 0;
        int totalDuration = 0;
        int totalCalories = 0;

        String recentWorkoutName = "No workout recorded";
        String recentWorkoutDate = "";
        long newestCreatedAt = Long.MIN_VALUE;

        for (DataSnapshot workoutSnapshot
                : workoutsSnapshot.getChildren()) {

            totalWorkouts++;

            int duration =
                    (int) getLongValue(
                            workoutSnapshot.child("duration")
                    );

            int calories =
                    (int) getLongValue(
                            workoutSnapshot.child("calories")
                    );

            totalDuration += duration;
            totalCalories += calories;

            String workoutName =
                    workoutSnapshot
                            .child("workoutName")
                            .getValue(String.class);

            String workoutDate =
                    workoutSnapshot
                            .child("date")
                            .getValue(String.class);

            long createdAt =
                    getLongValue(
                            workoutSnapshot.child("createdAt")
                    );

            /*
             * Old records may not contain createdAt.
             * In that case, the latest iterated record becomes the fallback.
             */
            if (createdAt > newestCreatedAt
                    || newestCreatedAt == Long.MIN_VALUE) {

                newestCreatedAt = createdAt;

                if (!isBlank(workoutName)) {
                    recentWorkoutName = workoutName.trim();
                }

                recentWorkoutDate =
                        isBlank(workoutDate)
                                ? ""
                                : workoutDate.trim();
            }
        }

        return new WorkoutSummary(
                totalWorkouts,
                totalDuration,
                totalCalories,
                recentWorkoutName,
                recentWorkoutDate
        );
    }

    private String buildUserContext(
            String name,
            long age,
            double height,
            double weight,
            double bmi,
            String goal,
            WorkoutSummary summary
    ) {

        String ageText =
                age > 0
                        ? String.valueOf(age)
                        : "Not available";

        String heightText =
                height > 0
                        ? String.format(
                        Locale.getDefault(),
                        "%.1f cm",
                        height
                )
                        : "Not available";

        String weightText =
                weight > 0
                        ? String.format(
                        Locale.getDefault(),
                        "%.1f kg",
                        weight
                )
                        : "Not available";

        String bmiText =
                bmi > 0
                        ? String.format(
                        Locale.getDefault(),
                        "%.1f",
                        bmi
                )
                        : "Not available";

        String recentWorkout =
                summary.recentWorkoutName;

        if (!isBlank(summary.recentWorkoutDate)) {
            recentWorkout +=
                    " on "
                            + summary.recentWorkoutDate;
        }

        return "Name: " + name
                + "\nAge: " + ageText
                + "\nHeight: " + heightText
                + "\nWeight: " + weightText
                + "\nBMI: " + bmiText
                + "\nFitness goal: " + goal
                + "\nTotal workouts: " + summary.totalWorkouts
                + "\nTotal workout duration: "
                + summary.totalDuration
                + " minutes"
                + "\nTotal calories burned: "
                + summary.totalCalories
                + " kcal"
                + "\nRecent workout: "
                + recentWorkout;
    }

    private String firstAvailableString(
            DataSnapshot snapshot,
            String... fieldNames
    ) {

        for (String fieldName : fieldNames) {

            String value =
                    snapshot
                            .child(fieldName)
                            .getValue(String.class);

            if (!isBlank(value)) {
                return value.trim();
            }
        }

        return null;
    }

    private long getLongValue(
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

    private double getDoubleValue(
            DataSnapshot snapshot
    ) {

        Object value = snapshot.getValue();

        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }

        if (value instanceof String) {
            try {
                return Double.parseDouble(
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

    private void showWelcomeMessage() {
        addAIMessage(
                "👋 Hello! I'm your VitaFit AI Fitness Coach.\n\n"
                        + "I can use your saved profile and workout "
                        + "progress to provide personalized guidance.\n\n"
                        + "Try asking:\n"
                        + "• Analyse my fitness progress\n"
                        + "• Create a workout for my goal\n"
                        + "• Suggest my next workout\n"
                        + "• Create a balanced diet plan\n"
                        + "• How can I improve my routine?"
        );
    }

    private void sendMessage() {

        if (requestInProgress) {
            Toast.makeText(
                    this,
                    "Please wait for the current response.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String userMessage =
                etMessage
                        .getText()
                        .toString()
                        .trim();

        if (userMessage.isEmpty()) {
            etMessage.setError("Enter a message");
            return;
        }

        addUserMessage(userMessage);
        etMessage.setText("");

        String localResponse =
                getRuleBasedResponse(userMessage);

        if (localResponse != null) {

            showTypingIndicator();
            setRequestState(true);

            mainHandler.postDelayed(
                    () -> {
                        removeTypingIndicator();
                        addAIMessage(localResponse);
                        setRequestState(false);
                    },
                    700
            );

            return;
        }

        requestGeminiResponse(userMessage);
    }

    private void requestGeminiResponse(
            @NonNull String userMessage
    ) {

        showTypingIndicator();
        setRequestState(true);

        if (!contextLoaded) {
            Toast.makeText(
                    this,
                    "Profile is still loading. AI will use available information.",
                    Toast.LENGTH_SHORT
            ).show();
        }

        geminiManager.generateResponse(
                userMessage,
                userFitnessContext,
                new GeminiManager.GeminiCallback() {

                    @Override
                    public void onSuccess(String response) {

                        runOnUiThread(
                                () -> {
                                    removeTypingIndicator();
                                    addAIMessage(response);
                                    setRequestState(false);
                                }
                        );
                    }

                    @Override
                    public void onError(String errorMessage) {

                        runOnUiThread(
                                () -> {
                                    removeTypingIndicator();

                                    addAIMessage(
                                            buildVisibleAiError(
                                                    errorMessage
                                            )
                                    );

                                    setRequestState(false);
                                }
                        );
                    }
                }
        );
    }

    private String buildVisibleAiError(
            String errorMessage
    ) {
        String details = errorMessage;

        if (isBlank(details)) {
            details = "No technical details were returned.";
        }

        details = details
                .replace('\n', ' ')
                .replace('\r', ' ')
                .trim();

        if (details.length() > 500) {
            details = details.substring(0, 500) + "...";
        }

        return "⚠️ AI service error\n\n"
                + details
                + "\n\nPlease take a screenshot of this message.";
    }

    private void setRequestState(boolean loading) {
        requestInProgress = loading;
        btnSend.setEnabled(!loading);
        btnSend.setAlpha(loading ? 0.55f : 1f);
    }

    private void addUserMessage(String text) {

        chatList.add(
                new ChatMessage(
                        text,
                        ChatMessage.TYPE_USER
                )
        );

        notifyAndScroll();
    }

    private void addAIMessage(String text) {

        chatList.add(
                new ChatMessage(
                        text,
                        ChatMessage.TYPE_AI
                )
        );

        notifyAndScroll();
    }

    private void showTypingIndicator() {

        chatList.add(
                new ChatMessage(
                        "AI is typing...",
                        ChatMessage.TYPE_AI
                )
        );

        notifyAndScroll();
    }

    private void removeTypingIndicator() {

        if (chatList.isEmpty()) {
            return;
        }

        int lastPosition = chatList.size() - 1;
        ChatMessage lastMessage =
                chatList.get(lastPosition);

        if (lastMessage.getMessageType()
                == ChatMessage.TYPE_AI
                && "AI is typing..."
                .equals(lastMessage.getMessage())) {

            chatList.remove(lastPosition);
            chatAdapter.notifyItemRemoved(lastPosition);
        }
    }

    private void notifyAndScroll() {

        int insertedPosition =
                chatList.size() - 1;

        chatAdapter.notifyItemInserted(
                insertedPosition
        );

        recyclerChat.post(
                () -> recyclerChat
                        .smoothScrollToPosition(
                                insertedPosition
                        )
        );
    }

    private String getRuleBasedResponse(
            String originalMessage
    ) {

        String message =
                originalMessage.toLowerCase(
                        Locale.getDefault()
                );

        if (message.equals("hello")
                || message.equals("hi")
                || message.equals("hey")) {

            return "👋 Hello!\n\n"
                    + "Ask me to analyse your saved profile, "
                    + "workout progress or fitness goal.";
        }

        /*
         * Only simple greetings remain offline.
         * Workout, diet and progress questions go to Gemini so that
         * saved profile information can be used.
         */
        return null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mainHandler.removeCallbacksAndMessages(null);
    }

    private static class WorkoutSummary {

        final int totalWorkouts;
        final int totalDuration;
        final int totalCalories;
        final String recentWorkoutName;
        final String recentWorkoutDate;

        WorkoutSummary(
                int totalWorkouts,
                int totalDuration,
                int totalCalories,
                String recentWorkoutName,
                String recentWorkoutDate
        ) {
            this.totalWorkouts = totalWorkouts;
            this.totalDuration = totalDuration;
            this.totalCalories = totalCalories;
            this.recentWorkoutName = recentWorkoutName;
            this.recentWorkoutDate = recentWorkoutDate;
        }
    }
}