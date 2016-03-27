package org.h2.tools;

import java.awt.Button;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Label;
import java.awt.MenuItem;
import java.awt.Panel;
import java.awt.PopupMenu;
import java.awt.SystemColor;
import java.awt.TextField;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.SQLException;
import org.h2.server.ShutdownHandler;
import org.h2.util.JdbcUtils;
import org.h2.util.Tool;
import org.h2.util.Utils;

public class Console
  extends Tool
  implements ActionListener, MouseListener, WindowListener, ShutdownHandler
{
  private Frame frame;
  private boolean trayIconUsed;
  private Font font;
  private Button startBrowser;
  private TextField urlText;
  private Object tray;
  private Object trayIcon;
  private Server web;
  private Server tcp;
  private Server pg;
  private boolean isWindows;
  private long lastOpen;
  
  public static void main(String... paramVarArgs)
    throws SQLException
  {
    new Console().runTool(paramVarArgs);
  }
  
  public void runTool(String... paramVarArgs)
    throws SQLException
  {
    this.isWindows = Utils.getProperty("os.name", "").startsWith("Windows");
    int i = 0;int j = 0;int k = 0;int m = 0;
    int n = 0;
    int i1 = 1;
    int i2 = (paramVarArgs != null) && (paramVarArgs.length > 0) ? 1 : 0;
    String str1 = null;String str2 = null;String str3 = null;String str4 = null;
    int i3 = 0;boolean bool = false;
    String str5 = "";
    String str6 = "";
    for (int i4 = 0; (paramVarArgs != null) && (i4 < paramVarArgs.length); i4++)
    {
      String str7 = paramVarArgs[i4];
      if (str7 != null)
      {
        if (("-?".equals(str7)) || ("-help".equals(str7)))
        {
          showUsage();
          return;
        }
        if ("-url".equals(str7))
        {
          i1 = 0;
          str2 = paramVarArgs[(++i4)];
        }
        else if ("-driver".equals(str7))
        {
          str1 = paramVarArgs[(++i4)];
        }
        else if ("-user".equals(str7))
        {
          str3 = paramVarArgs[(++i4)];
        }
        else if ("-password".equals(str7))
        {
          str4 = paramVarArgs[(++i4)];
        }
        else if (str7.startsWith("-web"))
        {
          if ("-web".equals(str7))
          {
            i1 = 0;
            k = 1;
          }
          else if (!"-webAllowOthers".equals(str7))
          {
            if (!"-webDaemon".equals(str7)) {
              if (!"-webSSL".equals(str7)) {
                if ("-webPort".equals(str7)) {
                  i4++;
                } else {
                  showUsageAndThrowUnsupportedOption(str7);
                }
              }
            }
          }
        }
        else if ("-tool".equals(str7))
        {
          i1 = 0;
          k = 1;
          m = 1;
        }
        else if ("-browser".equals(str7))
        {
          i1 = 0;
          k = 1;
          n = 1;
        }
        else if (str7.startsWith("-tcp"))
        {
          if ("-tcp".equals(str7))
          {
            i1 = 0;
            i = 1;
          }
          else if (!"-tcpAllowOthers".equals(str7))
          {
            if (!"-tcpDaemon".equals(str7)) {
              if (!"-tcpSSL".equals(str7)) {
                if ("-tcpPort".equals(str7))
                {
                  i4++;
                }
                else if ("-tcpPassword".equals(str7))
                {
                  str5 = paramVarArgs[(++i4)];
                }
                else if ("-tcpShutdown".equals(str7))
                {
                  i1 = 0;
                  i3 = 1;
                  str6 = paramVarArgs[(++i4)];
                }
                else if ("-tcpShutdownForce".equals(str7))
                {
                  bool = true;
                }
                else
                {
                  showUsageAndThrowUnsupportedOption(str7);
                }
              }
            }
          }
        }
        else if (str7.startsWith("-pg"))
        {
          if ("-pg".equals(str7))
          {
            i1 = 0;
            j = 1;
          }
          else if (!"-pgAllowOthers".equals(str7))
          {
            if (!"-pgDaemon".equals(str7)) {
              if ("-pgPort".equals(str7)) {
                i4++;
              } else {
                showUsageAndThrowUnsupportedOption(str7);
              }
            }
          }
        }
        else if ("-properties".equals(str7))
        {
          i4++;
        }
        else if (!"-trace".equals(str7))
        {
          if (!"-ifExists".equals(str7)) {
            if ("-baseDir".equals(str7)) {
              i4++;
            } else {
              showUsageAndThrowUnsupportedOption(str7);
            }
          }
        }
      }
    }
    if (i1 != 0)
    {
      k = 1;
      m = 1;
      n = 1;
      i = 1;
      j = 1;
    }
    if (i3 != 0)
    {
      this.out.println("Shutting down TCP Server at " + str6);
      Server.shutdownTcpServer(str6, str5, bool, false);
    }
    Object localObject = null;
    int i5 = 0;
    if (str2 != null)
    {
      Connection localConnection = JdbcUtils.getConnection(str1, str2, str3, str4);
      Server.startWebServer(localConnection);
    }
    if (k != 0) {
      try
      {
        this.web = Server.createWebServer(paramVarArgs);
        this.web.setShutdownHandler(this);
        this.web.start();
        if (i2 != 0) {
          this.out.println(this.web.getStatus());
        }
        i5 = 1;
      }
      catch (SQLException localSQLException1)
      {
        printProblem(localSQLException1, this.web);
        localObject = localSQLException1;
      }
    }
    if ((m != 0) && (i5 != 0) && (!GraphicsEnvironment.isHeadless()))
    {
      loadFont();
      try
      {
        if (!createTrayIcon()) {
          showWindow();
        }
      }
      catch (Exception localException)
      {
        localException.printStackTrace();
      }
    }
    if ((n != 0) && (this.web != null)) {
      openBrowser(this.web.getURL());
    }
    if (i != 0) {
      try
      {
        this.tcp = Server.createTcpServer(paramVarArgs);
        this.tcp.start();
        if (i2 != 0) {
          this.out.println(this.tcp.getStatus());
        }
        this.tcp.setShutdownHandler(this);
      }
      catch (SQLException localSQLException2)
      {
        printProblem(localSQLException2, this.tcp);
        if (localObject == null) {
          localObject = localSQLException2;
        }
      }
    }
    if (j != 0) {
      try
      {
        this.pg = Server.createPgServer(paramVarArgs);
        this.pg.start();
        if (i2 != 0) {
          this.out.println(this.pg.getStatus());
        }
      }
      catch (SQLException localSQLException3)
      {
        printProblem(localSQLException3, this.pg);
        if (localObject == null) {
          localObject = localSQLException3;
        }
      }
    }
    if (localObject != null)
    {
      shutdown();
      throw ((Throwable)localObject);
    }
  }
  
  private void printProblem(Exception paramException, Server paramServer)
  {
    if (paramServer == null)
    {
      paramException.printStackTrace();
    }
    else
    {
      this.out.println(paramServer.getStatus());
      this.out.println("Root cause: " + paramException.getMessage());
    }
  }
  
  private static Image loadImage(String paramString)
  {
    try
    {
      byte[] arrayOfByte = Utils.getResource(paramString);
      if (arrayOfByte == null) {
        return null;
      }
      return Toolkit.getDefaultToolkit().createImage(arrayOfByte);
    }
    catch (IOException localIOException)
    {
      localIOException.printStackTrace();
    }
    return null;
  }
  
  public void shutdown()
  {
    if ((this.web != null) && (this.web.isRunning(false)))
    {
      this.web.stop();
      this.web = null;
    }
    if ((this.tcp != null) && (this.tcp.isRunning(false)))
    {
      this.tcp.stop();
      this.tcp = null;
    }
    if ((this.pg != null) && (this.pg.isRunning(false)))
    {
      this.pg.stop();
      this.pg = null;
    }
    if (this.frame != null)
    {
      this.frame.dispose();
      this.frame = null;
    }
    if (this.trayIconUsed)
    {
      try
      {
        Utils.callMethod(this.tray, "remove", new Object[] { this.trayIcon });
      }
      catch (Exception localException) {}finally
      {
        this.trayIcon = null;
        this.tray = null;
        this.trayIconUsed = false;
      }
      System.gc();
    }
  }
  
  private void loadFont()
  {
    if (this.isWindows) {
      this.font = new Font("Dialog", 0, 11);
    } else {
      this.font = new Font("Dialog", 0, 12);
    }
  }
  
  private boolean createTrayIcon()
  {
    try
    {
      boolean bool = ((Boolean)Utils.callStaticMethod("java.awt.SystemTray.isSupported", new Object[0])).booleanValue();
      if (!bool) {
        return false;
      }
      PopupMenu localPopupMenu = new PopupMenu();
      MenuItem localMenuItem1 = new MenuItem("H2 Console");
      localMenuItem1.setActionCommand("console");
      localMenuItem1.addActionListener(this);
      localMenuItem1.setFont(this.font);
      localPopupMenu.add(localMenuItem1);
      MenuItem localMenuItem2 = new MenuItem("Status");
      localMenuItem2.setActionCommand("status");
      localMenuItem2.addActionListener(this);
      localMenuItem2.setFont(this.font);
      localPopupMenu.add(localMenuItem2);
      MenuItem localMenuItem3 = new MenuItem("Exit");
      localMenuItem3.setFont(this.font);
      localMenuItem3.setActionCommand("exit");
      localMenuItem3.addActionListener(this);
      localPopupMenu.add(localMenuItem3);
      
      this.tray = Utils.callStaticMethod("java.awt.SystemTray.getSystemTray", new Object[0]);
      
      Dimension localDimension = (Dimension)Utils.callMethod(this.tray, "getTrayIconSize", new Object[0]);
      String str;
      if ((localDimension.width >= 24) && (localDimension.height >= 24)) {
        str = "/org/h2/res/h2-24.png";
      } else if ((localDimension.width >= 22) && (localDimension.height >= 22)) {
        str = "/org/h2/res/h2-64-t.png";
      } else {
        str = "/org/h2/res/h2.png";
      }
      Image localImage = loadImage(str);
      
      this.trayIcon = Utils.newInstance("java.awt.TrayIcon", new Object[] { localImage, "H2 Database Engine", localPopupMenu });
      
      Utils.callMethod(this.trayIcon, "addMouseListener", new Object[] { this });
      
      Utils.callMethod(this.tray, "add", new Object[] { this.trayIcon });
      
      this.trayIconUsed = true;
      
      return true;
    }
    catch (Exception localException) {}
    return false;
  }
  
  private void showWindow()
  {
    if (this.frame != null) {
      return;
    }
    this.frame = new Frame("H2 Console");
    this.frame.addWindowListener(this);
    Image localImage = loadImage("/org/h2/res/h2.png");
    if (localImage != null) {
      this.frame.setIconImage(localImage);
    }
    this.frame.setResizable(false);
    this.frame.setBackground(SystemColor.control);
    
    GridBagLayout localGridBagLayout = new GridBagLayout();
    this.frame.setLayout(localGridBagLayout);
    
    Panel localPanel = new Panel(localGridBagLayout);
    
    GridBagConstraints localGridBagConstraints1 = new GridBagConstraints();
    localGridBagConstraints1.gridx = 0;
    localGridBagConstraints1.weightx = 1.0D;
    localGridBagConstraints1.weighty = 1.0D;
    localGridBagConstraints1.fill = 1;
    localGridBagConstraints1.insets = new Insets(0, 10, 0, 10);
    localGridBagConstraints1.gridy = 0;
    
    GridBagConstraints localGridBagConstraints2 = new GridBagConstraints();
    localGridBagConstraints2.gridx = 0;
    localGridBagConstraints2.gridwidth = 2;
    localGridBagConstraints2.insets = new Insets(10, 0, 0, 0);
    localGridBagConstraints2.gridy = 1;
    localGridBagConstraints2.anchor = 13;
    
    GridBagConstraints localGridBagConstraints3 = new GridBagConstraints();
    localGridBagConstraints3.fill = 2;
    localGridBagConstraints3.gridy = 0;
    localGridBagConstraints3.weightx = 1.0D;
    localGridBagConstraints3.insets = new Insets(0, 5, 0, 0);
    localGridBagConstraints3.gridx = 1;
    
    GridBagConstraints localGridBagConstraints4 = new GridBagConstraints();
    localGridBagConstraints4.gridx = 0;
    localGridBagConstraints4.gridy = 0;
    
    Label localLabel = new Label("H2 Console URL:", 0);
    localLabel.setFont(this.font);
    localPanel.add(localLabel, localGridBagConstraints4);
    
    this.urlText = new TextField();
    this.urlText.setEditable(false);
    this.urlText.setFont(this.font);
    this.urlText.setText(this.web.getURL());
    if (this.isWindows) {
      this.urlText.setFocusable(false);
    }
    localPanel.add(this.urlText, localGridBagConstraints3);
    
    this.startBrowser = new Button("Start Browser");
    this.startBrowser.setFocusable(false);
    this.startBrowser.setActionCommand("console");
    this.startBrowser.addActionListener(this);
    this.startBrowser.setFont(this.font);
    localPanel.add(this.startBrowser, localGridBagConstraints2);
    this.frame.add(localPanel, localGridBagConstraints1);
    
    int i = 300;int j = 120;
    this.frame.setSize(i, j);
    Dimension localDimension = Toolkit.getDefaultToolkit().getScreenSize();
    this.frame.setLocation((localDimension.width - i) / 2, (localDimension.height - j) / 2);
    try
    {
      this.frame.setVisible(true);
    }
    catch (Throwable localThrowable1) {}
    try
    {
      this.frame.setAlwaysOnTop(true);
      this.frame.setAlwaysOnTop(false);
    }
    catch (Throwable localThrowable2) {}
  }
  
  private void startBrowser()
  {
    if (this.web != null)
    {
      String str = this.web.getURL();
      if (this.urlText != null) {
        this.urlText.setText(str);
      }
      long l = System.currentTimeMillis();
      if ((this.lastOpen == 0L) || (this.lastOpen + 100L < l))
      {
        this.lastOpen = l;
        openBrowser(str);
      }
    }
  }
  
  private void openBrowser(String paramString)
  {
    try
    {
      Server.openBrowser(paramString);
    }
    catch (Exception localException)
    {
      this.out.println(localException.getMessage());
    }
  }
  
  public void actionPerformed(ActionEvent paramActionEvent)
  {
    String str = paramActionEvent.getActionCommand();
    if ("exit".equals(str)) {
      shutdown();
    } else if ("console".equals(str)) {
      startBrowser();
    } else if ("status".equals(str)) {
      showWindow();
    } else if (this.startBrowser == paramActionEvent.getSource()) {
      startBrowser();
    }
  }
  
  public void mouseClicked(MouseEvent paramMouseEvent)
  {
    if (paramMouseEvent.getButton() == 1) {
      startBrowser();
    }
  }
  
  public void mouseEntered(MouseEvent paramMouseEvent) {}
  
  public void mouseExited(MouseEvent paramMouseEvent) {}
  
  public void mousePressed(MouseEvent paramMouseEvent) {}
  
  public void mouseReleased(MouseEvent paramMouseEvent) {}
  
  public void windowClosing(WindowEvent paramWindowEvent)
  {
    if (this.trayIconUsed)
    {
      this.frame.dispose();
      this.frame = null;
    }
    else
    {
      shutdown();
    }
  }
  
  public void windowActivated(WindowEvent paramWindowEvent) {}
  
  public void windowClosed(WindowEvent paramWindowEvent) {}
  
  public void windowDeactivated(WindowEvent paramWindowEvent) {}
  
  public void windowDeiconified(WindowEvent paramWindowEvent) {}
  
  public void windowIconified(WindowEvent paramWindowEvent) {}
  
  public void windowOpened(WindowEvent paramWindowEvent) {}
}
