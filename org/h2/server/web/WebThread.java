package org.h2.server.web;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Properties;
import java.util.StringTokenizer;
import org.h2.engine.Constants;
import org.h2.engine.SysProperties;
import org.h2.message.DbException;
import org.h2.mvstore.DataUtils;
import org.h2.util.IOUtils;
import org.h2.util.NetUtils;
import org.h2.util.StringUtils;

class WebThread
  extends WebApp
  implements Runnable
{
  protected OutputStream output;
  protected final Socket socket;
  private final Thread thread;
  private InputStream input;
  private int headerBytes;
  private String ifModifiedSince;
  
  WebThread(Socket paramSocket, WebServer paramWebServer)
  {
    super(paramWebServer);
    this.socket = paramSocket;
    this.thread = new Thread(this, "H2 Console thread");
  }
  
  void start()
  {
    this.thread.start();
  }
  
  void join(int paramInt)
    throws InterruptedException
  {
    this.thread.join(paramInt);
  }
  
  void stopNow()
  {
    this.stop = true;
    try
    {
      this.socket.close();
    }
    catch (IOException localIOException) {}
  }
  
  private String getAllowedFile(String paramString)
  {
    if (!allow()) {
      return "notAllowed.jsp";
    }
    if (paramString.length() == 0) {
      return "index.do";
    }
    return paramString;
  }
  
  public void run()
  {
    try
    {
      this.input = new BufferedInputStream(this.socket.getInputStream());
      this.output = new BufferedOutputStream(this.socket.getOutputStream());
      while (!this.stop) {
        if (!process()) {
          break;
        }
      }
    }
    catch (Exception localException)
    {
      DbException.traceThrowable(localException);
    }
    IOUtils.closeSilently(this.output);
    IOUtils.closeSilently(this.input);
    try
    {
      this.socket.close();
    }
    catch (IOException localIOException) {}finally
    {
      this.server.remove(this);
    }
  }
  
  private boolean process()
    throws IOException
  {
    boolean bool = false;
    String str1 = readHeaderLine();
    if ((str1.startsWith("GET ")) || (str1.startsWith("POST ")))
    {
      int i = str1.indexOf('/');int j = str1.lastIndexOf(' ');
      if ((i < 0) || (j < i)) {
        str2 = "";
      } else {
        str2 = str1.substring(i + 1, j).trim();
      }
      trace(str1 + ": " + str2);
      String str2 = getAllowedFile(str2);
      this.attributes = new Properties();
      int k = str2.indexOf("?");
      this.session = null;
      if (k >= 0)
      {
        str3 = str2.substring(k + 1);
        parseAttributes(str3);
        str4 = this.attributes.getProperty("jsessionid");
        str2 = str2.substring(0, k);
        this.session = this.server.getSession(str4);
      }
      bool = parseHeader();
      String str3 = this.socket.getInetAddress().getHostAddress();
      str2 = processRequest(str2, str3);
      if (str2.length() == 0) {
        return true;
      }
      byte[] arrayOfByte;
      if ((this.cache) && (this.ifModifiedSince != null) && (this.ifModifiedSince.equals(this.server.getStartDateTime())))
      {
        arrayOfByte = null;
        str4 = "HTTP/1.1 304 Not Modified\r\n";
      }
      else
      {
        arrayOfByte = this.server.getFile(str2);
        if (arrayOfByte == null)
        {
          str4 = "HTTP/1.1 404 Not Found\r\n";
          arrayOfByte = ("File not found: " + str2).getBytes(Constants.UTF8);
          str4 = str4 + "Content-Length: " + arrayOfByte.length + "\r\n";
        }
        else
        {
          if ((this.session != null) && (str2.endsWith(".jsp")))
          {
            String str5 = new String(arrayOfByte, Constants.UTF8);
            if (SysProperties.CONSOLE_STREAM)
            {
              Iterator localIterator = (Iterator)this.session.map.remove("chunks");
              if (localIterator != null)
              {
                str4 = "HTTP/1.1 200 OK\r\n";
                str4 = str4 + "Content-Type: " + this.mimeType + "\r\n";
                str4 = str4 + "Cache-Control: no-cache\r\n";
                str4 = str4 + "Transfer-Encoding: chunked\r\n";
                str4 = str4 + "\r\n";
                trace(str4);
                this.output.write(str4.getBytes());
                while (localIterator.hasNext())
                {
                  String str6 = (String)localIterator.next();
                  str6 = PageParser.parse(str6, this.session.map);
                  arrayOfByte = str6.getBytes(Constants.UTF8);
                  if (arrayOfByte.length != 0)
                  {
                    this.output.write(Integer.toHexString(arrayOfByte.length).getBytes());
                    this.output.write("\r\n".getBytes());
                    this.output.write(arrayOfByte);
                    this.output.write("\r\n".getBytes());
                    this.output.flush();
                  }
                }
                this.output.write("0\r\n\r\n".getBytes());
                this.output.flush();
                return bool;
              }
            }
            str5 = PageParser.parse(str5, this.session.map);
            arrayOfByte = str5.getBytes(Constants.UTF8);
          }
          str4 = "HTTP/1.1 200 OK\r\n";
          str4 = str4 + "Content-Type: " + this.mimeType + "\r\n";
          if (!this.cache)
          {
            str4 = str4 + "Cache-Control: no-cache\r\n";
          }
          else
          {
            str4 = str4 + "Cache-Control: max-age=10\r\n";
            str4 = str4 + "Last-Modified: " + this.server.getStartDateTime() + "\r\n";
          }
          str4 = str4 + "Content-Length: " + arrayOfByte.length + "\r\n";
        }
      }
      String str4 = str4 + "\r\n";
      trace(str4);
      this.output.write(str4.getBytes());
      if (arrayOfByte != null) {
        this.output.write(arrayOfByte);
      }
      this.output.flush();
    }
    return bool;
  }
  
  private String readHeaderLine()
    throws IOException
  {
    StringBuilder localStringBuilder = new StringBuilder();
    for (;;)
    {
      this.headerBytes += 1;
      int i = this.input.read();
      if (i == -1) {
        throw new IOException("Unexpected EOF");
      }
      if (i == 13)
      {
        this.headerBytes += 1;
        if (this.input.read() == 10) {
          return localStringBuilder.length() > 0 ? localStringBuilder.toString() : null;
        }
      }
      else
      {
        if (i == 10) {
          return localStringBuilder.length() > 0 ? localStringBuilder.toString() : null;
        }
        localStringBuilder.append((char)i);
      }
    }
  }
  
  private void parseAttributes(String paramString)
  {
    trace("data=" + paramString);
    while (paramString != null)
    {
      int i = paramString.indexOf('=');
      if (i < 0) {
        break;
      }
      String str1 = paramString.substring(0, i);
      paramString = paramString.substring(i + 1);
      i = paramString.indexOf('&');
      String str2;
      if (i >= 0)
      {
        str2 = paramString.substring(0, i);
        paramString = paramString.substring(i + 1);
      }
      else
      {
        str2 = paramString;
      }
      String str3 = StringUtils.urlDecode(str2);
      this.attributes.put(str1, str3);
    }
    trace(this.attributes.toString());
  }
  
  private boolean parseHeader()
    throws IOException
  {
    boolean bool1 = false;
    trace("parseHeader");
    int i = 0;
    this.ifModifiedSince = null;
    int j = 0;
    Object localObject;
    for (;;)
    {
      localObject = readHeaderLine();
      if (localObject == null) {
        break;
      }
      trace(" " + (String)localObject);
      String str1 = StringUtils.toLowerEnglish((String)localObject);
      if (str1.startsWith("if-modified-since"))
      {
        this.ifModifiedSince = getHeaderLineValue((String)localObject);
      }
      else
      {
        String str3;
        if (str1.startsWith("connection"))
        {
          str3 = getHeaderLineValue((String)localObject);
          if ("keep-alive".equals(str3)) {
            bool1 = true;
          }
        }
        else if (str1.startsWith("content-type"))
        {
          str3 = getHeaderLineValue((String)localObject);
          if (str3.startsWith("multipart/form-data")) {
            j = 1;
          }
        }
        else if (str1.startsWith("content-length"))
        {
          i = Integer.parseInt(getHeaderLineValue((String)localObject));
          trace("len=" + i);
        }
        else if (str1.startsWith("user-agent"))
        {
          boolean bool2 = str1.contains("webkit/");
          if ((bool2) && (this.session != null))
          {
            this.session.put("frame-border", "1");
            this.session.put("frameset-border", "2");
          }
        }
        else if (str1.startsWith("accept-language"))
        {
          Locale localLocale = this.session == null ? null : this.session.locale;
          if (localLocale == null)
          {
            String str4 = getHeaderLineValue((String)localObject);
            StringTokenizer localStringTokenizer = new StringTokenizer(str4, ",;");
            for (; localStringTokenizer.hasMoreTokens(); goto 464)
            {
              String str5 = localStringTokenizer.nextToken();
              if ((!str5.startsWith("q=")) && 
                (this.server.supportsLanguage(str5)))
              {
                int m = str5.indexOf('-');
                if (m >= 0)
                {
                  String str6 = str5.substring(0, m);
                  String str7 = str5.substring(m + 1);
                  localLocale = new Locale(str6, str7);
                }
                else
                {
                  localLocale = new Locale(str5, "");
                }
                this.headerLanguage = localLocale.getLanguage();
                if (this.session == null) {
                  break;
                }
                this.session.locale = localLocale;
                this.session.put("language", this.headerLanguage);
                this.server.readTranslations(this.session, this.headerLanguage);
              }
            }
          }
        }
        else
        {
          if (((String)localObject).trim().length() == 0) {
            break;
          }
        }
      }
    }
    if (j != 0)
    {
      uploadMultipart(this.input, i);
    }
    else if ((this.session != null) && (i > 0))
    {
      localObject = DataUtils.newBytes(i);
      for (int k = 0; k < i;) {
        k += this.input.read((byte[])localObject, k, i - k);
      }
      String str2 = new String((byte[])localObject);
      parseAttributes(str2);
    }
    return bool1;
  }
  
  private void uploadMultipart(InputStream paramInputStream, int paramInt)
    throws IOException
  {
    if (!new File("transfer").exists()) {
      return;
    }
    String str1 = "temp.bin";
    this.headerBytes = 0;
    String str2 = readHeaderLine();
    for (;;)
    {
      localObject = readHeaderLine();
      if (localObject == null) {
        break;
      }
      int i = ((String)localObject).indexOf("filename=\"");
      if (i > 0) {
        str1 = ((String)localObject).substring(i + "filename=\"".length(), ((String)localObject).lastIndexOf('"'));
      }
      trace(" " + (String)localObject);
    }
    if (!WebServer.isSimpleName(str1)) {
      return;
    }
    paramInt -= this.headerBytes;
    Object localObject = new File("transfer", str1);
    FileOutputStream localFileOutputStream = new FileOutputStream((File)localObject);
    IOUtils.copy(paramInputStream, localFileOutputStream, paramInt);
    localFileOutputStream.close();
    
    RandomAccessFile localRandomAccessFile = new RandomAccessFile((File)localObject, "rw");
    int j = (int)Math.min(localRandomAccessFile.length(), 4096L);
    localRandomAccessFile.seek(localRandomAccessFile.length() - j);
    byte[] arrayOfByte = DataUtils.newBytes(4096);
    localRandomAccessFile.readFully(arrayOfByte, 0, j);
    String str3 = new String(arrayOfByte, "ASCII");
    int k = str3.lastIndexOf(str2);
    localRandomAccessFile.setLength(localRandomAccessFile.length() - j + k - 2L);
    localRandomAccessFile.close();
  }
  
  private static String getHeaderLineValue(String paramString)
  {
    return paramString.substring(paramString.indexOf(':') + 1).trim();
  }
  
  protected String adminShutdown()
  {
    stopNow();
    return super.adminShutdown();
  }
  
  private boolean allow()
  {
    if (this.server.getAllowOthers()) {
      return true;
    }
    try
    {
      return NetUtils.isLocalAddress(this.socket);
    }
    catch (UnknownHostException localUnknownHostException)
    {
      this.server.traceError(localUnknownHostException);
    }
    return false;
  }
  
  private void trace(String paramString)
  {
    this.server.trace(paramString);
  }
}
