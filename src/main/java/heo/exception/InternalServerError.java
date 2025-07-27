// Source code is decompiled from a .class file using FernFlower decompiler.
package heo.exception;

public class InternalServerError extends ErrorResponse {
   public InternalServerError(String message, int status) {
      super(message, status);
   }

   public InternalServerError(String message) {
      super(message, 500);
   }
}
