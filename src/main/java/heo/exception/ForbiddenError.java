// Source code is decompiled from a .class file using FernFlower decompiler.
package heo.exception;

public class ForbiddenError extends ErrorResponse {
   public ForbiddenError(String message, int status) {
      super(message, status);
   }

   public ForbiddenError(String message) {
      super(message, 403);
   }
}
