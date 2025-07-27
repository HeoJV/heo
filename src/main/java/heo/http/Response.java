// Source code is decompiled from a .class file using FernFlower decompiler.
package heo.http;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Response {
   private Runnable onFinish;
   private final Socket clientSocket;
   private final PrintWriter writer;
   private final OutputStream out;
   private final Map<String, String> headers = new HashMap();
   private int status = 200;
   private final ObjectMapper objectMapper = new ObjectMapper();

   public void setHeader(String key, String value) {
      this.headers.put(key, value);
   }

   public Response(Socket clientSocket) {
      this.clientSocket = clientSocket;

      try {
         this.out = clientSocket.getOutputStream();
         this.writer = new PrintWriter(new OutputStreamWriter(this.out, StandardCharsets.UTF_8), true);
      } catch (Exception var3) {
         throw new RuntimeException("Error initializing response writer", var3);
      }
   }

   public int getStatus() {
      return this.status;
   }

   public int getBodyLength() {
      return 0;
   }

   public Response status(int status) {
      this.status = status;
      return this;
   }

   public void send(String body) {
      try {
         this.writer.println("HTTP/1.1 " + this.status + " " + HttpStatusCode.getStatusMessage(this.status));
         this.writer.println("Content-Type: text/plain");
         this.writer.println("Content-Length: " + body.getBytes().length);
         this.addHeaders();
         this.writer.print(body);
         this.writer.flush();
      } catch (Exception e) {
         System.err.println("Error sending response: " + e.getMessage());
      } finally {
         try {
            this.clientSocket.close();
         } catch (IOException e) {
            System.err.println("Error closing client socket: " + e.getMessage());
         }

      }

   }

   public void json(Object object) {
      try {
         String json = this.objectMapper.writeValueAsString(object);
         this.writer.println("HTTP/1.1 " + this.status + " " + HttpStatusCode.getStatusMessage(this.status));
         this.writer.println("Content-Type: application/json");
         this.writer.println("Content-Length: " + json.getBytes().length);
         this.addHeaders();
         this.writer.print(json);
         this.writer.flush();
      } catch (Exception e) {
         System.err.println("Error sending JSON response: " + e.getMessage());
      } finally {
         try {
            if (this.onFinish != null) {
               this.onFinish.run();
            }

            this.clientSocket.close();
         } catch (IOException e) {
            System.err.println("Error closing client socket: " + e.getMessage());
         }

      }

   }

   public void onFinish(Runnable callback) {
      this.onFinish = callback;
   }

   private void addHeaders() {
      Iterator var1 = this.headers.entrySet().iterator();

      while(var1.hasNext()) {
         Map.Entry<String, String> entry = (Map.Entry)var1.next();
         PrintWriter var10000 = this.writer;
         String var10001 = (String)entry.getKey();
         var10000.println(var10001 + ": " + (String)entry.getValue());
      }

      this.writer.println();
   }
}
