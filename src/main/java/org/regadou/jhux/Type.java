package org.regadou.jhux;

public interface Type {
   
   String getName();
   
   Type getSuperType();
   
   Function getConstructor();
   
   boolean isInstance(Object value);
}
