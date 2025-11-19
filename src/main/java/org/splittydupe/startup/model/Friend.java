package org.splittydupe.startup.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Friend {
    private String id;

    private String userId; // ID of the user who owns this friend entry

    private String firstName;

    private String lastName;

    private String venmoHandle;

    private String zellePhoneNumber;
    
    private String paypalHandle;
}
