package com.taskaholic;

public class User {
    private String username;
    private String password;
    private Role role;

    public User() {}

    public User(String username, String password, String role) {
        this.username = username;
        this.password = password;
        this.role = "TASKMASTER".equalsIgnoreCase(role) ? Role.TASKMASTER : Role.TASKER;
    }

    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public Role getRole() { return role; }

    public void setUsername(String username) { this.username = username; }
    public void setPassword(String password) { this.password = password; }
    public void setRole(Role role) { this.role = role; }
}