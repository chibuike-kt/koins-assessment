package com.koins.repository;

import com.koins.entity.Transaction;
import com.koins.entity.User;
import com.koins.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    List<Transaction> findByUserOrderByCreatedAtDesc(User user);
    List<Transaction> findByWalletOrderByCreatedAtDesc(Wallet wallet);
    Optional<Transaction> findByReference(String reference);
    boolean existsByReference(String reference);
}
