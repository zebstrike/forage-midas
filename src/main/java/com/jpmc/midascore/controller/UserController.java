package com.jpmc.midascore.controller;

import com.jpmc.midascore.entity.UserRecord;
import com.jpmc.midascore.services.UserService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public Iterable<UserRecord> getUsers() {
        return userService.getAllUsers();
    }

    @GetMapping("/{id}")
    public UserRecord getUser(@PathVariable long id) {
        return userService.getUserById(id);
    }
}
