package heo.router;

import heo.interfaces.Middleware;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Route {
   private Map<String,Route> children = new HashMap<>();
   private Map<String,List<Middleware>> middlewares = new HashMap<>();
   private boolean isParameterized = false;
   private String keyParam;
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

   public boolean isParameterized() {
      return isParameterized;
   }

   public void setParameterized(boolean parameterized) {
      isParameterized = parameterized;
   }
}
