package database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

public class WorkoutDBHelper extends SQLiteOpenHelper {

    // Database Details
    private static final String DATABASE_NAME = "FitnessTracker.db";
    private static final int DATABASE_VERSION = 1;

    // Table Name
    public static final String TABLE_WORKOUT = "workouts";

    // Columns
    public static final String COL_ID = "id";
    public static final String COL_NAME = "workout_name";
    public static final String COL_DURATION = "duration";
    public static final String COL_CALORIES = "calories";
    public static final String COL_DATE = "date";

    public WorkoutDBHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        String CREATE_TABLE =
                "CREATE TABLE " + TABLE_WORKOUT + " (" +
                        COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                        COL_NAME + " TEXT," +
                        COL_DURATION + " INTEGER," +
                        COL_CALORIES + " INTEGER," +
                        COL_DATE + " TEXT)";

        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        db.execSQL("DROP TABLE IF EXISTS " + TABLE_WORKOUT);
        onCreate(db);
    }

    // Insert Workout
    public boolean insertWorkout(String name,
                                 int duration,
                                 int calories,
                                 String date) {

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put(COL_NAME, name);
        values.put(COL_DURATION, duration);
        values.put(COL_CALORIES, calories);
        values.put(COL_DATE, date);

        long result = db.insert(TABLE_WORKOUT, null, values);

        db.close();

        return result != -1;
    }

    // Total Workouts
    public int getTotalWorkouts() {

        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT COUNT(*) FROM " + TABLE_WORKOUT,
                null
        );

        int total = 0;

        if (cursor.moveToFirst()) {
            total = cursor.getInt(0);
        }

        cursor.close();
        db.close();

        return total;
    }

    // Total Workout Duration
    public int getTotalDuration() {

        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT SUM(" + COL_DURATION + ") FROM " + TABLE_WORKOUT,
                null
        );

        int total = 0;

        if (cursor.moveToFirst() && !cursor.isNull(0)) {
            total = cursor.getInt(0);
        }

        cursor.close();
        db.close();

        return total;
    }

    // Total Calories
    public int getTotalCalories() {

        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT SUM(" + COL_CALORIES + ") FROM " + TABLE_WORKOUT,
                null
        );

        int total = 0;

        if (cursor.moveToFirst() && !cursor.isNull(0)) {
            total = cursor.getInt(0);
        }

        cursor.close();
        db.close();

        return total;
    }

    // Get All Workouts
    public Cursor getAllWorkouts() {

        SQLiteDatabase db = this.getReadableDatabase();

        return db.rawQuery(
                "SELECT * FROM " + TABLE_WORKOUT +
                        " ORDER BY " + COL_ID + " DESC",
                null
        );
    }

    // Delete Workout
    public void deleteWorkout(int id) {

        SQLiteDatabase db = this.getWritableDatabase();

        db.delete(
                TABLE_WORKOUT,
                COL_ID + "=?",
                new String[]{String.valueOf(id)}
        );

        db.close();
    }

}