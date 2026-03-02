package com.avidreader.controllers;

import com.avidreader.dtos.AuthResponse;
import com.avidreader.dtos.LoginRequest;
import com.avidreader.dtos.UserDTO;
import com.avidreader.entity.User;
import com.avidreader.security.JwtTokenService;
import com.avidreader.services.UserService;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users") // Base mapping for all user related endpoints
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @RestController
    @RequestMapping("/api/auth")
    public class AuthController {
        private final UserService userService;
        private final JwtTokenService jwtTokenService;

        public AuthController(UserService users, JwtTokenService jwtTokenService) {
            this.userService = users;
            this.jwtTokenService = jwtTokenService;
        }

        @PostMapping("/signup")
        public ResponseEntity<Map<String, String>> signup(@Valid @RequestBody UserDTO req) {
            try {
                User user = userService.registerNewUser(req);
                return ResponseEntity.status(HttpStatus.CREATED)
                        .body(Map.of("message", "created successfully"));
            }catch(Exception e){
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Something went wrong"));
            }

        }

        @PostMapping("/login")
        public ResponseEntity<Map<String, String>> login(@Valid @RequestBody LoginRequest req) {
            User user = userService.login(req.getUsername(), req.getPassword());
            if(user != null){
                String token = jwtTokenService.generateToken(user.getUsername());
                return ResponseEntity.ok(Map.of(
                        "access_token", token,
                        "token_type", "Bearer"
                ));

            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid username or password"));
        }
    }
}
