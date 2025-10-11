package com.avidreader.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;

/**
 * Maps to the 'user' table in the PostgreSQL database.
 */

@Entity
@Table(name = "app_user")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {

    // Maps to id PRIMARY KEY
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    // Maps to username
    @Column(name = "username", length = 50, nullable = false, unique = true)
    private String username;

    // Maps to email
    @Column(name = "email", length = 255, nullable = false, unique = true)
    private String email;

    // Maps to password_hash
    @Column(name = "password_hash", length = 255, nullable = false)
    private String passwordHash;

    // Maps to created_at
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // Maps to updated_at
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Hashes the raw password using the provided PasswordEncoder and sets it
     * to the passwordHash field.
     * * @param rawPassword The plain text password entered by the user.
     * @param passwordEncoder The Spring Security PasswordEncoder instance (e.g., BCryptPasswordEncoder).
     */
    public void setPassword(String rawPassword, PasswordEncoder passwordEncoder) {
        // Encode the raw password and store the hash
        this.passwordHash = passwordEncoder.encode(rawPassword);
    }

    /**
     * Checks if the raw password matches the stored password hash.
     * * @param rawPassword The plain text password entered by the user.
     * @param passwordEncoder The Spring Security PasswordEncoder instance.
     * @return true if the raw password matches the hash, false otherwise.
     */
    public boolean checkPassword(String rawPassword, PasswordEncoder passwordEncoder) {
        // Use the encoder's matches method to safely compare the raw password with the hash
        return passwordEncoder.matches(rawPassword, this.passwordHash);
    }
}