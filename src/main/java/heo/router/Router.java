package heo.router;

import heo.core.Console;
import heo.exception.MethodNotAllowError;
import heo.http.HttpMethod;
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
        path = path.isEmpty() ? "/" : path;
        if (router == null || router.getRoot() == null) {
            return;
        }
        router.getRoot().getChildren().forEach((key, route) -> {
            if (!globalMiddlewares.containsKey(key)) {
                globalMiddlewares.put(key,router.globalMiddlewares.getOrDefault(key, List.of()));
            }else{
                globalMiddlewares.get(key).addAll(router.globalMiddlewares.getOrDefault(key, List.of()));
            }
        });
        Route current = this.root;
        String[] parts = path.split("/");

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

        Route finalCurrent = current;
        router.getRoot().getChildren().forEach((key, route)->{
            if (!finalCurrent.getChildren().containsKey(key)){
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
        addRoute(HttpMethod.GET, path, middlewares);
    }

    public void post(String path, Middleware ...middlewares) {
        addRoute(HttpMethod.POST, path, middlewares);
    }

    @Override
    public void put(String path, Middleware... middlewares) {
        addRoute(HttpMethod.PUT, path, middlewares);
    }

    @Override
    public void patch(String path, Middleware... middlewares) {
        addRoute(HttpMethod.PATCH, path, middlewares);
    }

    @Override
    public void delete(String path, Middleware... middlewares) {
        addRoute(HttpMethod.DELETE, path, middlewares);
    }

    /**
     * app.get("/path", (request, response, next) -> {})
     */

    private void addRoute(String method,String path,Middleware ...middlewares){
        System.out.println("Adding route: " + method + " " + path);
        Route current = this.root;
        String[] parts = path.split("/");
        boolean isNew = false;
        String keyParam = null;
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (part.isEmpty()) {
                continue;
            }
            if (part.startsWith(":")) {
                keyParam = part.substring(1);
            }
            if (current.getChildren().containsKey(part)) {
                current = current.getChildren().get(part);
            } else {
                System.out.println("Creating new route for part: " + part);
                isNew = true;
                Route newRoute = new Route();
                current.setParams(method, keyParam);
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
        System.out.println("Setting middlewares for route: " + method + " " + path+ " with middlewares: " + combined.size());
        current.setMiddlewares(method, combined);
        current.setParams(method, keyParam);
    }

    public Route search(String path,String method){
        Route current = root;
        String[] parts = path.split("/");
        System.out.println("Searching for path: " + path + " with method: " + method);
        String keyParam = null;
        for (int i = 0 ; i < parts.length; i++) {
            String part = parts[i];
            if (part.isEmpty()) {
                continue;
            }
            assert current != null;
            Map<String,Route> children = current.getChildren();
            System.out.println("part current: " + part);
            if (children.containsKey(part)) {
                current = current.getChildren().get(part);
            }
            else if (current.getChildren().values().stream().anyMatch(route -> route.isParameterized(method))){
                current = current.getChildren().values().stream()
                        .filter(route -> route.isParameterized(method))
                        .findFirst()
                        .orElse(null);
                assert current != null;
                System.out.println("Found parameterized route: " + current.getParam(method));
                if (current != null && current.getKeyParam() != null) {
                    keyParam = i+"-" + current.getParam(method);
                }
            }
            else{
                return null;
            }
        }

        assert current != null;
        if (!current.isMethodSupported(method)){
            throw new MethodNotAllowError("Method not allow");
        }

        current.setParams(method, keyParam);

        return current;
    }
}