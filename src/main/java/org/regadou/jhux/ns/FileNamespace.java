package org.regadou.jhux.ns;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.regadou.jhux.Format;
import org.regadou.jhux.JHUX;
import org.regadou.jhux.Namespace;
import org.regadou.jhux.Reference;
import org.regadou.jhux.ref.Resource;
import org.regadou.jhux.ref.Value;
import org.regadou.jhux.sys.Context;

public class FileNamespace implements Namespace {

   private String basedir;
   
   public FileNamespace(File basedir) {
      if (!basedir.isDirectory())
         throw new RuntimeException("Basedir is not a directory: "+basedir);
      //TODO: we must transform this string if os is Windows
      try {
      this.basedir = basedir.getCanonicalPath();
      if (!this.basedir.endsWith("/"))
         this.basedir += "/";
      }
      catch (IOException e) { throw new RuntimeException(e); }
   }
   
   @Override
   public String getPrefix() {
      return "file";
   }

   @Override
   public String getUri() {
      return "file:"+basedir;
   }

   @Override
   public Reference get(String uri) {
      File file = getFile(uri, basedir);
      if (!file.exists())
         return null;
      if (file.isDirectory())
         return new Value(file.listFiles());
      try {
         InputStream input = new FileInputStream(file);
         Object result = JHUX.get(Context.class).decode(input, getMimetype(file), null);
         input.close();
         return (result instanceof Reference) ? (Reference)result : new Value(result);
      }
      catch (IOException e) { throw new RuntimeException(e); }
   }

   @Override
   public Reference post(String uri, Object data) {
      return saveFile(uri, basedir, data, true);
   }

   @Override
   public Reference put(String uri, Object data) {
      return saveFile(uri, basedir, data, false);
   }

   @Override
   public int delete(String uri) {
      return deleteFile(getFile(uri, basedir));
   }
   
   @Override
   public String toString() {
      return getPrefix()+":";
   }   
   
   private static File getFile(String uri, String basedir) {
      if (uri == null || uri.trim().isEmpty())
         uri = basedir;
      int colon = uri.indexOf(':');
      int slash = uri.indexOf('/');
      if (colon >= 0) {
         if (slash < 0 || colon < slash) {
            if (!uri.substring(0, colon).equals("file"))
               throw new RuntimeException("URI is not a file: "+uri);
            uri = uri.substring(colon+1);
         }
      }
      
      boolean haveSlash = false;
      while (uri.startsWith("/")) {
         uri = uri.substring(1);
         haveSlash = true;
      }
      return new File(haveSlash ? "/" + uri : basedir + uri);
   }
   
   private static Reference saveFile(String uri, String basedir, Object data, boolean append) {
      File file = getFile(uri, basedir);
      if (file.isDirectory())
         return new Value(file.listFiles());
      File parent = file.getParentFile();
      if (!parent.exists()) {
         if (!parent.mkdirs())
            throw new RuntimeException("Cannot create directory: "+parent);
      }
      try {
         OutputStream output = new FileOutputStream(file, append);
         JHUX.get(Context.class).encode(data, output, getMimetype(file), null);
         output.close();
         return new Resource(file);
      }
      catch (IOException e) { throw new RuntimeException(e); }
   }

   private static int deleteFile(File file) {
      if (!file.exists())
         return 0;
      if (!file.isDirectory())
         return file.delete() ? 1 : 0;
      int deleted = 0;
      for (File f : file.listFiles())
         deleted += deleteFile(f);
      if (file.delete())
         deleted++;
      return deleted;
   }
   
   private static String getMimetype(File file) throws IOException {
      String name = file.getCanonicalFile().getName();
      int dot = name.lastIndexOf('.');
      if (dot > 0) {
         Format format = JHUX.get(Format.class, name.substring(dot+1).toLowerCase());
         if (format != null)
            return format.getName();
      }
      return "text/plain";
   }
}
