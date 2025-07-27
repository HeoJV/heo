package heo;

import heo.config.Dotenv;
import heo.exception.CREATED;
import heo.exception.OK;
import heo.http.HttpMethod;
import heo.http.Request;
import heo.http.Response;
import heo.interfaces.ErrorHandler;
import heo.middleware.Cors;
import heo.middleware.MiddlewareChain;
import heo.middleware.Morgan;
import heo.server.Heo;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Main class to start the Heo server.
 */
public class Main {
    public static void main(String[] args) throws IOException {
        Dotenv.config();
        Heo heo = new Heo();
        int port = Dotenv.get("PORT") != null ? Integer.parseInt(Dotenv.get("PORT")) : 3000;
        ApiController apiController = new ApiController();
        heo.use(heo.json());

        heo.get("/api/test",apiController::info);

        heo.post("/api/test", (req, res, next) -> {
            res.send("Post request received!"+req.getBody().get("name"));
        });

        heo.use((ErrorHandler) (e, req, res) -> res.json(Map.of(
            "error", e.getMessage(),
            "status", 500
        )));

        heo.listen(port, () -> {
            System.out.println("Server is running on port " + port);
        });
    }
}

class ApiController {
    public void info(Request req, Response res, MiddlewareChain next){
        new OK(
                "Heo API",
                ApiService.getAll()
        ).send(res);
    }
}

class ApiService {
    public static List<Map<String,String>> getAll() {
        return List.of(
            Map.of("name", "Heo", "version", "1.0.0"),
            Map.of("name", "Heo Server", "version", "1.0.0"),
            Map.of("name", "Heo Middleware", "version", "1.0.0"),
            Map.of("name", "Heo Router", "version", "1.0.0")
        );
    }
}