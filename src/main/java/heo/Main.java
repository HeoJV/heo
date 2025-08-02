package heo;
import heo.core.Console;
import heo.http.Request;
import heo.http.Response;
import heo.interfaces.ErrorHandler;
import heo.router.Router;
import heo.server.Heo;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


/**
 * Main class to start the Heo server.
 */
public class Main {
    public static void main(String[] args) {
        Heo heo = new Heo();
        int port = 4000;
        Router router = Heo.Router();
        heo.use(new ErrorHandlerMw());

        router.get("/:id",(req,res,next)->{
            res.send("router /:id");
        });

        heo.use("/api/v1/product",router);

        heo.get("/api/v1/product",(req,res,next)->{
            res.send("/api/v1/product");
        });

        heo.listen(port,()->{
            Console.log("Server is listening in port ",port);
        });
    }
}

class ErrorHandlerMw implements ErrorHandler{

    @Override
    public void handle(Throwable e, Request req, Response res) throws Exception {
        res.status(500).json(
                Map.of(
                        "message", "An error occurred",
                        "error", e.getMessage(),
                        "statusCode", 500
                )
        );
    }
}
