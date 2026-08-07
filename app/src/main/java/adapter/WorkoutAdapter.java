package adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ashutosh.codealpha_fitnesstrackerapp.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import model.Workout;

public class WorkoutAdapter extends
        RecyclerView.Adapter<WorkoutAdapter.WorkoutViewHolder> {

    public interface OnWorkoutDeleteListener {

        void onDeleteWorkout(
                Workout workout,
                int position
        );
    }

    public interface OnWorkoutEditListener {

        void onEditWorkout(
                Workout workout,
                int position
        );
    }

    private final Context context;

    private final ArrayList<Workout> completeWorkoutList;
    private final ArrayList<Workout> displayedWorkoutList;

    private final OnWorkoutDeleteListener deleteListener;
    private final OnWorkoutEditListener editListener;

    public WorkoutAdapter(
            Context context,
            ArrayList<Workout> workoutList,
            OnWorkoutDeleteListener deleteListener,
            OnWorkoutEditListener editListener
    ) {
        this.context = context;
        this.completeWorkoutList = new ArrayList<>();
        this.displayedWorkoutList = new ArrayList<>();
        this.deleteListener = deleteListener;
        this.editListener = editListener;

        setWorkouts(workoutList);
    }

    @NonNull
    @Override
    public WorkoutViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View view = LayoutInflater
                .from(parent.getContext())
                .inflate(
                        R.layout.item_workout,
                        parent,
                        false
                );

        return new WorkoutViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull WorkoutViewHolder holder,
            int position
    ) {
        Workout workout =
                displayedWorkoutList.get(position);

        String workoutName =
                workout.getName();

        if (workoutName == null
                || workoutName.trim().isEmpty()) {

            workoutName = "Unknown Workout";
        }

        String workoutDate =
                workout.getDate();

        if (workoutDate == null
                || workoutDate.trim().isEmpty()) {

            workoutDate = "No date";
        }

        holder.txtWorkoutName.setText(workoutName);

        holder.txtDate.setText(
                "📅 " + workoutDate
        );

        holder.txtDuration.setText(
                workout.getDuration() + " min"
        );

        holder.txtCalories.setText(
                workout.getCalories() + " kcal"
        );

        holder.itemView.setOnClickListener(v -> {

            int currentPosition =
                    holder.getBindingAdapterPosition();

            if (currentPosition ==
                    RecyclerView.NO_POSITION) {

                return;
            }

            if (editListener != null) {

                Workout selectedWorkout =
                        displayedWorkoutList.get(
                                currentPosition
                        );

                editListener.onEditWorkout(
                        selectedWorkout,
                        currentPosition
                );
            }
        });

        holder.btnDeleteWorkout.setOnClickListener(v -> {

            int currentPosition =
                    holder.getBindingAdapterPosition();

            if (currentPosition ==
                    RecyclerView.NO_POSITION) {

                return;
            }

            Workout selectedWorkout =
                    displayedWorkoutList.get(
                            currentPosition
                    );

            showDeleteDialog(
                    selectedWorkout,
                    currentPosition
            );
        });
    }

    public void setWorkouts(
            List<Workout> newWorkoutList
    ) {
        completeWorkoutList.clear();
        displayedWorkoutList.clear();

        if (newWorkoutList != null) {

            completeWorkoutList.addAll(
                    newWorkoutList
            );

            displayedWorkoutList.addAll(
                    newWorkoutList
            );
        }

        notifyDataSetChanged();
    }

    public void filterWorkouts(
            String searchText
    ) {
        displayedWorkoutList.clear();

        if (searchText == null
                || searchText.trim().isEmpty()) {

            displayedWorkoutList.addAll(
                    completeWorkoutList
            );

        } else {

            String normalizedSearchText =
                    searchText
                            .trim()
                            .toLowerCase(
                                    Locale.getDefault()
                            );

            for (Workout workout
                    : completeWorkoutList) {

                String workoutName =
                        workout.getName() == null
                                ? ""
                                : workout.getName()
                                .trim()
                                .toLowerCase(
                                        Locale.getDefault()
                                );

                String workoutDate =
                        workout.getDate() == null
                                ? ""
                                : workout.getDate()
                                .trim()
                                .toLowerCase(
                                        Locale.getDefault()
                                );

                boolean matchesName =
                        workoutName.contains(
                                normalizedSearchText
                        );

                boolean matchesDate =
                        workoutDate.contains(
                                normalizedSearchText
                        );

                if (matchesName || matchesDate) {

                    displayedWorkoutList.add(
                            workout
                    );
                }
            }
        }

        notifyDataSetChanged();
    }

    public int getFilteredItemCount() {
        return displayedWorkoutList.size();
    }

    public Workout getWorkoutAt(
            int position
    ) {
        if (position < 0
                || position >= displayedWorkoutList.size()) {

            return null;
        }

        return displayedWorkoutList.get(position);
    }

    private void showDeleteDialog(
            Workout workout,
            int position
    ) {
        String workoutName =
                workout.getName();

        if (workoutName == null
                || workoutName.trim().isEmpty()) {

            workoutName = "this workout";
        }

        new AlertDialog.Builder(context)
                .setTitle("Delete Workout")
                .setMessage(
                        "Are you sure you want to delete "
                                + workoutName
                                + "?"
                )
                .setPositiveButton(
                        "Delete",
                        (dialog, which) -> {

                            if (deleteListener != null) {

                                deleteListener.onDeleteWorkout(
                                        workout,
                                        position
                                );
                            }
                        }
                )
                .setNegativeButton(
                        "Cancel",
                        null
                )
                .show();
    }

    @Override
    public int getItemCount() {
        return displayedWorkoutList.size();
    }

    public static class WorkoutViewHolder
            extends RecyclerView.ViewHolder {

        private final TextView txtWorkoutName;
        private final TextView txtDuration;
        private final TextView txtCalories;
        private final TextView txtDate;
        private final Button btnDeleteWorkout;

        public WorkoutViewHolder(
                @NonNull View itemView
        ) {
            super(itemView);

            txtWorkoutName =
                    itemView.findViewById(
                            R.id.txtWorkoutName
                    );

            txtDuration =
                    itemView.findViewById(
                            R.id.txtDuration
                    );

            txtCalories =
                    itemView.findViewById(
                            R.id.txtCalories
                    );

            txtDate =
                    itemView.findViewById(
                            R.id.txtDate
                    );

            btnDeleteWorkout =
                    itemView.findViewById(
                            R.id.btnDeleteWorkout
                    );
        }
    }
}