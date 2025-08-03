# Quick Start Guide

- [Your First Heo Application](#your-first-heo-application)
- [Building a Simple API](#building-a-simple-api)
- [Adding Middleware](#adding-middleware)
- [Error Handling](#error-handling)
- [Testing Your Application](#testing-your-application)

## Your First Heo Application

Let's build a simple "Hello World" application to get you started with Heo.

### Step 1: Create the Main Class

Create a new file called `HelloHeo.java`:

```java
import heo.server.Heo;

public class HelloHeo {
    public static void main(String[] args) {
        // Create a new Heo application
        Heo app = new Heo();

        // Define a simple route
        app.get("/", (req, res, next) -> {
            res.send("Hello, World! Welcome to Heo 🐷");
        });

        // Start the server
        app.listen(3000, () -> {
            System.out.println("🚀 Server running on http://localhost:3000");
        });
    }
}
```

### Step 2: Run the Application

```bash
# Compile
javac -cp out HelloHeo.java

# Run
java -cp out:. HelloHeo
```

### Step 3: Test It

Open your browser and visit `http://localhost:3000`. You should see "Hello, World! Welcome to Heo 🐷".

## Building a Simple API

Now let's build a more practical example - a simple Todo API.

### Step 1: Create the Todo API

Create `TodoAPI.java`:

```java
import heo.server.Heo;
import heo.middleware.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class TodoAPI {
    // Simple in-memory storage
    private static final List<Map<String, Object>> todos = new ArrayList<>();
    private static final AtomicInteger idCounter = new AtomicInteger(1);

    public static void main(String[] args) {
        Heo app = new Heo();

        // Add some sample data
        addSampleTodos();

        // Configure middleware
        app.use(new Morgan("dev"));  // Request logging
        app.use(new Json());         // JSON parsing

        // Routes
        setupRoutes(app);

        // Start server
        app.listen(3000, () -> {
            System.out.println("🚀 Todo API running on http://localhost:3000");
            System.out.println("Try these endpoints:");
            System.out.println("  GET    /api/todos");
            System.out.println("  POST   /api/todos");
            System.out.println("  GET    /api/todos/:id");
            System.out.println("  PUT    /api/todos/:id");
            System.out.println("  DELETE /api/todos/:id");
        });
    }

    private static void setupRoutes(Heo app) {
        // Get all todos
        app.get("/api/todos", (req, res, next) -> {
            res.json(Map.of(
                "todos", todos,
                "count", todos.size()
            ));
        });

        // Get todo by ID
        app.get("/api/todos/:id", (req, res, next) -> {
            String id = req.params("id");

            Map<String, Object> todo = findTodoById(id);
            if (todo != null) {
                res.json(todo);
            } else {
                res.status(404).json(Map.of("error", "Todo not found"));
            }
        });

        // Create new todo
        app.post("/api/todos", (req, res, next) -> {
            Map<String, Object> body = req.getBody();

            // Validate input
            if (!body.containsKey("title") || body.get("title").toString().trim().isEmpty()) {
                res.status(400).json(Map.of("error", "Title is required"));
                return;
            }

            // Create todo
            Map<String, Object> todo = new HashMap<>();
            todo.put("id", idCounter.getAndIncrement());
            todo.put("title", body.get("title"));
            todo.put("completed", false);
            todo.put("createdAt", System.currentTimeMillis());

            todos.add(todo);

            res.status(201).json(todo);
        });

        // Update todo
        app.put("/api/todos/:id", (req, res, next) -> {
            String id = req.params("id");
            Map<String, Object> body = req.getBody();

            Map<String, Object> todo = findTodoById(id);
            if (todo == null) {
                res.status(404).json(Map.of("error", "Todo not found"));
                return;
            }

            // Update fields
            if (body.containsKey("title")) {
                todo.put("title", body.get("title"));
            }
            if (body.containsKey("completed")) {
                todo.put("completed", body.get("completed"));
            }
            todo.put("updatedAt", System.currentTimeMillis());

            res.json(todo);
        });

        // Delete todo
        app.delete("/api/todos/:id", (req, res, next) -> {
            String id = req.params("id");

            Map<String, Object> todo = findTodoById(id);
            if (todo == null) {
                res.status(404).json(Map.of("error", "Todo not found"));
                return;
            }

            todos.remove(todo);
            res.status(204).send("");
        });

        // Health check
        app.get("/health", (req, res, next) -> {
            res.json(Map.of(
                "status", "OK",
                "timestamp", System.currentTimeMillis(),
                "totalTodos", todos.size()
            ));
        });
    }

    private static Map<String, Object> findTodoById(String id) {
        try {
            int todoId = Integer.parseInt(id);
            return todos.stream()
                   .filter(todo -> todo.get("id").equals(todoId))
                   .findFirst()
                   .orElse(null);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static void addSampleTodos() {
        Map<String, Object> todo1 = new HashMap<>();
        todo1.put("id", idCounter.getAndIncrement());
        todo1.put("title", "Learn Heo Framework");
        todo1.put("completed", false);
        todo1.put("createdAt", System.currentTimeMillis());

        Map<String, Object> todo2 = new HashMap<>();
        todo2.put("id", idCounter.getAndIncrement());
        todo2.put("title", "Build awesome API");
        todo2.put("completed", false);
        todo2.put("createdAt", System.currentTimeMillis());

        todos.add(todo1);
        todos.add(todo2);
    }
}
```

## Adding Middleware

Let's enhance our Todo API with more middleware for better functionality.

### Step 1: Add CORS Support

```java
import heo.middleware.Cors;

// In your main method, after creating the app:
app.use(new Cors(
    List.of("*"),                    // Allow all origins
    List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"), // Allow these methods
    List.of("Content-Type", "Authorization"), // Allow these headers
    true                             // Allow credentials
));
```

### Step 2: Add Request Validation Middleware

```java
import heo.interfaces.Middleware;

// Create validation middleware
Middleware validateJsonMiddleware = (req, res, next) -> {
    if ("POST".equals(req.getMethod()) || "PUT".equals(req.getMethod())) {
        String contentType = req.getHeader("Content-Type");

        if (contentType == null || !contentType.contains("application/json")) {
            res.status(415).json(Map.of("error", "Content-Type must be application/json"));
            return;
        }
    }

    next.next(req, res);
};

// Apply to API routes
app.use("/api", validateJsonMiddleware);
```

### Step 3: Add Request ID Middleware

```java
import java.util.UUID;

Middleware requestIdMiddleware = (req, res, next) -> {
    String requestId = UUID.randomUUID().toString();
    res.setHeader("X-Request-ID", requestId);

    System.out.println("Request ID: " + requestId + " - " + req.getMethod() + " " + req.path);

    next.next(req, res);
};

app.use(requestIdMiddleware);
```

## Error Handling

Add proper error handling to your application:

```java
import heo.interfaces.ErrorHandler;

// Global error handler
ErrorHandler globalErrorHandler = (error, req, res) -> {
    String requestId = res.getHeaders().get("X-Request-ID");

    System.err.println("Error in request " + requestId + ": " + error.getMessage());
    error.printStackTrace();

    // Send error response
    Map<String, Object> errorResponse = new HashMap<>();
    errorResponse.put("error", "Internal Server Error");
    errorResponse.put("requestId", requestId);
    errorResponse.put("timestamp", System.currentTimeMillis());

    res.status(500).json(errorResponse);
};

app.use(globalErrorHandler);
```

## Testing Your Application

### Manual Testing with curl

```bash
# Get all todos
curl http://localhost:3000/api/todos

# Create a new todo
curl -X POST http://localhost:3000/api/todos \
  -H "Content-Type: application/json" \
  -d '{"title": "Test todo"}'

# Get specific todo
curl http://localhost:3000/api/todos/1

# Update todo
curl -X PUT http://localhost:3000/api/todos/1 \
  -H "Content-Type: application/json" \
  -d '{"title": "Updated todo", "completed": true}'

# Delete todo
curl -X DELETE http://localhost:3000/api/todos/1

# Health check
curl http://localhost:3000/health
```

### Testing with Browser

You can test GET endpoints directly in your browser:

- `http://localhost:3000/api/todos` - See all todos
- `http://localhost:3000/health` - Health check

### Sample Test Script

Create `TestAPI.java` for automated testing:

```java
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;

public class TestAPI {
    private static final HttpClient client = HttpClient.newHttpClient();
    private static final String BASE_URL = "http://localhost:3000";

    public static void main(String[] args) throws Exception {
        System.out.println("Testing Todo API...");

        // Test GET /api/todos
        testGetTodos();

        // Test POST /api/todos
        testCreateTodo();

        // Test health endpoint
        testHealth();

        System.out.println("Tests completed!");
    }

    private static void testGetTodos() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/todos"))
            .GET()
            .build();

        HttpResponse<String> response = client.send(request,
            HttpResponse.BodyHandlers.ofString());

        System.out.println("GET /api/todos: " + response.statusCode());
        System.out.println("Response: " + response.body());
    }

    private static void testCreateTodo() throws Exception {
        String jsonBody = "{\"title\": \"Test from Java\"}";

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/todos"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
            .build();

        HttpResponse<String> response = client.send(request,
            HttpResponse.BodyHandlers.ofString());

        System.out.println("POST /api/todos: " + response.statusCode());
        System.out.println("Response: " + response.body());
    }

    private static void testHealth() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/health"))
            .GET()
            .build();

        HttpResponse<String> response = client.send(request,
            HttpResponse.BodyHandlers.ofString());

        System.out.println("GET /health: " + response.statusCode());
        System.out.println("Response: " + response.body());
    }
}
```

## Environment Configuration

Create a `.env` file for configuration:

```bash
# .env
PORT=3000
APP_NAME=Todo API
DEBUG=true
LOG_LEVEL=dev
```

Update your application to use environment variables:

```java
import heo.config.Dotenv;

public class TodoAPI {
    public static void main(String[] args) {
        // Load environment variables
        Dotenv.config();

        Heo app = new Heo();

        // Use environment variables
        String appName = Dotenv.get("APP_NAME");
        if (appName == null) appName = "Todo API";

        int port = 3000;
        String portStr = Dotenv.get("PORT");
        if (portStr != null) {
            port = Integer.parseInt(portStr);
        }

        // ... rest of your setup

        app.listen(port, () -> {
            System.out.println("🚀 " + appName + " running on http://localhost:" + port);
        });
    }
}
```

## What's Next?

Now that you have a working Heo application, you can:

1. **Add a database** - Integrate with a database for persistent storage
2. **Add authentication** - Implement user authentication and authorization
3. **Add validation** - Use more sophisticated input validation
4. **Add tests** - Write unit and integration tests
5. **Deploy** - Deploy your application to a server

Continue reading the documentation to learn more:

- [Routing](routing.md) - Advanced routing techniques
- [Middleware](middleware.md) - Create custom middleware
- [Error Handling](errors.md) - Robust error handling
- [REST API Guide](rest-api.md) - Build complete REST APIs
