package com.koins.controller;

import com.koins.dto.request.LoanRepaymentRequest;
import com.koins.dto.request.LoanRequest;
import com.koins.dto.response.ApiResponse;
import com.koins.dto.response.LoanResponse;
import com.koins.service.LoanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/loans")
@RequiredArgsConstructor
public class LoanController {

    private final LoanService loanService;

    @PostMapping("/apply")
    public ResponseEntity<ApiResponse<LoanResponse>> applyForLoan(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody LoanRequest request) {
        LoanResponse response = loanService.applyForLoan(userDetails.getUsername(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Loan application submitted", response));
    }

    @PutMapping("/{loanId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<LoanResponse>> approveLoan(@PathVariable UUID loanId) {
        LoanResponse response = loanService.approveLoan(loanId);
        return ResponseEntity.ok(ApiResponse.success("Loan approved", response));
    }

    @PutMapping("/{loanId}/disburse")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<LoanResponse>> disburseLoan(@PathVariable UUID loanId) {
        LoanResponse response = loanService.disburseLoan(loanId);
        return ResponseEntity.ok(ApiResponse.success("Loan disbursed", response));
    }

    @PostMapping("/repay")
    public ResponseEntity<ApiResponse<LoanResponse>> repayLoan(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody LoanRepaymentRequest request) {
        LoanResponse response = loanService.repayLoan(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success("Repayment processed", response));
    }

    @GetMapping("/{loanId}")
    public ResponseEntity<ApiResponse<LoanResponse>> getLoan(@PathVariable UUID loanId) {
        LoanResponse response = loanService.getLoanById(loanId);
        return ResponseEntity.ok(ApiResponse.success("Loan fetched", response));
    }

    @GetMapping("/my-loans")
    public ResponseEntity<ApiResponse<List<LoanResponse>>> getUserLoans(
            @AuthenticationPrincipal UserDetails userDetails) {
        List<LoanResponse> response = loanService.getUserLoans(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Loans fetched", response));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<LoanResponse>>> getAllLoans() {
        List<LoanResponse> response = loanService.getAllLoans();
        return ResponseEntity.ok(ApiResponse.success("All loans fetched", response));
    }
}
