#!/bin/bash

# Setup Security Group for Banking System

AWS_REGION="us-east-1"

echo "🔒 Setting up Security Group for Banking System..."

# Get default VPC
DEFAULT_VPC=$(aws ec2 describe-vpcs --filters "Name=isDefault,Values=true" --query 'Vpcs[0].VpcId' --output text --region $AWS_REGION)

if [ "$DEFAULT_VPC" = "None" ] || [ -z "$DEFAULT_VPC" ]; then
    echo "❌ No default VPC found. Please create a VPC first."
    exit 1
fi

echo "✅ Using VPC: $DEFAULT_VPC"

# Create Security Group
SG_ID=$(aws ec2 create-security-group \
    --group-name banking-system-sg \
    --description "Security group for Banking System ECS tasks" \
    --vpc-id $DEFAULT_VPC \
    --region $AWS_REGION \
    --query 'GroupId' \
    --output text 2>/dev/null)

if [ $? -eq 0 ]; then
    echo "✅ Security Group created: $SG_ID"
else
    # Security group might already exist
    SG_ID=$(aws ec2 describe-security-groups \
        --filters "Name=group-name,Values=banking-system-sg" "Name=vpc-id,Values=$DEFAULT_VPC" \
        --query 'SecurityGroups[0].GroupId' \
        --output text \
        --region $AWS_REGION)
    echo "✅ Using existing Security Group: $SG_ID"
fi

# Add inbound rules
echo "Adding inbound rules..."

# Allow HTTP traffic on port 8080
aws ec2 authorize-security-group-ingress \
    --group-id $SG_ID \
    --protocol tcp \
    --port 8080 \
    --cidr 0.0.0.0/0 \
    --region $AWS_REGION 2>/dev/null || echo "Port 8080 rule already exists"

# Allow HTTPS traffic on port 443 (if using ALB)
aws ec2 authorize-security-group-ingress \
    --group-id $SG_ID \
    --protocol tcp \
    --port 443 \
    --cidr 0.0.0.0/0 \
    --region $AWS_REGION 2>/dev/null || echo "Port 443 rule already exists"

# Allow HTTP traffic on port 80 (if using ALB)
aws ec2 authorize-security-group-ingress \
    --group-id $SG_ID \
    --protocol tcp \
    --port 80 \
    --cidr 0.0.0.0/0 \
    --region $AWS_REGION 2>/dev/null || echo "Port 80 rule already exists"

echo "✅ Security Group setup complete!"
echo "Security Group ID: $SG_ID"
echo "VPC ID: $DEFAULT_VPC"

# Get subnets
echo "Available subnets in your VPC:"
aws ec2 describe-subnets \
    --filters "Name=vpc-id,Values=$DEFAULT_VPC" \
    --query 'Subnets[*].{SubnetId:SubnetId,AvailabilityZone:AvailabilityZone,CidrBlock:CidrBlock}' \
    --output table \
    --region $AWS_REGION