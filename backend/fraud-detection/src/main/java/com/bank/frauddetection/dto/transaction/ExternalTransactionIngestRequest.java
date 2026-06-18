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

public record ExternalTransactionIngestRequest(
        @NotNull Long bankId,
        @NotNull Long branchId,
        @Size(max = 80) String customerId,
        @NotBlank @Size(max = 80) String senderAccountNumber,
        @NotBlank @Size(max = 80) String receiverAccountNumber,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        @Size(min = 3, max = 3) String currency,
        @NotBlank @Size(max = 40) String transactionType,
        @NotBlank @Size(max = 50) String channel,
        @Size(max = 120) String deviceId,
        @Size(max = 120) String location,
        @Size(max = 80) String ipAddress,
        AccountType accountType,
        CustomerType customerType,
        Boolean beneficiaryTrusted,
        Boolean knownDevice,
        Boolean knownLocation,
        @Min(0) @Max(23) Integer transactionHour,
        DailyTransactionPattern dailyTransactionPattern,
        Integer step,
        @DecimalMin(value = "0.0") BigDecimal oldbalanceOrg,
        @DecimalMin(value = "0.0") BigDecimal newbalanceOrig,
        @DecimalMin(value = "0.0") BigDecimal oldbalanceDest,
        @DecimalMin(value = "0.0") BigDecimal newbalanceDest
) {
}
