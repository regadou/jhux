package org.regadou.jhux.impl;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.regadou.jhux.Function;
import org.regadou.jhux.JHUX;
import org.regadou.jhux.sys.Context;

public class ExecutableFunction implements Function {

   private Executable executable;
   
   public ExecutableFunction(Executable executable) {
      if (executable instanceof Constructor || executable instanceof Method)
         this.executable = executable;
      else
         throw new RuntimeException("Unknown executable: "+executable.getClass().getName());
   }
   
   @Override
   public Object execute(Object... parameters) {
      if (parameters.length != executable.getParameterTypes().length)
         throw new RuntimeException("Expected "+executable.getParameterTypes().length+" parameters but got "+parameters.length);
      try {
         if (executable instanceof Constructor)
            return ((Constructor)executable).newInstance(convert(parameters, executable.getParameterTypes()));
         Method m = (Method)executable;
         if (Modifier.isStatic(m.getModifiers()))
            return m.invoke(null, convert(parameters, executable.getParameterTypes()));
         if (m.getDeclaringClass().isInstance(parameters[0]))
            return m.invoke(parameters[0], convert(Arrays.asList(parameters).subList(1, parameters.length).toArray(), executable.getParameterTypes()));;
         throw new IllegalArgumentException("First parameter must be a "+m.getDeclaringClass().getName()+" but was a "+parameters[0].getClass().getName());
      }
      catch (Exception e) { 
         RuntimeException rte = (e instanceof RuntimeException) ? (RuntimeException)e : new RuntimeException(e);
         throw rte; 
      }
   }

   @Override
   public String getName() {
      if (executable instanceof Constructor)
         return ((Constructor)executable).getDeclaringClass().getName();
      return ((Method)executable).getName();
   }
   
   private Object[] convert(Object[] parameters, Class[] types) {
      Context cx = JHUX.get(Context.class);
      List dst = new ArrayList();
      for (int t = 0; t < types.length; t++)
         dst.add(cx.convert((t < parameters.length) ? parameters[t] : null, types[t]));
      return dst.toArray();
   }
}
