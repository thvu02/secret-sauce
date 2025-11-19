package org.splittydupe.startup.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ErrorResponse {
    private String error;
    private String message;

    // Legacy constructor for backward compatibility
    public ErrorResponse(String error) {
        this.error = error;
        this.message = error;
    }
}
