#!/bin/bash

# Banking System AWS Deployment Script
# Customized for your existing AWS infrastructure

set -euo pipefail

# Configuration - Update these with your actual values
AWS_REGION="us-east-1"
AWS_ACCOUNT_ID="655593806999"
ECR_REPOSITORY="eomaxl/banking-system-registry"
ECS_CLUSTER="bank-app-cluster"
IMAGE_TAG=${1:-latest}

echo "🚀 Starting Banking System Deployment"
echo "📍 Region: $AWS_REGION"
echo "🏷️  Image Tag: $IMAGE_TAG"
echo "🏦 ECR Repository: $ECR_REPOSITORY"
echo "⚙️  ECS Cluster: $ECS_CLUSTER"

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

print_status() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

# Step 1: Check AWS CLI configuration
if ! aws sts get-caller-identity > /dev/null 2>&1; then
    print_error "AWS CLI is not configured. Please run 'aws configure' first."
    exit 1
fi

print_status "AWS CLI is configured"

# Step 2: Build the application
print_status "Building Spring Boot application..."
mvn clean install -DskipTests

# mvn clean package -DskipTests
#if [ -x "./mvnw" ]; then
#  ./mvnw -B -DskipTests clean package
#elif command -v mvn.cmd >/dev/null 2>&1; then
#  mvn.cmd -B -DskipTests clean package
#elif command -v mvn >/dev/null 2>&1; then
#  mvn -B -DskipTests clean package
#else
#  echo "No local Maven found; skipping local build (Docker will build the app)..."
#fi
#

if [ ! -f "target/banking-system-1.0.0.jar" ]; then
    print_error "JAR file not found. Build failed."
    exit 1
fi

# --- pick the built jar (optional; Docker build may not need this) ---
ARTIFACT_JAR="$(ls -1 "${PROJECT_ROOT}/target"/*.jar 2>/dev/null | grep -vE 'original|sources|javadoc|plain' | head -n1)"
[ -f "${ARTIFACT_JAR}" ] && echo "Built JAR: ${ARTIFACT_JAR}"

# --- Docker build must use the project root as context ---
# assumes Dockerfile is at PROJECT_ROOT/Dockerfile
docker build -f "${PROJECT_ROOT}/Dockerfile" -t "${IMAGE_URI}" "${PROJECT_ROOT}"

print_status "Application built successfully"

# Step 3: Build Docker image
print_status "Building Docker image..."
docker build -t banking-system:$IMAGE_TAG .

# Step 4: Tag image for ECR
ECR_URI="$AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/$ECR_REPOSITORY:$IMAGE_TAG"
docker tag banking-system:$IMAGE_TAG $ECR_URI

print_status "Docker image tagged: $ECR_URI"

# Step 5: Login to ECR
print_status "Logging into ECR..."
aws ecr get-login-password --region $AWS_REGION | docker login --username AWS --password-stdin $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com

# Step 6: Push image to ECR
print_status "Pushing image to ECR..."
docker push $ECR_URI

print_status "Image pushed successfully"

# Step 7: Create CloudWatch Log Group
print_status "Creating CloudWatch Log Group..."
aws logs create-log-group --log-group-name "/ecs/banking-system" --region $AWS_REGION 2>/dev/null || print_warning "Log group already exists"

# Step 8: Create or update ECS Task Definition
print_status "Registering ECS Task Definition..."

# Update the task definition with the new image
sed "s|655593806999.dkr.ecr.us-east-1.amazonaws.com/eomaxl/banking-system-registry:latest|$ECR_URI|g" aws-deployment/task-definition.json > /tmp/task-definition-updated.json

TASK_DEFINITION_ARN=$(aws ecs register-task-definition \
    --cli-input-json file:///tmp/task-definition-updated.json \
    --region $AWS_REGION \
    --query 'taskDefinition.taskDefinitionArn' \
    --output text)

print_status "Task Definition registered: $TASK_DEFINITION_ARN"

# Step 9: Update ECS Service (or create if it doesn't exist)
print_status "Updating ECS Service..."

# Check if service exists
if aws ecs describe-services --cluster $ECS_CLUSTER --services banking-system-service --region $AWS_REGION --query 'services[0].serviceName' --output text 2>/dev/null | grep -q "banking-system-service"; then
    print_status "Service exists, updating..."

    aws ecs update-service \
        --cluster $ECS_CLUSTER \
        --service banking-system-service \
        --task-definition $TASK_DEFINITION_ARN \
        --region $AWS_REGION \
        --query 'service.serviceName' \
        --output text

    print_status "Service updated successfully"
else
    print_warning "Service doesn't exist. You'll need to create it manually or provide VPC/subnet details."
    echo "To create the service, you need:"
    echo "1. VPC Subnets (at least 2 in different AZs)"
    echo "2. Security Group allowing inbound traffic on port 8080"
    echo "3. Application Load Balancer (optional but recommended)"

    echo ""
    echo "Example command to create service:"
    echo "aws ecs create-service \\"
    echo "  --cluster $ECS_CLUSTER \\"
    echo "  --service-name banking-system-service \\"
    echo "  --task-definition $TASK_DEFINITION_ARN \\"
    echo "  --desired-count 2 \\"
    echo "  --launch-type FARGATE \\"
    echo "  --network-configuration 'awsvpcConfiguration={subnets=[subnet-xxx,subnet-yyy],securityGroups=[sg-xxx],assignPublicIp=ENABLED}'"
fi

# Step 10: Wait for deployment to complete
print_status "Waiting for service to stabilize..."
aws ecs wait services-stable --cluster $ECS_CLUSTER --services banking-system-service --region $AWS_REGION

print_status "Deployment completed successfully!"

# Step 11: Get service information
print_status "Getting service information..."
TASKS=$(aws ecs list-tasks --cluster $ECS_CLUSTER --service-name banking-system-service --region $AWS_REGION --query 'taskArns' --output text)

if [ ! -z "$TASKS" ]; then
    print_status "Service is running with tasks: $TASKS"

    # Get task details
    aws ecs describe-tasks --cluster $ECS_CLUSTER --tasks $TASKS --region $AWS_REGION --query 'tasks[0].{TaskArn:taskArn,LastStatus:lastStatus,HealthStatus:healthStatus}'
else
    print_warning "No tasks found. Service might be starting up."
fi

echo ""
echo "🎉 Banking System Deployment Summary:"
echo "📦 Image: $ECR_URI"
echo "⚙️  Cluster: $ECS_CLUSTER"
echo "🔧 Task Definition: $TASK_DEFINITION_ARN"
echo "📊 Service: banking-system-service"
echo ""
echo "📋 Next Steps:"
echo "1. Check ECS Console: https://console.aws.amazon.com/ecs/home?region=$AWS_REGION#/clusters/$ECS_CLUSTER/services"
echo "2. Monitor CloudWatch Logs: https://console.aws.amazon.com/cloudwatch/home?region=$AWS_REGION#logsV2:log-groups/log-group/%2Fecs%2Fbanking-system"
echo "3. If you have an ALB, check the target group health"
echo "4. Test the application health endpoint: http://[PUBLIC_IP]:8080/actuator/health"
echo ""