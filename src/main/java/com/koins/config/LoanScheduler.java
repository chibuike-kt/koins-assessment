package com.koins.config;

import com.koins.entity.Loan;
import com.koins.enums.LoanStatus;
import com.koins.repository.LoanRepository;
import com.koins.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoanScheduler {

    private final LoanRepository loanRepository;
    private final EmailService emailService;

    /**
     * Runs every day at midnight.
     * Marks all disbursed loans past their due date as DEFAULTED.
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void markOverdueLoans() {
        log.info("Running overdue loan check...");

        List<Loan> overdueLoans = loanRepository.findByStatusAndDueDateBefore(
                LoanStatus.DISBURSED, LocalDateTime.now());

        if (overdueLoans.isEmpty()) {
            log.info("No overdue loans found");
            return;
        }

        for (Loan loan : overdueLoans) {
            loan.setStatus(LoanStatus.DEFAULTED);
            loanRepository.save(loan);
            log.warn("Loan marked as DEFAULTED: {} user: {}",
                    loan.getId(), loan.getUser().getEmail());
        }

        log.info("Marked {} loans as DEFAULTED", overdueLoans.size());
    }

    /**
     * Runs every day at 9am.
     * Sends repayment reminders for loans due within the next 3 days.
     */
    @Scheduled(cron = "0 0 9 * * *")
    public void sendRepaymentReminders() {
        log.info("Running repayment reminder job...");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threeDaysFromNow = now.plusDays(3);

        List<Loan> upcomingLoans = loanRepository.findByStatusAndDueDateBetween(
                LoanStatus.DISBURSED, now, threeDaysFromNow);

        if (upcomingLoans.isEmpty()) {
            log.info("No upcoming loan repayments in next 3 days");
            return;
        }

        for (Loan loan : upcomingLoans) {
            BigDecimal interest = loan.getLoanAmount()
                    .multiply(loan.getInterestRate())
                    .divide(new BigDecimal("100"), 0, RoundingMode.HALF_UP);
            BigDecimal totalRepayable = loan.getLoanAmount().add(interest);

            String dueDate = loan.getDueDate()
                    .format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm"));

            emailService.sendLoanRepaymentReminderEmail(
                    loan.getUser().getEmail(),
                    loan.getUser().getFullName(),
                    totalRepayable.toString(),
                    dueDate
            );

            log.info("Repayment reminder sent to: {} for loan: {}",
                    loan.getUser().getEmail(), loan.getId());
        }

        log.info("Sent {} repayment reminders", upcomingLoans.size());
    }
}
