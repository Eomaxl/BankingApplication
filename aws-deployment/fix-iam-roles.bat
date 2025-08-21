@echo off
echo Creating IAM Roles for ECS...

REM Create trust policy for ECS tasks
echo {> ecs-trust-policy.json
echo   "Version": "2012-10-17",>> ecs-trust-policy.json
echo   "Statement": [>> ecs-trust-policy.json
echo     {>> ecs-trust-policy.json
echo       "Effect": "Allow",>> ecs-trust-policy.json
echo       "Principal": {>> ecs-trust-policy.json
echo         "Service": "ecs-tasks.amazonaws.com">> ecs-trust-policy.json
echo       },>> ecs-trust-policy.json
echo       "Action": "sts:AssumeRole">> ecs-trust-policy.json
echo     }>> ecs-trust-policy.json
echo   ]>> ecs-trust-policy.json
echo }>> ecs-trust-policy.json

REM Create ECS Task Execution Role
echo Creating ecsTaskExecutionRole...
aws iam create-role --role-name ecsTaskExecutionRole --assume-role-policy-document file://ecs-trust-policy.json 2>nul || echo Role already exists

aws iam attach-role-policy --role-name ecsTaskExecutionRole --policy-arn arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy

REM Create ECS Task Role
echo Creating ecsTaskRole...
aws iam create-role --role-name ecsTaskRole --assume-role-policy-document file://ecs-trust-policy.json 2>nul || echo Role already exists

REM Clean up
del ecs-trust-policy.json

echo IAM Roles created successfully!
echo ecsTaskExecutionRole ARN: arn:aws:iam::655593806999:role/ecsTaskExecutionRole
echo ecsTaskRole ARN: arn:aws:iam::655593806999:role/ecsTaskRole

pause