package heo.router;

import heo.interfaces.Middleware;
import java.util.List;

public class Route {
   public final String method;
   public final String path;
   public final List<Middleware> middlewares;

   public Route(String method, String path, List<Middleware> middlewares) {
      this.method = method;
      this.path = path;
      this.middlewares = middlewares;
   }
}

