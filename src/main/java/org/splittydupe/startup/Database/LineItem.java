package org.splittydupe.startup.Database;

import java.util.List;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LineItem {
    private String name;

    private double price;

    @Builder.Default
    private int quantity = 1;

    private int numAssignees;
    
    private List<String> assignees;
}
