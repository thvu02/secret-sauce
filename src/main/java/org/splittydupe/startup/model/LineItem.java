package org.splittydupe.startup.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    // Split mode: "equal" or "percentage"
    @Builder.Default
    private String splitMode = "equal";

    private Map<String, Double> assigneePercentages;

    // Payment status for each assignee: "paid" or "unpaid"
    @Builder.Default
    private Map<String, String> assigneePaymentStatus = new HashMap<>();
}
