package heo;
import heo.core.Console;
import heo.exception.BadRequest;
import heo.http.Request;
import heo.http.Response;
import heo.interfaces.ErrorHandler;
import heo.middleware.Morgan;
import heo.router.Router;
import heo.server.Heo;

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
