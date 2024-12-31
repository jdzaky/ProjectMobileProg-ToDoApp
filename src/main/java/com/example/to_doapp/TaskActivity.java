package com.example.to_doapp;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

// TaskActivity.java
public class TaskActivity extends AppCompatActivity {
    private RecyclerView todayTasksList, upcomingTasksList, completedTasksList;
    private TaskAdapter todayAdapter, upcomingAdapter, completedAdapter;
    private DatabaseHelper dbHelper;
    private ChipGroup categoryChipGroup;
    private FloatingActionButton fabAddTask;
    private View todaySection, upcomingSection, completedSection;
    private View emptyState;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.task_activity);

        dbHelper = new DatabaseHelper(this);
        initializeViews();
        setupBottomNavigation();
        setupRecyclerView();
        loadTasks();
        setupFloatingActionButton();
    }

    private void initializeViews() {
        categoryChipGroup = findViewById(R.id.category_chip_group);
        fabAddTask = findViewById(R.id.fab_add_task);
        emptyState = findViewById(R.id.empty_state);

        todaySection = findViewById(R.id.today_section);
        upcomingSection = findViewById(R.id.upcoming_section);
        completedSection = findViewById(R.id.completed_section);

        todayTasksList = findViewById(R.id.today_tasks_list);
        upcomingTasksList = findViewById(R.id.upcoming_tasks_list);
        completedTasksList = findViewById(R.id.completed_tasks_list);

        fabAddTask.setOnClickListener(v -> showAddTaskDialog());
    }

    private void setupFloatingActionButton() {
        fabAddTask = findViewById(R.id.fab_add_task);
        fabAddTask.setOnClickListener(v -> showAddTaskDialog());
    }


    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_calendar) {
                Intent calendarIntent = new Intent(TaskActivity.this, CalendarActivity.class);
                startActivity(calendarIntent);
                return true;
            } else
                if (itemId == R.id.nav_star) {
                Intent starIntent = new Intent(TaskActivity.this, StarTaskActivity.class);
                startActivity(starIntent);
                return true;
            } else return itemId == R.id.nav_tasks;
        });
    }


    private void setupRecyclerView() {
        todayAdapter = new TaskAdapter(this);
        upcomingAdapter = new TaskAdapter(this);
        completedAdapter = new TaskAdapter(this);

        todayTasksList.setLayoutManager(new LinearLayoutManager(this));
        upcomingTasksList.setLayoutManager(new LinearLayoutManager(this));
        completedTasksList.setLayoutManager(new LinearLayoutManager(this));

        todayTasksList.setAdapter(todayAdapter);
        upcomingTasksList.setAdapter(upcomingAdapter);
        completedTasksList.setAdapter(completedAdapter);
    }

    @SuppressLint("Range")
    private void loadTasks() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<Task> todayTasks = new ArrayList<>();
        List<Task> upcomingTasks = new ArrayList<>();
        List<Task> completedTasks = new ArrayList<>();

        // Get current date
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(new Date());

        Cursor cursor = db.query(
                DatabaseHelper.TABLE_TASKS,
                null,
                null,
                null,
                null,
                null,
                DatabaseHelper.COLUMN_DUE_DATE + " ASC"
        );

        if (cursor != null && cursor.moveToFirst()) {
            do {
                Task task = new Task();
                task.setId(cursor.getLong(cursor.getColumnIndex(DatabaseHelper.COLUMN_ID)));
                task.setTitle(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_TITLE)));
                task.setDueDate(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_DUE_DATE)));
                task.setCompleted(cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_IS_COMPLETED)) == 1);

                if (task.isCompleted()) {
                    completedTasks.add(task);
                } else if (task.getDueDate() != null) {
                    if (task.getDueDate().equals(today)) {
                        todayTasks.add(task);
                    } else if (task.getDueDate().compareTo(today) > 0) {
                        upcomingTasks.add(task);
                    }
                }
            } while (cursor.moveToNext());
            cursor.close();
        }

        // Check if all task lists are empty
        boolean hasNoTasks = todayTasks.isEmpty() &&
                upcomingTasks.isEmpty() &&
                completedTasks.isEmpty();

        // Show/hide empty state
        emptyState.setVisibility(hasNoTasks ? View.VISIBLE : View.GONE);

        // Update visibility and adapters
        updateSectionVisibility(todaySection, todayTasks);
        updateSectionVisibility(upcomingSection, upcomingTasks);
        updateSectionVisibility(completedSection, completedTasks);

        todayAdapter.setTasks(todayTasks);
        upcomingAdapter.setTasks(upcomingTasks);
        completedAdapter.setTasks(completedTasks);
    }

    private void updateSectionVisibility(View section, List<Task> tasks) {
        section.setVisibility(tasks.isEmpty() ? View.GONE : View.VISIBLE);
    }


    private void showAddTaskDialog() {
        View bottomSheetView = getLayoutInflater().inflate(R.layout.add_task, null);
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        bottomSheetDialog.setContentView(bottomSheetView);

        EditText inputTask = bottomSheetView.findViewById(R.id.input_task);
        ImageButton btnAddTask = bottomSheetView.findViewById(R.id.btn_add_task);
        ImageButton btnSubtask = bottomSheetView.findViewById(R.id.btn_subtask);
        ImageButton btnDueDate = bottomSheetView.findViewById(R.id.btn_due_date);
        LinearLayout subtasksContainer = bottomSheetView.findViewById(R.id.subtasks_container);
        LinearLayout deadlineContainer = bottomSheetView.findViewById(R.id.deadline_container);
        TextView selectedDate = bottomSheetView.findViewById(R.id.selected_date);
        TextView selectedTime = bottomSheetView.findViewById(R.id.selected_time);

        // Add subtask button click listener
        btnSubtask.setOnClickListener(v -> {
            View subtaskView = getLayoutInflater().inflate(R.layout.item_subtask, subtasksContainer, false);
            EditText subtaskInput = subtaskView.findViewById(R.id.subtask_input);
            ImageButton btnDeleteSubtask = subtaskView.findViewById(R.id.btn_delete_subtask);

            btnDeleteSubtask.setOnClickListener(deleteView -> subtasksContainer.removeView(subtaskView));
            subtasksContainer.addView(subtaskView);
        });

        // Due date button click listener
        btnDueDate.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    this,
                    (view, year, month, dayOfMonth) -> {
                        TimePickerDialog timePickerDialog = new TimePickerDialog(
                                this,
                                (timeView, hourOfDay, minute) -> {
                                    String date = String.format(Locale.getDefault(), "%d-%02d-%02d",
                                            year, month + 1, dayOfMonth);
                                    String time = String.format(Locale.getDefault(), "%02d:%02d",
                                            hourOfDay, minute);

                                    selectedDate.setText(date);
                                    selectedTime.setText(time);
                                    deadlineContainer.setVisibility(View.VISIBLE);
                                },
                                Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
                                Calendar.getInstance().get(Calendar.MINUTE),
                                true
                        );
                        timePickerDialog.show();
                    },
                    Calendar.getInstance().get(Calendar.YEAR),
                    Calendar.getInstance().get(Calendar.MONTH),
                    Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        });

        // Add task button click listener
        btnAddTask.setOnClickListener(v -> {
            String taskTitle = inputTask.getText().toString().trim();
            String date = selectedDate.getText().toString();
            String time = selectedTime.getText().toString();

            if (!taskTitle.isEmpty()) {
                if (date.isEmpty() || time.isEmpty() || date.equals("") || time.equals("")) {
                    Toast.makeText(this, "Tolong isi tanggal dan waktu", Toast.LENGTH_SHORT).show();
                    return;
                }

                SQLiteDatabase db = dbHelper.getWritableDatabase();
                ContentValues values = new ContentValues();
                values.put(DatabaseHelper.COLUMN_TITLE, taskTitle);
                values.put(DatabaseHelper.COLUMN_DUE_DATE, date);
                values.put(DatabaseHelper.COLUMN_TIME, time);

                long taskId = db.insert(DatabaseHelper.TABLE_TASKS, null, values);

                // Schedule alarm for the task
                scheduleTaskAlarm(taskId, taskTitle, date, time);

                // Save subtasks if any
                for (int i = 0; i < subtasksContainer.getChildCount(); i++) {
                    View subtaskView = subtasksContainer.getChildAt(i);
                    EditText subtaskInput = subtaskView.findViewById(R.id.subtask_input);
                    String subtaskTitle = subtaskInput.getText().toString().trim();

                    if (!subtaskTitle.isEmpty()) {
                        ContentValues subtaskValues = new ContentValues();
                        subtaskValues.put(DatabaseHelper.COLUMN_SUBTASK_TITLE, subtaskTitle);
                        subtaskValues.put(DatabaseHelper.COLUMN_PARENT_TASK_ID, taskId);
                        db.insert(DatabaseHelper.TABLE_SUBTASKS, null, subtaskValues);
                    }
                }

                bottomSheetDialog.dismiss();
                loadTasks();
            }
        });


        bottomSheetDialog.show();
    }

    private void scheduleTaskAlarm(long taskId, String taskTitle, String dueDate, String time) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, AlarmReceiver.class);
        intent.putExtra("task_title", taskTitle);

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            Date date = sdf.parse(dueDate + " " + time);
            long timeInMillis = date.getTime();

            Log.d("AlarmScheduler", "Setting alarm for: " + new Date(timeInMillis).toString());

            if (timeInMillis > System.currentTimeMillis()) {
                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                        this,
                        (int) taskId,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setAlarmClock(
                                new AlarmManager.AlarmClockInfo(timeInMillis, pendingIntent),
                                pendingIntent
                        );
                    }
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            timeInMillis,
                            pendingIntent
                    );
                }
            }
        } catch (ParseException e) {
            e.printStackTrace();
            Log.e("AlarmScheduler", "Error scheduling alarm: " + e.getMessage());
        }
    }




}

