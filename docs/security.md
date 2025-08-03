# Security

- [Authentication](#authentication)
- [Authorization](#authorization)
- [Input Validation](#input-validation)
- [Rate Limiting](#rate-limiting)
- [Security Headers](#security-headers)
- [JWT Implementation](#jwt-implementation)
- [Best Practices](#best-practices)

## Authentication

### Basic Authentication Middleware

```java
import heo.interfaces.Middleware;
import heo.http.Request;
import heo.http.Response;
import heo.middleware.MiddlewareChain;
import heo.exception.UnauthorizedError;
import java.util.Base64;

public class BasicAuthMiddleware implements Middleware {
    private final String username;
    private final String password;

    public BasicAuthMiddleware(String username, String password) {
        this.username = username;
        this.password = password;
    }

    @Override
    public void handle(Request req, Response res, MiddlewareChain next) {
        String authHeader = req.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            res.setHeader("WWW-Authenticate", "Basic realm=\"Protected Area\"");
            throw new UnauthorizedError("Basic authentication required");
        }

        String encodedCredentials = authHeader.substring(6);
        String credentials = new String(Base64.getDecoder().decode(encodedCredentials));
        String[] parts = credentials.split(":", 2);

        if (parts.length != 2 || !username.equals(parts[0]) || !password.equals(parts[1])) {
            res.setHeader("WWW-Authenticate", "Basic realm=\"Protected Area\"");
            throw new UnauthorizedError("Invalid credentials");
        }

        req.setAttribute("authenticated", true);
        req.setAttribute("username", parts[0]);
        next.next();
    }
}

// Usage
app.use("/admin", new BasicAuthMiddleware("admin", "secret123"));
```

### Session-Based Authentication

```java
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class SessionManager {
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private final long sessionTimeout = 30 * 60 * 1000; // 30 minutes

    public String createSession(User user) {
        String sessionId = UUID.randomUUID().toString();
        Session session = new Session(sessionId, user, System.currentTimeMillis() + sessionTimeout);
        sessions.put(sessionId, session);
        return sessionId;
    }

    public Session getSession(String sessionId) {
        Session session = sessions.get(sessionId);
        if (session != null && session.getExpiresAt() > System.currentTimeMillis()) {
            // Extend session
            session.setExpiresAt(System.currentTimeMillis() + sessionTimeout);
            return session;
        }
        // Remove expired session
        if (session != null) {
            sessions.remove(sessionId);
        }
        return null;
    }

    public void invalidateSession(String sessionId) {
        sessions.remove(sessionId);
    }

    public static class Session {
        private String sessionId;
        private User user;
        private long expiresAt;

        public Session(String sessionId, User user, long expiresAt) {
            this.sessionId = sessionId;
            this.user = user;
            this.expiresAt = expiresAt;
        }

        // Getters and setters...
    }
}

public class SessionAuthMiddleware implements Middleware {
    private final SessionManager sessionManager;

    public SessionAuthMiddleware(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Override
    public void handle(Request req, Response res, MiddlewareChain next) {
        String sessionId = getCookieValue(req, "sessionId");

        if (sessionId == null) {
            throw new UnauthorizedError("No session found");
        }

        SessionManager.Session session = sessionManager.getSession(sessionId);
        if (session == null) {
            throw new UnauthorizedError("Invalid or expired session");
        }

        req.setAttribute("user", session.getUser());
        req.setAttribute("sessionId", sessionId);
        next.next();
    }

    private String getCookieValue(Request req, String cookieName) {
        String cookieHeader = req.getHeader("Cookie");
        if (cookieHeader != null) {
            String[] cookies = cookieHeader.split(";");
            for (String cookie : cookies) {
                String[] parts = cookie.trim().split("=", 2);
                if (parts.length == 2 && cookieName.equals(parts[0])) {
                    return parts[1];
                }
            }
        }
        return null;
    }
}
```

### Login/Logout Endpoints

```java
// Login endpoint
app.post("/auth/login", (req, res, next) -> {
    String email = req.getBody("email");
    String password = req.getBody("password");

    if (email == null || password == null) {
        throw new BadRequest("Email and password are required");
    }

    // Authenticate user
    Optional<User> userOpt = userService.authenticate(email, password);
    if (userOpt.isEmpty()) {
        throw new UnauthorizedError("Invalid email or password");
    }

    User user = userOpt.get();
    String sessionId = sessionManager.createSession(user);

    // Set session cookie
    res.setHeader("Set-Cookie",
        "sessionId=" + sessionId + "; Path=/; HttpOnly; SameSite=Strict; Max-Age=1800");

    res.json(Map.of(
        "success", true,
        "user", Map.of(
            "id", user.getId(),
            "email", user.getEmail(),
            "name", user.getName()
        )
    ));
});

// Logout endpoint
app.post("/auth/logout", sessionAuthMiddleware, (req, res, next) -> {
    String sessionId = (String) req.getAttribute("sessionId");
    sessionManager.invalidateSession(sessionId);

    // Clear session cookie
    res.setHeader("Set-Cookie",
        "sessionId=; Path=/; HttpOnly; SameSite=Strict; Max-Age=0");

    res.json(Map.of("success", true, "message", "Logged out successfully"));
});
```

## Authorization

### Role-Based Access Control (RBAC)

```java
public enum Role {
    USER("user"),
    ADMIN("admin"),
    MODERATOR("moderator");

    private final String value;

    Role(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}

public class RoleAuthorizationMiddleware implements Middleware {
    private final Set<Role> requiredRoles;

    public RoleAuthorizationMiddleware(Role... roles) {
        this.requiredRoles = Set.of(roles);
    }

    @Override
    public void handle(Request req, Response res, MiddlewareChain next) {
        User user = (User) req.getAttribute("user");

        if (user == null) {
            throw new UnauthorizedError("Authentication required");
        }

        boolean hasRequiredRole = user.getRoles().stream()
            .anyMatch(requiredRoles::contains);

        if (!hasRequiredRole) {
            throw new ForbiddenError("Insufficient permissions");
        }

        next.next();
    }
}

// Usage
app.use("/admin", new RoleAuthorizationMiddleware(Role.ADMIN));
app.use("/api/users", new RoleAuthorizationMiddleware(Role.ADMIN, Role.MODERATOR));
```

### Permission-Based Authorization

```java
public class Permission {
    public static final String USER_READ = "user:read";
    public static final String USER_WRITE = "user:write";
    public static final String USER_DELETE = "user:delete";
    public static final String POST_READ = "post:read";
    public static final String POST_WRITE = "post:write";
    public static final String POST_DELETE = "post:delete";
}

public class PermissionAuthorizationMiddleware implements Middleware {
    private final Set<String> requiredPermissions;

    public PermissionAuthorizationMiddleware(String... permissions) {
        this.requiredPermissions = Set.of(permissions);
    }

    @Override
    public void handle(Request req, Response res, MiddlewareChain next) {
        User user = (User) req.getAttribute("user");

        if (user == null) {
            throw new UnauthorizedError("Authentication required");
        }

        boolean hasPermission = user.getPermissions().stream()
            .anyMatch(requiredPermissions::contains);

        if (!hasPermission) {
            throw new ForbiddenError("Insufficient permissions");
        }

        next.next();
    }
}

// Usage
app.get("/api/users",
    new PermissionAuthorizationMiddleware(Permission.USER_READ),
    userController::getUsers);

app.delete("/api/users/:id",
    new PermissionAuthorizationMiddleware(Permission.USER_DELETE),
    userController::deleteUser);
```

### Resource-Based Authorization

```java
public class ResourceAuthorizationMiddleware implements Middleware {
    private final ResourceChecker resourceChecker;

    public ResourceAuthorizationMiddleware(ResourceChecker resourceChecker) {
        this.resourceChecker = resourceChecker;
    }

    @Override
    public void handle(Request req, Response res, MiddlewareChain next) {
        User user = (User) req.getAttribute("user");

        if (user == null) {
            throw new UnauthorizedError("Authentication required");
        }

        if (!resourceChecker.canAccess(user, req)) {
            throw new ForbiddenError("Access denied to this resource");
        }

        next.next();
    }

    @FunctionalInterface
    public interface ResourceChecker {
        boolean canAccess(User user, Request request);
    }
}

// Usage for user profile access
app.get("/api/users/:id",
    sessionAuthMiddleware,
    new ResourceAuthorizationMiddleware((user, req) -> {
        int requestedUserId = Integer.parseInt(req.getParam("id"));
        return user.getId() == requestedUserId || user.hasRole(Role.ADMIN);
    }),
    userController::getUserById);
```

## Input Validation

### Input Sanitization Middleware

```java
public class SanitizationMiddleware implements Middleware {
    @Override
    public void handle(Request req, Response res, MiddlewareChain next) {
        // Sanitize query parameters
        Map<String, String> sanitizedQuery = new HashMap<>();
        req.getQueryParams().forEach((key, value) -> {
            sanitizedQuery.put(sanitizeInput(key), sanitizeInput(value));
        });
        req.setAttribute("sanitizedQuery", sanitizedQuery);

        // Sanitize body parameters
        if (req.hasBody()) {
            Map<String, Object> body = req.getBody();
            Map<String, Object> sanitizedBody = new HashMap<>();
            body.forEach((key, value) -> {
                if (value instanceof String) {
                    sanitizedBody.put(sanitizeInput(key), sanitizeInput((String) value));
                } else {
                    sanitizedBody.put(sanitizeInput(key), value);
                }
            });
            req.setAttribute("sanitizedBody", sanitizedBody);
        }

        next.next();
    }

    private String sanitizeInput(String input) {
        if (input == null) return null;

        return input
            .replaceAll("<script[^>]*>.*?</script>", "") // Remove script tags
            .replaceAll("<[^>]+>", "") // Remove HTML tags
            .replaceAll("javascript:", "") // Remove javascript: protocol
            .replaceAll("on\\w+\\s*=", "") // Remove event handlers
            .trim();
    }
}
```

### SQL Injection Prevention

```java
public class SQLInjectionPreventionMiddleware implements Middleware {
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
        "('(''|[^'])*')|(;)|(\\b(ALTER|CREATE|DELETE|DROP|EXEC(UTE)?|INSERT( +INTO)?|MERGE|SELECT|UPDATE|UNION( +ALL)?)\\b)",
        Pattern.CASE_INSENSITIVE
    );

    @Override
    public void handle(Request req, Response res, MiddlewareChain next) {
        // Check query parameters
        req.getQueryParams().forEach((key, value) -> {
            if (containsSQLInjection(value)) {
                throw new BadRequest("Invalid input detected: " + key);
            }
        });

        // Check body parameters
        if (req.hasBody()) {
            checkObjectForSQLInjection(req.getBody());
        }

        next.next();
    }

    private boolean containsSQLInjection(String input) {
        if (input == null) return false;
        return SQL_INJECTION_PATTERN.matcher(input).find();
    }

    private void checkObjectForSQLInjection(Object obj) {
        if (obj instanceof String) {
            if (containsSQLInjection((String) obj)) {
                throw new BadRequest("Invalid input detected");
            }
        } else if (obj instanceof Map) {
            ((Map<?, ?>) obj).forEach((key, value) -> {
                checkObjectForSQLInjection(key);
                checkObjectForSQLInjection(value);
            });
        } else if (obj instanceof List) {
            ((List<?>) obj).forEach(this::checkObjectForSQLInjection);
        }
    }
}
```

## Rate Limiting

### IP-Based Rate Limiting

```java
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class RateLimitMiddleware implements Middleware {
    private final Map<String, RateLimitInfo> requests = new ConcurrentHashMap<>();
    private final int maxRequests;
    private final long windowMs;

    public RateLimitMiddleware(int maxRequests, long windowMs) {
        this.maxRequests = maxRequests;
        this.windowMs = windowMs;
    }

    @Override
    public void handle(Request req, Response res, MiddlewareChain next) {
        String clientIp = getClientIp(req);
        long now = System.currentTimeMillis();

        RateLimitInfo rateLimitInfo = requests.computeIfAbsent(clientIp,
            k -> new RateLimitInfo(now, new AtomicInteger(0)));

        // Reset counter if window has passed
        if (now - rateLimitInfo.windowStart > windowMs) {
            rateLimitInfo.windowStart = now;
            rateLimitInfo.requestCount.set(0);
        }

        int currentCount = rateLimitInfo.requestCount.incrementAndGet();

        // Set rate limit headers
        res.setHeader("X-RateLimit-Limit", String.valueOf(maxRequests));
        res.setHeader("X-RateLimit-Remaining",
            String.valueOf(Math.max(0, maxRequests - currentCount)));
        res.setHeader("X-RateLimit-Reset",
            String.valueOf(rateLimitInfo.windowStart + windowMs));

        if (currentCount > maxRequests) {
            res.setHeader("Retry-After", String.valueOf(windowMs / 1000));
            throw new RateLimitError("Rate limit exceeded", (int) (windowMs / 1000));
        }

        next.next();
    }

    private String getClientIp(Request req) {
        String xForwardedFor = req.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return req.getHeader("X-Real-IP");
    }

    private static class RateLimitInfo {
        long windowStart;
        AtomicInteger requestCount;

        RateLimitInfo(long windowStart, AtomicInteger requestCount) {
            this.windowStart = windowStart;
            this.requestCount = requestCount;
        }
    }
}

// Custom rate limit error
public class RateLimitError extends ErrorResponse {
    public RateLimitError(String message, int retryAfter) {
        super(429, message);
        // Could add retryAfter field if needed
    }
}

// Usage
app.use(new RateLimitMiddleware(100, 60000)); // 100 requests per minute
```

### User-Based Rate Limiting

```java
public class UserRateLimitMiddleware implements Middleware {
    private final Map<String, RateLimitInfo> userRequests = new ConcurrentHashMap<>();
    private final int maxRequests;
    private final long windowMs;

    public UserRateLimitMiddleware(int maxRequests, long windowMs) {
        this.maxRequests = maxRequests;
        this.windowMs = windowMs;
    }

    @Override
    public void handle(Request req, Response res, MiddlewareChain next) {
        User user = (User) req.getAttribute("user");
        String userId = user != null ? String.valueOf(user.getId()) : getClientIp(req);

        // Different limits for authenticated vs anonymous users
        int effectiveMaxRequests = user != null ? maxRequests : maxRequests / 2;

        // Apply rate limiting logic (similar to IP-based)
        // ... implementation details

        next.next();
    }
}
```

## Security Headers

### Security Headers Middleware

```java
public class SecurityHeadersMiddleware implements Middleware {
    @Override
    public void handle(Request req, Response res, MiddlewareChain next) {
        // Prevent clickjacking
        res.setHeader("X-Frame-Options", "DENY");

        // Prevent MIME sniffing
        res.setHeader("X-Content-Type-Options", "nosniff");

        // Enable XSS filtering
        res.setHeader("X-XSS-Protection", "1; mode=block");

        // Enforce HTTPS
        res.setHeader("Strict-Transport-Security",
            "max-age=31536000; includeSubDomains; preload");

        // Content Security Policy
        res.setHeader("Content-Security-Policy",
            "default-src 'self'; script-src 'self' 'unsafe-inline'; " +
            "style-src 'self' 'unsafe-inline'; img-src 'self' data: https:;");

        // Referrer Policy
        res.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

        // Permissions Policy
        res.setHeader("Permissions-Policy",
            "camera=(), microphone=(), geolocation=()");

        next.next();
    }
}

// Usage
app.use(new SecurityHeadersMiddleware());
```

## JWT Implementation

### JWT Utility Class

```java
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class JWTUtil {
    private static final String SECRET_KEY = System.getenv("JWT_SECRET");
    private static final String ALGORITHM = "HmacSHA256";
    private static final long EXPIRATION_TIME = 86400000; // 24 hours

    public static String generateToken(User user) {
        long now = System.currentTimeMillis();
        long expirationTime = now + EXPIRATION_TIME;

        // Header
        String header = Base64.getUrlEncoder().withoutPadding()
            .encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes());

        // Payload
        String payload = String.format(
            "{\"sub\":\"%d\",\"email\":\"%s\",\"name\":\"%s\",\"roles\":%s,\"iat\":%d,\"exp\":%d}",
            user.getId(), user.getEmail(), user.getName(),
            rolesToJson(user.getRoles()), now / 1000, expirationTime / 1000
        );
        String encodedPayload = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(payload.getBytes());

        // Signature
        String data = header + "." + encodedPayload;
        String signature = createSignature(data);

        return data + "." + signature;
    }

    public static User validateToken(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new UnauthorizedError("Invalid token format");
        }

        String header = parts[0];
        String payload = parts[1];
        String signature = parts[2];

        // Verify signature
        String data = header + "." + payload;
        String expectedSignature = createSignature(data);
        if (!signature.equals(expectedSignature)) {
            throw new UnauthorizedError("Invalid token signature");
        }

        // Decode payload
        String decodedPayload = new String(Base64.getUrlDecoder().decode(payload));
        return parseUserFromPayload(decodedPayload);
    }

    private static String createSignature(String data) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            SecretKeySpec secretKeySpec = new SecretKeySpec(SECRET_KEY.getBytes(), ALGORITHM);
            mac.init(secretKeySpec);
            byte[] signatureBytes = mac.doFinal(data.getBytes());
            return Base64.getUrlEncoder().withoutPadding().encodeToString(signatureBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new InternalServerError("Error creating token signature");
        }
    }

    private static String rolesToJson(Set<Role> roles) {
        return "[" + roles.stream()
            .map(role -> "\"" + role.getValue() + "\"")
            .reduce((a, b) -> a + "," + b)
            .orElse("") + "]";
    }

    private static User parseUserFromPayload(String payload) {
        // Simple JSON parsing (in real app, use proper JSON library)
        // This is simplified for example purposes
        try {
            // Extract values using regex or proper JSON parsing
            String subPattern = "\"sub\":\"(\\d+)\"";
            String emailPattern = "\"email\":\"([^\"]+)\"";
            String namePattern = "\"name\":\"([^\"]+)\"";
            String expPattern = "\"exp\":(\\d+)";

            String sub = extractValue(payload, subPattern);
            String email = extractValue(payload, emailPattern);
            String name = extractValue(payload, namePattern);
            String exp = extractValue(payload, expPattern);

            // Check expiration
            long expirationTime = Long.parseLong(exp) * 1000;
            if (System.currentTimeMillis() > expirationTime) {
                throw new UnauthorizedError("Token has expired");
            }

            User user = new User();
            user.setId(Integer.parseInt(sub));
            user.setEmail(email);
            user.setName(name);
            // Set roles based on payload...

            return user;
        } catch (Exception e) {
            throw new UnauthorizedError("Invalid token payload");
        }
    }

    private static String extractValue(String text, String pattern) {
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(text);
        if (m.find()) {
            return m.group(1);
        }
        throw new UnauthorizedError("Invalid token format");
    }
}
```

### JWT Authentication Middleware

```java
public class JWTAuthMiddleware implements Middleware {
    @Override
    public void handle(Request req, Response res, MiddlewareChain next) {
        String authHeader = req.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new UnauthorizedError("JWT token required");
        }

        String token = authHeader.substring(7);

        try {
            User user = JWTUtil.validateToken(token);
            req.setAttribute("user", user);
            next.next();
        } catch (Exception e) {
            throw new UnauthorizedError("Invalid or expired token");
        }
    }
}

// JWT Login endpoint
app.post("/auth/jwt/login", (req, res, next) -> {
    String email = req.getBody("email");
    String password = req.getBody("password");

    Optional<User> userOpt = userService.authenticate(email, password);
    if (userOpt.isEmpty()) {
        throw new UnauthorizedError("Invalid credentials");
    }

    User user = userOpt.get();
    String token = JWTUtil.generateToken(user);

    res.json(Map.of(
        "success", true,
        "token", token,
        "user", Map.of(
            "id", user.getId(),
            "email", user.getEmail(),
            "name", user.getName()
        )
    ));
});
```

## Best Practices

### Password Security

```java
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class PasswordUtil {
    private static final String ALGORITHM = "SHA-256";
    private static final int SALT_LENGTH = 32;

    public static String hashPassword(String password) {
        byte[] salt = generateSalt();
        byte[] hash = hashWithSalt(password, salt);

        // Combine salt and hash
        byte[] combined = new byte[salt.length + hash.length];
        System.arraycopy(salt, 0, combined, 0, salt.length);
        System.arraycopy(hash, 0, combined, salt.length, hash.length);

        return Base64.getEncoder().encodeToString(combined);
    }

    public static boolean verifyPassword(String password, String hashedPassword) {
        try {
            byte[] combined = Base64.getDecoder().decode(hashedPassword);

            // Extract salt and hash
            byte[] salt = new byte[SALT_LENGTH];
            byte[] hash = new byte[combined.length - SALT_LENGTH];
            System.arraycopy(combined, 0, salt, 0, SALT_LENGTH);
            System.arraycopy(combined, SALT_LENGTH, hash, 0, hash.length);

            // Hash input password with extracted salt
            byte[] inputHash = hashWithSalt(password, salt);

            // Compare hashes
            return MessageDigest.isEqual(hash, inputHash);
        } catch (Exception e) {
            return false;
        }
    }

    private static byte[] generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH];
        random.nextBytes(salt);
        return salt;
    }

    private static byte[] hashWithSalt(String password, byte[] salt) {
        try {
            MessageDigest md = MessageDigest.getInstance(ALGORITHM);
            md.update(salt);
            return md.digest(password.getBytes());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Hashing algorithm not available", e);
        }
    }
}
```

### Secure Configuration

```java
// SecurityConfig.java
public class SecurityConfig {
    public static void validateConfiguration() {
        String jwtSecret = System.getenv("JWT_SECRET");
        if (jwtSecret == null || jwtSecret.length() < 32) {
            throw new IllegalStateException("JWT_SECRET must be at least 32 characters long");
        }

        String appEnv = System.getenv("APP_ENV");
        if ("production".equals(appEnv)) {
            validateProductionConfig();
        }
    }

    private static void validateProductionConfig() {
        // Ensure HTTPS in production
        String forceHttps = System.getenv("FORCE_HTTPS");
        if (!"true".equals(forceHttps)) {
            System.err.println("WARNING: FORCE_HTTPS not enabled in production");
        }

        // Check for debug mode
        String debug = System.getenv("DEBUG");
        if ("true".equals(debug)) {
            System.err.println("WARNING: DEBUG mode enabled in production");
        }
    }

    public static Heo configureSecureApp() {
        validateConfiguration();

        Heo app = new Heo();

        // Security middleware
        app.use(new SecurityHeadersMiddleware());
        app.use(new RateLimitMiddleware(100, 60000));
        app.use(new SanitizationMiddleware());
        app.use(new SQLInjectionPreventionMiddleware());

        return app;
    }
}
```

### Audit Logging

```java
public class AuditLogger {
    private static final java.util.logging.Logger logger =
        java.util.logging.Logger.getLogger(AuditLogger.class.getName());

    public static void logSecurityEvent(String event, User user, Request req) {
        Map<String, Object> auditData = new HashMap<>();
        auditData.put("event", event);
        auditData.put("timestamp", Instant.now().toString());
        auditData.put("clientIp", getClientIp(req));
        auditData.put("userAgent", req.getHeader("User-Agent"));
        auditData.put("method", req.getMethod());
        auditData.put("path", req.getPath());

        if (user != null) {
            auditData.put("userId", user.getId());
            auditData.put("userEmail", user.getEmail());
        }

        logger.info("SECURITY_AUDIT: " + formatAuditData(auditData));
    }

    private static String getClientIp(Request req) {
        String xForwardedFor = req.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return req.getHeader("X-Real-IP");
    }

    private static String formatAuditData(Map<String, Object> data) {
        // Format as JSON for structured logging
        return data.entrySet().stream()
            .map(entry -> "\"" + entry.getKey() + "\":\"" + entry.getValue() + "\"")
            .collect(Collectors.joining(",", "{", "}"));
    }
}

// Usage in authentication
public class AuditAuthMiddleware implements Middleware {
    @Override
    public void handle(Request req, Response res, MiddlewareChain next) {
        try {
            next.next();
            User user = (User) req.getAttribute("user");
            if (user != null) {
                AuditLogger.logSecurityEvent("AUTH_SUCCESS", user, req);
            }
        } catch (UnauthorizedError e) {
            AuditLogger.logSecurityEvent("AUTH_FAILURE", null, req);
            throw e;
        }
    }
}
```

## Next Steps

- [Error Handling](error-handling.md) - Learn about comprehensive error handling
- [Project Structure](structure.md) - Organize your secure application
- [Middleware](middleware.md) - Build custom security middleware
