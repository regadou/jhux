package org.regadou.jhux.sql;

import java.io.Closeable;
import java.io.IOException;
import java.math.BigInteger;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import org.regadou.jhux.JHUX;
import org.regadou.jhux.Reference;
import org.regadou.jhux.impl.MapAdapter;
import org.regadou.jhux.sys.Context;

public class SqlDatabase implements Reference, Closeable {

   public static final String PRIMARY_KEY_SEPARATOR = "_";  
   
   public static Map<String,Object> getRow(ResultSet rs) throws SQLException {
      ResultSetMetaData meta = rs.getMetaData();
      int nc = meta.getColumnCount();
      Map<String,Object>row = new LinkedHashMap<>();
      for (int c = 1; c <= nc; c++)
         row.put(meta.getColumnName(c).toLowerCase(), rs.getObject(c));
      return row;
   }

   private transient Map<String, SqlTable> tables = new LinkedHashMap<>();
   private transient JdbcConnectionInfo info;
   private transient Connection connection;
   private transient Statement statement;
   private String url;
   private transient String name;
   private transient Map<String,Object> adapter = new MapAdapter<>(
              () -> tables.keySet(),
              n -> getTable(n),
              (n, v) -> createTable(n, v),
              null);
   
   public SqlDatabase(String uri) {
      this(new JdbcConnectionInfo(uri));
   }
   
   public SqlDatabase(JdbcConnectionInfo info) {
      this.info = info;
      this.url = info.getUrl();
      this.name = info.getDatabase();
      loadTables(true);
   }

   @Override
   public Object getOwner() {
      return null;
   }

   @Override
   public String getKey() {
      return url;
   }

   @Override
   public Object getValue() {
      return adapter;
   }
   
   @Override
   public void close() throws IOException {
      if (statement != null) {
         try { statement.close(); }
         catch (Exception e) {}
         statement = null;
      }
      if (connection != null) {
         try { connection.close(); }
         catch (Exception e) {}
         connection = null;
      }
   }

   @Override
   public String toString() {
      return url;
   }

   @Override
   public boolean equals(Object that) {
      return toString().equals(that.toString());
   }

   @Override
   public int hashCode() {
      return toString().hashCode();
   }

   public JdbcConnectionInfo getConnectionInfo() {
      return info;
   }
   
   public Collection<Map<String,Object>> query(String sql) throws SQLException {
      ResultSet rs = statement.executeQuery(sql);
      ResultSetMetaData meta = rs.getMetaData();
      int nc = meta.getColumnCount();
      String table = meta.getTableName(1);
      for (int c = 2; c <= nc; c++) {
         if (!table.equals(meta.getTableName(c))) {
            table = null;
            break;
         }
      }
      Collection<Map<String,Object>> rows = new ArrayList<>();
      while (rs.next())
         rows.add(getRow(rs));
      return rows;
   }

   public SqlRowIterator queryIterator(String sql) throws SQLException {
      return new SqlRowIterator(sql, statement);      
   }
   
   public int execute(String sql) throws SQLException {
      return statement.executeUpdate(sql);
   }
   
   public Object[] executeKeys(String sql) throws SQLException {
      int nb = statement.executeUpdate(sql);
      if (nb == 0)
         return null;
      ResultSet rs = statement.getGeneratedKeys();
      int nc = rs.getMetaData().getColumnCount();
      Object[] keys = new Object[nc];
      for (int c = 0; c < nc; c++)
         keys[c] = rs.getObject(c+1);
      return keys;
   }
   
   private SqlTable getTable(Object key) {
      if (key == null)
         return null;
      String name = key.toString().toLowerCase();
      return tables.get(name);
   }
   
   private void createTable(Object key, Object value) throws IllegalArgumentException {
      if (key == null)
         return;
      String tableName = key.toString().toLowerCase();
      if (tables.containsKey(tableName))
         throw new IllegalArgumentException("Collection "+tableName+" already exists");
      //TODO: this is wrong, we should try to get Type.getProperties() or TypeProperty[]
      SqlColumn[] columns = JHUX.get(Context.class).convert(value, SqlColumn[].class);
      String first = null;
      String sql = "create table "+tableName+" {";
      for (SqlColumn col : columns) {
         if (col == null)
            throw new IllegalArgumentException("Column name cannot be null");
         String name = col.getKey();
         if (name.isEmpty())
            throw new IllegalArgumentException("Column name cannot be an empty string");
         if (first == null)
            first = name;
         else
            sql += ",";
         sql += "\n   "+name+" "+getColumnDefinition(col.getValue());
      }
      try {
         statement.executeUpdate(sql+"\n}");
         tables.put(tableName, new SqlTable(tableName, Arrays.asList(columns), this));
      }
      catch (SQLException e) { throw new IllegalArgumentException("Cannot create table "+tableName+": "+e, e); }
   }

   private void loadTables(boolean reloadConnection) {
      try {
         if (reloadConnection || connection == null)
            openConnection();
         DatabaseMetaData dmd = connection.getMetaData();
         ResultSet rs = dmd.getTables(info.getDatabase(), null, null, null);
         while (rs.next()) {
            String type = rs.getString("table_type").toLowerCase();
            if (type.equals("table")) {
               String table = rs.getString("table_name").toLowerCase();
               tables.put(table, new SqlTable(table, getColumns(table), this));
            }
         }
         rs.close();
      }
      catch (SQLException | IOException e) { throw new RuntimeException(e); }
   }   

   private Collection<SqlColumn> getColumns(String table) {
      try {
         DatabaseMetaData dmd = connection.getMetaData();
         Map<String,Class> map = new LinkedHashMap<>();
         ResultSet rs = dmd.getColumns(info.getDatabase(),null,table,null);
         while (rs.next())
            map.put(rs.getString("COLUMN_NAME").toLowerCase(), getJavaType(rs.getObject("DATA_TYPE")));
         rs.close();
         List<String> pkeys = new ArrayList<>();
         rs = dmd.getPrimaryKeys(null, null, table);
         while (rs.next())
            pkeys.add(rs.getString("COLUMN_NAME").toLowerCase());
         Collection<SqlColumn> columns = new ArrayList<>();
         for (String key : map.keySet()) {
            Class type = map.get(key);
            boolean primary = pkeys.contains(key);
            columns.add(new SqlColumn(name, type, primary));
         }
         return columns;
      }
      catch (SQLException e) {
         throw new RuntimeException(e);
      }
   }

   private String getColumnDefinition(Object def) {
      if (def instanceof Class)
         return getSqlType((Class)def);
      if (def instanceof CharSequence)
         return def.toString();
      if (def == null)
         throw new IllegalArgumentException("Column definition must not be null");
      if (def.getClass().isArray())
         def = JHUX.get(Context.class).convert(def, Collection.class);
      if (def instanceof Collection) {
         StringJoiner joiner = new StringJoiner(" ");
         for (Object e : (Collection)def)
            joiner.add(getColumnDefinition(e));
         return joiner.toString();
      }
      throw new IllegalArgumentException("Invalid definition element type: "+def.getClass().getName());
   }

   private void openConnection() throws IOException {
      if (info == null)
         throw new IOException("No connection info has been set");
      String checking = null;
      try {
           checking = "connection";
           if (connection != null && connection.isClosed()) {
              connection = null;
              statement = null;
           }
           checking = "statement";
           if (statement != null && statement.isClosed())
              statement = null;
           checking = null;
      }
      catch (Throwable t) {
         if ("connection".equals(checking))
            connection = null;
         statement = null;
      }

      try {
          if (connection == null)
              connection = DriverManager.getConnection(info.getUrl(), info.getUser(), info.getPassword());
          if (statement == null) {
              try {
                 statement = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
              }
              catch (Exception e) {
                 statement = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
              }
          }
      }
      catch (Exception e) {
         throw new IOException("Connection to " + info.getUrl() + " failed: " + e.toString(), e);
      }
   }

   private String getSqlType(Class type) {
      if (CharSequence.class.isAssignableFrom(type))
         return "text";
      if (Character.class.isAssignableFrom(type))
         return "char(1)";
      if (Date.class.isAssignableFrom(type)) {
         if (java.sql.Date.class.isAssignableFrom(type))
            return "date";
         if (java.sql.Time.class.isAssignableFrom(type))
            return "time";
         return "timestamp";
      }
      if (Boolean.class.isAssignableFrom(type))
         return info.getVendor().isHasBoolean() ? "boolean" : "tinyint";
      if (Number.class.isAssignableFrom(type)) {
         if (Byte.class.isAssignableFrom(type))
            return "tinyint";
         if (Short.class.isAssignableFrom(type))
            return "smallint";
         if (Integer.class.isAssignableFrom(type))
            return "int";
         if (Long.class.isAssignableFrom(type))
            return "bigint";
         if (Float.class.isAssignableFrom(type))
            return "real";
         if (Double.class.isAssignableFrom(type))
            return "float";
         if (BigInteger.class.isAssignableFrom(type))
            return "bigint";
         //TODO: check for primitives
         return "numeric";
      }
      if (byte[].class.isAssignableFrom(type))
         return "binary";
      if (Blob.class.isAssignableFrom(type))
         return "blob";
      if (Clob.class.isAssignableFrom(type))
         return "clob";
      throw new IllegalArgumentException("Unknown data type: "+type.getName());
   }

   private Class getJavaType(Object sqlType) {
      switch (Integer.parseInt(sqlType.toString())) {
         case java.sql.Types.ARRAY:
            return Object[].class;
         case java.sql.Types.BIGINT:
            return Long.class;
         case java.sql.Types.INTEGER:
            return Integer.class;
         case java.sql.Types.SMALLINT:
            return Short.class;
         case java.sql.Types.TINYINT:
            return Byte.class;
         case java.sql.Types.DECIMAL:
         case java.sql.Types.DOUBLE:
         case java.sql.Types.FLOAT:
         case java.sql.Types.NUMERIC:
         case java.sql.Types.REAL:
            return Double.class;
         case java.sql.Types.BINARY:
         case java.sql.Types.BLOB:
         case java.sql.Types.VARBINARY:
            return byte[].class;
         case java.sql.Types.BIT:
         case java.sql.Types.BOOLEAN:
            return Boolean.class;
         case java.sql.Types.CHAR:
         case java.sql.Types.CLOB:
         case java.sql.Types.LONGNVARCHAR:
         case java.sql.Types.LONGVARBINARY:
         case java.sql.Types.LONGVARCHAR:
         case java.sql.Types.NCHAR:
         case java.sql.Types.NCLOB:
         case java.sql.Types.NVARCHAR:
         case java.sql.Types.VARCHAR:
            return String.class;
         case java.sql.Types.NULL:
            return Object.class;
         case java.sql.Types.DATE:
            return java.sql.Date.class;
         case java.sql.Types.TIMESTAMP:
         case java.sql.Types.TIMESTAMP_WITH_TIMEZONE:
            return java.util.Date.class;
         case java.sql.Types.TIME:
         case java.sql.Types.TIME_WITH_TIMEZONE:
            return java.sql.Time.class;
         case java.sql.Types.DATALINK:
         case java.sql.Types.DISTINCT:
         case java.sql.Types.JAVA_OBJECT:
         case java.sql.Types.OTHER:
         case java.sql.Types.REF:
         case java.sql.Types.REF_CURSOR:
         case java.sql.Types.ROWID:
         case java.sql.Types.SQLXML:
         case java.sql.Types.STRUCT:
         default:
            return Object.class;
      }
   }
}
