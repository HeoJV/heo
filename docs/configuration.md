# Configuration

- [Introduction](#introduction)
- [Environment Configuration](#environment-configuration)
- [Application Configuration](#application-configuration)
- [Server Configuration](#server-configuration)
- [Middleware Configuration](#middleware-configuration)

## Introduction

Heo applications can be configured in several ways to suit your development and production needs. Configuration is typically handled through environment variables, configuration files, and programmatic setup.

## Environment Configuration

### Environment Variables

Heo supports loading configuration from environment variables using the built-in `Dotenv` class.

#### Creating a .env File

Create a `.env` file in your project root:

```bash
# Server Configuration
PORT=8080
HOST=localhost

# Application Settings
APP_NAME=MyHeoApp
APP_ENV=development
DEBUG=true

# Database (if you add database support)
DB_HOST=localhost
DB_PORT=5432
DB_NAME=myapp
DB_USER=username
DB_PASS=password

# Security
SECRET_KEY=your-secret-key-here
JWT_SECRET=jwt-secret-key

# Logging
LOG_LEVEL=debug
LOG_FILE=app.log
```

#### Loading Environment Variables

```java
import heo.config.Dotenv;

public class App {
    public static void main(String[] args) {
        // Load environment variables from .env file
        Dotenv.config();

        // Access environment variables
        String port = Dotenv.get("PORT");
        String appName = Dotenv.get("APP_NAME");
        boolean debug = Boolean.parseBoolean(Dotenv.get("DEBUG"));

        // Use default values if not set
        int serverPort = port != null ? Integer.parseInt(port) : 3000;

        Heo app = new Heo();
        // ... configure your app

        app.listen(serverPort, () -> {
            System.out.println(appName + " running on port " + serverPort);
        });
    }
}
```

## Application Configuration

### Basic Application Setup

```java
import heo.server.Heo;
import heo.middleware.*;

public class AppConfig {
    public static Heo createApp() {
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
        // Request logging
        app.use(new Morgan("dev"));

        // JSON parsing
        app.use(new Json());

        // URL encoded parsing
        app.use(new Urlencoded());

        // CORS
        app.use(new Cors(
            List.of("*"),
            List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"),
            List.of("Content-Type", "Authorization"),
            true
        ));
    }

    private static void configureRoutes(Heo app) {
        // API routes
        Router apiRouter = new Router();
        // ... define API routes
        app.use("/api", apiRouter);

        // Web routes
        app.get("/", (req, res, next) -> {
            res.send("Welcome to " + Dotenv.get("APP_NAME"));
        });
    }

    private static void configureErrorHandling(Heo app) {
        app.use((error, req, res) -> {
            boolean debug = Boolean.parseBoolean(Dotenv.get("DEBUG"));

            if (debug) {
                res.status(500).json(Map.of(
                    "error", error.getMessage(),
                    "stack", error.getStackTrace()
                ));
            } else {
                res.status(500).json(Map.of(
                    "error", "Internal Server Error"
                ));
            }
        });
    }
}
```

## Server Configuration

### Port Configuration

```java
public class ServerConfig {
    public static void main(String[] args) {
        Heo app = AppConfig.createApp();

        // Get port from environment or use default
        int port = getPort();
        String host = getHost();

        app.listen(port, () -> {
            System.out.println("🚀 Server running on " + host + ":" + port);
            if (isProduction()) {
                System.out.println("Environment: Production");
            } else {
                System.out.println("Environment: Development");
                System.out.println("Debug mode: enabled");
            }
        });
    }

    private static int getPort() {
        String port = Dotenv.get("PORT");
        return port != null ? Integer.parseInt(port) : 3000;
    }

    private static String getHost() {
        String host = Dotenv.get("HOST");
        return host != null ? host : "localhost";
    }

    private static boolean isProduction() {
        String env = Dotenv.get("APP_ENV");
        return "production".equals(env);
    }
}
```

### Thread Pool Configuration

If you need to customize the thread pool (this would require modifying the Heo source):

```java
// Example of how you might configure threading
// (This is conceptual - actual implementation may vary)
public class ThreadConfig {
    public static void configureThreadPool(Heo app) {
        int maxThreads = Integer.parseInt(Dotenv.get("MAX_THREADS") ?? "100");
        int coreThreads = Integer.parseInt(Dotenv.get("CORE_THREADS") ?? "10");

        // Configure thread pool settings
        // app.setThreadPoolSize(maxThreads, coreThreads);
    }
}
```

## Middleware Configuration

### CORS Configuration

```java
public class CorsConfig {
    public static Cors createCorsMiddleware() {
        String allowedOrigins = Dotenv.get("CORS_ORIGINS");
        String allowedMethods = Dotenv.get("CORS_METHODS");
        String allowedHeaders = Dotenv.get("CORS_HEADERS");

        List<String> origins = allowedOrigins != null ?
            Arrays.asList(allowedOrigins.split(",")) :
            List.of("*");

        List<String> methods = allowedMethods != null ?
            Arrays.asList(allowedMethods.split(",")) :
            List.of("GET", "POST", "PUT", "DELETE", "OPTIONS");

        List<String> headers = allowedHeaders != null ?
            Arrays.asList(allowedHeaders.split(",")) :
            List.of("Content-Type", "Authorization");

        return new Cors(origins, methods, headers, true);
    }
}
```

### Logging Configuration

```java
public class LoggingConfig {
    public static Morgan createMorganMiddleware() {
        String env = Dotenv.get("APP_ENV");
        String logFormat = Dotenv.get("LOG_FORMAT");

        if (logFormat != null) {
            return new Morgan(logFormat);
        }

        // Use different formats based on environment
        if ("production".equals(env)) {
            return new Morgan("combined");
        } else {
            return new Morgan("dev");
        }
    }
}
```

### Custom Configuration Class

```java
public class Config {
    // Server settings
    public static final int PORT = getIntEnv("PORT", 3000);
    public static final String HOST = getStringEnv("HOST", "localhost");

    // Application settings
    public static final String APP_NAME = getStringEnv("APP_NAME", "Heo App");
    public static final String APP_ENV = getStringEnv("APP_ENV", "development");
    public static final boolean DEBUG = getBooleanEnv("DEBUG", true);

    // Security settings
    public static final String SECRET_KEY = getStringEnv("SECRET_KEY", "default-secret");
    public static final String JWT_SECRET = getStringEnv("JWT_SECRET", "jwt-secret");

    // Helper methods
    private static String getStringEnv(String key, String defaultValue) {
        String value = Dotenv.get(key);
        return value != null ? value : defaultValue;
    }

    private static int getIntEnv(String key, int defaultValue) {
        String value = Dotenv.get(key);
        return value != null ? Integer.parseInt(value) : defaultValue;
    }

    private static boolean getBooleanEnv(String key, boolean defaultValue) {
        String value = Dotenv.get(key);
        return value != null ? Boolean.parseBoolean(value) : defaultValue;
    }

    // Environment checks
    public static boolean isProduction() {
        return "production".equals(APP_ENV);
    }

    public static boolean isDevelopment() {
        return "development".equals(APP_ENV);
    }

    public static boolean isDebug() {
        return DEBUG && isDevelopment();
    }
}
```

### Using the Configuration

```java
public class Main {
    public static void main(String[] args) {
        // Load environment
        Dotenv.config();

        Heo app = new Heo();

        // Use configuration
        if (Config.isDebug()) {
            app.use(LoggingConfig.createMorganMiddleware());
        }

        app.use(new Json());
        app.use(CorsConfig.createCorsMiddleware());

        app.get("/config", (req, res, next) -> {
            res.json(Map.of(
                "appName", Config.APP_NAME,
                "environment", Config.APP_ENV,
                "debug", Config.DEBUG
            ));
        });

        app.listen(Config.PORT, () -> {
            System.out.println("🚀 " + Config.APP_NAME + " running on " +
                             Config.HOST + ":" + Config.PORT);
            System.out.println("Environment: " + Config.APP_ENV);
        });
    }
}
```

## Best Practices

1. **Never commit `.env` files** - Add `.env` to your `.gitignore`
2. **Use environment-specific defaults** - Provide sensible defaults for development
3. **Validate configuration** - Check required environment variables at startup
4. **Document configuration** - Maintain a `.env.example` file
5. **Use type-safe configuration** - Create a configuration class with proper types

## Next Steps

- [Directory Structure](structure.md) - Learn about organizing your Heo application
- [Routing](routing.md) - Define routes for your application
- [Middleware](middleware.md) - Add middleware to your application
