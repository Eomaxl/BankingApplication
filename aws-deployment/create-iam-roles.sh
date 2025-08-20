#!/bin/bash

# Create IAM roles for ECS if they don't exist

AWS_REGION="us-east-1"
AWS_ACCOUNT_ID="655593806999"

echo "🔐 Creating IAM Roles for ECS..."

# Create ECS Task Execution Role
cat > /tmp/ecs-task-execution-trust-policy.json << EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Service": "ecs-tasks.amazonaws.com"
      },
      "Action": "sts:AssumeRole"
    }
  ]
}
EOF

# Check if role exists
if ! aws iam get-role --role-name ecsTaskExecutionRole 2>/dev/null; then
    echo "Creating ecsTaskExecutionRole..."
    aws iam create-role \
        --role-name ecsTaskExecutionRole \
        --assume-role-policy-document file:///tmp/ecs-task-execution-trust-policy.json

    aws iam attach-role-policy \
        --role-name ecsTaskExecutionRole \
        --policy-arn arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy

    echo "✅ ecsTaskExecutionRole created"
else
    echo "✅ ecsTaskExecutionRole already exists"
fi

# Create ECS Task Role
if ! aws iam get-role --role-name ecsTaskRole 2>/dev/null; then
    echo "Creating ecsTaskRole..."
    aws iam create-role \
        --role-name ecsTaskRole \
        --assume-role-policy-document file:///tmp/ecs-task-execution-trust-policy.json

    echo "✅ ecsTaskRole created"
else
    echo "✅ ecsTaskRole already exists"
fi

echo "🎉 IAM Roles setup complete!"