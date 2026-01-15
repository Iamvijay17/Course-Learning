# Database Schema Design

## Overview

The platform uses MySQL as the primary database with separate databases for each microservice to maintain loose coupling and independent scalability. Each service follows database-per-service pattern with optimized schemas for performance.

## Database Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   course_db     │    │   user_db       │    │   enrollment_db │
│                 │    │                 │    │                 │
│ • course        │    │ • user          │    │ • enrollment    │
│ • course_content│    │ • user_profile  │    │ • progress_track│
│ • category      │    │ • user_sessions │    │ • certificates  │
│ • tags          │    │ • roles         │    │ • payments      │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

## Course Service Database (course_db)

### Core Tables

```sql
-- Course Categories
CREATE TABLE category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) UNIQUE NOT NULL,
    description TEXT,
    parent_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (parent_id) REFERENCES category(id),
    INDEX idx_parent (parent_id)
);

-- Course Tags
CREATE TABLE tag (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) UNIQUE NOT NULL,
    color VARCHAR(7) DEFAULT '#007bff',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Courses Table (Enhanced)
CREATE TABLE course (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(255) NOT NULL,
    slug VARCHAR(255) UNIQUE NOT NULL,
    description TEXT,
    short_description VARCHAR(500),
    instructor_id BIGINT NOT NULL,
    category_id BIGINT,
    thumbnail_url VARCHAR(500),
    preview_video_url VARCHAR(500),
    price DECIMAL(10,2) DEFAULT 0.00,
    original_price DECIMAL(10,2),
    currency VARCHAR(3) DEFAULT 'USD',
    duration_hours INT,
    level ENUM('BEGINNER', 'INTERMEDIATE', 'ADVANCED') DEFAULT 'BEGINNER',
    language VARCHAR(50) DEFAULT 'English',
    status ENUM('DRAFT', 'REVIEW', 'PUBLISHED', 'ARCHIVED') DEFAULT 'DRAFT',
    is_featured BOOLEAN DEFAULT FALSE,
    is_free BOOLEAN DEFAULT FALSE,
    max_students INT,
    current_students INT DEFAULT 0,
    rating DECIMAL(3,2) DEFAULT 0.00,
    total_reviews INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    published_at TIMESTAMP NULL,

    -- Indexes for performance
    INDEX idx_instructor (instructor_id),
    INDEX idx_category (category_id),
    INDEX idx_status (status),
    INDEX idx_featured (is_featured),
    INDEX idx_free (is_free),
    INDEX idx_level (level),
    INDEX idx_price (price),
    FULLTEXT idx_fulltext (title, description, short_description)
);

-- Course Tags Junction Table
CREATE TABLE course_tags (
    course_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    PRIMARY KEY (course_id, tag_id),
    FOREIGN KEY (course_id) REFERENCES course(id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES tag(id) ON DELETE CASCADE,
    INDEX idx_course (course_id),
    INDEX idx_tag (tag_id)
);

-- Course Content (Enhanced)
CREATE TABLE course_content (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    course_id BIGINT NOT NULL,
    section_id BIGINT,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    content_type ENUM('VIDEO', 'DOCUMENT', 'QUIZ', 'ASSIGNMENT', 'LIVE_SESSION') NOT NULL,
    content_url VARCHAR(500),
    external_url VARCHAR(500),
    duration_minutes INT,
    sequence_order INT NOT NULL,
    is_preview BOOLEAN DEFAULT FALSE,
    is_mandatory BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (course_id) REFERENCES course(id) ON DELETE CASCADE,
    INDEX idx_course (course_id),
    INDEX idx_section (section_id),
    INDEX idx_sequence (course_id, sequence_order),
    INDEX idx_type (content_type)
);

-- Course Sections
CREATE TABLE course_section (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    course_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    sequence_order INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (course_id) REFERENCES course(id) ON DELETE CASCADE,
    INDEX idx_course (course_id),
    INDEX idx_sequence (course_id, sequence_order)
);

-- Course Reviews
CREATE TABLE course_review (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    course_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    rating INT NOT NULL CHECK (rating >= 1 AND rating <= 5),
    review_text TEXT,
    is_verified BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (course_id) REFERENCES course(id) ON DELETE CASCADE,
    UNIQUE KEY unique_review (course_id, user_id),
    INDEX idx_course (course_id),
    INDEX idx_user (user_id),
    INDEX idx_rating (rating)
);
```

## User Service Database (user_db)

### Core Tables

```sql
-- Users Table (Enhanced)
CREATE TABLE user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    email VARCHAR(255) UNIQUE NOT NULL,
    username VARCHAR(100) UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    phone VARCHAR(20),
    date_of_birth DATE,
    gender ENUM('MALE', 'FEMALE', 'OTHER'),
    role ENUM('STUDENT', 'INSTRUCTOR', 'ADMIN') DEFAULT 'STUDENT',
    status ENUM('ACTIVE', 'INACTIVE', 'SUSPENDED', 'PENDING_VERIFICATION') DEFAULT 'ACTIVE',
    email_verified BOOLEAN DEFAULT FALSE,
    phone_verified BOOLEAN DEFAULT FALSE,
    last_login_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    -- Indexes
    INDEX idx_email (email),
    INDEX idx_username (username),
    INDEX idx_role (role),
    INDEX idx_status (status),
    INDEX idx_email_verified (email_verified)
);

-- User Profiles
CREATE TABLE user_profile (
    user_id BIGINT PRIMARY KEY,
    bio TEXT,
    avatar_url VARCHAR(500),
    cover_image_url VARCHAR(500),
    website VARCHAR(255),
    linkedin_url VARCHAR(255),
    twitter_url VARCHAR(255),
    github_url VARCHAR(255),
    expertise TEXT,
    experience_years INT,
    company VARCHAR(255),
    job_title VARCHAR(255),
    location VARCHAR(255),
    timezone VARCHAR(50),

    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
);

-- User Sessions (for JWT invalidation)
CREATE TABLE user_session (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(255) UNIQUE NOT NULL,
    device_info VARCHAR(255),
    ip_address VARCHAR(45),
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    INDEX idx_user (user_id),
    INDEX idx_token (token_hash),
    INDEX idx_expires (expires_at)
);

-- Password Reset Tokens
CREATE TABLE password_reset_token (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    token VARCHAR(255) UNIQUE NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    INDEX idx_user (user_id),
    INDEX idx_token (token),
    INDEX idx_expires (expires_at)
);

-- Email Verification Tokens
CREATE TABLE email_verification_token (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    token VARCHAR(255) UNIQUE NOT NULL,
    email VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    INDEX idx_user (user_id),
    INDEX idx_token (token),
    INDEX idx_expires (expires_at)
);
```

## Enrollment Service Database (enrollment_db)

### Core Tables

```sql
-- Enrollments Table (Enhanced)
CREATE TABLE enrollment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    enrollment_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completion_date TIMESTAMP NULL,
    progress_percentage DECIMAL(5,2) DEFAULT 0.00,
    status ENUM('ENROLLED', 'IN_PROGRESS', 'COMPLETED', 'DROPPED', 'EXPIRED') DEFAULT 'ENROLLED',
    certificate_issued BOOLEAN DEFAULT FALSE,
    certificate_url VARCHAR(500),
    last_accessed_at TIMESTAMP NULL,
    expires_at TIMESTAMP NULL,
    payment_id BIGINT,

    UNIQUE KEY unique_enrollment (user_id, course_id),
    FOREIGN KEY (user_id) REFERENCES user(id),
    FOREIGN KEY (course_id) REFERENCES course(id),
    INDEX idx_user (user_id),
    INDEX idx_course (course_id),
    INDEX idx_status (status),
    INDEX idx_enrollment_date (enrollment_date),
    INDEX idx_completion_date (completion_date)
);

-- Progress Tracking (Detailed)
CREATE TABLE progress_tracking (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    enrollment_id BIGINT NOT NULL,
    content_id BIGINT NOT NULL,
    completed BOOLEAN DEFAULT FALSE,
    completed_at TIMESTAMP NULL,
    time_spent_minutes INT DEFAULT 0,
    attempts_count INT DEFAULT 0,
    last_attempt_at TIMESTAMP NULL,
    score DECIMAL(5,2) NULL,

    FOREIGN KEY (enrollment_id) REFERENCES enrollment(id) ON DELETE CASCADE,
    UNIQUE KEY unique_progress (enrollment_id, content_id),
    INDEX idx_enrollment (enrollment_id),
    INDEX idx_content (content_id),
    INDEX idx_completed (completed)
);

-- Certificates
CREATE TABLE certificate (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    enrollment_id BIGINT NOT NULL,
    certificate_number VARCHAR(100) UNIQUE NOT NULL,
    issued_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expiry_date TIMESTAMP NULL,
    certificate_url VARCHAR(500),
    qr_code_url VARCHAR(500),
    status ENUM('ACTIVE', 'REVOKED', 'EXPIRED') DEFAULT 'ACTIVE',

    FOREIGN KEY (enrollment_id) REFERENCES enrollment(id) ON DELETE CASCADE,
    INDEX idx_enrollment (enrollment_id),
    INDEX idx_certificate_number (certificate_number),
    INDEX idx_status (status)
);

-- Payments (if integrated)
CREATE TABLE payment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    enrollment_id BIGINT NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'USD',
    payment_method VARCHAR(50),
    transaction_id VARCHAR(255) UNIQUE,
    status ENUM('PENDING', 'COMPLETED', 'FAILED', 'REFUNDED') DEFAULT 'PENDING',
    payment_date TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (enrollment_id) REFERENCES enrollment(id) ON DELETE CASCADE,
    INDEX idx_enrollment (enrollment_id),
    INDEX idx_transaction (transaction_id),
    INDEX idx_status (status)
);
```

## Database Optimization Strategies

### Indexing Strategy
- Primary keys are auto-increment BIGINT for performance
- Foreign key columns are indexed
- Frequently queried columns have appropriate indexes
- Composite indexes for common query patterns
- Full-text indexes for search functionality

### Performance Optimizations
```sql
-- Example: Optimized course search query
SELECT c.*, u.first_name, u.last_name, cat.name as category_name,
       AVG(cr.rating) as avg_rating, COUNT(cr.id) as review_count
FROM course c
LEFT JOIN user u ON c.instructor_id = u.id
LEFT JOIN category cat ON c.category_id = cat.id
LEFT JOIN course_review cr ON c.id = cr.course_id
WHERE c.status = 'PUBLISHED'
  AND (c.title LIKE '%search_term%' OR c.description LIKE '%search_term%')
GROUP BY c.id
ORDER BY c.created_at DESC
LIMIT 20 OFFSET 0;

-- Indexes supporting this query:
-- idx_status, idx_title_description (fulltext), idx_instructor, idx_category
```

### Partitioning Strategy
```sql
-- Partition enrollment table by year for better performance
ALTER TABLE enrollment
PARTITION BY RANGE (YEAR(enrollment_date)) (
    PARTITION p2024 VALUES LESS THAN (2025),
    PARTITION p2025 VALUES LESS THAN (2026),
    PARTITION p_future VALUES LESS THAN MAXVALUE
);
```

### Read Replicas Configuration
- Master database for writes
- Multiple read replicas for read operations
- Connection routing based on operation type
- Automatic failover configuration

### Backup Strategy
- Daily full backups
- Hourly incremental backups for critical data
- Point-in-time recovery capability
- Cross-region backup replication

### Connection Pooling
```yaml
# application.yml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      idle-timeout: 300000
      max-lifetime: 1200000
      connection-timeout: 20000
```

## Data Consistency

### Cross-Service Data Synchronization
- Event-driven updates for related data
- Saga pattern for distributed transactions
- Eventual consistency model
- Compensation actions for failed operations

### Data Validation
- Database-level constraints
- Application-level validation
- Referential integrity enforcement
- Data type and format validation

## Migration Strategy

### Flyway Integration
```sql
-- V1__Initial_schema.sql
-- V2__Add_course_categories.sql
-- V3__Enhance_user_profiles.sql
-- V4__Add_progress_tracking.sql
```

### Zero-Downtime Migrations
- Backward compatible schema changes
- Feature flags for new functionality
- Gradual rollout of database changes
- Rollback scripts for each migration
