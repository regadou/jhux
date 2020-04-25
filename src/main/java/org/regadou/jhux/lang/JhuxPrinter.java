package org.regadou.jhux.lang;

import com.google.gson.Gson;
import org.regadou.jhux.Printer;
import java.io.File;
import java.lang.reflect.Array;
import java.net.URI;
import java.net.URL;
import java.util.Collection;
import java.util.Map;
import org.apache.commons.beanutils.BeanMap;
import org.regadou.jhux.Function;
import org.regadou.jhux.JHUX;
import org.regadou.jhux.Reference;
import org.regadou.jhux.Type;

public class JhuxPrinter implements Printer {

   private Gson gson;
    
   @Override
   public String print(Object value) {
      if (value == null)
         return "()";
      if (value instanceof char[])
          return getGson().toJson(new String((char[])value));
      if (value instanceof byte[])
          return getGson().toJson(new String((byte[])value));
      if (value.getClass().isArray()) {
          StringBuilder buffer = new StringBuilder("[");
          int n = Array.getLength(value);
          for (int i = 0; i < n; i++) {
              if (i > 0)
                  buffer.append(", ");
              buffer.append(print(Array.get(value, i)));
          }
          return buffer.append("]").toString();
      }
      if (value instanceof Map) {
          StringBuilder buffer = new StringBuilder("{");
          Map map = (Map)value;
          for (Object key : map.keySet()) {
              if (buffer.length() > 1)
                  buffer.append(", ");
              buffer.append(print(key)).append(":").append(print(map.get(key)));
          }
          return buffer.append("}").toString();          
      }
      if (value instanceof Class)
         return ((Class)value).getName();
      if (value instanceof Type)
         return ((Type)value).getName();
      if (value instanceof Function)
         return ((Function)value).getName();
      if (value instanceof File)
         return ((File)value).toURI().toString();
      if (value instanceof URI || value instanceof URL)
          return value.toString();
      if (value instanceof CharSequence || value instanceof Number || value instanceof Boolean || value instanceof Character)
          return getGson().toJson(value.toString());
      if (value instanceof Collection)
         return print(((Collection)value).toArray());
      if (value instanceof Reference)
         return print(((Reference)value).getValue());
      return print(new BeanMap(value));
   }
   
   private Gson getGson() {
       if (gson == null)
           gson = JHUX.get(Gson.class);
       return gson;
   }
}
