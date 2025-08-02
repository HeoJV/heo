package heo.interfaces;

import heo.router.Router;

public interface RouterHandler {
    void use(Router router);
    void use(String path,Router router);
    void use(Middleware middleware);
    void use(Middleware ... middlewares);
    void use(String path, Middleware... middlewares);
    void get(String path, Middleware ... middlewares);
    void post(String path, Middleware ...middlewares);
}
