# Installation

- [System Requirements](#system-requirements)
- [Installing Heo](#installing-heo)
- [Maven Setup](#maven-setup)
- [Manual Setup](#manual-setup)
- [Verification](#verification)

## System Requirements

Heo has minimal system requirements. You only need:

- **Java 22** or higher
- **Maven** (optional, but recommended)

## Installing Heo

### Via Git Clone

The easiest way to get started with Heo is to clone the repository:

```bash
git clone https://github.com/102004tan/heo.git
cd heo
```

### Download ZIP

Alternatively, you can download the ZIP file from GitHub and extract it to your desired location.

## Maven Setup

If you have Maven installed, you can use it to manage dependencies and run the project:

### Compile the Project

```bash
mvn compile
```

### Run Example

```bash
mvn exec:java -Dexec.mainClass="examples.HelloWorld"
```

### Package as JAR

```bash
mvn package
```

## Manual Setup

If you prefer not to use Maven, you can compile and run Heo manually:

### Compile All Sources

```bash
# Create output directory
mkdir -p out

# Compile all Java files
javac -d out src/main/java/heo/**/*.java
```

### Compile with Dependencies

If you're using Jackson for JSON processing:

```bash
# Download Jackson JAR files manually or use Maven to copy dependencies
mvn dependency:copy-dependencies -DoutputDirectory=lib

# Compile with classpath
javac -cp "lib/*" -d out src/main/java/heo/**/*.java
```

### Run Your Application

```bash
# Run without dependencies
java -cp out YourMainClass

# Run with dependencies
java -cp "out:lib/*" YourMainClass
```

## Verification

To verify that Heo is working correctly, create a simple test file:

**HelloHeo.java**

```java
import heo.server.Heo;

public class HelloHeo {
    public static void main(String[] args) {
        Heo app = new Heo();

        app.get("/", (req, res, next) -> {
            res.send("Hello, Heo is working! 🐷");
        });

        app.listen(3000, () -> {
            System.out.println("🚀 Heo server running on http://localhost:3000");
        });
    }
}
```

Compile and run:

```bash
# If using Maven
mvn compile exec:java -Dexec.mainClass="HelloHeo"

# If manual compilation
javac -cp out HelloHeo.java
java -cp out:. HelloHeo
```

Visit `http://localhost:3000` in your browser. You should see "Hello, Heo is working! 🐷".

## IDE Setup

### IntelliJ IDEA

1. Open IntelliJ IDEA
2. Choose "Open" and select the Heo project folder
3. IntelliJ will automatically detect the Maven project
4. Wait for dependencies to download
5. You can now run examples directly from the IDE

### Eclipse

1. Open Eclipse
2. Go to File → Import → Existing Maven Projects
3. Select the Heo project folder
4. Eclipse will import the project with all dependencies

### VS Code

1. Install the Java Extension Pack
2. Open the Heo folder in VS Code
3. The Java extensions will automatically configure the project
4. Use Ctrl+Shift+P and run "Java: Compile Workspace"

## Next Steps

Now that you have Heo installed, you're ready to start building web applications! Continue with:

- [Configuration](configuration.md) - Learn how to configure your Heo application
- [Quick Start Guide](quickstart.md) - Build your first Heo application
- [Routing](routing.md) - Define routes for your application
