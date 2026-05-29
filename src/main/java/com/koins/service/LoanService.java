package com.koins.service;

import com.koins.dto.request.LoanRepaymentRequest;
import com.koins.dto.request.LoanRequest;
import com.koins.dto.response.LoanResponse;

import java.util.List;
import java.util.UUID;

public interface LoanService {
    LoanResponse applyForLoan(String email, LoanRequest request);
    LoanResponse approveLoan(UUID loanId);
    LoanResponse disburseLoan(UUID loanId);
    LoanResponse repayLoan(String email, LoanRepaymentRequest request);
    LoanResponse getLoanById(UUID loanId);
    List<LoanResponse> getUserLoans(String email);
    List<LoanResponse> getAllLoans();
}
