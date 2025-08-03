# HTTP Requests

- [Accessing Request Data](#accessing-request-data)
- [Request Methods](#request-methods)
- [Route Parameters](#route-parameters)
- [Query Parameters](#query-parameters)
- [Request Headers](#request-headers)
- [Request Body](#request-body)
- [Client Information](#client-information)

## Accessing Request Data

The `Request` object contains all information about the HTTP request. It's automatically passed to your route handlers and middleware.

```java
app.get("/example", (req, res, next) -> {
    // Request object contains all request information
    String method = req.getMethod();
    String path = req.path;

    res.send("Method: " + method + ", Path: " + path);
});
```

## Request Methods

### Getting the HTTP Method

```java
app.use((req, res, next) -> {
    String method = req.getMethod(); // GET, POST, PUT, DELETE, etc.
    System.out.println("Request method: " + method);
    next.next(req, res);
});
```

### Method-specific Handling

```java
app.use("/api/users", (req, res, next) -> {
    switch (req.getMethod()) {
        case "GET":
            // Handle GET request
            break;
        case "POST":
            // Handle POST request
            break;
        case "PUT":
            // Handle PUT request
            break;
        case "DELETE":
            // Handle DELETE request
            break;
        default:
            res.status(405).send("Method Not Allowed");
            return;
    }
    next.next(req, res);
});
```

## Route Parameters

Route parameters are named URL segments used to capture values from the URL.

### Single Parameter

```java
app.get("/users/:id", (req, res, next) -> {
    String userId = req.params("id");

    // Use the parameter
    User user = getUserById(userId);
    if (user != null) {
        res.json(user);
    } else {
        res.status(404).json(Map.of("error", "User not found"));
    }
});
```

### Multiple Parameters

```java
app.get("/users/:userId/posts/:postId", (req, res, next) -> {
    String userId = req.params("userId");
    String postId = req.params("postId");

    Post post = getPostByUserAndId(userId, postId);
    res.json(post);
});
```

### Nested Parameters

```java
app.get("/categories/:categoryId/products/:productId/reviews/:reviewId", (req, res, next) -> {
    String categoryId = req.params("categoryId");
    String productId = req.params("productId");
    String reviewId = req.params("reviewId");

    Review review = getReview(categoryId, productId, reviewId);
    res.json(review);
});
```

### Parameter Validation

```java
app.get("/users/:id", (req, res, next) -> {
    String id = req.params("id");

    // Validate parameter
    if (id == null || id.trim().isEmpty()) {
        res.status(400).json(Map.of("error", "User ID is required"));
        return;
    }

    // Validate format (if needed)
    try {
        int userId = Integer.parseInt(id);
        if (userId <= 0) {
            res.status(400).json(Map.of("error", "User ID must be positive"));
            return;
        }

        User user = getUserById(userId);
        res.json(user);

    } catch (NumberFormatException e) {
        res.status(400).json(Map.of("error", "User ID must be a number"));
    }
});
```

## Query Parameters

Query parameters are key-value pairs that appear after the `?` in a URL.

### Basic Query Parameters

```java
// URL: /search?q=java&category=programming&limit=10
app.get("/search", (req, res, next) -> {
    String query = req.getQuery("q");
    String category = req.getQuery("category");
    String limitStr = req.getQuery("limit");

    // Provide defaults
    if (query == null) query = "";
    if (category == null) category = "all";
    int limit = limitStr != null ? Integer.parseInt(limitStr) : 20;

    List<SearchResult> results = performSearch(query, category, limit);
    res.json(Map.of("results", results));
});
```

### Getting All Query Parameters

```java
app.get("/debug", (req, res, next) -> {
    Map<String, String> allParams = req.getQuery();
    res.json(Map.of("queryParams", allParams));
});
```

### Query Parameter Validation

```java
app.get("/api/users", (req, res, next) -> {
    // Pagination parameters
    String pageStr = req.getQuery("page");
    String limitStr = req.getQuery("limit");
    String sortBy = req.getQuery("sortBy");
    String order = req.getQuery("order");

    // Validate and set defaults
    int page = 1;
    int limit = 10;

    try {
        if (pageStr != null) {
            page = Integer.parseInt(pageStr);
            if (page < 1) page = 1;
        }

        if (limitStr != null) {
            limit = Integer.parseInt(limitStr);
            if (limit < 1) limit = 10;
            if (limit > 100) limit = 100; // Max limit
        }
    } catch (NumberFormatException e) {
        res.status(400).json(Map.of("error", "Invalid pagination parameters"));
        return;
    }

    // Validate sort parameters
    List<String> validSortFields = List.of("name", "email", "createdAt");
    if (sortBy != null && !validSortFields.contains(sortBy)) {
        res.status(400).json(Map.of("error", "Invalid sort field"));
        return;
    }

    if (order != null && !List.of("asc", "desc").contains(order.toLowerCase())) {
        res.status(400).json(Map.of("error", "Order must be 'asc' or 'desc'"));
        return;
    }

    // Fetch users with parameters
    List<User> users = getUsers(page, limit, sortBy, order);
    res.json(Map.of(
        "users", users,
        "page", page,
        "limit", limit,
        "totalPages", getTotalPages(limit)
    ));
});
```

## Request Headers

### Getting Headers

```java
app.get("/api/protected", (req, res, next) -> {
    String authorization = req.getHeader("Authorization");
    String contentType = req.getHeader("Content-Type");
    String userAgent = req.getHeader("User-Agent");

    res.json(Map.of(
        "authorization", authorization,
        "contentType", contentType,
        "userAgent", userAgent
    ));
});
```

### Header Validation

```java
app.post("/api/data", (req, res, next) -> {
    String contentType = req.getHeader("Content-Type");

    if (contentType == null || !contentType.contains("application/json")) {
        res.status(415).json(Map.of("error", "Content-Type must be application/json"));
        return;
    }

    next.next(req, res);
});
```

### Custom Headers

```java
app.get("/api/version", (req, res, next) -> {
    String apiVersion = req.getHeader("X-API-Version");
    String clientVersion = req.getHeader("X-Client-Version");

    // Handle different API versions
    if ("2.0".equals(apiVersion)) {
        res.json(Map.of("version", "2.0", "features", getV2Features()));
    } else {
        res.json(Map.of("version", "1.0", "features", getV1Features()));
    }
});
```

## Request Body

### JSON Body

```java
app.post("/api/users", (req, res, next) -> {
    Map<String, Object> body = req.getBody();

    // Extract data from JSON body
    String name = (String) body.get("name");
    String email = (String) body.get("email");
    Integer age = (Integer) body.get("age");

    // Validate required fields
    if (name == null || email == null) {
        res.status(400).json(Map.of("error", "Name and email are required"));
        return;
    }

    User newUser = createUser(name, email, age);
    res.status(201).json(newUser);
});
```

### Raw Body

```java
app.post("/webhook", (req, res, next) -> {
    String rawBody = req.getRawBody();

    // Process raw body (useful for webhooks, file uploads, etc.)
    processWebhookData(rawBody);

    res.status(200).send("OK");
});
```

### Nested JSON Data

```java
app.post("/api/orders", (req, res, next) -> {
    Map<String, Object> body = req.getBody();

    // Extract nested data
    String customerId = (String) body.get("customerId");
    List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
    Map<String, Object> shippingAddress = (Map<String, Object>) body.get("shippingAddress");

    // Validate structure
    if (customerId == null || items == null || items.isEmpty()) {
        res.status(400).json(Map.of("error", "Invalid order data"));
        return;
    }

    // Process items
    List<OrderItem> orderItems = new ArrayList<>();
    for (Map<String, Object> item : items) {
        String productId = (String) item.get("productId");
        Integer quantity = (Integer) item.get("quantity");

        if (productId == null || quantity == null || quantity <= 0) {
            res.status(400).json(Map.of("error", "Invalid item data"));
            return;
        }

        orderItems.add(new OrderItem(productId, quantity));
    }

    Order order = createOrder(customerId, orderItems, shippingAddress);
    res.status(201).json(order);
});
```

## Client Information

### IP Address

```java
app.get("/api/client-info", (req, res, next) -> {
    String clientIP = req.getIpAddress();

    res.json(Map.of(
        "clientIP", clientIP,
        "timestamp", System.currentTimeMillis()
    ));
});
```

### User Agent Analysis

```java
app.get("/api/browser-info", (req, res, next) -> {
    String userAgent = req.getHeader("User-Agent");

    // Simple browser detection
    String browser = "Unknown";
    if (userAgent != null) {
        if (userAgent.contains("Chrome")) browser = "Chrome";
        else if (userAgent.contains("Firefox")) browser = "Firefox";
        else if (userAgent.contains("Safari")) browser = "Safari";
        else if (userAgent.contains("Edge")) browser = "Edge";
    }

    res.json(Map.of(
        "userAgent", userAgent,
        "browser", browser,
        "clientIP", req.getIpAddress()
    ));
});
```

## Request Examples

### Complete Request Handler

```java
app.post("/api/products/:categoryId", (req, res, next) -> {
    try {
        // Route parameters
        String categoryId = req.params("categoryId");

        // Query parameters
        String validateOnly = req.getQuery("validateOnly");
        boolean isValidation = "true".equals(validateOnly);

        // Headers
        String authorization = req.getHeader("Authorization");
        if (authorization == null) {
            res.status(401).json(Map.of("error", "Authorization required"));
            return;
        }

        // Body data
        Map<String, Object> body = req.getBody();
        String name = (String) body.get("name");
        String description = (String) body.get("description");
        Double price = (Double) body.get("price");

        // Validation
        List<String> errors = new ArrayList<>();

        if (name == null || name.trim().isEmpty()) {
            errors.add("Product name is required");
        }

        if (price == null || price <= 0) {
            errors.add("Valid price is required");
        }

        if (!errors.isEmpty()) {
            res.status(400).json(Map.of("errors", errors));
            return;
        }

        // If validation only, return success
        if (isValidation) {
            res.json(Map.of("valid", true));
            return;
        }

        // Create product
        Product product = createProduct(categoryId, name, description, price);
        res.status(201).json(product);

    } catch (Exception e) {
        res.status(500).json(Map.of("error", "Internal server error"));
    }
});
```

### File Upload Simulation

```java
app.post("/api/upload", (req, res, next) -> {
    String contentType = req.getHeader("Content-Type");

    if (contentType != null && contentType.contains("multipart/form-data")) {
        // Note: Heo doesn't have built-in multipart support yet
        // This is a conceptual example
        String rawBody = req.getRawBody();

        // You'd need to implement multipart parsing
        // FileUpload upload = parseMultipartData(rawBody);

        res.json(Map.of("message", "File upload simulation"));
    } else {
        res.status(400).json(Map.of("error", "Expected multipart/form-data"));
    }
});
```

## Best Practices

1. **Validate input early** - Check parameters and body data immediately
2. **Provide meaningful errors** - Return specific error messages
3. **Use appropriate status codes** - 400 for validation errors, 401 for auth, etc.
4. **Handle edge cases** - Check for null values and unexpected data types
5. **Log important requests** - Keep track of important operations
6. **Sanitize input** - Clean and validate all user input

## Next Steps

- [Response](response.md) - Learn about sending HTTP responses
- [Error Handling](errors.md) - Handle errors gracefully
- [Middleware](middleware.md) - Process requests with middleware
