package heo.server;


import heo.exception.MethodNotAllowError;
import heo.exception.NotFoundError;
import heo.http.HttpMethod;
import heo.http.HttpStatusCode;
import heo.http.Request;
import heo.http.Response;
import heo.interfaces.ErrorHandler;
import heo.interfaces.Middleware;
import heo.interfaces.RouterHandler;
import heo.middleware.Json;
import heo.middleware.MiddlewareChain;
import heo.middleware.Urlencoded;
import heo.router.Route;
import heo.router.Router;

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * File: Heo.java
 * Description: Heo is a simple HTTP server framework that allows you to create web applications easily.
 * Author: 102004tan
 * Created: 25/07/2025
 * Updated: 04/08/2025
 */
public class Heo implements RouterHandler {
    private ErrorHandler errorHandler;
    private Json json;
    private Urlencoded urlencoded;
    private Router router;

    public static Router Router(){
        return new Router();
    }


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

    public Heo(){
        this.router = new Router();
    }

    public void use(ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }


    public void listen(int port) {
        try(ServerSocket serverSocket = new ServerSocket(port);) {
            while (true) {
                try {
                    ExecutorService pool = Executors.newFixedThreadPool(100);
//                    Socket socket = serverSocket.accept();
//                    new Thread(() -> {
//                        try {
//                            handle(socket);
//                        } catch (IOException e) {
//                            throw new RuntimeException(e);
//                        }
//                    }).start();
                    Socket socket = serverSocket.accept();
                    pool.submit(() -> handle(socket));
                    socket.setSoTimeout(30000);
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

    private void handle(Socket socket){
        Response res = new Response(socket);
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            try {
                String requestLine  = in.readLine();
                if (requestLine  == null || requestLine .isEmpty()) {
                    in.close();
                    return;
                }
                String[] parts = requestLine .split(" ");
                String method = parts[0];
                String path = parts[1];


                /*
                Query
                */
                Map<String,String> queryParams = new HashMap<>();
                if (path.contains("?") && method.equals(HttpMethod.GET)) {
                    String[] pathParts = path.split("\\?");
                    path = pathParts[0];
                    String queryString = pathParts[1];
                    for (String param : queryString.split("&")) {
                        String[] keyValue = param.split("=");
                        if (keyValue.length == 2) {
                            queryParams.put(keyValue[0], keyValue[1]);
                        }
                    }
                }

                Request req = new Request(method, path);
                req.setQuery(queryParams);
                String ipAddress = socket.getInetAddress().getHostAddress();
                req.setIpAddress(ipAddress);

                // Header
                String line;
                while ((line = in.readLine()) != null && !line.isEmpty()) {
                    int separator = line.indexOf(":");
                    if (separator != -1) {
                        String key = line.substring(0, separator).trim();
                        String value = line.substring(separator + 1).trim();
                        if (key.equalsIgnoreCase("Content-Type") && value.equalsIgnoreCase("application/json")) {
                            req.setHeaders("Content-Type", "application/json");
                        } else if (key.equalsIgnoreCase("Content-Type") && value.equalsIgnoreCase("application/x-www-form-urlencoded")) {
                            req.setHeaders("Content-Type", "application/x-www-form-urlencoded");
                        } else {
                            req.setHeaders(key, value);
                        }
                    }
                }

//                int contentLength = 0;
//                while ((line = in.readLine()) != null && !line.isEmpty()) {
//                    if (line.toLowerCase().startsWith("content-length:")) {
//                        contentLength = Integer.parseInt(line.substring("content-length:".length()).trim());
//                    }
//                }
//                char[] bodyChars = new char[contentLength];
//                int read = in.read(bodyChars, 0, contentLength);
//                String body = new String(bodyChars, 0, read);
//                req.setRawBody(body);
//                req.setHeaders("Content-Type", "application/json");

                StringBuilder bodyBuilder = new StringBuilder();
                while (in.ready()) {
                    bodyBuilder.append((char) in.read());
                }
                String body = bodyBuilder.toString();
                req.setRawBody(body);



                Route routeFound = router.search(path,method);
                if (routeFound != null){
                    System.out.println("Has params: " + routeFound.hasParams(method));
                    if (routeFound.hasParams(method)){
                        Map<Integer,String> params = routeFound.getParams(method);
                        Map<String,String> paramMap = new HashMap<>();
                        for (Map.Entry<Integer, String> entry : params.entrySet()) {
                            int index = entry.getKey();
                            String key = entry.getValue();
                            String value = path.split("/")[index];
                            paramMap.put(key, value);
                        }
                        System.out.println("Params: " + paramMap);
                        req.setParams(paramMap);
                    }

                    MiddlewareChain chain = new MiddlewareChain(routeFound.getMiddlewares(method),errorHandler);
                    chain.next(req,res);
                }
                in.close();
            } catch (IOException e) {
                System.err.println("Error reading request: " + e.getMessage());
                res.send("400 Bad Request");
            }
        } catch (Exception e) {
            if (e instanceof MethodNotAllowError){
                res.status(HttpStatusCode.METHOD_NOT_ALLOWED).send("Method not allow");
            }else if (e instanceof NotFoundError){
                res.status(HttpStatusCode.NOT_FOUND).send(e.getMessage());
            }
        }
    }

    @Override
    public void use(Router router) {
        this.router.use(router);
    }

    @Override
    public void use(String path, Router router) {
        this.router.use(path,router);
    }

    @Override
    public void use(Middleware middleware) {
        this.router.use(middleware);
    }

    @Override
    public void use(Middleware... middlewares) {
        this.router.use(middlewares);
    }

    @Override
    public void use(String path, Middleware... middlewares) {
        this.router.use(path,middlewares);
    }

    @Override
    public void get(String path, Middleware... middlewares) {
        this.router.get(path,middlewares);
    }

    @Override
    public void post(String path, Middleware... middlewares) {
        this.router.post(path,middlewares);
    }

    @Override
    public void put(String path, Middleware... middlewares) {
        this.router.put(path,middlewares);
    }

    @Override
    public void patch(String path, Middleware... middlewares) {
        this.router.patch(path,middlewares);
    }

    @Override
    public void delete(String path, Middleware... middlewares) {
        this.router.delete(path,middlewares);
    }
}