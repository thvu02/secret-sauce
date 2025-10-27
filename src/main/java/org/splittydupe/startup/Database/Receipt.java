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
public class Receipt {
    private String uid;

    private String vendor;

    private String receiptDate;

    @Builder.Default
    private String currency = "USD";

    private double subtotal;

    private double tax;

    private double taxPercentage;

    private double tip;

    private double tipPercentage;

    private double total;

    private List<LineItem> lineItems;
}
