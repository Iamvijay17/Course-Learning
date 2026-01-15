# Microservices Design

## Service Overview

The platform is decomposed into three core microservices, each responsible for a specific business domain. This design follows Domain-Driven Design (DDD) principles and ensures loose coupling between services.

## Course Service

### Responsibilities
- Course CRUD operations (Create, Read, Update, Delete)
- Course content management
- Course search and filtering
- Category and tag management
- Instructor course assignments

### API Endpoints
```
POST   /api/courses          - Create new course
GET    /api/courses          - Get all courses (with pagination/filtering)
GET    /api/courses/{id}     - Get course by ID
PUT    /api/courses/{id}     - Update course
DELETE /api/courses/{id}     - Delete course
GET    /api/courses/search   - Search courses
POST   /api/courses/{id}/content - Add course content
```

### Database Schema
```sql
-- Course Table
CREATE TABLE course (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    instructor_id BIGINT NOT NULL,
    category VARCHAR(100),
    price DECIMAL(10,2),
    duration_hours INT,
    level ENUM('BEGINNER', 'INTERMEDIATE', 'ADVANCED'),
    status ENUM('DRAFT', 'PUBLISHED', 'ARCHIVED'),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_instructor (instructor_id),
    INDEX idx_category (category),
    INDEX idx_status (status)
);

-- Course Content Table
CREATE TABLE course_content (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    course_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    content_type ENUM('VIDEO', 'DOCUMENT', 'QUIZ', 'ASSIGNMENT'),
    content_url VARCHAR(500),
    sequence_order INT,
    duration_minutes INT,
    FOREIGN KEY (course_id) REFERENCES course(id),
    INDEX idx_course (course_id)
);
```

### DTOs
- `CourseDTO` - For course data transfer
- `CourseCreateDTO` - For course creation
- `CourseUpdateDTO` - For course updates
- `CourseSearchDTO` - For search parameters

## User Service

### Responsibilities
- User registration and profile management
- Authentication and JWT token generation
- Role-based authorization
- User preferences and settings
- Password management

### API Endpoints
```
POST   /api/auth/register    - User registration
POST   /api/auth/login       - User authentication
POST   /api/auth/refresh     - Token refresh
GET    /api/users/profile    - Get user profile
PUT    /api/users/profile    - Update user profile
POST   /api/auth/logout      - User logout
POST   /api/auth/forgot-password - Password reset request
```

### Database Schema
```sql
-- User Table
CREATE TABLE user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    role ENUM('STUDENT', 'INSTRUCTOR', 'ADMIN') DEFAULT 'STUDENT',
    status ENUM('ACTIVE', 'INACTIVE', 'SUSPENDED') DEFAULT 'ACTIVE',
    email_verified BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_email (email),
    INDEX idx_role (role),
    INDEX idx_status (status)
);

-- User Profile Table
CREATE TABLE user_profile (
    user_id BIGINT PRIMARY KEY,
    bio TEXT,
    avatar_url VARCHAR(500),
    website VARCHAR(255),
    linkedin_url VARCHAR(255),
    expertise VARCHAR(255),
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
);
```

### DTOs
- `UserDTO` - User data transfer
- `UserRegistrationDTO` - Registration data
- `LoginDTO` - Login credentials
- `UserProfileDTO` - Profile information

## Enrollment Service

### Responsibilities
- Course enrollment management
- Progress tracking
- Certificate generation
- Enrollment analytics
- Payment processing integration

### API Endpoints
```
POST   /api/enrollments       - Enroll in course
GET    /api/enrollments       - Get user enrollments
GET    /api/enrollments/{id}  - Get enrollment details
PUT    /api/enrollments/{id}/progress - Update progress
DELETE /api/enrollments/{id}  - Unenroll from course
GET    /api/enrollments/course/{courseId} - Get course enrollments
POST   /api/enrollments/{id}/certificate - Generate certificate
```

### Database Schema
```sql
-- Enrollment Table
CREATE TABLE enrollment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    enrollment_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completion_date TIMESTAMP NULL,
    progress_percentage DECIMAL(5,2) DEFAULT 0.00,
    status ENUM('ENROLLED', 'IN_PROGRESS', 'COMPLETED', 'DROPPED'),
    certificate_issued BOOLEAN DEFAULT FALSE,
    UNIQUE KEY unique_enrollment (user_id, course_id),
    FOREIGN KEY (user_id) REFERENCES user(id),
    FOREIGN KEY (course_id) REFERENCES course(id),
    INDEX idx_user (user_id),
    INDEX idx_course (course_id),
    INDEX idx_status (status)
);

-- Progress Tracking Table
CREATE TABLE progress_tracking (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    enrollment_id BIGINT NOT NULL,
    content_id BIGINT NOT NULL,
    completed BOOLEAN DEFAULT FALSE,
    completed_at TIMESTAMP NULL,
    time_spent_minutes INT DEFAULT 0,
    FOREIGN KEY (enrollment_id) REFERENCES enrollment(id) ON DELETE CASCADE,
    INDEX idx_enrollment (enrollment_id)
);
```

### DTOs
- `EnrollmentDTO` - Enrollment data
- `ProgressDTO` - Progress information
- `CertificateDTO` - Certificate data

## Service Communication

### Synchronous Communication (REST)
- API Gateway to Microservices
- Inter-service calls for data consistency

### Asynchronous Communication (Events)
- Course creation events
- Enrollment completion events
- User registration events

### Event Schema Examples
```json
// Course Created Event
{
  "eventType": "COURSE_CREATED",
  "courseId": 123,
  "instructorId": 456,
  "timestamp": "2024-01-01T10:00:00Z"
}

// Enrollment Completed Event
{
  "eventType": "ENROLLMENT_COMPLETED",
  "enrollmentId": 789,
  "userId": 456,
  "courseId": 123,
  "completionDate": "2024-01-15T14:30:00Z"
}
```

## Shared Components

### Common DTOs
- `ApiResponse<T>` - Standardized API response wrapper
- `ErrorResponse` - Error handling structure
- `PaginationDTO` - Pagination parameters

### Exception Handling
- `ResourceNotFoundException`
- `ValidationException`
- `UnauthorizedException`
- `GlobalExceptionHandler` - Centralized exception handling

### Configuration
- Database configuration
- JWT configuration
- Caching configuration
- External service configurations

## Service Discovery & Load Balancing
- Spring Cloud Netflix Eureka for service registration
- Ribbon for client-side load balancing
- Circuit breaker pattern with Hystrix

## Caching Strategy
- Redis for distributed caching
- Cache course metadata
- Cache user sessions
- Cache frequently accessed enrollment data

## Database Design Principles
- Each service owns its data
- Foreign key relationships within service boundaries
- Data consistency through sagas for cross-service transactions
- Database indexing for performance
- Read replicas for scalability
