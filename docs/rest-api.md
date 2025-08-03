# Building a REST API

- [Introduction](#introduction)
- [API Design Principles](#api-design-principles)
- [Complete User Management API](#complete-user-management-api)
- [Authentication & Authorization](#authentication--authorization)
- [Data Validation](#data-validation)
- [Error Handling](#error-handling)
- [API Testing](#api-testing)

## Introduction

This guide will walk you through building a complete REST API using Heo. We'll create a user management system with authentication, validation, and proper error handling.

## API Design Principles

Before we start coding, let's establish some REST API design principles:

### HTTP Methods

- `GET` - Retrieve data
- `POST` - Create new resources
- `PUT` - Update entire resources
- `PATCH` - Partial updates
- `DELETE` - Remove resources

### Status Codes

- `200` - OK (successful GET, PUT, PATCH)
- `201` - Created (successful POST)
- `204` - No Content (successful DELETE)
- `400` - Bad Request (validation errors)
- `401` - Unauthorized (authentication required)
- `403` - Forbidden (insufficient permissions)
- `404` - Not Found
- `500` - Internal Server Error

### URL Structure

```
GET    /api/v1/users           # List users
POST   /api/v1/users           # Create user
GET    /api/v1/users/:id       # Get specific user
PUT    /api/v1/users/:id       # Update user (full)
PATCH  /api/v1/users/:id       # Update user (partial)
DELETE /api/v1/users/:id       # Delete user
```

## Complete User Management API

Let's build a comprehensive user management API with all CRUD operations.

### Main Application Structure

```java
import heo.server.Heo;
import heo.router.Router;
import heo.middleware.*;
import heo.config.Dotenv;
import heo.interfaces.ErrorHandler;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class UserManagementAPI {
    // In-memory storage (use a database in production)
    private static final Map<Integer, User> users = new ConcurrentHashMap<>();
    private static final AtomicInteger idCounter = new AtomicInteger(1);

    public static void main(String[] args) {
        // Load environment configuration
        Dotenv.config();

        Heo app = new Heo();

        // Setup middleware
        setupMiddleware(app);

        // Setup routes
        setupRoutes(app);

        // Setup error handling
        setupErrorHandling(app);

        // Add sample data
        addSampleData();

        // Start server
        int port = getPort();
        app.listen(port, () -> {
            System.out.println("🚀 User Management API running on http://localhost:" + port);
            printApiEndpoints();
        });
    }

    private static void setupMiddleware(Heo app) {
        // Request logging
        app.use(new Morgan("dev"));

        // CORS
        app.use(new Cors(
            List.of("*"),
            List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"),
            List.of("Content-Type", "Authorization", "X-API-Key"),
            true
        ));

        // JSON parsing
        app.use(new Json());

        // Request ID middleware
        app.use((req, res, next) -> {
            String requestId = UUID.randomUUID().toString().substring(0, 8);
            res.setHeader("X-Request-ID", requestId);
            next.next(req, res);
        });
    }

    private static void setupRoutes(Heo app) {
        // Health check
        app.get("/health", (req, res, next) -> {
            res.json(Map.of(
                "status", "OK",
                "timestamp", System.currentTimeMillis(),
                "version", "1.0.0",
                "totalUsers", users.size()
            ));
        });

        // API info
        app.get("/", (req, res, next) -> {
            res.json(Map.of(
                "name", "User Management API",
                "version", "1.0.0",
                "documentation", "/api/docs",
                "endpoints", Map.of(
                    "users", "/api/v1/users",
                    "health", "/health"
                )
            ));
        });

        // Mount API routes
        Router apiV1 = createApiV1Router();
        app.use("/api/v1", apiV1);
    }

    private static Router createApiV1Router() {
        Router router = new Router();

        // List users with pagination and filtering
        router.get("/users", (req, res, next) -> {
            try {
                // Pagination parameters
                int page = parseInt(req.getQuery("page"), 1);
                int limit = parseInt(req.getQuery("limit"), 10);
                String search = req.getQuery("search");
                String role = req.getQuery("role");
                String sortBy = req.getQuery("sortBy");
                String order = req.getQuery("order");

                // Validate pagination
                if (page < 1) page = 1;
                if (limit < 1) limit = 10;
                if (limit > 100) limit = 100;

                // Filter users
                List<User> allUsers = new ArrayList<>(users.values());
                List<User> filteredUsers = filterUsers(allUsers, search, role);

                // Sort users
                sortUsers(filteredUsers, sortBy, order);

                // Paginate
                int start = (page - 1) * limit;
                int end = Math.min(start + limit, filteredUsers.size());
                List<User> paginatedUsers = filteredUsers.subList(start, end);

                // Calculate pagination info
                int totalPages = (int) Math.ceil((double) filteredUsers.size() / limit);

                res.json(Map.of(
                    "users", paginatedUsers,
                    "pagination", Map.of(
                        "page", page,
                        "limit", limit,
                        "total", filteredUsers.size(),
                        "totalPages", totalPages,
                        "hasNext", page < totalPages,
                        "hasPrevious", page > 1
                    )
                ));

            } catch (Exception e) {
                res.status(500).json(Map.of("error", "Failed to retrieve users"));
            }
        });

        // Get user by ID
        router.get("/users/:id", (req, res, next) -> {
            try {
                int userId = Integer.parseInt(req.params("id"));
                User user = users.get(userId);

                if (user == null) {
                    res.status(404).json(Map.of("error", "User not found"));
                    return;
                }

                res.json(user);

            } catch (NumberFormatException e) {
                res.status(400).json(Map.of("error", "Invalid user ID"));
            }
        });

        // Create user
        router.post("/users", (req, res, next) -> {
            try {
                Map<String, Object> body = req.getBody();

                // Validate input
                ValidationResult validation = validateUserData(body, false);
                if (!validation.isValid()) {
                    res.status(400).json(Map.of("errors", validation.getErrors()));
                    return;
                }

                // Check if email already exists
                String email = (String) body.get("email");
                if (emailExists(email)) {
                    res.status(409).json(Map.of("error", "Email already exists"));
                    return;
                }

                // Create user
                User newUser = createUser(body);
                users.put(newUser.getId(), newUser);

                res.status(201).json(newUser);

            } catch (Exception e) {
                res.status(500).json(Map.of("error", "Failed to create user"));
            }
        });

        // Update user (full update)
        router.put("/users/:id", (req, res, next) -> {
            try {
                int userId = Integer.parseInt(req.params("id"));
                User existingUser = users.get(userId);

                if (existingUser == null) {
                    res.status(404).json(Map.of("error", "User not found"));
                    return;
                }

                Map<String, Object> body = req.getBody();

                // Validate input
                ValidationResult validation = validateUserData(body, false);
                if (!validation.isValid()) {
                    res.status(400).json(Map.of("errors", validation.getErrors()));
                    return;
                }

                // Check email uniqueness (excluding current user)
                String email = (String) body.get("email");
                if (!existingUser.getEmail().equals(email) && emailExists(email)) {
                    res.status(409).json(Map.of("error", "Email already exists"));
                    return;
                }

                // Update user
                User updatedUser = updateUser(existingUser, body, false);
                users.put(userId, updatedUser);

                res.json(updatedUser);

            } catch (NumberFormatException e) {
                res.status(400).json(Map.of("error", "Invalid user ID"));
            } catch (Exception e) {
                res.status(500).json(Map.of("error", "Failed to update user"));
            }
        });

        // Partial update user
        router.patch("/users/:id", (req, res, next) -> {
            try {
                int userId = Integer.parseInt(req.params("id"));
                User existingUser = users.get(userId);

                if (existingUser == null) {
                    res.status(404).json(Map.of("error", "User not found"));
                    return;
                }

                Map<String, Object> body = req.getBody();

                if (body.isEmpty()) {
                    res.status(400).json(Map.of("error", "No data provided for update"));
                    return;
                }

                // Validate input (partial)
                ValidationResult validation = validateUserData(body, true);
                if (!validation.isValid()) {
                    res.status(400).json(Map.of("errors", validation.getErrors()));
                    return;
                }

                // Check email uniqueness if email is being updated
                if (body.containsKey("email")) {
                    String email = (String) body.get("email");
                    if (!existingUser.getEmail().equals(email) && emailExists(email)) {
                        res.status(409).json(Map.of("error", "Email already exists"));
                        return;
                    }
                }

                // Update user
                User updatedUser = updateUser(existingUser, body, true);
                users.put(userId, updatedUser);

                res.json(updatedUser);

            } catch (NumberFormatException e) {
                res.status(400).json(Map.of("error", "Invalid user ID"));
            } catch (Exception e) {
                res.status(500).json(Map.of("error", "Failed to update user"));
            }
        });

        // Delete user
        router.delete("/users/:id", (req, res, next) -> {
            try {
                int userId = Integer.parseInt(req.params("id"));
                User user = users.remove(userId);

                if (user == null) {
                    res.status(404).json(Map.of("error", "User not found"));
                    return;
                }

                res.status(204).send("");

            } catch (NumberFormatException e) {
                res.status(400).json(Map.of("error", "Invalid user ID"));
            }
        });

        return router;
    }

    private static void setupErrorHandling(Heo app) {
        ErrorHandler globalErrorHandler = (error, req, res) -> {
            String requestId = res.getHeaders().get("X-Request-ID");

            System.err.println("Error in request " + requestId + ": " + error.getMessage());
            error.printStackTrace();

            res.status(500).json(Map.of(
                "error", "Internal Server Error",
                "requestId", requestId,
                "timestamp", System.currentTimeMillis()
            ));
        };

        app.use(globalErrorHandler);
    }

    // Helper methods
    private static int parseInt(String value, int defaultValue) {
        if (value == null) return defaultValue;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static List<User> filterUsers(List<User> users, String search, String role) {
        return users.stream()
            .filter(user -> search == null ||
                user.getName().toLowerCase().contains(search.toLowerCase()) ||
                user.getEmail().toLowerCase().contains(search.toLowerCase()))
            .filter(user -> role == null || user.getRole().equalsIgnoreCase(role))
            .toList();
    }

    private static void sortUsers(List<User> users, String sortBy, String order) {
        if (sortBy == null) return;

        Comparator<User> comparator = switch (sortBy) {
            case "name" -> Comparator.comparing(User::getName);
            case "email" -> Comparator.comparing(User::getEmail);
            case "role" -> Comparator.comparing(User::getRole);
            case "createdAt" -> Comparator.comparing(User::getCreatedAt);
            default -> null;
        };

        if (comparator != null) {
            if ("desc".equalsIgnoreCase(order)) {
                comparator = comparator.reversed();
            }
            users.sort(comparator);
        }
    }

    private static ValidationResult validateUserData(Map<String, Object> data, boolean isPartial) {
        List<String> errors = new ArrayList<>();

        // Name validation
        if (!isPartial || data.containsKey("name")) {
            String name = (String) data.get("name");
            if (name == null || name.trim().isEmpty()) {
                errors.add("Name is required");
            } else if (name.length() < 2) {
                errors.add("Name must be at least 2 characters");
            } else if (name.length() > 100) {
                errors.add("Name must be less than 100 characters");
            }
        }

        // Email validation
        if (!isPartial || data.containsKey("email")) {
            String email = (String) data.get("email");
            if (email == null || email.trim().isEmpty()) {
                errors.add("Email is required");
            } else if (!isValidEmail(email)) {
                errors.add("Invalid email format");
            }
        }

        // Role validation
        if (!isPartial || data.containsKey("role")) {
            String role = (String) data.get("role");
            List<String> validRoles = List.of("admin", "user", "moderator");
            if (role == null || !validRoles.contains(role.toLowerCase())) {
                errors.add("Role must be one of: " + String.join(", ", validRoles));
            }
        }

        // Age validation (optional)
        if (data.containsKey("age")) {
            Object ageObj = data.get("age");
            if (ageObj != null) {
                try {
                    int age = ((Number) ageObj).intValue();
                    if (age < 13 || age > 120) {
                        errors.add("Age must be between 13 and 120");
                    }
                } catch (Exception e) {
                    errors.add("Age must be a valid number");
                }
            }
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }

    private static boolean isValidEmail(String email) {
        return email.contains("@") && email.contains(".") && email.length() > 5;
    }

    private static boolean emailExists(String email) {
        return users.values().stream()
               .anyMatch(user -> user.getEmail().equalsIgnoreCase(email));
    }

    private static User createUser(Map<String, Object> data) {
        return new User(
            idCounter.getAndIncrement(),
            (String) data.get("name"),
            (String) data.get("email"),
            (String) data.get("role"),
            data.containsKey("age") ? ((Number) data.get("age")).intValue() : null,
            System.currentTimeMillis(),
            System.currentTimeMillis()
        );
    }

    private static User updateUser(User existingUser, Map<String, Object> data, boolean isPartial) {
        return new User(
            existingUser.getId(),
            data.containsKey("name") ? (String) data.get("name") : existingUser.getName(),
            data.containsKey("email") ? (String) data.get("email") : existingUser.getEmail(),
            data.containsKey("role") ? (String) data.get("role") : existingUser.getRole(),
            data.containsKey("age") ?
                (data.get("age") != null ? ((Number) data.get("age")).intValue() : null) :
                existingUser.getAge(),
            existingUser.getCreatedAt(),
            System.currentTimeMillis()
        );
    }

    private static void addSampleData() {
        users.put(1, new User(1, "John Doe", "john@example.com", "admin", 30,
                             System.currentTimeMillis(), System.currentTimeMillis()));
        users.put(2, new User(2, "Jane Smith", "jane@example.com", "user", 25,
                             System.currentTimeMillis(), System.currentTimeMillis()));
        users.put(3, new User(3, "Bob Johnson", "bob@example.com", "moderator", 35,
                             System.currentTimeMillis(), System.currentTimeMillis()));
        idCounter.set(4);
    }

    private static int getPort() {
        String portStr = Dotenv.get("PORT");
        return portStr != null ? Integer.parseInt(portStr) : 3000;
    }

    private static void printApiEndpoints() {
        System.out.println("\n📋 Available Endpoints:");
        System.out.println("  GET    /                     - API info");
        System.out.println("  GET    /health               - Health check");
        System.out.println("  GET    /api/v1/users         - List users (supports pagination, search, filtering)");
        System.out.println("  POST   /api/v1/users         - Create user");
        System.out.println("  GET    /api/v1/users/:id     - Get user by ID");
        System.out.println("  PUT    /api/v1/users/:id     - Update user (full)");
        System.out.println("  PATCH  /api/v1/users/:id     - Update user (partial)");
        System.out.println("  DELETE /api/v1/users/:id     - Delete user");
        System.out.println("\n🔍 Query Parameters for GET /api/v1/users:");
        System.out.println("  ?page=1&limit=10             - Pagination");
        System.out.println("  ?search=john                 - Search by name/email");
        System.out.println("  ?role=admin                  - Filter by role");
        System.out.println("  ?sortBy=name&order=asc       - Sort results");
    }

    // Data classes
    static class User {
        private final int id;
        private final String name;
        private final String email;
        private final String role;
        private final Integer age;
        private final long createdAt;
        private final long updatedAt;

        public User(int id, String name, String email, String role, Integer age, long createdAt, long updatedAt) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.role = role;
            this.age = age;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
        }

        // Getters
        public int getId() { return id; }
        public String getName() { return name; }
        public String getEmail() { return email; }
        public String getRole() { return role; }
        public Integer getAge() { return age; }
        public long getCreatedAt() { return createdAt; }
        public long getUpdatedAt() { return updatedAt; }
    }

    static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;

        public ValidationResult(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = errors;
        }

        public boolean isValid() { return valid; }
        public List<String> getErrors() { return errors; }
    }
}
```

## Authentication & Authorization

Add JWT-based authentication to your API:

```java
// Simple JWT simulation (use a proper JWT library in production)
public class AuthService {
    private static final String SECRET = "your-secret-key";
    private static final Map<String, String> tokens = new ConcurrentHashMap<>();

    public static String generateToken(String email) {
        String token = UUID.randomUUID().toString();
        tokens.put(token, email);
        return token;
    }

    public static String validateToken(String token) {
        return tokens.get(token);
    }

    public static void revokeToken(String token) {
        tokens.remove(token);
    }
}

// Authentication middleware
Middleware authMiddleware = (req, res, next) -> {
    String authHeader = req.getHeader("Authorization");

    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        res.status(401).json(Map.of("error", "Missing or invalid authorization header"));
        return;
    }

    String token = authHeader.substring(7);
    String email = AuthService.validateToken(token);

    if (email == null) {
        res.status(401).json(Map.of("error", "Invalid or expired token"));
        return;
    }

    // Add user info to request context
    // req.setUserEmail(email); // You'd need to extend Request class

    next.next(req, res);
};

// Login endpoint
router.post("/auth/login", (req, res, next) -> {
    Map<String, Object> body = req.getBody();
    String email = (String) body.get("email");
    String password = (String) body.get("password");

    // Validate credentials (implement your own logic)
    if (validateCredentials(email, password)) {
        String token = AuthService.generateToken(email);
        res.json(Map.of("token", token, "email", email));
    } else {
        res.status(401).json(Map.of("error", "Invalid credentials"));
    }
});

// Protected routes
router.get("/users/me", authMiddleware, (req, res, next) -> {
    // Get current user info
    String email = getCurrentUserEmail(req);
    User currentUser = findUserByEmail(email);
    res.json(currentUser);
});
```

## API Testing

### Manual Testing with curl

```bash
# Create a user
curl -X POST http://localhost:3000/api/v1/users \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Alice Johnson",
    "email": "alice@example.com",
    "role": "user",
    "age": 28
  }'

# Get all users with pagination
curl "http://localhost:3000/api/v1/users?page=1&limit=5"

# Search users
curl "http://localhost:3000/api/v1/users?search=john"

# Filter by role
curl "http://localhost:3000/api/v1/users?role=admin"

# Get specific user
curl http://localhost:3000/api/v1/users/1

# Update user (partial)
curl -X PATCH http://localhost:3000/api/v1/users/1 \
  -H "Content-Type: application/json" \
  -d '{"name": "John Updated"}'

# Delete user
curl -X DELETE http://localhost:3000/api/v1/users/1
```

### Automated Test Suite

```java
public class APITestSuite {
    private static final String BASE_URL = "http://localhost:3000/api/v1";
    private static final HttpClient client = HttpClient.newHttpClient();

    public static void main(String[] args) throws Exception {
        System.out.println("🧪 Running API Test Suite...\n");

        testCreateUser();
        testGetUsers();
        testGetUserById();
        testUpdateUser();
        testDeleteUser();
        testValidation();

        System.out.println("✅ All tests completed!");
    }

    private static void testCreateUser() throws Exception {
        System.out.println("Testing POST /users...");

        String jsonBody = """
            {
                "name": "Test User",
                "email": "test@example.com",
                "role": "user",
                "age": 25
            }
            """;

        HttpResponse<String> response = sendRequest("POST", "/users", jsonBody);

        assert response.statusCode() == 201 : "Expected 201, got " + response.statusCode();
        System.out.println("✅ User creation test passed");
    }

    private static void testValidation() throws Exception {
        System.out.println("Testing validation...");

        String invalidJson = """
            {
                "name": "",
                "email": "invalid-email",
                "role": "invalid-role"
            }
            """;

        HttpResponse<String> response = sendRequest("POST", "/users", invalidJson);

        assert response.statusCode() == 400 : "Expected 400, got " + response.statusCode();
        System.out.println("✅ Validation test passed");
    }

    private static HttpResponse<String> sendRequest(String method, String path, String body) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + path))
            .header("Content-Type", "application/json");

        if (body != null) {
            builder.method(method, HttpRequest.BodyPublishers.ofString(body));
        } else {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        }

        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }
}
```

## Best Practices

1. **Use proper HTTP status codes** - Return meaningful status codes
2. **Validate all input** - Never trust client data
3. **Implement pagination** - Don't return all records at once
4. **Add filtering and searching** - Make your API flexible
5. **Use consistent error formats** - Standardize error responses
6. **Add rate limiting** - Protect against abuse
7. **Log everything** - Monitor API usage and errors
8. **Version your API** - Use versioning for breaking changes
9. **Document your API** - Provide clear documentation
10. **Test thoroughly** - Automated testing is essential

## Next Steps

- [Authentication Example](authentication.md) - Implement JWT authentication
- [Error Handling](errors.md) - Advanced error handling patterns
- [Logging](logging.md) - Comprehensive logging strategies
