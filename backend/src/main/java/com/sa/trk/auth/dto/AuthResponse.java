package com.sa.trk.auth.dto;

import java.time.Instant;

public record AuthResponse(String token, Instant expiresAt, AuthUserResponse user) {}
