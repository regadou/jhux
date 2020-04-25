package org.regadou.jhux.ref;

import java.io.File;
import java.lang.reflect.Array;
import java.net.URI;
import java.net.URL;
import java.util.Collection;
import java.util.Map;
import java.util.function.BiFunction;
import org.apache.commons.beanutils.BeanMap;
import org.regadou.jhux.JHUX;
import org.regadou.jhux.Namespace;
import org.regadou.jhux.Reference;
import org.regadou.jhux.impl.RootType;
import org.regadou.jhux.sys.Context;

public class Property implements Reference {

   public static interface Getter extends BiFunction<Object,String,Object> {}
   
   private static final Getter GENERIC_GETTER = (x, y) -> {
      switch (y) {
         case "class":
            return (x == null) ? Void.class : x.getClass();
         case "type":
            return RootType.getType((x == null) ? null : x.getClass());
         // TODO: case all to have the list of all properties
         default:
            // TODO: check if starts with [ or { to extract from json value composite propertiy
            return null;
      }
   };
   
   private Object owner;
   private String key;
   private transient Getter getter;

   public Property() {}

   public Property(String key) {
      this.key = key;
   }

   public Property(Object owner, String key) {
      this.key = key;
      this.owner = owner;
   }

   @Override
   public Object getOwner() {
      return owner;
   }

   @Override
   public String getKey() {
      return key;
   }

   @Override
   public Object getValue() {
      if (getter == null)
         getter = getGetter();
      return getter.apply(owner, key);
   }

   @Override
   public String toString() {
      return owner+"#"+key;
   }

   @Override
   public boolean equals(Object that) {
      return that != null && toString().equals(that.toString());
   }

   @Override
   public int hashCode() {
      return toString().hashCode();
   }
   
   private Getter getGetter() {
      if (owner == null)
         owner = JHUX.get(Context.class).getDictionary();
      if (key == null || key.equals("") || key.equals("."))
         return (x, y) -> x;
      if (owner.getClass().isArray()) {
         if (key.equals("length") || key.equals("size"))
            return (x, y) -> Array.getLength(x);
         return (x, y) -> {
            try { return Array.get(x, Integer.parseInt(y)); }
            catch (Exception e) { return GENERIC_GETTER.apply(x, y); }
         };
      }
      if (owner instanceof Map)
         return (x, y) -> {
            Map m = (Map)x;
            if (m.containsKey(y))
               return m.get(y);
            return GENERIC_GETTER.apply(x, y);
         };
      if (owner instanceof Namespace)
         return (x, y) -> ((Namespace)x).get(y);
      if (owner instanceof Number || owner instanceof Boolean)
         return GENERIC_GETTER;
      if (owner instanceof Character || owner instanceof CharSequence)
         return (x, y) -> new Property(x.toString().toCharArray(), y).getValue();
      if (owner instanceof Collection)
         return (x, y) -> new Property(((Collection)x).toArray(), y).getValue();
      if (owner instanceof File)
         owner = new Resource((File)owner);
      if (owner instanceof URI)
         owner = new Resource((URI)owner);
      if (owner instanceof URL)
         owner = new Resource((URL)owner);
      if (owner instanceof Reference)
         return (x, y) -> new Property(((Reference)x).getValue(), y).getValue();
      // TODO: check if owner is not beanable, we must throw an exception
      return (x, y) -> new Property(new BeanMap(x), y).getValue();
   }
}
