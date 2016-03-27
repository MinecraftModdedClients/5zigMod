package org.h2.server.web;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Properties;
import javax.servlet.ServletConfig;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.h2.engine.Constants;
import org.h2.util.New;

public class WebServlet
  extends HttpServlet
{
  private static final long serialVersionUID = 1L;
  private transient WebServer server;
  
  public void init()
  {
    ServletConfig localServletConfig = getServletConfig();
    Enumeration localEnumeration = localServletConfig.getInitParameterNames();
    ArrayList localArrayList = New.arrayList();
    while (localEnumeration.hasMoreElements())
    {
      localObject = localEnumeration.nextElement().toString();
      String str = localServletConfig.getInitParameter((String)localObject);
      if (!((String)localObject).startsWith("-")) {
        localObject = "-" + (String)localObject;
      }
      localArrayList.add(localObject);
      if (str.length() > 0) {
        localArrayList.add(str);
      }
    }
    Object localObject = new String[localArrayList.size()];
    localArrayList.toArray((Object[])localObject);
    this.server = new WebServer();
    this.server.setAllowChunked(false);
    this.server.init((String[])localObject);
  }
  
  public void destroy()
  {
    this.server.stop();
  }
  
  private boolean allow(HttpServletRequest paramHttpServletRequest)
  {
    if (this.server.getAllowOthers()) {
      return true;
    }
    String str = paramHttpServletRequest.getRemoteAddr();
    try
    {
      InetAddress localInetAddress = InetAddress.getByName(str);
      return localInetAddress.isLoopbackAddress();
    }
    catch (UnknownHostException localUnknownHostException)
    {
      return false;
    }
    catch (NoClassDefFoundError localNoClassDefFoundError) {}
    return false;
  }
  
  private String getAllowedFile(HttpServletRequest paramHttpServletRequest, String paramString)
  {
    if (!allow(paramHttpServletRequest)) {
      return "notAllowed.jsp";
    }
    if (paramString.length() == 0) {
      return "index.do";
    }
    return paramString;
  }
  
  public void doGet(HttpServletRequest paramHttpServletRequest, HttpServletResponse paramHttpServletResponse)
    throws IOException
  {
    paramHttpServletRequest.setCharacterEncoding("utf-8");
    String str1 = paramHttpServletRequest.getPathInfo();
    if (str1 == null)
    {
      paramHttpServletResponse.sendRedirect(paramHttpServletRequest.getRequestURI() + "/");
      return;
    }
    if (str1.startsWith("/")) {
      str1 = str1.substring(1);
    }
    str1 = getAllowedFile(paramHttpServletRequest, str1);
    
    Properties localProperties = new Properties();
    Enumeration localEnumeration = paramHttpServletRequest.getAttributeNames();
    while (localEnumeration.hasMoreElements())
    {
      localObject1 = localEnumeration.nextElement().toString();
      str2 = paramHttpServletRequest.getAttribute((String)localObject1).toString();
      localProperties.put(localObject1, str2);
    }
    localEnumeration = paramHttpServletRequest.getParameterNames();
    while (localEnumeration.hasMoreElements())
    {
      localObject1 = localEnumeration.nextElement().toString();
      str2 = paramHttpServletRequest.getParameter((String)localObject1);
      localProperties.put(localObject1, str2);
    }
    Object localObject1 = null;
    String str2 = localProperties.getProperty("jsessionid");
    if (str2 != null) {
      localObject1 = this.server.getSession(str2);
    }
    WebApp localWebApp = new WebApp(this.server);
    localWebApp.setSession((WebSession)localObject1, localProperties);
    String str3 = paramHttpServletRequest.getHeader("if-modified-since");
    
    String str4 = paramHttpServletRequest.getRemoteAddr();
    str1 = localWebApp.processRequest(str1, str4);
    localObject1 = localWebApp.getSession();
    
    String str5 = localWebApp.getMimeType();
    boolean bool = localWebApp.getCache();
    if ((bool) && (this.server.getStartDateTime().equals(str3)))
    {
      paramHttpServletResponse.setStatus(304);
      return;
    }
    byte[] arrayOfByte = this.server.getFile(str1);
    Object localObject2;
    if (arrayOfByte == null)
    {
      paramHttpServletResponse.sendError(404);
      arrayOfByte = ("File not found: " + str1).getBytes(Constants.UTF8);
    }
    else
    {
      if ((localObject1 != null) && (str1.endsWith(".jsp")))
      {
        localObject2 = new String(arrayOfByte, Constants.UTF8);
        localObject2 = PageParser.parse((String)localObject2, ((WebSession)localObject1).map);
        arrayOfByte = ((String)localObject2).getBytes(Constants.UTF8);
      }
      paramHttpServletResponse.setContentType(str5);
      if (!bool)
      {
        paramHttpServletResponse.setHeader("Cache-Control", "no-cache");
      }
      else
      {
        paramHttpServletResponse.setHeader("Cache-Control", "max-age=10");
        paramHttpServletResponse.setHeader("Last-Modified", this.server.getStartDateTime());
      }
    }
    if (arrayOfByte != null)
    {
      localObject2 = paramHttpServletResponse.getOutputStream();
      ((ServletOutputStream)localObject2).write(arrayOfByte);
    }
  }
  
  public void doPost(HttpServletRequest paramHttpServletRequest, HttpServletResponse paramHttpServletResponse)
    throws IOException
  {
    doGet(paramHttpServletRequest, paramHttpServletResponse);
  }
}
