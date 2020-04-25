package org.regadou.jhux.sys;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import org.regadou.jhux.Expression;
import org.regadou.jhux.Parser;
import org.regadou.jhux.Printer;
import org.regadou.jhux.Format;
import org.regadou.jhux.JHUX;
import org.regadou.jhux.Reference;
import org.regadou.jhux.impl.FormatImpl;

public class Context {
   
   private transient Configuration config;
   private transient Converter converter;
   private transient Parser parser;
   private transient Printer printer;
   private Map dictionary;
   
   public Context(Object...params) {
      for (Object param : params)
         setProperty(param);
      if (config == null)
         config = JHUX.get(Configuration.class);
   }

   public Map getDictionary() {
      if (dictionary == null)
         dictionary = new TreeMap<>();
      return dictionary;
   }
   
   public <T> T convert(Object src, Class<T> type) {
      if (converter == null)
         converter = config.getInstance(Converter.class);
      return converter.convert(src, type);
   }
   
   public Expression parse(String txt) {
      if (parser == null)
         parser = config.getInstance(Parser.class);
      return parser.parse(txt);
   }

   public String print(Object value) {
      if (printer == null)
         printer = config.getInstance(Printer.class);
      return printer.print(value);
   }

   public Object decode(InputStream input, String type, String charset) {
      return getFormat(type).decode(input, (charset == null || charset.isEmpty()) ? FormatImpl.DEFAULT_CHARSET : charset);
   }

   public void encode(Object value, OutputStream output, String type, String charset) {
      getFormat(type).encode(value, output, (charset == null || charset.isEmpty()) ? FormatImpl.DEFAULT_CHARSET : charset);
   }
   
   private Format getFormat(String type) {
      Format format = config.getInstance(Format.class, type);
      return (format != null) ? format : config.getInstance(Format.class, type.startsWith("text/") ? FormatImpl.TEXT_MIMETYPE : FormatImpl.BINARY_MIMETYPE);      
   }
         
   private void setProperty(Object param) {
      if (param instanceof Configuration)
         config = (Configuration)param;
      else if (param instanceof Converter)
         converter = (Converter)param;
      else if (param instanceof Parser)
         parser = (Parser)param;
      else if (param instanceof Printer)
         printer = (Printer)param;
      else if (param instanceof Properties)
         setProperties((Properties)param);
      else if (param instanceof Map)
         dictionary = (Map)dictionary;
   }
   
   private void setProperties(Properties props) {
      for (String key : props.stringPropertyNames()) {
         try {
            // TODO: check that key is interface and value is class (optionally with id or constructor args)
         }
         catch (Exception e) {
            // TODO: check that value is class (optionally with constructor args)            
         }
      }
   }
   
   private Reference addData(Object target, Object data) {
      //TODO: check the class of target to know how to add: collection, array, map, namespace, number, string, reference, perception, tensor ...
      // also a post on a function should execute it
      return null;
   }
}
