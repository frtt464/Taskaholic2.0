package com.taskaholic;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;


import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class DataStore {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final String USERS_FILE = "users.txt";
    private static final String TASKS_FILE = "tasks.txt";

    private static final Type USER_LIST = new TypeToken<List<User>>() {}.getType();
    private static final Type TASK_LIST = new TypeToken<List<Task>>() {}.getType();

    public static List<User> loadUsers() {
        return readList(USERS_FILE, USER_LIST, new ArrayList<>());
    }

    public static List<Task> loadTasks() {
        return readList(TASKS_FILE, TASK_LIST, new ArrayList<>());
    }

    public static void saveUsers(List<User> users) {
        writeFile(USERS_FILE, gson.toJson(users));
    }

    public static void saveTasks(List<Task> tasks) {
        writeFile(TASKS_FILE, gson.toJson(tasks));
    }

    private static <T> T readList(String fileName, Type type, T fallback) {
        try {
            Path path = Path.of(fileName);
            if (!Files.exists(path)) return fallback;
            String content = Files.readString(path);
            if (content.isBlank()) return fallback;
            T data = gson.fromJson(content, type);
            return data != null ? data : fallback;
        } catch (Exception e) {
            return fallback;
        }
    }

    private static void writeFile(String fileName, String content) {
        try (FileWriter writer = new FileWriter(fileName)) {
            writer.write(content);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
