import heo.core.Console;
import heo.router.Router;
import heo.server.Heo;
import java.util.Map;

public class HelloWorld {
    public static void main(String[]args){
        Heo heo = new Heo();
        int port = 4000;

        Router apiRouter = Heo.Router();
        apiRouter.get("/",(req,res,next)->{
            res.json(Map.of("message", "Hello World"));
        });
        apiRouter.get("/:id",(req,res,next)->{
            res.json(Map.of("message", "Hello World id"));
        });
        apiRouter.get("/users/:userId",(req,res,next)->{
            String userId = req.params("userId");
            res.json(Map.of("message", "User ID is " + userId));
        });
        heo.use("/api/v1", apiRouter);

        heo.get("/api/v1",(req,res,next)->{
            res.json(Map.of("message", "API Version 1"));
        });

        heo.get("/",(req,res,next)->{
            res.send("Hello World");
        });

        heo.listen(port,()->{
            Console.log("Server is listening in port ",port);
        });
    }
}
