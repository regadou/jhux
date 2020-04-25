package org.regadou.jhux.ns;

import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.regadou.jhux.JHUX;
import org.regadou.jhux.Namespace;
import org.regadou.jhux.Reference;

public class Vocabulary implements Namespace {
   
   private String prefix;
   private String uri;
   private Map map;
   private boolean readonly;

   public Vocabulary(String prefix) {
      this(prefix, null, null, false);
   }

   public Vocabulary(Map map) {
      this(null, null, map, false);
   }

   public Vocabulary(String prefix, String uri) {
      this(prefix, uri, null, false);
   }

   public Vocabulary(String prefix, Map map) {
      this(prefix, null, map, false);
   }

   public Vocabulary(String prefix, String uri, Map map) {
      this(prefix, uri, map, false);
   }

   public Vocabulary(String prefix, String uri, boolean readonly) {
      this(prefix, uri, null, readonly);
   }

   public Vocabulary(String prefix, Map map, boolean readonly) {
      this(prefix, null, map, readonly);
   }

   public Vocabulary(String prefix, String uri, Map map, boolean readonly) {
      this.prefix = (prefix == null) ? "" : prefix;
      this.uri = (uri == null) ? this.prefix+":" : uri;
      this.map = (map == null) ? new TreeMap() : map;
      this.readonly = readonly;
   }
   
   @Override
   public String getPrefix() {
      return prefix;
   }
   
   @Override
   public String getUri() {
      return uri;
   }

   @Override
   public Reference get(String uri) {
      Object value = map.get(uri);
      return (value == null) ? null : JHUX.ref(value);
   }

   @Override
   public Reference post(String uri, Object data) {
      return null;
   }

   @Override
   public Reference put(String uri, Object data) {
      if (readonly)
         return null;
      map.put(uri, data);
      return get(uri);
   }

   @Override
   public int delete(String uri) {
      if (readonly)
         return 0;
      return (map.remove(uri) == null) ? 0 : 1;
   }
   
   @Override
   public String toString() {
      return getUri();
   }   
}
