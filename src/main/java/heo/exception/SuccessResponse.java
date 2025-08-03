package heo.exception;

import heo.http.Response;
import java.util.Map;

public class SuccessResponse<T> {
   private String message = "Success";
   private T data;
   private int status = 200;

   public SuccessResponse(String message, T data) {
      this.message = message;
      this.data = data;
   }

   public SuccessResponse(String message, T data, int status) {
      this.message = message;
      this.data = data;
      this.status = status;
   }

   public SuccessResponse<T> send(Response res) {
      Map<String, Object> responseBody = Map.of("message", this.message, "data", this.data, "statusCode", this.status);
      res.status(this.status).json(responseBody);
      return this;
   }
}
