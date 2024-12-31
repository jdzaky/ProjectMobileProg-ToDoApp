package com.example.to_doapp;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {
    private List<Task> tasks;
    private Context context;
    private DatabaseHelper dbHelper;

    public TaskAdapter(Context context) {
        this.context = context;
        this.tasks = new ArrayList<>();
        this.dbHelper = new DatabaseHelper(context);
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.list_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = tasks.get(position);
        try {
            holder.taskTitle.setText(task.getTitle());
            holder.taskCheckbox.setChecked(task.isCompleted());

            holder.itemView.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(context, TaskDetailActivity.class);
                    intent.putExtra("task_id", task.getId());
                    context.startActivity(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(context, "Error opening task details", Toast.LENGTH_SHORT).show();
                }
            });
            if (task.isStarred()) {
                holder.btnStar.setImageResource(R.drawable.star_filled); //filled
            } else {
                holder.btnStar.setImageResource(R.drawable.ic_star);
            }

            holder.btnStar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    task.setStarred(!task.isStarred());
                    if (task.isStarred()) {
                        holder.btnStar.setImageResource(R.drawable.star_filled); //filled
                    } else {
                        holder.btnStar.setImageResource(R.drawable.ic_star);
                    }
                    dbHelper.updateTaskStarred(task.getId(), task.isStarred());
                }
            });

            holder.taskCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                task.setCompleted(isChecked);
                dbHelper.updateTaskStatus(task.getId(), isChecked);
            });

            holder.btnDelete.setOnClickListener(v -> {
                dbHelper.deleteTask(task.getId());
                tasks.remove(position);
                notifyItemRemoved(position);
                notifyItemRangeChanged(position, tasks.size());
            });
        }catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "Error binding task data", Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    public void setTasks(List<Task> taskList) {
        this.tasks = taskList;
        notifyDataSetChanged();
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        CheckBox taskCheckbox;
        TextView taskTitle;
        ImageButton btnStar, btnDelete;

        TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            taskCheckbox = itemView.findViewById(R.id.task_checkbox);
            taskTitle = itemView.findViewById(R.id.task_title);
            btnStar = itemView.findViewById(R.id.btn_star);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }

    }
}

