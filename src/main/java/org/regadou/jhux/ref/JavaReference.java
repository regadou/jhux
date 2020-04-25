package org.regadou.jhux.ref;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import org.regadou.jhux.Reference;

public class JavaReference implements Reference {
   
   private transient Reference owner;
   private transient String key;
   private AnnotatedElement value;
   
   public JavaReference() {}
   
   public JavaReference(AnnotatedElement e) {
      key = getName(e);
      value = e;
   }
   
   public JavaReference(String txt) {
      Package p = Package.getPackage(txt);
      if (p != null)
         value = p;
      else if ((value = getMethod(txt)) == null && (value = getConstructor(txt)) == null) {
         try { value = Class.forName(txt); }
         catch (ClassNotFoundException e) {
            try {
               int index = txt.lastIndexOf('.');
               if (index < 0)
                  throw e;
               String classname = txt.substring(0, index);
               String membername = txt.substring(index+1);
               Class c = Class.forName(classname);
               try { value = c.getDeclaredField(membername); }
               catch (NoSuchFieldException e2) {
                  for (Method m : c.getDeclaredMethods()) {
                     if (m.getName().equals(membername)) {
                        value = m;
                        return;
                     }
                  }
               }
               if (value == null)
                  throw e;
            }
            catch (ClassNotFoundException e2) { throw new IllegalArgumentException(e2); }
         }
      }
   }
   
   @Override
   public Object getOwner() {
      if (owner == null) {
         try { owner = new JavaReference(key.substring(0, key.lastIndexOf('.'))); }
         catch (Exception e) { owner = new Value(null); }
      }
      return owner;
   }

   @Override
   public String getKey() {
      return key;
   }
   
   @Override
   public Object getValue() {
      return value;
   }

   @Override
   public String toString() {
      return getKey();
   }

   @Override
   public boolean equals(Object that) {
      return that != null && toString().equals(that.toString());
   }

   @Override
   public int hashCode() {
      return getKey().hashCode();
   }
   
   private Method getMethod(String txt) {
      int index = txt.indexOf('(');
      if (index < 0)
         return null;
      String[] params = txt.substring(index+1).replace(")", "").split(",");
      String path = txt.substring(0, index);
      index = path.lastIndexOf('.');
      if (index < 0)
         return null;
      try { 
         Class c = Class.forName(txt.substring(0, index));
         String m = txt.substring(index+1);
         Class[] types = new Class[params.length];
         for (int t = 0; t < types.length; t++)
            types[t] = Class.forName(params[t].trim());
         return c.getDeclaredMethod(m, types);
      }
      catch (ClassNotFoundException|NoSuchMethodException e) { return null; }
   }
   
   private Constructor getConstructor(String txt) {
      int index = txt.indexOf('(');
      if (index < 0)
         return null;
      String[] params = txt.substring(index+1).replace(")", "").split(",");
      String path = txt.substring(0, index);
      try { 
         Class c = Class.forName(path);
         Class[] types = new Class[params.length];
         for (int t = 0; t < types.length; t++)
            types[t] = Class.forName(params[t].trim());
         return c.getConstructor(types);
      }
      catch (ClassNotFoundException|NoSuchMethodException e) { return null; }
   }

   private String getName(AnnotatedElement e) {
      switch (e.getClass().getName()) {
         case "java.lang.Class":
            return ((Class)e).getName();
         case "java.lang.Package":
            return ((Package)e).getName();
         case "java.lang.reflect.Field":
         case "java.lang.reflect.Constructor":
         case "java.lang.reflect.Method":
            Member m = (Member)e;
            return m.getDeclaringClass().getName() + "." + m.getName();
         case "java.lang.reflect.Parameter":
            return ((Parameter)e).getName();
         default:
            throw new RuntimeException("Unknown Java reference type: "+e.getClass().getName());
      }
   }
}
