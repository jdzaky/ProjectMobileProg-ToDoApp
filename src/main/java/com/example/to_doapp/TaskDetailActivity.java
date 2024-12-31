package com.example.to_doapp;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.card.MaterialCardView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class TaskDetailActivity extends AppCompatActivity {
    private DatabaseHelper dbHelper;
    private long taskId;
    private EditText taskTitle;
    private TextView dueDate, time;
    private MaterialCardView cardDueDate, cardTime;
    private LinearLayout subtaskContainer;
    private Button btnAddSubtask;
    private ImageButton btnMore;
    private boolean isStarred = false;
    private boolean hasChanges = false; // Track if any changes were made
    private String originalTitle; // Store original title for comparison
    private ImageButton btnBack;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.detail_task);

        requestNotificationPermission();
        Log.d("TaskDetailActivity", "onCreate started");

        dbHelper = new DatabaseHelper(this);
        taskId = getIntent().getLongExtra("task_id", -1);

        Log.d("TaskDetailActivity", "Task ID received: " + taskId);

        try {
            initializeViews();
            loadTaskDetails();
            setupListeners();
            setupMoreOptions();
            setupDateTimeEditing();

            originalTitle = taskTitle.getText().toString();

            setupTaskEditing();
            loadTaskDetails();

            btnBack = findViewById(R.id.btn_back);
            btnBack.setOnClickListener(v -> finish());
        }catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error initializing task details", Toast.LENGTH_SHORT).show();
            finish();
        }

    }

    private void setupTaskEditing() {
        taskTitle.setEnabled(true);

        Button btnSave = findViewById(R.id.btn_save);
        btnSave.setOnClickListener(v -> {
            saveTaskChanges();
            finish();
        });
    }

    private void saveTaskChanges() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues taskValues = new ContentValues();
        taskValues.put(DatabaseHelper.COLUMN_TITLE, taskTitle.getText().toString());
        db.update(DatabaseHelper.TABLE_TASKS, taskValues,
                DatabaseHelper.COLUMN_ID + " = ?",
                new String[]{String.valueOf(taskId)});

        db.delete(DatabaseHelper.TABLE_SUBTASKS,
                DatabaseHelper.COLUMN_PARENT_TASK_ID + " = ?",
                new String[]{String.valueOf(taskId)});

        for (int i = 0; i < subtaskContainer.getChildCount(); i++) {
            View subtaskView = subtaskContainer.getChildAt(i);
            EditText subtaskInput = subtaskView.findViewById(R.id.subtask_input);
            String subtaskTitle = subtaskInput.getText().toString().trim();

            if (!subtaskTitle.isEmpty()) {
                ContentValues subtaskValues = new ContentValues();
                subtaskValues.put(DatabaseHelper.COLUMN_SUBTASK_TITLE, subtaskTitle);
                subtaskValues.put(DatabaseHelper.COLUMN_PARENT_TASK_ID, taskId);
                db.insert(DatabaseHelper.TABLE_SUBTASKS, null, subtaskValues);
            }
        }

        Toast.makeText(this, "Changes saved", Toast.LENGTH_SHORT).show();
    }




    private void initializeViews() {
        taskTitle = findViewById(R.id.input_task);
        dueDate = findViewById(R.id.due_date);
        time = findViewById(R.id.time);
        cardDueDate = findViewById(R.id.card_due_date);
        cardTime = findViewById(R.id.card_time);
        subtaskContainer = findViewById(R.id.subtask_container);
        btnAddSubtask = findViewById(R.id.btn_add_subtask);
        taskTitle = findViewById(R.id.input_task);
    }

    @SuppressLint("Range")
    private void loadTaskDetails() {
        if (taskId == -1) {
            Toast.makeText(this, "Invalid task ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.query(
                DatabaseHelper.TABLE_TASKS,
                null,
                DatabaseHelper.COLUMN_ID + " = ?",
                new String[]{String.valueOf(taskId)},
                null, null, null
        );

        if (cursor != null && cursor.moveToFirst()) {
            try {
                String title = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_TITLE));
                String date = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_DUE_DATE));
                String time = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_TIME));

                taskTitle.setText(title != null ? title : "");
                if (date != null) dueDate.setText(date);
                if (time != null) this.time.setText(time);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Error loading task details", Toast.LENGTH_SHORT).show();
            } finally {
                cursor.close();
            }
        } else {
            Toast.makeText(this, "Task not found", Toast.LENGTH_SHORT).show();
            finish();
        }

        subtaskContainer.removeAllViews();

        cursor = db.query(
                DatabaseHelper.TABLE_SUBTASKS,
                null,
                DatabaseHelper.COLUMN_PARENT_TASK_ID + " = ?",
                new String[]{String.valueOf(taskId)},
                null, null, null
        );

        if (cursor != null && cursor.moveToFirst()) {
            try {
                do {
                    String subtaskTitle = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_SUBTASK_TITLE));
                    long subtaskId = cursor.getLong(cursor.getColumnIndex(DatabaseHelper.COLUMN_ID));
                    if (subtaskTitle != null) {
                        addSubtaskView(subtaskTitle, subtaskId);
                    }
                } while (cursor.moveToNext());
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Error loading subtasks", Toast.LENGTH_SHORT).show();
            } finally {
                cursor.close();
            }
        }
    }



    private void addSubtaskView(String subtaskTitle, long subtaskId) {
        View subtaskView = getLayoutInflater().inflate(R.layout.item_subtask, subtaskContainer, false);
        EditText subtaskInput = subtaskView.findViewById(R.id.subtask_input);
        ImageButton btnDeleteSubtask = subtaskView.findViewById(R.id.btn_delete_subtask);

        subtaskInput.setText(subtaskTitle);
        subtaskInput.setEnabled(true);

        btnDeleteSubtask.setOnClickListener(v -> {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            db.delete(DatabaseHelper.TABLE_SUBTASKS,
                    DatabaseHelper.COLUMN_ID + " = ?",
                    new String[]{String.valueOf(subtaskId)});
            subtaskContainer.removeView(subtaskView);
        });

        subtaskInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String newSubtaskTitle = subtaskInput.getText().toString().trim();
                if (!newSubtaskTitle.isEmpty()) {
                    SQLiteDatabase db = dbHelper.getWritableDatabase();
                    ContentValues values = new ContentValues();
                    values.put(DatabaseHelper.COLUMN_SUBTASK_TITLE, newSubtaskTitle);

                    db.update(DatabaseHelper.TABLE_SUBTASKS,
                            values,
                            DatabaseHelper.COLUMN_ID + " = ?",
                            new String[]{String.valueOf(subtaskId)});
                }
            }
        });

        subtaskContainer.addView(subtaskView);
    }



    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    private void setupListeners() {
        // Update title when changed
        taskTitle.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (!s.toString().equals(originalTitle)) {
                    hasChanges = true;
                    updateTaskInDb(DatabaseHelper.COLUMN_TITLE, s.toString());
                }
            }
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });

        // Due Date picker
        cardDueDate.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    TaskDetailActivity.this,
                    (view, year, month, dayOfMonth) -> {
                        String selectedDate = String.format(Locale.getDefault(), "%d-%02d-%02d",
                                year, month + 1, dayOfMonth);
                        dueDate.setText(selectedDate);
                        hasChanges = true;
                        updateTaskInDb(DatabaseHelper.COLUMN_DUE_DATE, selectedDate);
                    },
                    Calendar.getInstance().get(Calendar.YEAR),
                    Calendar.getInstance().get(Calendar.MONTH),
                    Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        });


        // Time picker
        cardTime.setOnClickListener(v -> {
            TimePickerDialog timePickerDialog = new TimePickerDialog(
                    TaskDetailActivity.this,
                    (view, hourOfDay, minute) -> {
                        String selectedTime = String.format(Locale.getDefault(), "%02d:%02d",
                                hourOfDay, minute);
                        time.setText(selectedTime);
                        hasChanges = true;
                        updateTaskInDb(DatabaseHelper.COLUMN_TIME, selectedTime);
                    },
                    Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
                    Calendar.getInstance().get(Calendar.MINUTE),
                    true
            );
            timePickerDialog.show();
        });

        btnAddSubtask.setOnClickListener(v -> {
            addNewSubtaskView();
            hasChanges = true;
        });
    }



    private void addNewSubtaskView() {
        View subtaskView = getLayoutInflater().inflate(R.layout.item_subtask, subtaskContainer, false);

        EditText subtaskInput = subtaskView.findViewById(R.id.subtask_input);
        ImageButton deleteSubtask = subtaskView.findViewById(R.id.btn_delete_subtask);

        deleteSubtask.setOnClickListener(v -> subtaskContainer.removeView(subtaskView));

        subtaskContainer.addView(subtaskView);

        subtaskInput.requestFocus();
    }
    // request permission untuk Android 13+
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        1);
            }
        }
    }

    private void updateTaskInDb(String column, String value) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(column, value);
        db.update(
                DatabaseHelper.TABLE_TASKS,
                values,
                DatabaseHelper.COLUMN_ID + " = ?",
                new String[]{String.valueOf(taskId)}
        );
    }

    //logic untuk menu dropdown detail task
    private void setupMoreOptions() {
        btnMore = findViewById(R.id.btn_more);

        loadStarredState();

        btnMore.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(TaskDetailActivity.this, btnMore);
            popup.getMenuInflater().inflate(R.menu.detail_task_menu, popup.getMenu());

            MenuItem starItem = popup.getMenu().findItem(R.id.action_star);
            starItem.setTitle(isStarred ? "Unstar Task" : "Star Task");

            popup.setOnMenuItemClickListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.action_star) {
                    toggleStarred();
                    return true;
                } else if (itemId == R.id.action_delete) {
                    deleteTask();
                    return true;
                }
                return false;
            });

            popup.show();
        });
    }

    @SuppressLint("Range")
    private void loadStarredState() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                DatabaseHelper.TABLE_TASKS,
                new String[]{DatabaseHelper.COLUMN_IS_STARRED},
                DatabaseHelper.COLUMN_ID + " = ?",
                new String[]{String.valueOf(taskId)},
                null, null, null
        );

        if (cursor != null && cursor.moveToFirst()) {
            isStarred = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_IS_STARRED)) == 1;
            cursor.close();
        }
    }

    private void toggleStarred() {
        isStarred = !isStarred;
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_IS_STARRED, isStarred ? 1 : 0);

        db.update(
                DatabaseHelper.TABLE_TASKS,
                values,
                DatabaseHelper.COLUMN_ID + " = ?",
                new String[]{String.valueOf(taskId)}
        );
    }

    private void deleteTask() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Task")
                .setMessage("Are you sure you want to delete this task?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    SQLiteDatabase db = dbHelper.getWritableDatabase();
                    db.delete(
                            DatabaseHelper.TABLE_TASKS,
                            DatabaseHelper.COLUMN_ID + " = ?",
                            new String[]{String.valueOf(taskId)}
                    );
                    finish(); // Close the activity after deletion
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void setupDateTimeEditing() {
        View cardDueDate = findViewById(R.id.card_due_date);
        View cardTime = findViewById(R.id.card_time);

        cardDueDate.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    this,
                    (view, year, month, dayOfMonth) -> {
                        String date = String.format(Locale.getDefault(), "%d-%02d-%02d",
                                year, month + 1, dayOfMonth);
                        dueDate.setText(date);
                        updateTaskDeadline(date, time.getText().toString());
                    },
                    Calendar.getInstance().get(Calendar.YEAR),
                    Calendar.getInstance().get(Calendar.MONTH),
                    Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        });

        cardTime.setOnClickListener(v -> {
            TimePickerDialog timePickerDialog = new TimePickerDialog(
                    this,
                    (view, hourOfDay, minute) -> {
                        String timeStr = String.format(Locale.getDefault(), "%02d:%02d",
                                hourOfDay, minute);
                        time.setText(timeStr);
                        updateTaskDeadline(dueDate.getText().toString(), timeStr);
                    },
                    Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
                    Calendar.getInstance().get(Calendar.MINUTE),
                    true
            );
            timePickerDialog.show();
        });
    }


    private void updateTaskDeadline(String date, String time) {
        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(DatabaseHelper.COLUMN_DUE_DATE, date);
            values.put(DatabaseHelper.COLUMN_TIME, time);

            int updated = db.update(DatabaseHelper.TABLE_TASKS, values,
                    DatabaseHelper.COLUMN_ID + " = ?",
                    new String[]{String.valueOf(taskId)});

            if (updated > 0) {
                // Cancel existing alarm
                Intent intent = new Intent(this, AlarmReceiver.class);
                intent.putExtra("task_title", taskTitle.getText().toString());

                AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                PendingIntent existingIntent = PendingIntent.getBroadcast(
                        this,
                        (int) taskId,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );
                alarmManager.cancel(existingIntent);

                // Set new alarm
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                    Date taskDate = sdf.parse(date + " " + time);
                    long timeInMillis = taskDate.getTime();

                    if (timeInMillis > System.currentTimeMillis()) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            alarmManager.setExactAndAllowWhileIdle(
                                    AlarmManager.RTC_WAKEUP,
                                    timeInMillis,
                                    existingIntent
                            );
                        }
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error updating task", Toast.LENGTH_SHORT).show();
        }
    }


}
