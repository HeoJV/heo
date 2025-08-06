# Heo - A Mini Java HTTP Framework 🐷

<p align="center">
  <img src="logo.png" alt="Heo Logo" width="150"/>
</p>

<p align="center">
  <strong>A lightweight HTTP framework for Java, inspired by Express.js</strong>
</p>

---

## 🌟 What is Heo?

**Heo** is a minimalist HTTP framework built entirely with Java Core. It brings the simplicity and convenience of [Express.js](https://expressjs.com/) to Java, perfect for learning, rapid prototyping, and building lightweight web applications.

### 🎯 Why choose Heo?

- **Easy to learn**: Familiar syntax like Express.js
- **Lightweight**: No external dependencies
- **Flexible**: Supports middleware and dynamic routing

---

## ✨ Key Features

- 🚀 **Express-like API**: `app.get()`, `app.post()`, `app.use()`, `app.listen()`,...
- ⚡ **Middleware Support**: CORS, JSON parsing, logging, error handling
- 🛣️ **Smart Routing**: Dynamic routes with parameters (e.g., `/users/:id`)
- 🔧 **Built-in Utilities**: JSON parser, URL-encoded parser, CORS, Morgan logging
- 📦 **Zero Dependencies**: Only uses Java Core APIs
- 🎯 **Thread Pool**: Efficient concurrent request handling

---

## 🚀 Quick Start

### System Requirements

- Java 22 or higher
- Maven (optional)

### 1. Download and Setup

```bash
git clone https://github.com/102004tan/heo.git
cd heo
```

### 2. Run with Maven (Recommended)

```bash
mvn compile exec:java -Dexec.mainClass="examples.HelloWorld"
```

### 3. Or Manual Compilation

```bash
# Compile all Java files
javac -d out src/main/java/heo/**/*.java

# Run example
java -cp out examples.HelloWorld
```

### 4. Hello World Example

```java
import heo.server.Heo;

public class HelloWorld {
    public static void main(String[] args) {
        Heo app = new Heo();

        // Simple route
        app.get("/", (req, res, next) -> {
            res.send("Hello from Heo! 🐷");
        });

        // Start server
        app.listen(3000, () -> {
            System.out.println("🚀 Server running at http://localhost:3000");
        });
    }
}
```

---

## 📖 Usage Guide

### Basic Routing

```java
Heo app = new Heo();

// GET route
app.get("/users", (req, res, next) -> {
        res.json(Map.of("users", Arrays.asList("Alice", "Bob", "Charlie")));
        });

// POST route
        app.post("/users", (req, res, next) -> {
Map<String, Object> newUser = req.getBody();
    res.status(201).json(Map.of(
        "message", "User created successfully",
                                 "user", newUser
                         ));
        });

// Dynamic route with parameters
        app.get("/users/:id", (req, res, next) -> {
String userId = req.params("id");
    res.json(Map.of("userId", userId, "name", "User " + userId));
        });

// Route with multiple parameters
        app.get("/users/:id/posts/:postId", (req, res, next) -> {
String userId = req.params("id");
String postId = req.params("postId");
    res.json(Map.of("userId", userId, "postId", postId));
        });
```

### Using Middleware

#### Custom Middleware Auth Sample
```java
import heo.middleware.Middleware;
import heo.http.Request;
import heo.http.Response;
class AuthMiddleware implements Middleware {
    @Override
    public void handle(Request req, Response res, Middleware next) {
        String authHeader = req.getHeader("Authorization");
        if (authHeader != null && authHeader.equals("Bearer secret-token")) {
            next.handle(req, res);
        } else {
            res.status(401).json(Map.of("error", "Unauthorized"));
        }
    }
}
```
#### Use the Middleware in your app
```java
import heo.middleware.*;

Heo app = new Heo();

// Use built-in middleware


// Route-specific middleware
app.get("/protected", authMiddleware, (req, res, next) -> {
        res.send("Protected content");
});

```

### Working with Request & Response

```java
app.post("/api/data", (req, res, next) -> {
// Getting information from Request
String method = req.getMethod();           // GET, POST, etc.
String path = req.path;                    // /api/data
String userId = req.params("id");          // URL parameters
String searchQuery = req.getQuery("q");    // Query parameter ?q=...
Map<String, Object> body = req.getBody(); // Parsed JSON body
String authHeader = req.getHeader("Authorization");
String clientIP = req.getIpAddress();

// Sending Response
    res.status(200)                           // Set status code
       .setHeader("Custom-Header", "value")   // Set custom header
       .json(Map.of(                         // Send JSON response
        "success", true,
                     "data", body,
           "timestamp", System.currentTimeMillis()
       ));

               // Or send plain text
               // res.send("Hello World!");
               });
```

### Error Handling


```java
// Global error handler
app.use((error, req, res) -> {
        System.err.println("Error occurred: " + error.getMessage());
        res.status(500).json(Map.of("error", "Internal server error"));
        });

// Throwing custom errors
        app.get("/error-demo", (req, res, next) -> {
        throw new NotFoundError("Resource not found");
});

// Handling specific errors
        app.use((error, req, res) -> {
        if (error instanceof BadRequest) {
        res.status(400).json(Map.of("error", "Invalid request"));
        } else if (error instanceof NotFoundError) {
        res.status(404).json(Map.of("error", "Not found"));
        } else {
        res.status(500).json(Map.of("error", "Server error"));
        }
        });
```

### Sub-routers

```java
Router apiRouter = new Router();

// Define API routes
apiRouter.get("/users", (req, res, next) -> {
        res.json(Map.of("users", getUserList()));
        });

        apiRouter.post("/users", (req, res, next) -> {
Map<String, Object> newUser = req.getBody();
// User creation logic
    res.status(201).json(Map.of("message", "User created"));
        });

// Mount sub-router to main app
        app.use("/api/v1", apiRouter);  // All routes will have /api/v1 prefix
```

---

## 🛠️ Available Middleware

| Middleware     | Description             | Usage                        |
| -------------- | ----------------------- | ---------------------------- |
| `Json()`       | Parse JSON request body | `app.use(new Json())`        |
| `Urlencoded()` | Parse URL-encoded forms | `app.use(new Urlencoded())`  |
| `Morgan()`     | HTTP request logger     | `app.use(new Morgan("dev"))` |
| `Cors()`       | Enable CORS             | `app.use(new Cors(...))`     |

### Morgan Logging Formats

- `"dev"` - Colored output for development, shows method, path, status, time
- `"tiny"` - Minimal format
- `"short"` - Short format
- `"combined"` - Apache combined log format for production

---

## 🎯 Real-world Example

### Simple User Management API

```java
import heo.server.Heo;
import heo.middleware.*;
import java.util.*;

public class UserAPI {
    private static List<Map<String, Object>> users = new ArrayList<>();

    public static void main(String[] args) {
        Heo app = new Heo();

        // Setup middleware
        app.use(new Morgan("dev"));
        app.use(new Json());
        app.use(new Cors(List.of("*"), List.of("*"), List.of("*"), true));

        // Get all users
        app.get("/users", (req, res, next) -> {
            res.json(Map.of("users", users));
        });

        // Create new user
        app.post("/users", (req, res, next) -> {
            Map<String, Object> newUser = req.getBody();
            newUser.put("id", users.size() + 1);
            users.add(newUser);
            res.status(201).json(Map.of("message", "User created", "user", newUser));
        });

        // Get user by ID
        app.get("/users/:id", (req, res, next) -> {
            int userId = Integer.parseInt(req.params("id"));
            users.stream()
                    .filter(user -> user.get("id").equals(userId))
                    .findFirst()
                    .ifPresentOrElse(
                            user -> res.json(user),
                            () -> res.status(404).json(Map.of("error", "User not found"))
                    );
        });

        app.listen(3000, () -> {
            System.out.println("🚀 User API server running on port 3000");
        });
    }
}
```

---

## ⚙️ Configuration

### Using .env files

```bash
# .env file
PORT=8080
DEBUG=true
DB_URL=localhost:5432
```

```java
import heo.config.Dotenv;

public class App {
    public static void main(String[] args) {
        Dotenv.config();  // Load .env file

        int port = Dotenv.get("PORT") != null ?
                Integer.parseInt(Dotenv.get("PORT")) : 3000;

        Heo app = new Heo();
        // ... setup routes

        app.listen(port, () -> {
            System.out.println("Server running on port " + port);
        });
    }
}
```

---

## 🧪 Testing

Run the included examples:

```bash
# HelloWorld example
cd examples
javac -cp ../out HelloWorld.java
java -cp ../out:. HelloWorld

# Visit http://localhost:3000
```

---

## 📁 Project Structure

```
heo/
├── src/main/java/heo/
│   ├── server/          # Core server implementation
│   ├── router/          # Routing system
│   ├── middleware/      # Built-in middleware
│   ├── http/           # HTTP request/response handling
│   ├── exception/      # Error handling
│   ├── config/         # Configuration utilities
│   └── core/           # Core utilities
├── examples/           # Example applications
└── docs/              # Documentation
```

---

## 🤝 Contributing

We welcome all contributions! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for detailed guidelines.

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 🙏 Acknowledgments

- Inspired by [Express.js](https://expressjs.com/)
- Built with ❤️ for the Java community
- Thanks to all contributors

---

<p align="center">
  <strong>Happy coding with Heo! 🐷</strong>
</p>
