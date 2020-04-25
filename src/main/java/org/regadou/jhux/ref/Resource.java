package org.regadou.jhux.ref;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import org.regadou.jhux.Reference;

public class Resource implements Reference {
   
   private URI uri;
   private transient Object value;
   private transient Resource owner;
   
   public Resource(String txt) {
      try { uri = new URI(txt); } 
      catch (URISyntaxException e) {
         File f = new File(txt);
         if (f.exists())
            uri = f.toURI();
         else
            throw new RuntimeException("Invalid resource id: "+txt);
      }
   }
   
   public Resource(File file) {
      uri = file.toURI();
   }
   
   public Resource(URL url) {
      try { uri = url.toURI(); }
      catch (URISyntaxException e) { throw new RuntimeException(e); }
   }
   
   public Resource(URI uri) {
      this.uri = uri;
   }

   @Override
   public Object getOwner() {
      if (owner == null) {
         
      }
      return owner;
   }

   @Override
   public String getKey() {
      return uri.toString();
   }

   @Override
   public Object getValue() {
      if (value == null) {
         // TODO: read the uri data
      }
      return value;
   }
   
   // TODO: should we support setting and deleting this resource ? maybe inherit these methods from Property
}
