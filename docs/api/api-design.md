# API Design

## Overview

The platform exposes RESTful APIs through an API Gateway that routes requests to appropriate microservices. All APIs follow REST principles with consistent request/response formats, error handling, and authentication mechanisms.

## API Gateway Configuration

### Routing Rules
```
API Gateway Routes:
├── /api/auth/**     → User Service
├── /api/users/**    → User Service
├── /api/courses/**  → Course Service
├── /api/enrollments/** → Enrollment Service
└── /api/admin/**    → Admin Service (future)
```

### Gateway Features
- Request routing and load balancing
- Authentication and authorization
- Rate limiting
- Request/response logging
- CORS handling
- Request transformation

## Authentication & Authorization

### JWT Token Structure
```json
{
  "header": {
    "alg": "HS256",
    "typ": "JWT"
  },
  "payload": {
    "sub": "user_id",
    "email": "user@example.com",
    "role": "STUDENT",
    "iat": 1640995200,
    "exp": 1641081600,
    "iss": "course-platform",
    "aud": "course-platform-api"
  },
  "signature": "base64-encoded-signature"
}
```

### Authentication Headers
```
Authorization: Bearer <jwt_token>
X-API-Key: <api_key>  // For service-to-service calls
```

### Role-Based Access Control
- **STUDENT**: Read courses, enroll, track progress
- **INSTRUCTOR**: CRUD courses, view enrollments, manage content
- **ADMIN**: All permissions, system management

## API Response Format

### Success Response
```json
{
  "success": true,
  "data": {
    // Response data
  },
  "message": "Operation completed successfully",
  "timestamp": "2024-01-01T12:00:00Z",
  "requestId": "req-12345"
}
```

### Error Response
```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Invalid input data",
    "details": {
      "field": "email",
      "reason": "Email format is invalid"
    }
  },
  "timestamp": "2024-01-01T12:00:00Z",
  "requestId": "req-12345"
}
```

### Paginated Response
```json
{
  "success": true,
  "data": {
    "items": [...],
    "pagination": {
      "page": 1,
      "size": 20,
      "totalItems": 150,
      "totalPages": 8,
      "hasNext": true,
      "hasPrevious": false
    }
  },
  "message": "Courses retrieved successfully",
  "timestamp": "2024-01-01T12:00:00Z",
  "requestId": "req-12345"
}
```

## Course Service APIs

### Course Management

#### Create Course
```http
POST /api/courses
Authorization: Bearer <token>
Content-Type: application/json

{
  "title": "Introduction to Java",
  "description": "Learn Java programming fundamentals",
  "categoryId": 1,
  "price": 99.99,
  "level": "BEGINNER",
  "tags": ["java", "programming"]
}
```

#### Get Courses (with filtering)
```http
GET /api/courses?page=1&size=20&category=programming&level=BEGINNER&sort=createdAt,desc
Authorization: Bearer <token>
```

#### Update Course
```http
PUT /api/courses/{id}
Authorization: Bearer <token>
Content-Type: application/json

{
  "title": "Advanced Java Programming",
  "price": 149.99
}
```

#### Delete Course
```http
DELETE /api/courses/{id}
Authorization: Bearer <token>
```

### Course Content Management

#### Add Course Content
```http
POST /api/courses/{courseId}/content
Authorization: Bearer <token>
Content-Type: application/json

{
  "title": "Variables and Data Types",
  "contentType": "VIDEO",
  "contentUrl": "https://example.com/video.mp4",
  "durationMinutes": 30,
  "sequenceOrder": 1
}
```

#### Get Course Content
```http
GET /api/courses/{courseId}/content
Authorization: Bearer <token>
```

## User Service APIs

### Authentication

#### User Registration
```http
POST /api/auth/register
Content-Type: application/json

{
  "email": "student@example.com",
  "password": "SecurePass123!",
  "firstName": "John",
  "lastName": "Doe",
  "role": "STUDENT"
}
```

#### User Login
```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "student@example.com",
  "password": "SecurePass123!"
}
```

Response:
```json
{
  "success": true,
  "data": {
    "user": {
      "id": 123,
      "email": "student@example.com",
      "firstName": "John",
      "lastName": "Doe",
      "role": "STUDENT"
    },
    "tokens": {
      "accessToken": "eyJhbGciOiJIUzI1NiIs...",
      "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
      "expiresIn": 3600
    }
  }
}
```

#### Refresh Token
```http
POST /api/auth/refresh
Content-Type: application/json

{
  "refreshToken": "eyJhbGciOiJIUzI1NiIs..."
}
```

### User Profile Management

#### Get User Profile
```http
GET /api/users/profile
Authorization: Bearer <token>
```

#### Update User Profile
```http
PUT /api/users/profile
Authorization: Bearer <token>
Content-Type: application/json

{
  "firstName": "John",
  "lastName": "Smith",
  "bio": "Software developer with 5 years experience",
  "website": "https://johnsmith.dev"
}
```

## Enrollment Service APIs

### Course Enrollment

#### Enroll in Course
```http
POST /api/enrollments
Authorization: Bearer <token>
Content-Type: application/json

{
  "courseId": 123
}
```

#### Get User Enrollments
```http
GET /api/enrollments?page=1&size=10&status=IN_PROGRESS
Authorization: Bearer <token>
```

#### Update Progress
```http
PUT /api/enrollments/{enrollmentId}/progress
Authorization: Bearer <token>
Content-Type: application/json

{
  "contentId": 456,
  "completed": true,
  "timeSpentMinutes": 45
}
```

#### Get Enrollment Details
```http
GET /api/enrollments/{id}
Authorization: Bearer <token>
```

Response:
```json
{
  "success": true,
  "data": {
    "id": 789,
    "courseId": 123,
    "courseTitle": "Introduction to Java",
    "enrollmentDate": "2024-01-01T10:00:00Z",
    "progressPercentage": 65.5,
    "status": "IN_PROGRESS",
    "lastAccessedAt": "2024-01-15T14:30:00Z",
    "certificateIssued": false,
    "progress": [
      {
        "contentId": 456,
        "title": "Variables and Data Types",
        "completed": true,
        "completedAt": "2024-01-10T09:15:00Z",
        "timeSpentMinutes": 45
      }
    ]
  }
}
```

## Error Codes and Handling

### HTTP Status Codes
- `200 OK` - Successful request
- `201 Created` - Resource created successfully
- `400 Bad Request` - Invalid request data
- `401 Unauthorized` - Authentication required
- `403 Forbidden` - Insufficient permissions
- `404 Not Found` - Resource not found
- `409 Conflict` - Resource conflict (e.g., duplicate enrollment)
- `422 Unprocessable Entity` - Validation errors
- `429 Too Many Requests` - Rate limit exceeded
- `500 Internal Server Error` - Server error

### Error Code Reference
```javascript
const ERROR_CODES = {
  // Authentication Errors
  INVALID_CREDENTIALS: 'INVALID_CREDENTIALS',
  TOKEN_EXPIRED: 'TOKEN_EXPIRED',
  TOKEN_INVALID: 'TOKEN_INVALID',
  INSUFFICIENT_PERMISSIONS: 'INSUFFICIENT_PERMISSIONS',

  // Validation Errors
  VALIDATION_ERROR: 'VALIDATION_ERROR',
  REQUIRED_FIELD_MISSING: 'REQUIRED_FIELD_MISSING',
  INVALID_FORMAT: 'INVALID_FORMAT',

  // Resource Errors
  RESOURCE_NOT_FOUND: 'RESOURCE_NOT_FOUND',
  RESOURCE_ALREADY_EXISTS: 'RESOURCE_ALREADY_EXISTS',
  RESOURCE_CONFLICT: 'RESOURCE_CONFLICT',

  // Business Logic Errors
  COURSE_NOT_AVAILABLE: 'COURSE_NOT_AVAILABLE',
  ENROLLMENT_LIMIT_REACHED: 'ENROLLMENT_LIMIT_REACHED',
  PAYMENT_REQUIRED: 'PAYMENT_REQUIRED',

  // System Errors
  INTERNAL_SERVER_ERROR: 'INTERNAL_SERVER_ERROR',
  SERVICE_UNAVAILABLE: 'SERVICE_UNAVAILABLE',
  DATABASE_ERROR: 'DATABASE_ERROR'
};
```

## Rate Limiting

### Rate Limit Headers
```
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 95
X-RateLimit-Reset: 1640995200
X-RateLimit-Retry-After: 60
```

### Rate Limits by Endpoint
- **Authentication endpoints**: 5 requests per minute per IP
- **Course listing**: 100 requests per minute per user
- **Course creation**: 10 requests per hour per instructor
- **Enrollment operations**: 20 requests per minute per user
- **Progress updates**: 60 requests per minute per user

## API Versioning

### URL-based Versioning
```
/api/v1/courses
/api/v1/auth/login
/api/v2/courses  // Future version
```

### Header-based Versioning
```
Accept: application/vnd.courseplatform.v1+json
```

## Content Negotiation

### Supported Content Types
- `application/json` - Default
- `application/xml` - Alternative (limited support)

### Request/Response Headers
```
Accept: application/json
Content-Type: application/json
Accept-Language: en-US
```

## Caching Strategy

### Cache Headers
```
Cache-Control: max-age=300, public
ETag: "course-123-v1"
Last-Modified: Wed, 01 Jan 2024 12:00:00 GMT
```

### Cacheable Resources
- Course listings (5 minutes)
- Course details (10 minutes)
- User profiles (15 minutes)
- Static content URLs (1 hour)

## API Documentation

### OpenAPI Specification
All APIs are documented using OpenAPI 3.0 specification with Swagger UI for interactive documentation.

### API Documentation URL
- Development: `http://localhost:8080/swagger-ui.html`
- Production: `https://api.courseplatform.com/docs`

## Security Measures

### Input Validation
- Request payload validation using Bean Validation
- SQL injection prevention
- XSS protection
- CSRF protection for state-changing operations

### Data Sanitization
- HTML sanitization for rich text fields
- File upload validation and virus scanning
- URL validation and normalization

### Audit Logging
All API requests are logged with:
- Request ID for tracing
- User ID and IP address
- Timestamp and endpoint
- Request/response status
- Processing time

## Testing

### API Testing Strategy
- Unit tests for controllers and services
- Integration tests for API endpoints
- Contract tests between services
- End-to-end tests for critical flows

### Test Data Management
- Test databases with sample data
- Mock external services
- API test collections (Postman/Newman)
- Performance testing with JMeter
