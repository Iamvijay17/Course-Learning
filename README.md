# Course Learning Platform

A comprehensive e-learning platform built with Spring Boot and React.js.

## Quick Start with Docker

1. **Prerequisites**: Make sure you have Docker and Docker Compose installed.

2. **Clone and run**:
   ```bash
   git clone <repository-url>
   cd course-learning
   docker-compose up --build
   ```

3. **Access the application**:
   - Backend API: http://localhost:8080
   - Database: TiDB Cloud (configured in docker-compose.yml)

4. **Stop the application**:
   ```bash
   docker-compose down
   ```

## Documentation

For detailed documentation, see the [docs/](docs/) directory.

## Development

- Backend: Spring Boot 4.0.1 with Java 21
- Database: MySQL 8.0
- Security: JWT authentication
- API Documentation: Swagger/OpenAPI

## Project Structure

```
├── backend/          # Spring Boot application
├── docs/            # Documentation
├── docker-compose.yml # Docker orchestration
└── README.md        # This file
