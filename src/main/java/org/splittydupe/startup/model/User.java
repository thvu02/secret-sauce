package org.splittydupe.startup.model;

import com.google.cloud.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class User {
    private String uid;

    private String email;

    private String passwordHash;

    @Builder.Default
    private boolean emailVerified = false;

    @Builder.Default
    private boolean enabled = true;

    @Builder.Default
    private List<String> roles = new ArrayList<>();

    private Timestamp createdAt;

    private Timestamp lastLoginAt;

    private String resetToken;

    private Timestamp resetTokenExpiry;
}
