package com.ashutosh.codealpha_fitnesstrackerapp;

import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import adapter.WorkoutAdapter;
import model.Workout;
import utils.ThemeManager;

public class HistoryActivity extends AppCompatActivity {

    private static final String FIREBASE_DATABASE_URL =
            "https://fitnesstrackerapp-cb360-default-rtdb.asia-southeast1.firebasedatabase.app";

    private static final int PDF_PAGE_WIDTH = 595;
    private static final int PDF_PAGE_HEIGHT = 842;

    private static final int PDF_LEFT_MARGIN = 35;
    private static final int PDF_RIGHT_MARGIN = 35;
    private static final int PDF_BOTTOM_MARGIN = 40;

    private static final int TABLE_ROW_HEIGHT = 34;

    private static final int MIN_DURATION_MINUTES = 1;
    private static final int MAX_DURATION_MINUTES = 600;
    private static final int MIN_CALORIES = 1;
    private static final int MAX_CALORIES = 5000;

    private RecyclerView recyclerWorkout;
    private ProgressBar progressLoading;
    private View layoutEmptyState;

    private Button btnAddFirstWorkout;
    private MaterialButton btnExportPdf;
    private MaterialButton btnExportCsv;

    private TextInputEditText editSearchWorkout;
    private TextView txtNoSearchResults;
    private BottomNavigationView bottomNavigationView;

    private WorkoutAdapter adapter;
    private ArrayList<Workout> workoutList;

    private DatabaseReference workoutsReference;
    private ValueEventListener workoutsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applySavedTheme(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        initializeViews();
        applyBottomNavigationInsets();
        setupBottomNavigation();
        setupRecyclerView();
        enableSwipeToDelete();
        setupFirebase();
        setupClickListeners();
        setupWorkoutSearch();
    }

    private void initializeViews() {
        recyclerWorkout =
                findViewById(R.id.recyclerWorkout);

        progressLoading =
                findViewById(R.id.progressLoading);

        layoutEmptyState =
                findViewById(R.id.layoutEmptyState);

        btnAddFirstWorkout =
                findViewById(R.id.btnAddFirstWorkout);

        btnExportPdf =
                findViewById(R.id.btnExportPdf);

        btnExportCsv =
                findViewById(R.id.btnExportCsv);

        editSearchWorkout =
                findViewById(R.id.editSearchWorkout);

        txtNoSearchResults =
                findViewById(R.id.txtNoSearchResults);

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

            if (itemId == R.id.nav_history) {
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

            if (itemId == R.id.nav_profile) {
                openBottomNavigationDestination(
                        ProfileActivity.class
                );
                return true;
            }

            return false;
        });

        bottomNavigationView.setSelectedItemId(
                R.id.nav_history
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

    private void setupRecyclerView() {
        recyclerWorkout.setLayoutManager(
                new LinearLayoutManager(this)
        );

        recyclerWorkout.setHasFixedSize(true);

        workoutList = new ArrayList<>();

        adapter = new WorkoutAdapter(
                this,
                workoutList,
                this::deleteWorkoutFromFirebase,
                this::showEditWorkoutDialog
        );

        recyclerWorkout.setAdapter(adapter);
    }

    private void enableSwipeToDelete() {
        ItemTouchHelper.SimpleCallback callback =
                new ItemTouchHelper.SimpleCallback(
                        0,
                        ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT
                ) {
                    @Override
                    public boolean onMove(
                            @NonNull RecyclerView recyclerView,
                            @NonNull RecyclerView.ViewHolder viewHolder,
                            @NonNull RecyclerView.ViewHolder target
                    ) {
                        return false;
                    }

                    @Override
                    public void onSwiped(
                            @NonNull RecyclerView.ViewHolder viewHolder,
                            int direction
                    ) {
                        int position =
                                viewHolder.getBindingAdapterPosition();

                        if (position == RecyclerView.NO_POSITION) {
                            return;
                        }

                        Workout deletedWorkout =
                                adapter.getWorkoutAt(position);

                        if (deletedWorkout == null) {
                            adapter.notifyItemChanged(position);
                            return;
                        }

                        String firebaseId =
                                deletedWorkout.getFirebaseId();

                        if (firebaseId == null
                                || firebaseId.trim().isEmpty()) {

                            adapter.notifyItemChanged(position);

                            Toast.makeText(
                                    HistoryActivity.this,
                                    "Firebase workout ID is missing",
                                    Toast.LENGTH_SHORT
                            ).show();

                            return;
                        }

                        workoutsReference
                                .child(firebaseId)
                                .removeValue()
                                .addOnSuccessListener(unused ->
                                        Snackbar.make(
                                                recyclerWorkout,
                                                "Workout deleted",
                                                Snackbar.LENGTH_LONG
                                        ).setAction(
                                                "UNDO",
                                                view -> restoreWorkoutInFirebase(
                                                        deletedWorkout
                                                )
                                        ).show()
                                )
                                .addOnFailureListener(exception -> {
                                    adapter.notifyItemChanged(position);

                                    Toast.makeText(
                                            HistoryActivity.this,
                                            "Delete failed: "
                                                    + exception.getMessage(),
                                            Toast.LENGTH_LONG
                                    ).show();
                                });
                    }
                };

        new ItemTouchHelper(callback)
                .attachToRecyclerView(recyclerWorkout);
    }

    private void restoreWorkoutInFirebase(
            Workout workout
    ) {
        if (workout == null
                || workout.getFirebaseId() == null
                || workout.getFirebaseId().trim().isEmpty()) {

            Toast.makeText(
                    this,
                    "Unable to restore workout",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        Map<String, Object> workoutData =
                new HashMap<>();

        workoutData.put(
                "firebaseId",
                workout.getFirebaseId()
        );
        workoutData.put(
                "workoutName",
                workout.getName()
        );
        workoutData.put(
                "duration",
                workout.getDuration()
        );
        workoutData.put(
                "calories",
                workout.getCalories()
        );
        workoutData.put(
                "date",
                workout.getDate()
        );
        workoutData.put(
                "restoredAt",
                System.currentTimeMillis()
        );

        workoutsReference
                .child(workout.getFirebaseId())
                .setValue(workoutData)
                .addOnSuccessListener(unused ->
                        Toast.makeText(
                                HistoryActivity.this,
                                "Workout restored",
                                Toast.LENGTH_SHORT
                        ).show()
                )
                .addOnFailureListener(exception ->
                        Toast.makeText(
                                HistoryActivity.this,
                                "Restore failed: "
                                        + exception.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    private void setupFirebase() {
        FirebaseDatabase firebaseDatabase =
                FirebaseDatabase.getInstance(
                        FIREBASE_DATABASE_URL
                );

        workoutsReference =
                firebaseDatabase.getReference(
                        "workouts"
                );
    }

    private void setupClickListeners() {
        btnAddFirstWorkout.setOnClickListener(v ->
                openAddWorkoutScreen()
        );

        btnExportPdf.setOnClickListener(v -> {

            if (workoutList.isEmpty()) {
                Toast.makeText(
                        HistoryActivity.this,
                        "Add at least one workout before exporting",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            generateWorkoutPdf();
        });

        btnExportCsv.setOnClickListener(v -> {

            if (workoutList.isEmpty()) {
                Toast.makeText(
                        HistoryActivity.this,
                        "Add at least one workout before exporting",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            exportAndShareCsv();
        });
    }

    private void setupWorkoutSearch() {
        editSearchWorkout.addTextChangedListener(
                new TextWatcher() {

                    @Override
                    public void beforeTextChanged(
                            CharSequence text,
                            int start,
                            int count,
                            int after
                    ) {
                        // No action required
                    }

                    @Override
                    public void onTextChanged(
                            CharSequence text,
                            int start,
                            int before,
                            int count
                    ) {
                        String searchText =
                                text == null
                                        ? ""
                                        : text.toString();

                        adapter.filterWorkouts(
                                searchText
                        );

                        updateSearchResultVisibility(
                                searchText
                        );
                    }

                    @Override
                    public void afterTextChanged(
                            Editable editable
                    ) {
                        // No action required
                    }
                }
        );
    }

    private void updateSearchResultVisibility(
            String searchText
    ) {
        boolean searchIsActive =
                searchText != null
                        && !searchText
                        .trim()
                        .isEmpty();

        boolean noMatchingResult =
                searchIsActive
                        && adapter
                        .getFilteredItemCount() == 0
                        && !workoutList.isEmpty();

        if (noMatchingResult) {
            txtNoSearchResults.setVisibility(
                    View.VISIBLE
            );

            recyclerWorkout.setVisibility(
                    View.GONE
            );

        } else {
            txtNoSearchResults.setVisibility(
                    View.GONE
            );

            if (!workoutList.isEmpty()) {
                recyclerWorkout.setVisibility(
                        View.VISIBLE
                );
            }
        }
    }

    private void openAddWorkoutScreen() {
        Intent intent = new Intent(
                HistoryActivity.this,
                AddWorkoutActivity.class
        );

        startActivity(intent);
    }

    @Override
    protected void onStart() {
        super.onStart();
        loadWorkoutsFromFirebase();
    }

    private void loadWorkoutsFromFirebase() {

        if (workoutsListener != null) {
            return;
        }

        showLoadingState();

        workoutsListener =
                new ValueEventListener() {

                    @Override
                    public void onDataChange(
                            @NonNull DataSnapshot snapshot
                    ) {
                        workoutList.clear();

                        for (DataSnapshot workoutSnapshot
                                : snapshot.getChildren()) {

                            String firebaseId =
                                    workoutSnapshot
                                            .child("firebaseId")
                                            .getValue(String.class);

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

                            String date =
                                    workoutSnapshot
                                            .child("date")
                                            .getValue(String.class);

                            if (firebaseId == null
                                    || firebaseId
                                    .trim()
                                    .isEmpty()) {

                                firebaseId =
                                        workoutSnapshot
                                                .getKey();
                            }

                            if (workoutName == null
                                    || workoutName
                                    .trim()
                                    .isEmpty()) {

                                workoutName =
                                        "Unknown Workout";
                            }

                            int duration = 0;

                            if (durationValue != null) {
                                duration =
                                        durationValue
                                                .intValue();
                            }

                            int calories = 0;

                            if (caloriesValue != null) {
                                calories =
                                        caloriesValue
                                                .intValue();
                            }

                            if (date == null
                                    || date
                                    .trim()
                                    .isEmpty()) {

                                date = "No date";
                            }

                            Workout workout =
                                    new Workout(
                                            firebaseId,
                                            workoutName,
                                            duration,
                                            calories,
                                            date
                                    );

                            workoutList.add(
                                    workout
                            );
                        }

                        adapter.setWorkouts(
                                workoutList
                        );

                        String currentSearchText =
                                editSearchWorkout
                                        .getText() == null
                                        ? ""
                                        : editSearchWorkout
                                        .getText()
                                        .toString();

                        adapter.filterWorkouts(
                                currentSearchText
                        );

                        if (workoutList.isEmpty()) {
                            showEmptyState();
                        } else {
                            showWorkoutList();

                            updateSearchResultVisibility(
                                    currentSearchText
                            );
                        }
                    }

                    @Override
                    public void onCancelled(
                            @NonNull DatabaseError error
                    ) {
                        progressLoading.setVisibility(
                                View.GONE
                        );

                        btnExportPdf.setEnabled(
                                false
                        );

                        btnExportCsv.setEnabled(
                                false
                        );

                        txtNoSearchResults.setVisibility(
                                View.GONE
                        );

                        Toast.makeText(
                                HistoryActivity.this,
                                "Unable to load workouts: "
                                        + error.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();

                        if (workoutList.isEmpty()) {
                            showEmptyState();
                        }
                    }
                };

        workoutsReference.addValueEventListener(
                workoutsListener
        );
    }

    private void showLoadingState() {
        progressLoading.setVisibility(
                View.VISIBLE
        );

        recyclerWorkout.setVisibility(
                View.GONE
        );

        layoutEmptyState.setVisibility(
                View.GONE
        );

        txtNoSearchResults.setVisibility(
                View.GONE
        );

        btnExportPdf.setEnabled(
                false
        );

        btnExportCsv.setEnabled(
                false
        );
    }

    private void showEmptyState() {
        progressLoading.setVisibility(
                View.GONE
        );

        recyclerWorkout.setVisibility(
                View.GONE
        );

        layoutEmptyState.setVisibility(
                View.VISIBLE
        );

        txtNoSearchResults.setVisibility(
                View.GONE
        );

        btnExportPdf.setEnabled(
                false
        );

        btnExportCsv.setEnabled(
                false
        );
    }

    private void showWorkoutList() {
        progressLoading.setVisibility(
                View.GONE
        );

        recyclerWorkout.setVisibility(
                View.VISIBLE
        );

        layoutEmptyState.setVisibility(
                View.GONE
        );

        btnExportPdf.setEnabled(
                true
        );

        btnExportCsv.setEnabled(
                true
        );
    }

    private void generateWorkoutPdf() {
        btnExportPdf.setEnabled(false);
        btnExportPdf.setText("Creating PDF...");

        PdfDocument pdfDocument =
                new PdfDocument();

        try {
            Paint paint =
                    new Paint(
                            Paint.ANTI_ALIAS_FLAG
                    );

            int pageNumber = 1;
            int currentWorkoutIndex = 0;

            while (currentWorkoutIndex
                    < workoutList.size()) {

                PdfDocument.PageInfo pageInfo =
                        new PdfDocument
                                .PageInfo
                                .Builder(
                                PDF_PAGE_WIDTH,
                                PDF_PAGE_HEIGHT,
                                pageNumber
                        ).create();

                PdfDocument.Page page =
                        pdfDocument.startPage(
                                pageInfo
                        );

                Canvas canvas =
                        page.getCanvas();

                int currentY =
                        drawPdfHeader(
                                canvas,
                                paint,
                                pageNumber
                        );

                currentY =
                        drawTableHeader(
                                canvas,
                                paint,
                                currentY
                        );

                while (currentWorkoutIndex
                        < workoutList.size()) {

                    if (currentY
                            + TABLE_ROW_HEIGHT
                            > PDF_PAGE_HEIGHT
                            - PDF_BOTTOM_MARGIN
                            - 80) {

                        break;
                    }

                    Workout workout =
                            workoutList.get(
                                    currentWorkoutIndex
                            );

                    drawWorkoutRow(
                            canvas,
                            paint,
                            workout,
                            currentWorkoutIndex + 1,
                            currentY
                    );

                    currentY +=
                            TABLE_ROW_HEIGHT;

                    currentWorkoutIndex++;
                }

                if (currentWorkoutIndex
                        >= workoutList.size()) {

                    drawPdfSummary(
                            canvas,
                            paint,
                            currentY + 20
                    );
                }

                drawPdfFooter(
                        canvas,
                        paint
                );

                pdfDocument.finishPage(
                        page
                );

                pageNumber++;
            }

            File pdfDirectory =
                    new File(
                            getCacheDir(),
                            "reports"
                    );

            if (!pdfDirectory.exists()
                    && !pdfDirectory.mkdirs()) {

                throw new IOException(
                        "Unable to create report folder"
                );
            }

            String timestamp =
                    new SimpleDateFormat(
                            "yyyyMMdd_HHmmss",
                            Locale.getDefault()
                    ).format(new Date());

            File pdfFile =
                    new File(
                            pdfDirectory,
                            "VitaFit_Workout_Report_"
                                    + timestamp
                                    + ".pdf"
                    );

            try (FileOutputStream outputStream =
                         new FileOutputStream(
                                 pdfFile
                         )) {

                pdfDocument.writeTo(
                        outputStream
                );
            }

            Toast.makeText(
                    this,
                    "PDF created successfully",
                    Toast.LENGTH_SHORT
            ).show();

            sharePdfFile(pdfFile);

        } catch (IOException exception) {

            Toast.makeText(
                    this,
                    "Unable to create PDF: "
                            + exception.getMessage(),
                    Toast.LENGTH_LONG
            ).show();

        } catch (Exception exception) {

            Toast.makeText(
                    this,
                    "Unexpected error: "
                            + exception.getMessage(),
                    Toast.LENGTH_LONG
            ).show();

        } finally {
            pdfDocument.close();

            btnExportPdf.setEnabled(
                    !workoutList.isEmpty()
            );

            btnExportPdf.setText(
                    "PDF"
            );
        }
    }

    private int drawPdfHeader(
            Canvas canvas,
            Paint paint,
            int pageNumber
    ) {
        paint.setColor(
                Color.rgb(
                        109,
                        79,
                        194
                )
        );

        paint.setStyle(
                Paint.Style.FILL
        );

        canvas.drawRect(
                0,
                0,
                PDF_PAGE_WIDTH,
                125,
                paint
        );

        paint.setColor(
                Color.WHITE
        );

        paint.setTextSize(
                30
        );

        paint.setTypeface(
                Typeface.create(
                        Typeface.DEFAULT,
                        Typeface.BOLD
                )
        );

        canvas.drawText(
                "VitaFit",
                PDF_LEFT_MARGIN,
                55,
                paint
        );

        paint.setTextSize(
                17
        );

        paint.setTypeface(
                Typeface.DEFAULT
        );

        canvas.drawText(
                "Workout History Report",
                PDF_LEFT_MARGIN,
                85,
                paint
        );

        paint.setTextSize(
                10
        );

        String generatedDate =
                new SimpleDateFormat(
                        "dd MMM yyyy, hh:mm a",
                        Locale.getDefault()
                ).format(new Date());

        canvas.drawText(
                "Generated: "
                        + generatedDate,
                PDF_LEFT_MARGIN,
                108,
                paint
        );

        String pageText =
                "Page " + pageNumber;

        float pageTextWidth =
                paint.measureText(
                        pageText
                );

        canvas.drawText(
                pageText,
                PDF_PAGE_WIDTH
                        - PDF_RIGHT_MARGIN
                        - pageTextWidth,
                108,
                paint
        );

        return 150;
    }

    private int drawTableHeader(
            Canvas canvas,
            Paint paint,
            int startY
    ) {
        paint.setColor(
                Color.rgb(
                        236,
                        231,
                        250
                )
        );

        paint.setStyle(
                Paint.Style.FILL
        );

        canvas.drawRoundRect(
                PDF_LEFT_MARGIN,
                startY,
                PDF_PAGE_WIDTH
                        - PDF_RIGHT_MARGIN,
                startY
                        + TABLE_ROW_HEIGHT,
                6,
                6,
                paint
        );

        paint.setColor(
                Color.rgb(
                        46,
                        36,
                        77
                )
        );

        paint.setTextSize(
                10
        );

        paint.setTypeface(
                Typeface.create(
                        Typeface.DEFAULT,
                        Typeface.BOLD
                )
        );

        int textY =
                startY + 21;

        canvas.drawText(
                "No.",
                PDF_LEFT_MARGIN + 8,
                textY,
                paint
        );

        canvas.drawText(
                "Workout",
                PDF_LEFT_MARGIN + 42,
                textY,
                paint
        );

        canvas.drawText(
                "Date",
                PDF_LEFT_MARGIN + 218,
                textY,
                paint
        );

        canvas.drawText(
                "Duration",
                PDF_LEFT_MARGIN + 350,
                textY,
                paint
        );

        canvas.drawText(
                "Calories",
                PDF_LEFT_MARGIN + 445,
                textY,
                paint
        );

        paint.setTypeface(
                Typeface.DEFAULT
        );

        return startY
                + TABLE_ROW_HEIGHT;
    }

    private void drawWorkoutRow(
            Canvas canvas,
            Paint paint,
            Workout workout,
            int serialNumber,
            int startY
    ) {
        if (serialNumber % 2 == 0) {

            paint.setColor(
                    Color.rgb(
                            248,
                            247,
                            252
                    )
            );

            paint.setStyle(
                    Paint.Style.FILL
            );

            canvas.drawRect(
                    PDF_LEFT_MARGIN,
                    startY,
                    PDF_PAGE_WIDTH
                            - PDF_RIGHT_MARGIN,
                    startY
                            + TABLE_ROW_HEIGHT,
                    paint
            );
        }

        paint.setColor(
                Color.rgb(
                        55,
                        65,
                        81
                )
        );

        paint.setTextSize(
                9
        );

        paint.setTypeface(
                Typeface.DEFAULT
        );

        int textY =
                startY + 21;

        canvas.drawText(
                String.valueOf(
                        serialNumber
                ),
                PDF_LEFT_MARGIN + 8,
                textY,
                paint
        );

        canvas.drawText(
                shortenText(
                        workout.getName(),
                        24
                ),
                PDF_LEFT_MARGIN + 42,
                textY,
                paint
        );

        canvas.drawText(
                shortenText(
                        workout.getDate(),
                        17
                ),
                PDF_LEFT_MARGIN + 218,
                textY,
                paint
        );

        canvas.drawText(
                workout.getDuration()
                        + " min",
                PDF_LEFT_MARGIN + 350,
                textY,
                paint
        );

        canvas.drawText(
                workout.getCalories()
                        + " kcal",
                PDF_LEFT_MARGIN + 445,
                textY,
                paint
        );

        paint.setColor(
                Color.rgb(
                        225,
                        225,
                        235
                )
        );

        paint.setStrokeWidth(
                1
        );

        canvas.drawLine(
                PDF_LEFT_MARGIN,
                startY
                        + TABLE_ROW_HEIGHT,
                PDF_PAGE_WIDTH
                        - PDF_RIGHT_MARGIN,
                startY
                        + TABLE_ROW_HEIGHT,
                paint
        );
    }

    private void drawPdfSummary(
            Canvas canvas,
            Paint paint,
            int startY
    ) {
        int totalDuration = 0;
        int totalCalories = 0;

        for (Workout workout
                : workoutList) {

            totalDuration +=
                    workout.getDuration();

            totalCalories +=
                    workout.getCalories();
        }

        if (startY + 100
                > PDF_PAGE_HEIGHT
                - PDF_BOTTOM_MARGIN) {

            startY =
                    PDF_PAGE_HEIGHT
                            - PDF_BOTTOM_MARGIN
                            - 100;
        }

        paint.setColor(
                Color.rgb(
                        245,
                        242,
                        252
                )
        );

        paint.setStyle(
                Paint.Style.FILL
        );

        canvas.drawRoundRect(
                PDF_LEFT_MARGIN,
                startY,
                PDF_PAGE_WIDTH
                        - PDF_RIGHT_MARGIN,
                startY + 85,
                12,
                12,
                paint
        );

        paint.setColor(
                Color.rgb(
                        46,
                        36,
                        77
                )
        );

        paint.setTextSize(
                14
        );

        paint.setTypeface(
                Typeface.create(
                        Typeface.DEFAULT,
                        Typeface.BOLD
                )
        );

        canvas.drawText(
                "Workout Summary",
                PDF_LEFT_MARGIN + 15,
                startY + 24,
                paint
        );

        paint.setTextSize(
                11
        );

        paint.setTypeface(
                Typeface.DEFAULT
        );

        canvas.drawText(
                "Total Workouts: "
                        + workoutList.size(),
                PDF_LEFT_MARGIN + 15,
                startY + 52,
                paint
        );

        canvas.drawText(
                "Total Duration: "
                        + totalDuration
                        + " minutes",
                PDF_LEFT_MARGIN + 175,
                startY + 52,
                paint
        );

        canvas.drawText(
                "Total Calories: "
                        + totalCalories
                        + " kcal",
                PDF_LEFT_MARGIN + 370,
                startY + 52,
                paint
        );
    }

    private void drawPdfFooter(
            Canvas canvas,
            Paint paint
    ) {
        paint.setColor(
                Color.rgb(
                        107,
                        114,
                        128
                )
        );

        paint.setTextSize(
                9
        );

        paint.setTypeface(
                Typeface.DEFAULT
        );

        String footerText =
                "Generated by VitaFit • Stay active, stay healthy";

        float footerWidth =
                paint.measureText(
                        footerText
                );

        canvas.drawText(
                footerText,
                (PDF_PAGE_WIDTH
                        - footerWidth) / 2,
                PDF_PAGE_HEIGHT - 20,
                paint
        );
    }

    private String shortenText(
            String text,
            int maximumLength
    ) {
        if (text == null
                || text.trim().isEmpty()) {

            return "-";
        }

        String cleanedText =
                text.trim();

        if (cleanedText.length()
                <= maximumLength) {

            return cleanedText;
        }

        return cleanedText.substring(
                0,
                maximumLength - 3
        ) + "...";
    }

    private void exportAndShareCsv() {
        btnExportCsv.setEnabled(false);
        btnExportCsv.setText("Creating...");

        try {
            File documentsDirectory =
                    getExternalFilesDir(
                            Environment.DIRECTORY_DOCUMENTS
                    );

            File csvDirectory;

            if (documentsDirectory != null) {
                csvDirectory =
                        new File(
                                documentsDirectory,
                                "VitaFit"
                        );
            } else {
                csvDirectory =
                        new File(
                                getCacheDir(),
                                "exports"
                        );
            }

            if (!csvDirectory.exists()
                    && !csvDirectory.mkdirs()) {
                throw new IOException(
                        "Unable to create export folder"
                );
            }

            String timestamp =
                    new SimpleDateFormat(
                            "yyyyMMdd_HHmmss",
                            Locale.getDefault()
                    ).format(new Date());

            File csvFile =
                    new File(
                            csvDirectory,
                            "VitaFit_Workout_History_"
                                    + timestamp
                                    + ".csv"
                    );

            try (BufferedWriter writer =
                         new BufferedWriter(
                                 new OutputStreamWriter(
                                         new FileOutputStream(csvFile),
                                         StandardCharsets.UTF_8
                                 )
                         )) {

                writer.write('\uFEFF');
                writer.write(
                        "Workout Name,Date,Duration (min),Calories (kcal)"
                );
                writer.newLine();

                for (Workout workout : workoutList) {
                    writer.write(
                            escapeCsv(workout.getName())
                                    + ","
                                    + escapeCsv(workout.getDate())
                                    + ","
                                    + workout.getDuration()
                                    + ","
                                    + workout.getCalories()
                    );
                    writer.newLine();
                }
            }

            Toast.makeText(
                    this,
                    "CSV created successfully",
                    Toast.LENGTH_SHORT
            ).show();

            shareExportFile(
                    csvFile,
                    "text/csv",
                    "Share workout CSV",
                    "VitaFit Workout History CSV",
                    "Here is my VitaFit workout history CSV file."
            );

        } catch (IOException exception) {
            Toast.makeText(
                    this,
                    "Unable to create CSV: "
                            + exception.getMessage(),
                    Toast.LENGTH_LONG
            ).show();

        } finally {
            btnExportCsv.setEnabled(
                    !workoutList.isEmpty()
            );
            btnExportCsv.setText("CSV");
        }
    }

    private String escapeCsv(String value) {
        String safeValue =
                value == null
                        ? ""
                        : value;

        return "\""
                + safeValue.replace(
                "\"",
                "\"\""
        )
                + "\"";
    }

    private void sharePdfFile(File pdfFile) {
        shareExportFile(
                pdfFile,
                "application/pdf",
                "Share workout report",
                "VitaFit Workout History Report",
                "Here is my VitaFit workout history report."
        );
    }

    private void shareExportFile(
            File exportFile,
            String mimeType,
            String chooserTitle,
            String subject,
            String message
    ) {
        try {
            Uri exportUri =
                    FileProvider.getUriForFile(
                            this,
                            getPackageName()
                                    + ".fileprovider",
                            exportFile
                    );

            Intent shareIntent =
                    new Intent(
                            Intent.ACTION_SEND
                    );

            shareIntent.setType(mimeType);
            shareIntent.putExtra(
                    Intent.EXTRA_STREAM,
                    exportUri
            );
            shareIntent.putExtra(
                    Intent.EXTRA_SUBJECT,
                    subject
            );
            shareIntent.putExtra(
                    Intent.EXTRA_TEXT,
                    message
            );
            shareIntent.setClipData(
                    ClipData.newUri(
                            getContentResolver(),
                            "VitaFit export",
                            exportUri
                    )
            );
            shareIntent.addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
            );

            startActivity(
                    Intent.createChooser(
                            shareIntent,
                            chooserTitle
                    )
            );

        } catch (ActivityNotFoundException exception) {
            Toast.makeText(
                    this,
                    "No application is available to share this file",
                    Toast.LENGTH_LONG
            ).show();

        } catch (IllegalArgumentException
                 | SecurityException exception) {
            Toast.makeText(
                    this,
                    "Unable to share file: check FileProvider configuration",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void showEditWorkoutDialog(
            Workout workout,
            int position
    ) {
        if (workout == null) {
            Toast.makeText(
                    this,
                    "Unable to edit this workout",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        View dialogView =
                getLayoutInflater().inflate(
                        R.layout.dialog_edit_workout,
                        null
                );

        EditText etWorkoutName =
                dialogView.findViewById(
                        R.id.etWorkoutName
                );

        EditText etDuration =
                dialogView.findViewById(
                        R.id.etDuration
                );

        EditText etCalories =
                dialogView.findViewById(
                        R.id.etCalories
                );

        etWorkoutName.setText(workout.getName());
        etDuration.setText(
                String.valueOf(workout.getDuration())
        );
        etCalories.setText(
                String.valueOf(workout.getCalories())
        );

        AlertDialog editDialog =
                new MaterialAlertDialogBuilder(this)
                        .setTitle("Edit Workout")
                        .setView(dialogView)
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Update", null)
                        .create();

        editDialog.setOnShowListener(dialog ->
                editDialog.getButton(
                        AlertDialog.BUTTON_POSITIVE
                ).setOnClickListener(view -> {

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

                    if (workoutName.isEmpty()) {
                        etWorkoutName.setError(
                                "Enter workout name"
                        );
                        etWorkoutName.requestFocus();
                        return;
                    }

                    if (workoutName.length() < 2
                            || workoutName.length() > 40) {

                        etWorkoutName.setError(
                                "Workout name must contain 2 to 40 characters"
                        );
                        etWorkoutName.requestFocus();
                        return;
                    }

                    if (durationText.isEmpty()) {
                        etDuration.setError(
                                "Enter workout duration"
                        );
                        etDuration.requestFocus();
                        return;
                    }

                    if (caloriesText.isEmpty()) {
                        etCalories.setError(
                                "Enter calories burned"
                        );
                        etCalories.requestFocus();
                        return;
                    }

                    int duration;
                    int calories;

                    try {
                        duration =
                                Integer.parseInt(durationText);

                        calories =
                                Integer.parseInt(caloriesText);

                    } catch (NumberFormatException exception) {
                        Toast.makeText(
                                HistoryActivity.this,
                                "Enter valid whole numbers",
                                Toast.LENGTH_SHORT
                        ).show();
                        return;
                    }

                    if (duration < MIN_DURATION_MINUTES
                            || duration > MAX_DURATION_MINUTES) {

                        etDuration.setError(
                                "Enter duration between 1 and 600 minutes"
                        );
                        etDuration.requestFocus();
                        return;
                    }

                    if (calories < MIN_CALORIES
                            || calories > MAX_CALORIES) {

                        etCalories.setError(
                                "Enter calories between 1 and 5000 kcal"
                        );
                        etCalories.requestFocus();
                        return;
                    }

                    updateWorkoutInFirebase(
                            workout,
                            workoutName,
                            duration,
                            calories,
                            editDialog
                    );
                })
        );

        editDialog.show();
    }

    private void updateWorkoutInFirebase(
            Workout workout,
            String workoutName,
            int duration,
            int calories,
            AlertDialog editDialog
    ) {
        String firebaseId =
                workout.getFirebaseId();

        if (firebaseId == null
                || firebaseId.trim().isEmpty()) {

            Toast.makeText(
                    this,
                    "Firebase workout ID is missing",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        Map<String, Object> updatedData =
                new HashMap<>();

        updatedData.put(
                "workoutName",
                workoutName
        );
        updatedData.put(
                "duration",
                duration
        );
        updatedData.put(
                "calories",
                calories
        );
        updatedData.put(
                "updatedAt",
                System.currentTimeMillis()
        );

        editDialog.getButton(
                AlertDialog.BUTTON_POSITIVE
        ).setEnabled(false);

        workoutsReference
                .child(firebaseId)
                .updateChildren(updatedData)
                .addOnSuccessListener(unused -> {
                    workout.setName(workoutName);
                    workout.setDuration(duration);
                    workout.setCalories(calories);

                    editDialog.dismiss();

                    Toast.makeText(
                            HistoryActivity.this,
                            "Workout updated successfully",
                            Toast.LENGTH_SHORT
                    ).show();
                })
                .addOnFailureListener(exception -> {
                    editDialog.getButton(
                            AlertDialog.BUTTON_POSITIVE
                    ).setEnabled(true);

                    Toast.makeText(
                            HistoryActivity.this,
                            "Update failed: "
                                    + exception.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void deleteWorkoutFromFirebase(
            Workout workout,
            int position
    ) {
        String firebaseId =
                workout.getFirebaseId();

        if (firebaseId == null
                || firebaseId
                .trim()
                .isEmpty()) {

            Toast.makeText(
                    this,
                    "Firebase workout ID is missing",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        workoutsReference
                .child(firebaseId)
                .removeValue()
                .addOnSuccessListener(unused ->
                        Toast.makeText(
                                HistoryActivity.this,
                                "Workout deleted successfully",
                                Toast.LENGTH_SHORT
                        ).show()
                )
                .addOnFailureListener(exception ->
                        Toast.makeText(
                                HistoryActivity.this,
                                "Delete failed: "
                                        + exception
                                        .getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (bottomNavigationView != null
                && bottomNavigationView.getSelectedItemId()
                != R.id.nav_history) {
            bottomNavigationView.setSelectedItemId(
                    R.id.nav_history
            );
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (workoutsReference != null
                && workoutsListener != null) {

            workoutsReference
                    .removeEventListener(
                            workoutsListener
                    );

            workoutsListener = null;
        }
    }
}