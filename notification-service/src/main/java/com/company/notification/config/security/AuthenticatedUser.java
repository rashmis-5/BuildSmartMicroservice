package com.company.notification.config.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.Serializable;

/**
 * Lightweight principal extracted from the JWT.
 * Stored inside Spring Security's Authentication object.
 */
@Getter
@RequiredArgsConstructor
public class AuthenticatedUser implements Serializable {
    private final Long userId;
    private final String role;
    private final Long departmentId;
}
