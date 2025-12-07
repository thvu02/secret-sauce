package org.splittydupe.startup.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfile {
    private String userId; // Links to User.uid

    private String displayName;

    private String venmoHandle;

    private String zellePhoneNumber;

    private String paypalHandle;
}
