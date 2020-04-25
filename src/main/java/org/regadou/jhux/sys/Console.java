package org.regadou.jhux.sys;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import org.regadou.jhux.JHUX;

public class Console implements Closeable {
   
   private String name;
   private BufferedReader input;
   private PrintStream output;
   private transient boolean running;
   
   public Console(String name, InputStream input, OutputStream output) {
      this.name = name;
      this.input = new BufferedReader(new InputStreamReader(input));
      this.output = (output instanceof PrintStream) ? (PrintStream)output : new PrintStream(output);
   }

   public String getName() {
      return name;
   }

   public BufferedReader getInput() {
      return input;
   }

   public PrintStream getOutput() {
      return output;
   }
   
   public void run(String inputPrompt, String outputPrompt, String ... endWords) {
      run(null, inputPrompt, outputPrompt, endWords);
   }
      
   public void run(Context cx, String inputPrompt, String outputPrompt, String ... endWords) {
      if (cx == null)
         cx = JHUX.get(Context.class);
      if (inputPrompt == null)
         inputPrompt = "";
      if (outputPrompt == null)
         outputPrompt = "";
      List<String> endWordsList = Arrays.asList((endWords.length == 0) ? new String[]{""} : endWords);
      running = true;
      while (running) {
         try {
            output.print(inputPrompt);
            String txt = input.readLine();
            if (txt == null)
               running = false;
            else {
               txt = txt.trim();
               if (endWordsList.indexOf(txt) >= 0)
                  running = false;
               else if (!txt.isEmpty())
                  output.println(outputPrompt+cx.print(cx.parse(txt).getValue()));
            }
         }
         catch (Exception e) {
            e.printStackTrace();
         }
      }
   }

   public boolean isRunning() {
      return running;
   }

   @Override
   public void close() {
      synchronized (this) { running = false; }
   }
}
