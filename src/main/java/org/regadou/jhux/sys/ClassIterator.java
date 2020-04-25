package org.regadou.jhux.sys;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class ClassIterator implements Iterator<Class> {

   private Class currentClass;
   private List<Class> interfaces;
   private int at;

   public ClassIterator(Class c) {
      currentClass = (c == null) ? Void.class : c;
   }

   public ClassIterator(Object obj) {
      currentClass = (obj == null) ? Void.class : obj.getClass();
   }

   @Override
   public boolean hasNext() {
      //TODO: each interface can extends parent interfaces
      if (interfaces != null) {
         if (at < interfaces.size())
            return true;
         interfaces = null;
         currentClass = getSuperClass(currentClass);
      }
      return currentClass != null;
   }

   @Override
   public Class next() {
      //TODO: each interface can extends parent interfaces
      if (interfaces != null) {
         if (at < interfaces.size())
            return interfaces.get(at++);
         interfaces = null;
         currentClass = getSuperClass(currentClass);
      }
      if (currentClass == null)
         throw new NoSuchElementException("No more element to iterate over");
      interfaces = new ArrayList<>();
      getInterfaces(currentClass.getInterfaces());
      at = 0;
      return currentClass;
   }

   private Class getSuperClass(Class src) {
      if (src == null)
         return null;
      else if (src.isArray()) {
         Class comp = src.getComponentType();
         //TODO: we should check if component is an interface and loop through all implemented interfaces
         comp = comp.getSuperclass();
         return (comp == null) ? Object.class : Array.newInstance(comp, 0).getClass();
      }
      else
         return src.getSuperclass();
   }

   private void getInterfaces(Class[] src) {
      for (Class type : src) {
         interfaces.add(type);
         getInterfaces(type.getInterfaces());
      }
   }
}
