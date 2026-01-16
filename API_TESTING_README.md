# 🧪 Course Learning Platform - API Testing Suite

Complete automated testing solution for the Course Learning Platform APIs including Certificate Generation and Enrollment Analytics.

## 📋 Overview

This testing suite provides multiple levels of API testing:

- **JUnit Integration Tests** - Spring Boot integration tests
- **Postman API Tests** - Contract and end-to-end API testing
- **CI/CD Integration** - GitHub Actions automated testing
- **Local Test Runner** - Bash script for local testing

## 🏗️ Test Architecture

```
Course Learning Platform APIs
├── Certificate Generation
│   ├── PDF Generation
│   ├── Certificate Storage
│   └── Download/View Endpoints
└── Enrollment Analytics
    ├── Platform Overview
    ├── Course Analytics
    ├── Trend Analysis
    └── Top Courses
```

## 🚀 Quick Start

### Prerequisites

- **Java 21+** (for Spring Boot)
- **Maven** or `./mvnw` (Maven wrapper)
- **Node.js & npm** (for Newman/Postman tests)
- **MySQL** (for database)

### Run All Tests

```bash
# Make script executable (Linux/Mac)
chmod +x run-api-tests.sh

# Run all tests
./run-api-tests.sh

# Or run specific test types
./run-api-tests.sh junit     # Only JUnit tests
./run-api-tests.sh postman   # Only Postman tests
./run-api-tests.sh report    # Generate reports only
```

## 🧪 Testing Tools

### 1. JUnit Integration Tests

**Location:** `backend/src/test/java/com/course_learning/backend/ApiIntegrationTest.java`

**What it tests:**
- Spring Boot context loading
- Database connectivity
- Bean initialization

**Run JUnit tests:**
```bash
cd backend
./mvnw test
```

### 2. Postman API Tests

**Files:**
- `Course-Learning-API-Testing.postman_collection.json` - Test collection
- `postman_environment.json` - Environment variables
- `newman-config.json` - Newman configuration

**Test Coverage:**
- ✅ Authentication (Admin & Student login)
- ✅ Course Management (Get courses, course details)
- ✅ Enrollment Management (Enroll, complete course)
- ✅ Certificate Generation (Auto-generation, download, view)
- ✅ Analytics (Overview, course analytics, trends)

**Run Postman tests:**
```bash
# Install Newman (if not installed)
npm install -g newman newman-reporter-html newman-reporter-json

# Run tests
newman run Course-Learning-API-Testing.postman_collection.json -e postman_environment.json

# With configuration file
newman run --config newman-config.json
```

### 3. GitHub Actions CI/CD

**File:** `.github/workflows/api-tests.yml`

**Triggers:**
- Push to `main` or `develop` branches
- Pull requests to `main` or `develop`
- Manual trigger via GitHub UI

**What it does:**
1. Sets up MySQL database
2. Runs JUnit tests
3. Starts Spring Boot application
4. Runs Postman API tests
5. Generates test reports
6. Uploads artifacts

### 4. Local Test Runner Script

**File:** `run-api-tests.sh`

**Features:**
- ✅ Dependency checking
- ✅ Colored output
- ✅ Automatic Spring Boot startup/shutdown
- ✅ Comprehensive test reporting
- ✅ Error handling and cleanup

## 📊 Test Reports

### Generated Reports

1. **JUnit Reports:** `backend/target/surefire-reports/`
2. **Postman HTML Report:** `test-results/postman-report.html`
3. **Postman JSON Report:** `test-results/postman-report.json`
4. **Test Summary:** `TEST_RESULTS.md`

### Report Contents

**JUnit Report:**
- Test execution time
- Pass/fail counts
- Error details
- Code coverage (if enabled)

**Postman Report:**
- Request/response details
- Test assertions
- Response times
- Failure reasons

## 🔧 Configuration

### Environment Variables

**Postman Environment (`postman_environment.json`):**
```json
{
  "base_url": "http://localhost:8080",
  "admin_username": "vijayk",
  "admin_password": "Admin@123",
  "student_username": "testuser",
  "student_password": "password123"
}
```

**Spring Boot Test Profile:**
```properties
# Create src/test/resources/application-test.properties
spring.datasource.url=jdbc:mysql://localhost:3306/test
spring.datasource.username=testuser
spring.datasource.password=testpass
spring.jpa.hibernate.ddl-auto=create-drop
```

### Newman Configuration

**`newman-config.json`:**
```json
{
  "collection": "Course-Learning-API-Testing.postman_collection.json",
  "environment": "postman_environment.json",
  "reporters": ["cli", "html", "json"],
  "timeout": 30000
}
```

## 🎯 Test Scenarios

### Certificate Generation Flow
1. **Student Login** → Get auth token
2. **Get Courses** → Select first course
3. **Enroll** → Create enrollment
4. **Complete Course** → Auto-generate certificate
5. **Download Certificate** → Verify PDF generation
6. **View Certificate** → Verify inline display

### Analytics Testing Flow
1. **Admin Login** → Get admin token
2. **Get Overview** → Platform-wide statistics
3. **Get Course Analytics** → Course-specific data
4. **Get Top Courses** → Popularity rankings
5. **Get Trends** → Time-series data
6. **Get Summary** → Dashboard data

## 🐛 Debugging

### Common Issues

**1. Database Connection Issues:**
```bash
# Check MySQL is running
mysql -u root -p -e "SELECT 1"

# Reset test database
mysql -u root -p -e "DROP DATABASE IF EXISTS test; CREATE DATABASE test;"
```

**2. Port Conflicts:**
```bash
# Kill process on port 8080
lsof -ti:8080 | xargs kill -9

# Or change port in application.properties
server.port=8081
```

**3. Newman Installation Issues:**
```bash
# Clear npm cache
npm cache clean --force

# Reinstall Newman
npm uninstall -g newman
npm install -g newman newman-reporter-html newman-reporter-json
```

### Debug Commands

```bash
# Run with verbose output
newman run collection.json -e environment.json --verbose

# Run single request
newman run collection.json -e environment.json --folder "Authentication"

# Check Spring Boot logs
tail -f spring-boot.log

# Check test database
mysql -u testuser -p test -e "SELECT * FROM enrollments LIMIT 5;"
```

## 📈 Performance Testing

### Response Time Assertions

Postman tests include response time checks:
- All requests < 30 seconds
- Most requests < 5 seconds
- Certificate downloads < 10 seconds

### Load Testing

For load testing, consider:
- **k6** for scenario-based load testing
- **JMeter** for complex load patterns
- **Artillery** for simple load tests

## 🔒 Security Testing

### Authentication Tests
- ✅ Valid login flows
- ✅ Invalid credentials handling
- ✅ Token expiration
- ✅ Role-based access control

### Authorization Tests
- ✅ Admin-only analytics access
- ✅ Student certificate access
- ✅ Cross-user data protection

## 📚 API Documentation

### Certificate APIs
```
POST   /api/auth/login                    # Authentication
GET    /api/courses                      # Get courses
POST   /api/enrollments                  # Enroll in course
PUT    /api/enrollments/complete         # Complete course
GET    /api/certificates/enrollment/{id} # Get certificate
GET    /api/certificates/download/{id}   # Download PDF
GET    /api/certificates/view/{id}       # View PDF
```

### Analytics APIs
```
GET    /api/analytics/enrollments/overview     # Platform overview
GET    /api/analytics/enrollments/courses/{id} # Course analytics
GET    /api/analytics/enrollments/courses/top  # Top courses
GET    /api/analytics/enrollments/trends       # Trends
GET    /api/analytics/enrollments/summary      # Dashboard
```

## 🎉 Success Metrics

**Test Coverage Goals:**
- ✅ All major API endpoints tested
- ✅ Authentication flows covered
- ✅ Business logic validated
- ✅ Error scenarios handled
- ✅ Performance benchmarks met

**CI/CD Integration:**
- ✅ Automated testing on commits
- ✅ Test reports generated
- ✅ Failure notifications
- ✅ Artifact storage

## 🚀 Next Steps

1. **Add More Test Scenarios**
   - User registration flows
   - Password reset functionality
   - Bulk operations testing

2. **Performance Testing**
   - Load testing with k6
   - Stress testing scenarios
   - Database performance monitoring

3. **Contract Testing**
   - API schema validation
   - Response format verification
   - Backward compatibility checks

4. **Monitoring Integration**
   - Real-time API monitoring
   - Alert configuration
   - SLA tracking

---

**Happy Testing! 🎯**

For issues or questions, check the test logs and reports in the `test-results/` directory.
