package com.ashutosh.codealpha_fitnesstrackerapp;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.ashutosh.codealpha_fitnesstrackerapp.ai.GeminiManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import utils.ThemeManager;

public class AIProgressActivity extends AppCompatActivity {

    private static final String DATABASE_URL =
            "https://fitnesstrackerapp-cb360-default-rtdb.asia-southeast1.firebasedatabase.app";

    private TextView txtFitnessScore;
    private TextView txtFitnessLevel;
    private TextView txtProgressProfile;
    private TextView txtWorkoutSummary;
    private TextView txtProgressResult;

    private ProgressBar progressFitnessScore;
    private ProgressBar progressAnalysis;

    private MaterialButton btnAnalyzeProgress;
    private MaterialButton btnExportProgressPdf;

    private MaterialCardView cardProgressResult;

    private GeminiManager geminiManager;
    private DatabaseReference userReference;

    private boolean dataLoaded = false;
    private boolean analysisInProgress = false;

    private String progressContext =
            "Profile and workout data are not available.";

    private String currentProfileText = "";
    private String currentWorkoutText = "";
    private String currentAiAnalysis = "";

    private int currentFitnessScore = 0;
    private String currentFitnessLevel = "";

    private Uri lastGeneratedPdfUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applySavedTheme(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_progress);

        initialiseViews();

        geminiManager = new GeminiManager();

        btnAnalyzeProgress.setEnabled(false);
        btnExportProgressPdf.setEnabled(false);

        initialiseFirebase();

        btnAnalyzeProgress.setOnClickListener(
                view -> analyzeProgress()
        );

        btnExportProgressPdf.setOnClickListener(
                view -> exportProgressReportPdf()
        );

    }

    private void initialiseViews() {

        txtFitnessScore =
                findViewById(R.id.txtFitnessScore);

        txtFitnessLevel =
                findViewById(R.id.txtFitnessLevel);

        txtProgressProfile =
                findViewById(R.id.txtProgressProfile);

        txtWorkoutSummary =
                findViewById(R.id.txtWorkoutSummary);

        txtProgressResult =
                findViewById(R.id.txtProgressResult);

        progressFitnessScore =
                findViewById(R.id.progressFitnessScore);

        progressAnalysis =
                findViewById(R.id.progressAnalysis);

        btnAnalyzeProgress =
                findViewById(R.id.btnAnalyzeProgress);

        btnExportProgressPdf =
                findViewById(R.id.btnExportProgressPdf);


        cardProgressResult =
                findViewById(R.id.cardProgressResult);
    }

    private void initialiseFirebase() {

        FirebaseUser currentUser =
                FirebaseAuth
                        .getInstance()
                        .getCurrentUser();

        if (currentUser == null) {

            txtProgressProfile.setText(
                    "Please sign in to analyze your progress."
            );

            txtWorkoutSummary.setText(
                    "Workout data unavailable."
            );

            txtFitnessLevel.setText(
                    "Sign in required"
            );

            btnAnalyzeProgress.setEnabled(false);
            btnExportProgressPdf.setEnabled(false);

            return;
        }

        userReference =
                FirebaseDatabase
                        .getInstance(DATABASE_URL)
                        .getReference("users")
                        .child(currentUser.getUid());

        loadProgressData(currentUser);
    }

    private void loadProgressData(
            @NonNull FirebaseUser currentUser
    ) {

        txtProgressProfile.setText(
                "Loading profile..."
        );

        txtWorkoutSummary.setText(
                "Loading workout data..."
        );

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
                            name =
                                    getString(
                                            snapshot,
                                            "fullName"
                                    );
                        }

                        if (isBlank(name)) {
                            name =
                                    currentUser.getDisplayName();
                        }

                        if (isBlank(name)
                                && currentUser.getEmail() != null) {

                            String email =
                                    currentUser.getEmail();

                            int atIndex =
                                    email.indexOf("@");

                            name =
                                    atIndex > 0
                                            ? email.substring(
                                            0,
                                            atIndex
                                    )
                                            : email;
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

                        WorkoutStats stats =
                                calculateWorkoutStats(
                                        snapshot.child(
                                                "workouts"
                                        )
                                );

                        int fitnessScore =
                                calculateFitnessScore(
                                        stats.totalWorkouts,
                                        stats.totalDuration
                                );

                        String fitnessLevel =
                                getFitnessLevel(
                                        fitnessScore
                                );

                        currentFitnessScore =
                                fitnessScore;

                        currentFitnessLevel =
                                fitnessLevel;

                        txtFitnessScore.setText(
                                fitnessScore + " / 100"
                        );

                        progressFitnessScore.setProgress(
                                fitnessScore
                        );

                        txtFitnessLevel.setText(
                                fitnessLevel
                        );

                        String profileText =
                                "Name: "
                                        + name
                                        + "\nAge: "
                                        + formatLong(age)
                                        + "\nGoal: "
                                        + goal;

                        txtProgressProfile.setText(
                                profileText
                        );

                        String recentWorkout =
                                stats.recentWorkoutName;

                        if (!isBlank(
                                stats.recentWorkoutDate
                        )) {

                            recentWorkout +=
                                    " on "
                                            + stats.recentWorkoutDate;
                        }

                        String workoutText =
                                "Total workouts: "
                                        + stats.totalWorkouts
                                        + "\nTotal duration: "
                                        + stats.totalDuration
                                        + " minutes"
                                        + "\nAverage duration: "
                                        + calculateAverage(
                                        stats.totalDuration,
                                        stats.totalWorkouts
                                )
                                        + " minutes/workout"
                                        + "\nRecent workout: "
                                        + recentWorkout;

                        txtWorkoutSummary.setText(
                                workoutText
                        );

                        currentProfileText =
                                profileText;

                        currentWorkoutText =
                                workoutText;

                        progressContext =
                                profileText
                                        + "\nFitness score: "
                                        + fitnessScore
                                        + "/100"
                                        + "\nFitness level: "
                                        + fitnessLevel
                                        + "\n"
                                        + workoutText;

                        dataLoaded = true;

                        btnAnalyzeProgress.setEnabled(
                                true
                        );

                        btnExportProgressPdf.setEnabled(
                                true
                        );

                    }

                    @Override
                    public void onCancelled(
                            @NonNull DatabaseError error
                    ) {

                        dataLoaded = false;

                        txtProgressProfile.setText(
                                "Unable to load profile."
                        );

                        txtWorkoutSummary.setText(
                                "Unable to load workout statistics."
                        );

                        txtFitnessLevel.setText(
                                "Data loading failed"
                        );

                        btnAnalyzeProgress.setEnabled(
                                false
                        );

                        btnExportProgressPdf.setEnabled(
                                false
                        );


                        Toast.makeText(
                                AIProgressActivity.this,
                                "Unable to load progress data: "
                                        + error.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    private WorkoutStats calculateWorkoutStats(
            @NonNull DataSnapshot workoutsSnapshot
    ) {

        int totalWorkouts = 0;
        int totalDuration = 0;
        String recentWorkoutName =
                "No workout recorded";

        String recentWorkoutDate = "";

        long latestCreatedAt =
                Long.MIN_VALUE;

        for (DataSnapshot workoutSnapshot
                : workoutsSnapshot.getChildren()) {

            totalWorkouts++;

            int duration =
                    (int) getLong(
                            workoutSnapshot.child(
                                    "duration"
                            )
                    );

            totalDuration += duration;

            String workoutName =
                    getString(
                            workoutSnapshot,
                            "workoutName"
                    );

            if (isBlank(workoutName)) {
                workoutName =
                        getString(
                                workoutSnapshot,
                                "name"
                        );
            }

            String workoutDate =
                    getString(
                            workoutSnapshot,
                            "date"
                    );

            long createdAt =
                    getLong(
                            workoutSnapshot.child(
                                    "createdAt"
                            )
                    );

            /*
             * For older records without createdAt,
             * the latest iterated workout is used as fallback.
             */
            if (createdAt > latestCreatedAt
                    || latestCreatedAt
                    == Long.MIN_VALUE) {

                latestCreatedAt = createdAt;

                if (!isBlank(workoutName)) {
                    recentWorkoutName =
                            workoutName;
                }

                recentWorkoutDate =
                        workoutDate;
            }
        }

        return new WorkoutStats(
                totalWorkouts,
                totalDuration,
                recentWorkoutName,
                recentWorkoutDate
        );
    }

    private int calculateFitnessScore(
            int totalWorkouts,
            int totalDuration
    ) {

        int score = 0;

        /*
         * Recorded workout habit: maximum 60 points.
         */
        score += Math.min(
                60,
                totalWorkouts * 6
        );

        /*
         * Recorded active time: maximum 40 points.
         */
        score += Math.min(
                40,
                totalDuration / 25
        );

        return Math.min(
                100,
                Math.max(0, score)
        );
    }

    private String getFitnessLevel(
            int score
    ) {

        if (score >= 85) {
            return "Strong activity tracking ⭐⭐⭐⭐⭐";
        }

        if (score >= 70) {
            return "Steady activity habit ⭐⭐⭐⭐";
        }

        if (score >= 50) {
            return "Building a regular habit ⭐⭐⭐";
        }

        if (score >= 30) {
            return "Getting started consistently ⭐⭐";
        }

        return "Start tracking your workouts ⭐";
    }

    private void analyzeProgress() {

        if (!dataLoaded) {

            Toast.makeText(
                    this,
                    "Please wait while your data loads.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (analysisInProgress) {

            Toast.makeText(
                    this,
                    "Progress analysis is already running.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String analysisRequest =
                "Analyze the user's fitness progress using the "
                        + "profile and workout data supplied in the context.\n\n"

                        + "The response must include:\n"
                        + "• Overall progress summary\n"
                        + "• Activity score interpretation\n"
                        + "• Workout consistency analysis\n"
                        + "• Main strengths\n"
                        + "• Areas needing improvement\n"
                        + "• Recovery recommendations\n"
                        + "• Realistic goals for the next seven days\n"
                        + "• A short motivational conclusion\n\n"

                        + "Do not invent information that is not present. "
                        + "Do not diagnose medical conditions. "
                        + "Do not interpret weight, BMI, calories, body shape "
                        + "or appearance. Do not recommend weight loss, extreme "
                        + "training, maximum-load attempts or exercising through "
                        + "pain. Keep advice age-appropriate for a teen and include "
                        + "rest, recovery and easier alternatives. Explain that the "
                        + "activity score is only a workout-record summary, not a "
                        + "medical or fitness grade. "
                        + "Keep the response clear, practical, safe and "
                        + "easy to read on a mobile screen.";

        setLoadingState(true);

        geminiManager.generateResponse(
                analysisRequest,
                progressContext,
                new GeminiManager.GeminiCallback() {

                    @Override
                    public void onSuccess(
                            String response
                    ) {

                        runOnUiThread(
                                () -> {

                                    setLoadingState(false);

                                    currentAiAnalysis =
                                            response;

                                    txtProgressResult.setText(
                                            response
                                    );

                                    cardProgressResult.setVisibility(
                                            View.VISIBLE
                                    );

                                    btnExportProgressPdf.setEnabled(
                                            true
                                    );
                                }
                        );
                    }

                    @Override
                    public void onError(
                            String errorMessage
                    ) {

                        runOnUiThread(
                                () -> {

                                    setLoadingState(false);

                                    Toast.makeText(
                                            AIProgressActivity.this,
                                            errorMessage,
                                            Toast.LENGTH_LONG
                                    ).show();
                                }
                        );
                    }
                }
        );
    }

    private void setLoadingState(
            boolean loading
    ) {

        analysisInProgress = loading;

        btnAnalyzeProgress.setEnabled(
                !loading && dataLoaded
        );

        btnExportProgressPdf.setEnabled(
                !loading && dataLoaded
        );


        btnAnalyzeProgress.setText(
                loading
                        ? "Analyzing Progress..."
                        : "✨ Analyze My Progress"
        );

        progressAnalysis.setVisibility(
                loading
                        ? View.VISIBLE
                        : View.GONE
        );

        if (loading) {
            cardProgressResult.setVisibility(
                    View.GONE
            );
        }
    }

    private void exportProgressReportPdf() {

        if (!dataLoaded) {

            Toast.makeText(
                    this,
                    "Please wait while your progress data loads.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        btnExportProgressPdf.setEnabled(false);

        btnExportProgressPdf.setText(
                "Creating PDF..."
        );

        PdfDocument pdfDocument =
                new PdfDocument();

        try {

            PdfReportWriter writer =
                    new PdfReportWriter(
                            pdfDocument
                    );

            writer.startReport();

            writer.drawMainTitle(
                    "VitaFit AI Progress Report"
            );

            String reportDate =
                    new SimpleDateFormat(
                            "dd MMM yyyy, hh:mm a",
                            Locale.getDefault()
                    ).format(new Date());

            writer.drawSmallText(
                    "Generated: " + reportDate
            );

            writer.addVerticalSpace(14);

            writer.drawSectionHeading(
                    "Activity Score"
            );

            writer.drawLargeText(
                    currentFitnessScore
                            + " / 100"
            );

            writer.drawBodyText(
                    currentFitnessLevel
            );

            writer.addVerticalSpace(12);

            writer.drawSectionHeading(
                    "Profile Summary"
            );

            writer.drawBodyText(
                    currentProfileText
            );

            writer.addVerticalSpace(12);

            writer.drawSectionHeading(
                    "Workout Summary"
            );

            writer.drawBodyText(
                    currentWorkoutText
            );

            writer.addVerticalSpace(12);

            writer.drawSectionHeading(
                    "AI Progress Analysis"
            );

            String analysisText =
                    isBlank(currentAiAnalysis)
                            ? "AI analysis has not been generated yet. "
                              + "Open the AI Progress Analyzer and tap "
                              + "\"Analyze My Progress\" before exporting "
                              + "to include detailed AI recommendations."
                            : currentAiAnalysis;

            writer.drawBodyText(
                    analysisText
            );

            writer.addVerticalSpace(18);

            writer.drawSmallText(
                    "This report is based on saved VitaFit profile "
                            + "and workout data. AI guidance is not "
                            + "medical advice."
            );

            writer.finishReport();

            savePdfToDownloads(
                    pdfDocument
            );

            shareProgressReportPdf();

        } catch (Exception exception) {

            Toast.makeText(
                    this,
                    "Unable to create PDF: "
                            + getSafeErrorMessage(
                            exception
                    ),
                    Toast.LENGTH_LONG
            ).show();

        } finally {

            pdfDocument.close();

            btnExportProgressPdf.setEnabled(
                    dataLoaded
            );

            btnExportProgressPdf.setText(
                    "📤 Export & Share AI Report"
            );

        }
    }

    private void savePdfToDownloads(
            PdfDocument pdfDocument
    ) throws IOException {

        String timeStamp =
                new SimpleDateFormat(
                        "yyyyMMdd_HHmmss",
                        Locale.getDefault()
                ).format(new Date());

        String fileName =
                "VitaFit_AI_Progress_"
                        + timeStamp
                        + ".pdf";

        if (Build.VERSION.SDK_INT
                >= Build.VERSION_CODES.Q) {

            savePdfUsingMediaStore(
                    pdfDocument,
                    fileName
            );

        } else {

            savePdfForLegacyAndroid(
                    pdfDocument,
                    fileName
            );
        }
    }

    private void savePdfUsingMediaStore(
            PdfDocument pdfDocument,
            String fileName
    ) throws IOException {

        ContentValues values =
                new ContentValues();

        values.put(
                MediaStore.MediaColumns.DISPLAY_NAME,
                fileName
        );

        values.put(
                MediaStore.MediaColumns.MIME_TYPE,
                "application/pdf"
        );

        values.put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                Environment.DIRECTORY_DOWNLOADS
                        + "/VitaFit"
        );

        values.put(
                MediaStore.MediaColumns.IS_PENDING,
                1
        );

        Uri uri =
                getContentResolver().insert(
                        MediaStore.Downloads
                                .EXTERNAL_CONTENT_URI,
                        values
                );

        if (uri == null) {

            throw new IOException(
                    "Unable to create the PDF file."
            );
        }

        boolean savedSuccessfully = false;

        try (OutputStream outputStream =
                     getContentResolver()
                             .openOutputStream(uri)) {

            if (outputStream == null) {

                throw new IOException(
                        "Unable to open the PDF output stream."
                );
            }

            pdfDocument.writeTo(
                    outputStream
            );

            savedSuccessfully = true;

        } finally {

            if (savedSuccessfully) {

                ContentValues completedValues =
                        new ContentValues();

                completedValues.put(
                        MediaStore.MediaColumns.IS_PENDING,
                        0
                );

                getContentResolver().update(
                        uri,
                        completedValues,
                        null,
                        null
                );

                lastGeneratedPdfUri = uri;

            } else {

                getContentResolver().delete(
                        uri,
                        null,
                        null
                );
            }
        }

        Toast.makeText(
                this,
                "PDF saved in Downloads/VitaFit\n"
                        + fileName,
                Toast.LENGTH_LONG
        ).show();
    }

    private void savePdfForLegacyAndroid(
            PdfDocument pdfDocument,
            String fileName
    ) throws IOException {

        File downloadsFolder =
                Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS
                );

        File vitaFitFolder =
                new File(
                        downloadsFolder,
                        "VitaFit"
                );

        if (!vitaFitFolder.exists()
                && !vitaFitFolder.mkdirs()) {

            /*
             * Safe fallback if the public Downloads folder
             * cannot be created on an older device.
             */
            File externalFilesDirectory =
                    getExternalFilesDir(
                            Environment.DIRECTORY_DOCUMENTS
                    );

            if (externalFilesDirectory == null) {

                throw new IOException(
                        "Storage location is unavailable."
                );
            }

            vitaFitFolder =
                    new File(
                            externalFilesDirectory,
                            "VitaFit"
                    );

            if (!vitaFitFolder.exists()
                    && !vitaFitFolder.mkdirs()) {

                throw new IOException(
                        "Unable to create the VitaFit folder."
                );
            }
        }

        File pdfFile =
                new File(
                        vitaFitFolder,
                        fileName
                );

        try (OutputStream outputStream =
                     new FileOutputStream(
                             pdfFile
                     )) {

            pdfDocument.writeTo(
                    outputStream
            );
        }

        lastGeneratedPdfUri =
                FileProvider.getUriForFile(
                        this,
                        getPackageName() + ".fileprovider",
                        pdfFile
                );

        Toast.makeText(
                this,
                "PDF saved:\n"
                        + pdfFile.getAbsolutePath(),
                Toast.LENGTH_LONG
        ).show();
    }

    private void shareProgressReportPdf() {

        if (lastGeneratedPdfUri == null) {

            Toast.makeText(
                    this,
                    "Export the progress report first.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        Intent shareIntent =
                new Intent(Intent.ACTION_SEND);

        shareIntent.setType("application/pdf");

        shareIntent.putExtra(
                Intent.EXTRA_STREAM,
                lastGeneratedPdfUri
        );

        shareIntent.putExtra(
                Intent.EXTRA_SUBJECT,
                "VitaFit AI Progress Report"
        );

        shareIntent.putExtra(
                Intent.EXTRA_TEXT,
                "Here is my VitaFit AI fitness progress report."
        );

        shareIntent.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION
        );

        try {

            startActivity(
                    Intent.createChooser(
                            shareIntent,
                            "Share VitaFit Report"
                    )
            );

        } catch (Exception exception) {

            Toast.makeText(
                    this,
                    "Unable to share PDF: "
                            + getSafeErrorMessage(exception),
                    Toast.LENGTH_LONG
            ).show();
        }
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

        Object value =
                snapshot.getValue();

        if (value instanceof Number) {

            return ((Number) value)
                    .longValue();
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

    private int calculateAverage(
            int total,
            int count
    ) {

        if (count <= 0) {
            return 0;
        }

        return Math.round(
                (float) total / count
        );
    }

    private String formatLong(
            long value
    ) {

        return value > 0
                ? String.valueOf(value)
                : "Not available";
    }

    private boolean isBlank(
            String value
    ) {

        return value == null
                || value.trim().isEmpty();
    }

    private String getSafeErrorMessage(
            Exception exception
    ) {

        String message =
                exception.getMessage();

        if (message == null
                || message.trim().isEmpty()) {

            return "Unknown error";
        }

        return message;
    }

    private static class WorkoutStats {

        final int totalWorkouts;
        final int totalDuration;

        final String recentWorkoutName;
        final String recentWorkoutDate;

        WorkoutStats(
                int totalWorkouts,
                int totalDuration,
                String recentWorkoutName,
                String recentWorkoutDate
        ) {

            this.totalWorkouts =
                    totalWorkouts;

            this.totalDuration =
                    totalDuration;

            this.recentWorkoutName =
                    recentWorkoutName;

            this.recentWorkoutDate =
                    recentWorkoutDate;
        }
    }

    /*
     * Handles PDF text wrapping and automatic page creation.
     * This prevents a long Gemini analysis from being cut off.
     */
    private static class PdfReportWriter {

        private static final int PAGE_WIDTH = 595;
        private static final int PAGE_HEIGHT = 842;

        private static final int LEFT_MARGIN = 45;
        private static final int RIGHT_MARGIN = 45;
        private static final int TOP_MARGIN = 50;
        private static final int BOTTOM_MARGIN = 52;

        private final PdfDocument pdfDocument;
        private final Paint paint;

        private PdfDocument.Page currentPage;
        private Canvas canvas;

        private int currentPageNumber = 0;
        private int currentY = TOP_MARGIN;

        PdfReportWriter(
                PdfDocument pdfDocument
        ) {

            this.pdfDocument =
                    pdfDocument;

            paint =
                    new Paint(
                            Paint.ANTI_ALIAS_FLAG
                    );
        }

        void startReport() {
            startNewPage();
        }

        void finishReport() {

            if (currentPage != null) {

                drawPageNumber();

                pdfDocument.finishPage(
                        currentPage
                );

                currentPage = null;
            }
        }

        void drawMainTitle(
                String title
        ) {

            ensureSpace(40);

            paint.setTypeface(
                    Typeface.create(
                            Typeface.DEFAULT,
                            Typeface.BOLD
                    )
            );

            paint.setTextSize(24f);

            canvas.drawText(
                    title,
                    LEFT_MARGIN,
                    currentY,
                    paint
            );

            currentY += 34;
        }

        void drawSectionHeading(
                String heading
        ) {

            ensureSpace(32);

            paint.setTypeface(
                    Typeface.create(
                            Typeface.DEFAULT,
                            Typeface.BOLD
                    )
            );

            paint.setTextSize(16f);

            canvas.drawText(
                    heading,
                    LEFT_MARGIN,
                    currentY,
                    paint
            );

            currentY += 23;

            paint.setStrokeWidth(1f);

            canvas.drawLine(
                    LEFT_MARGIN,
                    currentY - 8,
                    PAGE_WIDTH - RIGHT_MARGIN,
                    currentY - 8,
                    paint
            );

            currentY += 5;
        }

        void drawLargeText(
                String text
        ) {

            ensureSpace(34);

            paint.setTypeface(
                    Typeface.create(
                            Typeface.DEFAULT,
                            Typeface.BOLD
                    )
            );

            paint.setTextSize(20f);

            canvas.drawText(
                    text,
                    LEFT_MARGIN,
                    currentY,
                    paint
            );

            currentY += 28;
        }

        void drawSmallText(
                String text
        ) {

            paint.setTypeface(
                    Typeface.DEFAULT
            );

            paint.setTextSize(10.5f);

            drawWrappedText(
                    text,
                    15
            );
        }

        void drawBodyText(
                String text
        ) {

            paint.setTypeface(
                    Typeface.DEFAULT
            );

            paint.setTextSize(12f);

            drawWrappedText(
                    text,
                    18
            );
        }

        void addVerticalSpace(
                int space
        ) {

            ensureSpace(space);
            currentY += space;
        }

        private void drawWrappedText(
                String text,
                int lineHeight
        ) {

            if (text == null
                    || text.trim().isEmpty()) {

                drawSingleLine(
                        "Not available",
                        lineHeight
                );

                return;
            }

            String cleanedText =
                    text.replace(
                            "\r\n",
                            "\n"
                    );

            String[] paragraphs =
                    cleanedText.split(
                            "\n",
                            -1
                    );

            int maximumWidth =
                    PAGE_WIDTH
                            - LEFT_MARGIN
                            - RIGHT_MARGIN;

            for (String paragraph
                    : paragraphs) {

                if (paragraph.trim().isEmpty()) {

                    ensureSpace(lineHeight);
                    currentY += lineHeight;

                    continue;
                }

                String[] words =
                        paragraph
                                .trim()
                                .split("\\s+");

                StringBuilder currentLine =
                        new StringBuilder();

                for (String word : words) {

                    String testLine =
                            currentLine.length() == 0
                                    ? word
                                    : currentLine
                                      + " "
                                      + word;

                    if (paint.measureText(testLine)
                            > maximumWidth) {

                        if (currentLine.length() > 0) {

                            drawSingleLine(
                                    currentLine.toString(),
                                    lineHeight
                            );
                        }

                        currentLine =
                                new StringBuilder(word);

                    } else {

                        if (currentLine.length() > 0) {
                            currentLine.append(" ");
                        }

                        currentLine.append(word);
                    }
                }

                if (currentLine.length() > 0) {

                    drawSingleLine(
                            currentLine.toString(),
                            lineHeight
                    );
                }
            }
        }

        private void drawSingleLine(
                String line,
                int lineHeight
        ) {

            ensureSpace(lineHeight + 3);

            canvas.drawText(
                    line,
                    LEFT_MARGIN,
                    currentY,
                    paint
            );

            currentY += lineHeight;
        }

        private void ensureSpace(
                int requiredSpace
        ) {

            int pageBottom =
                    PAGE_HEIGHT
                            - BOTTOM_MARGIN;

            if (currentY + requiredSpace
                    <= pageBottom) {

                return;
            }

            startNewPage();
        }

        private void startNewPage() {

            if (currentPage != null) {

                drawPageNumber();

                pdfDocument.finishPage(
                        currentPage
                );
            }

            currentPageNumber++;

            PdfDocument.PageInfo pageInfo =
                    new PdfDocument.PageInfo.Builder(
                            PAGE_WIDTH,
                            PAGE_HEIGHT,
                            currentPageNumber
                    ).create();

            currentPage =
                    pdfDocument.startPage(
                            pageInfo
                    );

            canvas =
                    currentPage.getCanvas();

            currentY =
                    TOP_MARGIN;
        }

        private void drawPageNumber() {

            paint.setTypeface(
                    Typeface.DEFAULT
            );

            paint.setTextSize(10f);

            String pageText =
                    "VitaFit | Page "
                            + currentPageNumber;

            float textWidth =
                    paint.measureText(
                            pageText
                    );

            canvas.drawText(
                    pageText,
                    PAGE_WIDTH
                            - RIGHT_MARGIN
                            - textWidth,
                    PAGE_HEIGHT - 25,
                    paint
            );
        }
    }
}