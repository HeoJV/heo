package heo.router;

import heo.interfaces.Middleware;

import java.util.*;

public class Router {
    private String path = "/";
    private final List<Route> routes = new ArrayList<>();
    private final Map<String, List<Middleware>> pathMiddlewares = new HashMap<>();


    public List<Route> getRoutes() {
        return Collections.unmodifiableList(routes);
    }

    public Map<String, List<Middleware>> getPathMiddlewares() {
        return Collections.unmodifiableMap(pathMiddlewares);
    }



    public void get(String path,Middleware ... mws){
        addRoute("GET", path, mws);
    }

    public void use(String path, Middleware... middlewares) {
        if (this.pathMiddlewares.containsKey(path)) {
            this.pathMiddlewares.get(path).addAll(Arrays.asList(middlewares));
        } else {
            this.pathMiddlewares.put(path, new ArrayList<>(Arrays.asList(middlewares)));
        }
        List<Route> foundRoutes = findRoutesByPath(path);
        if (!foundRoutes.isEmpty()) {
            for (Route route : foundRoutes) {
                route.middlewares.addAll(Arrays.asList(middlewares));
            }
        }
    }

    public void use(Middleware... middlewares) {
        if (this.pathMiddlewares.containsKey("/")) {
            this.pathMiddlewares.get("/").addAll(Arrays.asList(middlewares));
        } else {
            this.pathMiddlewares.put("/", new ArrayList<>(Arrays.asList(middlewares)));
        }
        List<Route> foundRoutes = findRoutesByPath("/");
        if (!foundRoutes.isEmpty()) {
            for (Route route : foundRoutes) {
                route.middlewares.addAll(Arrays.asList(middlewares));
            }
        }
    }

    public void use(Router router){
        this.routes.addAll(router.getRoutes());
        for (Map.Entry<String, List<Middleware>> entry : router.getPathMiddlewares().entrySet()) {
            String path = entry.getKey();
            List<Middleware> middlewares = entry.getValue();
            if (this.pathMiddlewares.containsKey(path)) {
                this.pathMiddlewares.get(path).addAll(middlewares);
            } else {
                this.pathMiddlewares.put(path, new ArrayList<>(middlewares));
            }
        }
    }

    public void use(String path,Router router){
        for (Route route : router.getRoutes()) {
            String fullPath = path + route.path;
            List<Middleware> allMiddlewares = new ArrayList<>(route.middlewares);
            List<Middleware> pathMiddlewares = findMiddlewaresByPath(fullPath);
            allMiddlewares.addAll(pathMiddlewares);
            this.routes.add(new Route(route.method, fullPath, allMiddlewares));
        }
        for (Map.Entry<String, List<Middleware>> entry : router.getPathMiddlewares().entrySet()) {
            String fullPath = path + entry.getKey();
            List<Middleware> middlewares = entry.getValue();
            if (this.pathMiddlewares.containsKey(fullPath)) {
                this.pathMiddlewares.get(fullPath).addAll(middlewares);
            } else {
                this.pathMiddlewares.put(fullPath, new ArrayList<>(middlewares));
            }
        }
    }

    private List<Route> findRoutesByPath(String path) {
        List<Route> matchedRoutes = new ArrayList<>();
        String[] pathParts = path.split("/");
        for (Route route : this.routes) {
            String[] routeParts = route.path.split("/");
            if (routeParts.length >= pathParts.length) {
                for (int i = 0; i < pathParts.length && Objects.equals(routeParts[i], pathParts[i]); i++) {
                    if (i == pathParts.length - 1) {
                        matchedRoutes.add(route);
                    }
                }
            }
        }
        return matchedRoutes;
    }

    private void addRoute(String method, String path, Middleware... mws) {
        List<Middleware> all = new ArrayList<>();
        List<Middleware> pathMws = findMiddlewaresByPath(path);
        System.out.println("Path Middlewares: " + pathMws.size());
        all.addAll(pathMws);
        all.addAll(Arrays.asList(mws));
        System.out.println("All Middlewares: " + all.size());
        this.routes.add(new Route(method, path, all));
    }

    private List<Middleware> findMiddlewaresByPath(String path) {
        String[] pathParts = path.split("/");
        StringBuilder currentPath = new StringBuilder();
        List<Middleware> matchedMiddlewares = new ArrayList<>(this.pathMiddlewares.getOrDefault("/", new ArrayList<>()));
        for (String pathPart : pathParts) {
            if (!pathPart.isEmpty()) {
                currentPath.append("/").append(pathPart);
                System.out.println("Current Path: " + String.valueOf(currentPath));
                if (this.pathMiddlewares.containsKey(currentPath.toString())) {
                    matchedMiddlewares.addAll(this.pathMiddlewares.get(currentPath.toString()));
                }
            }
        }
        return matchedMiddlewares;
    }
}
