package com.koins.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Async
    public void sendOtpEmail(String to, String otp) {
        sendEmail(to, "Your Koins OTP Code",
                "Your OTP code is: " + otp + "\n\nThis code expires in 10 minutes.\nDo not share it with anyone.");
    }

    @Async
    public void sendLoanApprovalEmail(String to, String fullName, String amount) {
        sendEmail(to, "Loan Approved - Koins",
                "Dear " + fullName + ",\n\nYour loan request of NGN " + amount +
                " has been approved and will be disbursed to your wallet shortly.\n\nKoins Team");
    }

    @Async
    public void sendLoanRepaymentReminderEmail(String to, String fullName, String amount, String dueDate) {
        sendEmail(to, "Loan Repayment Reminder - Koins",
                "Dear " + fullName + ",\n\nThis is a reminder that your loan repayment of NGN " + amount +
                " is due on " + dueDate + ".\n\nPlease ensure your wallet is funded.\n\nKoins Team");
    }

    @Async
    public void sendRepaymentSuccessEmail(String to, String fullName, String amount) {
        sendEmail(to, "Loan Repayment Successful - Koins",
                "Dear " + fullName + ",\n\nYour loan repayment of NGN " + amount +
                " has been received successfully.\n\nThank you for banking with Koins.");
    }

    private void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Email sent to {}", to);
        } catch (Exception ex) {
            log.error("Failed to send email to {}: {}", to, ex.getMessage());
        }
    }
}
