package org.regadou.jhux.ref;

import java.util.Map;
import org.regadou.jhux.JHUX;
import org.regadou.jhux.Reference;
import org.regadou.jhux.sys.Context;

public class Value implements Reference {   
   
   private String key;
   private Object value;
   
   public Value() {}
   
   public Value(Object value) {
      this.value = value;
      if (value instanceof Map.Entry) {
         Object k = ((Map.Entry)value).getKey();
         if (k != null)
            key = k.toString();
      }
   }
   
   public Value(String key, Object value) {
      this.key = key;
      this.value = value;
   }
   
   @Override
   public Object getOwner() {
      return null;
   }
   
   @Override
   public String getKey() {
      return key;
   }
   
   @Override
   public Object getValue() {
      return (value instanceof Map.Entry) ? ((Map.Entry)value).getValue() : value;
   }

   @Override
   public String toString() {
      Context cx = JHUX.get(Context.class);
      if (key == null)
         return cx.print(value);
      return cx.print(key)+":"+cx.print(value);
   } 

   @Override
   public boolean equals(Object that) {
      return that != null && toString().equals(that.toString());
   }

   @Override
   public int hashCode() {
      return toString().hashCode();
   }   
}

