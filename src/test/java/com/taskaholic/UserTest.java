package com.taskaholic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserTest {

    @Test
    void constructorMapsTaskmasterRoleCaseInsensitively() {
        User user = new User("anna", "secret", "taskMaster");

        assertEquals(Role.TASKMASTER, user.getRole());
    }

    @Test
    void constructorDefaultsToTaskerForUnknownRole() {
        User user = new User("bob", "secret", "unknown");

        assertEquals(Role.TASKER, user.getRole());
    }

    @Test
    void settersUpdateFields() {
        User user = new User();
        user.setUsername("john");
        user.setPassword("pw");
        user.setRole(Role.TASKER);

        assertEquals("john", user.getUsername());
        assertEquals("pw", user.getPassword());
        assertEquals(Role.TASKER, user.getRole());
    }
}
