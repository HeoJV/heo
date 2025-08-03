# Error Handling

- [Error Response Classes](#error-response-classes)
- [Built-in Error Types](#built-in-error-types)
- [Custom Error Handling](#custom-error-handling)
- [Error Middleware](#error-middleware)
- [Best Practices](#best-practices)

## Error Response Classes

Heo provides built-in error response classes for common HTTP errors. All error responses extend the `ErrorResponse` base class.

### Base Error Response

```java
import heo.exception.ErrorResponse;

// All error responses inherit from this base class
public abstract class ErrorResponse {
    protected int status;
    protected String message;
    protected long timestamp;

    public ErrorResponse(int status, String message) {
        this.status = status;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters and setters...
}
```

## Built-in Error Types

### 400 Bad Request

```java
import heo.exception.BadRequest;

// Usage in route handlers
app.post("/users", (req, res, next) -> {
    String name = req.getBody("name");
    if (name == null || name.trim().isEmpty()) {
        throw new BadRequest("Name is required");
    }

    // Continue with user creation...
});
```

### 401 Unauthorized

```java
import heo.exception.UnauthorizedError;

// Usage in authentication middleware
public class AuthMiddleware implements Middleware {
    @Override
    public void handle(Request req, Response res, MiddlewareChain next) {
        String token = req.getHeader("Authorization");

        if (token == null || !isValidToken(token)) {
            throw new UnauthorizedError("Invalid or missing authentication token");
        }

        next.next();
    }

    private boolean isValidToken(String token) {
        // Token validation logic
        return token.startsWith("Bearer ") && validateJWT(token.substring(7));
    }
}
```

### 403 Forbidden

```java
import heo.exception.ForbiddenError;

// Usage in authorization middleware
public class AdminMiddleware implements Middleware {
    @Override
    public void handle(Request req, Response res, MiddlewareChain next) {
        User user = (User) req.getAttribute("user");

        if (user == null || !user.isAdmin()) {
            throw new ForbiddenError("Admin access required");
        }

        next.next();
    }
}
```

### 404 Not Found

```java
import heo.exception.NotFoundError;

// Usage in route handlers
app.get("/users/:id", (req, res, next) -> {
    int userId = Integer.parseInt(req.getParam("id"));
    User user = userService.findById(userId);

    if (user == null) {
        throw new NotFoundError("User not found with ID: " + userId);
    }

    res.json(user);
});
```

### 500 Internal Server Error

```java
import heo.exception.InternalServerError;

// Usage for unexpected errors
app.get("/users", (req, res, next) -> {
    try {
        List<User> users = userService.getAllUsers();
        res.json(users);
    } catch (DatabaseException e) {
        throw new InternalServerError("Database connection failed: " + e.getMessage());
    }
});
```

## Custom Error Handling

### Creating Custom Error Types

```java
// Custom validation error
public class ValidationError extends ErrorResponse {
    private Map<String, String> fieldErrors;

    public ValidationError(String message) {
        super(422, message);
        this.fieldErrors = new HashMap<>();
    }

    public ValidationError(String message, Map<String, String> fieldErrors) {
        super(422, message);
        this.fieldErrors = fieldErrors;
    }

    public void addFieldError(String field, String error) {
        this.fieldErrors.put(field, error);
    }

    // Getters and setters...
}

// Custom rate limit error
public class RateLimitError extends ErrorResponse {
    private int retryAfter;

    public RateLimitError(String message, int retryAfter) {
        super(429, message);
        this.retryAfter = retryAfter;
    }

    // Getters and setters...
}

// Custom business logic error
public class InsufficientFundsError extends ErrorResponse {
    private double available;
    private double requested;

    public InsufficientFundsError(double available, double requested) {
        super(402, "Insufficient funds");
        this.available = available;
        this.requested = requested;
    }

    // Getters and setters...
}
```

### Using Custom Errors

```java
// In a service class
public class UserService {
    public User createUser(CreateUserRequest request) {
        ValidationError validationError = new ValidationError("Validation failed");

        if (request.getName() == null || request.getName().trim().isEmpty()) {
            validationError.addFieldError("name", "Name is required");
        }

        if (request.getEmail() == null || !isValidEmail(request.getEmail())) {
            validationError.addFieldError("email", "Valid email is required");
        }

        if (request.getAge() != null && request.getAge() < 18) {
            validationError.addFieldError("age", "Must be at least 18 years old");
        }

        if (validationError.getFieldErrors().size() > 0) {
            throw validationError;
        }

        // Check if email already exists
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new ConflictError("Email already exists");
        }

        return userRepository.save(new User(request));
    }
}
```

## Error Middleware

### Global Error Handler

```java
import heo.interfaces.ErrorHandler;
import heo.exception.ErrorResponse;

public class GlobalErrorHandler implements ErrorHandler {
    @Override
    public void handleError(Exception error, Request req, Response res) {
        // Log the error
        logError(error, req);

        if (error instanceof ErrorResponse) {
            ErrorResponse errorResponse = (ErrorResponse) error;
            res.status(errorResponse.getStatus()).json(errorResponse);
        } else if (error instanceof IllegalArgumentException) {
            res.status(400).json(new BadRequest(error.getMessage()));
        } else if (error instanceof RuntimeException) {
            // Hide internal errors in production
            String message = isProduction() ? "Internal server error" : error.getMessage();
            res.status(500).json(new InternalServerError(message));
        } else {
            res.status(500).json(new InternalServerError("An unexpected error occurred"));
        }
    }

    private void logError(Exception error, Request req) {
        String method = req.getMethod();
        String path = req.getPath();
        String userAgent = req.getHeader("User-Agent");
        String clientIp = getClientIp(req);

        System.err.printf("[ERROR] %s %s - %s - IP: %s - User-Agent: %s%n",
            method, path, error.getMessage(), clientIp, userAgent);

        if (!(error instanceof ErrorResponse)) {
            error.printStackTrace();
        }
    }

    private String getClientIp(Request req) {
        String xForwardedFor = req.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return req.getHeader("X-Real-IP");
    }

    private boolean isProduction() {
        return "production".equals(System.getenv("APP_ENV"));
    }
}

// Register the global error handler
app.setErrorHandler(new GlobalErrorHandler());
```

### Async Error Handling

```java
// Error handling middleware for async operations
public class AsyncErrorMiddleware implements Middleware {
    @Override
    public void handle(Request req, Response res, MiddlewareChain next) {
        try {
            next.next();
        } catch (Exception e) {
            // Handle async errors
            if (e instanceof CompletionException) {
                Throwable cause = e.getCause();
                if (cause instanceof ErrorResponse) {
                    throw (ErrorResponse) cause;
                } else {
                    throw new InternalServerError("Async operation failed: " + cause.getMessage());
                }
            } else {
                throw e;
            }
        }
    }
}
```

### Validation Middleware

```java
// Middleware for request validation
public class ValidationMiddleware implements Middleware {
    private final Map<String, Validator> validators = new HashMap<>();

    public ValidationMiddleware() {
        // Register validators
        validators.put("email", this::validateEmail);
        validators.put("required", this::validateRequired);
        validators.put("minLength", this::validateMinLength);
        validators.put("maxLength", this::validateMaxLength);
        validators.put("numeric", this::validateNumeric);
    }

    @Override
    public void handle(Request req, Response res, MiddlewareChain next) {
        // Get validation rules from request attributes
        @SuppressWarnings("unchecked")
        Map<String, List<String>> rules = (Map<String, List<String>>) req.getAttribute("validation");

        if (rules != null) {
            ValidationError validationError = validateRequest(req, rules);
            if (validationError.getFieldErrors().size() > 0) {
                throw validationError;
            }
        }

        next.next();
    }

    private ValidationError validateRequest(Request req, Map<String, List<String>> rules) {
        ValidationError error = new ValidationError("Validation failed");

        for (Map.Entry<String, List<String>> entry : rules.entrySet()) {
            String field = entry.getKey();
            List<String> fieldRules = entry.getValue();
            String value = req.getBody(field);

            for (String rule : fieldRules) {
                String[] parts = rule.split(":");
                String ruleName = parts[0];
                String ruleValue = parts.length > 1 ? parts[1] : null;

                Validator validator = validators.get(ruleName);
                if (validator != null) {
                    String fieldError = validator.validate(value, ruleValue);
                    if (fieldError != null) {
                        error.addFieldError(field, fieldError);
                        break; // Stop at first error for this field
                    }
                }
            }
        }

        return error;
    }

    // Validator implementations
    private String validateRequired(String value, String param) {
        return (value == null || value.trim().isEmpty()) ? "This field is required" : null;
    }

    private String validateEmail(String value, String param) {
        if (value == null) return null;
        return value.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
            ? null : "Invalid email format";
    }

    private String validateMinLength(String value, String param) {
        if (value == null) return null;
        int minLength = Integer.parseInt(param);
        return value.length() >= minLength
            ? null : "Must be at least " + minLength + " characters";
    }

    private String validateMaxLength(String value, String param) {
        if (value == null) return null;
        int maxLength = Integer.parseInt(param);
        return value.length() <= maxLength
            ? null : "Must be no more than " + maxLength + " characters";
    }

    private String validateNumeric(String value, String param) {
        if (value == null) return null;
        try {
            Double.parseDouble(value);
            return null;
        } catch (NumberFormatException e) {
            return "Must be a valid number";
        }
    }

    @FunctionalInterface
    private interface Validator {
        String validate(String value, String param);
    }
}
```

## Best Practices

### Error Response Format

Use a consistent error response format across your application:

```java
// Standard error response format
public class StandardErrorResponse {
    private boolean success = false;
    private int status;
    private String error;
    private String message;
    private Map<String, Object> details;
    private long timestamp;
    private String path;

    // Constructors, getters, and setters...
}

// Example usage in error handler
public class StandardErrorHandler implements ErrorHandler {
    @Override
    public void handleError(Exception error, Request req, Response res) {
        StandardErrorResponse errorResponse = new StandardErrorResponse();
        errorResponse.setTimestamp(System.currentTimeMillis());
        errorResponse.setPath(req.getPath());

        if (error instanceof ErrorResponse) {
            ErrorResponse err = (ErrorResponse) error;
            errorResponse.setStatus(err.getStatus());
            errorResponse.setMessage(err.getMessage());
            errorResponse.setError(getErrorType(err.getStatus()));

            // Add additional details for validation errors
            if (err instanceof ValidationError) {
                ValidationError validationErr = (ValidationError) err;
                errorResponse.setDetails(Map.of("fieldErrors", validationErr.getFieldErrors()));
            }
        } else {
            errorResponse.setStatus(500);
            errorResponse.setError("INTERNAL_SERVER_ERROR");
            errorResponse.setMessage("An unexpected error occurred");
        }

        res.status(errorResponse.getStatus()).json(errorResponse);
    }

    private String getErrorType(int status) {
        return switch (status) {
            case 400 -> "BAD_REQUEST";
            case 401 -> "UNAUTHORIZED";
            case 403 -> "FORBIDDEN";
            case 404 -> "NOT_FOUND";
            case 422 -> "VALIDATION_ERROR";
            case 429 -> "RATE_LIMIT_EXCEEDED";
            case 500 -> "INTERNAL_SERVER_ERROR";
            default -> "UNKNOWN_ERROR";
        };
    }
}
```

### Error Logging

Implement comprehensive error logging:

```java
public class ErrorLogger {
    private static final java.util.logging.Logger logger =
        java.util.logging.Logger.getLogger(ErrorLogger.class.getName());

    public static void logError(Exception error, Request req) {
        String correlationId = generateCorrelationId();
        req.setAttribute("correlationId", correlationId);

        Map<String, Object> logData = new HashMap<>();
        logData.put("correlationId", correlationId);
        logData.put("method", req.getMethod());
        logData.put("path", req.getPath());
        logData.put("query", req.getQueryParams());
        logData.put("userAgent", req.getHeader("User-Agent"));
        logData.put("clientIp", getClientIp(req));
        logData.put("errorType", error.getClass().getSimpleName());
        logData.put("errorMessage", error.getMessage());
        logData.put("timestamp", Instant.now().toString());

        // Add user context if available
        Object user = req.getAttribute("user");
        if (user != null) {
            logData.put("userId", ((User) user).getId());
        }

        // Log based on error severity
        if (error instanceof ErrorResponse) {
            ErrorResponse errorResponse = (ErrorResponse) error;
            if (errorResponse.getStatus() >= 500) {
                logger.severe("Server error: " + formatLogData(logData));
            } else if (errorResponse.getStatus() >= 400) {
                logger.warning("Client error: " + formatLogData(logData));
            } else {
                logger.info("Error: " + formatLogData(logData));
            }
        } else {
            logger.severe("Unexpected error: " + formatLogData(logData));
        }
    }

    private static String generateCorrelationId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private static String getClientIp(Request req) {
        String xForwardedFor = req.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return req.getHeader("X-Real-IP");
    }

    private static String formatLogData(Map<String, Object> logData) {
        // Convert to JSON string for structured logging
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        logData.forEach((key, value) ->
            sb.append("\"").append(key).append("\":\"").append(value).append("\","));
        if (sb.length() > 1) {
            sb.setLength(sb.length() - 1); // Remove trailing comma
        }
        sb.append("}");
        return sb.toString();
    }
}
```

### Circuit Breaker Pattern

Implement circuit breaker for external service calls:

```java
public class CircuitBreaker {
    private final int failureThreshold;
    private final long recoveryTimeout;
    private int failureCount = 0;
    private long lastFailureTime = 0;
    private State state = State.CLOSED;

    public enum State {
        CLOSED, OPEN, HALF_OPEN
    }

    public CircuitBreaker(int failureThreshold, long recoveryTimeout) {
        this.failureThreshold = failureThreshold;
        this.recoveryTimeout = recoveryTimeout;
    }

    public <T> T execute(Supplier<T> operation) throws CircuitBreakerOpenException {
        if (state == State.OPEN) {
            if (System.currentTimeMillis() - lastFailureTime >= recoveryTimeout) {
                state = State.HALF_OPEN;
            } else {
                throw new CircuitBreakerOpenException("Circuit breaker is open");
            }
        }

        try {
            T result = operation.get();
            onSuccess();
            return result;
        } catch (Exception e) {
            onFailure();
            throw e;
        }
    }

    private void onSuccess() {
        failureCount = 0;
        state = State.CLOSED;
    }

    private void onFailure() {
        failureCount++;
        lastFailureTime = System.currentTimeMillis();

        if (failureCount >= failureThreshold) {
            state = State.OPEN;
        }
    }
}

// Custom exception for circuit breaker
public class CircuitBreakerOpenException extends ErrorResponse {
    public CircuitBreakerOpenException(String message) {
        super(503, message);
    }
}
```

### Health Check Endpoint

Implement health check with error monitoring:

```java
app.get("/health", (req, res, next) -> {
    Map<String, Object> health = new HashMap<>();
    health.put("status", "UP");
    health.put("timestamp", Instant.now().toString());
    health.put("service", "heo-app");
    health.put("version", "1.0.0");

    // Check dependencies
    Map<String, String> checks = new HashMap<>();

    // Database check
    try {
        if (isDatabaseHealthy()) {
            checks.put("database", "UP");
        } else {
            checks.put("database", "DOWN");
            health.put("status", "DEGRADED");
        }
    } catch (Exception e) {
        checks.put("database", "DOWN");
        health.put("status", "DOWN");
    }

    // External service check
    try {
        if (isExternalServiceHealthy()) {
            checks.put("external-service", "UP");
        } else {
            checks.put("external-service", "DOWN");
            health.put("status", "DEGRADED");
        }
    } catch (Exception e) {
        checks.put("external-service", "DOWN");
        health.put("status", "DOWN");
    }

    health.put("checks", checks);

    // Set appropriate status code
    int statusCode = "UP".equals(health.get("status")) ? 200 : 503;
    res.status(statusCode).json(health);
});
```

## Next Steps

- [Middleware](middleware.md) - Learn about middleware development
- [Request Handling](request.md) - Understand request processing
- [Project Structure](structure.md) - Organize your application properly
