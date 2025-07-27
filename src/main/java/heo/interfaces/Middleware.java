// Source code is decompiled from a .class file using FernFlower decompiler.
package heo.interfaces;

import heo.http.Request;
import heo.http.Response;
import heo.middleware.MiddlewareChain;

@FunctionalInterface
public interface Middleware {
   void handle(Request var1, Response var2, MiddlewareChain var3) throws Exception;
}
