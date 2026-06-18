package com.bank.frauddetection.dto.transaction;

import com.bank.frauddetection.enums.AccountType;
import com.bank.frauddetection.enums.CustomerType;
import com.bank.frauddetection.enums.DailyTransactionPattern;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record TransactionRequest(
        @NotBlank @Size(max = 80) String customerId,
        @NotBlank @Size(max = 80) String sourceAccount,
        @NotBlank @Size(max = 80) String destinationAccount,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        @NotBlank @Size(min = 3, max = 3) String currency,
        @NotBlank @Size(max = 50) String channel,
        @Size(max = 80) String merchantCategory,
        @Size(max = 80) String country,
        @Size(max = 80) String ipAddress,
        @Size(max = 120) String deviceId,
        AccountType accountType,
        CustomerType customerType,
        boolean beneficiaryTrusted,
        boolean knownDevice,
        boolean knownLocation,
        @Min(0) @Max(23) Integer transactionHour,
        DailyTransactionPattern dailyTransactionPattern,
        @Size(max = 40) String type,
        Integer step,
        @DecimalMin(value = "0.0") BigDecimal oldbalanceOrg,
        @DecimalMin(value = "0.0") BigDecimal newbalanceOrig,
        @DecimalMin(value = "0.0") BigDecimal oldbalanceDest,
        @DecimalMin(value = "0.0") BigDecimal newbalanceDest,
        Long bankId,
        Long branchId
) {
}
