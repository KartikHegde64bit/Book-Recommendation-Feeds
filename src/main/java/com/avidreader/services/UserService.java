package com.avidreader.services;

import com.avidreader.dtos.UserDTO;
import com.avidreader.entity.User;
import com.avidreader.repository.UserRepository;
import com.avidreader.security.JwtTokenService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;

    // Inject AuthenticationManager (expose a ProviderManager bean in SecurityConfig)
    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtTokenService jwtTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtTokenService = jwtTokenService;
    }

    /**
     * Registers a new user with a hashed password, enforcing unique username/email.
     */
    @Transactional
    public User registerNewUser(UserDTO userReq) {
        String userName = userReq.getUsername();
        String email = userReq.getEmail();

        if (userRepository.findByUsername(userName).isPresent()) {
            throw new IllegalStateException("Username '" + userName + "' is already taken.");
        }
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalStateException("Email '" + email + "' is already in use.");
        }

        User user = new User();
        user.setUsername(userName);
        user.setEmail(email);
        user.setPassword(userReq.getPassword(), passwordEncoder);

        return userRepository.save(user);

    }

    /**
     * Authenticates credentials and establishes session-backed authentication.
     * For stateless APIs, authenticate and return a token instead of mutating the session.
     *
     * @param username the username
     * @param rawPassword the password
     * @return the authenticated domain User
     * @throws IllegalStateException if authentication fails
     */
    // Assuming the method is in UserService and still uses AuthenticationManager
    // NOTE: We change the method signature, removing HttpServletRequest.
    @Transactional(readOnly = true)
    public User login(String username, String rawPassword) {

        // 1. Attempt to authenticate the user using the AuthenticationManager
        UsernamePasswordAuthenticationToken authRequest =
                new UsernamePasswordAuthenticationToken(username, rawPassword);

        // This line performs the actual authentication (checks password, loads user details).
        // If it fails, an AuthenticationException is thrown (e.g., BadCredentialsException).
        authenticationManager.authenticate(authRequest);

        // 2. If authentication succeeds, retrieve and return the domain User object.
        // NOTE: In a stateless API, this User object is often used to get the UserDetails
        // needed to generate the JWT claims (like ID, username, roles).
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found in repository."));
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
