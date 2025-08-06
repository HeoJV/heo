package heo.router;

import heo.interfaces.Middleware;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * File: Route.java
 * Description: Route class for handling route definitions, parameters, and middlewares in a web application.
 * Author: 102004tan
 * Created: 25/07/2025
 * Updated: 04/08/2025
 */
public class Route {
   private Map<String,Route> children = new HashMap<>();
   private Map<String,List<Middleware>> middlewares = new HashMap<>();
   private String keyParam;
   private Map<String,Map<Integer,String>> params = new HashMap<>();
   private boolean isEndpoint = false;

   public void setEndpoint(boolean isEndpoint) {
      this.isEndpoint = isEndpoint;
   }

   public void setMiddlewares(Map<String,List<Middleware>> middlewares) {
      this.middlewares = middlewares;
   }

   public Map<String,List<Middleware>> getMiddlewares() {
      return middlewares;
   }

    public boolean isEndpoint() {
        return isEndpoint;
    }

   /**
    POST 2-postId,4-userId
    GET 2-postId,4-userId
    */

   public void setParams(Map<Integer,String> params, String method) {
      this.params.put(method, params);
   }

   public Map<Integer,String> getParams(String method) {
      return params.getOrDefault(method, new HashMap<>());
   }

   public boolean hasParams(String method) {
      return params.containsKey(method);
   }

   public Map<String, Route> getChildren() {
      return children;
   }




   public String getKeyParam() {
        return keyParam;
   }

    public void setKeyParam(String keyParam) {
        this.keyParam = keyParam;
    }


   public void setMiddlewares(String method, List<Middleware> middlewares) {
      this.middlewares.put(
              method, middlewares
      );
   }

   public int totalMethods() {
      return middlewares.size();
   }


   public List<Middleware> getMiddlewares(String method) {
        return middlewares.getOrDefault(method, List.of());
   }

   public boolean isMethodSupported(String method) {
      return middlewares.containsKey(method);
   }
}
