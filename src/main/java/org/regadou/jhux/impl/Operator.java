package org.regadou.jhux.impl;

import java.util.Arrays;
import org.regadou.jhux.ref.Property;
import java.util.Map;
import java.util.function.BiFunction;
import org.regadou.jhux.Function;
import org.regadou.jhux.JHUX;
import org.regadou.jhux.Reference;
import org.regadou.jhux.sys.Context;

public enum Operator implements Function {

   DO,             BE(":"),      HAVE("#"),
   GREATER(">"),   EQUAL("="),   LESSER("<"),
   TO("->"),       AT("@"),      FROM("<-"),
   AND("&"),       OR("|"),      NOT("!"),
   ADD("+"),       NOOP,         REMOVE("-"),
   MULTIPLY("*"),  MODULO("%"),  DIVIDE("/"),
   POWER("^"),     ROOT,         LOGARITHM,
   EACH,           WHILE,        IF,
   JOIN(","),      EVAL(";");
   
   private static final Map<Operator, BiFunction> IMPLEMENTATIONS = new ImmutableMap(
           AT, (BiFunction)(x, y) -> new Property(y, stringify(x)),
           HAVE, (BiFunction)(x, y) -> new Property(x, stringify(y)),
           DO, (BiFunction)(x, y) -> {
              ((Reference)x).getValue();
              return (y instanceof Reference) ? ((Reference)y).getValue() : y;
           }
           // TODO: a lot of operators are not defined
   );
   
   private static final Map<Operator, Object> DEFAULT_VALUE = new ImmutableMap(
           AND, false,
           OR, true,
           NOT, false,
           MULTIPLY, 1,
           MODULO, 0,
           DIVIDE, 1,
           POWER, 1,
           ROOT, 1,
           LOGARITHM, 1
   );
   
   private String symbol;
   
   private Operator() {
      
   }
   
   private Operator(String symbol) {
      this.symbol = symbol;
   }   
   
   @Override
   public Object execute(Object...parameters) {
      switch (this) {
         case EVAL:
            Object result = null;
            for (Object param : parameters)
               result = (param instanceof Reference) ? ((Reference)param).getValue() : param;
            return result;
         case NOOP:
            switch (parameters.length) {
               case 0:
                  return null;
               case 1:
                  return parameters[0];
               default:
                  return Arrays.asList(parameters);
            }
      }
      
      BiFunction f = IMPLEMENTATIONS.get(this);
      if (f == null || parameters == null)
         return DEFAULT_VALUE.get(this);
      switch (parameters.length) {
         case 0:
            return DEFAULT_VALUE.get(this);
         case 1:
            return f.apply(DEFAULT_VALUE.get(this), parameters[0]);
         case 2:
            return f.apply(parameters[0], parameters[1]);
         default:
            Object result = parameters[0];
            for (int i = 1; i < parameters.length; i++)
               result = f.apply(result, parameters[i]);
            return result;
      }
   }  
   
   @Override
   public String getName() {
      return name().toLowerCase();
   }
   
   public String getSymbol() {
      return symbol;
   }
   
   @Override
   public String toString() {
      return name().toLowerCase();
   }
   
   private static String stringify(Object value) {
      if (value == null)
         return null;
      return JHUX.get(Context.class).print(value);
   }
}

