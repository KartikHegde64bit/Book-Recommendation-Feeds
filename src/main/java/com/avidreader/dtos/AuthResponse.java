package com.avidreader.dtos;

public record AuthResponse(
        String access_token,
        String token_type
) {}
