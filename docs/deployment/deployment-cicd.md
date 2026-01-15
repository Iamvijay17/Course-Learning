# Deployment & CI/CD

## Overview

The platform implements a comprehensive CI/CD pipeline using GitHub Actions, containerization with Docker, and automated deployment strategies to ensure reliable, scalable, and secure delivery of the microservices-based e-learning platform.

## Containerization Strategy

### Docker Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Frontend      │    │   API Gateway   │    │   Microservices │
│   (nginx)       │    │   (Spring Cloud │◄──►│   (Spring Boot) │
│                 │    │   Gateway)      │    │                 │
└─────────────────┘    └─────────────────┘    └─────────────────┘
          │                       │                       │
          └───────────────────────┼───────────────────────┘
                                  │
                    ┌─────────────────┐
                    │   Databases     │
                    │   (MySQL)       │
                    └─────────────────┘
```

### Dockerfile Examples

#### Backend Services (Spring Boot)

```dockerfile
# Multi-stage build for Spring Boot services
FROM maven:3.8.4-openjdk-17-slim AS build

WORKDIR /app
COPY pom.xml .
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Runtime stage
FROM openjdk:17-jre-slim

WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Add health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

#### Frontend (React)

```dockerfile
# Multi-stage build for React application
FROM node:18-alpine AS build

WORKDIR /app
COPY package*.json ./
RUN npm ci --only=production

COPY . .
RUN npm run build

# Production stage with nginx
FROM nginx:alpine

# Copy built assets
COPY --from=build /app/dist /usr/share/nginx/html

# Copy nginx configuration
COPY nginx.conf /etc/nginx/nginx.conf

# Add health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost/health || exit 1

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

#### API Gateway

```dockerfile
FROM openjdk:17-jre-slim

WORKDIR /app
COPY target/api-gateway-*.jar app.jar

# Environment variables
ENV SPRING_PROFILES_ACTIVE=docker
ENV EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://eureka-server:8761/eureka/

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Docker Compose (Development)

```yaml
# docker-compose.yml
version: '3.8'

services:
  # Databases
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: rootpassword
      MYSQL_DATABASE: course_platform
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql
      - ./docker/mysql/init:/docker-entrypoint-initdb.d

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data

  # Microservices
  user-service:
    build:
      context: ./user-service
      dockerfile: Dockerfile
    environment:
      SPRING_PROFILES_ACTIVE: docker
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/user_db
    depends_on:
      - mysql
    ports:
      - "8081:8080"

  course-service:
    build:
      context: ./course-service
      dockerfile: Dockerfile
    environment:
      SPRING_PROFILES_ACTIVE: docker
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/course_db
    depends_on:
      - mysql
    ports:
      - "8082:8080"

  enrollment-service:
    build:
      context: ./enrollment-service
      dockerfile: Dockerfile
    environment:
      SPRING_PROFILES_ACTIVE: docker
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/enrollment_db
    depends_on:
      - mysql
    ports:
      - "8083:8080"

  # API Gateway
  api-gateway:
    build:
      context: ./api-gateway
      dockerfile: Dockerfile
    ports:
      - "8080:8080"
    depends_on:
      - user-service
      - course-service
      - enrollment-service

  # Frontend
  frontend:
    build:
      context: ./frontend
      dockerfile: Dockerfile
    ports:
      - "3000:80"
    depends_on:
      - api-gateway

volumes:
  mysql_data:
  redis_data:
```

## CI/CD Pipeline

### GitHub Actions Workflow

```yaml
# .github/workflows/cicd.yml
name: CI/CD Pipeline

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

env:
  REGISTRY: ghcr.io
  IMAGE_NAME: ${{ github.repository }}

jobs:
  # Testing and Quality Gates
  test:
    runs-on: ubuntu-latest
    services:
      mysql:
        image: mysql:8.0
        env:
          MYSQL_ROOT_PASSWORD: test
          MYSQL_DATABASE: test_db
        ports:
          - 3306:3306
        options: --health-cmd="mysqladmin ping" --health-interval=10s --health-timeout=5s --health-retries=3

    steps:
      - name: Checkout code
        uses: actions/checkout@v3

      - name: Setup Java
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Setup Node.js
        uses: actions/setup-node@v3
        with:
          node-version: '18'
          cache: 'npm'
          cache-dependency-path: 'frontend/package-lock.json'

      - name: Cache Maven dependencies
        uses: actions/cache@v3
        with:
          path: ~/.m2
          key: ${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}
          restore-keys: ${{ runner.os }}-m2

      - name: Run backend tests
        run: |
          cd backend
          mvn test -Dspring.profiles.active=test

      - name: Run frontend tests
        run: |
          cd frontend
          npm ci
          npm run test:ci

      - name: Generate test reports
        uses: dorny/test-reporter@v1
        if: success() || failure()
        with:
          name: Test Results
          path: '**/target/surefire-reports/*.xml'
          reporter: java-junit

  # Security Scanning
  security:
    runs-on: ubuntu-latest
    needs: test

    steps:
      - name: Checkout code
        uses: actions/checkout@v3

      - name: Run Trivy vulnerability scanner
        uses: aquasecurity/trivy-action@master
        with:
          scan-type: 'fs'
          scan-ref: '.'
          format: 'sarif'
          output: 'trivy-results.sarif'

      - name: Upload Trivy scan results
        uses: github/codeql-action/upload-sarif@v2
        if: always()
        with:
          sarif_file: 'trivy-results.sarif'

  # Build and Push Docker Images
  build:
    runs-on: ubuntu-latest
    needs: [test, security]
    permissions:
      contents: read
      packages: write

    steps:
      - name: Checkout code
        uses: actions/checkout@v3

      - name: Log in to Container Registry
        uses: docker/login-action@v2
        with:
          registry: ${{ env.REGISTRY }}
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Extract metadata
        id: meta
        uses: docker/metadata-action@v4
        with:
          images: ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}
          tags: |
            type=ref,event=branch
            type=ref,event=pr
            type=sha,prefix={{branch}}-
            type=raw,value=latest,enable={{is_default_branch}}

      - name: Build and push user-service
        uses: docker/build-push-action@v4
        with:
          context: ./user-service
          push: true
          tags: ${{ steps.meta.outputs.tags }}-user-service
          labels: ${{ steps.meta.outputs.labels }}
          cache-from: type=gha
          cache-to: type=gha,mode=max

      - name: Build and push course-service
        uses: docker/build-push-action@v4
        with:
          context: ./course-service
          push: true
          tags: ${{ steps.meta.outputs.tags }}-course-service
          labels: ${{ steps.meta.outputs.labels }}

      - name: Build and push enrollment-service
        uses: docker/build-push-action@v4
        with:
          context: ./enrollment-service
          push: true
          tags: ${{ steps.meta.outputs.tags }}-enrollment-service
          labels: ${{ steps.meta.outputs.labels }}

      - name: Build and push api-gateway
        uses: docker/build-push-action@v4
        with:
          context: ./api-gateway
          push: true
          tags: ${{ steps.meta.outputs.tags }}-api-gateway
          labels: ${{ steps.meta.outputs.labels }}

      - name: Build and push frontend
        uses: docker/build-push-action@v4
        with:
          context: ./frontend
          push: true
          tags: ${{ steps.meta.outputs.tags }}-frontend
          labels: ${{ steps.meta.outputs.labels }}

  # Deploy to Staging
  deploy-staging:
    runs-on: ubuntu-latest
    needs: build
    if: github.ref == 'refs/heads/develop'
    environment: staging

    steps:
      - name: Checkout code
        uses: actions/checkout@v3

      - name: Deploy to staging
        run: |
          echo "Deploying to staging environment"
          # Add deployment commands here

  # Deploy to Production
  deploy-production:
    runs-on: ubuntu-latest
    needs: build
    if: github.ref == 'refs/heads/main'
    environment: production

    steps:
      - name: Checkout code
        uses: actions/checkout@v3

      - name: Deploy to production
        run: |
          echo "Deploying to production environment"
          # Add deployment commands here
```

### Quality Gates

```yaml
# Code Quality Checks
- name: SonarQube Scan
  uses: sonarsource/sonarqube-scan-action@v1
  with:
    projectBaseDir: .
    args: >
      -Dsonar.projectKey=course-platform
      -Dsonar.sources=.
      -Dsonar.exclusions=**/node_modules/**,**/target/**,**/*.test.*
      -Dsonar.tests=.
      -Dsonar.test.inclusions=**/*.test.*,**/*.spec.*
      -Dsonar.javascript.lcov.reportPaths=coverage/lcov.info
      -Dsonar.java.binaries=target/classes

# Coverage Requirements
- name: Check test coverage
  run: |
    BACKEND_COVERAGE=$(find . -name 'jacoco.xml' -exec grep -oP '(?<=<counter type="LINE" covered=")[^"]*' {} \; | awk '{sum+=$1} END {print sum}')
    FRONTEND_COVERAGE=$(find . -name 'coverage-final.json' -exec jq -r '.total.lines.pct' {} \;)

    if (( $(echo "$BACKEND_COVERAGE < 80" | bc -l) )); then
      echo "Backend test coverage is below 80%: $BACKEND_COVERAGE%"
      exit 1
    fi

    if (( $(echo "$FRONTEND_COVERAGE < 80" | bc -l) )); then
      echo "Frontend test coverage is below 80%: $FRONTEND_COVERAGE%"
      exit 1
    fi
```

## Deployment Strategies

### Blue-Green Deployment

```yaml
# Kubernetes deployment for blue-green
apiVersion: apps/v1
kind: Deployment
metadata:
  name: course-service-blue
spec:
  replicas: 3
  selector:
    matchLabels:
      app: course-service
      version: blue
  template:
    metadata:
      labels:
        app: course-service
        version: blue
    spec:
      containers:
      - name: course-service
        image: ghcr.io/org/course-platform:main-course-service
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "production"
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 30
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
```

### Service Mesh (Istio)

```yaml
# Istio Gateway
apiVersion: networking.istio.io/v1beta1
kind: Gateway
metadata:
  name: course-platform-gateway
spec:
  selector:
    istio: ingressgateway
  servers:
  - port:
      number: 80
      name: http
      protocol: HTTP
    hosts:
    - "courseplatform.com"
  - port:
      number: 443
      name: https
      protocol: HTTPS
    tls:
      mode: SIMPLE
      credentialName: course-platform-tls
    hosts:
    - "courseplatform.com"

# Virtual Service for traffic routing
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: course-platform
spec:
  hosts:
  - "courseplatform.com"
  gateways:
  - course-platform-gateway
  http:
  - match:
    - uri:
        prefix: "/api"
    route:
    - destination:
        host: api-gateway
  - match:
    - uri:
        prefix: "/"
    route:
    - destination:
        host: frontend
```

### Canary Deployment

```yaml
# Istio DestinationRule for canary
apiVersion: networking.istio.io/v1beta1
kind: DestinationRule
metadata:
  name: course-service
spec:
  host: course-service
  subsets:
  - name: v1
    labels:
      version: v1
  - name: v2
    labels:
      version: v2

# VirtualService for canary routing
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: course-service
spec:
  hosts:
  - course-service
  http:
  - route:
    - destination:
        host: course-service
        subset: v2
      weight: 20  # 20% traffic to v2
    - destination:
        host: course-service
        subset: v1
      weight: 80  # 80% traffic to v1
```

## Infrastructure as Code

### Terraform Configuration

```hcl
# main.tf
terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 4.0"
    }
  }
}

provider "aws" {
  region = var.aws_region
}

# VPC Configuration
module "vpc" {
  source = "terraform-aws-modules/vpc/aws"

  name = "course-platform-vpc"
  cidr = "10.0.0.0/16"

  azs             = ["${var.aws_region}a", "${var.aws_region}b", "${var.aws_region}c"]
  private_subnets = ["10.0.1.0/24", "10.0.2.0/24", "10.0.3.0/24"]
  public_subnets  = ["10.0.101.0/24", "10.0.102.0/24", "10.0.103.0/24"]

  enable_nat_gateway = true
  enable_vpn_gateway = false

  tags = {
    Environment = var.environment
    Project     = "course-platform"
  }
}

# EKS Cluster
module "eks" {
  source  = "terraform-aws-modules/eks/aws"
  version = "~> 19.0"

  cluster_name    = "course-platform-${var.environment}"
  cluster_version = "1.27"

  vpc_id     = module.vpc.vpc_id
  subnet_ids = module.vpc.private_subnets

  eks_managed_node_groups = {
    general = {
      desired_size = 3
      min_size     = 1
      max_size     = 10

      instance_types = ["t3.medium"]
      capacity_type  = "ON_DEMAND"
    }
  }

  tags = {
    Environment = var.environment
    Project     = "course-platform"
  }
}

# RDS MySQL
module "db" {
  source  = "terraform-aws-modules/rds/aws"
  version = "~> 5.0"

  identifier = "course-platform-${var.environment}"

  engine            = "mysql"
  engine_version    = "8.0"
  instance_class    = "db.t3.micro"
  allocated_storage = 20

  db_name  = "courseplatform"
  username = var.db_username
  password = var.db_password
  port     = "3306"

  vpc_security_group_ids = [aws_security_group.rds.id]
  subnet_ids             = module.vpc.private_subnets

  family = "mysql8.0"

  tags = {
    Environment = var.environment
    Project     = "course-platform"
  }
}

# ElastiCache Redis
module "redis" {
  source = "terraform-aws-modules/elasticache/aws"

  cluster_id      = "course-platform-${var.environment}"
  engine          = "redis"
  node_type       = "cache.t3.micro"
  num_cache_nodes = 1
  port            = 6379

  subnet_ids = module.vpc.private_subnets
  security_group_ids = [aws_security_group.redis.id]

  tags = {
    Environment = var.environment
    Project     = "course-platform"
  }
}
```

### Helm Charts

```yaml
# Chart.yaml
apiVersion: v2
name: course-platform
description: A Helm chart for Course E-Learning Platform
type: application
version: 0.1.0
appVersion: "1.0.0"

# values.yaml
replicaCount: 3

image:
  repository: ghcr.io/org/course-platform
  tag: "latest"
  pullPolicy: IfNotPresent

service:
  type: ClusterIP
  port: 80

ingress:
  enabled: true
  className: "nginx"
  hosts:
    - host: courseplatform.com
      paths:
        - path: /
          pathType: Prefix

resources:
  limits:
    cpu: 500m
    memory: 512Mi
  requests:
    cpu: 250m
    memory: 256Mi

autoscaling:
  enabled: true
  minReplicas: 2
  maxReplicas: 10
  targetCPUUtilizationPercentage: 70

config:
  spring:
    profiles:
      active: production
    datasource:
      url: jdbc:mysql://mysql:3306/course_db
      username: courseuser
      password: coursepass
    redis:
      host: redis
      port: 6379
```

## Monitoring & Observability

### Application Monitoring

```yaml
# Prometheus ServiceMonitor
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: course-service-monitor
  labels:
    team: backend
spec:
  selector:
    matchLabels:
      app: course-service
  endpoints:
  - port: http
    path: /actuator/prometheus
    interval: 30s
```

### Centralized Logging

```yaml
# Fluent Bit ConfigMap
apiVersion: v1
kind: ConfigMap
metadata:
  name: fluent-bit-config
data:
  fluent-bit.conf: |
    [SERVICE]
        Flush         5
        Log_Level     info
        Daemon        off

    [INPUT]
        Name              tail
        Path              /var/log/containers/*course-service*.log
        Parser            docker
        Tag               course-service.*
        Refresh_Interval  5

    [OUTPUT]
        Name  es
        Match course-service.*
        Host  elasticsearch
        Port  9200
        Index course-service
```

### Distributed Tracing

```yaml
# Jaeger Configuration
apiVersion: jaegertracing.io/v1
kind: Jaeger
metadata:
  name: course-platform-jaeger
spec:
  strategy: allInOne
  allInOne:
    image: jaegertracing/all-in-one:latest
    options:
      log-level: info
      query:
        base-path: /jaeger
```

## Backup & Disaster Recovery

### Database Backup Strategy

```bash
#!/bin/bash
# MySQL backup script
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="/backups"
MYSQL_USER="backup"
MYSQL_PASSWORD="backup_password"

# Create backup
mysqldump -u$MYSQL_USER -p$MYSQL_PASSWORD \
  --all-databases \
  --single-transaction \
  --routines \
  --triggers \
  > $BACKUP_DIR/course_platform_$DATE.sql

# Compress backup
gzip $BACKUP_DIR/course_platform_$DATE.sql

# Upload to S3
aws s3 cp $BACKUP_DIR/course_platform_$DATE.sql.gz s3://course-platform-backups/

# Clean up old backups (keep last 30 days)
find $BACKUP_DIR -name "*.sql.gz" -mtime +30 -delete
```

### Disaster Recovery Plan

1. **RTO (Recovery Time Objective)**: 4 hours
2. **RPO (Recovery Point Objective)**: 1 hour
3. **Backup Frequency**: 
   - Full backup: Daily
   - Incremental backup: Hourly
   - Log backup: Every 15 minutes

4. **Recovery Steps**:
   - Restore from latest backup
   - Apply transaction logs
   - Update DNS records
   - Verify application functionality

## Performance Optimization

### CDN Configuration

```yaml
# CloudFront Distribution
AWSTemplateFormatVersion: '2010-09-09'
Resources:
  CloudFrontDistribution:
    Type: AWS::CloudFront::Distribution
    Properties:
      DistributionConfig:
        Origins:
        - DomainName: !Ref LoadBalancerDNS
          Id: AppOrigin
          CustomOriginConfig:
            HTTPPort: 80
            HTTPSPort: 443
            OriginProtocolPolicy: https-only
        Enabled: true
        DefaultCacheBehavior:
          TargetOriginId: AppOrigin
          ViewerProtocolPolicy: redirect-to-https
          CachePolicyId: !Ref CachePolicy
        CacheBehaviors:
        - PathPattern: '/api/*'
          TargetOriginId: AppOrigin
          ViewerProtocolPolicy: https-only
          CachePolicyId: !Ref APICachePolicy
        - PathPattern: '/static/*'
          TargetOriginId: AppOrigin
          ViewerProtocolPolicy: https-only
          CachePolicyId: !Ref StaticCachePolicy
```

### Auto Scaling

```yaml
# Horizontal Pod Autoscaler
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: course-service-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: course-service
  minReplicas: 2
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
```

This deployment and CI/CD strategy ensures reliable, scalable, and secure delivery of the microservices-based e-learning platform with comprehensive monitoring, testing, and rollback capabilities.
