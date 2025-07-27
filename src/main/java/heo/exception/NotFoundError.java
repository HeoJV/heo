// Source code is decompiled from a .class file using FernFlower decompiler.
package heo.exception;

public class NotFoundError extends ErrorResponse {
   public NotFoundError(String message, int status) {
      super(message, status);
   }

   public NotFoundError(String message) {
      super(message, 404);
   }
}
