package model;

public class Workout {

    private int id;
    private String firebaseId;
    private String name;
    private int duration;
    private int calories;
    private String date;

    // Required by Firebase
    public Workout() {
    }

    // Constructor for SQLite
    public Workout(
            int id,
            String name,
            int duration,
            int calories,
            String date
    ) {
        this.id = id;
        this.name = name;
        this.duration = duration;
        this.calories = calories;
        this.date = date;
    }

    // Constructor for Firebase
    public Workout(
            String firebaseId,
            String name,
            int duration,
            int calories,
            String date
    ) {
        this.firebaseId = firebaseId;
        this.name = name;
        this.duration = duration;
        this.calories = calories;
        this.date = date;
    }

    // ID
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    // Firebase ID
    public String getFirebaseId() {
        return firebaseId;
    }

    public void setFirebaseId(String firebaseId) {
        this.firebaseId = firebaseId;
    }

    // Workout Name
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    // Compatibility method for PDF and other code
    public String getWorkoutName() {
        return name;
    }

    public void setWorkoutName(String workoutName) {
        this.name = workoutName;
    }

    // Duration
    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    // Calories
    public int getCalories() {
        return calories;
    }

    public void setCalories(int calories) {
        this.calories = calories;
    }

    // Date
    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}