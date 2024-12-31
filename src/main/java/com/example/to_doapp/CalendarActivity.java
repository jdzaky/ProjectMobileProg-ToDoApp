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
import android.widget.CalendarView;
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
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CalendarActivity extends AppCompatActivity {
    private CalendarView calendarView;
    private RecyclerView taskList;
    private TaskAdapter taskAdapter;
    private DatabaseHelper dbHelper;
    private LinearLayout emptyState;
    private FloatingActionButton fabAddTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.kalender_activity);

        initializeViews();
        setupCalendarView();
        setupRecyclerView();
        setupBottomNavigation();
        setupFloatingActionButton();
    }

    private void initializeViews() {
        calendarView = findViewById(R.id.calendar_view);
        taskList = findViewById(R.id.task_list);
        emptyState = findViewById(R.id.empty_state);
        fabAddTask = findViewById(R.id.fab_add_task);
        dbHelper = new DatabaseHelper(this);
        fabAddTask.setOnClickListener(v -> showAddTaskDialog());
    }

    private void setupCalendarView() {
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            // Format selected date to match database format
            String selectedDate = String.format(Locale.getDefault(), "%d-%02d-%02d",
                    year, month + 1, dayOfMonth);
            loadTasksForDate(selectedDate);
        });
    }

    private void setupRecyclerView() {
        taskAdapter = new TaskAdapter(this);
        taskList.setLayoutManager(new LinearLayoutManager(this));
        taskList.setAdapter(taskAdapter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_tasks) {
                startActivity(new Intent(this, TaskActivity.class));
                return true;
            } else if (itemId == R.id.nav_star) {
                startActivity(new Intent(this, StarTaskActivity.class));
                return true;
            }
            return itemId == R.id.nav_calendar;
        });
    }

    private void setupFloatingActionButton() {
        fabAddTask = findViewById(R.id.fab_add_task);
        fabAddTask.setOnClickListener(v -> showAddTaskDialog());
    }

    @SuppressLint("Range")
    private void loadTasksForDate(String date) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // Query tasks for selected date
        String selection = DatabaseHelper.COLUMN_DUE_DATE + " = ?";
        String[] selectionArgs = {date};

        Cursor cursor = db.query(
                DatabaseHelper.TABLE_TASKS,
                null,
                selection,
                selectionArgs,
                null,
                null,
                null
        );

        List<Task> tasksForDate = new ArrayList<>();

        if (cursor != null && cursor.moveToFirst()) {
            do {
                Task task = new Task();
                task.setId(cursor.getLong(cursor.getColumnIndex(DatabaseHelper.COLUMN_ID)));
                task.setTitle(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_TITLE)));
                task.setCompleted(cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_IS_COMPLETED)) == 1);
                tasksForDate.add(task);
            } while (cursor.moveToNext());
            cursor.close();
        }

        // Update UI based on whether we have tasks or not
        if (tasksForDate.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            taskList.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            taskList.setVisibility(View.VISIBLE);
            taskAdapter.setTasks(tasksForDate);
        }
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

            // Tambahkan log untuk debugging
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




    private void loadTasks() {
        // Get current selected date from CalendarView
        long selectedDate = calendarView.getDate();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String dateString = sdf.format(new Date(selectedDate));

        // Load tasks for the selected date
        loadTasksForDate(dateString);
    }
}
