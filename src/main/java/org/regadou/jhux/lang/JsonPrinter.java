package org.regadou.jhux.lang;

import org.regadou.jhux.Printer;
import com.google.gson.Gson;
import org.regadou.jhux.sys.Configuration;

public class JsonPrinter implements Printer {

   private Configuration config;
   private Gson gson;
   
   public JsonPrinter(Configuration config) {
      this.config = config;
   }
   @Override
   public String print(Object value) {
      if (gson == null)
         gson = config.getInstance(Gson.class);
      return gson.toJson(value);
   }
   
}
