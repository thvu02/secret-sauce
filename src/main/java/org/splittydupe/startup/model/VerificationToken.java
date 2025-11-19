package org.splittydupe.startup.model;

import com.google.cloud.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VerificationToken {
    private String uid;

    private String token;

    private String userEmail;

    private String userId;

    private Timestamp expiryDate;

    @Builder.Default
    private boolean used = false;

    // Token type: "email_verification" or "password_reset"
    @Builder.Default
    private String tokenType = "email_verification";

    private Timestamp createdAt;
}
