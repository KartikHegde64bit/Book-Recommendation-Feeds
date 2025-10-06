package com.avidreader.services;

import com.avidreader.entity.User;
import com.avidreader.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // Dependency Injection via Constructor
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Registers a new user, handles password hashing, and checks for existing username/email.
     *
     * @param username The user's chosen username.
     * @param email The user's email.
     * @param rawPassword The plain text password.
     * @return The newly created User entity.
     * @throws IllegalStateException if the username or email already exists.
     */
    @Transactional
    public User registerNewUser(String username, String email, String rawPassword) {

        // 1. Basic Existence Check (Business Logic)
        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalStateException("Username '" + username + "' is already taken.");
        }
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalStateException("Email '" + email + "' is already in use.");
        }

        // 2. Create and Populate Entity
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);

        // 3. Hash Password using the entity's method
        user.setPassword(rawPassword, passwordEncoder);

        // 4. Save to Database
        return userRepository.save(user);
    }

    /**
     * Authenticates a user based on their username and password.
     * * @param username The username to check.
     * @param rawPassword The plain text password to verify.
     * @return An Optional containing the User if credentials are valid, or empty otherwise.
     */
    public Optional<User> authenticateUser(String username, String rawPassword) {

        Optional<User> userOptional = userRepository.findByUsername(username);

        if (userOptional.isPresent()) {
            User user = userOptional.get();

            // Use the entity's checkPassword method
            if (user.checkPassword(rawPassword, passwordEncoder)) {
                return Optional.of(user); // Authentication successful
            }
        }

        return Optional.empty(); // User not found or password incorrect
    }

    /**
     * Retrieves a user by their ID.
     */
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    /**
     * Retrieves all users from the database.
     */
    public List<User> findAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Deletes a user by their ID.
     */
    @Transactional
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }
}