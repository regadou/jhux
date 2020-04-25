package org.regadou.jhux.ns;

import java.util.function.BiFunction;
import java.util.function.Function;
import org.regadou.jhux.Namespace;
import org.regadou.jhux.Reference;

public class NamespaceAdapter implements Namespace {
   
   private String name;
   private Function<String,Reference> getter;
   private BiFunction<String,Object,Reference> setter;
   private Function<String,Integer> deleter;
   private BiFunction<String,Object,Reference> poster;

   public NamespaceAdapter(String name, Function<String,Reference> getter) {
      this(name, getter, null, null, null);
   }

   public NamespaceAdapter(String name, Function<String,Reference> getter, BiFunction<String,Object,Reference> setter) {
      this(name, getter, setter, null, null);      
   }

   public NamespaceAdapter(String name, Function<String,Reference> getter, BiFunction<String,Object,Reference> setter, Function<String,Integer> deleter) {
      this(name, getter, setter, deleter, null);            
   }

   public NamespaceAdapter(String name, Function<String,Reference> getter, BiFunction<String,Object,Reference> setter, Function<String,Integer> deleter, BiFunction<String,Object,Reference> poster) {
      this.name = name.trim();
      this.getter = getter;
      this.setter = setter;
      this.deleter = deleter;
      this.poster = poster;
   }
   
   @Override
   public String getPrefix() {
      return name;
   }
   
   @Override
   public String getUri() {
      return name+":";
   }

   @Override
   public Reference get(String uri) {
      return (getter == null) ? null : getter.apply(uri);
   }

   @Override
   public Reference post(String uri, Object data) {
      return (poster == null) ? null : poster.apply(uri, data);
   }

   @Override
   public Reference put(String uri, Object data) {
      return (setter == null) ? null : setter.apply(uri, data);
   }

   @Override
   public int delete(String uri) {
      return (deleter == null) ? 0 : deleter.apply(uri);
   }
   
   @Override
   public String toString() {
      return getUri();
   }   
}
