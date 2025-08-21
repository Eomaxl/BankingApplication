@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

echo 🚀 Complete Banking System Deployment
echo.

REM Configuration
set AWS_REGION=us-east-1
set AWS_ACCOUNT_ID=655593806999
set ECR_REPOSITORY=eomaxl/banking-system-registry
set ECS_CLUSTER=bank-app-cluster
set IMAGE_TAG=latest

REM Step 1: Create IAM Roles
echo ✅ Step 1: Creating IAM Roles...
call aws-deployment\fix-iam-roles.bat

REM Step 2: Build and Push Image
echo.
echo ✅ Step 2: Building Application...
call mvn clean package -DskipTests
if errorlevel 1 (
    echo ❌ Build failed
    pause
    exit /b 1
)

echo ✅ Step 3: Building Docker Image...
docker build -t banking-system:%IMAGE_TAG% .

echo ✅ Step 4: ECR Login and Push...
REM Get ECR login token and login
for /f "tokens=*" %%i in ('aws ecr get-login-password --region %AWS_REGION%') do (
    echo %%i | docker login --username AWS --password-stdin %AWS_ACCOUNT_ID%.dkr.ecr.%AWS_REGION%.amazonaws.com
)

REM Tag and push image
set ECR_URI=%AWS_ACCOUNT_ID%.dkr.ecr.%AWS_REGION%.amazonaws.com/%ECR_REPOSITORY%:%IMAGE_TAG%
docker tag banking-system:%IMAGE_TAG% !ECR_URI!
docker push !ECR_URI!

echo ✅ Step 5: Getting VPC Information...
REM Get default VPC
for /f "tokens=*" %%i in ('aws ec2 describe-vpcs --filters "Name=isDefault,Values=true" --query "Vpcs[0].VpcId" --output text --region %AWS_REGION%') do set VPC_ID=%%i

echo Default VPC: !VPC_ID!

REM Get subnets
echo Getting subnets...
for /f "tokens=1,2" %%a in ('aws ec2 describe-subnets --filters "Name=vpc-id,Values=!VPC_ID!" --query "Subnets[0:2].[SubnetId]" --output text --region %AWS_REGION%') do (
    if not defined SUBNET1 (
        set SUBNET1=%%a
    ) else (
        set SUBNET2=%%a
    )
)

echo Subnet 1: !SUBNET1!
echo Subnet 2: !SUBNET2!

REM Step 6: Create Security Group
echo ✅ Step 6: Creating Security Group...
for /f "tokens=*" %%i in ('aws ec2 create-security-group --group-name banking-system-sg --description "Banking System Security Group" --vpc-id !VPC_ID! --query "GroupId" --output text --region %AWS_REGION% 2^>nul') do set SG_ID=%%i

if "!SG_ID!"=="" (
    REM Security group might already exist
    for /f "tokens=*" %%i in ('aws ec2 describe-security-groups --filters "Name=group-name,Values=banking-system-sg" "Name=vpc-id,Values=!VPC_ID!" --query "SecurityGroups[0].GroupId" --output text --region %AWS_REGION%') do set SG_ID=%%i
)

echo Security Group: !SG_ID!

REM Add security group rules
aws ec2 authorize-security-group-ingress --group-id !SG_ID! --protocol tcp --port 8080 --cidr 0.0.0.0/0 --region %AWS_REGION% 2>nul || echo Port 8080 rule already exists

REM Step 7: Create CloudWatch Log Group
echo ✅ Step 7: Creating CloudWatch Log Group...
aws logs create-log-group --log-group-name "/ecs/banking-system" --region %AWS_REGION% 2>nul || echo Log group already exists

REM Step 8: Register Task Definition
echo ✅ Step 8: Registering Task Definition...
powershell -Command "(Get-Content 'aws-deployment\task-definition.json') -replace '655593806999\.dkr\.ecr\.us-east-1\.amazonaws\.com/eomaxl/banking-system-registry:latest', '!ECR_URI!' | Set-Content 'task-definition-updated.json'"

for /f "tokens=*" %%i in ('aws ecs register-task-definition --cli-input-json file://task-definition-updated.json --region %AWS_REGION% --query "taskDefinition.taskDefinitionArn" --output text') do set TASK_DEF_ARN=%%i

echo Task Definition: !TASK_DEF_ARN!

REM Step 9: Create ECS Service
echo ✅ Step 9: Creating ECS Service...
aws ecs create-service ^
  --cluster %ECS_CLUSTER% ^
  --service-name banking-system-service ^
  --task-definition !TASK_DEF_ARN! ^
  --desired-count 2 ^
  --launch-type FARGATE ^
  --network-configuration "awsvpcConfiguration={subnets=[!SUBNET1!,!SUBNET2!],securityGroups=[!SG_ID!],assignPublicIp=ENABLED}" ^
  --region %AWS_REGION%

if errorlevel 1 (
    echo ⚠️ Service might already exist, trying to update...
    aws ecs update-service --cluster %ECS_CLUSTER% --service banking-system-service --task-definition !TASK_DEF_ARN! --region %AWS_REGION%
)

REM Step 10: Wait for service to stabilize
echo ✅ Step 10: Waiting for service to stabilize...
aws ecs wait services-stable --cluster %ECS_CLUSTER% --services banking-system-service --region %AWS_REGION%

REM Step 11: Get service information
echo ✅ Step 11: Getting service information...
for /f "tokens=*" %%i in ('aws ecs list-tasks --cluster %ECS_CLUSTER% --service-name banking-system-service --region %AWS_REGION% --query "taskArns[0]" --output text') do set TASK_ARN=%%i

if not "!TASK_ARN!"=="None" (
    echo Task ARN: !TASK_ARN!

    REM Get task details and public IP
    for /f "tokens=*" %%i in ('aws ecs describe-tasks --cluster %ECS_CLUSTER% --tasks !TASK_ARN! --region %AWS_REGION% --query "tasks[0].attachments[0].details[?name=='networkInterfaceId'].value" --output text') do set ENI_ID=%%i

    if not "!ENI_ID!"=="" (
        for /f "tokens=*" %%i in ('aws ec2 describe-network-interfaces --network-interface-ids !ENI_ID! --query "NetworkInterfaces[0].Association.PublicIp" --output text --region %AWS_REGION%') do set PUBLIC_IP=%%i

        echo Public IP: !PUBLIC_IP!
        echo.
        echo 🎉 Deployment Summary:
        echo 📦 Image: !ECR_URI!
        echo 🌐 Public IP: !PUBLIC_IP!
        echo 🔗 Health Check: http://!PUBLIC_IP!:8080/actuator/health
        echo 📚 Swagger UI: http://!PUBLIC_IP!:8080/swagger-ui.html
        echo.
        echo Testing health endpoint...
        timeout /t 30 /nobreak >nul
        curl -f http://!PUBLIC_IP!:8080/actuator/health || echo Health check will be available shortly...
    )
)

REM Cleanup
del task-definition-updated.json 2>nul

echo.
echo ✅ Deployment completed!
pause