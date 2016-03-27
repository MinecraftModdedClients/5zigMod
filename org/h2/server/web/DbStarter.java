package org.h2.server.web;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import org.h2.Driver;
import org.h2.tools.Server;
import org.h2.util.StringUtils;

public class DbStarter
  implements ServletContextListener
{
  private Connection conn;
  private Server server;
  
  public void contextInitialized(ServletContextEvent paramServletContextEvent)
  {
    try
    {
      Driver.load();
      
      ServletContext localServletContext = paramServletContextEvent.getServletContext();
      String str1 = getParameter(localServletContext, "db.url", "jdbc:h2:~/test");
      String str2 = getParameter(localServletContext, "db.user", "sa");
      String str3 = getParameter(localServletContext, "db.password", "sa");
      
      String str4 = getParameter(localServletContext, "db.tcpServer", null);
      if (str4 != null)
      {
        String[] arrayOfString = StringUtils.arraySplit(str4, ' ', true);
        this.server = Server.createTcpServer(arrayOfString);
        this.server.start();
      }
      this.conn = DriverManager.getConnection(str1, str2, str3);
      localServletContext.setAttribute("connection", this.conn);
    }
    catch (Exception localException)
    {
      localException.printStackTrace();
    }
  }
  
  private static String getParameter(ServletContext paramServletContext, String paramString1, String paramString2)
  {
    String str = paramServletContext.getInitParameter(paramString1);
    return str == null ? paramString2 : str;
  }
  
  public Connection getConnection()
  {
    return this.conn;
  }
  
  public void contextDestroyed(ServletContextEvent paramServletContextEvent)
  {
    try
    {
      Statement localStatement = this.conn.createStatement();
      localStatement.execute("SHUTDOWN");
      localStatement.close();
    }
    catch (Exception localException1)
    {
      localException1.printStackTrace();
    }
    try
    {
      this.conn.close();
    }
    catch (Exception localException2)
    {
      localException2.printStackTrace();
    }
    if (this.server != null)
    {
      this.server.stop();
      this.server = null;
    }
  }
}
