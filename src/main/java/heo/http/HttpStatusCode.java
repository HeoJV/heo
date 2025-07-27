// Source code is decompiled from a .class file using FernFlower decompiler.
package heo.http;

public class HttpStatusCode {
   public static final int OK = 200;
   public static final int CREATED = 201;
   public static final int ACCEPTED = 202;
   public static final int NO_CONTENT = 204;
   public static final int MOVED_PERMANENTLY = 301;
   public static final int FOUND = 302;
   public static final int NOT_MODIFIED = 304;
   public static final int BAD_REQUEST = 400;
   public static final int UNAUTHORIZED = 401;
   public static final int FORBIDDEN = 403;
   public static final int NOT_FOUND = 404;
   public static final int METHOD_NOT_ALLOWED = 405;
   public static final int CONFLICT = 409;
   public static final int INTERNAL_SERVER_ERROR = 500;
   public static final int NOT_IMPLEMENTED = 501;
   public static final int BAD_GATEWAY = 502;
   public static final int SERVICE_UNAVAILABLE = 503;
   public static final int GATEWAY_TIMEOUT = 504;

   public HttpStatusCode() {
   }

   public static String getStatusMessage(int statusCode) {
      String var10000;
      switch (statusCode) {
         case 200:
            var10000 = "OK";
            break;
         case 201:
            var10000 = "Created";
            break;
         case 202:
            var10000 = "Accepted";
            break;
         case 204:
            var10000 = "No Content";
            break;
         case 301:
            var10000 = "Moved Permanently";
            break;
         case 302:
            var10000 = "Found";
            break;
         case 304:
            var10000 = "Not Modified";
            break;
         case 400:
            var10000 = "Bad Request";
            break;
         case 401:
            var10000 = "Unauthorized";
            break;
         case 403:
            var10000 = "Forbidden";
            break;
         case 404:
            var10000 = "Not Found";
            break;
         case 405:
            var10000 = "Method Not Allowed";
            break;
         case 409:
            var10000 = "Conflict";
            break;
         case 500:
            var10000 = "Internal Server Error";
            break;
         case 501:
            var10000 = "Not Implemented";
            break;
         case 502:
            var10000 = "Bad Gateway";
            break;
         case 503:
            var10000 = "Service Unavailable";
            break;
         case 504:
            var10000 = "Gateway Timeout";
            break;
         default:
            var10000 = "Unknown";
      }

      return var10000;
   }
}
