package com.taskaholic;

public class Task {
    private int id;
    private String title;
    private String location;
    private double price;
    private TaskStatus status;
    private String createdBy;
    private String bookedBy;

    public Task() {}

    public Task(int id, String title, String location, double price, String createdBy, TaskStatus status) {
        this.id = id;
        this.title = title;
        this.location = location;
        this.price = price;
        this.createdBy = createdBy;
        this.status = status;
    }

    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getLocation() { return location; }
    public double getPrice() { return price; }
    public TaskStatus getStatus() { return status; }
    public String getCreatedBy() { return createdBy; }
    public String getBookedBy() { return bookedBy; }

    public void setId(int id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setLocation(String location) { this.location = location; }
    public void setPrice(double price) { this.price = price; }
    public void setStatus(TaskStatus status) { this.status = status; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public void setBookedBy(String bookedBy) { this.bookedBy = bookedBy; }

    public boolean isVisibleToTasker() {
        return status == TaskStatus.PUBLISHED;
    }

    @Override
    public String toString() {
        return String.format("%d | %s | %s | $%.2f | %s | By: %s%s",
                id, title, location, price, status, createdBy,
                bookedBy == null ? "" : " | Booked by: " + bookedBy);
    }
}