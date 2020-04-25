package org.regadou.jhux.sys;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Executable;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.beanutils.BeanMap;
import org.regadou.jhux.Function;
import org.regadou.jhux.JHUX;
import org.regadou.jhux.Reference;
import org.regadou.jhux.impl.ExecutableFunction;

public class Converter {
   
   private static enum DatePart {YEAR, MONTH, DAY, HOUR, MINUTE, SECOND, MILLISECOND};
   private static final String DATE_SEPARATORS = " \ttT,_";
   private static final String DEFAULT_PROPERTY_NAME = "value";
   private static final Map<Class,Class> PRIMITIVES_MAP = new LinkedHashMap<>();
   static {
      PRIMITIVES_MAP.put(Boolean.TYPE, Boolean.class);
      PRIMITIVES_MAP.put(Byte.TYPE, Byte.class);
      PRIMITIVES_MAP.put(Short.TYPE, Short.class);
      PRIMITIVES_MAP.put(Integer.TYPE, Integer.class);
      PRIMITIVES_MAP.put(Long.TYPE, Long.class);
      PRIMITIVES_MAP.put(Float.TYPE, Float.class);
      PRIMITIVES_MAP.put(Double.TYPE, Double.class);
      PRIMITIVES_MAP.put(Character.TYPE, Character.class);
   }
   
   private Configuration config;
   
   public Converter(Configuration config) {
      this.config = config;
   }
   
   public <T> T convert(Object src, Class<T> type) {
      if (type == null || type == Object.class)
         return (T)src;
      if (src != null && type.isAssignableFrom(src.getClass()))
         return (T)src;
      if (type.isArray())
         return (T)toArray(JHUX.unref(src), type.getComponentType());
      if (type.isEnum())
         return (T)toEnum(JHUX.unref(src), type);
      if (type.isPrimitive())
         type = PRIMITIVES_MAP.get(type);
      
      if (type.isAssignableFrom(Reference.class))
         return (T)JHUX.ref(src);
      if (type.isAssignableFrom(URI.class))
         return (T)toURI(src);
      src = JHUX.unref(src);
      
      if (type.isAssignableFrom(Boolean.class))
         return (T)toBoolean(src);
      if (type.isAssignableFrom(Number.class))
         return (T)toNumber(src);
      if (type.isAssignableFrom(Function.class))
         return (T)toFunction(src);
      if (type.isAssignableFrom(Map.class))
         return (T)toMap(src);
      if (type.isAssignableFrom(List.class))
         return (T)toList(src);
      if (type.isAssignableFrom(Collection.class))
         return (T)toCollection(src);
      if (type.isAssignableFrom(String.class))
         return (T)toString(src);
      if (type.isAssignableFrom(CharSequence.class))
         return (T)toCharSequence(src);
      if (type.isAssignableFrom(Date.class))
         return (T)toDate(src);
      if (type.isAssignableFrom(Class.class))
         return (T)toClass(src);
      if (type.isAssignableFrom(Map.Entry.class))
         return (T)toMapEntry(src);
      
      //TODO: check for type constructor with a single parameter which is type of src (or no-arg if src == null)
      throw new RuntimeException("Don't know how to convert to "+type.getName());
   }
    
   private URI toURI(Object value) {
      if (value instanceof URI)
         return (URI)value;
      if (value instanceof URL) {
         try { return ((URL)value).toURI(); }
         catch (URISyntaxException e) { throw new RuntimeException(e); }         
      }
      if (value instanceof File)
         return ((File)value).toURI();
      if (value instanceof CharSequence) {
         try { return new URI(value.toString()); }
         catch (URISyntaxException e) { return null; }
      }
      if (value instanceof char[])
         return toURI(new String((char[])value));
      if (value instanceof byte[])
         return toURI(new String((byte[])value));
      if (value instanceof Reference)
         return toURI(((Reference)value).getKey());
      return null;
   }
  
   private Boolean toBoolean(Object value) {
      if (value instanceof Boolean)
         return (Boolean)value;
      if (value instanceof Number)
         return ((Number)value).doubleValue() != 0;
      if (value instanceof Collection)
         return toBoolean(((Collection)value).size());
      if (value instanceof Map)
         return toBoolean(((Map)value).size());
      if (value == null)
         return false;
      if (value.getClass().isArray())
         return toBoolean(Array.getLength(value));
      String txt = value.toString();
      if (txt.equalsIgnoreCase("true"))
         return true;
      if (txt.equalsIgnoreCase("false"))
         return false;
      try { return toBoolean(Double.parseDouble(txt)); }
      catch (Exception e) {}
      return null;
   }

   private Number toNumber(Object value) {
      if (value instanceof Number)
         return (Number)value;
      if (value instanceof Boolean)
         return (byte)((Boolean)value ? 1 : 0);
      if (value instanceof CharSequence) {
         String text = String.join("", value.toString().split("$")).trim();
         try {
            if (text.equalsIgnoreCase("true"))
               return (byte)1;
            if (text.equalsIgnoreCase("false"))
               return (byte)0;
            if (text.startsWith("0x"))
               return Long.parseLong(text, 16);
            if (text.indexOf(' ') > 0) {
               text = String.join(".", String.join("", text.split(" ")).split(","));
               return  (text.indexOf('.') >= 0) ? new Double(text) : new Long(text);
            }
            if (text.indexOf(',') >= 0) {
               text = String.join("", text.split(","));
               return  (text.indexOf('.') >= 0) ? new Double(text) : new Long(text);               
            }
            if (text.indexOf('.') >= 0 || text.indexOf('e') > 0 || text.indexOf('E') > 0)
               return new Double(text);
            return new Long(text);
         }
         catch (Exception e) { return null; }
      }
      if (value instanceof Map) {
         Map map = (Map)value;
         if (map.size() == 1) {
            Number n = toNumber(map.values().iterator().next());
            if (n != null)
               return n;
         }
         return 1;
      }
      if (value instanceof Collection) {
         Collection c = (Collection)value;
         if (c.size() != 1)
            return c.size();
         Number n = toNumber(c.iterator().next());
         return (n == null) ? 1 : n;
      }
      if (value == null)
         return 0;
      if (value.getClass().isArray()) {
         int length = Array.getLength(value);
         if (length != 1)
            return length;
         Number n = toNumber(Array.get(value, 0));
         return (n == null) ? 1 : n;
      }
      return null;
   }
   
   private Function toFunction(Object value) {
      if (value instanceof Function)
         return (Function)value;
      if (value instanceof Executable)
         return new ExecutableFunction((Executable)value);
      if (value instanceof CharSequence)
         return config.getInstance(Function.class, value.toString());
      return null;
   }
   
   private Map toMap(Object value) {
      if (value instanceof Map)
         return (Map)value;
      if (value instanceof Collection) {
         Map map = new LinkedHashMap();
         for (Object e : (Collection)value)
            map.put(map.size(), e);
         return map;
      }
      if (value instanceof CharSequence) {
         Map map = new LinkedHashMap();
         addEntry(map, value);
         return map;
      }
      if (value == null)
         return new LinkedHashMap();
      if (value.getClass().isArray()) {
         Map map = new LinkedHashMap();
         int length = Array.getLength(value);
         for (int i = 0; i < length; i++)
            map.put(i, Array.get(value, i));
         return map;
      }
      return new BeanMap(value);
   }

   private Collection toCollection(Object value) {
      if (value instanceof Collection)
         return (Collection)value;
      if (value instanceof Object[])
         return Arrays.asList((Object[])value);
      if (value == null)
         return new ArrayList();
      if (value.getClass().isArray()) {
         int length = Array.getLength(value);
         List dst = new ArrayList(length);
         for (int i = 0; i < length; i++)
            dst.add(Array.get(value, i));
         return dst;
      }
      List lst = new ArrayList();
      lst.add(value);
      return lst;
   }
   
   private List toList(Object value) {
      if (value instanceof List)
         return (List)value;
      Collection c = toCollection(value);
      return (c instanceof List) ? (List)c : new ArrayList(c);
   }

   private Object toArray(Object value, Class type) {
      Object array;
      if (value == null)
         return Array.newInstance(type, 0);
      if (value.getClass().isArray())
         array = value;
      else if (value instanceof Collection)
         array = ((Collection)value).toArray();
      else
         array = new Object[]{value};

      if (type == null || type.isAssignableFrom(array.getClass().getComponentType()))
         return array;
      int length = Array.getLength(array);
      Object dst = Array.newInstance(type, length);
      for (int i = 0; i < length; i++)
         Array.set(dst, i, convert(Array.get(array, i), type));
      return dst;
   }
   
   private Object toEnum(Object value, Class type) {
      if (value == null)
         return null;
      String name = value.toString();
      for (Object e : EnumSet.allOf(type)) {
         if (((Enum)e).name().equals(name))
            return e;
      }
      return null;
   }

   private CharSequence toCharSequence(Object value) {
      if (value instanceof CharSequence)
         return (CharSequence)value;
      return toString(value);
   }

   private String toString(Object value) {
      if (value instanceof CharSequence || value instanceof URI || value instanceof URL)
         return value.toString();
      if (value instanceof File)
         return ((File)value).toURI().toString();
      if (value instanceof char[])
         return new String((char[])value);
      if (value instanceof byte[])
         return new String((byte[])value);
      if (value instanceof Calendar)
         value = ((Calendar)value).getTime();
      if (value instanceof Date) {
         if (value instanceof java.sql.Date || value instanceof Time || value instanceof Timestamp)
            return value.toString();
         return new Timestamp(((Date)value).getTime()).toString();
      }
      if (value instanceof Class)
         return ((Class)value).getName();
      if (value instanceof Map.Entry) {
         Map.Entry e = (Map.Entry)value;
         return toString(e.getKey())+"="+toString(e.getValue());
      }
      if (value != null && value.getClass().isEnum())
         return value.toString();
      return JHUX.get(Context.class).print(value);
   }

   private Date toDate(Object value) {
      if (value instanceof Calendar)
         value = ((Calendar)value).getTime();
      if (value instanceof Date) {
         if (value.getClass() == Date.class)
            return new Timestamp(((Date)value).getTime());
         return (Date)value;
      }
      if (value instanceof Number)
         return new Timestamp(((Number)value).longValue());
      if (value == null)
         return new Timestamp(System.currentTimeMillis());
      if (value instanceof Collection)
         value = ((Collection)value).toArray();
      if (value.getClass().isArray())
         return createDate(value, (Array.getLength(value) < 4) ? java.sql.Date.class : Timestamp.class);
      
      String txt = value.toString().trim();
      String[] parts = null;
      for (char c : DATE_SEPARATORS.toCharArray()) {
         int i = txt.indexOf(c);
         if (i > 0) {
            parts = new String[]{txt.substring(0, i).trim(), txt.substring(i+1).trim()};
            break;
         }
      }
      if (parts != null) {
         List lst = new ArrayList();
         lst.addAll(Arrays.asList(parts[0].split("-")));
         while (lst.size() < 3)
            lst.add(1);
         lst.addAll(Arrays.asList(parts[1].split(":")));
         return createDate(lst.toArray(), Timestamp.class);
      }
      if (txt.indexOf(':') > 0) {
         List lst = new ArrayList();
         lst.add(1970);
         while (lst.size() < 3)
            lst.add(1);
         return createDate(txt.split(":"), Time.class);
      }
      return createDate(txt.split("-"), java.sql.Date.class);
      
   }

   private Class toClass(Object value)  {
      if (value instanceof Class)
         return (Class)value;
      if (value == null)
         return Void.class;
      String className = value.toString().trim();
      try { return Class.forName(className); }
      catch (ClassNotFoundException e) {
         // we might wanna scan the classpath
         return value.getClass();
      }
   }

   private Map.Entry toMapEntry(Object value) {
      if (value instanceof Map.Entry)
         return (Map.Entry)value;
      if (value instanceof Map && ((Map)value).size() == 1)
         return (Map.Entry)((Map)value).entrySet().toArray()[0];
      return new AbstractMap.SimpleEntry(DEFAULT_PROPERTY_NAME, value);
   }
   
   private void addEntry(Map map, Object value) {
      Map.Entry e = toMapEntry(value);
      Object key = e.getKey();
      if (key == null)
         key = map.size();
      map.put(key, value);
   }
   
   private Date createDate(Object src, Class type) {
      Date date;
      try { date = (Date)type.getConstructor(Long.TYPE).newInstance(0); }
      catch (Exception e) { throw new RuntimeException(e); }
      int length = Math.min(6, Array.getLength(src));
      for (int i = 0; i < length; i++) {
         Number n = toNumber(Array.get(src, i));
         if (n == null)
            n = (i < 3) ? 1 : 0;
         int v = n.intValue();
         switch (DatePart.values()[i]) {
            case YEAR:
               date.setYear(v);
               break;
            case MONTH:
               date.setMonth(v+1);
               break;
            case DAY:
               date.setDate(v);
               break;
            case HOUR:
               date.setHours(v);
               break;
            case MINUTE:
               date.setMinutes(v);
               break;
            case SECOND:
               date.setSeconds(v);
               break;
         }
      }
      return date;
   }
}
