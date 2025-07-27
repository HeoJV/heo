// Source code is decompiled from a .class file using FernFlower decompiler.
package heo.middleware;

import heo.http.Request;
import heo.http.Response;
import heo.interfaces.ErrorHandler;
import heo.interfaces.Middleware;
import java.util.List;

public class MiddlewareChain {
   private final List<Middleware> middlewares;
   private int index = 0;
   private final ErrorHandler errorHandler;

   public MiddlewareChain(List<Middleware> middlewares, ErrorHandler errorHandler) {
      this.middlewares = middlewares;
      this.errorHandler = errorHandler;
   }

   public void next(Request req, Response res) throws Exception {
      if (this.index < this.middlewares.size()) {
         Middleware current = this.middlewares.get(this.index++);
         try {
            current.handle(req, res, this);
         } catch (Exception var5) {
            this.next(req, res, var5);
         }
      }

   }

   public void next(Request req, Response res, Exception e) throws Exception {
      if (this.errorHandler != null) {
         this.errorHandler.handle(e, req, res);
      } else {
         throw e;
      }
   }
}
