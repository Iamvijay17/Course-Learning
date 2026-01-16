 #!/bin/bash

# Course Learning Platform API Test Runner
# This script runs both JUnit and Postman API tests

set -e

echo "🚀 Starting Course Learning Platform API Tests"
echo "=============================================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

# Check if required tools are installed
check_dependencies() {
    print_status "Checking dependencies..."

    if ! command -v java &> /dev/null; then
        print_error "Java is not installed. Please install Java 21+"
        exit 1
    fi

    if ! command -v mvn &> /dev/null && [ ! -f "./mvnw" ]; then
        print_error "Maven is not installed and mvnw not found"
        exit 1
    fi

    if ! command -v node &> /dev/null; then
        print_warning "Node.js not found. Installing Newman globally..."
        if command -v npm &> /dev/null; then
            npm install -g newman newman-reporter-html newman-reporter-json
        else
            print_error "npm not found. Please install Node.js to run Postman tests"
            exit 1
        fi
    fi

    print_success "Dependencies check completed"
}

# Run JUnit tests
run_junit_tests() {
    print_status "Running JUnit Integration Tests..."

    cd backend

    if [ -f "./mvnw" ]; then
        MAVEN_CMD="./mvnw"
    else
        MAVEN_CMD="mvn"
    fi

    # Run tests
    if $MAVEN_CMD test -Dspring.profiles.active=test; then
        print_success "JUnit tests passed"
    else
        print_error "JUnit tests failed"
        exit 1
    fi

    cd ..
}

# Run Postman tests
run_postman_tests() {
    print_status "Running Postman API Tests..."

    # Create test results directory
    mkdir -p test-results

    # Check if Newman is installed
    if ! command -v newman &> /dev/null; then
        print_error "Newman not found. Installing..."
        npm install -g newman newman-reporter-html newman-reporter-json
    fi

    # Start the Spring Boot application in background
    print_status "Starting Spring Boot application..."
    cd backend

    if [ -f "./mvnw" ]; then
        MAVEN_CMD="./mvnw"
    else
        MAVEN_CMD="mvn"
    fi

    # Start application
    $MAVEN_CMD spring-boot:run > ../spring-boot.log 2>&1 &
    SPRING_PID=$!

    cd ..

    # Wait for application to start
    print_status "Waiting for application to start..."
    sleep 30

    # Check if application is running
    if ! curl -f http://localhost:8080/api/courses > /dev/null 2>&1; then
        print_error "Spring Boot application failed to start"
        kill $SPRING_PID 2>/dev/null || true
        cat spring-boot.log
        exit 1
    fi

    print_success "Spring Boot application started successfully"

    # Run Postman tests
    if newman run --config newman-config.json; then
        print_success "Postman API tests passed"
    else
        print_error "Postman API tests failed"
        kill $SPRING_PID 2>/dev/null || true
        exit 1
    fi

    # Stop the application
    print_status "Stopping Spring Boot application..."
    kill $SPRING_PID 2>/dev/null || true
    sleep 5
}

# Generate test report
generate_report() {
    print_status "Generating test report..."

    echo "# 🧪 Course Learning Platform API Test Results" > TEST_RESULTS.md
    echo "" >> TEST_RESULTS.md
    echo "Generated on: $(date)" >> TEST_RESULTS.md
    echo "" >> TEST_RESULTS.md

    # JUnit Results
    if [ -d "backend/target/surefire-reports" ]; then
        echo "## JUnit Test Results" >> TEST_RESULTS.md
        echo "" >> TEST_RESULTS.md

        # Count tests
        TEST_COUNT=$(find backend/target/surefire-reports -name "*.xml" -exec grep -l "testsuite" {} \; | wc -l)
        echo "- **Test Classes:** $TEST_COUNT" >> TEST_RESULTS.md

        # Extract test statistics
        if [ -f "backend/target/surefire-reports/TEST-com.course_learning.backend.ApiIntegrationTest.xml" ]; then
            TESTS=$(grep -o 'tests="[0-9]*"' backend/target/surefire-reports/TEST-*.xml | sed 's/.*="//;s/".*//' | awk '{sum += $1} END {print sum}')
            FAILURES=$(grep -o 'failures="[0-9]*"' backend/target/surefire-reports/TEST-*.xml | sed 's/.*="//;s/".*//' | awk '{sum += $1} END {print sum}')
            ERRORS=$(grep -o 'errors="[0-9]*"' backend/target/surefire-reports/TEST-*.xml | sed 's/.*="//;s/".*//' | awk '{sum += $1} END {print sum}')

            echo "- **Total Tests:** $TESTS" >> TEST_RESULTS.md
            echo "- **Failures:** $FAILURES" >> TEST_RESULTS.md
            echo "- **Errors:** $ERRORS" >> TEST_RESULTS.md
        fi
        echo "" >> TEST_RESULTS.md
    fi

    # Postman Results
    if [ -f "test-results/postman-report.json" ]; then
        echo "## Postman API Test Results" >> TEST_RESULTS.md
        echo "" >> TEST_RESULTS.md

        # Extract statistics from JSON
        REQUESTS=$(jq '.run.stats.requests.total' test-results/postman-report.json 2>/dev/null || echo "N/A")
        ASSERTIONS=$(jq '.run.stats.assertions.total' test-results/postman-report.json 2>/dev/null || echo "N/A")
        FAILED=$(jq '.run.stats.requests.failed' test-results/postman-report.json 2>/dev/null || echo "N/A")

        echo "- **Total Requests:** $REQUESTS" >> TEST_RESULTS.md
        echo "- **Total Assertions:** $ASSERTIONS" >> TEST_RESULTS.md
        echo "- **Failed Requests:** $FAILED" >> TEST_RESULTS.md
        echo "" >> TEST_RESULTS.md

        # Add link to HTML report
        if [ -f "test-results/postman-report.html" ]; then
            echo "### 📊 Detailed Reports" >> TEST_RESULTS.md
            echo "- [Postman HTML Report](./test-results/postman-report.html)" >> TEST_RESULTS.md
            echo "- [JUnit Test Reports](./backend/target/surefire-reports/)" >> TEST_RESULTS.md
        fi
    fi

    print_success "Test report generated: TEST_RESULTS.md"
}

# Main execution
main() {
    echo "🏃 Starting API test execution..."
    echo ""

    check_dependencies

    echo ""
    print_status "Phase 1: Running JUnit Tests"
    run_junit_tests

    echo ""
    print_status "Phase 2: Running Postman API Tests"
    run_postman_tests

    echo ""
    print_status "Phase 3: Generating Reports"
    generate_report

    echo ""
    print_success "🎉 All API tests completed successfully!"
    echo ""
    echo "📊 Test Results Summary:"
    echo "   - JUnit Tests: ✅ Passed"
    echo "   - Postman API Tests: ✅ Passed"
    echo "   - Reports: 📄 Generated"
    echo ""
    echo "📁 Check TEST_RESULTS.md for detailed results"
    echo "📁 Check test-results/ for Postman reports"
    echo "📁 Check backend/target/surefire-reports/ for JUnit reports"
}

# Handle script arguments
case "${1:-all}" in
    "junit")
        check_dependencies
        run_junit_tests
        ;;
    "postman")
        check_dependencies
        run_postman_tests
        ;;
    "report")
        generate_report
        ;;
    "all")
        main
        ;;
    *)
        echo "Usage: $0 [junit|postman|report|all]"
        echo "  junit   - Run only JUnit tests"
        echo "  postman - Run only Postman API tests"
        echo "  report  - Generate test report"
        echo "  all     - Run all tests (default)"
        exit 1
        ;;
esac
