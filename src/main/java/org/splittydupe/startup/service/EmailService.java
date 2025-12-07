package org.splittydupe.startup.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.splittydupe.startup.exception.EmailException;
import org.splittydupe.startup.model.LineItem;
import org.splittydupe.startup.model.Receipt;
import org.splittydupe.startup.model.UserProfile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.base-url:http://localhost:5173}")
    private String baseUrl;

    public void sendVerificationEmail(String toEmail, String token) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("SplittyDupe - Verify Your Email Address");

            String verificationLink = baseUrl + "/verify-email?token=" + token;
            String emailBody = String.format(
                    "Welcome to SplittyDupe!\n\n" +
                            "Please verify your email address by clicking the link below:\n\n" +
                            "%s\n\n" +
                            "This link will expire in 24 hours.\n\n" +
                            "If you did not create an account, please ignore this email.\n\n" +
                            "Best regards,\n" +
                            "The SplittyDupe Team",
                    verificationLink
            );

            message.setText(emailBody);
            mailSender.send(message);
            log.info("Verification email sent successfully to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send verification email to: {}", toEmail, e);
            throw new EmailException("Failed to send verification email to: " + toEmail, e);
        }
    }

    public void sendPasswordResetEmail(String toEmail, String token) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("SplittyDupe - Password Reset Request");

            String resetLink = baseUrl + "/reset-password?token=" + token;
            String emailBody = String.format(
                    "Hello,\n\n" +
                            "We received a request to reset your password for your SplittyDupe account.\n\n" +
                            "Click the link below to reset your password:\n\n" +
                            "%s\n\n" +
                            "This link will expire in 1 hour.\n\n" +
                            "If you did not request a password reset, please ignore this email or contact support if you have concerns.\n\n" +
                            "Best regards,\n" +
                            "The SplittyDupe Team",
                    resetLink
            );

            message.setText(emailBody);
            mailSender.send(message);
            log.info("Password reset email sent successfully to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send password reset email to: {}", toEmail, e);
            throw new EmailException("Failed to send password reset email to: " + toEmail, e);
        }
    }

    public void sendWelcomeEmail(String toEmail, String userName) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Welcome to SplittyDupe!");

            String emailBody = String.format(
                    "Hello %s,\n\n" +
                            "Your email has been verified successfully!\n\n" +
                            "You can now enjoy all features of SplittyDupe:\n" +
                            "- Upload and parse receipts with OCR\n" +
                            "- Split expenses with friends\n" +
                            "- Track payment status\n" +
                            "- Generate PDF reports\n" +
                            "- Access your receipt history from any device\n\n" +
                            "Get started by logging in at: %s\n\n" +
                            "Best regards,\n" +
                            "The SplittyDupe Team",
                    userName,
                    baseUrl
            );

            message.setText(emailBody);
            mailSender.send(message);
            log.info("Welcome email sent successfully to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send welcome email to: {}", toEmail, e);
        }
    }

    public void sendPaymentReminderEmail(String toEmail, String assigneeName, Receipt receipt,
                                         UserProfile senderProfile, byte[] pdfReport) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String senderName = getProfileDisplayName(senderProfile);

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Payment Reminder from " + senderName + " - " + receipt.getVendor());

            String emailBody = buildPaymentReminderEmailBody(assigneeName, receipt, senderProfile);
            helper.setText(emailBody, true);

            if (pdfReport != null && pdfReport.length > 0) {
                helper.addAttachment("receipt-report.pdf", new ByteArrayResource(pdfReport));
            }

            mailSender.send(message);
            log.info("Payment reminder email sent successfully to: {} for assignee: {}", toEmail, assigneeName);
        } catch (MessagingException e) {
            log.error("Failed to send payment reminder email to: {}", toEmail, e);
            throw new EmailException("Failed to send payment reminder email to: " + toEmail, e);
        }
    }

    private String buildPaymentReminderEmailBody(String assigneeName, Receipt receipt, UserProfile senderProfile) {
        List<LineItemBreakdown> assigneeItems = calculateAssigneeBreakdown(assigneeName, receipt);
        double assigneeSubtotal = assigneeItems.stream()
                .mapToDouble(item -> item.amount)
                .sum();

        String taxTipMode = receipt.getTaxTipDistribution() != null ? receipt.getTaxTipDistribution() : "proportional";
        double assigneeTax;
        double assigneeTip;

        if ("even".equals(taxTipMode)) {
            long totalAssignees = receipt.getLineItems().stream()
                    .flatMap(li -> li.getAssignees() != null ? li.getAssignees().stream() : null)
                    .distinct()
                    .count();
            assigneeTax = totalAssignees > 0 ? receipt.getTax() / totalAssignees : 0;
            assigneeTip = totalAssignees > 0 ? receipt.getTip() / totalAssignees : 0;
        } else {
            double subtotalRatio = receipt.getSubtotal() > 0 ? assigneeSubtotal / receipt.getSubtotal() : 0;
            assigneeTax = subtotalRatio * receipt.getTax();
            assigneeTip = subtotalRatio * receipt.getTip();
        }

        double assigneeTotal = assigneeSubtotal + assigneeTax + assigneeTip;

        StringBuilder html = new StringBuilder();
        html.append("<html><body style='font-family: Arial, sans-serif; color: #333;'>");
        html.append("<h2 style='color: #2563eb;'>Payment Reminder</h2>");
        html.append("<p>Hi ").append(assigneeName).append(",</p>");
        html.append("<p>This is a friendly reminder about your share of the receipt from <strong>").append(receipt.getVendor()).append("</strong>.</p>");

        html.append("<h3 style='color: #374151; border-bottom: 2px solid #e5e7eb; padding-bottom: 8px;'>Your Items</h3>");
        html.append("<table style='width: 100%; border-collapse: collapse; margin-bottom: 20px;'>");
        html.append("<thead><tr style='background-color: #f3f4f6;'>");
        html.append("<th style='padding: 10px; text-align: left; border-bottom: 2px solid #d1d5db;'>Item</th>");
        html.append("<th style='padding: 10px; text-align: right; border-bottom: 2px solid #d1d5db;'>Amount</th>");
        html.append("</tr></thead><tbody>");

        for (LineItemBreakdown item : assigneeItems) {
            html.append("<tr>");
            html.append("<td style='padding: 8px; border-bottom: 1px solid #e5e7eb;'>").append(item.name).append("</td>");
            html.append("<td style='padding: 8px; text-align: right; border-bottom: 1px solid #e5e7eb;'>$").append(String.format("%.2f", item.amount)).append("</td>");
            html.append("</tr>");
        }

        html.append("</tbody></table>");

        html.append("<table style='width: 100%; max-width: 400px; margin-left: auto; margin-bottom: 20px;'>");
        html.append("<tr><td style='padding: 5px; text-align: right;'><strong>Subtotal:</strong></td><td style='padding: 5px; text-align: right;'>$").append(String.format("%.2f", assigneeSubtotal)).append("</td></tr>");
        html.append("<tr><td style='padding: 5px; text-align: right;'><strong>Tax (").append(taxTipMode).append("):</strong></td><td style='padding: 5px; text-align: right;'>$").append(String.format("%.2f", assigneeTax)).append("</td></tr>");
        html.append("<tr><td style='padding: 5px; text-align: right;'><strong>Tip (").append(taxTipMode).append("):</strong></td><td style='padding: 5px; text-align: right;'>$").append(String.format("%.2f", assigneeTip)).append("</td></tr>");
        html.append("<tr style='border-top: 2px solid #2563eb;'><td style='padding: 8px; text-align: right; font-size: 18px; color: #2563eb;'><strong>Total Due:</strong></td><td style='padding: 8px; text-align: right; font-size: 18px; color: #2563eb;'><strong>$").append(String.format("%.2f", assigneeTotal)).append("</strong></td></tr>");
        html.append("</table>");

        html.append("<h3 style='color: #374151; border-bottom: 2px solid #e5e7eb; padding-bottom: 8px;'>Payment Information</h3>");
        html.append("<p>Please send payment to <strong>").append(getProfileDisplayName(senderProfile)).append("</strong> using one of the following methods:</p>");
        html.append("<ul style='list-style-type: none; padding-left: 0;'>");

        if (senderProfile.getVenmoHandle() != null && !senderProfile.getVenmoHandle().isEmpty()) {
            html.append("<li style='padding: 5px 0;'><strong>Venmo:</strong> ").append(senderProfile.getVenmoHandle()).append("</li>");
        }
        if (senderProfile.getZellePhoneNumber() != null && !senderProfile.getZellePhoneNumber().isEmpty()) {
            html.append("<li style='padding: 5px 0;'><strong>Zelle:</strong> ").append(senderProfile.getZellePhoneNumber()).append("</li>");
        }
        if (senderProfile.getPaypalHandle() != null && !senderProfile.getPaypalHandle().isEmpty()) {
            html.append("<li style='padding: 5px 0;'><strong>PayPal:</strong> ").append(senderProfile.getPaypalHandle()).append("</li>");
        }

        html.append("</ul>");

        html.append("<p style='margin-top: 20px;'>The full receipt breakdown is attached as a PDF.</p>");
        html.append("<p style='color: #6b7280; font-size: 12px; margin-top: 30px;'>This is an automated reminder sent via SplittyDupe.</p>");
        html.append("</body></html>");

        return html.toString();
    }

    private List<LineItemBreakdown> calculateAssigneeBreakdown(String assigneeName, Receipt receipt) {
        List<LineItemBreakdown> breakdown = new ArrayList<>();

        if (receipt.getLineItems() == null) {
            return breakdown;
        }

        for (LineItem item : receipt.getLineItems()) {
            if (item.getAssignees() == null || !item.getAssignees().contains(assigneeName)) {
                continue;
            }

            double itemTotal = item.getPrice() * item.getQuantity();
            double assigneeAmount;

            String splitMode = item.getSplitMode() != null ? item.getSplitMode() : "equal";

            if ("percentage".equals(splitMode) && item.getAssigneePercentages() != null) {
                Double percentage = item.getAssigneePercentages().get(assigneeName);
                assigneeAmount = percentage != null ? (itemTotal * percentage / 100.0) : 0;
            } else {
                int numAssignees = item.getAssignees().size();
                assigneeAmount = numAssignees > 0 ? itemTotal / numAssignees : 0;
            }

            breakdown.add(new LineItemBreakdown(item.getName(), assigneeAmount));
        }

        return breakdown;
    }

    private static class LineItemBreakdown {
        String name;
        double amount;

        LineItemBreakdown(String name, double amount) {
            this.name = name;
            this.amount = amount;
        }
    }

    private String getProfileDisplayName(UserProfile profile) {
        if (profile.getDisplayName() != null && !profile.getDisplayName().trim().isEmpty()) {
            return profile.getDisplayName();
        }
        return "Unknown";
    }
}
