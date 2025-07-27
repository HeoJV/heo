// Source code is decompiled from a .class file using FernFlower decompiler.
package heo.config;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class Dotenv {
   private static final String filePath = ".env";

   public Dotenv() {
   }

   public static void config() {
      try {
         BufferedReader reader = new BufferedReader(new FileReader(".env"));

         String line;
         try {
            while((line = reader.readLine()) != null) {
               line = line.trim();
               if (!line.isEmpty() && !line.startsWith("#")) {
                  String[] parts = line.split("=", 2);
                  if (parts.length == 2) {
                     String key = parts[0].trim().toLowerCase();
                     String value = parts[1].trim();
                     System.setProperty(key, value);
                  }
               }
            }
         } catch (Throwable e) {
            try {
               reader.close();
            } catch (Throwable var5) {
               e.addSuppressed(var5);
            }

            throw e;
         }
         reader.close();
      } catch (IOException e) {
//         System.err.println("Error loading .env file: " + e.getMessage());
      }

   }

   public static String get(String key) {
      return System.getProperty(key.toLowerCase());
   }
}
