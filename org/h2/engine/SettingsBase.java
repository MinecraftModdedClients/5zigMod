package org.h2.engine;

import java.util.HashMap;
import org.h2.message.DbException;
import org.h2.util.Utils;

public class SettingsBase
{
  private final HashMap<String, String> settings;
  
  protected SettingsBase(HashMap<String, String> paramHashMap)
  {
    this.settings = paramHashMap;
  }
  
  protected boolean get(String paramString, boolean paramBoolean)
  {
    String str = get(paramString, "" + paramBoolean);
    try
    {
      return Boolean.parseBoolean(str);
    }
    catch (NumberFormatException localNumberFormatException)
    {
      throw DbException.get(22018, localNumberFormatException, new String[] { "key:" + paramString + " value:" + str });
    }
  }
  
  protected int get(String paramString, int paramInt)
  {
    String str = get(paramString, "" + paramInt);
    try
    {
      return Integer.decode(str).intValue();
    }
    catch (NumberFormatException localNumberFormatException)
    {
      throw DbException.get(22018, localNumberFormatException, new String[] { "key:" + paramString + " value:" + str });
    }
  }
  
  protected String get(String paramString1, String paramString2)
  {
    StringBuilder localStringBuilder = new StringBuilder("h2.");
    int i = 0;
    for (char c : paramString1.toCharArray()) {
      if (c == '_')
      {
        i = 1;
      }
      else
      {
        localStringBuilder.append(i != 0 ? Character.toUpperCase(c) : Character.toLowerCase(c));
        i = 0;
      }
    }
    ??? = localStringBuilder.toString();
    String str = (String)this.settings.get(paramString1);
    if (str == null)
    {
      str = Utils.getProperty((String)???, paramString2);
      this.settings.put(paramString1, str);
    }
    return str;
  }
  
  protected boolean containsKey(String paramString)
  {
    return this.settings.containsKey(paramString);
  }
  
  public HashMap<String, String> getSettings()
  {
    return this.settings;
  }
}
