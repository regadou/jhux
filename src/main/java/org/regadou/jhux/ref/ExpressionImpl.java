package org.regadou.jhux.ref;

import java.lang.reflect.Executable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.regadou.jhux.Expression;
import org.regadou.jhux.Function;
import org.regadou.jhux.Reference;
import org.regadou.jhux.Type;
import org.regadou.jhux.impl.ExecutableFunction;
import org.regadou.jhux.impl.Operator;
import org.regadou.jhux.impl.RootType;

public class ExpressionImpl implements Expression {
   
   private Function function;
   private List parameters = new ArrayList();
   private transient String text;
   
   public ExpressionImpl() {}
   
   public ExpressionImpl(Object...tokens) {
      this((String)null, tokens);
   }
   
   public ExpressionImpl(String text, Object...tokens) {
      this.text = text;
      for (Object token : tokens)
         addToken(token);
   }
   
   public ExpressionImpl(Function function, Object...parameters) {
      this.function = function;
      if (parameters != null)
         this.parameters.addAll(Arrays.asList(parameters));
   }

   @Override
   public Object getValue() {
      if (function == null)
         function = detectImplicitFunction();
      return function.execute(parameters.toArray());
   }
    
   @Override
   public Function getFunction() {
      return function;
   }

   @Override
   public Object[] getParameters() {
      return parameters.toArray();
   }
   
   @Override
   public String toString() {
      if (text == null) {
         if (function instanceof Operator && parameters.size() == 2) {
            String symbol = ((Operator)function).getSymbol();
            if (symbol != null)
               return text = "("+parameters.get(0)+" "+symbol+" "+parameters.get(1)+")";
         }
         StringBuilder buffer = new StringBuilder("(");
         if (function != null)
            buffer.append(function.getName());
         for (Object param : parameters)
            buffer.append((buffer.length() == 1) ? "" : " ").append(param);
         text = buffer.append(")").toString();
      }
      return text;
   }

   @Override
   public boolean equals(Object that) {
      return that != null && toString().equals(that.toString());
   }

   @Override
   public int hashCode() {
      return toString().hashCode();
   }
   
   private void addToken(Object token) {
      Function f = getFunction(token);
      if (f == null)
         parameters.add(token);
      else if (function == null) {
         function = f;
         if (parameters.size() > 1) {
            List params = new ArrayList();
            params.add(parameters);
            parameters = params;
         }
      }
      else {
         Expression exp = new ExpressionImpl(function, parameters.toArray());
         function = f;
         parameters = new ArrayList();
         parameters.add(exp);
      }
   }
   
   private Function getFunction(Object token) {
      if (token instanceof Function)
         return (Function)token;
      if (token instanceof Executable)
         return new ExecutableFunction((Executable)token);
      if (token instanceof Expression || token instanceof Property)
         return null;
      if (token instanceof Reference)
         return getFunction(((Reference)token).getValue());
      return null;
   }
   
   private Function detectImplicitFunction() {
      Type t;
      for (int p = 0; p < parameters.size(); p++) {
         Object param = parameters.get(p);
         if (param instanceof Type)
            t = (Type)param;
         else if (param instanceof Class)
            t = RootType.getType((Class)param);
         else
            continue;
         parameters.remove(p);
         return t.getConstructor();
      }
      return Operator.NOOP;
   }
}

