// Source code is decompiled from a .class file using FernFlower decompiler.
package heo.core;

public class Console {
   private static final String RESET = "\u001b[0m";
   private static final String RED = "\u001b[31m";
   private static final String YELLOW = "\u001b[33m";
   private static final String BLUE = "\u001b[34m";
   private static final String CYAN = "\u001b[36m";
   private static final String GRAY = "\u001b[90m";

   public Console() {
   }

   public static void log(Object... args) {
      printWithPrefix("[LOG]", "\u001b[36m", args);
   }

   public static void info(Object... args) {
      printWithPrefix("[INFO]", "\u001b[34m", args);
   }

   public static void warn(Object... args) {
      printWithPrefix("[WARN]", "\u001b[33m", args);
   }

   public static void error(Object... args) {
      printWithPrefix("[ERROR]", "\u001b[31m", args);
   }

   public static void debug(Object... args) {
      printWithPrefix("[DEBUG]", "\u001b[90m", args);
   }

   private static void printWithPrefix(String prefix, String color, Object... args) {
      StringBuilder sb = new StringBuilder();
      sb.append(color).append(prefix).append(" ");
      Object[] var4 = args;
      int var5 = args.length;

      for(int var6 = 0; var6 < var5; ++var6) {
         Object arg = var4[var6];
         sb.append(arg).append(" ");
      }

      sb.append("\u001b[0m");
      System.out.println(sb.toString());
   }
}
