package org.regadou.jhux;

import java.net.URI;
import java.util.Arrays;
import org.regadou.jhux.ref.Resource;
import org.regadou.jhux.ref.Value;
import org.regadou.jhux.sys.Configuration;
import org.regadou.jhux.sys.Context;
import org.regadou.jhux.sys.Converter;

public class JHUX {

   private static Configuration GLOBAL_CONFIG;
   
   private static final ThreadLocal<Context> CURRENT_CONTEXT = new ThreadLocal() {
      @Override
      protected synchronized Context initialValue() { return null; }
   };
   
   public static Reference ref(Object value) {
      if (value instanceof Reference)
         return (Reference)value;
      URI uri = get(Converter.class).convert(value, URI.class);
      return (uri != null) ? new Resource(uri) : new Value(value);
   }
   
   public static Object unref(Object value) {
      while (value instanceof Reference)
         value = ((Reference)value).getValue();
      return value;
   }
   
   public static <T> T get(Class<T> type, Object ... properties) {
      if (type == Context.class) {
         Context cx = CURRENT_CONTEXT.get();
         if (cx == null) {
            if (properties.length == 1 && properties[0] instanceof Context)
               cx = (Context)properties[0];
            else
               cx = new Context(properties);
            CURRENT_CONTEXT.set(cx);
         }
         else if (properties.length > 0)
            throw new RuntimeException("Context already configured on this thread");
         return (T)cx;
      }
      if (GLOBAL_CONFIG == null) {
         if (type == Configuration.class)
            return (T)(GLOBAL_CONFIG = createInstance(Configuration.class, properties));
         GLOBAL_CONFIG = new Configuration();
      }
      switch (properties.length) {
         case 0:
            return GLOBAL_CONFIG.getInstance(type);
         case 1:
            if (properties[0] instanceof CharSequence) {
               String id = properties[0].toString().trim();
               if (id.isEmpty())
                  return GLOBAL_CONFIG.getInstance(type);
               switch (id.charAt(0)) {
                  case '[':
                  case '{':
                  case '"':
                  case '(':
                  default:
                     return GLOBAL_CONFIG.getInstance(type, id);
               }
            }
         default:
            return createInstance(type, properties);
      }
   }
   
   private static <T> T createInstance(Class<T> type, Object ... properties) {
      try {
         if (properties.length == 0)
            return type.newInstance();
         Class target = (properties[0] instanceof Class) ? (Class)properties[0] : Class.forName(properties[0].toString());
         Object[] params = Arrays.asList(properties).subList(1, properties.length).toArray();
         return (T)new java.beans.Expression(target, "new", params).getValue();
      }
      catch (Exception e) {
         RuntimeException rte = (e instanceof RuntimeException) ? (RuntimeException)e : new RuntimeException(e);
         throw rte;
      }
   }
}
