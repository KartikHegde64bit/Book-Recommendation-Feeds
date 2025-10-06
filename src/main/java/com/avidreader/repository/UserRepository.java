package com.avidreader.repository;

import com.avidreader.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Finds a User by their unique username.
     * Spring Data JPA automatically generates the query from the method name.
     */
    Optional<User> findByUsername(String username);

    /**
     * Finds a User by their unique email.
     */
    Optional<User> findByEmail(String email);
}