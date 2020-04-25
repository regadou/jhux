package org.regadou.jhux.impl;

import java.util.AbstractMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class ImmutableMap extends AbstractMap {

   private Set entries = new LinkedHashSet();
   
   public ImmutableMap(Object...tokens) {
      super();
      if (tokens.length % 2 != 0)
         throw new RuntimeException("Tokens must be in pairs");
      for (int i = 0; i < tokens.length; i += 2)
         entries.add(new AbstractMap.SimpleImmutableEntry(tokens[i], tokens[i+1]));
   }   
   
   @Override
   public Set<Map.Entry> entrySet() {
      return entries;
   }
   
}
