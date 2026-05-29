package com.koins.util;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class ReferenceUtil {

    public String generateReference() {
        return "KNS-" + Instant.now().toEpochMilli() + "-" +
                UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
