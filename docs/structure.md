# Directory Structure

- [Recommended Project Structure](#recommended-project-structure)
- [Core Framework Structure](#core-framework-structure)
- [Application Organization](#application-organization)
- [Configuration Files](#configuration-files)
- [Best Practices](#best-practices)

## Recommended Project Structure

Here's the recommended directory structure for a Heo application:

```
my-heo-app/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   ├── com/myapp/
│   │   │   │   ├── Application.java          # Main application entry point
│   │   │   │   ├── config/                   # Configuration classes
│   │   │   │   │   ├── AppConfig.java
│   │   │   │   │   ├── DatabaseConfig.java
│   │   │   │   │   └── SecurityConfig.java
│   │   │   │   ├── controllers/              # Route handlers (controllers)
│   │   │   │   │   ├── UserController.java
│   │   │   │   │   ├── ProductController.java
│   │   │   │   │   └── AuthController.java
│   │   │   │   ├── middleware/               # Custom middleware
│   │   │   │   │   ├── AuthMiddleware.java
│   │   │   │   │   ├── ValidationMiddleware.java
│   │   │   │   │   └── RateLimitMiddleware.java
│   │   │   │   ├── models/                   # Data models
│   │   │   │   │   ├── User.java
│   │   │   │   │   ├── Product.java
│   │   │   │   │   └── Order.java
│   │   │   │   ├── services/                 # Business logic
│   │   │   │   │   ├── UserService.java
│   │   │   │   │   ├── ProductService.java
│   │   │   │   │   └── EmailService.java
│   │   │   │   ├── repositories/             # Data access layer
│   │   │   │   │   ├── UserRepository.java
│   │   │   │   │   └── ProductRepository.java
│   │   │   │   ├── dto/                      # Data Transfer Objects
│   │   │   │   │   ├── CreateUserRequest.java
│   │   │   │   │   ├── UpdateUserRequest.java
│   │   │   │   │   └── UserResponse.java
│   │   │   │   ├── exceptions/               # Custom exceptions
│   │   │   │   │   ├── ValidationException.java
│   │   │   │   │   ├── NotFoundException.java
│   │   │   │   │   └── AuthenticationException.java
│   │   │   │   └── utils/                    # Utility classes
│   │   │   │       ├── DateUtils.java
│   │   │   │       ├── ValidationUtils.java
│   │   │   │       └── JsonUtils.java
│   │   │   └── heo/                          # Heo framework (if embedded)
│   │   └── resources/                        # Resources
│   │       ├── application.properties
│   │       ├── static/                       # Static files
│   │       │   ├── css/
│   │       │   ├── js/
│   │       │   └── images/
│   │       └── templates/                    # Template files (if any)
│   └── test/
│       └── java/
│           └── com/myapp/
│               ├── controllers/
│               ├── services/
│               ├── repositories/
│               └── integration/
├── docs/                                     # Documentation
│   ├── api.md
│   ├── deployment.md
│   └── development.md
├── scripts/                                  # Build and deployment scripts
│   ├── build.sh
│   ├── deploy.sh
│   └── test.sh
├── .env                                      # Environment variables (not in git)
├── .env.example                              # Environment variables template
├── .gitignore
├── pom.xml                                   # Maven configuration
└── README.md
```

## Core Framework Structure

The Heo framework itself is organized as follows:

```
heo/
├── src/main/java/heo/
│   ├── server/
│   │   └── Heo.java                         # Main server class
│   ├── router/
│   │   ├── Router.java                      # Router implementation
│   │   └── Route.java                       # Route representation
│   ├── middleware/
│   │   ├── MiddlewareChain.java            # Middleware chain execution
│   │   ├── Json.java                       # JSON parsing middleware
│   │   ├── Urlencoded.java                 # URL-encoded parsing middleware
│   │   ├── Morgan.java                     # HTTP request logger
│   │   └── Cors.java                       # CORS middleware
│   ├── http/
│   │   ├── Request.java                    # HTTP request wrapper
│   │   ├── Response.java                   # HTTP response wrapper
│   │   ├── HttpMethod.java                 # HTTP method constants
│   │   └── HttpStatusCode.java             # HTTP status codes
│   ├── interfaces/
│   │   ├── Middleware.java                 # Middleware interface
│   │   ├── RouterHandler.java              # Router handler interface
│   │   └── ErrorHandler.java               # Error handler interface
│   ├── exception/
│   │   ├── ErrorResponse.java              # Base error response
│   │   ├── BadRequest.java                 # 400 Bad Request
│   │   ├── NotFoundError.java              # 404 Not Found
│   │   ├── UnauthorizedError.java          # 401 Unauthorized
│   │   ├── ForbiddenError.java             # 403 Forbidden
│   │   └── InternalServerError.java        # 500 Internal Server Error
│   ├── config/
│   │   └── Dotenv.java                     # Environment configuration
│   ├── core/
│   │   └── Console.java                    # Logging utilities
│   └── enums/
│       └── MorganFormat.java               # Morgan logging formats
└── examples/
    └── HelloWorld.java                     # Basic example
```

## Application Organization

### Controllers (Route Handlers)

Organize your route handlers into controller classes:

```java
// UserController.java
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    public static Router createRoutes(UserService userService) {
        UserController controller = new UserController(userService);
        Router router = new Router();

        router.get("/", controller::listUsers);
        router.post("/", controller::createUser);
        router.get("/:id", controller::getUserById);
        router.put("/:id", controller::updateUser);
        router.delete("/:id", controller::deleteUser);

        return router;
    }

    private void listUsers(Request req, Response res, MiddlewareChain next) {
        // Implementation
    }

    private void createUser(Request req, Response res, MiddlewareChain next) {
        // Implementation
    }

    // Other methods...
}
```

### Services (Business Logic)

Keep business logic in service classes:

```java
// UserService.java
public class UserService {
    private final UserRepository userRepository;
    private final EmailService emailService;

    public UserService(UserRepository userRepository, EmailService emailService) {
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    public User createUser(CreateUserRequest request) {
        // Business logic for user creation
        validateUserData(request);
        User user = new User(request.getName(), request.getEmail());
        User savedUser = userRepository.save(user);
        emailService.sendWelcomeEmail(savedUser);
        return savedUser;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // Other business methods...
}
```

### Repositories (Data Access)

Organize data access in repository classes:

```java
// UserRepository.java
public class UserRepository {
    private final Map<Integer, User> users = new ConcurrentHashMap<>();
    private final AtomicInteger idCounter = new AtomicInteger(1);

    public User save(User user) {
        if (user.getId() == null) {
            user.setId(idCounter.getAndIncrement());
        }
        users.put(user.getId(), user);
        return user;
    }

    public Optional<User> findById(Integer id) {
        return Optional.ofNullable(users.get(id));
    }

    public List<User> findAll() {
        return new ArrayList<>(users.values());
    }

    public Optional<User> findByEmail(String email) {
        return users.values().stream()
                .filter(user -> user.getEmail().equals(email))
                .findFirst();
    }

    public void deleteById(Integer id) {
        users.remove(id);
    }
}
```

### Configuration Classes

Organize configuration in dedicated classes:

```java
// AppConfig.java
public class AppConfig {
    public static Heo createApplication() {
        Heo app = new Heo();

        // Load environment
        Dotenv.config();

        // Configure middleware
        configureMiddleware(app);

        // Configure routes
        configureRoutes(app);

        // Configure error handling
        configureErrorHandling(app);

        return app;
    }

    private static void configureMiddleware(Heo app) {
        // Global middleware configuration
        app.use(new Morgan(getLogFormat()));
        app.use(createCorsMiddleware());
        app.use(new Json());
    }

    private static void configureRoutes(Heo app) {
        // Dependency injection (manual for now)
        UserRepository userRepository = new UserRepository();
        EmailService emailService = new EmailService();
        UserService userService = new UserService(userRepository, emailService);

        // Mount routes
        app.use("/api/users", UserController.createRoutes(userService));
        app.use("/api/auth", AuthController.createRoutes());
    }

    // Other configuration methods...
}
```

## Configuration Files

### Environment Configuration (.env)

```bash
# Server Configuration
PORT=8080
HOST=localhost

# Application Settings
APP_NAME=My Heo Application
APP_ENV=development
DEBUG=true

# Database Configuration
DB_HOST=localhost
DB_PORT=5432
DB_NAME=myapp
DB_USER=username
DB_PASS=password

# Security
SECRET_KEY=your-secret-key-here
JWT_SECRET=jwt-secret-key
JWT_EXPIRATION=86400

# External Services
EMAIL_SMTP_HOST=smtp.gmail.com
EMAIL_SMTP_PORT=587
EMAIL_USERNAME=your-email@gmail.com
EMAIL_PASSWORD=your-app-password

# Logging
LOG_LEVEL=debug
LOG_FORMAT=dev

# Rate Limiting
RATE_LIMIT_WINDOW=60000
RATE_LIMIT_MAX_REQUESTS=100

# CORS
CORS_ORIGINS=http://localhost:3000,http://localhost:8080
CORS_METHODS=GET,POST,PUT,DELETE,OPTIONS
CORS_HEADERS=Content-Type,Authorization
```

### Maven Configuration (pom.xml)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.mycompany</groupId>
    <artifactId>my-heo-app</artifactId>
    <version>1.0.0</version>
    <name>My Heo Application</name>

    <properties>
        <maven.compiler.source>22</maven.compiler.source>
        <maven.compiler.target>22</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <heo.version>1.0.0</heo.version>
        <jackson.version>2.19.0</jackson.version>
        <junit.version>5.10.0</junit.version>
    </properties>

    <dependencies>
        <!-- Heo Framework -->
        <dependency>
            <groupId>com.heo</groupId>
            <artifactId>heo</artifactId>
            <version>${heo.version}</version>
        </dependency>

        <!-- JSON Processing -->
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
            <version>${jackson.version}</version>
        </dependency>

        <!-- Testing -->
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>${junit.version}</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.11.0</version>
                <configuration>
                    <source>22</source>
                    <target>22</target>
                </configuration>
            </plugin>

            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>3.1.2</version>
            </plugin>

            <plugin>
                <groupId>org.codehaus.mojo</groupId>
                <artifactId>exec-maven-plugin</artifactId>
                <version>3.1.0</version>
                <configuration>
                    <mainClass>com.myapp.Application</mainClass>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

### Git Configuration (.gitignore)

```gitignore
# Compiled class files
*.class
target/
out/

# Log files
*.log

# Environment files
.env
.env.local
.env.production

# IDE files
.idea/
*.iml
.vscode/
.eclipse/

# OS files
.DS_Store
Thumbs.db

# Temporary files
*.tmp
*.temp

# Application specific
uploads/
cache/
logs/
```

## Best Practices

### Package Organization

1. **Use meaningful package names** - Organize by feature or layer
2. **Keep packages focused** - Each package should have a single responsibility
3. **Avoid circular dependencies** - Structure packages to avoid circular references
4. **Use standard naming conventions** - Follow Java package naming conventions

### File Organization

1. **Group related functionality** - Keep related classes together
2. **Separate concerns** - Controllers, services, and repositories in separate packages
3. **Use descriptive names** - File names should clearly indicate their purpose
4. **Keep files manageable** - Break large files into smaller, focused ones

### Configuration Management

1. **Use environment variables** - Keep sensitive data in environment variables
2. **Provide defaults** - Always provide sensible defaults for configuration
3. **Validate configuration** - Check configuration values at startup
4. **Document configuration** - Maintain clear documentation for all configuration options

### Resource Management

1. **Organize static files** - Keep CSS, JavaScript, and images organized
2. **Use relative paths** - Avoid hardcoded absolute paths
3. **Optimize resources** - Minimize and compress static resources
4. **Version resources** - Use versioning for cache busting

### Testing Structure

1. **Mirror source structure** - Test packages should mirror source packages
2. **Use descriptive test names** - Test method names should describe what they test
3. **Organize test types** - Separate unit, integration, and end-to-end tests
4. **Use test utilities** - Create helper classes for common test operations

## Example Main Application

```java
// Application.java
package com.myapp;

import heo.server.Heo;
import heo.config.Dotenv;
import com.myapp.config.AppConfig;

public class Application {
    public static void main(String[] args) {
        // Load environment configuration
        Dotenv.config();

        // Create and configure application
        Heo app = AppConfig.createApplication();

        // Get port from environment
        int port = getPort();

        // Start server
        app.listen(port, () -> {
            String appName = Dotenv.get("APP_NAME");
            String environment = Dotenv.get("APP_ENV");

            System.out.println("🚀 " + appName + " running on http://localhost:" + port);
            System.out.println("Environment: " + environment);

            if ("development".equals(environment)) {
                System.out.println("API Documentation: http://localhost:" + port + "/docs");
                System.out.println("Health Check: http://localhost:" + port + "/health");
            }
        });
    }

    private static int getPort() {
        String portStr = Dotenv.get("PORT");
        return portStr != null ? Integer.parseInt(portStr) : 8080;
    }
}
```

## Next Steps

- [Configuration](configuration.md) - Learn more about application configuration
- [Quick Start Guide](quickstart.md) - Build your first application
- [REST API Guide](rest-api.md) - Build a complete REST API
