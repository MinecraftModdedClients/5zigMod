package org.h2.tools;

import java.io.PrintStream;
import java.lang.reflect.Method;
import java.net.URI;
import java.sql.Connection;
import java.sql.SQLException;
import org.h2.message.DbException;
import org.h2.server.Service;
import org.h2.server.ShutdownHandler;
import org.h2.server.TcpServer;
import org.h2.server.pg.PgServer;
import org.h2.server.web.WebServer;
import org.h2.util.StringUtils;
import org.h2.util.Tool;
import org.h2.util.Utils;

public class Server
  extends Tool
  implements Runnable, ShutdownHandler
{
  private final Service service;
  private Server web;
  private Server tcp;
  private Server pg;
  private ShutdownHandler shutdownHandler;
  private boolean started;
  
  public Server()
  {
    this.service = null;
  }
  
  public Server(Service paramService, String... paramVarArgs)
    throws SQLException
  {
    verifyArgs(paramVarArgs);
    this.service = paramService;
    try
    {
      paramService.init(paramVarArgs);
    }
    catch (Exception localException)
    {
      throw DbException.toSQLException(localException);
    }
  }
  
  public static void main(String... paramVarArgs)
    throws SQLException
  {
    new Server().runTool(paramVarArgs);
  }
  
  private void verifyArgs(String... paramVarArgs)
    throws SQLException
  {
    for (int i = 0; (paramVarArgs != null) && (i < paramVarArgs.length); i++)
    {
      String str = paramVarArgs[i];
      if (str != null) {
        if ((!"-?".equals(str)) && (!"-help".equals(str))) {
          if (str.startsWith("-web"))
          {
            if (!"-web".equals(str)) {
              if (!"-webAllowOthers".equals(str)) {
                if (!"-webDaemon".equals(str)) {
                  if (!"-webSSL".equals(str)) {
                    if ("-webPort".equals(str)) {
                      i++;
                    } else {
                      throwUnsupportedOption(str);
                    }
                  }
                }
              }
            }
          }
          else if (!"-browser".equals(str)) {
            if (str.startsWith("-tcp"))
            {
              if (!"-tcp".equals(str)) {
                if (!"-tcpAllowOthers".equals(str)) {
                  if (!"-tcpDaemon".equals(str)) {
                    if (!"-tcpSSL".equals(str)) {
                      if ("-tcpPort".equals(str)) {
                        i++;
                      } else if ("-tcpPassword".equals(str)) {
                        i++;
                      } else if ("-tcpShutdown".equals(str)) {
                        i++;
                      } else if (!"-tcpShutdownForce".equals(str)) {
                        throwUnsupportedOption(str);
                      }
                    }
                  }
                }
              }
            }
            else if (str.startsWith("-pg"))
            {
              if (!"-pg".equals(str)) {
                if (!"-pgAllowOthers".equals(str)) {
                  if (!"-pgDaemon".equals(str)) {
                    if ("-pgPort".equals(str)) {
                      i++;
                    } else {
                      throwUnsupportedOption(str);
                    }
                  }
                }
              }
            }
            else if (str.startsWith("-ftp"))
            {
              if ("-ftpPort".equals(str)) {
                i++;
              } else if ("-ftpDir".equals(str)) {
                i++;
              } else if ("-ftpRead".equals(str)) {
                i++;
              } else if ("-ftpWrite".equals(str)) {
                i++;
              } else if ("-ftpWritePassword".equals(str)) {
                i++;
              } else if (!"-ftpTask".equals(str)) {
                throwUnsupportedOption(str);
              }
            }
            else if ("-properties".equals(str)) {
              i++;
            } else if (!"-trace".equals(str)) {
              if (!"-ifExists".equals(str)) {
                if ("-baseDir".equals(str)) {
                  i++;
                } else if ("-key".equals(str)) {
                  i += 2;
                } else if (!"-tool".equals(str)) {
                  throwUnsupportedOption(str);
                }
              }
            }
          }
        }
      }
    }
  }
  
  public void runTool(String... paramVarArgs)
    throws SQLException
  {
    int i = 0;int j = 0;int k = 0;
    int m = 0;
    int n = 0;boolean bool = false;
    String str1 = "";
    String str2 = "";
    int i1 = 1;
    for (int i2 = 0; (paramVarArgs != null) && (i2 < paramVarArgs.length); i2++)
    {
      String str3 = paramVarArgs[i2];
      if (str3 != null)
      {
        if (("-?".equals(str3)) || ("-help".equals(str3)))
        {
          showUsage();
          return;
        }
        if (str3.startsWith("-web"))
        {
          if ("-web".equals(str3))
          {
            i1 = 0;
            k = 1;
          }
          else if (!"-webAllowOthers".equals(str3))
          {
            if (!"-webDaemon".equals(str3)) {
              if (!"-webSSL".equals(str3)) {
                if ("-webPort".equals(str3)) {
                  i2++;
                } else {
                  showUsageAndThrowUnsupportedOption(str3);
                }
              }
            }
          }
        }
        else if ("-browser".equals(str3))
        {
          i1 = 0;
          m = 1;
        }
        else if (str3.startsWith("-tcp"))
        {
          if ("-tcp".equals(str3))
          {
            i1 = 0;
            i = 1;
          }
          else if (!"-tcpAllowOthers".equals(str3))
          {
            if (!"-tcpDaemon".equals(str3)) {
              if (!"-tcpSSL".equals(str3)) {
                if ("-tcpPort".equals(str3))
                {
                  i2++;
                }
                else if ("-tcpPassword".equals(str3))
                {
                  str1 = paramVarArgs[(++i2)];
                }
                else if ("-tcpShutdown".equals(str3))
                {
                  i1 = 0;
                  n = 1;
                  str2 = paramVarArgs[(++i2)];
                }
                else if ("-tcpShutdownForce".equals(str3))
                {
                  bool = true;
                }
                else
                {
                  showUsageAndThrowUnsupportedOption(str3);
                }
              }
            }
          }
        }
        else if (str3.startsWith("-pg"))
        {
          if ("-pg".equals(str3))
          {
            i1 = 0;
            j = 1;
          }
          else if (!"-pgAllowOthers".equals(str3))
          {
            if (!"-pgDaemon".equals(str3)) {
              if ("-pgPort".equals(str3)) {
                i2++;
              } else {
                showUsageAndThrowUnsupportedOption(str3);
              }
            }
          }
        }
        else if ("-properties".equals(str3))
        {
          i2++;
        }
        else if (!"-trace".equals(str3))
        {
          if (!"-ifExists".equals(str3)) {
            if ("-baseDir".equals(str3)) {
              i2++;
            } else if ("-key".equals(str3)) {
              i2 += 2;
            } else {
              showUsageAndThrowUnsupportedOption(str3);
            }
          }
        }
      }
    }
    verifyArgs(paramVarArgs);
    if (i1 != 0)
    {
      i = 1;
      j = 1;
      k = 1;
      m = 1;
    }
    if (n != 0)
    {
      this.out.println("Shutting down TCP Server at " + str2);
      shutdownTcpServer(str2, str1, bool, false);
    }
    try
    {
      if (i != 0)
      {
        this.tcp = createTcpServer(paramVarArgs);
        this.tcp.start();
        this.out.println(this.tcp.getStatus());
        this.tcp.setShutdownHandler(this);
      }
      if (j != 0)
      {
        this.pg = createPgServer(paramVarArgs);
        this.pg.start();
        this.out.println(this.pg.getStatus());
      }
      if (k != 0)
      {
        this.web = createWebServer(paramVarArgs);
        this.web.setShutdownHandler(this);
        SQLException localSQLException1 = null;
        try
        {
          this.web.start();
        }
        catch (Exception localException1)
        {
          localSQLException1 = DbException.toSQLException(localException1);
        }
        this.out.println(this.web.getStatus());
        if (m != 0) {
          try
          {
            openBrowser(this.web.getURL());
          }
          catch (Exception localException2)
          {
            this.out.println(localException2.getMessage());
          }
        }
        if (localSQLException1 != null) {
          throw localSQLException1;
        }
      }
      else if (m != 0)
      {
        this.out.println("The browser can only start if a web server is started (-web)");
      }
    }
    catch (SQLException localSQLException2)
    {
      stopAll();
      throw localSQLException2;
    }
  }
  
  public static void shutdownTcpServer(String paramString1, String paramString2, boolean paramBoolean1, boolean paramBoolean2)
    throws SQLException
  {
    TcpServer.shutdown(paramString1, paramString2, paramBoolean1, paramBoolean2);
  }
  
  public String getStatus()
  {
    StringBuilder localStringBuilder = new StringBuilder();
    if (!this.started)
    {
      localStringBuilder.append("Not started");
    }
    else if (isRunning(false))
    {
      localStringBuilder.append(this.service.getType()).append(" server running at ").append(this.service.getURL()).append(" (");
      if (this.service.getAllowOthers()) {
        localStringBuilder.append("others can connect");
      } else {
        localStringBuilder.append("only local connections");
      }
      localStringBuilder.append(')');
    }
    else
    {
      localStringBuilder.append("The ").append(this.service.getType()).append(" server could not be started. Possible cause: another server is already running at ").append(this.service.getURL());
    }
    return localStringBuilder.toString();
  }
  
  public static Server createWebServer(String... paramVarArgs)
    throws SQLException
  {
    WebServer localWebServer = new WebServer();
    Server localServer = new Server(localWebServer, paramVarArgs);
    localWebServer.setShutdownHandler(localServer);
    return localServer;
  }
  
  public static Server createTcpServer(String... paramVarArgs)
    throws SQLException
  {
    TcpServer localTcpServer = new TcpServer();
    Server localServer = new Server(localTcpServer, paramVarArgs);
    localTcpServer.setShutdownHandler(localServer);
    return localServer;
  }
  
  public static Server createPgServer(String... paramVarArgs)
    throws SQLException
  {
    return new Server(new PgServer(), paramVarArgs);
  }
  
  public Server start()
    throws SQLException
  {
    try
    {
      this.started = true;
      this.service.start();
      String str = this.service.getName() + " (" + this.service.getURL() + ")";
      Thread localThread = new Thread(this, str);
      localThread.setDaemon(this.service.isDaemon());
      localThread.start();
      for (int i = 1; i < 64; i += i)
      {
        wait(i);
        if (isRunning(false)) {
          return this;
        }
      }
      if (isRunning(true)) {
        return this;
      }
      throw DbException.get(90061, new String[] { str, "timeout; please check your network configuration, specially the file /etc/hosts" });
    }
    catch (DbException localDbException)
    {
      throw DbException.toSQLException(localDbException);
    }
  }
  
  private static void wait(int paramInt)
  {
    try
    {
      long l = paramInt * paramInt;
      Thread.sleep(l);
    }
    catch (InterruptedException localInterruptedException) {}
  }
  
  private void stopAll()
  {
    Server localServer = this.web;
    if ((localServer != null) && (localServer.isRunning(false)))
    {
      localServer.stop();
      this.web = null;
    }
    localServer = this.tcp;
    if ((localServer != null) && (localServer.isRunning(false)))
    {
      localServer.stop();
      this.tcp = null;
    }
    localServer = this.pg;
    if ((localServer != null) && (localServer.isRunning(false)))
    {
      localServer.stop();
      this.pg = null;
    }
  }
  
  public boolean isRunning(boolean paramBoolean)
  {
    return this.service.isRunning(paramBoolean);
  }
  
  public void stop()
  {
    this.started = false;
    if (this.service != null) {
      this.service.stop();
    }
  }
  
  public String getURL()
  {
    return this.service.getURL();
  }
  
  public int getPort()
  {
    return this.service.getPort();
  }
  
  public void run()
  {
    try
    {
      this.service.listen();
    }
    catch (Exception localException)
    {
      DbException.traceThrowable(localException);
    }
  }
  
  public void setShutdownHandler(ShutdownHandler paramShutdownHandler)
  {
    this.shutdownHandler = paramShutdownHandler;
  }
  
  public void shutdown()
  {
    if (this.shutdownHandler != null) {
      this.shutdownHandler.shutdown();
    } else {
      stopAll();
    }
  }
  
  public Service getService()
  {
    return this.service;
  }
  
  public static void openBrowser(String paramString)
    throws Exception
  {
    try
    {
      String str1 = StringUtils.toLowerEnglish(Utils.getProperty("os.name", "linux"));
      
      Runtime localRuntime = Runtime.getRuntime();
      String str2 = Utils.getProperty("h2.browser", null);
      if (str2 == null) {
        try
        {
          str2 = System.getenv("BROWSER");
        }
        catch (SecurityException localSecurityException) {}
      }
      Object localObject1;
      if (str2 != null)
      {
        if (str2.startsWith("call:"))
        {
          str2 = str2.substring("call:".length());
          Utils.callStaticMethod(str2, new Object[] { paramString });
        }
        else if (str2.contains("%url"))
        {
          localObject1 = StringUtils.arraySplit(str2, ',', false);
          for (int i = 0; i < localObject1.length; i++) {
            localObject1[i] = StringUtils.replaceAll(localObject1[i], "%url", paramString);
          }
          localRuntime.exec((String[])localObject1);
        }
        else if (str1.contains("windows"))
        {
          localRuntime.exec(new String[] { "cmd.exe", "/C", str2, paramString });
        }
        else
        {
          localRuntime.exec(new String[] { str2, paramString });
        }
        return;
      }
      Object localObject2;
      try
      {
        localObject1 = Class.forName("java.awt.Desktop");
        
        Boolean localBoolean = (Boolean)((Class)localObject1).getMethod("isDesktopSupported", new Class[0]).invoke(null, new Object[0]);
        
        localObject2 = new URI(paramString);
        if (localBoolean.booleanValue())
        {
          Object localObject3 = ((Class)localObject1).getMethod("getDesktop", new Class[0]).invoke(null, new Object[0]);
          
          ((Class)localObject1).getMethod("browse", new Class[] { URI.class }).invoke(localObject3, new Object[] { localObject2 });
          
          return;
        }
      }
      catch (Exception localException2) {}
      if (str1.contains("windows"))
      {
        localRuntime.exec(new String[] { "rundll32", "url.dll,FileProtocolHandler", paramString });
      }
      else if ((str1.contains("mac")) || (str1.contains("darwin")))
      {
        Runtime.getRuntime().exec(new String[] { "open", paramString });
      }
      else
      {
        String[] arrayOfString = { "chromium", "google-chrome", "firefox", "mozilla-firefox", "mozilla", "konqueror", "netscape", "opera", "midori" };
        
        int j = 0;
        for (Object localObject4 : arrayOfString) {
          try
          {
            localRuntime.exec(new String[] { localObject4, paramString });
            j = 1;
          }
          catch (Exception localException3) {}
        }
        if (j == 0) {
          throw new Exception("Browser detection failed and system property h2.browser not set");
        }
      }
    }
    catch (Exception localException1)
    {
      throw new Exception("Failed to start a browser to open the URL " + paramString + ": " + localException1.getMessage());
    }
  }
  
  public static void startWebServer(Connection paramConnection)
    throws SQLException
  {
    WebServer localWebServer = new WebServer();
    Server localServer1 = new Server(localWebServer, new String[] { "-webPort", "0" });
    localServer1.start();
    Server localServer2 = new Server();
    localServer2.web = localServer1;
    localWebServer.setShutdownHandler(localServer2);
    String str = localWebServer.addSession(paramConnection);
    try
    {
      openBrowser(str);
      while (!localWebServer.isStopped()) {
        Thread.sleep(1000L);
      }
    }
    catch (Exception localException) {}
  }
}
