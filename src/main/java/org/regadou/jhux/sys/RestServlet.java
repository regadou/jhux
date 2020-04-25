package org.regadou.jhux.sys;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Properties;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.regadou.jhux.JHUX;
import org.regadou.jhux.impl.FormatImpl;
import org.regadou.jhux.impl.RestFunction;
import org.regadou.jhux.ns.HttpNamespace;
import org.regadou.jhux.ref.ExpressionImpl;

public class RestServlet implements Servlet {

   private static final int NOT_FOUND_STATUS = 404;
   private static final int OK_STATUS = 200;

   private ServletConfig servletConfig;
   private Properties contextProperties;

   @Override
   public String toString() {
      return "[Servlet "+servletConfig.getServletName()+"]";
   }

   @Override
   public String getServletInfo() {
      return "JHUX REST Servlet";
   }

   @Override
   public void init(ServletConfig config) throws ServletException {
      servletConfig = config;
      contextProperties = new Properties();
      Enumeration<String> e = config.getInitParameterNames();
      while (e.hasMoreElements()) {
         String name = e.nextElement();
         contextProperties.put(name, config.getInitParameter(name));
      }
      ServletContext scx = config.getServletContext();
      e = scx.getInitParameterNames();
      while (e.hasMoreElements()) {
         String name = e.nextElement();
         contextProperties.put(name, scx.getInitParameter(name));
      }
      JHUX.get(Configuration.class, contextProperties);
  }

   @Override
   public ServletConfig getServletConfig() {
      return servletConfig;
   }

   @Override
   public void destroy() {
   }

   @Override
   public void service(ServletRequest request, ServletResponse response) throws IOException {
      doRequest((HttpServletRequest)request, (HttpServletResponse)response);
   }

   private void doRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
      RestFunction method;
      try { method = RestFunction.valueOf(request.getMethod().toUpperCase()); }
      catch (Exception e) {
         return;  //TODO: need to return unsupported method status
      }
      String rcx = request.getContextPath();
      String uri = request.getRequestURI();
      if (uri.startsWith(rcx))
         uri = uri.substring(rcx.length());
      String query = request.getQueryString();
      if (query != null && !query.isEmpty())
         uri += "?" + query;
      // TODO: check if we already have a session
      Context cx = JHUX.get(Context.class);
      Object value = new ExpressionImpl(method, getParameters(cx, method, uri, request)).getValue();
      response.setStatus((value == null || failedDelete(method, value)) ? NOT_FOUND_STATUS : OK_STATUS);
      response.setContentType(HttpNamespace.DEFAULT_MIMETYPE);
      response.setCharacterEncoding(FormatImpl.DEFAULT_CHARSET);
      cx.encode(value, response.getOutputStream(), HttpNamespace.DEFAULT_MIMETYPE, FormatImpl.DEFAULT_CHARSET);
   }
   
   private Object[] getParameters(Context cx, RestFunction method, String uri, HttpServletRequest request) throws IOException {
      return method.hasDataParameter() ? new Object[]{uri}
           : new Object[]{uri, cx.decode(request.getInputStream(), request.getContentType(), request.getCharacterEncoding())};
   }
   
   private boolean failedDelete(RestFunction method, Object value) {
      if (method != RestFunction.DELETE)
         return false;
      if (value instanceof Number)
         return ((Number)value).intValue() < 1;
      return value == null;
   }
}
