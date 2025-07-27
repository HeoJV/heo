// Source code is decompiled from a .class file using FernFlower decompiler.
package heo.exception;

public class UnauthorizedError extends ErrorResponse {
   public UnauthorizedError(String message, int status) {
      super(message, status);
   }

   public UnauthorizedError(String message) {
      super(message, 401);
   }
}
