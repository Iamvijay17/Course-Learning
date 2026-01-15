# Authentication & Authorization

## Overview

The platform implements JWT (JSON Web Token) based authentication with role-based access control (RBAC) to secure API endpoints and ensure proper user authorization across all microservices.

## Authentication Flow

### User Registration & Login

```
1. User Registration
   Client → API Gateway → User Service
   ↓
   Validate input → Hash password → Create user → Send verification email

2. Email Verification (Optional)
   User clicks verification link → User Service → Mark email as verified

3. User Login
   Client → API Gateway → User Service
   ↓
   Validate credentials → Generate JWT tokens → Return tokens

4. Token Usage
   Client includes JWT in Authorization header for subsequent requests
   ↓
   API Gateway validates token → Routes to appropriate service
```

## JWT Token Management

### Token Types

#### Access Token
- **Purpose**: Authenticate API requests
- **Expiration**: 15 minutes (short-lived for security)
- **Storage**: Memory only (not persisted)
- **Refresh**: Automatic via refresh token

#### Refresh Token
- **Purpose**: Obtain new access tokens
- **Expiration**: 7 days
- **Storage**: HttpOnly, Secure cookie or secure storage
- **Rotation**: New refresh token issued with each use

### Token Structure

```json
// Access Token Payload
{
  "sub": "user_123",
  "email": "user@example.com",
  "role": "STUDENT",
  "permissions": ["course:read", "enrollment:create"],
  "iat": 1640995200,
  "exp": 1640996100,
  "iss": "course-platform",
  "aud": "course-platform-api",
  "jti": "token_456"
}

// Refresh Token Payload
{
  "sub": "user_123",
  "type": "refresh",
  "iat": 1640995200,
  "exp": 1641600000,
  "iss": "course-platform",
  "jti": "refresh_789"
}
```

### Token Generation

```java
@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.access-token.expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token.expiration}")
    private long refreshTokenExpiration;

    public String generateAccessToken(User user) {
        return Jwts.builder()
                .setSubject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .claim("permissions", getPermissionsForRole(user.getRole()))
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
                .setIssuer("course-platform")
                .setAudience("course-platform-api")
                .setId(UUID.randomUUID().toString())
                .signWith(SignatureAlgorithm.HS256, secretKey)
                .compact();
    }

    public String generateRefreshToken(User user) {
        return Jwts.builder()
                .setSubject(user.getId().toString())
                .claim("type", "refresh")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + refreshTokenExpiration))
                .setIssuer("course-platform")
                .setId(UUID.randomUUID().toString())
                .signWith(SignatureAlgorithm.HS256, secretKey)
                .compact();
    }
}
```

## Password Security

### Password Requirements
- Minimum 8 characters
- At least one uppercase letter
- At least one lowercase letter
- At least one number
- At least one special character

### Password Hashing
```java
@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12); // Strength 12 for better security
    }
}

// Usage
@Service
public class UserService {

    @Autowired
    private PasswordEncoder passwordEncoder;

    public User registerUser(UserRegistrationDTO dto) {
        String hashedPassword = passwordEncoder.encode(dto.getPassword());
        // Save user with hashed password
    }

    public boolean validatePassword(String rawPassword, String hashedPassword) {
        return passwordEncoder.matches(rawPassword, hashedPassword);
    }
}
```

### Password Reset Flow
```
1. User requests password reset → Enter email
2. System generates reset token → Send email with reset link
3. User clicks link → Frontend loads reset form
4. User submits new password → Validate token → Update password
5. Invalidate reset token → Send confirmation email
```

## Role-Based Access Control (RBAC)

### User Roles

#### STUDENT
```json
{
  "role": "STUDENT",
  "permissions": [
    "course:read",
    "course:search",
    "enrollment:create",
    "enrollment:read",
    "enrollment:update",
    "profile:read",
    "profile:update"
  ]
}
```

#### INSTRUCTOR
```json
{
  "role": "INSTRUCTOR",
  "permissions": [
    "course:read",
    "course:create",
    "course:update",
    "course:delete",
    "course:publish",
    "enrollment:read",
    "content:create",
    "content:update",
    "content:delete",
    "profile:read",
    "profile:update",
    "analytics:read"
  ]
}
```

#### ADMIN
```json
{
  "role": "ADMIN",
  "permissions": [
    "*:*"
  ]
}
```

### Permission Checking

```java
@Component
public class PermissionEvaluator {

    public boolean hasPermission(Authentication auth, String permission) {
        User user = (User) auth.getPrincipal();
        Set<String> userPermissions = getPermissionsForRole(user.getRole());

        // Check exact permission match
        if (userPermissions.contains(permission)) {
            return true;
        }

        // Check wildcard permissions (e.g., "course:*" covers "course:read")
        String[] parts = permission.split(":");
        if (parts.length == 2) {
            String wildcardPermission = parts[0] + ":*";
            return userPermissions.contains(wildcardPermission);
        }

        return false;
    }
}
```

### Method-Level Security

```java
@RestController
@RequestMapping("/api/courses")
public class CourseController {

    @PostMapping
    @PreAuthorize("hasPermission('course:create')")
    public ResponseEntity<CourseDTO> createCourse(@RequestBody CourseCreateDTO dto) {
        // Only instructors and admins can create courses
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission('course:read')")
    public ResponseEntity<CourseDTO> getCourse(@PathVariable Long id) {
        // All authenticated users can read courses
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasPermission('course:update') and @courseSecurity.isOwner(#id, principal)")
    public ResponseEntity<CourseDTO> updateCourse(@PathVariable Long id, @RequestBody CourseUpdateDTO dto) {
        // Only course owner or admin can update
    }
}
```

## API Gateway Security

### Request Filtering

```java
@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("user-service", r -> r.path("/api/auth/**", "/api/users/**")
                        .filters(f -> f.filter(authenticationFilter()))
                        .uri("lb://user-service"))
                .route("course-service", r -> r.path("/api/courses/**")
                        .filters(f -> f.filter(jwtAuthenticationFilter()))
                        .uri("lb://course-service"))
                .build();
    }
}
```

### JWT Validation Filter

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String token = extractToken(request);

        if (token != null && jwtService.validateToken(token)) {
            Claims claims = jwtService.getClaimsFromToken(token);
            String userId = claims.getSubject();
            String role = claims.get("role", String.class);

            // Set authentication context
            UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userId, null, getAuthorities(role));
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
```

## Security Best Practices

### Token Security
- Use strong secret keys (256-bit minimum)
- Implement token rotation
- Blacklist compromised tokens
- Use HTTPS only (no plain HTTP)

### Session Management
- Implement proper logout (invalidate tokens)
- Handle concurrent sessions
- Set secure cookie attributes
- Implement session timeout

### Rate Limiting
```java
@Configuration
public class RateLimitConfig {

    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String token = exchange.getRequest().getHeaders().getFirst("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                // Extract user ID from JWT token
                return Mono.just(extractUserIdFromToken(token));
            }
            return Mono.just(exchange.getRequest().getRemoteAddress().getAddress().getHostAddress());
        };
    }
}
```

### CORS Configuration
```java
@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedOrigin("https://courseplatform.com");
        config.addAllowedOrigin("https://app.courseplatform.com");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return new CorsFilter(source);
    }
}
```

## Security Monitoring

### Audit Logging
```java
@Aspect
@Component
public class SecurityAuditAspect {

    @Autowired
    private AuditLogService auditLogService;

    @AfterReturning(pointcut = "execution(* com.courseplatform..*Controller.*(..))",
                    returning = "result")
    public void logSuccessfulOperation(JoinPoint joinPoint, Object result) {
        auditLogService.log(
            SecurityContextHolder.getContext().getAuthentication(),
            joinPoint.getSignature().toString(),
            "SUCCESS",
            null
        );
    }

    @AfterThrowing(pointcut = "execution(* com.courseplatform..*Controller.*(..))",
                   throwing = "ex")
    public void logFailedOperation(JoinPoint joinPoint, Exception ex) {
        auditLogService.log(
            SecurityContextHolder.getContext().getAuthentication(),
            joinPoint.getSignature().toString(),
            "FAILED",
            ex.getMessage()
        );
    }
}
```

### Security Events
- Failed login attempts
- Token validation failures
- Permission denied attempts
- Suspicious activity patterns
- Password reset requests

## Multi-Factor Authentication (Future)

### Implementation Plan
1. **TOTP (Time-based One-Time Password)**
   - Google Authenticator integration
   - Backup codes for recovery
   - Optional for high-risk operations

2. **SMS/Email Verification**
   - For password resets
   - Account security changes
   - High-value transactions

3. **Biometric Authentication**
   - Fingerprint/face recognition
   - Device-based authentication

## Compliance & Standards

### GDPR Compliance
- Right to erasure (delete user data)
- Data portability
- Consent management
- Privacy by design

### Security Standards
- OWASP Top 10 compliance
- JWT RFC 7519 compliance
- BCrypt for password hashing
- HTTPS enforcement

### Penetration Testing
- Regular security audits
- Automated vulnerability scanning
- Third-party security assessments
- Bug bounty program

## Configuration

### Environment Variables
```bash
# JWT Configuration
JWT_SECRET=your-super-secure-secret-key-here
JWT_ACCESS_TOKEN_EXPIRATION=900000        # 15 minutes
JWT_REFRESH_TOKEN_EXPIRATION=604800000    # 7 days

# Security Headers
SECURITY_CORS_ALLOWED_ORIGINS=https://courseplatform.com,https://app.courseplatform.com
SECURITY_RATE_LIMIT_REQUESTS_PER_MINUTE=100

# Password Policy
PASSWORD_MIN_LENGTH=8
PASSWORD_REQUIRE_UPPERCASE=true
PASSWORD_REQUIRE_LOWERCASE=true
PASSWORD_REQUIRE_DIGITS=true
PASSWORD_REQUIRE_SPECIAL_CHARS=true
```

### Spring Security Configuration
```java
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .cors().and()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .authorizeRequests()
                .antMatchers("/api/auth/**").permitAll()
                .antMatchers("/api/public/**").permitAll()
                .anyRequest().authenticated()
            .and()
            .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
    }
}
