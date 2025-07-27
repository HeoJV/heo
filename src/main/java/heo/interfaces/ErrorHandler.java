// Source code is decompiled from a .class file using FernFlower decompiler.
package heo.interfaces;

import heo.http.Request;
import heo.http.Response;

@FunctionalInterface
public interface ErrorHandler {
   void handle(Throwable e, Request req, Response res) throws Exception;
}
