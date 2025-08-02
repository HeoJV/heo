package heo.router;

import heo.core.Console;
import heo.exception.MethodNotAllowError;
import heo.interfaces.Middleware;
import heo.interfaces.RouterHandler;

import java.util.*;

public class Router implements RouterHandler {
    private Route root;
    private final Map<String,List<Middleware>> globalMiddlewares = new HashMap<>();
    public Route getRoot() {
        return root;
    }
    public Router() {
        this.root = new Route();
    }

    public void use(Router router){
        use("/", router);
    }

    public void use(String path,Router router){
        System.out.println("Path: " + path);
        if (router == null || router.getRoot() == null) {
            return;
        }
        System.out.println("Router child size "+router.getRoot().getChildren().size());
        router.getRoot().getChildren().forEach((key, route) -> {
            if (!globalMiddlewares.containsKey(key)) {
                globalMiddlewares.put(key,router.globalMiddlewares.getOrDefault(key, List.of()));
            }else{
                globalMiddlewares.get(key).addAll(router.globalMiddlewares.getOrDefault(key, List.of()));
            }
        });
        System.out.println("Router child size 2"+router.getRoot().getChildren().size());
        Route current = this.root;
        String[] parts = path.split("/");
        // add middlewares from router to globalMiddlewares
        System.out.println("this.root == router.getRoot(): " + (this.root == router.getRoot()));

        // create node from path
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (current.getChildren().containsKey(part)) {
                current = current.getChildren().get(part);
            } else {
                Route newRoute = new Route();
                current.getChildren().put(part, newRoute);
                current = newRoute;
            }
        }

        // add node from router to current
        System.out.println("Current : "+ current.getChildren().size());
        System.out.println("router : "+ router.getRoot().getChildren().size());
        router.getRoot().getChildren().forEach((s, route) -> {
            System.out.println("Keyyy::"+s);
        });
        Route finalCurrent = current;
        router.getRoot().getChildren().forEach((key, route)->{
            System.out.println("Key "+key);
            if (!finalCurrent.getChildren().containsKey(key)){
                System.out.println("key "+key);
                finalCurrent.getChildren().put(key, route);
            }
        });
    }

    public void use(Middleware middleware) {
        use("/", middleware);
    }

    public void use(Middleware... middlewares) {
        use("/", middlewares);
    }

    public void use(String path, Middleware... middlewares) {
        globalMiddlewares.put(path, List.of(middlewares));
    }

    public void get(String path, Middleware ... middlewares) {
        addRoute("GET", path, middlewares);
    }

    public void post(String path, Middleware ...middlewares) {
        addRoute("POST", path, middlewares);
    }
    /**
     * app.get("/path", (request, response, next) -> {})
     */

    private void addRoute(String method,String path,Middleware ...middlewares){
        Route current = this.root;
        String[] parts = path.split("/");
        boolean isNew = false;
        for (String part: parts){
            if (part.isEmpty()) {
                continue;
            }
            if (current.getChildren().containsKey(part)) {
                current = current.getChildren().get(part);
            } else {
                System.out.println("Creating new route for part: " + part +" isParameterized: " + part.startsWith(":"));
                isNew = true;
                Route newRoute = new Route();
                newRoute.setParameterized(part.startsWith(":"));
                current.getChildren().put(part, newRoute);
                current = newRoute;
            }
        }

        if (!isNew && current.isMethodSupported(method)) {
            return;
        }
        List<Middleware> combined = new ArrayList<>();
        globalMiddlewares.forEach((pathKey, middlewareList) -> {
            if (pathKey.equals("/") || path.startsWith(pathKey)) {
                combined.addAll(middlewareList);
            }
        });
        combined.addAll(List.of(middlewares));
        current.setMiddlewares(method, combined);
    }

    public Route search(String path,String method){
        Route current = root;
        String[] parts = path.split("/");
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            assert current != null;
            Console.log("length ",current.getChildren().size());
            Map<String,Route> children = current.getChildren();
            if (children.containsKey(part)) {
                current = current.getChildren().get(part);
            }
            else if (children.values().stream().anyMatch(Route::isParameterized)){
                current = current.getChildren().values().stream()
                        .filter(Route::isParameterized)
                        .findFirst()
                        .orElse(null);
            }
            else{
                return null;
            }
        }

        assert current != null;
        if (!current.isMethodSupported(method)){
            throw new MethodNotAllowError("Method not allow");
        }

        return current;
    }
}