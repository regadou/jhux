package org.regadou.jhux.sys;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import org.regadou.jhux.Reference;

public class TcpServer extends Thread implements Reference, Closeable {

   private String uri;
   private long sleepTime = 1000;
   private transient ServerSocket server = null;
   private transient Map<String,Console> clients = new LinkedHashMap<>();
   private transient boolean running = false;

   public TcpServer() {}

   public TcpServer(String uri) {
      setName(this.uri = uri);
      try {
         String[] parts = (uri.startsWith("tcp:") ? uri.substring(4) : uri).split(":");
         String host;
         int port;
         switch (parts.length) {
            case 1:
               host = null;
               port = Integer.parseInt(parts[0]);
               break;
            case 2:
               host = parts[0];
               while (host.startsWith("/"))
                  host = host.substring(1);
               port = Integer.parseInt(parts[1]);
               break;
            default:
               throw new RuntimeException("Invalid tcp uri: "+uri);
         }
         if (host == null)
            server = new ServerSocket(port);
         else
            server = new ServerSocket(port, 0, InetAddress.getByName(host));
      }
      catch (IOException e) {
         throw new RuntimeException(e);
      }
   }

   public TcpServer(int port) throws IOException {
      this(null, port);
   }

   public TcpServer(String host, int port) throws IOException {
      if (host == null)
         server = new ServerSocket(port);
      else
         server = new ServerSocket(port, 0, InetAddress.getByName(host));
      setName(uri = "tcp:" + getHost() + ":" + getPort());
   }

   @Override
   public Object getOwner() {
      return null;
   }

   @Override
   public String getKey() {
      return uri;
   }

   @Override
   public Object getValue() {
      return clients;
   }

   @Override
   protected void finalize() throws Throwable {
      super.finalize();
      close();
   }

   @Override
   public String toString() {
      return uri;
   }

   public final String getHost() { return server.getInetAddress().getHostName(); }

   public final int getPort() { return server.getLocalPort(); }

   public synchronized boolean isRunning() { return running; }

   public synchronized long getSleepTime() { return sleepTime; }

   public synchronized void setSleepTime(long sleepTime) {
      this.sleepTime = sleepTime;
   }

   public void run() {
      if (running || server == null)
         return;
      synchronized (this ) { running = true; }

      while (isRunning()) {
         try {
            final Socket s = server.accept();
            if (s != null) {
               new Thread(new Runnable() {
                  public void run() { readClient(s); }
               }).start();
            }
         }
         catch (Exception e) {
	         e.printStackTrace();
         }
         try { sleep(getSleepTime()); }
         catch (Exception e) {
            synchronized (this) { running = false; }
         }
      }

      close();
   }

   @Override
   public void close() {
      synchronized (this) { running = false; }
      if (server != null) {
         try { server.close(); }
         catch (Exception e) {}
         server = null;
      }
      Iterator iter = clients.keySet().iterator();
      while (iter.hasNext())
         close((Socket)iter.next());
   }

   private void close(Socket s) {
      try { s.close(); }
      catch (Exception e) {}
      Console c = clients.remove(getSocketId(s));
      if (c != null)
         c.close();
   }

   private void readClient(Socket s) {
      try {
         String id = getSocketId(s);
         Console c = new Console(id, s.getInputStream(), s.getOutputStream());
         clients.put(id, c);
         c.run("\n? ", "= ", "quit", "exit");
      }
      catch (Exception e) { e.printStackTrace(); }
      finally { close(s); }
   }
   
   private String getSocketId(Socket s) {
      return "tcp:"+s.getRemoteSocketAddress();
   }
}
