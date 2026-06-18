package com.bank.frauddetection.service;

import com.bank.frauddetection.entity.Transaction;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class MlServiceClient {

    private static final Logger log = LoggerFactory.getLogger(MlServiceClient.class);

    private final RestTemplate restTemplate;
    private final URI predictionUri;
    private final boolean enabled;

    public MlServiceClient(
            @Value("${app.ml-service.url:http://localhost:8000}") String mlServiceUrl,
            @Value("${app.ml-service.enabled:true}") boolean enabled,
            @Value("${app.ml-service.connect-timeout-ms:2000}") long connectTimeoutMs,
            @Value("${app.ml-service.read-timeout-ms:5000}") long readTimeoutMs
    ) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        factory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
        this.restTemplate = new RestTemplate(factory);
        this.predictionUri = URI.create(trimTrailingSlash(mlServiceUrl) + "/predict");
        this.enabled = enabled;
    }

    public double predictFraudProbability(Transaction transaction) {
        if (!enabled) {
            log.info("ML_SERVICE_DISABLED using heuristic fallback reference={}", transaction.getReference());
            return heuristicProbability(transaction);
        }

        try {
            MlPredictionRequest request = MlPredictionRequest.from(transaction);
            log.info(
                    "CALLING_ML_SERVICE reference={} transactionId={} type={} amount={}",
                    request.reference(),
                    request.transactionId(),
                    request.type(),
                    request.amount()
            );
            MlPredictionResponse response = restTemplate.postForObject(
                    predictionUri,
                    request,
                    MlPredictionResponse.class
            );
            if (response == null) {
                log.warn("ML_SERVICE_EMPTY_RESPONSE using heuristic fallback reference={}", transaction.getReference());
                return heuristicProbability(transaction);
            }
            log.info(
                    "ML_SCORE_RECEIVED reference={} fraudProbability={} mlRiskScore={}",
                    transaction.getReference(),
                    response.fraudProbability(),
                    response.mlRiskScore()
            );
            return clamp(response.fraudProbability());
        } catch (RestClientException | IllegalArgumentException ex) {
            log.warn("ML service prediction failed; using heuristic fallback. reason={}", ex.getMessage());
            return heuristicProbability(transaction);
        }
    }

    private double heuristicProbability(Transaction transaction) {
        double probability = 0.05;
        if (transaction.getAmount().compareTo(new BigDecimal("10000")) > 0) {
            probability += 0.25;
        }
        if ("TRANSFER".equalsIgnoreCase(inferType(transaction)) || "CASH_OUT".equalsIgnoreCase(inferType(transaction))) {
            probability += 0.20;
        }
        if (transaction.getOldbalanceOrg() != null && transaction.getNewbalanceOrig() != null) {
            BigDecimal expectedOriginAfter = transaction.getOldbalanceOrg().subtract(transaction.getAmount());
            if (expectedOriginAfter.subtract(transaction.getNewbalanceOrig()).abs().compareTo(new BigDecimal("1.00")) > 0) {
                probability += 0.25;
            }
        }
        double clamped = clamp(probability);
        log.info("HEURISTIC_ML_FALLBACK reference={} probability={} mlRiskScore={}", transaction.getReference(), clamped, Math.round(clamped * 100));
        return clamped;
    }

    private double clamp(double probability) {
        if (!Double.isFinite(probability)) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, probability));
    }

    private static String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "http://localhost:8000";
        }
        String normalized = value.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private record MlPredictionRequest(
            Long transactionId,
            String reference,
            BigDecimal amount,
            String type,
            BigDecimal oldbalanceOrg,
            BigDecimal newbalanceOrig,
            BigDecimal oldbalanceDest,
            BigDecimal newbalanceDest
    ) {
        static MlPredictionRequest from(Transaction transaction) {
            return new MlPredictionRequest(
                    transaction.getId(),
                    transaction.getReference(),
                    transaction.getAmount(),
                    inferType(transaction),
                    transaction.getOldbalanceOrg(),
                    transaction.getNewbalanceOrig(),
                    transaction.getOldbalanceDest(),
                    transaction.getNewbalanceDest()
            );
        }
    }

    private record MlPredictionResponse(
            double fraudProbability,
            int mlRiskScore
    ) {
    }

    private static String inferType(Transaction transaction) {
        if (transaction.getTransactionType() != null && !transaction.getTransactionType().isBlank()) {
            return normalized(transaction.getTransactionType());
        }
        String merchantCategory = normalized(transaction.getMerchantCategory());
        String channel = normalized(transaction.getChannel());

        if ("WIRE_TRANSFER".equals(merchantCategory) || "TRANSFER".equals(channel)) {
            return "TRANSFER";
        }
        if ("ATM".equals(channel) || "ATM_WITHDRAWAL".equals(channel)) {
            return "CASH_OUT";
        }
        if ("DEPOSIT".equals(channel)) {
            return "CASH_IN";
        }
        if ("DEBIT".equals(channel)) {
            return "DEBIT";
        }
        return "PAYMENT";
    }

    private static String normalized(String value) {
        return value == null ? "" : value.trim().toUpperCase().replace('-', '_').replace(' ', '_');
    }
}
