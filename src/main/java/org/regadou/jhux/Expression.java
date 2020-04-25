package org.regadou.jhux;

public interface Expression extends Reference {

   @Override
   default Object getOwner() {
      return null;
   }

   @Override
   default String getKey() {
      return null;
   }

   Function getFunction();

   Object[] getParameters();
}

