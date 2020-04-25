package org.regadou.jhux.sql;

import org.regadou.jhux.Reference;

public class SqlColumn implements Reference {

   private SqlTable table;
   private String name;
   private Class type;
   private boolean primaryKey;

   public SqlColumn(String name, Class type, boolean primaryKey) {
      this(null, name, type, primaryKey);
   }

   public SqlColumn(SqlTable table, String name, Class type, boolean primaryKey) {
      this.table = table;
      this.name = name;
      this.type = type;
      this.primaryKey = primaryKey;
   }

   @Override
   public Object getOwner() {
      return table;
   }

   @Override
   public String getKey() {
      return name;
   }

   @Override
   public Object getValue() {
      return type;
   }

   public boolean isPrimaryKey() {
      return primaryKey;
   }
   
   protected void setTable(SqlTable table) {
      this.table = table;
   }
}
