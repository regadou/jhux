package org.regadou.jhux.sys;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import org.regadou.jhux.JHUX;

public class Main {
  
   public static void main(String[] args) throws IOException {
      waitDebugger();
      Context cx = JHUX.get(Context.class);
      if (args.length == 0)
         new Console("main", System.in, System.out).run(cx, "\n? ", "= ", "quit", "exit");
      else
         System.out.println(cx.print(cx.parse(String.join(" ", args)).getValue()));
   }
   
   private static void waitDebugger() {
      String debug = System.getProperty("org.regadou.jhux.debug");
      if (debug != null && debug.compareToIgnoreCase("true") == 0) {
         try {
            System.out.println("*** press enter after starting debugger ***");
            new BufferedReader(new InputStreamReader(System.in)).readLine();
         }
         catch (IOException e) { throw new RuntimeException(e); }
      }
   }

   private Main() {}
}
