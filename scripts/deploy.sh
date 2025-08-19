#!/bin/bash

# Banking system deployment script
set -e

# Configuration
ENVIRONMENT=${1:-dev}
AWS_REGION=
AWS_ACCOUNT_ID=
ECR_REPOSITORY=
IMAGE_TAG=

echo " Starting deployment for environment: $ENVIRONMENT"
echo " Region: $AWS_REGION"
echo " Image tag: $IMAGE_TAG"

#Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color


# Function to print colored output
print_status() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

# Check if AWS CLI is configured
if ! aws sts get-caller-identity > /dev/null 2>&1; then
    print_error "AWS CLI is not configured. Please run 'aws configure' first."
    exit 1
fi

print_status "AWS CLI is configured"

# Create ECR repository if it doesn't exist
print_status "Checking ECR repository..."
if ! aws ecr describe-repositories --repository-names $ECR_REPOSITORY --region $AWS_REGION > /dev/null 2>&1; then
    print_warning "ECR repository doesn't exist. Creating..."
    aws ecr create-repository \
        --repository-name $ECR_REPOSITORY \
        --region $AWS_REGION \
        --image-scanning-configuration scanOnPush=true \
        --encryption-configuration encryptionType=AES256
    print_status "ECR repository created"
else
    print_status "ECR repository exists"
fi

# Get ECR login token
print_status "Logging into ECR..."
aws ecr get-login-password --region $AWS_REGION | docker login --username AWS --password-stdin $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com

# Build Docker image
print_status "Building Docker image..."
docker build -t $ECR_REPOSITORY:$IMAGE_TAG .

# Tag image for ECR
ECR_URI="$AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/$ECR_REPOSITORY:$IMAGE_TAG"
docker tag $ECR_REPOSITORY:$IMAGE_TAG $ECR_URI

# Push image to ECR
print_status "Pushing image to ECR..."
docker push $ECR_URI

print_status "Image pushed: $ECR_URI"

# Deploy infrastructure
print_status "Deploying infrastructure..."
aws cloudformation deploy \
    --template-file infrastructure/cloudformation/banking-infrastructure.yml \
    --stack-name banking-infrastructure-$ENVIRONMENT \
    --parameter-overrides \
        Environment=$ENVIRONMENT \
        DBPassword=$(openssl rand -base64 32) \
    --capabilities CAPABILITY_NAMED_IAM \
    --region $AWS_REGION

print_status "Infrastructure deployed"

# Get RDS endpoint
RDS_ENDPOINT=$(aws cloudformation describe-stacks \
    --stack-name banking-infrastructure-$ENVIRONMENT \
    --region $AWS_REGION \
    --query 'Stacks[0].Outputs[?OutputKey==`RDSEndpoint`].OutputValue' \
    --output text)

print_status "RDS Endpoint: $RDS_ENDPOINT"

# Deploy application
print_status "Deploying application..."
aws cloudformation deploy \
    --template-file infrastructure/cloudformation/banking-application.yml \
    --stack-name banking-application-$ENVIRONMENT \
    --parameter-overrides \
        Environment=$ENVIRONMENT \
        ImageURI=$ECR_URI \
        DBPassword=$(aws secretsmanager get-secret-value --secret-id banking-db-password-$ENVIRONMENT --query SecretString --output text 2>/dev/null || echo "defaultpassword") \
        AdminUsername=admin \
        AdminPassword=$(openssl rand -base64 32) \
        JWTSecret=$(openssl rand -base64 64) \
    --capabilities CAPABILITY_IAM \
    --region $AWS_REGION

print_status "Application deployed"

# Get ALB DNS name
ALB_DNS=$(aws cloudformation describe-stacks \
    --stack-name banking-infrastructure-$ENVIRONMENT \
    --region $AWS_REGION \
    --query 'Stacks[0].Outputs[?OutputKey==`ALBDNSName`].OutputValue' \
    --output text)

print_status "Application URL: http://$ALB_DNS"

# Wait for service to be stable
print_status "Waiting for service to be stable..."
aws ecs wait services-stable \
    --cluster banking-cluster-$ENVIRONMENT \
    --services banking-service-$ENVIRONMENT \
    --region $AWS_REGION

print_status "Service is stable"

# Health check
print_status "Performing health check..."
sleep 30
if curl -f http://$ALB_DNS/actuator/health > /dev/null 2>&1; then
    print_status "Health check passed"
else
    print_warning "Health check failed - service might still be starting up"
fi

echo ""
echo "🎉 Deployment completed successfully!"
echo "📊 Application URL: http://$ALB_DNS"
echo "📚 API Documentation: http://$ALB_DNS/swagger-ui.html"
echo "❤️  Health Check: http://$ALB_DNS/actuator/health"
echo ""
echo "📋 Next steps:"
echo "   1. Configure DNS (Route 53) to point to the ALB"
echo "   2. Set up SSL certificate (ACM) for HTTPS"
echo "   3. Configure monitoring and alerting"
echo "   4. Set up CI/CD pipeline"
echo ""