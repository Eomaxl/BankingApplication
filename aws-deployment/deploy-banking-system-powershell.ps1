# Banking System AWS Deployment Script for PowerShell
# Customized for your existing AWS infrastructure

param(
    [string]$ImageTag = "latest"
)

# Configuration
$AWS_REGION = "us-east-1"
$AWS_ACCOUNT_ID = "655593806999"
$ECR_REPOSITORY = "eomaxl/banking-system-registry"
$ECS_CLUSTER = "bank-app-cluster"

Write-Host "🚀 Starting Banking System Deployment" -ForegroundColor Green
Write-Host "📍 Region: $AWS_REGION" -ForegroundColor Cyan
Write-Host "🏷️  Image Tag: $ImageTag" -ForegroundColor Cyan
Write-Host "🏦 ECR Repository: $ECR_REPOSITORY" -ForegroundColor Cyan
Write-Host "⚙️  ECS Cluster: $ECS_CLUSTER" -ForegroundColor Cyan

function Write-Success {
    param([string]$Message)
    Write-Host "✅ $Message" -ForegroundColor Green
}

function Write-Warning {
    param([string]$Message)
    Write-Host "⚠️  $Message" -ForegroundColor Yellow
}

function Write-Error {
    param([string]$Message)
    Write-Host "❌ $Message" -ForegroundColor Red
}

# Step 1: Check AWS CLI configuration
try {
    $null = aws sts get-caller-identity 2>$null
    Write-Success "AWS CLI is configured"
} catch {
    Write-Error "AWS CLI is not configured. Please run 'aws configure' first."
    exit 1
}

# Step 2: Build the application
Write-Success "Building Spring Boot application..."
try {
    & mvn clean package -DskipTests
    if (-not (Test-Path "target\banking-system-1.0.0.jar")) {
        throw "JAR file not found"
    }
    Write-Success "Application built successfully"
} catch {
    Write-Error "Build failed: $($_.Exception.Message)"
    exit 1
}

# Step 3: Build Docker image
Write-Success "Building Docker image..."
try {
    & docker build -t "banking-system:$ImageTag" .
    Write-Success "Docker image built successfully"
} catch {
    Write-Error "Docker build failed: $($_.Exception.Message)"
    exit 1
}

# Step 4: Tag image for ECR
$ECR_URI = "$AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/$ECR_REPOSITORY`:$ImageTag"
& docker tag "banking-system:$ImageTag" $ECR_URI
Write-Success "Docker image tagged: $ECR_URI"

# Step 5: Login to ECR
Write-Success "Logging into ECR..."
try {
    $loginToken = aws ecr get-login-password --region $AWS_REGION
    $loginToken | docker login --username AWS --password-stdin "$AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com"
    Write-Success "ECR login successful"
} catch {
    Write-Error "ECR login failed: $($_.Exception.Message)"
    exit 1
}

# Step 6: Push image to ECR
Write-Success "Pushing image to ECR..."
try {
    & docker push $ECR_URI
    Write-Success "Image pushed successfully"
} catch {
    Write-Error "Image push failed: $($_.Exception.Message)"
    exit 1
}

# Step 7: Create CloudWatch Log Group
Write-Success "Creating CloudWatch Log Group..."
try {
    aws logs create-log-group --log-group-name "/ecs/banking-system" --region $AWS_REGION 2>$null
} catch {
    Write-Warning "Log group already exists or creation failed"
}

# Step 8: Update task definition with new image
Write-Success "Preparing Task Definition..."
$taskDefContent = Get-Content "aws-deployment\task-definition.json" -Raw
$updatedTaskDef = $taskDefContent -replace "655593806999\.dkr\.ecr\.us-east-1\.amazonaws\.com/eomaxl/banking-system-registry:latest", $ECR_URI
$updatedTaskDef | Set-Content "task-definition-updated.json"

# Step 9: Register ECS Task Definition
Write-Success "Registering ECS Task Definition..."
try {
    $taskDefArn = aws ecs register-task-definition --cli-input-json file://task-definition-updated.json --region $AWS_REGION --query "taskDefinition.taskDefinitionArn" --output text
    Write-Success "Task Definition registered: $taskDefArn"
} catch {
    Write-Error "Task definition registration failed: $($_.Exception.Message)"
    exit 1
}

# Step 10: Update ECS Service
Write-Success "Checking ECS Service..."
try {
    $serviceExists = aws ecs describe-services --cluster $ECS_CLUSTER --services banking-system-service --region $AWS_REGION --query "services[0].serviceName" --output text 2>$null

    if ($serviceExists -eq "banking-system-service") {
        Write-Success "Service exists, updating..."
        aws ecs update-service --cluster $ECS_CLUSTER --service banking-system-service --task-definition $taskDefArn --region $AWS_REGION --query "service.serviceName" --output text
        Write-Success "Service updated successfully"
    } else {
        Write-Warning "Service doesn't exist. You'll need to create it manually."
        Write-Host "To create the service, you need:" -ForegroundColor Yellow
        Write-Host "1. VPC Subnets (at least 2 in different AZs)" -ForegroundColor Yellow
        Write-Host "2. Security Group allowing inbound traffic on port 8080" -ForegroundColor Yellow
        Write-Host "3. Application Load Balancer (optional but recommended)" -ForegroundColor Yellow
        Write-Host ""
        Write-Host "Example command to create service:" -ForegroundColor Cyan
        Write-Host "aws ecs create-service \\" -ForegroundColor Cyan
        Write-Host "  --cluster $ECS_CLUSTER \\" -ForegroundColor Cyan
        Write-Host "  --service-name banking-system-service \\" -ForegroundColor Cyan
        Write-Host "  --task-definition $taskDefArn \\" -ForegroundColor Cyan
        Write-Host "  --desired-count 2 \\" -ForegroundColor Cyan
        Write-Host "  --launch-type FARGATE \\" -ForegroundColor Cyan
        Write-Host "  --network-configuration 'awsvpcConfiguration={subnets=[subnet-06e3c2ed3ba238b90,subnet-0c53ff0bdf1ece475],securityGroups=[sg-0b9faa0ae8490a3d0],assignPublicIp=ENABLED}'" -ForegroundColor Cyan
    }
} catch {
    Write-Error "Service update failed: $($_.Exception.Message)"
}

# Step 11: Wait for deployment (if service exists)
if ($serviceExists -eq "banking-system-service") {
    Write-Success "Waiting for service to stabilize..."
    aws ecs wait services-stable --cluster $ECS_CLUSTER --services banking-system-service --region $AWS_REGION
    Write-Success "Deployment completed successfully!"
}

# Step 12: Get service information
Write-Success "Getting service information..."
try {
    $tasks = aws ecs list-tasks --cluster $ECS_CLUSTER --service-name banking-system-service --region $AWS_REGION --query "taskArns" --output text 2>$null

    if ($tasks) {
        Write-Success "Service is running with tasks: $tasks"
        aws ecs describe-tasks --cluster $ECS_CLUSTER --tasks $tasks --region $AWS_REGION --query "tasks[0].{TaskArn:taskArn,LastStatus:lastStatus,HealthStatus:healthStatus}"
    } else {
        Write-Warning "No tasks found. Service might be starting up."
    }
} catch {
    Write-Warning "Could not retrieve task information"
}

# Cleanup
Remove-Item "task-definition-updated.json" -ErrorAction SilentlyContinue

Write-Host ""
Write-Host "🎉 Banking System Deployment Summary:" -ForegroundColor Green
Write-Host "📦 Image: $ECR_URI" -ForegroundColor Cyan
Write-Host "⚙️  Cluster: $ECS_CLUSTER" -ForegroundColor Cyan
Write-Host "🔧 Task Definition: $taskDefArn" -ForegroundColor Cyan
Write-Host "📊 Service: banking-system-service" -ForegroundColor Cyan
Write-Host ""
Write-Host "📋 Next Steps:" -ForegroundColor Yellow
Write-Host "1. Check ECS Console: https://console.aws.amazon.com/ecs/home?region=$AWS_REGION#/clusters/$ECS_CLUSTER/services" -ForegroundColor Cyan
Write-Host "2. Monitor CloudWatch Logs: https://console.aws.amazon.com/cloudwatch/home?region=$AWS_REGION#logsV2:log-groups/log-group/%2Fecs%2Fbanking-system" -ForegroundColor Cyan
Write-Host "3. If you have an ALB, check the target group health" -ForegroundColor Cyan
Write-Host "4. Test the application health endpoint: http://[PUBLIC_IP]:8080/actuator/health" -ForegroundColor Cyan