package com.taskaholic;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataStoreTest {

    private Path usersPath;
    private Path tasksPath;
    private Path usersBackupPath;
    private Path tasksBackupPath;

    @BeforeEach
    void setUp() throws IOException {
        Path projectDir = Path.of("").toAbsolutePath();
        usersPath = projectDir.resolve("users.txt");
        tasksPath = projectDir.resolve("tasks.txt");
        usersBackupPath = projectDir.resolve("users.txt.junit-backup");
        tasksBackupPath = projectDir.resolve("tasks.txt.junit-backup");

        Files.deleteIfExists(usersBackupPath);
        Files.deleteIfExists(tasksBackupPath);

        if (Files.exists(usersPath)) {
            Files.move(usersPath, usersBackupPath, StandardCopyOption.REPLACE_EXISTING);
        }
        if (Files.exists(tasksPath)) {
            Files.move(tasksPath, tasksBackupPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.deleteIfExists(usersPath);
        Files.deleteIfExists(tasksPath);

        if (Files.exists(usersBackupPath)) {
            Files.move(usersBackupPath, usersPath, StandardCopyOption.REPLACE_EXISTING);
        }
        if (Files.exists(tasksBackupPath)) {
            Files.move(tasksBackupPath, tasksPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @Test
    void loadUsersReturnsFallbackWhenFileMissing() {
        List<User> users = DataStore.loadUsers();

        assertTrue(users.isEmpty());
    }

    @Test
    void saveUsersAndLoadUsersRoundTrip() {
        List<User> source = List.of(new User("u1", "p1", "TASKMASTER"), new User("u2", "p2", "TASKER"));

        DataStore.saveUsers(source);
        List<User> reloaded = DataStore.loadUsers();

        assertEquals(2, reloaded.size());
        assertEquals("u1", reloaded.get(0).getUsername());
        assertEquals(Role.TASKMASTER, reloaded.get(0).getRole());
        assertEquals("u2", reloaded.get(1).getUsername());
        assertEquals(Role.TASKER, reloaded.get(1).getRole());
    }

    @Test
    void loadTasksReturnsFallbackWhenFileBlank() throws IOException {
        Files.writeString(tasksPath, "  \n\t  ");

        List<Task> tasks = DataStore.loadTasks();

        assertTrue(tasks.isEmpty());
    }

    @Test
    void saveTasksAndLoadTasksRoundTrip() {
        Task task = new Task(42, "Move boxes", "Warehouse", 55.0, "owner", TaskStatus.PUBLISHED);
        task.setBookedBy("worker");

        DataStore.saveTasks(List.of(task));
        List<Task> reloaded = DataStore.loadTasks();

        assertEquals(1, reloaded.size());
        Task loaded = reloaded.get(0);
        assertEquals(42, loaded.getId());
        assertEquals("Move boxes", loaded.getTitle());
        assertEquals("Warehouse", loaded.getLocation());
        assertEquals(55.0, loaded.getPrice(), 0.0001);
        assertEquals("owner", loaded.getCreatedBy());
        assertEquals(TaskStatus.PUBLISHED, loaded.getStatus());
        assertEquals("worker", loaded.getBookedBy());
    }
}
