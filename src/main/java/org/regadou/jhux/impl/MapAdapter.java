package org.regadou.jhux.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class MapAdapter<K,V> implements Map<K,V> {

   private static class MapEntry<K, V> implements Entry<K, V> {

      private Map<K, V> map;
      private K key;

      public MapEntry(Map<K, V> map, K key) {
         this.map = map;
         this.key = key;
      }

      @Override
      public K getKey() {
         return key;
      }

      @Override
      public V getValue() {
         return map.get(key);
      }

      @Override
      public V setValue(V value) {
         return map.put(key, value);
      }
   }

   private Supplier<Set<K>> keys;
   private Function<K,V> getter;
   private BiConsumer<K,V> setter;
   private Consumer<K> remover;

   public MapAdapter(Function<K,V> getter) {
      this(null, getter, null, null);
   }

   public MapAdapter(Supplier<Set<K>> keys, Function<K,V> getter) {
      this(keys, getter, null, null);
   }

   public MapAdapter(Function<K,V> getter, BiConsumer<K,V> setter) {
      this(null, getter, setter, null);
   }

   public MapAdapter(Supplier<Set<K>> keys, Function<K,V> getter, BiConsumer<K,V> setter) {
      this(keys, getter, setter, null);
   }

   public MapAdapter(Supplier<Set<K>> keys, Function<K,V> getter, BiConsumer<K,V> setter, Consumer<K> remover) {
      this.keys = keys;
      this.getter = getter;
      this.setter = setter;
      this.remover = remover;
   }

   @Override
   public int size() {
      if (keys != null)
         return keys.get().size();
      throw new UnsupportedOperationException("Not supported");
   }

   @Override
   public boolean isEmpty() {
      if (keys != null)
         return keys.get().isEmpty();
      throw new UnsupportedOperationException("Not supported");
   }

   @Override
   public boolean containsKey(Object key) {
      if (keys != null)
         return keys.get().contains((K)key);
      throw new UnsupportedOperationException("Not supported");
   }

   @Override
   public boolean containsValue(Object value) {
      return values().contains((V)value);
   }

   @Override
   public V get(Object key) {
      if (getter != null)
         return getter.apply((K)key);
      throw new UnsupportedOperationException("Not supported");
   }

   @Override
   public V put(K key, V value) {
      if (setter != null) {
         V old = (getter == null) ? null : getter.apply(key);
         setter.accept(key, value);
         return old;
      }
      throw new UnsupportedOperationException("Not supported");
   }

   @Override
   public V remove(Object key) {
      if (remover != null) {
         V old = (getter == null) ? null : getter.apply((K)key);
         remover.accept((K)key);
         return old;
      }
      throw new UnsupportedOperationException("Not supported");
   }

   @Override
   public void putAll(Map<? extends K, ? extends V> m) {
      if (setter != null) {
         if (m != null) {
            for (K key : m.keySet())
               put(key, m.get(key));
         }
      }
      else
         throw new UnsupportedOperationException("Not supported");
   }

   @Override
   public void clear() {
      if (keys != null && remover != null) {
         for (K key : keys.get())
            remover.accept(key);
      }
      else
         throw new UnsupportedOperationException("Not supported");
   }

   @Override
   public Set<K> keySet() {
      if (keys != null)
         return keys.get();
      throw new UnsupportedOperationException("Not supported");
   }

   @Override
   public Collection<V> values() {
      if (keys != null && getter != null) {
         Collection<V> values = new ArrayList<>();
         for (K key : keys.get())
            values.add(getter.apply(key));
         return values;
      }
      throw new UnsupportedOperationException("Not supported");
   }

   @Override
   public Set<Entry<K, V>> entrySet() {
      if (keys != null) {
         Set<Entry<K, V>> entries = new LinkedHashSet<>();
         for (K key : keys.get())
            entries.add(new MapEntry<K, V>(this, key));
         return entries;
      }
      throw new UnsupportedOperationException("Not supported");
   }

   @Override
   public String toString() {
      return new LinkedHashMap(this).toString();
   }
}
