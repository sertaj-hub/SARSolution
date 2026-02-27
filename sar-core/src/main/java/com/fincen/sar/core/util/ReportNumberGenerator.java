package com.fincen.sar.core.util;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Generates unique SAR report numbers in format: SAR-YYYYMMDD-NNNNNN
 */
@Component
public class ReportNumberGenerator {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private final AtomicLong sequence = new AtomicLong(System.currentTimeMillis() % 1_000_000);

    public String generate() {
        String date = LocalDate.now().format(DATE_FORMAT);
        long seq = sequence.incrementAndGet() % 1_000_000;
        return String.format("SAR-%s-%06d", date, seq);
    }

    public String generateBatchNumber() {
        String date = LocalDate.now().format(DATE_FORMAT);
        long seq = sequence.incrementAndGet() % 100_000;
        return String.format("BATCH-%s-%05d", date, seq);
    }
}
