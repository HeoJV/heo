// Source code is decompiled from a .class file using FernFlower decompiler.
package heo.middleware;

import com.fasterxml.jackson.databind.ObjectMapper;
import heo.http.Request;
import heo.http.Response;
import heo.interfaces.Middleware;
import java.util.Map;

public class Json implements Middleware {
   private static final String JSON_CONTENT_TYPE = "application/json";
   private static final String JSON_ERROR_MESSAGE = "Invalid JSON format";
   private final ObjectMapper objectMapper = new ObjectMapper();

   public Json() {
   }

   public void handle(Request req, Response res, MiddlewareChain next) throws Exception {
      String rawBody = req.getRawBody();
      System.out.println("Raw body: " + rawBody);
      if (req.getMethod().equals("GET")) {
         next.next(req, res);
      }

      if (JSON_CONTENT_TYPE.equals(req.getHeader("Content-Type"))) {
         try {
            req.setBody(this.objectMapper.readValue(rawBody, Map.class));
         } catch (Exception e) {
            res.status(400).send(JSON_ERROR_MESSAGE);
            return;
         }
         next.next(req, res);
      } else {
         res.status(415).send("Unsupported Media Type");
      }
   }
}
