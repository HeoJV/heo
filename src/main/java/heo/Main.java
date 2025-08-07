package heo;
import heo.core.Console;
import heo.exception.NotFoundError;
import heo.http.Request;
import heo.http.Response;
import heo.interfaces.ErrorHandler;
import heo.middleware.Morgan;

import java.util.Map;


/**
 * Main class to start the Heo server.
 */
public class Main {
    public static void main(String[] args) {
        Heo heo = new Heo();
        int port = 4000;
        heo.use(new Morgan());
        heo.use(new ErrorHandlerMw());

        heo.get("/", (req,res,next) -> {
            throw new NotFoundError("Not Found");
        });

        heo.get("/test-change", (req,res,next) -> {
            res.json(Map.of("message", "Hello World test"));
        });


        heo.listen(port,()->{
            Console.log("Server is listening in port ",port);
        });
    }
}

class ErrorHandlerMw implements ErrorHandler{

    @Override
    public void handle(Throwable e, Request req, Response res) throws Exception {
        res.status(res.getStatus()).json(
                Map.of(
                        "message", "An error occurred",
                        "error", e.getMessage(),
                        "statusCode", res.getStatus()
                )
        );
    }
}
