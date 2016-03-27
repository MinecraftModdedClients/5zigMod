package org.h2.server.web;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.h2.util.New;

public class PageParser
{
  private static final int TAB_WIDTH = 4;
  private final String page;
  private int pos;
  private final Map<String, Object> settings;
  private final int len;
  private StringBuilder result;
  
  private PageParser(String paramString, Map<String, Object> paramMap, int paramInt)
  {
    this.page = paramString;
    this.pos = paramInt;
    this.len = paramString.length();
    this.settings = paramMap;
    this.result = new StringBuilder(this.len);
  }
  
  public static String parse(String paramString, Map<String, Object> paramMap)
  {
    PageParser localPageParser = new PageParser(paramString, paramMap, 0);
    return localPageParser.replaceTags();
  }
  
  private void setError(int paramInt)
  {
    String str = this.page.substring(0, paramInt) + "####BUG####" + this.page.substring(paramInt);
    str = escapeHtml(str);
    this.result = new StringBuilder();
    this.result.append(str);
  }
  
  private String parseBlockUntil(String paramString)
    throws ParseException
  {
    PageParser localPageParser = new PageParser(this.page, this.settings, this.pos);
    localPageParser.parseAll();
    if (!localPageParser.readIf(paramString)) {
      throw new ParseException(this.page, localPageParser.pos);
    }
    this.pos = localPageParser.pos;
    return localPageParser.result.toString();
  }
  
  private String replaceTags()
  {
    try
    {
      parseAll();
      if (this.pos != this.len) {
        setError(this.pos);
      }
    }
    catch (ParseException localParseException)
    {
      setError(this.pos);
    }
    return this.result.toString();
  }
  
  private void parseAll()
    throws ParseException
  {
    StringBuilder localStringBuilder = this.result;
    String str1 = this.page;
    for (int i = this.pos; i < this.len; i++)
    {
      char c = str1.charAt(i);
      String str5;
      switch (c)
      {
      case '<': 
        if ((str1.charAt(i + 3) == ':') && (str1.charAt(i + 1) == '/'))
        {
          this.pos = i;
          return;
        }
        if (str1.charAt(i + 2) == ':')
        {
          this.pos = i;
          String str2;
          int m;
          Object localObject1;
          Object localObject2;
          if (readIf("<c:forEach"))
          {
            str2 = readParam("var");
            String str3 = readParam("items");
            read(">");
            m = this.pos;
            localObject1 = (List)get(str3);
            if (localObject1 == null)
            {
              this.result.append("?items?");
              localObject1 = New.arrayList();
            }
            if (((List)localObject1).size() == 0) {
              parseBlockUntil("</c:forEach>");
            }
            for (localObject2 = ((List)localObject1).iterator(); ((Iterator)localObject2).hasNext();)
            {
              Object localObject3 = ((Iterator)localObject2).next();
              this.settings.put(str2, localObject3);
              this.pos = m;
              String str6 = parseBlockUntil("</c:forEach>");
              this.result.append(str6);
            }
          }
          else if (readIf("<c:if"))
          {
            str2 = readParam("test");
            int k = str2.indexOf("=='");
            if (k < 0)
            {
              setError(i);
              return;
            }
            str5 = str2.substring(k + 3, str2.length() - 1);
            str2 = str2.substring(0, k);
            localObject1 = (String)get(str2);
            read(">");
            localObject2 = parseBlockUntil("</c:if>");
            this.pos -= 1;
            if (((String)localObject1).equals(str5)) {
              this.result.append((String)localObject2);
            }
          }
          else
          {
            setError(i);
            return;
          }
          i = this.pos;
        }
        else
        {
          localStringBuilder.append(c);
        }
        break;
      case '$': 
        if ((str1.length() > i + 1) && (str1.charAt(i + 1) == '{'))
        {
          i += 2;
          int j = str1.indexOf('}', i);
          if (j < 0)
          {
            setError(i);
            return;
          }
          String str4 = str1.substring(i, j).trim();
          i = j;
          str5 = (String)get(str4);
          replaceTags(str5);
        }
        else
        {
          localStringBuilder.append(c);
        }
        break;
      default: 
        localStringBuilder.append(c);
      }
    }
    this.pos = i;
  }
  
  private Object get(String paramString)
  {
    int i = paramString.indexOf('.');
    if (i >= 0)
    {
      String str = paramString.substring(i + 1);
      paramString = paramString.substring(0, i);
      HashMap localHashMap = (HashMap)this.settings.get(paramString);
      if (localHashMap == null) {
        return "?" + paramString + "?";
      }
      return localHashMap.get(str);
    }
    return this.settings.get(paramString);
  }
  
  private void replaceTags(String paramString)
  {
    if (paramString != null) {
      this.result.append(parse(paramString, this.settings));
    }
  }
  
  private String readParam(String paramString)
    throws ParseException
  {
    read(paramString);
    read("=");
    read("\"");
    int i = this.pos;
    while (this.page.charAt(this.pos) != '"') {
      this.pos += 1;
    }
    int j = this.pos;
    read("\"");
    String str = this.page.substring(i, j);
    return parse(str, this.settings);
  }
  
  private void skipSpaces()
  {
    while (this.page.charAt(this.pos) == ' ') {
      this.pos += 1;
    }
  }
  
  private void read(String paramString)
    throws ParseException
  {
    if (!readIf(paramString)) {
      throw new ParseException(paramString, this.pos);
    }
  }
  
  private boolean readIf(String paramString)
  {
    skipSpaces();
    if (this.page.regionMatches(this.pos, paramString, 0, paramString.length()))
    {
      this.pos += paramString.length();
      skipSpaces();
      return true;
    }
    return false;
  }
  
  static String escapeHtmlData(String paramString)
  {
    return escapeHtml(paramString, false);
  }
  
  public static String escapeHtml(String paramString)
  {
    return escapeHtml(paramString, true);
  }
  
  private static String escapeHtml(String paramString, boolean paramBoolean)
  {
    if (paramString == null) {
      return null;
    }
    if ((paramBoolean) && 
      (paramString.length() == 0)) {
      return "&nbsp;";
    }
    StringBuilder localStringBuilder = new StringBuilder(paramString.length());
    int i = 1;
    for (int j = 0; j < paramString.length(); j++)
    {
      char c = paramString.charAt(j);
      if ((c == ' ') || (c == '\t'))
      {
        for (int k = 0; k < (c == ' ' ? 1 : 4); k++) {
          if ((i != 0) && (paramBoolean))
          {
            localStringBuilder.append("&nbsp;");
          }
          else
          {
            localStringBuilder.append(' ');
            i = 1;
          }
        }
      }
      else
      {
        i = 0;
        switch (c)
        {
        case '$': 
          localStringBuilder.append("&#36;");
          break;
        case '<': 
          localStringBuilder.append("&lt;");
          break;
        case '>': 
          localStringBuilder.append("&gt;");
          break;
        case '&': 
          localStringBuilder.append("&amp;");
          break;
        case '"': 
          localStringBuilder.append("&quot;");
          break;
        case '\'': 
          localStringBuilder.append("&#39;");
          break;
        case '\n': 
          if (paramBoolean)
          {
            localStringBuilder.append("<br />");
            i = 1;
          }
          else
          {
            localStringBuilder.append(c);
          }
          break;
        default: 
          if (c >= '') {
            localStringBuilder.append("&#").append(c).append(';');
          } else {
            localStringBuilder.append(c);
          }
          break;
        }
      }
    }
    return localStringBuilder.toString();
  }
  
  static String escapeJavaScript(String paramString)
  {
    if (paramString == null) {
      return null;
    }
    if (paramString.length() == 0) {
      return "";
    }
    StringBuilder localStringBuilder = new StringBuilder(paramString.length());
    for (int i = 0; i < paramString.length(); i++)
    {
      char c = paramString.charAt(i);
      switch (c)
      {
      case '"': 
        localStringBuilder.append("\\\"");
        break;
      case '\'': 
        localStringBuilder.append("\\'");
        break;
      case '\\': 
        localStringBuilder.append("\\\\");
        break;
      case '\n': 
        localStringBuilder.append("\\n");
        break;
      case '\r': 
        localStringBuilder.append("\\r");
        break;
      case '\t': 
        localStringBuilder.append("\\t");
        break;
      default: 
        localStringBuilder.append(c);
      }
    }
    return localStringBuilder.toString();
  }
}
