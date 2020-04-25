package org.regadou.jhux.sql;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

public class SqlRowIterator implements Iterator<Map<String,Object>> {
   
   private String sql;
   private ResultSet resultSet;
   private int count;
   
   public SqlRowIterator(String sql, Statement statement) throws SQLException {
      int index = sql.toLowerCase().indexOf("from");
      ResultSet rscount = statement.executeQuery("select count(*) as nb "+ sql.substring(index));
      count = rscount.next() ? rscount.getInt("nb") : 0;
      resultSet = statement.executeQuery(sql);
   }
   
   @Override
   public boolean hasNext() {
      try { return !resultSet.isAfterLast(); }
      catch (SQLException e) { throw new RuntimeException(e); }
   }

   @Override
   public Map<String,Object> next() {
      try {
         if (resultSet.isAfterLast())
            throw new NoSuchElementException("Iteration is after last row");
         if (resultSet.isBeforeFirst())
            resultSet.next();
         Map<String,Object> row = SqlDatabase.getRow(resultSet);
         resultSet.next();
         if (resultSet.isAfterLast())
            resultSet.close();
         return row;
      }
      catch (SQLException e) { throw new RuntimeException(e); }
   }

   public int size() {
      return count;
   }
      
   public Collection toCollection() {
      return new AbstractCollection() {
         @Override
         public Iterator iterator() {
            return SqlRowIterator.this;
         }

         @Override
         public int size() {
            return SqlRowIterator.this.size();
         }            
      };
   }
}
