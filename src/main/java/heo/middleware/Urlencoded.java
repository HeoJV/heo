package heo.middleware;

import heo.http.Request;
import heo.http.Response;
import heo.interfaces.Middleware;

public class Urlencoded implements Middleware {
   public Urlencoded() {
   }

   public void handle(Request req, Response res, MiddlewareChain next) throws Exception {
      next.next(req, res);
   }
}
