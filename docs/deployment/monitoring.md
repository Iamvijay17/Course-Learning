# Monitoring & Observability

## Overview

The platform implements comprehensive monitoring and observability using Spring Boot Actuator, distributed tracing, centralized logging, and metrics collection to ensure system reliability, performance, and quick issue resolution.

## Application Monitoring (Spring Boot Actuator)

### Actuator Endpoints Configuration

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus,loggers,env,configprops,threaddump,heapdump
      base-path: /actuator
  endpoint:
    health:
      show-details: when-authorized
      probes:
        enabled: true
    metrics:
      enabled: true
    prometheus:
      enabled: true
  health:
    diskspace:
      enabled: true
    db:
      enabled: true
  metrics:
    export:
      prometheus:
        enabled: true
    tags:
      application: ${spring.application.name}
```

### Custom Health Indicators

```java
@Component
public class DatabaseHealthIndicator implements HealthIndicator {

    @Autowired
    private DataSource dataSource;

    @Override
    public Health health() {
        try (Connection connection = dataSource.getConnection()) {
            // Test database connectivity
            connection.createStatement().execute("SELECT 1");

            // Check connection pool status
            if (connection instanceof HikariProxyConnection) {
                HikariPoolMXBean poolProxy = connection.unwrap(HikariProxyConnection.class)
                    .getHikariPoolMXBean();

                return Health.up()
                    .withDetail("activeConnections", poolProxy.getActiveConnections())
                    .withDetail("idleConnections", poolProxy.getIdleConnections())
                    .withDetail("totalConnections", poolProxy.getTotalConnections())
                    .withDetail("threadsAwaitingConnection", poolProxy.getThreadsAwaitingConnection())
                    .build();
            }

            return Health.up().build();

        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}

@Component
public class ExternalServiceHealthIndicator implements HealthIndicator {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${external.service.url}")
    private String externalServiceUrl;

    @Override
    public Health health() {
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(
                externalServiceUrl + "/health", String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                return Health.up()
                    .withDetail("externalService", "available")
                    .withDetail("responseTime", "fast")
                    .build();
            } else {
                return Health.down()
                    .withDetail("externalService", "unavailable")
                    .withDetail("statusCode", response.getStatusCode())
                    .build();
            }
        } catch (Exception e) {
            return Health.down(e).build();
        }
    }
}
```

### Custom Metrics

```java
@Service
public class CourseMetricsService {

    private final Counter coursesCreated;
    private final Counter coursesEnrolled;
    private final Timer courseCreationTimer;
    private final Gauge courseCount;

    public CourseMetricsService(MeterRegistry registry) {
        this.coursesCreated = Counter.builder("courses.created.total")
                .description("Total number of courses created")
                .register(registry);

        this.coursesEnrolled = Counter.builder("courses.enrollments.total")
                .description("Total number of course enrollments")
                .register(registry);

        this.courseCreationTimer = Timer.builder("courses.creation.duration")
                .description("Time taken to create a course")
                .register(registry);

        // Custom gauge for course count
        this.courseCount = Gauge.builder("courses.count", this::getCourseCount)
                .description("Current number of courses")
                .register(registry);
    }

    public void incrementCoursesCreated() {
        coursesCreated.increment();
    }

    public void incrementEnrollments() {
        coursesEnrolled.increment();
    }

    public void recordCourseCreationTime(Runnable operation) {
        courseCreationTimer.record(operation);
    }

    private int getCourseCount() {
        // Return actual course count from database
        return courseRepository.count();
    }
}

// Usage in service
@Service
public class CourseService {

    @Autowired
    private CourseMetricsService metricsService;

    @Transactional
    public Course createCourse(CourseCreateDTO dto) {
        return metricsService.recordCourseCreationTime(() -> {
            Course course = new Course();
            // ... course creation logic
            courseRepository.save(course);
            metricsService.incrementCoursesCreated();
            return course;
        });
    }
}
```

## Centralized Logging

### Logback Configuration

```xml
<!-- logback-spring.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <include resource="org/springframework/boot/logging/logback/defaults.xml"/>

    <!-- Console appender for development -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <!-- JSON appender for production -->
    <appender name="JSON_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/course-service.json</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/course-service.%d{yyyy-MM-dd}.%i.json</fileNamePattern>
            <maxFileSize>100MB</maxFileSize>
            <maxHistory>30</maxHistory>
            <totalSizeCap>3GB</totalSizeCap>
        </rollingPolicy>
        <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
            <providers>
                <timestamp/>
                <logLevel/>
                <loggerName/>
                <message/>
                <mdc/>
                <stackTrace/>
            </providers>
        </encoder>
    </appender>

    <!-- Async appender for performance -->
    <appender name="ASYNC_JSON" class="ch.qos.logback.classic.AsyncAppender">
        <appender-ref ref="JSON_FILE"/>
        <queueSize>512</queueSize>
        <discardingThreshold>20</discardingThreshold>
    </appender>

    <!-- Root logger -->
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="ASYNC_JSON"/>
    </root>

    <!-- Application specific loggers -->
    <logger name="com.courseplatform" level="DEBUG" additivity="false">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="ASYNC_JSON"/>
    </logger>

    <!-- Spring framework logging -->
    <logger name="org.springframework" level="INFO"/>
    <logger name="org.springframework.security" level="DEBUG"/>

    <!-- Hibernate logging -->
    <logger name="org.hibernate.SQL" level="DEBUG"/>
    <logger name="org.hibernate.type.descriptor.sql.BasicBinder" level="TRACE"/>
</configuration>
```

### Structured Logging

```java
@Service
public class CourseService {

    private static final Logger logger = LoggerFactory.getLogger(CourseService.class);

    public Course createCourse(CourseCreateDTO dto) {
        logger.info("Creating new course",
            kv("courseTitle", dto.getTitle()),
            kv("instructorId", dto.getInstructorId()),
            kv("operation", "course_creation"));

        try {
            Course course = courseRepository.save(courseMapper.toEntity(dto));

            logger.info("Course created successfully",
                kv("courseId", course.getId()),
                kv("courseTitle", course.getTitle()),
                kv("operation", "course_creation"),
                kv("duration", System.currentTimeMillis() - startTime));

            return course;

        } catch (Exception e) {
            logger.error("Failed to create course",
                kv("courseTitle", dto.getTitle()),
                kv("error", e.getMessage()),
                kv("operation", "course_creation"), e);

            throw e;
        }
    }
}
```

### Log Aggregation with ELK Stack

```yaml
# Filebeat configuration
filebeat.inputs:
- type: log
  enabled: true
  paths:
    - /var/log/course-service/*.json
  json.keys_under_root: true
  json.add_error_key: true

processors:
- add_kubernetes_metadata:
    host: ${NODE_NAME}
    matchers:
    - logs_path:
        logs_path: "/var/log/containers/"

output.elasticsearch:
  hosts: ["elasticsearch:9200"]
  index: "course-service-%{+yyyy.MM.dd}"
```

## Metrics Collection (Prometheus)

### Prometheus Configuration

```yaml
# prometheus.yml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

rule_files:
  - "alert_rules.yml"

alerting:
  alertmanagers:
    - static_configs:
        - targets:
          - alertmanager:9093

scrape_configs:
  - job_name: 'course-service'
    static_configs:
      - targets: ['course-service:8080']
    metrics_path: '/actuator/prometheus'
    scrape_interval: 5s

  - job_name: 'user-service'
    static_configs:
      - targets: ['user-service:8080']
    metrics_path: '/actuator/prometheus'
    scrape_interval: 5s

  - job_name: 'enrollment-service'
    static_configs:
      - targets: ['enrollment-service:8080']
    metrics_path: '/actuator/prometheus'
    scrape_interval: 5s

  - job_name: 'api-gateway'
    static_configs:
      - targets: ['api-gateway:8080']
    metrics_path: '/actuator/prometheus'
    scrape_interval: 5s
```

### Alert Rules

```yaml
# alert_rules.yml
groups:
- name: course-platform
  rules:
  - alert: HighErrorRate
    expr: rate(http_server_requests_seconds_count{status=~"5.."}[5m]) / rate(http_server_requests_seconds_count[5m]) > 0.05
    for: 5m
    labels:
      severity: critical
    annotations:
      summary: "High error rate detected"
      description: "Error rate is {{ $value }}% for {{ $labels.service }}"

  - alert: HighResponseTime
    expr: histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m])) > 2
    for: 5m
    labels:
      severity: warning
    annotations:
      summary: "High response time detected"
      description: "95th percentile response time is {{ $value }}s for {{ $labels.service }}"

  - alert: LowDiskSpace
    expr: (1 - (node_filesystem_avail_bytes / node_filesystem_size_bytes)) * 100 > 85
    for: 5m
    labels:
      severity: warning
    annotations:
      summary: "Low disk space"
      description: "Disk usage is {{ $value }}% on {{ $labels.instance }}"

  - alert: DatabaseConnectionPoolExhausted
    expr: hikari_active_connections / hikari_total_connections > 0.9
    for: 5m
    labels:
      severity: critical
    annotations:
      summary: "Database connection pool exhausted"
      description: "Connection pool usage is {{ $value }}% for {{ $labels.pool }}"
```

## Distributed Tracing (Jaeger)

### Jaeger Configuration

```yaml
# application.yml
opentracing:
  jaeger:
    enabled: true
    service-name: ${spring.application.name}
    udp-sender:
      host: jaeger-agent
      port: 6831
    log-spans: false
    probabilistic-sampler:
      sampling-rate: 0.1

spring:
  sleuth:
    enabled: true
    sampler:
      probability: 0.1
```

### Custom Tracing

```java
@Service
public class CourseService {

    @Autowired
    private Tracer tracer;

    public Course getCourse(Long courseId) {
        Span span = tracer.buildSpan("getCourse").start();
        span.setTag("courseId", courseId);

        try (Tracer.SpanInScope ws = tracer.withSpanInScope(span)) {
            logger.info("Fetching course with ID: {}", courseId);

            Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));

            span.setTag("courseTitle", course.getTitle());
            span.setTag("instructorId", course.getInstructorId());

            return course;

        } catch (Exception e) {
            span.setTag("error", true);
            span.log(Map.of("error.message", e.getMessage()));
            throw e;
        } finally {
            span.finish();
        }
    }
}
```

### Inter-Service Tracing

```java
@Service
public class EnrollmentService {

    @Autowired
    private RestTemplate restTemplate;

    public Enrollment enrollInCourse(Long userId, Long courseId) {
        Span span = tracer.buildSpan("enrollInCourse").start();

        try (Tracer.SpanInScope ws = tracer.withSpanInScope(span)) {
            // Check if course exists (inter-service call)
            Span courseCheckSpan = tracer.buildSpan("checkCourseExists")
                .asChildOf(span)
                .start();

            try (Tracer.SpanInScope courseScope = tracer.withSpanInScope(courseCheckSpan)) {
                ResponseEntity<CourseDTO> courseResponse = restTemplate.getForEntity(
                    "http://course-service/api/courses/" + courseId, CourseDTO.class);

                if (!courseResponse.getStatusCode().is2xxSuccessful()) {
                    throw new RuntimeException("Course not available");
                }
            } finally {
                courseCheckSpan.finish();
            }

            // Create enrollment
            Enrollment enrollment = new Enrollment();
            enrollment.setUserId(userId);
            enrollment.setCourseId(courseId);
            enrollment.setEnrollmentDate(LocalDateTime.now());

            return enrollmentRepository.save(enrollment);

        } finally {
            span.finish();
        }
    }
}
```

## Alerting (Alertmanager)

### Alertmanager Configuration

```yaml
# alertmanager.yml
global:
  smtp_smarthost: 'smtp.gmail.com:587'
  smtp_from: 'alerts@courseplatform.com'
  smtp_auth_username: 'alerts@courseplatform.com'
  smtp_auth_password: 'your-password'

route:
  group_by: ['alertname']
  group_wait: 10s
  group_interval: 10s
  repeat_interval: 1h
  receiver: 'team'
  routes:
  - match:
      severity: critical
    receiver: 'team-critical'

receivers:
- name: 'team'
  email_configs:
  - to: 'team@courseplatform.com'
    subject: '[{{ .GroupLabels.alertname }}] {{ .Annotations.summary }}'
    body: |
      {{ range .Alerts }}
      Alert: {{ .Annotations.summary }}
      Description: {{ .Annotations.description }}
      Runbook: {{ .Annotations.runbook }}
      {{ end }}

- name: 'team-critical'
  email_configs:
  - to: 'team@courseplatform.com'
  slack_configs:
  - api_url: 'https://hooks.slack.com/services/YOUR/SLACK/WEBHOOK'
    channel: '#alerts-critical'
    title: '{{ .GroupLabels.alertname }}'
    text: |
      {{ range .Alerts }}
      *Alert:* {{ .Annotations.summary }}
      *Description:* {{ .Annotations.description }}
      *Severity:* {{ .Labels.severity }}
      {{ end }}
```

## Dashboard (Grafana)

### Key Metrics Dashboard

```json
{
  "dashboard": {
    "title": "Course Platform Overview",
    "tags": ["course-platform"],
    "panels": [
      {
        "title": "Request Rate",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(http_server_requests_seconds_count[5m])",
            "legendFormat": "{{service}}"
          }
        ]
      },
      {
        "title": "Error Rate",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(http_server_requests_seconds_count{status=~\"5..\"}[5m]) / rate(http_server_requests_seconds_count[5m]) * 100",
            "legendFormat": "{{service}}"
          }
        ]
      },
      {
        "title": "Response Time (95th percentile)",
        "type": "graph",
        "targets": [
          {
            "expr": "histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))",
            "legendFormat": "{{service}}"
          }
        ]
      },
      {
        "title": "Database Connections",
        "type": "graph",
        "targets": [
          {
            "expr": "hikari_active_connections",
            "legendFormat": "Active - {{pool}}"
          },
          {
            "expr": "hikari_idle_connections",
            "legendFormat": "Idle - {{pool}}"
          }
        ]
      },
      {
        "title": "JVM Memory Usage",
        "type": "graph",
        "targets": [
          {
            "expr": "jvm_memory_used_bytes / jvm_memory_max_bytes * 100",
            "legendFormat": "{{area}} - {{service}}"
          }
        ]
      },
      {
        "title": "Business Metrics",
        "type": "stat",
        "targets": [
          {
            "expr": "courses_created_total",
            "legendFormat": "Courses Created"
          },
          {
            "expr": "courses_enrollments_total",
            "legendFormat": "Total Enrollments"
          }
        ]
      }
    ]
  }
}
```

## Log Analysis & Correlation

### Log Correlation with Tracing

```java
@Component
public class LoggingAspect {

    @Autowired
    private Tracer tracer;

    @Around("@annotation(org.springframework.web.bind.annotation.RequestMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.GetMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.PostMapping)")
    public Object logControllerMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        Span span = tracer.activeSpan();
        if (span != null) {
            String traceId = span.context().toTraceId();
            String spanId = span.context().toSpanId();

            MDC.put("traceId", traceId);
            MDC.put("spanId", spanId);
        }

        try {
            logger.info("Entering method: {}.{}",
                joinPoint.getSignature().getDeclaringTypeName(),
                joinPoint.getSignature().getName());

            Object result = joinPoint.proceed();

            logger.info("Exiting method: {}.{}",
                joinPoint.getSignature().getDeclaringTypeName(),
                joinPoint.getSignature().getName());

            return result;

        } finally {
            MDC.clear();
        }
    }
}
```

### Error Tracking (Sentry)

```yaml
# application.yml
sentry:
  dsn: https://your-sentry-dsn@sentry.io/project-id
  environment: production
  release: ${app.version:1.0.0}
  servername: ${spring.application.name}
  before-send:
    - com.courseplatform.monitoring.SentryEventProcessor
```

```java
@Component
public class SentryEventProcessor implements EventProcessor {

    @Autowired
    private Tracer tracer;

    @Override
    public SentryEvent process(SentryEvent event, Hint hint) {
        // Add tracing information to Sentry events
        Span span = tracer.activeSpan();
        if (span != null) {
            event.setTag("traceId", span.context().toTraceId());
            event.setTag("spanId", span.context().toSpanId());
        }

        // Add user context if available
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User) {
            User user = (User) auth.getPrincipal();
            event.setTag("userId", user.getId().toString());
            event.setTag("userRole", user.getRole().toString());
        }

        return event;
    }
}
```

## Performance Monitoring

### Application Performance Monitoring (APM)

```java
@Configuration
public class ApmConfiguration {

    @Bean
    public MeterRegistry meterRegistry() {
        return new CompositeMeterRegistry();
    }

    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }
}

// Usage
@Service
public class CourseService {

    @Timed(value = "course.service.getCourse", description = "Time taken to get course")
    public Course getCourse(Long courseId) {
        // Method implementation
    }

    @Counted(value = "course.service.createCourse", description = "Number of course creations")
    public Course createCourse(CourseCreateDTO dto) {
        // Method implementation
    }
}
```

### Database Performance Monitoring

```sql
-- Slow query log configuration
SET GLOBAL slow_query_log = 'ON';
SET GLOBAL long_query_time = 2; -- Log queries taking more than 2 seconds
SET GLOBAL slow_query_log_file = '/var/log/mysql/mysql-slow.log';

-- Query performance analysis
SELECT
    sql_text,
    exec_count,
    avg_timer_wait/1000000000 as avg_time_sec,
    min_timer_wait/1000000000 as min_time_sec,
    max_timer_wait/1000000000 as max_time_sec
FROM performance_schema.events_statements_summary_by_digest
WHERE schema_name = 'course_db'
ORDER BY avg_timer_wait DESC
LIMIT 10;
```

## Incident Response

### Runbooks

#### High Error Rate Runbook

1. **Detection**: Alert triggered when error rate > 5% for 5 minutes
2. **Investigation**:
   - Check application logs for error patterns
   - Review recent deployments
   - Check database connectivity
   - Monitor resource utilization
3. **Response**:
   - Rollback to previous version if deployment-related
   - Scale up resources if load-related
   - Restart services if memory leaks suspected
4. **Recovery**:
   - Verify error rate returns to normal
   - Update incident ticket
   - Schedule post-mortem if major incident

#### Database Connection Pool Exhausted Runbook

1. **Detection**: Alert when connection pool usage > 90%
2. **Investigation**:
   - Check active connections: `SHOW PROCESSLIST;`
   - Review slow queries
   - Check connection leaks in application code
3. **Response**:
   - Kill long-running queries
   - Restart application services
   - Scale database if needed
4. **Prevention**:
   - Implement connection pooling best practices
   - Add database monitoring
   - Review and optimize slow queries

## Monitoring as Code

### Infrastructure Monitoring

```yaml
# Kubernetes monitoring
apiVersion: v1
kind: ServiceMonitor
metadata:
  name: course-platform-services
spec:
  selector:
    matchLabels:
      app.kubernetes.io/name: course-platform
  endpoints:
  - port: http
    path: /actuator/prometheus
    interval: 30s
    scrapeTimeout: 10s
```

### Automated Monitoring Tests

```java
@SpringBootTest
public class MonitoringIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    public void healthEndpointShouldReturnHealthy() {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/health", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        JsonNode health = new ObjectMapper().readTree(response.getBody());
        assertThat(health.get("status").asText()).isEqualTo("UP");
    }

    @Test
    public void metricsEndpointShouldReturnMetrics() {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/metrics", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        JsonNode metrics = new ObjectMapper().readTree(response.getBody());
        assertThat(metrics.get("names")).isNotEmpty();
    }

    @Test
    public void prometheusEndpointShouldReturnPrometheusFormat() {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/prometheus", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("# HELP");
        assertThat(response.getBody()).contains("# TYPE");
    }
}
```

This comprehensive monitoring and observability setup ensures the platform's reliability, performance, and maintainability through proactive monitoring, quick issue detection, and efficient incident response.
