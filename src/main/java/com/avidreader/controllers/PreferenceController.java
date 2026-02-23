package com.avidreader.controllers;

import com.avidreader.dtos.PreferenceRequest;
import com.avidreader.services.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.Set;

@RestController
@RequestMapping("/api/preferences")
public class PreferenceController {

    private final UserService userService;

    public PreferenceController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<Set<String>> getPreferences(Authentication auth) {
        Set<String> prefs = userService.getPreferences(auth.getName());
        return ResponseEntity.ok(prefs);
    }

    @PutMapping
    public ResponseEntity<Set<String>> updatePreferences(
            @RequestBody PreferenceRequest request, Authentication auth) {
        Set<String> updated = userService.updatePreferences(
                auth.getName(), new HashSet<>(request.getTags()));
        return ResponseEntity.ok(updated);
    }
}
