# Middleware

- [Introduction](#introduction)
- [Built-in Middleware](#built-in-middleware)
- [Using Middleware](#using-middleware)
- [Creating Custom Middleware](#creating-custom-middleware)
- [Middleware Order](#middleware-order)
- [Error Handling Middleware](#error-handling-middleware)

## Introduction

Middleware functions are functions that have access to the request object (`req`), the response object (`res`), and the `next` middleware function in the application's request-response cycle. Middleware can:

- Execute code
- Make changes to the request and response objects
- End the request-response cycle
- Call the next middleware in the stack

If the current middleware function does not end the request-response cycle, it must call `next.next(req, res)` to pass control to the next middleware function. Otherwise, the request will be left hanging.

## Built-in Middleware

Heo comes with several built-in middleware functions:

### JSON Middleware

Parses incoming JSON requests and populates `req.getBody()`:

```java
import heo.middleware.Json;

app.use(new Json());

app.post("/api/data", (req, res, next) -> {
    Map<String, Object> jsonData = req.getBody();
    System.out.println("Received: " + jsonData);
    res.json(Map.of("received", jsonData));
});
```

### URL-encoded Middleware

Parses URL-encoded request bodies:

```java
import heo.middleware.Urlencoded;

app.use(new Urlencoded());

app.post("/form", (req, res, next) -> {
    Map<String, Object> formData = req.getBody();
    res.send("Form received: " + formData);
});
```

### Morgan (Logging) Middleware

Logs HTTP requests:

```java
import heo.middleware.Morgan;

// Development logging (colorized)
app.use(new Morgan("dev"));

// Production logging
app.use(new Morgan("combined"));

// Other formats
app.use(new Morgan("tiny"));
app.use(new Morgan("short"));
```

### CORS Middleware

Enables Cross-Origin Resource Sharing:

```java
import heo.middleware.Cors;
import java.util.List;

app.use(new Cors(
    List.of("*"),                           // Allowed origins
    List.of("GET", "POST", "PUT", "DELETE"), // Allowed methods
    List.of("Content-Type", "Authorization"), // Allowed headers
    true                                     // Allow credentials
));
```

## Using Middleware

### Global Middleware

Apply middleware to all routes:

```java
// Apply to all routes
app.use(new Morgan("dev"));
app.use(new Json());
app.use(new Cors(List.of("*"), List.of("*"), List.of("*"), true));

// All routes will now have logging, JSON parsing, and CORS enabled
app.get("/api/users", (req, res, next) -> {
    res.json(Map.of("users", getAllUsers()));
});
```

### Path-specific Middleware

Apply middleware only to specific paths:

```java
// Only apply to routes starting with /api
app.use("/api", new Json());

// Only apply to admin routes
app.use("/admin", authenticationMiddleware);
```

### Route-specific Middleware

Apply middleware to individual routes:

```java
// Authentication middleware for specific route
app.get("/profile", authMiddleware, (req, res, next) -> {
    res.json(getCurrentUser(req));
});

// Multiple middleware for a route
app.post("/api/users",
    authMiddleware,
    validationMiddleware,
    (req, res, next) -> {
        Map<String, Object> userData = req.getBody();
        User newUser = createUser(userData);
        res.status(201).json(newUser);
    }
);
```

## Creating Custom Middleware

### Basic Custom Middleware

```java
import heo.interfaces.Middleware;
import heo.middleware.MiddlewareChain;

// Simple logging middleware
Middleware customLogger = (req, res, next) -> {
    long startTime = System.currentTimeMillis();
    System.out.println("Request started: " + req.getMethod() + " " + req.path);

    // Continue to next middleware
    next.next(req, res);

    long endTime = System.currentTimeMillis();
    System.out.println("Request completed in " + (endTime - startTime) + "ms");
};

app.use(customLogger);
```

### Authentication Middleware

```java
Middleware authenticationMiddleware = (req, res, next) -> {
    String token = req.getHeader("Authorization");

    if (token == null) {
        res.status(401).json(Map.of("error", "No token provided"));
        return; // Don't call next, end the request here
    }

    if (!token.startsWith("Bearer ")) {
        res.status(401).json(Map.of("error", "Invalid token format"));
        return;
    }

    String jwtToken = token.substring(7); // Remove "Bearer "

    try {
        // Validate token (implement your own validation)
        User user = validateToken(jwtToken);

        // Add user to request context (you'd need to extend Request class)
        // req.setUser(user);

        // Continue to next middleware
        next.next(req, res);

    } catch (Exception e) {
        res.status(401).json(Map.of("error", "Invalid token"));
    }
};
```

### Validation Middleware

```java
Middleware validateUserData = (req, res, next) -> {
    Map<String, Object> body = req.getBody();
    List<String> errors = new ArrayList<>();

    // Validate required fields
    if (!body.containsKey("name") || body.get("name").toString().trim().isEmpty()) {
        errors.add("Name is required");
    }

    if (!body.containsKey("email") || !isValidEmail(body.get("email").toString())) {
        errors.add("Valid email is required");
    }

    if (!errors.isEmpty()) {
        res.status(400).json(Map.of("errors", errors));
        return;
    }

    // Validation passed, continue
    next.next(req, res);
};

// Helper method
private boolean isValidEmail(String email) {
    return email != null && email.contains("@") && email.contains(".");
}
```

### Rate Limiting Middleware

```java
public class RateLimitMiddleware implements Middleware {
    private final Map<String, List<Long>> requests = new ConcurrentHashMap<>();
    private final int maxRequests;
    private final long windowMs;

    public RateLimitMiddleware(int maxRequests, long windowMs) {
        this.maxRequests = maxRequests;
        this.windowMs = windowMs;
    }

    @Override
    public void handle(Request req, Response res, MiddlewareChain next) throws Exception {
        String clientIP = req.getIpAddress();
        long now = System.currentTimeMillis();

        // Clean old requests
        requests.computeIfAbsent(clientIP, k -> new ArrayList<>())
               .removeIf(timestamp -> now - timestamp > windowMs);

        List<Long> clientRequests = requests.get(clientIP);

        if (clientRequests.size() >= maxRequests) {
            res.status(429).json(Map.of(
                "error", "Too many requests",
                "retryAfter", windowMs / 1000
            ));
            return;
        }

        // Add current request
        clientRequests.add(now);

        next.next(req, res);
    }
}

// Usage
app.use(new RateLimitMiddleware(100, 60000)); // 100 requests per minute
```

### Request ID Middleware

```java
Middleware requestIdMiddleware = (req, res, next) -> {
    String requestId = UUID.randomUUID().toString();

    // Add to response headers
    res.setHeader("X-Request-ID", requestId);

    // You could also add to request for logging
    System.out.println("Request ID: " + requestId + " - " + req.path);

    next.next(req, res);
};
```

## Middleware Order

The order in which you define middleware is crucial. Middleware executes in the order it's defined:

```java
// 1. First - Request logging
app.use(new Morgan("dev"));

// 2. Second - CORS (should be early)
app.use(new Cors(List.of("*"), List.of("*"), List.of("*"), true));

// 3. Third - Body parsing
app.use(new Json());
app.use(new Urlencoded());

// 4. Fourth - Authentication (after body parsing)
app.use("/api", authenticationMiddleware);

// 5. Fifth - Rate limiting
app.use(new RateLimitMiddleware(100, 60000));

// 6. Routes come after middleware
app.get("/api/users", (req, res, next) -> {
    res.json(getAllUsers());
});

// 7. Last - Error handling (should be last)
app.use(errorHandlingMiddleware);
```

## Error Handling Middleware

### Global Error Handler

```java
import heo.interfaces.ErrorHandler;

ErrorHandler globalErrorHandler = (error, req, res) -> {
    System.err.println("Error occurred: " + error.getMessage());
    error.printStackTrace();

    // Determine status code
    int statusCode = 500;
    if (error instanceof BadRequest) {
        statusCode = 400;
    } else if (error instanceof NotFoundError) {
        statusCode = 404;
    } else if (error instanceof UnauthorizedError) {
        statusCode = 401;
    }

    // Send error response
    res.status(statusCode).json(Map.of(
        "error", error.getMessage(),
        "timestamp", System.currentTimeMillis(),
        "path", req.path
    ));
};

app.use(globalErrorHandler);
```

### Error Recovery Middleware

```java
Middleware errorRecoveryMiddleware = (req, res, next) -> {
    try {
        next.next(req, res);
    } catch (Exception e) {
        System.err.println("Recovered from error: " + e.getMessage());

        // Log error details
        logError(e, req);

        // Send generic error response
        res.status(500).json(Map.of(
            "error", "An unexpected error occurred",
            "requestId", UUID.randomUUID().toString()
        ));
    }
};
```

## Middleware Examples

### API Key Validation

```java
Middleware apiKeyMiddleware = (req, res, next) -> {
    String apiKey = req.getHeader("X-API-Key");

    if (apiKey == null) {
        res.status(401).json(Map.of("error", "API key required"));
        return;
    }

    if (!isValidApiKey(apiKey)) {
        res.status(403).json(Map.of("error", "Invalid API key"));
        return;
    }

    next.next(req, res);
};

// Apply to all API routes
app.use("/api", apiKeyMiddleware);
```

### Content Type Validation

```java
Middleware jsonOnlyMiddleware = (req, res, next) -> {
    if ("POST".equals(req.getMethod()) || "PUT".equals(req.getMethod())) {
        String contentType = req.getHeader("Content-Type");

        if (contentType == null || !contentType.contains("application/json")) {
            res.status(415).json(Map.of("error", "Content-Type must be application/json"));
            return;
        }
    }

    next.next(req, res);
};
```

### Security Headers Middleware

```java
Middleware securityHeadersMiddleware = (req, res, next) -> {
    // Add security headers
    res.setHeader("X-Content-Type-Options", "nosniff");
    res.setHeader("X-Frame-Options", "DENY");
    res.setHeader("X-XSS-Protection", "1; mode=block");
    res.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");

    next.next(req, res);
};
```

## Best Practices

1. **Order matters** - Place middleware in logical order
2. **Fail fast** - Validate early and return errors immediately
3. **Keep it focused** - Each middleware should have a single responsibility
4. **Handle errors** - Always handle potential exceptions
5. **Document behavior** - Make middleware behavior clear and predictable
6. **Test thoroughly** - Test middleware in isolation and integration

## Next Steps

- [Request](request.md) - Learn about working with HTTP requests
- [Response](response.md) - Learn about sending HTTP responses
- [Error Handling](errors.md) - Advanced error handling techniques
