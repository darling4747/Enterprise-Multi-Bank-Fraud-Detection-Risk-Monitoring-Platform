package com.bank.frauddetection.util;

import com.bank.frauddetection.exception.BusinessException;
import java.math.BigDecimal;

public final class ValidationUtil {

    private ValidationUtil() {
    }

    public static void ensurePositiveAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Transaction amount must be greater than zero");
        }
    }
}
