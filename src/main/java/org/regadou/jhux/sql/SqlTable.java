package org.regadou.jhux.sql;

import java.lang.reflect.Array;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.Predicate;
import org.regadou.jhux.Expression;
import org.regadou.jhux.JHUX;
import org.regadou.jhux.Reference;
import org.regadou.jhux.impl.Operator;
import org.regadou.jhux.ref.Property;
import org.regadou.jhux.sys.Context;

public class SqlTable implements Reference {

   private transient SqlDatabase database;
   private String databaseUri;
   private String name;
   private Map<String,SqlColumn> columns;
   private String[] primaryKeys;

   public SqlTable() {}

   public SqlTable(String name, Collection<SqlColumn> columns, SqlDatabase database) {
      this.name = name;
      this.database = database;
      databaseUri = database.getKey();
      this.columns = new LinkedHashMap<>();
      List<String> pkeys = new ArrayList<>();
      for (SqlColumn col : columns) {
         this.columns.put(col.getKey(), col);
         if (col.isPrimaryKey())
            pkeys.add(col.getKey());
         col.setTable(this);
      }
      if (pkeys.isEmpty())
         pkeys.add(columns.iterator().next().getKey());
      primaryKeys = pkeys.toArray(new String[pkeys.size()]);
   }

   @Override
   public Object getOwner() {
      return database;
   }

   @Override
   public String getKey() {
      return name;
   }

   @Override
   public Object getValue() {
      return columns;
   }

   public Object getInstance(Object... src) {
      Object[] keys;
      if (src == null || src.length == 0)
         return null;
      if (src.length == 1) {
         Object value = JHUX.unref(src[0]);
         if (value instanceof Map) {
            Map map = (Map)value;
            keys = extractKeys(map);
            return (keys == null) ? insertRow(map) : updateRow(keys, map);
         }
         else if (value instanceof Collection)
            keys = ((Collection)value).toArray();
         else if (value instanceof Object[])
            keys = (Object[])value;
         else if (value != null && value.getClass().isArray())
            keys = JHUX.get(Context.class).convert(value, Object[].class);
         else 
            keys = new Object[]{value};
      }
      else
         keys = src;
      String sql = "select * from " + name +  getFilter(mapKeys(keys));
      try {
         SqlRowIterator it = database.queryIterator(sql);
         return it.hasNext() ? createRow(it.next()) :  null ;
      }
      catch (SQLException e) { throw new RuntimeException(e); }
   }

   public boolean destroyInstance(Object key) {
      if (key == null)
         return false;
      String sql = "delete from " + name + getFilter(mapKeys(expandKey(key)));
      try { return database.execute(sql) > 0; }
      catch (SQLException e) { throw new RuntimeException(e); }
   }

   public Collection filter(Expression exp) {
      try { return database.queryIterator("select * from "+name+getFilter(exp)).toCollection(); }
      catch (SQLException e) { throw new RuntimeException(e); }
   }

   @Override
   public String toString() {
      return databaseUri + "/" + name;
   } 

   @Override
   public boolean equals(Object that) {
      return that != null && toString().equals(that.toString());
   }

   @Override
   public int hashCode() {
      return toString().hashCode();
   }
   
   private SqlRow insertRow(Map row) {
      try {
         //TODO: must filter row keys to make sure no invalid column is in the qeury
         String sql = "insert into " + name + " ("
                    + String.join(",", row.keySet())
                    + ") values " + printValue(row.values(), false);
         Object[] keys = database.executeKeys(sql);
         if (keys == null)
            return null;
         applyKeys(row, keys);
         return createRow(row);
      }
      catch (SQLException e) { throw new RuntimeException(e); }
   }
   
   private SqlRow updateRow(Object[] keys, Map row) {
      try {
         //TODO: must filter row keys to make sure no invalid column is in the qeury
         String filter = getFilter(mapKeys(keys));
         int nb = database.execute("update " + name + getUpdate(row) + filter);
         if (nb == 0)
            return null;
         String sql = "select * from " + name + filter;
         SqlRowIterator it = database.queryIterator(sql);
         return it.hasNext() ? createRow(it.next()) : null;
      }
      catch (SQLException e) { throw new RuntimeException(e); }      
   }
   
   private Object[] expandKey(Object key) {
      if (key == null)
         return new String[0];
      if (key instanceof Object[])
         return (Object[])key;
      String txt = key.toString();
      while (txt.startsWith(SqlDatabase.PRIMARY_KEY_SEPARATOR))
         txt = txt.substring(1);
      while (txt.endsWith(SqlDatabase.PRIMARY_KEY_SEPARATOR))
         txt = txt.substring(0, txt.length()-1);
      return txt.split(SqlDatabase.PRIMARY_KEY_SEPARATOR);
   }

   private String extractKey(Map src) {
      List<String> keys = new ArrayList<>();
      for (String key : primaryKeys)
         keys.add(String.valueOf(src.get(key)));
      return String.join(SqlDatabase.PRIMARY_KEY_SEPARATOR, keys);
   }

   private String[] extractKeys(Map src) {
      List<String> keys = new ArrayList<>();
      for (String key : primaryKeys)
         keys.add(String.valueOf(src.get(key)));
      return keys.toArray(new String[keys.size()]);
   }
            
   private void applyKeys(Map row, Object[] keys) {
      for (int k = 0; k < primaryKeys.length; k++)
         row.put(primaryKeys[k], keys[k]);
   }

   private Map mapKeys(Object[] keys) {
      Map map = new LinkedHashMap();
      for (int k = 0; k < primaryKeys.length; k++) {
         Object value = (k >= keys.length) ? null : keys[k];
         map.put(primaryKeys[k], value);
      }
      return map;
   }

   private String getFilter(Expression exp) {
      Operator op = (Operator)exp.getFunction();
      Object[] tokens = exp.getParameters();
      if (tokens == null || tokens.length == 0)
         tokens = new Reference[2];
      else if (tokens.length < 2)
         tokens = new Object[]{tokens[0], null};
      String sql = "";
      for (Object token : tokens) {
         while (token instanceof Reference) {
            if (token instanceof Expression || token instanceof Property)
               break;
            token = ((Reference)token).getValue();
         }
         if (!sql.isEmpty())
            sql += printFunction(op, token);
         sql += printValue(token, false);
      }
      return sql;
   }

   private String getFilter(Map filter) {
      if (filter == null || filter.isEmpty())
         return "";
      String sql = "";
      Context cx = JHUX.get(Context.class);
      for (Object key : filter.keySet()) {
         Object value = filter.get(key);
         if (value == null)
            return "";
         SqlColumn col = columns.get(key.toString());
         if (col == null)
            key = " NULL ";
         else { 
            Class type = (Class)col.getValue();
            if (!type.isAssignableFrom(value.getClass()))
            value = cx.convert(value, type);
         }
         sql += (sql.isEmpty() ? " where " : " and ")
              + key + printValue(value, true);
      }
      return sql;
   }
   
   private SqlRow createRow(Map src) throws SQLException {
      Predicate<Map<String,Object>> updateFunction = map -> updateRow(expandKey(extractKey(map)), map) != null;
      SqlRow row = new SqlRow(src, updateFunction);
      row.cancelUpdate();
      return row;
   }

   private String getUpdate(Map entity) {
      String sql = "";
      for (Object key : entity.keySet())
         sql += (sql.isEmpty() ? " set " : ", ") + key + " = " + printValue(entity.get(key), false);
      return sql;
   }

   private String printFunction(Operator op, Object value) {
      switch (op) {
         case ADD:
         case REMOVE:
         case MULTIPLY:
         case MODULO:
         case DIVIDE:
         case LESSER:
         case GREATER:
            return op.getSymbol();
         case AND: return " AND ";
         case OR: return " OR ";
         case NOT: return " NOT ";
         case AT: return " IN ";
         case HAVE: return ".";
         case EQUAL:
            return (value == null) ? " IS " : " = ";
         default:
            throw new RuntimeException("Operator "+op+" is not supported for SQL databases");
      }
   }

   private String printValue(Object value, boolean printOperator) {
      if (value == null)
         return printOperator ? " IS NULL " : " NULL ";
      if (value instanceof Collection)
         value = ((Collection)value).toArray();
      if (value.getClass().isArray()) {
         int length = Array.getLength(value);
         StringJoiner joiner = new StringJoiner(", ", printOperator ? " in (" : "(", ")");
         for (int i = 0; i < length; i++)
            joiner.add(printValue(Array.get(value, i), false));
         return joiner.toString();
      }
      String op = printOperator ? " = " : "";
      if (value instanceof Boolean)
         return op + (database.getConnectionInfo().getVendor().isHasBoolean() ? value.toString() : ((Boolean)value ? "1" : "0"));
      if (value instanceof Number)
         return op + value;
      //TODO: check for complex, probability and time which need escaping
      if (value instanceof java.sql.Time || value instanceof java.sql.Date)
         return op + "'" + value + "'";
      if (value instanceof Date)
         return op + "'" + new SimpleDateFormat("YYYY-MM-dd HH:mm:ss").format(value) + "'";
      return op + "'" + value.toString().replace("'", "''") + "'";
   }
}
