package org.regadou.jhux.sql;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

//TODO: should be a reference holding the map with type of the related SqlTable and SqlRowProperty to give the values
public class SqlRow extends LinkedHashMap<String,Object> {

   private transient Predicate<Map<String,Object>> updateFunction;
   private transient boolean modified;

   public SqlRow() {
      super();
   }

   public SqlRow(Map<String,Object> map) {
      super(map);
   }

   public SqlRow(Predicate<Map<String,Object>> updateFunction) {
      super();
      this.updateFunction = updateFunction;
   }

   public SqlRow(Map<String,Object> map, Predicate<Map<String,Object>> function) {
      super(map);
      this.updateFunction = function;
   }

   @Override
   public Object put(String key, Object value) {
      Object old = super.put(key, value);
      modified = true;
      return old;
   }

   @Override
   public Object remove(Object key) {
      Object old = super.remove(key);
      modified = true;
      return old;
   }

   @Override
   public void clear() {
      super.clear();
      modified = true;
   }

   public boolean isModified() {
      return modified;
   }

   public boolean update() {
      if (!modified || updateFunction == null)
         return false;
      boolean success = updateFunction.test(this);
      if (success)
         modified = false;
      return success;
   }

   public void cancelUpdate() {
      modified = false;
   }

   public Predicate<Map<String,Object>> getUpdateFunction() {
      return updateFunction;
   }
}
