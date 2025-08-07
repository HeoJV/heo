package heo.router;

import heo.exception.MethodNotAllowError;
import heo.exception.NotFoundError;
import heo.http.HttpMethod;
import heo.interfaces.Middleware;
import heo.interfaces.RouterHandler;

import java.util.*;

/**
 * File: Router.java
 * Description: Router class for handling routes and middleware in a web application.
 * Author: 102004tan
 * Created: 25/07/2025
 * Updated: 04/08/2025
 */
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
            if (key.equals("/")) {
                finalCurrent.setMiddlewares(route.getMiddlewares());
                finalCurrent.setEndpoint(route.isEndpoint());
            }
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
        Route current = this.root;
        String[] parts = path.split("/");
        boolean isNew = false;
        Map<Integer,String> params = new HashMap<>();
        if (path.isEmpty() || path.equals("/")) {
            Route newRoute = new Route();
            newRoute.setEndpoint(true);
            newRoute.setMiddlewares(method, List.of(middlewares));
            this.root.getChildren().put("/", newRoute);
            return;
        }
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (part.isEmpty()) {
                continue;
            }
            if (part.startsWith(":")) {
                String keyParam = part.substring(1);
                params.put(i, keyParam);
            }
            if (current.getChildren().containsKey(part)) {
                current = current.getChildren().get(part);
            }else if (
                current.getChildren().keySet().stream().anyMatch(key -> key.startsWith(":"))
            ){
                boolean isDynamic = current.getChildren().keySet().stream()
                        .anyMatch(key -> key.startsWith(":") && key.substring(1).equals(part));
                if (!isDynamic) {
                    return;
                }
                current = current.getChildren().entrySet().stream()
                        .filter(entry -> entry.getKey().startsWith(":"))
                        .findFirst()
                        .map(Map.Entry::getValue)
                        .orElse(null);
                if (current == null) {
                    throw new NotFoundError("Dynamic route not found for part: " + part);
                }
            }else {
                isNew = true;
                Route newRoute = new Route();
                if (i == parts.length - 1) {
                    newRoute.setEndpoint(true);
                }
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
        current.setParams(params,method);
    }

    public Route search(String path,String method){
        Route current = root;
        if (path.isEmpty() || path.equals("/")) {
            if (current.getChildren().containsKey("/")) {
                current = current.getChildren().get("/");
            } else {
                throw new NotFoundError("Cannot " + method + " " + path);
            }
        }
        String[] parts = path.split("/");
        for (int i = 0 ; i < parts.length; i++) {
            String part = parts[i];
            if (part.isEmpty()) {
                continue;
            }
            assert current != null;
            Map<String,Route> children = current.getChildren();
            if (children.containsKey(part)) {
                current = current.getChildren().get(part);
            }
            else if (
                children.keySet().stream().anyMatch(key -> key.startsWith(":"))
            ){
                current = children.entrySet().stream()
                        .filter(entry -> entry.getKey().startsWith(":"))
                        .findFirst()
                        .map(Map.Entry::getValue)
                        .orElse(null);
            }
            else{
                current = null;
            }
        }

        if (current == null || !current.isEndpoint()) {
            throw new NotFoundError("Cannot "+method + " "+path);
        }


        if (!current.isMethodSupported(method)) {
            throw new MethodNotAllowError("Cannot "+method + " " + path);
        }

        return current;

    }
}