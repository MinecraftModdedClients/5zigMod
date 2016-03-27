package org.h2.util;

import java.io.IOException;
import java.net.BindException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import org.h2.engine.SysProperties;
import org.h2.message.DbException;
import org.h2.security.CipherFactory;

public class NetUtils
{
  private static final int CACHE_MILLIS = 1000;
  private static InetAddress cachedBindAddress;
  private static String cachedLocalAddress;
  private static long cachedLocalAddressTime;
  
  public static Socket createLoopbackSocket(int paramInt, boolean paramBoolean)
    throws IOException
  {
    InetAddress localInetAddress = getBindAddress();
    if (localInetAddress == null) {
      localInetAddress = InetAddress.getLocalHost();
    }
    try
    {
      return createSocket(getHostAddress(localInetAddress), paramInt, paramBoolean);
    }
    catch (IOException localIOException1)
    {
      try
      {
        return createSocket("localhost", paramInt, paramBoolean);
      }
      catch (IOException localIOException2)
      {
        throw localIOException1;
      }
    }
  }
  
  private static String getHostAddress(InetAddress paramInetAddress)
  {
    String str = paramInetAddress.getHostAddress();
    if (((paramInetAddress instanceof Inet6Address)) && 
      (str.indexOf(':') >= 0) && (!str.startsWith("["))) {
      str = "[" + str + "]";
    }
    return str;
  }
  
  public static Socket createSocket(String paramString, int paramInt, boolean paramBoolean)
    throws IOException
  {
    int i = paramInt;
    
    int j = paramString.startsWith("[") ? paramString.indexOf(']') : 0;
    int k = paramString.indexOf(':', j);
    if (k >= 0)
    {
      i = Integer.decode(paramString.substring(k + 1)).intValue();
      paramString = paramString.substring(0, k);
    }
    InetAddress localInetAddress = InetAddress.getByName(paramString);
    return createSocket(localInetAddress, i, paramBoolean);
  }
  
  public static Socket createSocket(InetAddress paramInetAddress, int paramInt, boolean paramBoolean)
    throws IOException
  {
    long l1 = System.currentTimeMillis();
    for (int i = 0;; i++) {
      try
      {
        if (paramBoolean) {
          return CipherFactory.createSocket(paramInetAddress, paramInt);
        }
        Socket localSocket = new Socket();
        localSocket.connect(new InetSocketAddress(paramInetAddress, paramInt), SysProperties.SOCKET_CONNECT_TIMEOUT);
        
        return localSocket;
      }
      catch (IOException localIOException)
      {
        if (System.currentTimeMillis() - l1 >= SysProperties.SOCKET_CONNECT_TIMEOUT) {
          throw localIOException;
        }
        if (i >= SysProperties.SOCKET_CONNECT_RETRY) {
          throw localIOException;
        }
        try
        {
          long l2 = Math.min(256, i * i);
          Thread.sleep(l2);
        }
        catch (InterruptedException localInterruptedException) {}
      }
    }
  }
  
  public static ServerSocket createServerSocket(int paramInt, boolean paramBoolean)
  {
    try
    {
      return createServerSocketTry(paramInt, paramBoolean);
    }
    catch (Exception localException) {}
    return createServerSocketTry(paramInt, paramBoolean);
  }
  
  private static InetAddress getBindAddress()
    throws UnknownHostException
  {
    String str = SysProperties.BIND_ADDRESS;
    if ((str == null) || (str.length() == 0)) {
      return null;
    }
    synchronized (NetUtils.class)
    {
      if (cachedBindAddress == null) {
        cachedBindAddress = InetAddress.getByName(str);
      }
    }
    return cachedBindAddress;
  }
  
  private static ServerSocket createServerSocketTry(int paramInt, boolean paramBoolean)
  {
    try
    {
      InetAddress localInetAddress = getBindAddress();
      if (paramBoolean) {
        return CipherFactory.createServerSocket(paramInt, localInetAddress);
      }
      if (localInetAddress == null) {
        return new ServerSocket(paramInt);
      }
      return new ServerSocket(paramInt, 0, localInetAddress);
    }
    catch (BindException localBindException)
    {
      throw DbException.get(90061, localBindException, new String[] { "" + paramInt, localBindException.toString() });
    }
    catch (IOException localIOException)
    {
      throw DbException.convertIOException(localIOException, "port: " + paramInt + " ssl: " + paramBoolean);
    }
  }
  
  public static boolean isLocalAddress(Socket paramSocket)
    throws UnknownHostException
  {
    InetAddress localInetAddress1 = paramSocket.getInetAddress();
    if (localInetAddress1.isLoopbackAddress()) {
      return true;
    }
    InetAddress localInetAddress2 = InetAddress.getLocalHost();
    
    String str = localInetAddress2.getHostAddress();
    for (InetAddress localInetAddress3 : InetAddress.getAllByName(str)) {
      if (localInetAddress1.equals(localInetAddress3)) {
        return true;
      }
    }
    return false;
  }
  
  public static ServerSocket closeSilently(ServerSocket paramServerSocket)
  {
    if (paramServerSocket != null) {
      try
      {
        paramServerSocket.close();
      }
      catch (IOException localIOException) {}
    }
    return null;
  }
  
  public static synchronized String getLocalAddress()
  {
    long l = System.currentTimeMillis();
    if ((cachedLocalAddress != null) && 
      (cachedLocalAddressTime + 1000L > l)) {
      return cachedLocalAddress;
    }
    InetAddress localInetAddress = null;
    int i = 0;
    try
    {
      localInetAddress = getBindAddress();
      if (localInetAddress == null) {
        i = 1;
      }
    }
    catch (UnknownHostException localUnknownHostException1) {}
    if (i != 0) {
      try
      {
        localInetAddress = InetAddress.getLocalHost();
      }
      catch (UnknownHostException localUnknownHostException2)
      {
        throw DbException.convert(localUnknownHostException2);
      }
    }
    String str = localInetAddress == null ? "localhost" : getHostAddress(localInetAddress);
    if (str.equals("127.0.0.1")) {
      str = "localhost";
    }
    cachedLocalAddress = str;
    cachedLocalAddressTime = l;
    return str;
  }
}
