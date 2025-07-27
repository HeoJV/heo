// Source code is decompiled from a .class file using FernFlower decompiler.
package heo.exception;

public class BadRequest extends ErrorResponse {
   public BadRequest(String message, int status) {
      super(message, status);
   }

   public BadRequest(String message) {
      super(message, 400);
   }
}
