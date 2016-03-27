package org.h2.engine;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import org.h2.command.dml.SetTypes;
import org.h2.message.DbException;
import org.h2.security.SHA256;
import org.h2.store.fs.FilePathEncrypt;
import org.h2.store.fs.FilePathRec;
import org.h2.store.fs.FileUtils;
import org.h2.util.New;
import org.h2.util.SortedProperties;
import org.h2.util.StringUtils;
import org.h2.util.Utils;

public class ConnectionInfo
  implements Cloneable
{
  private static final HashSet<String> KNOWN_SETTINGS = ;
  private Properties prop = new Properties();
  private String originalURL;
  private String url;
  private String user;
  private byte[] filePasswordHash;
  private byte[] fileEncryptionKey;
  private byte[] userPasswordHash;
  private String name;
  private String nameNormalized;
  private boolean remote;
  private boolean ssl;
  private boolean persistent;
  private boolean unnamed;
  
  public ConnectionInfo(String paramString)
  {
    this.name = paramString;
    this.url = ("jdbc:h2:" + paramString);
    parseName();
  }
  
  public ConnectionInfo(String paramString, Properties paramProperties)
  {
    paramString = remapURL(paramString);
    this.originalURL = paramString;
    if (!paramString.startsWith("jdbc:h2:")) {
      throw DbException.getInvalidValueException("url", paramString);
    }
    this.url = paramString;
    readProperties(paramProperties);
    readSettingsFromURL();
    setUserName(removeProperty("USER", ""));
    convertPasswords();
    this.name = this.url.substring("jdbc:h2:".length());
    parseName();
    String str = removeProperty("RECOVER_TEST", null);
    if (str != null)
    {
      FilePathRec.register();
      try
      {
        Utils.callStaticMethod("org.h2.store.RecoverTester.init", new Object[] { str });
      }
      catch (Exception localException)
      {
        throw DbException.convert(localException);
      }
      this.name = ("rec:" + this.name);
    }
  }
  
  static
  {
    ArrayList localArrayList = SetTypes.getTypes();
    HashSet localHashSet = KNOWN_SETTINGS;
    localHashSet.addAll(localArrayList);
    String[] arrayOfString1 = { "ACCESS_MODE_DATA", "AUTOCOMMIT", "CIPHER", "CREATE", "CACHE_TYPE", "FILE_LOCK", "IGNORE_UNKNOWN_SETTINGS", "IFEXISTS", "INIT", "PASSWORD", "RECOVER", "RECOVER_TEST", "USER", "AUTO_SERVER", "AUTO_SERVER_PORT", "NO_UPGRADE", "AUTO_RECONNECT", "OPEN_NEW", "PAGE_SIZE", "PASSWORD_HASH", "JMX" };
    for (String str : arrayOfString1)
    {
      if ((SysProperties.CHECK) && (localHashSet.contains(str))) {
        DbException.throwInternalError(str);
      }
      localHashSet.add(str);
    }
  }
  
  private static boolean isKnownSetting(String paramString)
  {
    return KNOWN_SETTINGS.contains(paramString);
  }
  
  public ConnectionInfo clone()
    throws CloneNotSupportedException
  {
    ConnectionInfo localConnectionInfo = (ConnectionInfo)super.clone();
    localConnectionInfo.prop = ((Properties)this.prop.clone());
    localConnectionInfo.filePasswordHash = Utils.cloneByteArray(this.filePasswordHash);
    localConnectionInfo.fileEncryptionKey = Utils.cloneByteArray(this.fileEncryptionKey);
    localConnectionInfo.userPasswordHash = Utils.cloneByteArray(this.userPasswordHash);
    return localConnectionInfo;
  }
  
  private void parseName()
  {
    if (".".equals(this.name)) {
      this.name = "mem:";
    }
    if (this.name.startsWith("tcp:"))
    {
      this.remote = true;
      this.name = this.name.substring("tcp:".length());
    }
    else if (this.name.startsWith("ssl:"))
    {
      this.remote = true;
      this.ssl = true;
      this.name = this.name.substring("ssl:".length());
    }
    else if (this.name.startsWith("mem:"))
    {
      this.persistent = false;
      if ("mem:".equals(this.name)) {
        this.unnamed = true;
      }
    }
    else if (this.name.startsWith("file:"))
    {
      this.name = this.name.substring("file:".length());
      this.persistent = true;
    }
    else
    {
      this.persistent = true;
    }
    if ((this.persistent) && (!this.remote)) {
      if ("/".equals(SysProperties.FILE_SEPARATOR)) {
        this.name = this.name.replace('\\', '/');
      } else {
        this.name = this.name.replace('/', '\\');
      }
    }
  }
  
  public void setBaseDir(String paramString)
  {
    if (this.persistent)
    {
      String str1 = FileUtils.unwrap(FileUtils.toRealPath(paramString));
      boolean bool = FileUtils.isAbsolute(this.name);
      
      String str3 = null;
      if (paramString.endsWith(SysProperties.FILE_SEPARATOR)) {
        paramString = paramString.substring(0, paramString.length() - 1);
      }
      String str2;
      if (bool)
      {
        str2 = this.name;
      }
      else
      {
        str2 = FileUtils.unwrap(this.name);
        str3 = this.name.substring(0, this.name.length() - str2.length());
        str2 = paramString + SysProperties.FILE_SEPARATOR + str2;
      }
      String str4 = FileUtils.unwrap(FileUtils.toRealPath(str2));
      if ((str4.equals(str1)) || (!str4.startsWith(str1))) {
        throw DbException.get(90028, str4 + " outside " + str1);
      }
      if ((!str1.endsWith("/")) && (!str1.endsWith("\\"))) {
        if (str4.charAt(str1.length()) != '/') {
          throw DbException.get(90028, str4 + " outside " + str1);
        }
      }
      if (!bool) {
        this.name = (str3 + paramString + SysProperties.FILE_SEPARATOR + FileUtils.unwrap(this.name));
      }
    }
  }
  
  public boolean isRemote()
  {
    return this.remote;
  }
  
  public boolean isPersistent()
  {
    return this.persistent;
  }
  
  boolean isUnnamedInMemory()
  {
    return this.unnamed;
  }
  
  private void readProperties(Properties paramProperties)
  {
    Object[] arrayOfObject1 = new Object[paramProperties.size()];
    paramProperties.keySet().toArray(arrayOfObject1);
    DbSettings localDbSettings = null;
    for (Object localObject1 : arrayOfObject1)
    {
      String str = StringUtils.toUpperEnglish(localObject1.toString());
      if (this.prop.containsKey(str)) {
        throw DbException.get(90066, str);
      }
      Object localObject2 = paramProperties.get(localObject1);
      if (isKnownSetting(str))
      {
        this.prop.put(str, localObject2);
      }
      else
      {
        if (localDbSettings == null) {
          localDbSettings = getDbSettings();
        }
        if (localDbSettings.containsKey(str)) {
          this.prop.put(str, localObject2);
        }
      }
    }
  }
  
  private void readSettingsFromURL()
  {
    DbSettings localDbSettings = DbSettings.getInstance(null);
    int i = this.url.indexOf(';');
    if (i >= 0)
    {
      String str1 = this.url.substring(i + 1);
      this.url = this.url.substring(0, i);
      String[] arrayOfString1 = StringUtils.arraySplit(str1, ';', false);
      for (String str2 : arrayOfString1) {
        if (str2.length() != 0)
        {
          int m = str2.indexOf('=');
          if (m < 0) {
            throw getFormatException();
          }
          String str3 = str2.substring(m + 1);
          String str4 = str2.substring(0, m);
          str4 = StringUtils.toUpperEnglish(str4);
          if ((!isKnownSetting(str4)) && (!localDbSettings.containsKey(str4))) {
            throw DbException.get(90113, str4);
          }
          String str5 = this.prop.getProperty(str4);
          if ((str5 != null) && (!str5.equals(str3))) {
            throw DbException.get(90066, str4);
          }
          this.prop.setProperty(str4, str3);
        }
      }
    }
  }
  
  private char[] removePassword()
  {
    Object localObject = this.prop.remove("PASSWORD");
    if (localObject == null) {
      return new char[0];
    }
    if ((localObject instanceof char[])) {
      return (char[])localObject;
    }
    return localObject.toString().toCharArray();
  }
  
  private void convertPasswords()
  {
    Object localObject = removePassword();
    boolean bool = removeProperty("PASSWORD_HASH", false);
    if (getProperty("CIPHER", null) != null)
    {
      int i = -1;
      int j = 0;
      for (int k = localObject.length; j < k; j++) {
        if (localObject[j] == ' ')
        {
          i = j;
          break;
        }
      }
      if (i < 0) {
        throw DbException.get(90050);
      }
      char[] arrayOfChar1 = new char[localObject.length - i - 1];
      char[] arrayOfChar2 = new char[i];
      System.arraycopy(localObject, i + 1, arrayOfChar1, 0, arrayOfChar1.length);
      System.arraycopy(localObject, 0, arrayOfChar2, 0, i);
      Arrays.fill((char[])localObject, '\000');
      localObject = arrayOfChar1;
      this.fileEncryptionKey = FilePathEncrypt.getPasswordBytes(arrayOfChar2);
      this.filePasswordHash = hashPassword(bool, "file", arrayOfChar2);
    }
    this.userPasswordHash = hashPassword(bool, this.user, (char[])localObject);
  }
  
  private static byte[] hashPassword(boolean paramBoolean, String paramString, char[] paramArrayOfChar)
  {
    if (paramBoolean) {
      return StringUtils.convertHexToBytes(new String(paramArrayOfChar));
    }
    if ((paramString.length() == 0) && (paramArrayOfChar.length == 0)) {
      return new byte[0];
    }
    return SHA256.getKeyPasswordHash(paramString, paramArrayOfChar);
  }
  
  boolean getProperty(String paramString, boolean paramBoolean)
  {
    String str = getProperty(paramString, null);
    if (str == null) {
      return paramBoolean;
    }
    if ((str.length() == 1) && (Character.isDigit(str.charAt(0)))) {
      return Integer.parseInt(str) != 0;
    }
    return Boolean.parseBoolean(str);
  }
  
  public boolean removeProperty(String paramString, boolean paramBoolean)
  {
    String str = removeProperty(paramString, null);
    return str == null ? paramBoolean : Boolean.parseBoolean(str);
  }
  
  String removeProperty(String paramString1, String paramString2)
  {
    if ((SysProperties.CHECK) && (!isKnownSetting(paramString1))) {
      DbException.throwInternalError(paramString1);
    }
    Object localObject = this.prop.remove(paramString1);
    return localObject == null ? paramString2 : localObject.toString();
  }
  
  public String getName()
  {
    if (this.persistent)
    {
      if (this.nameNormalized == null)
      {
        if ((!SysProperties.IMPLICIT_RELATIVE_PATH) && 
          (!FileUtils.isAbsolute(this.name)) && 
          (this.name.indexOf("./") < 0) && (this.name.indexOf(".\\") < 0) && (this.name.indexOf(":/") < 0) && (this.name.indexOf(":\\") < 0)) {
          throw DbException.get(90011, this.originalURL);
        }
        String str1 = ".h2.db";
        String str2;
        if (FileUtils.exists(this.name + str1))
        {
          str2 = FileUtils.toRealPath(this.name + str1);
        }
        else
        {
          str1 = ".mv.db";
          str2 = FileUtils.toRealPath(this.name + str1);
        }
        String str3 = FileUtils.getName(str2);
        if (str3.length() < str1.length() + 1) {
          throw DbException.get(90138, this.name);
        }
        this.nameNormalized = str2.substring(0, str2.length() - str1.length());
      }
      return this.nameNormalized;
    }
    return this.name;
  }
  
  public byte[] getFilePasswordHash()
  {
    return this.filePasswordHash;
  }
  
  byte[] getFileEncryptionKey()
  {
    return this.fileEncryptionKey;
  }
  
  public String getUserName()
  {
    return this.user;
  }
  
  byte[] getUserPasswordHash()
  {
    return this.userPasswordHash;
  }
  
  String[] getKeys()
  {
    String[] arrayOfString = new String[this.prop.size()];
    this.prop.keySet().toArray(arrayOfString);
    return arrayOfString;
  }
  
  String getProperty(String paramString)
  {
    Object localObject = this.prop.get(paramString);
    if ((localObject == null) || (!(localObject instanceof String))) {
      return null;
    }
    return localObject.toString();
  }
  
  int getProperty(String paramString, int paramInt)
  {
    if ((SysProperties.CHECK) && (!isKnownSetting(paramString))) {
      DbException.throwInternalError(paramString);
    }
    String str = getProperty(paramString);
    return str == null ? paramInt : Integer.parseInt(str);
  }
  
  public String getProperty(String paramString1, String paramString2)
  {
    if ((SysProperties.CHECK) && (!isKnownSetting(paramString1))) {
      DbException.throwInternalError(paramString1);
    }
    String str = getProperty(paramString1);
    return str == null ? paramString2 : str;
  }
  
  String getProperty(int paramInt, String paramString)
  {
    String str1 = SetTypes.getTypeName(paramInt);
    String str2 = getProperty(str1);
    return str2 == null ? paramString : str2;
  }
  
  int getIntProperty(int paramInt1, int paramInt2)
  {
    String str1 = SetTypes.getTypeName(paramInt1);
    String str2 = getProperty(str1, null);
    try
    {
      return str2 == null ? paramInt2 : Integer.decode(str2).intValue();
    }
    catch (NumberFormatException localNumberFormatException) {}
    return paramInt2;
  }
  
  boolean isSSL()
  {
    return this.ssl;
  }
  
  public void setUserName(String paramString)
  {
    this.user = StringUtils.toUpperEnglish(paramString);
  }
  
  public void setUserPasswordHash(byte[] paramArrayOfByte)
  {
    this.userPasswordHash = paramArrayOfByte;
  }
  
  public void setFilePasswordHash(byte[] paramArrayOfByte)
  {
    this.filePasswordHash = paramArrayOfByte;
  }
  
  public void setFileEncryptionKey(byte[] paramArrayOfByte)
  {
    this.fileEncryptionKey = paramArrayOfByte;
  }
  
  public void setProperty(String paramString1, String paramString2)
  {
    if (paramString2 != null) {
      this.prop.setProperty(paramString1, paramString2);
    }
  }
  
  public String getURL()
  {
    return this.url;
  }
  
  public String getOriginalURL()
  {
    return this.originalURL;
  }
  
  public void setOriginalURL(String paramString)
  {
    this.originalURL = paramString;
  }
  
  DbException getFormatException()
  {
    String str = "jdbc:h2:{ {.|mem:}[name] | [file:]fileName | {tcp|ssl}:[//]server[:port][,server2[:port]]/name }[;key=value...]";
    return DbException.get(90046, new String[] { str, this.url });
  }
  
  public void setServerKey(String paramString)
  {
    this.remote = true;
    this.persistent = false;
    this.name = paramString;
  }
  
  public DbSettings getDbSettings()
  {
    DbSettings localDbSettings = DbSettings.getInstance(null);
    HashMap localHashMap = null;
    for (Object localObject : this.prop.keySet())
    {
      String str = localObject.toString();
      if ((!isKnownSetting(str)) && (localDbSettings.containsKey(str)))
      {
        if (localHashMap == null) {
          localHashMap = New.hashMap();
        }
        localHashMap.put(str, this.prop.getProperty(str));
      }
    }
    return DbSettings.getInstance(localHashMap);
  }
  
  private static String remapURL(String paramString)
  {
    String str1 = SysProperties.URL_MAP;
    if ((str1 != null) && (str1.length() > 0)) {
      try
      {
        SortedProperties localSortedProperties = SortedProperties.loadProperties(str1);
        String str2 = localSortedProperties.getProperty(paramString);
        if (str2 == null)
        {
          localSortedProperties.put(paramString, "");
          localSortedProperties.store(str1);
        }
        else
        {
          str2 = str2.trim();
          if (str2.length() > 0) {
            return str2;
          }
        }
      }
      catch (IOException localIOException)
      {
        throw DbException.convert(localIOException);
      }
    }
    return paramString;
  }
}
