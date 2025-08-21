@echo off
REM Banking System AWS Deployment Script for Windows
REM Customized for your existing AWS infrastructure

setlocal enabledelayedexpansion

REM Configuration - Update these with your actual values
set AWS_REGION=us-east-1
set AWS_ACCOUNT_ID=655593806999
set ECR_REPOSITORY=eomaxl/banking-system-registry
set ECS_CLUSTER=bank-app-cluster
set IMAGE_TAG=%1
if "%IMAGE_TAG%"=="" set IMAGE_TAG=latest

echo 🚀 Starting Banking System Deployment
echo 📍 Region: %AWS_REGION%
echo 🏷️  Image Tag: %IMAGE_TAG%
echo 🏦 ECR Repository: %ECR_REPOSITORY%
echo ⚙️  ECS Cluster: %ECS_CLUSTER%

REM Step 1: Check AWS CLI configuration
aws sts get-caller-identity >nul 2>&1
if errorlevel 1 (
    echo ❌ AWS CLI is not configured. Please run 'aws configure' first.
    exit /b 1
)

echo ✅ AWS CLI is configured

REM Step 2: Build the application
echo ✅ Building Spring Boot application...
call mvn clean package -DskipTests

if not exist "target\BankApplication-0.0.1-SNAPSHOT.jar" (
    echo ❌ JAR file not found. Build failed.
    exit /b 1
)

echo ✅ Application built successfully

REM Step 3: Build Docker image
echo ✅ Building Docker image...
docker build -t banking-system:%IMAGE_TAG% .

REM Step 4: Tag image for ECR
set ECR_URI=%AWS_ACCOUNT_ID%.dkr.ecr.%AWS_REGION%.amazonaws.com/%ECR_REPOSITORY%:%IMAGE_TAG%
echo ECR_URI
docker tag banking-system:%IMAGE_TAG% %ECR_URI%

echo ✅ Docker image tagged: %ECR_URI%

REM Step 5: Login to ECR
echo ✅ Logging into ECR...
for /f "tokens=*" %%i in ('aws ecr get-login-password --region %AWS_REGION%') do docker login --username AWS --password-stdin %AWS_ACCOUNT_ID%.dkr.ecr.%AWS_REGION%.amazonaws.com < echo %%i

REM Step 6: Push image to ECR
echo ✅ Pushing image to ECR...
docker push %ECR_URI%

echo ✅ Image pushed successfully

REM Step 7: Create CloudWatch Log Group
echo ✅ Creating CloudWatch Log Group...
aws logs create-log-group --log-group-name "/ecs/banking-system" --region %AWS_REGION% 2>nul || echo ⚠️  Log group already exists

REM Step 8: Create temporary task definition with updated image
echo ✅ Preparing Task Definition...
powershell -Command "(Get-Content 'aws-deployment\task-definition.json') -replace '655593806999\.dkr\.ecr\.us-east-1\.amazonaws\.com/eomaxl/banking-system-registry:latest', '%ECR_URI%' | Set-Content 'task-definition-updated.json'"

REM Step 9: Register ECS Task Definition
echo ✅ Registering ECS Task Definition...
for /f "tokens=*" %%i in ('aws ecs register-task-definition --cli-input-json file://task-definition-updated.json --region %AWS_REGION% --query "taskDefinition.taskDefinitionArn" --output text') do set TASK_DEFINITION_ARN=%%i

echo ✅ Task Definition registered: %TASK_DEFINITION_ARN%

REM Step 10: Update ECS Service (or create if it doesn't exist)
echo ✅ Updating ECS Service...

REM Check if service exists
aws ecs describe-services --cluster %ECS_CLUSTER% --services banking-system-service --region %AWS_REGION% --query "services[0].serviceName" --output text 2>nul | findstr "banking-system-service" >nul
if errorlevel 1 (
    echo ⚠️  Service doesn't exist. You'll need to create it manually or provide VPC/subnet details.
    echo To create the service, you need:
    echo 1. VPC Subnets ^(at least 2 in different AZs^)
    echo 2. Security Group allowing inbound traffic on port 8080
    echo 3. Application Load Balancer ^(optional but recommended^)
    echo.
    echo Example command to create service:
    echo aws ecs create-service \
    echo   --cluster %ECS_CLUSTER% \
    echo   --service-name banking-system-service \
    echo   --task-definition %TASK_DEFINITION_ARN% \
    echo   --desired-count 2 \
    echo   --launch-type FARGATE \
    echo   --network-configuration "awsvpcConfiguration={subnets=[subnet-06e3c2ed3ba238b90,subnet-0c53ff0bdf1ece475],securityGroups=[sg-0b9faa0ae8490a3d0],assignPublicIp=ENABLED}"
) else (
    echo ✅ Service exists, updating...
    aws ecs update-service --cluster %ECS_CLUSTER% --service banking-system-service --task-definition %TASK_DEFINITION_ARN% --region %AWS_REGION% --query "service.serviceName" --output text
    echo ✅ Service updated successfully
)

REM Step 11: Wait for deployment to complete
echo ✅ Waiting for service to stabilize...
aws ecs wait services-stable --cluster %ECS_CLUSTER% --services banking-system-service --region %AWS_REGION%

echo ✅ Deployment completed successfully!

REM Step 12: Get service information
echo ✅ Getting service information...
for /f "tokens=*" %%i in ('aws ecs list-tasks --cluster %ECS_CLUSTER% --service-name banking-system-service --region %AWS_REGION% --query "taskArns" --output text') do set TASKS=%%i

if not "%TASKS%"=="" (
    echo ✅ Service is running with tasks: %TASKS%
    aws ecs describe-tasks --cluster %ECS_CLUSTER% --tasks %TASKS% --region %AWS_REGION% --query "tasks[0].{TaskArn:taskArn,LastStatus:lastStatus,HealthStatus:healthStatus}"
) else (
    echo ⚠️  No tasks found. Service might be starting up.
)

echo.
echo 🎉 Banking System Deployment Summary:
echo 📦 Image: %ECR_URI%
echo ⚙️  Cluster: %ECS_CLUSTER%
echo 🔧 Task Definition: %TASK_DEFINITION_ARN%
echo 📊 Service: banking-system-service
echo.
echo 📋 Next Steps:
echo 1. Check ECS Console: https://console.aws.amazon.com/ecs/home?region=%AWS_REGION%#/clusters/%ECS_CLUSTER%/services
echo 2. Monitor CloudWatch Logs: https://console.aws.amazon.com/cloudwatch/home?region=%AWS_REGION%#logsV2:log-groups/log-group/%%2Fecs%%2Fbanking-system
echo 3. If you have an ALB, check the target group health
echo 4. Test the application health endpoint: http://[PUBLIC_IP]:8080/actuator/health

REM Cleanup
del task-definition-updated.json 2>nul

pause