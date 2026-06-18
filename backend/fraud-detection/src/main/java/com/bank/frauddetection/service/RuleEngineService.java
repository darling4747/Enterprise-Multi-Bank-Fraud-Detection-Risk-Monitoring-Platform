package com.bank.frauddetection.service;

import com.bank.frauddetection.dto.fraud.FraudAnalysisResponse;
import com.bank.frauddetection.entity.Transaction;
import com.bank.frauddetection.enums.AccountType;
import com.bank.frauddetection.enums.CustomerType;
import com.bank.frauddetection.enums.DailyTransactionPattern;
import com.bank.frauddetection.util.RiskCalculator;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class RuleEngineService {

    private static final BigDecimal INDIVIDUAL_HIGH_AMOUNT = new BigDecimal("500000");

    public FraudAnalysisResponse analyze(Transaction transaction, double mlProbability) {
        List<String> rules = new ArrayList<>();
        int score = RiskCalculator.clamp((int) Math.round(mlProbability * 100));
        rules.add("ML_MODEL_SCORE_" + score);

        score += contextAdjustment(transaction, rules);

        int normalizedScore = RiskCalculator.clamp(score);
        return new FraudAnalysisResponse(
                transaction.getId(),
                transaction.getReference(),
                normalizedScore,
                RiskCalculator.levelFor(normalizedScore),
                RiskCalculator.decisionFor(normalizedScore),
                rules.isEmpty() ? List.of("NO_RULE_TRIGGERED") : rules,
                mlProbability
        );
    }

    private int contextAdjustment(Transaction transaction, List<String> rules) {
        AccountType accountType = transaction.getAccountType() == null ? AccountType.INDIVIDUAL : transaction.getAccountType();
        CustomerType customerType = transaction.getCustomerType() == null ? CustomerType.RETAIL : transaction.getCustomerType();
        int adjustment = 0;

        if (accountType == AccountType.CORPORATE) {
            adjustment -= 20;
            rules.add("CORPORATE_ACCOUNT:-20");
        }

        String channel = transaction.getChannel() == null ? "" : transaction.getChannel().trim().toUpperCase();
        if (accountType == AccountType.CORPORATE && "CORPORATE_PORTAL".equals(channel)) {
            adjustment -= 5;
            rules.add("CORPORATE_PORTAL_CHANNEL:-5");
        } else if (accountType == AccountType.CORPORATE && "UPI".equals(channel)) {
            adjustment += 15;
            rules.add("UNUSUAL_CORPORATE_UPI_CHANNEL:+15");
        } else if (accountType == AccountType.INDIVIDUAL && "CORPORATE_PORTAL".equals(channel)) {
            adjustment += 10;
            rules.add("UNUSUAL_INDIVIDUAL_CORPORATE_PORTAL:+10");
        }

        adjustment += customerTypeAdjustment(customerType, rules);

        if (transaction.isBeneficiaryTrusted()) {
            adjustment -= 15;
            rules.add("TRUSTED_BENEFICIARY:-15");
        } else {
            adjustment += 15;
            rules.add("NEW_BENEFICIARY:+15");
        }

        if (transaction.isKnownDevice()) {
            adjustment -= 10;
            rules.add("KNOWN_DEVICE:-10");
        } else {
            adjustment += 15;
            rules.add("NEW_DEVICE:+15");
        }

        if (transaction.isKnownLocation()) {
            adjustment -= 10;
            rules.add("KNOWN_LOCATION:-10");
        } else {
            adjustment += 15;
            rules.add("NEW_LOCATION:+15");
        }

        Integer hour = transaction.getTransactionHour();
        if (hour != null && hour >= 9 && hour <= 17) {
            adjustment -= 5;
            rules.add("REGULAR_BUSINESS_HOURS:-5");
        } else if (hour != null && hour >= 0 && hour <= 5) {
            adjustment += 10;
            rules.add("MIDNIGHT_TRANSACTION:+10");
        }

        if (accountType == AccountType.INDIVIDUAL && transaction.getAmount().compareTo(INDIVIDUAL_HIGH_AMOUNT) > 0) {
            adjustment += 20;
            rules.add("INDIVIDUAL_HIGH_AMOUNT:+20");
        }

        if (transaction.getDailyTransactionPattern() == DailyTransactionPattern.UNUSUAL) {
            adjustment += 15;
            rules.add("UNUSUAL_DAILY_TRANSACTION_PATTERN:+15");
        } else {
            adjustment -= 5;
            rules.add("NORMAL_DAILY_TRANSACTION_PATTERN:-5");
        }

        return adjustment;
    }

    private int customerTypeAdjustment(CustomerType customerType, List<String> rules) {
        return switch (customerType) {
            case NEW_CUSTOMER -> {
                rules.add("NEW_CUSTOMER:+10");
                yield 10;
            }
            case PREMIUM -> {
                rules.add("PREMIUM_CUSTOMER:-5");
                yield -5;
            }
            case HIGH_NET_WORTH -> {
                rules.add("HIGH_NET_WORTH_CUSTOMER:-5");
                yield -5;
            }
            case ENTERPRISE -> {
                rules.add("ENTERPRISE_CUSTOMER:-5");
                yield -5;
            }
            case GOVERNMENT -> {
                rules.add("GOVERNMENT_CUSTOMER:-10");
                yield -10;
            }
            case RETAIL, SME -> 0;
        };
    }
}
