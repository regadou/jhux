package org.regadou.jhux.impl;

import com.esotericsoftware.yamlbeans.YamlException;
import com.esotericsoftware.yamlbeans.YamlReader;
import com.esotericsoftware.yamlbeans.YamlWriter;
import com.google.gson.Gson;
import ezvcard.Ezvcard;
import ezvcard.VCard;
import ezvcard.property.Address;
import ezvcard.property.StructuredName;
import ezvcard.property.Telephone;
import ezvcard.property.TextListProperty;
import ezvcard.property.TextProperty;
import ezvcard.property.VCardProperty;
import java.awt.image.RenderedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.BiFunction;
import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.regadou.jhux.Format;
import org.regadou.jhux.JHUX;
import org.regadou.jhux.Parser;
import org.regadou.jhux.sys.Configuration;
import org.regadou.jhux.sys.Context;
import org.xml.sax.SAXException;

public class FormatImpl implements Format {

   public static final String DEFAULT_CHARSET = "utf-8";
   private static final String MIMETYPES_FILE = "/mimetypes.txt";
   public static final String TEXT_MIMETYPE = "text/plain";
   public static final String BINARY_MIMETYPE = "application/octet-stream";
   public static final String DIRECTORY_MIMETYPE = "inode/directory";
   public static final String[] YAML_MIMETYPES = {
        "text/yaml",
        "text/x-yaml",
        "application/x-yaml",
        "text/vnd.yaml"
   };
   private static final CSVFormat DEFAULT_CSV_FORMAT = CSVFormat.EXCEL;
   private static final int CSV_MINIMUM_CONSECUTIVE_FIELDS_SIZE = 5;
   
   @FunctionalInterface
   public static interface TriConsumer<T,U,V> {
      void accept(T t, U u, V v);
   }
   
   public static void initFormats(Configuration config) {
      Map<String,Format> formats = registerFormats(config);
      registerExtensions(config, formats);
   }
   
   private String name;
   private BiFunction<InputStream,String,Object> decoder;
   private TriConsumer<Object,OutputStream,String> encoder;

   public FormatImpl(String name, BiFunction<InputStream,String,Object> decoder, TriConsumer<Object,OutputStream,String> encoder) {
      this.name = name;
      this.decoder = decoder;
      this.encoder = encoder;
   }

   @Override
   public String getName() {
      return name;
   }

   @Override
   public Object decode(InputStream input, String charset) {
      if (charset == null)
         charset = DEFAULT_CHARSET;
      return decoder.apply(input, charset);
   }

   @Override
   public void encode(Object value, OutputStream output, String charset) {
      if (charset == null)
         charset = DEFAULT_CHARSET;
      encoder.accept(value, output, charset);
      try { output.flush(); }
      catch (IOException e) {}
   }
   
   @Override
   public String toString() {
      return name;
   }   
   
   private static void registerExtensions(Configuration config, Map<String,Format> formats) {
      InputStream input = Configuration.class.getResourceAsStream(MIMETYPES_FILE);
      if (input == null)
         throw new RuntimeException("Missing file " + MIMETYPES_FILE + " from the resources location");
      for (String line : readString(input, DEFAULT_CHARSET).toString().split("\n")) {
         int index = line.indexOf('#');
         if (index >= 0)
            line = line.substring(0, index);
         line = line.trim();
         if (line.isEmpty())
            continue;
         List<String> tokens = new ArrayList<>();
         StringTokenizer tokenizer = new StringTokenizer(line);
         while (tokenizer.hasMoreTokens())
            tokens.add(tokenizer.nextToken());
         String mimetype = tokens.remove(0);
         Format format = formats.get(mimetype);
         if (format != null) {
            for (String token : tokens)
               config.registerInstance(Format.class, format, token);
         }
      }
   }
   
   private static Map<String,Format> registerFormats(Configuration config) {
      Map<String,Format> formats = new HashMap<>();
      
      for (String mimetype : ImageIO.getReaderMIMETypes()) {
         if (!mimetype.startsWith("x-"))
            addFormat(formats, mimetype,
               (input, charset) -> readImage(input),
               (value, output, charset) -> writeImage(output, value, mimetype)                    
            );
      }
      
      addFormat(formats, BINARY_MIMETYPE, (input, charset) -> {
         return readBytes(input);
      }, (value, output, charset) -> {
            if (value instanceof byte[])
               writeBytes(output, (byte[])value);
            else if (value == null) 
               writeBytes(output, new byte[0]);
            else
              writeString(output, String.valueOf(value), charset);
      });

      addFormat(formats, TEXT_MIMETYPE, (input, charset) -> {
         return readString(input, charset);
      }, (value, output, charset) -> {
            writeString(output, String.valueOf(value), charset);
      });

      addFormat(formats, DIRECTORY_MIMETYPE, (input, charset) -> {
         //TODO: what to return from reading a folder ?
         return null;
      }, (value, output, charset) -> {
         //TODO: how to add items to a folder ?
      });

      addFormat(formats, "application/json", (input, charset) -> {
         try { return JHUX.get(Gson.class).fromJson(new InputStreamReader(input, charset), Object.class); }
         catch (UnsupportedEncodingException e) { throw new RuntimeException(e); }
      }, (value, output, charset) -> {
         writeString(output, charset, JHUX.get(Gson.class).toJson(value));
      });

      addFormat(formats, YAML_MIMETYPES, (input, charset) -> {
         try {
            Reader reader = new InputStreamReader(input, charset);
            return new YamlReader(reader).read();
         }
         catch (YamlException | UnsupportedEncodingException e) { throw new RuntimeException(e); }
      }, (value, output, charset) -> {
         try {
            Writer writer = new OutputStreamWriter(output, charset);
            YamlWriter ywriter = new YamlWriter(writer);
            ywriter.write(value);
            ywriter.close();
         }
         catch (YamlException | UnsupportedEncodingException e) { throw new RuntimeException(e); }
      });

      addFormat(formats, "text/x-jhux", (input, charset) -> {
         return JHUX.get(Parser.class, "jhux").parse(readString(input, charset));
      }, (value, output, charset) -> {
         writeString(output, charset, JHUX.get(Gson.class).toJson(value));
      });

      addFormat(formats, "text/x-java-properties", (input, charset) -> {
         Properties p = new Properties();
         try { p.load(new InputStreamReader(input, charset)); }
         catch (IOException e) { throw new RuntimeException(e); }
         return p;
      }, (value, output, charset) -> {
         Properties p;
         if (value instanceof Properties)
            p = (Properties)value;
         else if (value == null)
            p = new Properties();
         else {
            Map map = JHUX.get(Context.class).convert(value, Map.class);
            p = new Properties();
            for (Object key : map.keySet())
               p.setProperty(String.valueOf(key), String.valueOf(value));
         }
         try { p.store(new OutputStreamWriter(output, charset), ""); }
         catch (IOException e) { throw new RuntimeException(e); }
      });

      addFormat(formats, "application/xml", (input, charset) -> {
         try { return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(input); }
         catch (ParserConfigurationException|SAXException|IOException e) { throw new RuntimeException(e); }
      }, (value, output, charset) -> {
         try {
            TransformerFactory.newInstance().newTransformer().transform(
                 new DOMSource(JHUX.get(Context.class).convert(value, org.w3c.dom.Document.class)),
                 new StreamResult(output)
            );
         }
         catch (TransformerException e) { throw new RuntimeException(e); }
      });

/**
      addFormat(formats, "text/html", (input, charset) -> {
         return Jsoup.parse(readString(input, charset));
      }, (value, output, charset) -> {
         writeString(output, charset, String.valueOf(JHUX.get(Context.class).convert(value, org.jsoup.nodes.Document.class)));
      });
**/
         
      addFormat(formats, "text/csv", (input, charset) -> {
         try {
            Collection dst = new ArrayList();
            CSVParser parser = DEFAULT_CSV_FORMAT.parse(new InputStreamReader(input, charset));
            List<String> fields = null;
            for (CSVRecord record : parser) {
               if (fields == null) {
                  fields = new ArrayList<>();
                  Iterator<String> it = record.iterator();
                  while (it.hasNext())
                     fields.add(it.next());
               }
               else {
                  Map map = new LinkedHashMap();
                  int n = Math.min(fields.size(), record.size());
                  for (int f = 0; f < n; f++)
                     map.put(fields.get(f), record.get(f));
                  dst.add(map);
               }
            }
            return dst;
         }
         catch (IOException e) { throw new RuntimeException(e); }
      }, (value, output, charset) -> {
         try {
            Context cx = JHUX.get(Context.class);
            CSVPrinter printer = DEFAULT_CSV_FORMAT.print(new OutputStreamWriter(output, charset));
            Map[] records = cx.convert(value, Map[].class);
            Set<String> fields = new LinkedHashSet<>();
            int lastSize = 0;
            int consecutives = 0;
            for (Map record : records) {
               for (Object key : record.keySet())
                  fields.add(String.valueOf(key));
               if (lastSize == fields.size())
                  consecutives++;
               else {
                  consecutives = 0;
                  lastSize = fields.size();
               }
               if (consecutives >= CSV_MINIMUM_CONSECUTIVE_FIELDS_SIZE)
                  break;
            }               
            printer.printRecord(fields);
            for (Map record : records) {
               for (Object field : fields) {
                  Object cell = record.get(field);
                  printer.print((cell == null) ? "" : cx.print(cell));
               }
               printer.println();
            }
         }
         catch (IOException e) { throw new RuntimeException(e); }
      });

      addFormat(formats, "text/vcard", (input, charset) -> {
         Collection<Map> rows = new TreeSet<>((a, b) -> stringCompare(a.get("FormattedName"), b.get("FormattedName")) );
         try {
            for (VCard card : Ezvcard.parse(input).all()) {
               Map row = new TreeMap<>();
               Iterator<VCardProperty> properties = card.iterator();
               while (properties.hasNext()) {
                  VCardProperty property = properties.next();
                  String name = property.getClass().getSimpleName();
                  String value = getVcardPropertyValue(property);
                  Object old = row.get(name);
                  if (old == null)
                     row.put(name, value);
                  else if (old instanceof Collection)
                     ((Collection)old).add(value);
                  else
                     row.put(name, new LinkedHashSet(Arrays.asList(old, value)));
               }
               rows.add(row);
            }
            return rows;
         }
         catch (IOException e) { throw new RuntimeException(e); }
      }, (value, output, charset) -> {
            throw new RuntimeException("Vcard encoding is not yet implemented");
      });
                    
      for (Format format : formats.values())
         config.registerInstance(Format.class, format, format.getName());
      
      return formats;
   }
   
   private static void addFormat(Map<String,Format> formats, String[] names, BiFunction<InputStream,String,Object> decoder, TriConsumer<Object,OutputStream,String> encoder) {
      Format format = new FormatImpl(names[0], decoder, encoder);
      for (String name : names)
         formats.put(name, format);
   }
   
   private static void addFormat(Map<String,Format> formats, String name, BiFunction<InputStream,String,Object> decoder, TriConsumer<Object,OutputStream,String> encoder) {
      formats.put(name, new FormatImpl(name, decoder, encoder));
   }
   
   private static Object readImage(InputStream input) {
      try { return ImageIO.read(input); }
      catch (IOException e) { throw new RuntimeException(e); }
   }
   
   private static void writeImage(OutputStream output, Object value, String mimetype) {
      //TODO: probably different process if we save to SVG
      try { ImageIO.write(JHUX.get(Context.class).convert(value, RenderedImage.class), mimetype, output); }
      catch (IOException e) { throw new RuntimeException(e); }
   }
   
   private static byte[] readBytes(InputStream input) {
      try {
         byte[] bytes = new byte[1024];
         ByteArrayOutputStream buffer = new ByteArrayOutputStream();
         for (int got = 0; got >= 0; ) {
            got = input.read(bytes);
            if (got > 0)
               buffer.write(bytes, 0, got);
         }
         return buffer.toByteArray();
      }
      catch (IOException e) { throw new RuntimeException(e); }            
   }
   
   private static void writeBytes(OutputStream output, byte[] bytes) {
      try { output.write(bytes); }
      catch (IOException e) { throw new RuntimeException(e); }
   }
   
   private static String readString(InputStream input, String charset) {
      try { return new String(readBytes(input),  charset); }
      catch (IOException e) { throw new RuntimeException(e); }
   }
   
   private static void writeString(OutputStream output, String charset, String txt) {
      try { output.write(txt.getBytes(charset)); }
      catch (IOException e) { throw new RuntimeException(e); }
   }
   
   private static String getVcardPropertyValue(VCardProperty property) {
      String value;
      if (property instanceof TextProperty)
         value = ((TextProperty)property).getValue();
      else if (property instanceof Address)
         value = ((Address)property).getStreetAddressFull();
      else if (property instanceof StructuredName) {
         StructuredName sname = (StructuredName)property;
         List<String> names = new ArrayList<>();
         names.add(sname.getGiven());
         names.addAll(sname.getAdditionalNames());
         String family = sname.getFamily();
         if (family != null && !family.isEmpty())
            names.add(family);
         value = names.isEmpty() ? null : String.join(" ", names);
      }
      else if (property instanceof TextListProperty) {
         List<String> values = ((TextListProperty)property).getValues();
         value = values.isEmpty() ? null : String.join(" ", values);
      }
      else if (property instanceof Telephone)
         value = ((Telephone)property).getText();
      else
         value = property.getParameters().toString();
      
      return (value == null) ? "" : value.trim();
   }
   
   private static int stringCompare(Object o1, Object o2) {
      String s1 = (o1 == null) ? "" : o1.toString().trim();
      String s2 = (o2 == null) ? "" : o2.toString().trim();
      return s1.compareToIgnoreCase(s2);
   }
}
