$basePath = "c:\Users\HP Victus\Music\fraud detection system\fraud-detection"
$srcMainJava = "$basePath\src\main\java\com\bank\frauddetection"
$srcMainRes = "$basePath\src\main\resources"
$testMainJava = "$basePath\src\test\java\com\bank\frauddetection"

$directories = @(
    "$basePath\.mvn",
    "$srcMainJava\controller",
    "$srcMainJava\service",
    "$srcMainJava\repository",
    "$srcMainJava\entity",
    "$srcMainJava\dto\auth",
    "$srcMainJava\dto\transaction",
    "$srcMainJava\dto\fraud",
    "$srcMainJava\dto\alert",
    "$srcMainJava\dto\dashboard",
    "$srcMainJava\security",
    "$srcMainJava\config",
    "$srcMainJava\kafka\producer",
    "$srcMainJava\kafka\consumer",
    "$srcMainJava\kafka\event",
    "$srcMainJava\exception",
    "$srcMainJava\enums",
    "$srcMainJava\util",
    "$srcMainRes\static",
    "$srcMainRes\templates",
    "$testMainJava\controller",
    "$testMainJava\service",
    "$testMainJava\repository"
)

foreach ($dir in $directories) {
    New-Item -ItemType Directory -Force -Path $dir | Out-Null
}

$files = @(
    "$basePath\.gitignore",
    "$basePath\mvnw",
    "$basePath\mvnw.cmd",
    "$basePath\pom.xml",
    "$basePath\README.md",
    "$srcMainRes\application.properties",
    "$srcMainRes\application-dev.properties",
    "$srcMainRes\application-prod.properties",
    "$srcMainJava\FraudDetectionApplication.java",
    "$srcMainJava\controller\AuthController.java",
    "$srcMainJava\controller\TransactionController.java",
    "$srcMainJava\controller\FraudController.java",
    "$srcMainJava\controller\AlertController.java",
    "$srcMainJava\controller\DashboardController.java",
    "$srcMainJava\service\AuthService.java",
    "$srcMainJava\service\TransactionService.java",
    "$srcMainJava\service\FraudDetectionService.java",
    "$srcMainJava\service\RuleEngineService.java",
    "$srcMainJava\service\AlertService.java",
    "$srcMainJava\service\DashboardService.java",
    "$srcMainJava\service\MlServiceClient.java",
    "$srcMainJava\repository\UserRepository.java",
    "$srcMainJava\repository\TransactionRepository.java",
    "$srcMainJava\repository\FraudCaseRepository.java",
    "$srcMainJava\repository\AlertRepository.java",
    "$srcMainJava\entity\User.java",
    "$srcMainJava\entity\Role.java",
    "$srcMainJava\entity\Transaction.java",
    "$srcMainJava\entity\FraudCase.java",
    "$srcMainJava\entity\Alert.java",
    "$srcMainJava\dto\auth\LoginRequest.java",
    "$srcMainJava\dto\auth\RegisterRequest.java",
    "$srcMainJava\dto\auth\AuthResponse.java",
    "$srcMainJava\dto\transaction\TransactionRequest.java",
    "$srcMainJava\dto\transaction\TransactionResponse.java",
    "$srcMainJava\dto\transaction\TransactionSummaryResponse.java",
    "$srcMainJava\dto\fraud\FraudScoreResponse.java",
    "$srcMainJava\dto\fraud\FraudCaseResponse.java",
    "$srcMainJava\dto\fraud\FraudAnalysisResponse.java",
    "$srcMainJava\dto\alert\AlertResponse.java",
    "$srcMainJava\dto\dashboard\DashboardStatsResponse.java",
    "$srcMainJava\dto\dashboard\DashboardChartResponse.java",
    "$srcMainJava\security\JwtAuthenticationFilter.java",
    "$srcMainJava\security\JwtService.java",
    "$srcMainJava\security\CustomUserDetailsService.java",
    "$srcMainJava\security\SecurityConfig.java",
    "$srcMainJava\config\CorsConfig.java",
    "$srcMainJava\config\KafkaConfig.java",
    "$srcMainJava\config\OpenApiConfig.java",
    "$srcMainJava\kafka\producer\TransactionProducer.java",
    "$srcMainJava\kafka\consumer\FraudConsumer.java",
    "$srcMainJava\kafka\event\TransactionCreatedEvent.java",
    "$srcMainJava\kafka\event\FraudDetectedEvent.java",
    "$srcMainJava\exception\GlobalExceptionHandler.java",
    "$srcMainJava\exception\ResourceNotFoundException.java",
    "$srcMainJava\exception\BusinessException.java",
    "$srcMainJava\exception\UnauthorizedException.java",
    "$srcMainJava\enums\RoleType.java",
    "$srcMainJava\enums\TransactionStatus.java",
    "$srcMainJava\enums\FraudDecision.java",
    "$srcMainJava\enums\RiskLevel.java",
    "$srcMainJava\enums\AlertStatus.java",
    "$srcMainJava\util\DateUtil.java",
    "$srcMainJava\util\RiskCalculator.java",
    "$srcMainJava\util\ValidationUtil.java"
)

foreach ($file in $files) {
    if (-not (Test-Path $file)) {
        New-Item -ItemType File -Force -Path $file | Out-Null
        
        # Add basic package declaration for java files
        if ($file.EndsWith(".java")) {
            $relativePath = $file.Substring($srcMainJava.Length + 1)
            $dirPath = Split-Path $relativePath -Parent
            if ($dirPath -eq "") {
                $packageName = "com.bank.frauddetection"
            } else {
                $packageSuffix = $dirPath -replace "\\", "."
                $packageName = "com.bank.frauddetection.$packageSuffix"
            }
            $className = (Split-Path $file -Leaf) -replace "\.java", ""
            
            $isInterface = $file -match "repository"
            $isEnum = $file -match "enums"
            
            $type = "public class"
            if ($isInterface) { $type = "public interface" }
            if ($isEnum) { $type = "public enum" }

            $content = "package $packageName;`n`n$type $className {`n`n}`n"
            Set-Content -Path $file -Value $content
        }
    }
}
Write-Output "Scaffolding complete."
