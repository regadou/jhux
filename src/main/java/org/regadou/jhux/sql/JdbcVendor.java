package org.regadou.jhux.sql;

public class JdbcVendor {

   private String name;
   private boolean password;
   private String driver;
   private String quoteBegin;
   private String quoteEnd;
   private String alterColumn;
   private boolean hasBoolean;

   public JdbcVendor() {}

   public JdbcVendor(String name, boolean password, String driver, String quoteBegin, String quoteEnd, String alterColumn, boolean hasBoolean) {
      this.name = name;
      this.password = password;
      this.driver = driver;
      this.quoteBegin = (quoteBegin == null) ? "" : quoteBegin;
      this.quoteEnd = (quoteEnd == null) ? "" : quoteEnd;
      this.alterColumn = alterColumn;
      this.hasBoolean = hasBoolean;
   }

   public String getName() {
      return name;
   }

   public boolean isPassword() {
      return password;
   }

   public String getDriver() {
      return driver;
   }

   public String getQuoteBegin() {
      return quoteBegin;
   }

   public String getQuoteEnd() {
      return quoteEnd;
   }

   public String getAlterColumn() {
      return alterColumn;
   }

   public boolean isHasBoolean() {
      return hasBoolean;
   }
}
