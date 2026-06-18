$baseDir = "c:\Users\HP Victus\Music\fraud detection system\backend\fraud-detection"

$dirs = @(
    "src/main/java/com/bank/frauddetection/controller",
    "src/main/java/com/bank/frauddetection/service",
    "src/main/java/com/bank/frauddetection/repository",
    "src/main/java/com/bank/frauddetection/entity",
    "src/main/java/com/bank/frauddetection/dto/auth",
    "src/main/java/com/bank/frauddetection/dto/transaction",
    "src/main/java/com/bank/frauddetection/dto/fraud",
    "src/main/java/com/bank/frauddetection/dto/alert",
    "src/main/java/com/bank/frauddetection/dto/dashboard",
    "src/main/java/com/bank/frauddetection/security",
    "src/main/java/com/bank/frauddetection/config",
    "src/main/java/com/bank/frauddetection/kafka/producer",
    "src/main/java/com/bank/frauddetection/kafka/consumer",
    "src/main/java/com/bank/frauddetection/kafka/event",
    "src/main/java/com/bank/frauddetection/exception",
    "src/main/java/com/bank/frauddetection/enums",
    "src/main/java/com/bank/frauddetection/util",
    "src/main/resources/static",
    "src/main/resources/templates",
    "src/test/java/com/bank/frauddetection/controller",
    "src/test/java/com/bank/frauddetection/service",
    "src/test/java/com/bank/frauddetection/repository"
)

foreach ($dir in $dirs) {
    $targetDir = "$baseDir/$dir"
    if (-not (Test-Path -Path $targetDir)) {
        New-Item -ItemType Directory -Force -Path $targetDir | Out-Null
    }
}

$files = @(
    "src/main/java/com/bank/frauddetection/FraudDetectionApplication.java",
    "src/main/java/com/bank/frauddetection/controller/AuthController.java",
    "src/main/java/com/bank/frauddetection/controller/TransactionController.java",
    "src/main/java/com/bank/frauddetection/controller/FraudController.java",
    "src/main/java/com/bank/frauddetection/controller/AlertController.java",
    "src/main/java/com/bank/frauddetection/controller/DashboardController.java",
    "src/main/java/com/bank/frauddetection/service/AuthService.java",
    "src/main/java/com/bank/frauddetection/service/TransactionService.java",
    "src/main/java/com/bank/frauddetection/service/FraudDetectionService.java",
    "src/main/java/com/bank/frauddetection/service/RuleEngineService.java",
    "src/main/java/com/bank/frauddetection/service/AlertService.java",
    "src/main/java/com/bank/frauddetection/service/DashboardService.java",
    "src/main/java/com/bank/frauddetection/service/MlServiceClient.java",
    "src/main/java/com/bank/frauddetection/repository/UserRepository.java",
    "src/main/java/com/bank/frauddetection/repository/TransactionRepository.java",
    "src/main/java/com/bank/frauddetection/repository/FraudCaseRepository.java",
    "src/main/java/com/bank/frauddetection/repository/AlertRepository.java",
    "src/main/java/com/bank/frauddetection/entity/User.java",
    "src/main/java/com/bank/frauddetection/entity/Role.java",
    "src/main/java/com/bank/frauddetection/entity/Transaction.java",
    "src/main/java/com/bank/frauddetection/entity/FraudCase.java",
    "src/main/java/com/bank/frauddetection/entity/Alert.java",
    "src/main/java/com/bank/frauddetection/dto/auth/LoginRequest.java",
    "src/main/java/com/bank/frauddetection/dto/auth/RegisterRequest.java",
    "src/main/java/com/bank/frauddetection/dto/auth/AuthResponse.java",
    "src/main/java/com/bank/frauddetection/dto/transaction/TransactionRequest.java",
    "src/main/java/com/bank/frauddetection/dto/transaction/TransactionResponse.java",
    "src/main/java/com/bank/frauddetection/dto/transaction/TransactionSummaryResponse.java",
    "src/main/java/com/bank/frauddetection/dto/fraud/FraudScoreResponse.java",
    "src/main/java/com/bank/frauddetection/dto/fraud/FraudCaseResponse.java",
    "src/main/java/com/bank/frauddetection/dto/fraud/FraudAnalysisResponse.java",
    "src/main/java/com/bank/frauddetection/dto/alert/AlertResponse.java",
    "src/main/java/com/bank/frauddetection/dto/dashboard/DashboardStatsResponse.java",
    "src/main/java/com/bank/frauddetection/dto/dashboard/DashboardChartResponse.java",
    "src/main/java/com/bank/frauddetection/security/JwtAuthenticationFilter.java",
    "src/main/java/com/bank/frauddetection/security/JwtService.java",
    "src/main/java/com/bank/frauddetection/security/CustomUserDetailsService.java",
    "src/main/java/com/bank/frauddetection/security/SecurityConfig.java",
    "src/main/java/com/bank/frauddetection/config/CorsConfig.java",
    "src/main/java/com/bank/frauddetection/config/KafkaConfig.java",
    "src/main/java/com/bank/frauddetection/config/OpenApiConfig.java",
    "src/main/java/com/bank/frauddetection/kafka/producer/TransactionProducer.java",
    "src/main/java/com/bank/frauddetection/kafka/consumer/FraudConsumer.java",
    "src/main/java/com/bank/frauddetection/kafka/event/TransactionCreatedEvent.java",
    "src/main/java/com/bank/frauddetection/kafka/event/FraudDetectedEvent.java",
    "src/main/java/com/bank/frauddetection/exception/GlobalExceptionHandler.java",
    "src/main/java/com/bank/frauddetection/exception/ResourceNotFoundException.java",
    "src/main/java/com/bank/frauddetection/exception/BusinessException.java",
    "src/main/java/com/bank/frauddetection/exception/UnauthorizedException.java",
    "src/main/java/com/bank/frauddetection/enums/RoleType.java",
    "src/main/java/com/bank/frauddetection/enums/TransactionStatus.java",
    "src/main/java/com/bank/frauddetection/enums/FraudDecision.java",
    "src/main/java/com/bank/frauddetection/enums/RiskLevel.java",
    "src/main/java/com/bank/frauddetection/enums/AlertStatus.java",
    "src/main/java/com/bank/frauddetection/util/DateUtil.java",
    "src/main/java/com/bank/frauddetection/util/RiskCalculator.java",
    "src/main/java/com/bank/frauddetection/util/ValidationUtil.java",
    "src/main/resources/application.properties",
    "src/main/resources/application-dev.properties",
    "src/main/resources/application-prod.properties",
    ".gitignore",
    "mvnw",
    "mvnw.cmd",
    "pom.xml",
    "README.md"
)

foreach ($file in $files) {
    $targetFile = "$baseDir/$file"
    if (-not (Test-Path -Path $targetFile)) {
        New-Item -ItemType File -Force -Path $targetFile | Out-Null
    }
}

Write-Host "Scaffolding completed, existing files were preserved."
