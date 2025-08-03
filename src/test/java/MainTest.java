import heo.core.Console;
import heo.http.Request;
import heo.http.Response;
import heo.interfaces.Middleware;
import heo.middleware.MiddlewareChain;
import heo.router.Route;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainTest {
    public static void main(String[]args){
      Router router = new Router();

      router.addRoute("POST","/api/v1/auth/login",new LogMiddleware(),(req,res,next)->{
          Console.log("[POST]","/api/v1/auth/login");
      });
      router.addRoute("POST","/api/v1/auth/signup",new LogMiddleware(),(req,res,next)->{
            Console.log("[POST]","/api/v1/auth/post");
      });
    }
}

class Node{
    String label; // "api"
    List<Middleware> middlewares = new ArrayList<>();
    String method; // GET,POST
    Map<String,Node> children = new HashMap<>(); // Node v1,Node v2

}

class Router{
    Node root = new Node();
    /**
     /api/v1/auth/login
     */
    public void addRoute(String method,String path,Middleware ... middlewares){
        String[] parts = path.split("/");
        Node current = root;

        for (int i = 0 ; i < parts.length; i++){
            String segment = parts[i];
            Node next = null;

            for (Node child : current.children.values()){
                if (child.label.equals(segment) || method.equals(child.method)){
                    next = child;
                    break;
                }
            }

            if (next == null){
                Console.log("Node ",segment," chua co, tao ngay bay gio");
                next = new Node();
                next.label = segment;
                current.children.put(segment,next);
            }

            current = next;
        }
        current.middlewares = List.of(middlewares);
    }

    public void addMiddleware(Middleware middleware){
        root.middlewares.add(middleware);
    }

    public void addMiddleware(String path,Middleware ... middlewares){
        String[] parts = path.split("/");
        Node current = root;
        // /api, /api/v1
        for (int i = 0 ; i < parts.length; i++){
            String segment = parts[i];
            Node next = null;

            for (Node child : current.children.values()){
                if (child.label.equals(segment)){
                    next = child;
                    break;
                }
            }

            if (next == null){
                next = new Node();
                next.label = segment;
                current.children.put(segment,next);
            }

            current = next;
        }
        current.middlewares.addAll(List.of(middlewares));
    }
}

class LogMiddleware implements  Middleware{

    @Override
    public void handle(Request var1, Response var2, MiddlewareChain var3) throws Exception {
        Console.log("LOG mw");
    }
}
