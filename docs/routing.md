# Routing

- [Basic Routing](#basic-routing)
- [Route Parameters](#route-parameters)
- [Route Methods](#route-methods)
- [Route Groups](#route-groups)
- [Route Middleware](#route-middleware)
- [Sub-routers](#sub-routers)

## Basic Routing

Heo provides an Express.js-like routing system that makes defining application routes simple and intuitive.

### Defining Routes

The most basic Heo routes accept a URI and a closure:

```java
import heo.server.Heo;

Heo app = new Heo();

app.get("/", (req, res, next) -> {
    res.send("Hello World");
});

app.post("/users", (req, res, next) -> {
    res.send("Create a new user");
});
```

### Available Router Methods

The router allows you to register routes that respond to any HTTP verb:

```java
app.get("/users", handler);
app.post("/users", handler);
app.put("/users/:id", handler);
app.patch("/users/:id", handler);
app.delete("/users/:id", handler);
```

## Route Parameters

### Required Parameters

You may capture segments of the URI within your route definition by defining route parameters:

```java
app.get("/users/:id", (req, res, next) -> {
    String userId = req.params("id");
    res.send("User ID: " + userId);
});

app.get("/users/:id/posts/:postId", (req, res, next) -> {
    String userId = req.params("id");
    String postId = req.params("postId");

    res.json(Map.of(
        "userId", userId,
        "postId", postId
    ));
});
```

### Multiple Parameters

Routes can have multiple parameters:

```java
app.get("/users/:userId/posts/:postId/comments/:commentId", (req, res, next) -> {
    Map<String, String> params = Map.of(
        "userId", req.params("userId"),
        "postId", req.params("postId"),
        "commentId", req.params("commentId")
    );

    res.json(params);
});
```

## Route Methods

### GET Routes

```java
// Simple GET route
app.get("/", (req, res, next) -> {
    res.send("Welcome to Heo!");
});

// GET with parameters
app.get("/users/:id", (req, res, next) -> {
    String id = req.params("id");
    // Fetch user logic here
    res.json(Map.of("user", getUserById(id)));
});

// GET with query parameters
app.get("/search", (req, res, next) -> {
    String query = req.getQuery("q");
    String category = req.getQuery("category");

    res.json(Map.of(
        "query", query,
        "category", category,
        "results", performSearch(query, category)
    ));
});
```

### POST Routes

```java
app.post("/users", (req, res, next) -> {
    Map<String, Object> userData = req.getBody();

    // Create user logic here
    User newUser = createUser(userData);

    res.status(201).json(Map.of(
        "message", "User created successfully",
        "user", newUser
    ));
});

app.post("/auth/login", (req, res, next) -> {
    String email = (String) req.getBody().get("email");
    String password = (String) req.getBody().get("password");

    if (authenticate(email, password)) {
        String token = generateToken(email);
        res.json(Map.of("token", token));
    } else {
        res.status(401).json(Map.of("error", "Invalid credentials"));
    }
});
```

### PUT Routes

```java
app.put("/users/:id", (req, res, next) -> {
    String userId = req.params("id");
    Map<String, Object> updateData = req.getBody();

    User updatedUser = updateUser(userId, updateData);

    res.json(Map.of(
        "message", "User updated successfully",
        "user", updatedUser
    ));
});
```

### DELETE Routes

```java
app.delete("/users/:id", (req, res, next) -> {
    String userId = req.params("id");

    deleteUser(userId);

    res.status(204).send("");
});
```

## Route Groups

### Using Sub-routers

You can group related routes using sub-routers:

```java
import heo.router.Router;

// Create API router
Router apiRouter = new Router();

// Define API routes
apiRouter.get("/users", (req, res, next) -> {
    res.json(Map.of("users", getAllUsers()));
});

apiRouter.post("/users", (req, res, next) -> {
    Map<String, Object> userData = req.getBody();
    User newUser = createUser(userData);
    res.status(201).json(newUser);
});

apiRouter.get("/users/:id", (req, res, next) -> {
    String id = req.params("id");
    User user = getUserById(id);
    if (user != null) {
        res.json(user);
    } else {
        res.status(404).json(Map.of("error", "User not found"));
    }
});

// Mount the API router
app.use("/api/v1", apiRouter);
```

### Versioned APIs

```java
// API v1
Router apiV1 = new Router();
apiV1.get("/users", (req, res, next) -> {
    res.json(Map.of("version", "v1", "users", getAllUsersV1()));
});

// API v2
Router apiV2 = new Router();
apiV2.get("/users", (req, res, next) -> {
    res.json(Map.of("version", "v2", "users", getAllUsersV2()));
});

// Mount different versions
app.use("/api/v1", apiV1);
app.use("/api/v2", apiV2);
```

## Route Middleware

### Single Middleware

You can apply middleware to specific routes:

```java
// Authentication middleware
Middleware authMiddleware = (req, res, next) -> {
    String token = req.getHeader("Authorization");

    if (token == null || !isValidToken(token)) {
        res.status(401).json(Map.of("error", "Unauthorized"));
        return;
    }

    // Add user info to request
    User user = getUserFromToken(token);
    req.setUser(user); // Assuming this method exists

    next.next(req, res);
};

// Apply middleware to specific route
app.get("/profile", authMiddleware, (req, res, next) -> {
    User user = req.getUser(); // Assuming this method exists
    res.json(user);
});
```

### Multiple Middleware

```java
// Logging middleware
Middleware logMiddleware = (req, res, next) -> {
    System.out.println("Request: " + req.getMethod() + " " + req.path);
    next.next(req, res);
};

// Validation middleware
Middleware validateUserData = (req, res, next) -> {
    Map<String, Object> body = req.getBody();

    if (!body.containsKey("name") || !body.containsKey("email")) {
        res.status(400).json(Map.of("error", "Name and email are required"));
        return;
    }

    next.next(req, res);
};

// Apply multiple middleware to route
app.post("/users", logMiddleware, authMiddleware, validateUserData, (req, res, next) -> {
    Map<String, Object> userData = req.getBody();
    User newUser = createUser(userData);
    res.status(201).json(newUser);
});
```

## Sub-routers

### Resource Routes

Create a resource router for CRUD operations:

```java
public class UserRouter {
    public static Router create() {
        Router router = new Router();

        // GET /users
        router.get("/", (req, res, next) -> {
            res.json(Map.of("users", getAllUsers()));
        });

        // POST /users
        router.post("/", (req, res, next) -> {
            Map<String, Object> userData = req.getBody();
            User newUser = createUser(userData);
            res.status(201).json(newUser);
        });

        // GET /users/:id
        router.get("/:id", (req, res, next) -> {
            String id = req.params("id");
            User user = getUserById(id);

            if (user != null) {
                res.json(user);
            } else {
                res.status(404).json(Map.of("error", "User not found"));
            }
        });

        // PUT /users/:id
        router.put("/:id", (req, res, next) -> {
            String id = req.params("id");
            Map<String, Object> updateData = req.getBody();

            User updatedUser = updateUser(id, updateData);
            res.json(updatedUser);
        });

        // DELETE /users/:id
        router.delete("/:id", (req, res, next) -> {
            String id = req.params("id");
            deleteUser(id);
            res.status(204).send("");
        });

        return router;
    }
}

// Use the resource router
app.use("/users", UserRouter.create());
```

### Nested Routes

```java
// Posts router
Router postsRouter = new Router();
postsRouter.get("/", (req, res, next) -> {
    String userId = req.params("userId"); // From parent route
    res.json(Map.of("posts", getPostsByUserId(userId)));
});

postsRouter.post("/", (req, res, next) -> {
    String userId = req.params("userId");
    Map<String, Object> postData = req.getBody();
    Post newPost = createPost(userId, postData);
    res.status(201).json(newPost);
});

// Comments router
Router commentsRouter = new Router();
commentsRouter.get("/", (req, res, next) -> {
    String postId = req.params("postId");
    res.json(Map.of("comments", getCommentsByPostId(postId)));
});

// Nest routers
postsRouter.use("/:postId/comments", commentsRouter);
app.use("/users/:userId/posts", postsRouter);

// Results in routes like:
// GET /users/123/posts
// POST /users/123/posts
// GET /users/123/posts/456/comments
```

## Route Examples

### RESTful API

```java
public class ProductAPI {
    public static void setupRoutes(Heo app) {
        Router productRouter = new Router();

        // GET /api/products - List all products
        productRouter.get("/", (req, res, next) -> {
            String category = req.getQuery("category");
            String sort = req.getQuery("sort");

            List<Product> products = getProducts(category, sort);
            res.json(Map.of("products", products));
        });

        // POST /api/products - Create product
        productRouter.post("/", (req, res, next) -> {
            Map<String, Object> productData = req.getBody();
            Product product = createProduct(productData);
            res.status(201).json(product);
        });

        // GET /api/products/:id - Get single product
        productRouter.get("/:id", (req, res, next) -> {
            String id = req.params("id");
            Product product = getProductById(id);

            if (product != null) {
                res.json(product);
            } else {
                res.status(404).json(Map.of("error", "Product not found"));
            }
        });

        // PUT /api/products/:id - Update product
        productRouter.put("/:id", (req, res, next) -> {
            String id = req.params("id");
            Map<String, Object> updateData = req.getBody();

            Product updatedProduct = updateProduct(id, updateData);
            res.json(updatedProduct);
        });

        // DELETE /api/products/:id - Delete product
        productRouter.delete("/:id", (req, res, next) -> {
            String id = req.params("id");
            deleteProduct(id);
            res.status(204).send("");
        });

        app.use("/api/products", productRouter);
    }
}
```

## Best Practices

1. **Use meaningful route names** - Choose clear, descriptive paths
2. **Follow RESTful conventions** - Use standard HTTP methods appropriately
3. **Group related routes** - Use sub-routers to organize your API
4. **Apply middleware consistently** - Use middleware for cross-cutting concerns
5. **Validate input** - Always validate route parameters and request bodies
6. **Handle errors gracefully** - Provide meaningful error responses

## Next Steps

- [Middleware](middleware.md) - Learn about using and creating middleware
- [Request](request.md) - Working with HTTP requests
- [Response](response.md) - Sending HTTP responses
