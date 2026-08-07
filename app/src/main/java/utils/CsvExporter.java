package utils;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import model.Workout;

public class CsvExporter {

    private CsvExporter() {
        // Prevent object creation
    }

    public static void exportWorkouts(
            Context context,
            List<Workout> workoutList
    ) {
        if (context == null) {
            return;
        }

        if (workoutList == null || workoutList.isEmpty()) {
            Toast.makeText(
                    context,
                    "No workouts available to export",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        try {
            File reportsDirectory =
                    new File(
                            context.getCacheDir(),
                            "reports"
                    );

            if (!reportsDirectory.exists()
                    && !reportsDirectory.mkdirs()) {

                throw new IOException(
                        "Unable to create reports folder"
                );
            }

            String timestamp =
                    new SimpleDateFormat(
                            "yyyyMMdd_HHmmss",
                            Locale.getDefault()
                    ).format(new Date());

            File csvFile =
                    new File(
                            reportsDirectory,
                            "VitaFit_Workouts_"
                                    + timestamp
                                    + ".csv"
                    );

            String csvContent =
                    createCsvContent(workoutList);

            try (FileOutputStream outputStream =
                         new FileOutputStream(csvFile)) {

                /*
                 * UTF-8 BOM helps Excel correctly display
                 * special characters and emojis.
                 */
                outputStream.write(
                        new byte[]{
                                (byte) 0xEF,
                                (byte) 0xBB,
                                (byte) 0xBF
                        }
                );

                outputStream.write(
                        csvContent.getBytes(
                                StandardCharsets.UTF_8
                        )
                );

                outputStream.flush();
            }

            Toast.makeText(
                    context,
                    "CSV created successfully",
                    Toast.LENGTH_SHORT
            ).show();

            shareCsvFile(
                    context,
                    csvFile
            );

        } catch (IOException exception) {

            Toast.makeText(
                    context,
                    "Unable to create CSV: "
                            + exception.getMessage(),
                    Toast.LENGTH_LONG
            ).show();

        } catch (Exception exception) {

            Toast.makeText(
                    context,
                    "Unexpected error: "
                            + exception.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private static String createCsvContent(
            List<Workout> workoutList
    ) {
        StringBuilder csvBuilder =
                new StringBuilder();

        csvBuilder.append(
                "No.,Workout,Duration (minutes),Calories (kcal),Date"
        );

        csvBuilder.append("\n");

        int totalDuration = 0;
        int totalCalories = 0;

        for (int index = 0;
             index < workoutList.size();
             index++) {

            Workout workout =
                    workoutList.get(index);

            String workoutName =
                    workout.getName();

            String date =
                    workout.getDate();

            if (workoutName == null
                    || workoutName.trim().isEmpty()) {

                workoutName =
                        "Unknown Workout";
            }

            if (date == null
                    || date.trim().isEmpty()) {

                date = "No date";
            }

            csvBuilder.append(
                    index + 1
            );

            csvBuilder.append(",");

            csvBuilder.append(
                    escapeCsvValue(workoutName)
            );

            csvBuilder.append(",");

            csvBuilder.append(
                    workout.getDuration()
            );

            csvBuilder.append(",");

            csvBuilder.append(
                    workout.getCalories()
            );

            csvBuilder.append(",");

            csvBuilder.append(
                    escapeCsvValue(date)
            );

            csvBuilder.append("\n");

            totalDuration +=
                    workout.getDuration();

            totalCalories +=
                    workout.getCalories();
        }

        csvBuilder.append("\n");
        csvBuilder.append("Summary");
        csvBuilder.append("\n");

        csvBuilder.append(
                "Total Workouts,"
        );

        csvBuilder.append(
                workoutList.size()
        );

        csvBuilder.append("\n");

        csvBuilder.append(
                "Total Duration,"
        );

        csvBuilder.append(
                totalDuration
        );

        csvBuilder.append(" minutes");
        csvBuilder.append("\n");

        csvBuilder.append(
                "Total Calories,"
        );

        csvBuilder.append(
                totalCalories
        );

        csvBuilder.append(" kcal");
        csvBuilder.append("\n");

        return csvBuilder.toString();
    }

    private static String escapeCsvValue(
            String value
    ) {
        if (value == null) {
            return "";
        }

        String escapedValue =
                value.replace(
                        "\"",
                        "\"\""
                );

        boolean requiresQuotes =
                escapedValue.contains(",")
                        || escapedValue.contains("\"")
                        || escapedValue.contains("\n")
                        || escapedValue.contains("\r");

        if (requiresQuotes) {
            return "\""
                    + escapedValue
                    + "\"";
        }

        return escapedValue;
    }

    private static void shareCsvFile(
            Context context,
            File csvFile
    ) {
        try {
            Uri csvUri =
                    FileProvider.getUriForFile(
                            context,
                            context.getPackageName()
                                    + ".fileprovider",
                            csvFile
                    );

            Intent shareIntent =
                    new Intent(
                            Intent.ACTION_SEND
                    );

            shareIntent.setType(
                    "text/csv"
            );

            shareIntent.putExtra(
                    Intent.EXTRA_STREAM,
                    csvUri
            );

            shareIntent.putExtra(
                    Intent.EXTRA_SUBJECT,
                    "VitaFit Workout History"
            );

            shareIntent.putExtra(
                    Intent.EXTRA_TEXT,
                    "Here is my VitaFit workout history CSV file."
            );

            shareIntent.addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
            );

            Intent chooserIntent =
                    Intent.createChooser(
                            shareIntent,
                            "Share workout CSV"
                    );

            context.startActivity(
                    chooserIntent
            );

        } catch (ActivityNotFoundException exception) {

            Toast.makeText(
                    context,
                    "No application is available to share the CSV",
                    Toast.LENGTH_LONG
            ).show();

        } catch (IllegalArgumentException exception) {

            Toast.makeText(
                    context,
                    "FileProvider configuration is missing",
                    Toast.LENGTH_LONG
            ).show();
        }
    }
}