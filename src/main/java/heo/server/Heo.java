package heo.server;


import heo.exception.MethodNotAllowError;
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
 * Heo server class that handles HTTP requests and routes.
 * This class allows you to define routes, use middlewares, and handle requests.
 * @author 102004tan
 * @version 1.0
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
                String body = new String(bodyChars);
                req.setRawBody(body);
                req.setHeaders("Content-Type", "application/json");

                Route routeFound = router.search(path,method);
                if (routeFound != null){
                    MiddlewareChain chain = new MiddlewareChain(routeFound.getMiddlewares(method),errorHandler);
                    chain.next(req,res);
                }
                else{
                    res.status(404).send("404");
                }

                in.close();
            } catch (IOException e) {
                System.err.println("Error reading request: " + e.getMessage());
                res.send("400 Bad Request");
            }
        } catch (Exception e) {
            if (e instanceof MethodNotAllowError){
                res.status(HttpStatusCode.METHOD_NOT_ALLOWED).send("Method not allow");
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
}