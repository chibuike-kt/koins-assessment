package com.koins.repository;

import com.koins.entity.Loan;
import com.koins.entity.User;
import com.koins.enums.LoanStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface LoanRepository extends JpaRepository<Loan, UUID> {
    List<Loan> findByUser(User user);
    List<Loan> findByUserId(UUID userId);
    List<Loan> findByStatus(LoanStatus status);
    List<Loan> findByStatusAndDueDateBefore(LoanStatus status, LocalDateTime date);
    List<Loan> findByStatusAndDueDateBetween(LoanStatus status, LocalDateTime start, LocalDateTime end);
}
