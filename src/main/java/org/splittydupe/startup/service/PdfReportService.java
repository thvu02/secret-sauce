package org.splittydupe.startup.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.splittydupe.startup.exception.PdfGenerationException;
import org.splittydupe.startup.model.LineItem;
import org.splittydupe.startup.model.Receipt;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;

@Slf4j
@Service
public class PdfReportService {

    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("$0.00");
    private static final float MARGIN = 50;
    private static final float FONT_SIZE_TITLE = 18;
    private static final float FONT_SIZE_HEADING = 14;
    private static final float FONT_SIZE_NORMAL = 12;
    private static final float FONT_SIZE_SMALL = 10;
    private static final float LINE_HEIGHT = 15;
    private static final float SECTION_SPACING = LINE_HEIGHT * 3;

    public byte[] generateReceiptReport(Receipt receipt) {
        log.info("Generating PDF report for receipt UID: {}", receipt.getUid());

        try (PDDocument document = new PDDocument(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            PDPageContentStream contentStream = new PDPageContentStream(document, page);
            float yPosition = page.getMediaBox().getHeight() - MARGIN;

            yPosition = addTitle(contentStream, yPosition, "Receipt Expense Report");
            yPosition -= LINE_HEIGHT * 2;

            yPosition = addReceiptDetails(contentStream, yPosition, receipt);
            yPosition -= SECTION_SPACING;

            PageContext context = new PageContext(contentStream, yPosition);
            addPersonBreakdown(document, context, receipt);

            if (context.contentStream != null) {
                context.contentStream.close();
            }

            document.save(baos);
            log.info("PDF report generated successfully for receipt UID: {}", receipt.getUid());
            return baos.toByteArray();
        } catch (IOException e) {
            log.error("Failed to generate PDF report for receipt UID: {}", receipt.getUid(), e);
            throw new PdfGenerationException("Failed to generate PDF report for receipt: " + receipt.getUid(), e);
        }
    }

    private float addTitle(PDPageContentStream contentStream, float yPosition, String title) throws IOException {
        contentStream.setFont(PDType1Font.HELVETICA_BOLD, FONT_SIZE_TITLE);
        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN, yPosition);
        contentStream.showText(title);
        contentStream.endText();
        return yPosition - LINE_HEIGHT * 2;
    }

    private float addReceiptDetails(PDPageContentStream contentStream, float yPosition, Receipt receipt) throws IOException {
        contentStream.setFont(PDType1Font.HELVETICA_BOLD, FONT_SIZE_HEADING);
        yPosition = addLine(contentStream, yPosition, "Receipt Details");
        yPosition -= LINE_HEIGHT;

        contentStream.setFont(PDType1Font.HELVETICA, FONT_SIZE_NORMAL);
        yPosition = addLine(contentStream, yPosition, "Vendor: " + (receipt.getVendor() != null ? receipt.getVendor() : "N/A"));
        yPosition = addLine(contentStream, yPosition, "Date: " + (receipt.getReceiptDate() != null ? receipt.getReceiptDate() : "N/A"));
        yPosition = addLine(contentStream, yPosition, "Currency: " + (receipt.getCurrency() != null ? receipt.getCurrency() : "USD"));
        yPosition -= LINE_HEIGHT / 2;

        yPosition = addLine(contentStream, yPosition, "Subtotal: " + MONEY_FORMAT.format(receipt.getSubtotal()));
        yPosition = addLine(contentStream, yPosition, "Tax: " + MONEY_FORMAT.format(receipt.getTax()) +
                " (" + String.format("%.2f", receipt.getTaxPercentage()) + "%)");
        yPosition = addLine(contentStream, yPosition, "Tip: " + MONEY_FORMAT.format(receipt.getTip()) +
                " (" + String.format("%.2f", receipt.getTipPercentage()) + "%)");

        contentStream.setFont(PDType1Font.HELVETICA_BOLD, FONT_SIZE_NORMAL);
        yPosition = addLine(contentStream, yPosition, "Total: " + MONEY_FORMAT.format(receipt.getTotal()));

        return yPosition;
    }

    private void addPersonBreakdown(PDDocument document, PageContext context, Receipt receipt) throws IOException {
        if (context.yPosition < MARGIN + 150) {
            createNewPage(document, context);
        }

        context.contentStream.setFont(PDType1Font.HELVETICA_BOLD, FONT_SIZE_HEADING);
        context.yPosition = addLine(context.contentStream, context.yPosition, "Person-by-Person Breakdown");
        context.yPosition -= LINE_HEIGHT;

        Map<String, PersonExpense> personExpenses = calculatePersonExpenses(receipt);

        int totalAssignees = personExpenses.size();

        for (Map.Entry<String, PersonExpense> entry : personExpenses.entrySet()) {
            String person = entry.getKey();
            PersonExpense expense = entry.getValue();

            float spaceNeeded = LINE_HEIGHT * (expense.lineItems.size() + 6);

            if (context.yPosition < MARGIN + spaceNeeded) {
                createNewPage(document, context);
            }

            context.yPosition = addPersonSection(context.contentStream, context.yPosition, person, expense, receipt, totalAssignees);
        }
    }

    private void createNewPage(PDDocument document, PageContext context) throws IOException {
        if (context.contentStream != null) {
            context.contentStream.close();
        }

        PDPage newPage = new PDPage(PDRectangle.A4);
        document.addPage(newPage);
        context.contentStream = new PDPageContentStream(document, newPage);
        context.yPosition = newPage.getMediaBox().getHeight() - MARGIN;
    }

    private float addPersonSection(PDPageContentStream contentStream, float yPosition, String person, PersonExpense expense, Receipt receipt, int totalAssignees) throws IOException {
        contentStream.setFont(PDType1Font.HELVETICA_BOLD, FONT_SIZE_NORMAL);
        yPosition = addLine(contentStream, yPosition, person);
        yPosition -= LINE_HEIGHT / 2;

        contentStream.setFont(PDType1Font.HELVETICA, FONT_SIZE_SMALL);

        for (LineItemDetail detail : expense.lineItems) {
            // Format quantity: "2 pad thais" or just "pad thai" if quantity is 1
            String itemDescription = detail.quantity > 1
                    ? detail.quantity + " " + detail.itemName
                    : detail.itemName;

            String itemText = "  - " + itemDescription + " (" +
                    (detail.splitMode.equals("percentage")
                            ? detail.percentage + "%"
                            : "Equal split") +
                    "): " + MONEY_FORMAT.format(detail.amount);
            yPosition = addLine(contentStream, yPosition, itemText);
        }

        yPosition -= LINE_HEIGHT / 2;

        contentStream.setFont(PDType1Font.HELVETICA, FONT_SIZE_NORMAL);
        yPosition = addLine(contentStream, yPosition, "  Items Subtotal: " + MONEY_FORMAT.format(expense.itemsSubtotal));

        String distributionMode = receipt.getTaxTipDistribution() != null ? receipt.getTaxTipDistribution() : "proportional";
        double personTax;
        double personTip;
        String taxLabel;
        String tipLabel;

        if ("even".equals(distributionMode)) {
            personTax = totalAssignees > 0 ? receipt.getTax() / totalAssignees : 0;
            personTip = totalAssignees > 0 ? receipt.getTip() / totalAssignees : 0;
            taxLabel = "  Even Share Tax";
            tipLabel = "  Even Share Tip";
        } else {
            personTax = receipt.getSubtotal() > 0 ? (expense.itemsSubtotal / receipt.getSubtotal()) * receipt.getTax() : 0;
            personTip = receipt.getSubtotal() > 0 ? (expense.itemsSubtotal / receipt.getSubtotal()) * receipt.getTip() : 0;
            taxLabel = "  Proportional Tax";
            tipLabel = "  Proportional Tip";
        }

        contentStream.setFont(PDType1Font.HELVETICA, FONT_SIZE_SMALL);
        yPosition = addLine(contentStream, yPosition, taxLabel + ": " + MONEY_FORMAT.format(personTax));
        yPosition = addLine(contentStream, yPosition, tipLabel + ": " + MONEY_FORMAT.format(personTip));

        double personTotal = expense.itemsSubtotal + personTax + personTip;
        contentStream.setFont(PDType1Font.HELVETICA_BOLD, FONT_SIZE_NORMAL);
        yPosition = addLine(contentStream, yPosition, "  Total: " + MONEY_FORMAT.format(personTotal));
        yPosition -= LINE_HEIGHT * 1.5;

        return yPosition;
    }

    private float addLine(PDPageContentStream contentStream, float yPosition, String text) throws IOException {
        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN, yPosition);
        contentStream.showText(text);
        contentStream.endText();
        return yPosition - LINE_HEIGHT;
    }

    private Map<String, PersonExpense> calculatePersonExpenses(Receipt receipt) {
        Map<String, PersonExpense> expenses = new HashMap<>();

        for (LineItem item : receipt.getLineItems()) {
            List<String> assignees = item.getAssignees();
            if (assignees == null || assignees.isEmpty()) continue;

            String splitMode = item.getSplitMode() != null ? item.getSplitMode() : "equal";
            double itemTotal = item.getPrice() * item.getQuantity();

            for (String assignee : assignees) {
                PersonExpense expense = expenses.computeIfAbsent(assignee, k -> new PersonExpense());

                double assigneeAmount;
                int percentage = 0;

                if ("percentage".equals(splitMode) && item.getAssigneePercentages() != null) {
                    Double pct = item.getAssigneePercentages().get(assignee);
                    percentage = pct != null ? pct.intValue() : 0;
                    assigneeAmount = (itemTotal * percentage) / 100.0;
                } else {
                    assigneeAmount = itemTotal / assignees.size();
                }

                expense.lineItems.add(new LineItemDetail(
                        item.getName(),
                        assigneeAmount,
                        splitMode,
                        percentage,
                        item.getQuantity()
                ));
                expense.itemsSubtotal += assigneeAmount;
            }
        }

        return expenses;
    }

    private static class PageContext {
        PDPageContentStream contentStream;
        float yPosition;

        PageContext(PDPageContentStream contentStream, float yPosition) {
            this.contentStream = contentStream;
            this.yPosition = yPosition;
        }
    }

    private static class PersonExpense {
        List<LineItemDetail> lineItems = new ArrayList<>();
        double itemsSubtotal = 0.0;
    }

    private static class LineItemDetail {
        String itemName;
        double amount;
        String splitMode;
        int percentage;
        int quantity;

        LineItemDetail(String itemName, double amount, String splitMode, int percentage, int quantity) {
            this.itemName = itemName;
            this.amount = amount;
            this.splitMode = splitMode;
            this.percentage = percentage;
            this.quantity = quantity;
        }
    }
}
