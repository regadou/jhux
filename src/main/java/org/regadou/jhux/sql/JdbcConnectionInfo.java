package org.regadou.jhux.sql;

import java.util.LinkedHashMap;
import java.util.Map;
import org.regadou.jhux.JHUX;

public class JdbcConnectionInfo {

   private String url;
   private String user;
   private String password;
   private String host;
   private String database;
   private String error;
   private JdbcVendor vendor;

   public JdbcConnectionInfo() {}

   public JdbcConnectionInfo(String url) {
      String txt;
      String[] parts;
      if (url == null || url.trim().equals("") || !url.startsWith("jdbc:")) {
         this.error = "Invalid url " + url;
      } else {
         String name = url.split(":")[1];
         this.vendor = JHUX.get(JdbcVendor.class, name);
         if (this.vendor == null) {
            this.error = "Cannot find vendor for url " + url;
         } else {
            int i = url.indexOf('?');
            if (i > 0) {
               Map<String, String> params = getParameters(url.substring(i + 1).split("\\&"));
               if (params.containsKey("user")) {
                  this.user = params.get("user");
                  this.password = params.containsKey("password") ? params.get("password") : "";
                  this.url = url.substring(0, i);
               }
            }
            if (this.url == null) {
               int i2 = url.indexOf('@');
               if (i2 > 0 && (i < 0 || i2 < i)) {
                  txt = url.substring(0, i2);
                  int start = txt.lastIndexOf('/');
                  parts = txt.substring(start + 1).split(":");
                  this.user = parts[0];
                  this.password = (parts.length == 1) ? "" : parts[1];
                  this.url = url.substring(0, start + 1) + url.substring(i2 + 1);
               } else {
                  this.url = url;
                  this.user = this.password = "";
               }
            }
            i = this.url.lastIndexOf('/');
            this.database = this.url.substring(i + 1);
            txt = this.url.substring(0, i);
            i = txt.lastIndexOf('/');
            parts = ((i < 0) ? txt : txt.substring(i + 1)).split(":");
            try {
               Integer.parseInt(parts[parts.length - 1]);
               this.host = parts[parts.length - 2];
            } catch (Exception e) {
               this.host = parts[parts.length - 1];
            }
         }
      }
   }

   public String getUrl() {
      return url;
   }

   public String getUser() {
      return user;
   }

   public String getPassword() {
      return password;
   }

   public String getHost() {
      return host;
   }

   public String getDatabase() {
      return database;
   }

   public String getError() {
      return error;
   }

   public JdbcVendor getVendor() {
      return vendor;
   }

   private Map<String, String> getParameters(String[] parts) {
      Map<String, String> map = new LinkedHashMap<>();
      for (String part : parts) {
         int eq = part.indexOf('=');
         if (eq > 0) {
            map.put(part.substring(0, eq), part.substring(eq + 1));
         }
      }
      return map;
   }
}
