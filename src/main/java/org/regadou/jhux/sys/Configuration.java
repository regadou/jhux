package org.regadou.jhux.sys;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.ConfigurationException;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.binder.ScopedBindingBuilder;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import org.regadou.jhux.JHUX;
import org.regadou.jhux.Namespace;
import org.regadou.jhux.lang.JavaPrinter;
import org.regadou.jhux.lang.JhuxParser;
import org.regadou.jhux.lang.JsonPrinter;
import org.regadou.jhux.Parser;
import org.regadou.jhux.Printer;
import org.regadou.jhux.Reference;
import org.regadou.jhux.impl.FormatImpl;
import org.regadou.jhux.lang.JhuxPrinter;
import org.regadou.jhux.lang.OpenNLPParser;
import org.regadou.jhux.ns.FileNamespace;
import org.regadou.jhux.ns.GeoLocation;
import org.regadou.jhux.ns.HttpNamespace;
import org.regadou.jhux.ns.NamespaceAdapter;
import org.regadou.jhux.ref.Value;
import org.regadou.jhux.sql.JdbcVendor;
import org.regadou.jhux.sql.SqlDatabase;

public class Configuration extends AbstractModule {
      
   private final Injector injector;
   private final Map<Class,Map<String,Object>> instancesMap = new LinkedHashMap<>();

   public Configuration() {
      injector = Guice.createInjector(this);
   }

   public <T> T getInstance(Class<T> type) {
      return getInstance(type, null);
   }

   public <T> T getInstance(Class<T> type, String id) {
      if (id == null) {
         try { return injector.getInstance(type); }
         catch (ConfigurationException e) {}     
      }
      Map<String,Object> instances = instancesMap.get(type);
      if (instances == null || instances.isEmpty())
         return null;
      if (id == null)
         return (T)instances.values().iterator().next();
      return (T)instances.get(id);
   }

   public void registerType(Class iface, Class impl, boolean singleton) {
      ScopedBindingBuilder builder = bind(iface).to(impl);
      if (singleton)
         builder.in(Singleton.class);
   }
   
   public void registerInstance(Class iface, Object instance, String id) {
      if (instance instanceof CharSequence)
         instance = createInstance(instance.toString());
      if (!iface.isInstance(instance))
         throw new RuntimeException("Not an instance of "+iface.getName()+": "+instance.getClass().getName());
      if (id == null)
         bind(iface).toInstance(instance);
      else {
         Map<String,Object> instances = instancesMap.get(iface);
         if (instances == null)
            instancesMap.put(iface, instances = new LinkedHashMap<>());
         instances.put(id, instance);
      }
   }

   @Override
   public void configure() {
      bind(Configuration.class).toInstance(this);
      bind(Converter.class).toInstance(new Converter(this));
      registerInstance(Parser.class, new JhuxParser(), "jhux");
      registerInstance(Parser.class, new OpenNLPParser(getEnglishDictionary(), true, true), "en");
      registerInstance(Printer.class, new JhuxPrinter(), "jhux");
      registerInstance(Printer.class, new JsonPrinter(this), "json");
      registerInstance(Printer.class, new JavaPrinter(), "java");
      FormatImpl.initFormats(this);
      
      for (Namespace ns : new Namespace[]{
         new FileNamespace(new File(System.getProperty("user.dir"))),
         new HttpNamespace("http"),
         new HttpNamespace("https"),
         new NamespaceAdapter("data", uri -> dataRequest(uri)),
         new NamespaceAdapter("geo", uri -> new Value(new GeoLocation(uri))),
         new NamespaceAdapter("mailto", uri -> mailtoRequest(uri)),
         new NamespaceAdapter("jdbc", uri -> new SqlDatabase(uri)),
         new NamespaceAdapter("tcp", uri -> {
            TcpServer server = new TcpServer(uri);
            server.run();
            return server;
         }),
      }) {
         registerInstance(Namespace.class, ns, ns.getPrefix());
      }
      
      for (JdbcVendor vendor : new JdbcVendor[]{
         new JdbcVendor("derby",       true,  "org.apache.derby.jdbc.EmbeddedDriver",                  "",   "",   "ALTER COLUMN",  false),
         new JdbcVendor("hsqldb",      false, "org.hsqldb.jdbcDriver",                                 "",   "",   "ALTER COLUMN",  true),
         new JdbcVendor("mysql",       true,  "com.mysql.jdbc.Driver",                                 "`",  "`",  "MODIFY COLUMN", true),
         new JdbcVendor("oracle",      false, "oracle.jdbc.driver.OracleDriver",                       "",   "",   "MODIFY COLUMN", false),
         new JdbcVendor("postgresql",  true,  "org.postgresql.Driver",                                 "\"", "\"", "ALTER COLUMN",  true),
         new JdbcVendor("access",      true,  "org.regadou.jmdb.MDBDriver",                            "[",  "]",  "ALTER COLUMN",  true),
         new JdbcVendor("sqlserver",   true,  "com.microsoft.sqlserver.jdbc.SQLServerDriver",          "\"", "\"", "ALTER COLUMN",  true),
         new JdbcVendor("sqlite",      true,  "org.sqlite.JDBC",                                       "\"", "\"", null,            true),
         new JdbcVendor("cassandra",   true,  "com.github.adejanovski.cassandra.jdbc.CassandraDriver", "",    "",  null,            true),
         new JdbcVendor("c*",          true,  "com.github.cassandra.jdbc.CassandraDriver",             "",    "",  null,            true),
      }) {
         registerInstance(JdbcVendor.class, vendor, vendor.getName());         
      }
      // TODO: register namespaces
   }

   @Provides
   public Context provideContext() {
      return JHUX.get(Context.class);
   }
     
   @Provides
   @Singleton
   public Gson provideGson() {
      return new GsonBuilder()
                     .setPrettyPrinting()
                     .setDateFormat("YYYY-MM-dd HH:mm:ss")
                     .serializeNulls()
                     .serializeSpecialFloatingPointValues()
                     .setLenient()
                     .registerTypeAdapter(Class.class, new GsonClassAdapter())
                     .registerTypeAdapter(File.class, new GsonStringableAdapter())
                     .registerTypeAdapter(URI.class, new GsonStringableAdapter())
                     .registerTypeAdapter(URL.class, new GsonStringableAdapter())
                     .create();
   }
   
   private Object createInstance(String txt) {
      throw new RuntimeException("Text configuration not supported yet");
   }
   
   private Map getEnglishDictionary() {
      return new TreeMap();
   }

   private static Reference dataRequest(String uri) {
      String originalUri = uri;
      uri = uri.substring("data:".length());
      int index = uri.indexOf(",");
      if (index < 0)
         throw new RuntimeException("Data uri missing comma delimited mimetype: "+originalUri);
      Map<String,String> options = mimetypeOptions(uri.substring(0, index));
      uri = uri.substring(index+1);
      byte[] bytes;
      if (options.containsKey("base64"))
         bytes = Base64.getDecoder().decode(uri);
      else {
         try { bytes = URLDecoder.decode(uri, options.get("charset")).getBytes(); }
         catch (UnsupportedEncodingException e) { bytes = URLDecoder.decode(uri).getBytes(); }
      }
      Object value = JHUX.get(Context.class).decode(new ByteArrayInputStream(bytes), options.get("type"), options.get("charset"));
      return (value instanceof Reference) ? (Reference)value : new Value(value);
   }
   
   private static Map<String,String> mimetypeOptions(String mimetype) {
      Map<String,String> options = new LinkedHashMap<>();
      String[] parts = mimetype.split(";");
      String type = null, charset = null;
      for (String part : parts) {
         String name, value;
         if (type == null) {
            name = "type";
            value = type = part.trim();
         }
         else {
            int index = part.indexOf('=');
            if (index < 0) {
               name = part.trim();
               value = "true";
            }
            else {
               name = part.substring(0, index).trim();
               value = part.substring(index+1).trim();
            }
            if (name.equals("charset"))
               value = charset = (value.equals("true") || value.trim().isEmpty()) ? FormatImpl.DEFAULT_CHARSET : value;
         }
         options.put(name, value);
      }
      if (charset == null)
         options.put("charset", FormatImpl.DEFAULT_CHARSET);
      return options;
   }

   private static Reference mailtoRequest(String uri) {
      throw new RuntimeException("URI scheme not yet implemented: mailto");
      /**
      uri = uri.substring("mailto:".length());
      String src, dst, subject, msg;
      Object att;
      Authentication auth = null;
      String host = parameter("smtp");
      String port = "25";
      int index = host.indexOf('@');
      if (index > 0) {
         String[] parts = host.substring(0, index).split(":");
         String password = (parts.length < 2) ? "" : parts[1];
         host = host.substring(index + 1);
         auth = new Authentication(parts[0], password);
      }
      index = host.indexOf(':');
      if (index > 0) {
         port = host.substring(index+1);
         host = host.substring(0, index);
      }
      
      Properties props = new Properties();
      props.put("mail.smtp.host", host);
      props.put("mail.smtp.port", port);
      props.put("mail.smtp.auth", auth != null);
      if (String.valueOf(parameter("starttls")).toLowerCase().equals("true"))
         props.put("mail.smtp.starttls.enable", "true");
      
      Session session = Session.getDefaultInstance(props, auth);      
      InternetAddress fad = new InternetAddress(src);
      InternetAddress tad = new InternetAddress(dst);
      MimeMessage message = new MimeMessage(session);
      message.setFrom(fad);
      message.addRecipient(Message.RecipientType.TO, tad);
      message.setSubject(subject);
      if (att != null) {
         MimeBodyPart part = new MimeBodyPart();
         part.setText(msg);
         MimeMultipart multi = new MimeMultipart();
         multi.addBodyPart(part);
         part = new MimeBodyPart();
         FileDataSource source = new FileDataSource(att.toString());
         part.setDataHandler(new DataHandler(source));
         part.setFileName(att.toString());
         multi.addBodyPart(part);
         message.setContent(multi);
      } else {
         message.setText(msg);
      }
      Transport.send(message);
      * **/
   }
}
