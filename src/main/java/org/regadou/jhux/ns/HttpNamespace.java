package org.regadou.jhux.ns;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.HttpURLConnection;
import org.regadou.jhux.Format;
import org.regadou.jhux.JHUX;
import org.regadou.jhux.Namespace;
import org.regadou.jhux.Reference;
import org.regadou.jhux.impl.FormatImpl;
import org.regadou.jhux.impl.RestFunction;
import org.regadou.jhux.ref.Value;
import org.regadou.jhux.sys.Context;

public class HttpNamespace implements Namespace {

   public static final String DEFAULT_MIMETYPE = "application/json";
   private static final String USER_AGENT = "JHUX";
   
   private String prefix;
   
   public HttpNamespace(String prefix) {
      this.prefix = prefix;
   }
   
   @Override
   public String getPrefix() {
      return prefix;
   }

   @Override
   public String getUri() {
      return prefix+"://localhost/";
   }

   @Override
   public Reference get(String uri) {
      return request(RestFunction.GET, getUrl(uri), null);
   }

   @Override
   public Reference post(String uri, Object data) {
      return request(RestFunction.POST, getUrl(uri), data);
   }

   @Override
   public Reference put(String uri, Object data) {
      return request(RestFunction.PUT, getUrl(uri), data);
   }

   @Override
   public int delete(String uri) {
      Reference result = request(RestFunction.DELETE, getUrl(uri), null);
      Number n = JHUX.get(Context.class).convert(result, Number.class);
      return (n == null) ? 0 : n.intValue();
   }
   
   @Override
   public String toString() {
      return getUri();
   }   
      
   private static Reference request(RestFunction method, URL url, Object data) {
      try {
         HttpURLConnection con = (HttpURLConnection) url.openConnection();
         con.setRequestMethod(method.name());
	 con.setRequestProperty("User-Agent", USER_AGENT);
         if (data != null)
            sendData(con, data);

         InputStream input = con.getInputStream();
         Object result = JHUX.get(Context.class).decode(input, getMimetype(url, con), con.getContentEncoding());
         input.close();
         return (result instanceof Reference) ? (Reference)result : new Value(result);
      }
      catch (IOException e) { throw new RuntimeException(e); }
   }
   
   private static URL getUrl(String uri) {
      try { return new URL(uri); }
      catch (Exception e) { throw new RuntimeException(e); }
   }
   
   private static String getMimetype(URL url, HttpURLConnection con) {
      String mimetype = con.getContentType();
      if (mimetype == null || mimetype.isEmpty())
         mimetype = FormatImpl.TEXT_MIMETYPE;
      else if (mimetype.equals("content/unknown")) {
         String[] parts = url.toString().split("#")[0].split("\\?")[0].split("/");
         String last = parts[parts.length-1];
         if (last.isEmpty())
            mimetype = null;
         else {
            int dot = last.lastIndexOf('.');
            if (dot > 0) {
               Format format = JHUX.get(FormatImpl.class, last.substring(dot+1).toLowerCase());
               if (format != null)
                  mimetype = format.getName();
            }
         }
      }
      return (mimetype != null) ? mimetype : FormatImpl.BINARY_MIMETYPE;
   }
   
   private static void sendData(HttpURLConnection con, Object data) {
      OutputStream output = null;
      try {
         Format format = JHUX.get(Format.class, DEFAULT_MIMETYPE);
         con.setDoOutput(true);
         con.setRequestProperty("Content-type", format.getName());
         output = con.getOutputStream();
         format.encode(data, output, null);
         output.flush();
      }
      catch (IOException e) { throw new RuntimeException(e); }
      finally {
         try {
            if (output != null)
               output.close();
         }
         catch (IOException e) {}
      }
   }
}
