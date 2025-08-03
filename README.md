# Heo - A Mini Java HTTP Framework 🐷
<p align="center">
  <img src="logo.png" alt="Heo Logo" width="150"/>
</p>

**Heo** is a lightweight and minimal HTTP server framework for Java Core, inspired by the simplicity and power of [Express.js](https://expressjs.com/). It is designed for learning, prototyping, and small web server applications without using any third-party libraries.

---

## ✨ Features

- Express-style API: `app.use()`, `app.get()`, `app.post()`, `app.listen()`, etc.
- Middleware support (like Express).
- Simple routing system.
- Built using Java Core (`Socket`, `InputStream`, `Thread`).
- No external dependencies.

---

## 🏁 Getting Started

### 1. Clone the project

```bash
git clone https://github.com/102004tan/heo.git
cd heo
```
### 2. Run the server
```bash
javac -d out src/**/*.java
java -cp out heo.MainTest
```

### 3. Example Usage
```java
public class MainTest {
    public static void main(String[] args){
        Dotenv.config();
        Heo heo = new Heo();
        int port = Dotenv.get("PORT") != null ? Integer.parseInt(Dotenv.get("PORT")) : 3000;

        heo.get("/api/test",(req,res,next)->{
            res.send("Hello, Heo!");
        });

        heo.listen(port, () -> {
            System.out.println("Server is running on port " + port);
        });
    }
}
```



