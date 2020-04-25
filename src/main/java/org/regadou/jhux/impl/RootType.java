package org.regadou.jhux.impl;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAmount;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.regadou.jhux.Function;
import org.regadou.jhux.JHUX;
import org.regadou.jhux.Reference;
import org.regadou.jhux.Type;
import org.regadou.jhux.sys.ClassIterator;
import org.regadou.jhux.sys.Context;

public enum RootType implements Type {

   ANY, NUMBER, FUNCTION, OBJECT, ARRAY, STRING; //TODO: we will eventually need the perception, tensor, dimension, measure type
   
   private static final Class[][] CLASSES = {
      {Reference.class, Map.Entry.class, Void.class, Throwable.class, InputStream.class, OutputStream.class, Reader.class, Writer.class}, 
      {Number.class, Boolean.class, boolean.class, byte.class, short.class, int.class, long.class, float.class, double.class, TemporalAmount.class}, 
      {Function.class, Executable.class},
      {Map.class, Type.class, Class.class, Date.class, Calendar.class, TemporalAccessor.class}, 
      {List.class, Iterable.class, Object[].class, Iterator.class, Enumeration.class, short[].class, int[].class, long[].class, float[].class, double[].class}, 
      {String.class, CharSequence.class, byte[].class, Byte[].class, char[].class, Character[].class, Character.class, char.class, File.class, URI.class, URL.class}
   };
   
   private static final Map<Class,Type> CLASSMAP = new LinkedHashMap<>();
   
   public static Type getType(Class c) {
      if (CLASSMAP.isEmpty()) {
         RootType types[] = values();
         for (int t = 0; t < CLASSES.length; t++) {
            Class[] classes = CLASSES[t];
            Type type = types[t];
            for (Class cl : classes)
               CLASSMAP.put(cl, type);
         }
      }
      if (c == null)
         c = Void.class;
      Type t = CLASSMAP.get(c);
      if (t != null)
         return t;
      ClassIterator it = new ClassIterator(c);
      while (it.hasNext()) {
         Class c2 = it.next();
         t = CLASSMAP.get(c2);
         if (t != null) {
            CLASSMAP.put(c, t);
            return t;
         }
      }
      for (Method m : c.getMethods()) {
         if (m.getParameterCount() > 0)
            continue;
         String name = m.getName();
         if (   (name.startsWith("is") && name.length() > 2 && Character.isUpperCase(name.charAt(2)))
            ||  (name.startsWith("get") && name.length() > 3 && Character.isUpperCase(name.charAt(3)))) {
            CLASSMAP.put(c, OBJECT);
            return OBJECT;
         }
      }
      return ANY;
   }

   @Override
   public String getName() {
      return name().toLowerCase();
   }
   
   @Override
   public Type getSuperType() {
      return ANY;
   }

   @Override
   public Function getConstructor() {
      return parameters -> {
         Object param;
         switch (parameters.length) {
            case 0:
               param = null;
               break;
            case 1:
               param = parameters[0];
               break;
            default:
               param = parameters;
         }
         //TODO: how to make a copy if parameter is already instance of this type
         return JHUX.get(Context.class).convert(param, CLASSES[ordinal()][0]);
      };
   }
   
   @Override
   public boolean isInstance(Object value) {
      if (this == ANY)
         return true;
      if (value == null)
         return false;
      return getType(value.getClass()) == this;
   }
}
