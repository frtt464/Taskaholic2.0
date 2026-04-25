package com.taskaholic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskTest {

    @Test
    void isVisibleToTaskerReturnsTrueOnlyForPublishedStatus() {
        Task published = new Task(1, "Deliver package", "City", 10.0, "alice", TaskStatus.PUBLISHED);
        Task draft = new Task(2, "Draft task", "City", 15.0, "bob", TaskStatus.DRAFT);

        assertTrue(published.isVisibleToTasker());
        assertFalse(draft.isVisibleToTasker());
    }

    @Test
    void toStringIncludesBookingInfoWhenBookedByIsSet() {
        Task task = new Task(3, "Fix sink", "Home", 40.5, "master", TaskStatus.ACCEPTED);
        task.setBookedBy("tasker1");

        String value = task.toString();

        assertTrue(value.contains("Booked by: tasker1"));
        assertTrue(value.contains("Fix sink"));
    }

    @Test
    void gettersAndSettersWorkAsExpected() {
        Task task = new Task();
        task.setId(99);
        task.setTitle("Paint wall");
        task.setLocation("Office");
        task.setPrice(22.75);
        task.setStatus(TaskStatus.PERFORMING);
        task.setCreatedBy("owner");
        task.setBookedBy("worker");

        assertEquals(99, task.getId());
        assertEquals("Paint wall", task.getTitle());
        assertEquals("Office", task.getLocation());
        assertEquals(22.75, task.getPrice(), 0.0001);
        assertEquals(TaskStatus.PERFORMING, task.getStatus());
        assertEquals("owner", task.getCreatedBy());
        assertEquals("worker", task.getBookedBy());
    }
}
