package com.bank.frauddetection.util;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public final class DateUtil {

    private DateUtil() {
    }

    public static Instant hoursAgo(long hours) {
        return Instant.now().minus(hours, ChronoUnit.HOURS);
    }
}
