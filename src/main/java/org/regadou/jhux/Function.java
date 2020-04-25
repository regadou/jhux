package org.regadou.jhux;

@FunctionalInterface
public interface Function {

   Object execute(Object...parameters);   
   
   default String getName() {
      return "function#"+hashCode();
   }
}

