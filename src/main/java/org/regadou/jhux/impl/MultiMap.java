package org.regadou.jhux.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.TreeMap;
import org.apache.commons.beanutils.BeanMap;

public class MultiMap implements Map {

   protected List<Map> maps = new ArrayList<>();

   public MultiMap(Map...maps) {
      if (maps != null) {
         for (Map map : maps) {
            if (map != null)
               this.maps.add(map);
         }
      }
   }

   @Override
   public String toString() {
      StringJoiner joiner = new StringJoiner(", ", "{", "}");
      for (Map map : maps) {
          for (Object key : map.keySet())
             joiner.add(key+":"+map.get(key));
      }
      return joiner.toString();
   }

   @Override
   public Set keySet() {
      Set keys = new LinkedHashSet();
      for (Map map : maps)
          keys.addAll(map.keySet());
      return keys;
   }

   @Override
   public Set entrySet() {
      Set entries = new LinkedHashSet();
      for (Map map : maps)
          entries.addAll(map.entrySet());
      return entries;
   }

   @Override
   public Collection values() {
      List values = new ArrayList();
      for (Map map : maps)
          values.addAll(map.values());
      return values;
   }

   @Override
   public Object get(Object key) {
      for (Map map : maps) {
         if (map.containsKey(key))
            return map.get(key);
      }
      return null;
   }

   @Override
   public Object put(Object key, Object value) {
      for (Map map : maps) {
         if (map.containsKey(key))
            return map.put(key, value);
      }
      if (maps.isEmpty())
         maps.add(new LinkedHashMap());
      return maps.get(0).put(key, value);
   }

   @Override
   public void putAll(Map map) {
      if (map != null) {
         for (Object key : map.keySet())
            put(key, map.get(key));
      }
   }

   @Override
   public boolean containsKey(Object key) {
      for (Map map : maps) {
         if (map.containsKey(key))
            return true;
      }
      return false;
   }

   @Override
   public boolean containsValue(Object value) {
      for (Map map : maps) {
         if (map.containsValue(value))
            return true;
      }
      return false;
   }

   @Override
   public boolean isEmpty() {
      for (Map map : maps) {
         if (!map.isEmpty())
            return false;
      }
      return true;
   }

   @Override
   public int size() {
      int size = 0;
      for (Map map : maps)
         size += map.size();
      return size;
   }

   @Override
   public void clear() {
      for (Map map : maps)
         map.clear();
   }

   @Override
   public Object remove(Object key) {
      for (Map map : maps) {
         if (map.containsKey(key))
            return map.remove(key);
      }
      return null;
   }

   @Override
   public boolean equals(Object that) {
      TreeMap map;
      if (that == null)
         return this.isEmpty();
      if (that instanceof TreeMap)
         map = (TreeMap)that;
      else if (that instanceof Map)
         map = new TreeMap((Map)that);
      else
         map = new TreeMap(new BeanMap(that));
      return new TreeMap(this).equals(that);
   }

   public int getMaps() { return maps.size(); }

   public Map getMap(int index) {
      return (index < 0 || index >= maps.size()) ? null : maps.get(index);
   }
}
