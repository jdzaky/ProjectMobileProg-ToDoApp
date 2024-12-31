package com.example.to_doapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

// DatabaseHelper.java
public class DatabaseHelper extends SQLiteOpenHelper {
    public static final String COLUMN_TIME = "time";
    private static final String DATABASE_NAME = "TaskManager.db";
    private static final int DATABASE_VERSION = 2;
    public static final String TABLE_SUBTASKS = "subtasks";
    public static final String COLUMN_PARENT_TASK_ID = "parent_task_id";
    public static final String COLUMN_SUBTASK_TITLE = "subtask_title";

    // Table names
    public static final String TABLE_TASKS = "tasks";
    public static final String TABLE_CATEGORIES = "categories";

    // Common column names
    public static final String COLUMN_ID = "id";

    // Tasks table columns
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_DUE_DATE = "due_date";
    public static final String COLUMN_CATEGORY_ID = "category_id";
    public static final String COLUMN_IS_COMPLETED = "is_completed";
    public static final String COLUMN_IS_STARRED = "is_starred";

    // Categories table columns
    public static final String COLUMN_CATEGORY_NAME = "name";

    // Create table queries
    private static final String CREATE_TABLE_TASKS =
            "CREATE TABLE " + TABLE_TASKS + "("
                    + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + COLUMN_TITLE + " TEXT NOT NULL, "
                    + COLUMN_DUE_DATE + " TEXT, "
                    + COLUMN_CATEGORY_ID + " INTEGER, "
                    + COLUMN_IS_COMPLETED + " INTEGER DEFAULT 0, "
                    + COLUMN_IS_STARRED + " INTEGER DEFAULT 0, "
                    + COLUMN_TIME + " TEXT, "
                    + "FOREIGN KEY(" + COLUMN_CATEGORY_ID + ") REFERENCES "
                    + TABLE_CATEGORIES + "(" + COLUMN_ID + "))";

    private static final String CREATE_TABLE_CATEGORIES =
            "CREATE TABLE " + TABLE_CATEGORIES + "("
                    + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + COLUMN_CATEGORY_NAME + " TEXT NOT NULL)";

    // Query pembuatan tabel subtasks
    private static final String CREATE_TABLE_SUBTASKS =
            "CREATE TABLE " + TABLE_SUBTASKS + "("
                    + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + COLUMN_SUBTASK_TITLE + " TEXT NOT NULL, "
                    + COLUMN_PARENT_TASK_ID + " INTEGER, "
                    + "FOREIGN KEY(" + COLUMN_PARENT_TASK_ID + ") REFERENCES "
                    + TABLE_TASKS + "(" + COLUMN_ID + "))";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_CATEGORIES);
        db.execSQL(CREATE_TABLE_TASKS);
        db.execSQL(CREATE_TABLE_SUBTASKS);

        // Insert default categories
        insertDefaultCategories(db);
    }

    private void insertDefaultCategories(SQLiteDatabase db) {
        String[] defaultCategories = {"Work", "Personal", "Wishlist"};
        for (String category : defaultCategories) {
            ContentValues values = new ContentValues();
            values.put(COLUMN_CATEGORY_NAME, category);
            db.insert(TABLE_CATEGORIES, null, values);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TASKS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CATEGORIES);
        onCreate(db);
    }

    public void updateTaskStarred(long taskId, boolean starred) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_IS_STARRED, starred ? 1 : 0);
        db.update(TABLE_TASKS, values, COLUMN_ID + " = ?",
                new String[]{String.valueOf(taskId)});
    }

    public void updateTaskStatus(long taskId, boolean isCompleted) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_IS_COMPLETED, isCompleted ? 1 : 0);
        db.update(TABLE_TASKS, values, COLUMN_ID + " = ?",
                new String[]{String.valueOf(taskId)});
    }

    public void deleteTask(long taskId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_TASKS, COLUMN_ID + " = ?",
                new String[]{String.valueOf(taskId)});
    }
}

