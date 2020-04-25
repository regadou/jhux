package org.regadou.jhux.impl;

import java.net.URI;
import org.regadou.jhux.Function;
import org.regadou.jhux.JHUX;
import org.regadou.jhux.Namespace;
import org.regadou.jhux.Reference;
import org.regadou.jhux.ns.Vocabulary;
import org.regadou.jhux.sys.Context;

public enum RestFunction implements Function {
   
   GET, POST, PUT, DELETE;
   
   @Override
   public Object execute(Object...parameters) {
      int expectedParams = hasDataParameter() ? 2 : 1;
      if (parameters.length != expectedParams)
         throw new RuntimeException("Expected "+expectedParams+" parameters but got "+parameters.length);
      switch (this) {
         case GET:
               return get(parameters[0]);
         case POST:
               return post(parameters[0], parameters[1]);
         case PUT:
               return put(parameters[0], parameters[1]);
         case DELETE:
               return delete(parameters[0]);
         default:
            throw new RuntimeException("Unknown REST function: "+name());
      }
   }
   
   @Override
   public String getName() {
      return name().toLowerCase();
   }
   
   public boolean hasDataParameter() {
      switch (this) {
         case GET:
         case DELETE:
               return false;
         case POST:
         case PUT:
               return true;
         default:
            throw new RuntimeException("Unknown REST function: "+name());
      }
   }
   
   private Reference get(Object target) {
      if (target instanceof Reference || target == null)
         return (Reference)target;
      return JHUX.ref(target);
   }
   
   private Reference post(Object target, Object data) {
      NamespaceUri nsu = getNamespaceUri(target);
      return (nsu.ns == null) ? null : nsu.ns.post(nsu.uri, data);
   }
   
   private Reference put(Object target, Object data) {
      NamespaceUri nsu = getNamespaceUri(target);
      return (nsu.ns == null) ? null : nsu.ns.put(nsu.uri, data);
   }
   
   private int delete(Object target) {
      NamespaceUri nsu = getNamespaceUri(target);
      return (nsu.ns == null) ? 0 : nsu.ns.delete(nsu.uri);
   }
   
   private NamespaceUri getNamespaceUri(Object target) {
      if (target == null)
         return new NamespaceUri();
      Context cx = JHUX.get(Context.class);
      URI uri = cx.convert(target, URI.class);
      if (uri == null)
         return new NamespaceUri();
      String scheme = uri.getScheme();
      Namespace ns = (scheme == null) ? new Vocabulary(cx.getDictionary()) : JHUX.get(Namespace.class, scheme);
      return new NamespaceUri(ns, uri.toString());
   }
   
   private static class NamespaceUri {
      Namespace ns;
      String uri;
      
      public NamespaceUri() {}
      
      public NamespaceUri(Namespace ns, String uri) {
         this.ns = ns;
         this.uri = uri;
      }
   }
}
