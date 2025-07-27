package heo.server;


import heo.http.HttpMethod;
import heo.http.Request;
import heo.http.Response;
import heo.interfaces.ErrorHandler;
import heo.interfaces.Middleware;
import heo.middleware.Json;
import heo.middleware.MiddlewareChain;
import heo.middleware.Urlencoded;
import heo.router.Route;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Heo server class that handles HTTP requests and routes.
 * This class allows you to define routes, use middlewares, and handle requests.
 * @author 102004tan
 * @version 1.0
 */
public class Heo {
    private final List<Route> routes = new ArrayList<>();
    private final Map<String, List<Middleware>> pathMiddlewares = new HashMap<>();
    private ErrorHandler errorHandler;
    private Json json;
    private Urlencoded urlencoded;

    public Json json() {
        if (this.json == null) {
            this.json = new Json();
        }
        return this.json;
    }

    public Urlencoded urlencoded() {
        if (this.urlencoded == null) {
            this.urlencoded = new Urlencoded();
        }
        return this.urlencoded;
    }

    public void use(ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

    public void use(Middleware middleware) {
        if (this.pathMiddlewares.containsKey("/")) {
            this.pathMiddlewares.get("/").add(middleware);
        } else {
            this.pathMiddlewares.put("/", new ArrayList<>(Collections.singletonList(middleware)));
        }
    }

    public void use(String path, Middleware... middlewares) {
        this.pathMiddlewares.put(path, new ArrayList<>(Arrays.asList(middlewares)));
        List<Route> foundRoutes = findRoutesByPath(path);
        if (!foundRoutes.isEmpty()) {
            for (Route route : foundRoutes) {
                route.middlewares.addAll(Arrays.asList(middlewares));
            }
        }
    }

    public void get(String path, Middleware... mws) {
        addRoute(HttpMethod.GET, path, mws);
    }

    public void post(String path, Middleware... mws) {
        addRoute(HttpMethod.POST, path, mws);
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

    public void listen(int port) {
        try(ServerSocket serverSocket = new ServerSocket(port);) {
            while (true) {
                try {
                    Socket socket = serverSocket.accept();
                    new Thread(() -> {
                        try {
                            handle(socket);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }).start();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void listen(int port, Runnable callback) {
        callback.run();
        listen(port);
    }

    private void handle(Socket socket) throws IOException {
        Response res = new Response(socket);
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            try {
                String line = in.readLine();
                if (line == null || line.isEmpty()) {
                    in.close();
                    return;
                }
                String[] parts = line.split(" ");
                String method = parts[0];
                String path = parts[1];
                Request req = new Request(method, path);
                String ipAddress = socket.getInetAddress().getHostAddress();
                req.setIpAddress(ipAddress);
                int contentLength = 0;
                while ((line = in.readLine()) != null && !line.isEmpty()) {
                    if (line.toLowerCase().startsWith("content-length:")) {
                        contentLength = Integer.parseInt(line.substring("content-length:".length()).trim());
                    }
                }
                char[] bodyChars = new char[contentLength];
                int read = in.read(bodyChars, 0, contentLength);
                String body = new String(bodyChars);
                System.out.println("Body: " + body);
                req.setRawBody(body);
                req.setHeaders("Content-Type", "application/json");
                if (isNotFound(method, path)) {
                    res.send("404 Not Found: " + path);
                    in.close();
                    return;
                }
                for (Route route : this.routes) {
                    if (route.method.equals(method) && route.path.equals(path)) {
                        MiddlewareChain chain = new MiddlewareChain(route.middlewares, this.errorHandler);
                        chain.next(req, res);
                        in.close();
                        return;
                    }
                }
                in.close();
            } catch (IOException e) {
                System.err.println("Error reading request: " + e.getMessage());
                res.send("400 Bad Request");
            }
        } catch (Exception e) {
            System.err.println("Error handling request: " + e.getMessage());
        }
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

    private boolean isNotFound(String method, String path) {
        for (Route route : this.routes) {
            if (route.method.equals(method) && route.path.equals(path)) {
                return false;
            }
        }
        return true;
    }
}