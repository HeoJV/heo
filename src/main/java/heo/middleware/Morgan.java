// Source code is decompiled from a .class file using FernFlower decompiler.
package heo.middleware;

import heo.enums.MorganFormat;
import heo.http.Request;
import heo.http.Response;
import heo.interfaces.Middleware;

import java.text.Format;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class Morgan implements Middleware {

   private final MorganFormat format;

   public Morgan() {
      this.format = MorganFormat.DEV;
   }

   public Morgan(String formatString) {
      switch (formatString.toLowerCase()) {
         case "tiny":
            this.format = MorganFormat.TINY;
            break;
         case "short":
            this.format = MorganFormat.SHORT;
            break;
         case "combined":
            this.format = MorganFormat.COMBINED;
            break;
         case "dev":
         default:
            this.format = MorganFormat.DEV;
      }

   }

   public void handle(Request req, Response res, MiddlewareChain next) throws Exception {
      long startTime = System.currentTimeMillis();
      res.onFinish(() -> {
         long duration = System.currentTimeMillis() - startTime;
         String logMessage = this.formatLog(this.format, req, res, duration);
         System.out.println(logMessage);
      });
      next.next(req, res);
   }

   private String formatLog(MorganFormat format, Request req, Response res, long durationMs) {
      String method = req.getMethod();
      String path = req.path;
      int status = res.getStatus();
      String length = String.valueOf(res.getBodyLength());
      String ip = req.getIpAddress() != null ? req.getIpAddress() : "127.0.0.1";
      String ua = req.getHeader("user-agent") != null ? req.getHeader("user-agent") : "-";
      String referer = req.getHeader("referer") != null ? req.getHeader("referer") : "-";
      String time = DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss Z").format(ZonedDateTime.now());
      String var10000;
      switch (format.ordinal()) {
         case 0:
            var10000 = String.format("%s %s %s %d %dms", method, path, this.getColorStatus(status), status, durationMs);
            break;
         case 1:
            var10000 = String.format("%s %s %s - %dms", method, path, this.getColorStatus(status), durationMs);
            break;
         case 2:
            var10000 = String.format("%s %s %s - %dms", method, path, this.getColorStatus(status), durationMs);
            break;
         case 3:
            var10000 = String.format("%s - - [%s] \"%s %s HTTP/1.1\" %s %s \"%s\" \"%s\"", ip, time, method, path, this.getColorStatus(status), length, referer, ua);
            break;
         default:
            var10000 = method + " " + path + " " + this.getColorStatus(status);
      }

      return var10000;
   }

   private String getColorStatus(int status) {
      if (status >= 500) {
         return "\u001b[31m" + status + "\u001b[0m";
      } else if (status >= 400) {
         return "\u001b[33m" + status + "\u001b[0m";
      } else if (status >= 300) {
         return "\u001b[36m" + status + "\u001b[0m";
      } else {
         return status >= 200 ? "\u001b[32m" + status + "\u001b[0m" : String.valueOf(status);
      }
   }
}
