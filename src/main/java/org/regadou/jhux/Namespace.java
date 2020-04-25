package org.regadou.jhux;

public interface Namespace {
   
   String getUri();
   
   String getPrefix();
   
   Reference get(String uri);
   
   Reference post(String uri, Object data);
   
   Reference put(String uri, Object data);
   
   int delete(String uri);
}
