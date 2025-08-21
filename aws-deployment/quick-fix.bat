@echo off
echo 🔧 Quick Fix for Banking System Deployment Issues
echo.

set AWS_REGION=us-east-1
set AWS_ACCOUNT_ID=655593806999
set ECR_REPOSITORY=eomaxl/banking-system-registry
set ECS_CLUSTER=bank-app-cluster

echo ✅ Step 1: Creating IAM Roles...

REM Create trust policy
echo {"Version": "2012-10-17","Statement": [{"Effect": "Allow","Principal": {"Service": "ecs-tasks.amazonaws.com"},"Action": "sts:AssumeRole"}]} > trust-policy.json

REM Create roles
aws iam create-role --role-name ecsTaskExecutionRole --assume-role-policy-document file://trust-policy.json 2>nul
aws iam attach-role-policy --role-name ecsTaskExecutionRole --policy-arn arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy

aws iam create-role --role-name ecsTaskRole --assume-role-policy-document file://trust-policy.json 2>nul

del trust-policy.json

echo ✅ Step 2: Fixing ECR Login...
REM Proper ECR login
aws ecr get-login-password --region %AWS_REGION% > ecr-token.txt
set /p ECR_TOKEN=<ecr-token.txt
echo %ECR_TOKEN% | docker login --username AWS --password-stdin %AWS_ACCOUNT_ID%.dkr.ecr.%AWS_REGION%.amazonaws.com
del ecr-token.txt

echo ✅ Step 3: Re-pushing image...
docker push %AWS_ACCOUNT_ID%.dkr.ecr.%AWS_REGION%.amazonaws.com/%ECR_REPOSITORY%:latest

echo ✅ Step 4: Getting network information...
REM Get VPC and subnets
for /f "tokens=*" %%i in ('aws ec2 describe-vpcs --filters "Name=isDefault,Values=true" --query "Vpcs[0].VpcId" --output text') do set VPC_ID=%%i
echo VPC ID: %VPC_ID%

REM Get first two subnets
aws ec2 describe-subnets --filters "Name=vpc-id,Values=%VPC_ID%" --query "Subnets[0:2].{SubnetId:SubnetId,AZ:AvailabilityZone}" --output table

echo.
echo ✅ Step 5: Create Security Group (if needed)...
aws ec2 create-security-group --group-name banking-sg --description "Banking System" --vpc-id %VPC_ID% --query "GroupId" --output text 2>nul || echo Security group might already exist

REM Get security group ID
for /f "tokens=*" %%i in ('aws ec2 describe-security-groups --filters "Name=group-name,Values=banking-sg" --query "SecurityGroups[0].GroupId" --output text') do set SG_ID=%%i
echo Security Group ID: %SG_ID%

REM Add port 8080 rule
aws ec2 authorize-security-group-ingress --group-id %SG_ID% --protocol tcp --port 8080 --cidr 0.0.0.0/0 2>nul || echo Port rule already exists

echo.
echo 📋 Manual Service Creation Command:
echo Copy and paste this command with your actual subnet IDs:
echo.
echo aws ecs create-service \
echo   --cluster %ECS_CLUSTER% \
echo   --service-name banking-system-service \
echo   --task-definition banking-system-task:1 \
echo   --desired-count 2 \
echo   --launch-type FARGATE \
echo   --network-configuration "awsvpcConfiguration={subnets=[SUBNET1,SUBNET2],securityGroups=[%SG_ID%],assignPublicIp=ENABLED}" \
echo   --region %AWS_REGION%
echo.
echo Replace SUBNET1 and SUBNET2 with actual subnet IDs from the table above.

pause