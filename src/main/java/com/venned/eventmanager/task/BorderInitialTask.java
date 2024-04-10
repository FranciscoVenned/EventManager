package com.venned.eventmanager.task;

import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;

public class BorderInitialTask {
    private static final Map<String, BukkitTask> tasks = new HashMap<>();

    public static void addTask(String taskName, BukkitTask task) {
        tasks.put(taskName, task);
    }

    public static BukkitTask getTask(String taskName) {
        return tasks.get(taskName);
    }

    public static void removeTask(String taskName) {
        tasks.remove(taskName);
    }

    public static void cancelTask(String taskName) {
        BukkitTask task = tasks.get(taskName);
        if (task != null) {
            task.cancel();
            tasks.remove(taskName);
        }
    }
}
