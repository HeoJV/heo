// Source code is decompiled from a .class file using FernFlower decompiler.
package heo.http;

import java.util.HashMap;
import java.util.Map;

public class Request {
   private String method;
   public String path;
   private Map<String, String> query;
   private Map<String, String> headers;
   private Map<String, String> params;
   private String ipAddress;
   private String rawBody;
   private final Map<String, Object> body;

   public String getIpAddress() {
      return this.ipAddress;
   }

   public void setIpAddress(String ipAddress) {
      this.ipAddress = ipAddress;
   }

   public String getMethod() {
      return this.method;
   }

   public void setHeaders(String key, String value) {
      this.headers.put(key, value);
   }

   public String getHeader(String key) {
      return (String)this.headers.get(key);
   }

   public String params(String key) {
      return (String)this.params.get(key);
   }

   public String getQuery(String key) {
      return (String)this.query.get(key);
   }

   public Map<String, String> getQuery() {
      return this.query;
   }

   public void setParams(Map<String, String> params) {
      this.params = params;
   }

   public void setQuery(Map<String, String> query) {
      this.query = query;
   }

   public void setRawBody(String rawBody) {
      this.rawBody = rawBody;
   }

   public String getRawBody() {
      return this.rawBody;
   }

   public void setBody(Map<String, Object> body) {
      assert this.body != null;

      this.body.clear();
      if (body != null) {
         this.body.putAll(body);
      }
   }

   public Map<String, Object> getBody() {
      return this.body;
   }

   public void setHeaders(Map<String, String> headers) {
      this.headers = headers;
   }

   public Request(String method, String path) {
      this.method = method;
      this.path = path;
      this.body = new HashMap<>();
      this.query = new HashMap<>();
      this.headers = new HashMap<>();
      this.params = new HashMap<>();
   }

   public Request(String method, String path, Map<String, Object> body, Map<String, String> query, Map<String, String> headers) {
      this.method = method;
      this.path = path;
      this.body = body;
      this.query = query;
      this.headers = headers;
   }
}
