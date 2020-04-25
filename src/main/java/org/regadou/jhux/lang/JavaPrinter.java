package org.regadou.jhux.lang;

import org.regadou.jhux.Printer;
import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import org.regadou.jhux.JHUX;
import org.regadou.jhux.sys.Context;

public class JavaPrinter implements Printer {

   @Override
   public String print(Object value) {
      if (value == null)
         return "null";
      if (value instanceof Object[])
         return Arrays.asList((Object[])value).toString();
      if (value.getClass().isArray())
         return JHUX.get(Context.class).convert(value, Collection.class).toString();
      if (value instanceof Class)
         return ((Class)value).getName();
      if (value instanceof File)
         return ((File)value).toURI().toString();
      return value.toString();
   }
   
}
