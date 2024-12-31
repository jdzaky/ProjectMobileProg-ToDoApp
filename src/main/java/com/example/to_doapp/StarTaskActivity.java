package com.example.to_doapp;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;


import java.util.ArrayList;
import java.util.List;

public class StarTaskActivity extends AppCompatActivity {
    private RecyclerView starTasksList;
    private TaskAdapter taskAdapter;
    private DatabaseHelper dbHelper;
    private LinearLayout emptyState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.startask_activity);

        starTasksList = findViewById(R.id.star_tasks_list);
        emptyState = findViewById(R.id.empty_state);

        // Initialize database helper
        dbHelper = new DatabaseHelper(this);

        setupRecyclerView();

        setupBottomNavigation();

        loadStarredTasks();
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_tasks) {
                startActivity(new Intent(StarTaskActivity.this, TaskActivity.class));
                return true;
            } else if (itemId == R.id.nav_calendar) {
                startActivity(new Intent(StarTaskActivity.this, CalendarActivity.class));
                return true;
            }
            return itemId == R.id.nav_star;
        });

    }

    private void setupRecyclerView() {
        taskAdapter = new TaskAdapter(this);
        starTasksList.setLayoutManager(new LinearLayoutManager(this));
        starTasksList.setAdapter(taskAdapter);
    }

    @SuppressLint("Range")
    private void loadStarredTasks() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // Query to get only starred tasks
        String selection = DatabaseHelper.COLUMN_IS_STARRED + " = ?";
        String[] selectionArgs = {"1"};

        Cursor cursor = db.query(
                DatabaseHelper.TABLE_TASKS,
                null,
                selection,
                selectionArgs,
                null,
                null,
                null
        );

        List<Task> starredTasks = new ArrayList<>();

        if (cursor != null && cursor.moveToFirst()) {
            do {
                Task task = new Task();
                task.setId(cursor.getLong(cursor.getColumnIndex(DatabaseHelper.COLUMN_ID)));
                task.setTitle(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_TITLE)));
                task.setCompleted(cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_IS_COMPLETED)) == 1);
                task.setStarred(true); // Since we're only getting starred tasks
                starredTasks.add(task);
            } while (cursor.moveToNext());

            cursor.close();
        }

        // Update UI jika tidak ada tugas
        if (starredTasks.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            starTasksList.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            starTasksList.setVisibility(View.VISIBLE);
            taskAdapter.setTasks(starredTasks);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh list
        loadStarredTasks();
    }
}

