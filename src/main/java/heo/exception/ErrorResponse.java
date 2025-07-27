// Source code is decompiled from a .class file using FernFlower decompiler.
package heo.exception;

public class ErrorResponse extends RuntimeException {
   private int status;

   public ErrorResponse(String message, int status) {
      super(message);
      this.status = status;
   }
}
