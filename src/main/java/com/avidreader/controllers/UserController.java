package com.avidreader.controllers;

import com.avidreader.entity.User;
import com.avidreader.services.UserService;
import com.avidreader.dtos.UserRegistrationRequest;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/users") // Base mapping for all user related endpoints
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // 1. Creating User: POST /api/users
    @PostMapping("/signup")
    public ResponseEntity<User> createUser(@Valid @RequestBody UserRegistrationRequest request) {
        try {
            // Call the service layer to register the new user
            User newUser = userService.registerNewUser(
                    request.getUsername(),
                    request.getEmail(),
                    request.getPassword()
            );

            // Returns 201 Created status
            return new ResponseEntity<>(newUser, HttpStatus.CREATED);
        } catch (IllegalStateException e) {
            // Handle business logic errors (like user/email already exists)
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
    }

//    // 2. Getting User by ID: GET /api/users/{id}
//    @GetMapping("/{id}")
//    public ResponseEntity<User> getUserById(@PathVariable Long id) {
//        return userService.findById(id)
//                .map(ResponseEntity::ok) // If user is found, return 200 OK
//                .orElseThrow(() -> new ResponseStatusException(
//                        HttpStatus.NOT_FOUND,
//                        "User not found with ID: " + id
//                )); // If not found, return 404 Not Found
//    }

//    // 3. Getting All Users: GET /api/users
//    @GetMapping
//    public List<User> getAllUsers() {
//        return userService.findAllUsers();
//    }

    // 4. Deleting User by ID: DELETE /api/users/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUserById(@PathVariable Long id) {
        // Find the user first to ensure it exists before attempting to delete
        if (userService.findById(id).isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "User not found with ID: " + id
            );
        }

        userService.deleteUser(id);
        // Returns 204 No Content status on successful deletion
        return ResponseEntity.noContent().build();
    }
}
