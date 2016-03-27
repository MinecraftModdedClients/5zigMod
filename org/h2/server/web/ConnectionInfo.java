package org.h2.server.web;

import org.h2.util.MathUtils;
import org.h2.util.StringUtils;

public class ConnectionInfo
  implements Comparable<ConnectionInfo>
{
  public String driver;
  public String url;
  public String user;
  String name;
  int lastAccess;
  
  ConnectionInfo() {}
  
  public ConnectionInfo(String paramString)
  {
    String[] arrayOfString = StringUtils.arraySplit(paramString, '|', false);
    this.name = get(arrayOfString, 0);
    this.driver = get(arrayOfString, 1);
    this.url = get(arrayOfString, 2);
    this.user = get(arrayOfString, 3);
  }
  
  private static String get(String[] paramArrayOfString, int paramInt)
  {
    return (paramArrayOfString != null) && (paramArrayOfString.length > paramInt) ? paramArrayOfString[paramInt] : "";
  }
  
  String getString()
  {
    return StringUtils.arrayCombine(new String[] { this.name, this.driver, this.url, this.user }, '|');
  }
  
  public int compareTo(ConnectionInfo paramConnectionInfo)
  {
    return -MathUtils.compareInt(this.lastAccess, paramConnectionInfo.lastAccess);
  }
}
