// Source code is decompiled from a .class file using FernFlower decompiler.
package heo.middleware;

import heo.http.Request;
import heo.http.Response;
import heo.interfaces.Middleware;
import java.util.List;

public class Cors implements Middleware {
   private final List<String> allowedOrigins;
   private final List<String> allowedMethods;
   private final List<String> allowedHeaders;
   private final boolean allowCredentials;

   public Cors(List<String> allowedOrigins, List<String> allowedMethods, List<String> allowedHeaders, boolean allowCredentials) {
      this.allowedOrigins = allowedOrigins;
      this.allowedMethods = allowedMethods;
      this.allowedHeaders = allowedHeaders;
      this.allowCredentials = allowCredentials;
   }

   public void handle(Request req, Response res, MiddlewareChain next) throws Exception {
      System.out.println("[CORS] : " + req.path);
      String origin = req.getHeader("Origin");
      if (origin != null && (this.allowedOrigins.contains("*") || this.allowedOrigins.contains(origin))) {
         res.setHeader("Access-Control-Allow-Origin", origin);
         res.setHeader("Access-Control-Allow-Methods", String.join(", ", this.allowedMethods));
         res.setHeader("Access-Control-Allow-Headers", String.join(", ", this.allowedHeaders));
         if (this.allowCredentials) {
            res.setHeader("Access-Control-Allow-Credentials", "true");
         }
      }

      if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
         res.status(204).send("");
      } else {
         next.next(req, res);
      }

   }
}
