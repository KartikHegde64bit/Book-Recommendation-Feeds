package com.avidreader.services;

import com.avidreader.entity.User;
import com.avidreader.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    // Inject AuthenticationManager (expose a ProviderManager bean in SecurityConfig)
    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
    }

    /**
     * Registers a new user with a hashed password, enforcing unique username/email.
     */
    @Transactional
    public User registerNewUser(String username, String email, String rawPassword) {

        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalStateException("Username '" + username + "' is already taken.");
        }
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalStateException("Email '" + email + "' is already in use.");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(rawPassword, passwordEncoder);

        return userRepository.save(user);
    }

    /**
     * Authenticates credentials and establishes session-backed authentication.
     * For stateless APIs, authenticate and return a token instead of mutating the session.
     *
     * @param username the username
     * @param rawPassword the password
     * @param request the current HttpServletRequest (to associate SecurityContext with HttpSession)
     * @return the authenticated domain User
     * @throws IllegalStateException if authentication fails
     */
    @Transactional(readOnly = true)
    public User login(String username, String rawPassword, HttpServletRequest request) {
        UsernamePasswordAuthenticationToken authRequest =
                new UsernamePasswordAuthenticationToken(username, rawPassword);

        Authentication authentication = authenticationManager.authenticate(authRequest);

        // On success, store Authentication in a new SecurityContext and bind it
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        // Ensure the context is associated with the HttpSession (for stateful session persistence)
        HttpSession session = request.getSession(true);
        // Optionally force session ID change after authentication (fixation protection is also handled by Spring Security)
        request.changeSessionId();

        // Return the domain user
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("User not found post-authentication"));
    }

    /**
     * Legacy credential check without establishing a session.
     * Useful if building a stateless login to issue JWTs.
     */
    @Transactional(readOnly = true)
    public Optional<User> authenticateUser(String username, String rawPassword) {
        Optional<User> userOptional = userRepository.findByUsername(username);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            if (user.checkPassword(rawPassword, passwordEncoder)) {
                return Optional.of(user);
            }
        }
        return Optional.empty();
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    @Transactional
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }
}
