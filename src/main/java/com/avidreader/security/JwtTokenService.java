package com.avidreader.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Collections; // Import for the new method

@Service
public class JwtTokenService {
    private final Algorithm alg;

    public JwtTokenService() {
        String secret = System.getenv("JWT_SECRET");
        if (secret == null || secret.length() < 64) {
            throw new IllegalStateException("JWT_SECRET not set or too short for HMAC512");
        }
        this.alg = Algorithm.HMAC512(secret);
    }

    // --- Existing method: Generates token from a full UserDetails object ---
    public String generate(UserDetails principal) {
        Instant now = Instant.now();
        return JWT.create()
                .withSubject(principal.getUsername())
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(now.plusSeconds(3600))) // 1 hour expiration
                .withClaim("roles", principal.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList())
                .sign(alg);
    }

    // --- New method: Generates token using only the username (suitable for signup) ---
    /**
     * Generates a token for a new user, assuming a default set of authorities (e.g., "ROLE_USER").
     * @param username The subject of the token.
     * @return The signed JWT string.
     */
    public String generateToken(String username) {
        Instant now = Instant.now();
        // Assuming a new user is always given the "ROLE_USER" authority
        List<String> defaultRoles = Collections.singletonList("ROLE_USER");

        return JWT.create()
                .withSubject(username)
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(now.plusSeconds(3600))) // 1 hour expiration
                .withClaim("roles", defaultRoles)
                .sign(alg);
    }
    // --------------------------------------------------------------------------

    public Authentication parseAndBuildAuthentication(String token) {
        JWTVerifier verifier = JWT.require(alg)
                // .withIssuer("avidreader") // optional hardening
                .build();
        DecodedJWT jwt = verifier.verify(token);
        String username = jwt.getSubject();
        List<SimpleGrantedAuthority> auths =
                jwt.getClaim("roles").asList(String.class).stream().map(SimpleGrantedAuthority::new).toList();
        return new UsernamePasswordAuthenticationToken(username, null, auths);
    }
}