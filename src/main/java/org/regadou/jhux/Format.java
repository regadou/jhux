package org.regadou.jhux;

import java.io.InputStream;
import java.io.OutputStream;

public interface Format {

   String getName();
   
   Object decode(InputStream input, String charset);

   void encode(Object value, OutputStream output, String charset);
}

