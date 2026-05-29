package com.koins.dto.response;

import com.koins.enums.AccountStatus;
import com.koins.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private UUID id;
    private String fullName;
    private String email;
    private String phoneNumber;
    private AccountStatus status;
    private Role role;
    private LocalDateTime createdAt;
}
