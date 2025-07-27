package heo;
import heo.core.Console;
import heo.router.Router;
import heo.server.Heo;

import java.io.IOException;
import java.util.Map;


/**
 * Main class to start the Heo server.
 */
public class Main {
    public static void main(String[] args) {
        Heo heo = new Heo();

        Router ApiRouter = new Router();
        Router BlogRouter = new Router();
        BlogRouter.get("/blogs", (req, res, next) -> {
            res.json(Map.of("message", "List of blogs"));
        });

        ApiRouter.use(BlogRouter);
        heo.use("/api/v1", ApiRouter);

        heo.listen(5000, () -> {
            Console.log("Server is running on port 5000");
        });
    }
}
