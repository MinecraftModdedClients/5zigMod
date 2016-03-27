package org.h2.util;

import java.lang.ref.SoftReference;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Locale;
import org.h2.engine.Constants;
import org.h2.engine.SysProperties;
import org.h2.message.DbException;

public class StringUtils
{
  private static SoftReference<String[]> softCache = new SoftReference(null);
  private static long softCacheCreated;
  private static final char[] HEX = "0123456789abcdef".toCharArray();
  private static final int[] HEX_DECODE = new int[103];
  
  static
  {
    for (int i = 0; i < HEX_DECODE.length; i++) {
      HEX_DECODE[i] = -1;
    }
    for (i = 0; i <= 9; i++) {
      HEX_DECODE[(i + 48)] = i;
    }
    for (i = 0; i <= 5; i++) {
      HEX_DECODE[(i + 97)] = (HEX_DECODE[(i + 65)] = i + 10);
    }
  }
  
  private static String[] getCache()
  {
    String[] arrayOfString1;
    if (softCache != null)
    {
      arrayOfString1 = (String[])softCache.get();
      if (arrayOfString1 != null) {
        return arrayOfString1;
      }
    }
    long l = System.currentTimeMillis();
    if ((softCacheCreated != 0L) && (l - softCacheCreated < 5000L)) {
      return null;
    }
    try
    {
      arrayOfString1 = new String[SysProperties.OBJECT_CACHE_SIZE];
      softCache = new SoftReference(arrayOfString1);
      return arrayOfString1;
    }
    finally
    {
      softCacheCreated = System.currentTimeMillis();
    }
  }
  
  public static boolean equals(String paramString1, String paramString2)
  {
    if (paramString1 == null) {
      return paramString2 == null;
    }
    return paramString1.equals(paramString2);
  }
  
  public static String toUpperEnglish(String paramString)
  {
    return paramString.toUpperCase(Locale.ENGLISH);
  }
  
  public static String toLowerEnglish(String paramString)
  {
    return paramString.toLowerCase(Locale.ENGLISH);
  }
  
  public static boolean startsWithIgnoreCase(String paramString1, String paramString2)
  {
    if (paramString1.length() < paramString2.length()) {
      return false;
    }
    return paramString1.substring(0, paramString2.length()).equalsIgnoreCase(paramString2);
  }
  
  public static String quoteStringSQL(String paramString)
  {
    if (paramString == null) {
      return "NULL";
    }
    int i = paramString.length();
    StringBuilder localStringBuilder = new StringBuilder(i + 2);
    localStringBuilder.append('\'');
    for (int j = 0; j < i; j++)
    {
      char c = paramString.charAt(j);
      if (c == '\'') {
        localStringBuilder.append(c);
      } else if ((c < ' ') || (c > '')) {
        return "STRINGDECODE(" + quoteStringSQL(javaEncode(paramString)) + ")";
      }
      localStringBuilder.append(c);
    }
    localStringBuilder.append('\'');
    return localStringBuilder.toString();
  }
  
  public static String javaEncode(String paramString)
  {
    int i = paramString.length();
    StringBuilder localStringBuilder = new StringBuilder(i);
    for (int j = 0; j < i; j++)
    {
      int k = paramString.charAt(j);
      switch (k)
      {
      case 9: 
        localStringBuilder.append("\\t");
        break;
      case 10: 
        localStringBuilder.append("\\n");
        break;
      case 12: 
        localStringBuilder.append("\\f");
        break;
      case 13: 
        localStringBuilder.append("\\r");
        break;
      case 34: 
        localStringBuilder.append("\\\"");
        break;
      case 92: 
        localStringBuilder.append("\\\\");
        break;
      default: 
        int m = k & 0xFFFF;
        if ((m >= 32) && (m < 128))
        {
          localStringBuilder.append(k);
        }
        else
        {
          localStringBuilder.append("\\u");
          String str = Integer.toHexString(m);
          for (int n = str.length(); n < 4; n++) {
            localStringBuilder.append('0');
          }
          localStringBuilder.append(str);
        }
        break;
      }
    }
    return localStringBuilder.toString();
  }
  
  public static String addAsterisk(String paramString, int paramInt)
  {
    if (paramString != null)
    {
      paramInt = Math.min(paramInt, paramString.length());
      paramString = paramString.substring(0, paramInt) + "[*]" + paramString.substring(paramInt);
    }
    return paramString;
  }
  
  private static DbException getFormatException(String paramString, int paramInt)
  {
    return DbException.get(90095, addAsterisk(paramString, paramInt));
  }
  
  public static String javaDecode(String paramString)
  {
    int i = paramString.length();
    StringBuilder localStringBuilder = new StringBuilder(i);
    for (int j = 0; j < i; j++)
    {
      char c = paramString.charAt(j);
      if (c == '\\')
      {
        if (j + 1 >= paramString.length()) {
          throw getFormatException(paramString, j);
        }
        c = paramString.charAt(++j);
        switch (c)
        {
        case 't': 
          localStringBuilder.append('\t');
          break;
        case 'r': 
          localStringBuilder.append('\r');
          break;
        case 'n': 
          localStringBuilder.append('\n');
          break;
        case 'b': 
          localStringBuilder.append('\b');
          break;
        case 'f': 
          localStringBuilder.append('\f');
          break;
        case '#': 
          localStringBuilder.append('#');
          break;
        case '=': 
          localStringBuilder.append('=');
          break;
        case ':': 
          localStringBuilder.append(':');
          break;
        case '"': 
          localStringBuilder.append('"');
          break;
        case '\\': 
          localStringBuilder.append('\\');
          break;
        case 'u': 
          try
          {
            c = (char)Integer.parseInt(paramString.substring(j + 1, j + 5), 16);
          }
          catch (NumberFormatException localNumberFormatException1)
          {
            throw getFormatException(paramString, j);
          }
          j += 4;
          localStringBuilder.append(c);
          break;
        default: 
          if ((c >= '0') && (c <= '9'))
          {
            try
            {
              c = (char)Integer.parseInt(paramString.substring(j, j + 3), 8);
            }
            catch (NumberFormatException localNumberFormatException2)
            {
              throw getFormatException(paramString, j);
            }
            j += 2;
            localStringBuilder.append(c); continue;
          }
          throw getFormatException(paramString, j);
        }
      }
      else
      {
        localStringBuilder.append(c);
      }
    }
    return localStringBuilder.toString();
  }
  
  public static String quoteJavaString(String paramString)
  {
    if (paramString == null) {
      return "null";
    }
    return "\"" + javaEncode(paramString) + "\"";
  }
  
  public static String quoteJavaStringArray(String[] paramArrayOfString)
  {
    if (paramArrayOfString == null) {
      return "null";
    }
    StatementBuilder localStatementBuilder = new StatementBuilder("new String[]{");
    for (String str : paramArrayOfString)
    {
      localStatementBuilder.appendExceptFirst(", ");
      localStatementBuilder.append(quoteJavaString(str));
    }
    return localStatementBuilder.append('}').toString();
  }
  
  public static String quoteJavaIntArray(int[] paramArrayOfInt)
  {
    if (paramArrayOfInt == null) {
      return "null";
    }
    StatementBuilder localStatementBuilder = new StatementBuilder("new int[]{");
    for (int k : paramArrayOfInt)
    {
      localStatementBuilder.appendExceptFirst(", ");
      localStatementBuilder.append(k);
    }
    return localStatementBuilder.append('}').toString();
  }
  
  public static String enclose(String paramString)
  {
    if (paramString.startsWith("(")) {
      return paramString;
    }
    return "(" + paramString + ")";
  }
  
  public static String unEnclose(String paramString)
  {
    if ((paramString.startsWith("(")) && (paramString.endsWith(")"))) {
      return paramString.substring(1, paramString.length() - 1);
    }
    return paramString;
  }
  
  public static String urlEncode(String paramString)
  {
    try
    {
      return URLEncoder.encode(paramString, "UTF-8");
    }
    catch (Exception localException)
    {
      throw DbException.convert(localException);
    }
  }
  
  public static String urlDecode(String paramString)
  {
    int i = paramString.length();
    byte[] arrayOfByte = new byte[i];
    int j = 0;
    for (int k = 0; k < i; k++)
    {
      int m = paramString.charAt(k);
      if (m == 43)
      {
        arrayOfByte[(j++)] = 32;
      }
      else if (m == 37)
      {
        arrayOfByte[(j++)] = ((byte)Integer.parseInt(paramString.substring(k + 1, k + 3), 16));
        k += 2;
      }
      else
      {
        if ((SysProperties.CHECK) && (
          (m > 127) || (m < 32))) {
          throw new IllegalArgumentException("Unexpected char " + m + " decoding " + paramString);
        }
        arrayOfByte[(j++)] = ((byte)m);
      }
    }
    String str = new String(arrayOfByte, 0, j, Constants.UTF8);
    return str;
  }
  
  public static String[] arraySplit(String paramString, char paramChar, boolean paramBoolean)
  {
    if (paramString == null) {
      return null;
    }
    int i = paramString.length();
    if (i == 0) {
      return new String[0];
    }
    ArrayList localArrayList = New.arrayList();
    StringBuilder localStringBuilder = new StringBuilder(i);
    for (int j = 0; j < i; j++)
    {
      char c = paramString.charAt(j);
      if (c == paramChar)
      {
        String str2 = localStringBuilder.toString();
        localArrayList.add(paramBoolean ? str2.trim() : str2);
        localStringBuilder.setLength(0);
      }
      else if ((c == '\\') && (j < i - 1))
      {
        localStringBuilder.append(paramString.charAt(++j));
      }
      else
      {
        localStringBuilder.append(c);
      }
    }
    String str1 = localStringBuilder.toString();
    localArrayList.add(paramBoolean ? str1.trim() : str1);
    String[] arrayOfString = new String[localArrayList.size()];
    localArrayList.toArray(arrayOfString);
    return arrayOfString;
  }
  
  public static String arrayCombine(String[] paramArrayOfString, char paramChar)
  {
    StatementBuilder localStatementBuilder = new StatementBuilder();
    for (String str : paramArrayOfString)
    {
      localStatementBuilder.appendExceptFirst(String.valueOf(paramChar));
      if (str == null) {
        str = "";
      }
      int k = 0;
      for (int m = str.length(); k < m; k++)
      {
        char c = str.charAt(k);
        if ((c == '\\') || (c == paramChar)) {
          localStatementBuilder.append('\\');
        }
        localStatementBuilder.append(c);
      }
    }
    return localStatementBuilder.toString();
  }
  
  public static String xmlAttr(String paramString1, String paramString2)
  {
    return " " + paramString1 + "=\"" + xmlText(paramString2) + "\"";
  }
  
  public static String xmlNode(String paramString1, String paramString2, String paramString3)
  {
    return xmlNode(paramString1, paramString2, paramString3, true);
  }
  
  public static String xmlNode(String paramString1, String paramString2, String paramString3, boolean paramBoolean)
  {
    String str = paramString1 + paramString2;
    if (paramString3 == null) {
      return "<" + str + "/>\n";
    }
    if ((paramBoolean) && (paramString3.indexOf('\n') >= 0)) {
      paramString3 = "\n" + indent(paramString3);
    }
    return "<" + str + ">" + paramString3 + "</" + paramString1 + ">\n";
  }
  
  public static String indent(String paramString)
  {
    return indent(paramString, 4, true);
  }
  
  public static String indent(String paramString, int paramInt, boolean paramBoolean)
  {
    StringBuilder localStringBuilder = new StringBuilder(paramString.length() + paramInt);
    for (int i = 0; i < paramString.length();)
    {
      for (int j = 0; j < paramInt; j++) {
        localStringBuilder.append(' ');
      }
      j = paramString.indexOf('\n', i);
      j = j < 0 ? paramString.length() : j + 1;
      localStringBuilder.append(paramString.substring(i, j));
      i = j;
    }
    if ((paramBoolean) && (!paramString.endsWith("\n"))) {
      localStringBuilder.append('\n');
    }
    return localStringBuilder.toString();
  }
  
  public static String xmlComment(String paramString)
  {
    int i = 0;
    for (;;)
    {
      i = paramString.indexOf("--", i);
      if (i < 0) {
        break;
      }
      paramString = paramString.substring(0, i + 1) + " " + paramString.substring(i + 1);
    }
    if (paramString.indexOf('\n') >= 0) {
      return "<!--\n" + indent(paramString) + "-->\n";
    }
    return "<!-- " + paramString + " -->\n";
  }
  
  public static String xmlCData(String paramString)
  {
    if (paramString.contains("]]>")) {
      return xmlText(paramString);
    }
    boolean bool = paramString.endsWith("\n");
    paramString = "<![CDATA[" + paramString + "]]>";
    return bool ? paramString + "\n" : paramString;
  }
  
  public static String xmlStartDoc()
  {
    return "<?xml version=\"1.0\"?>\n";
  }
  
  public static String xmlText(String paramString)
  {
    return xmlText(paramString, false);
  }
  
  public static String xmlText(String paramString, boolean paramBoolean)
  {
    int i = paramString.length();
    StringBuilder localStringBuilder = new StringBuilder(i);
    for (int j = 0; j < i; j++)
    {
      char c = paramString.charAt(j);
      switch (c)
      {
      case '<': 
        localStringBuilder.append("&lt;");
        break;
      case '>': 
        localStringBuilder.append("&gt;");
        break;
      case '&': 
        localStringBuilder.append("&amp;");
        break;
      case '\'': 
        localStringBuilder.append("&apos;");
        break;
      case '"': 
        localStringBuilder.append("&quot;");
        break;
      case '\n': 
      case '\r': 
        if (paramBoolean) {
          localStringBuilder.append("&#x").append(Integer.toHexString(c)).append(';');
        } else {
          localStringBuilder.append(c);
        }
        break;
      case '\t': 
        localStringBuilder.append(c);
        break;
      default: 
        if ((c < ' ') || (c > '')) {
          localStringBuilder.append("&#x").append(Integer.toHexString(c)).append(';');
        } else {
          localStringBuilder.append(c);
        }
        break;
      }
    }
    return localStringBuilder.toString();
  }
  
  public static String replaceAll(String paramString1, String paramString2, String paramString3)
  {
    int i = paramString1.indexOf(paramString2);
    if (i < 0) {
      return paramString1;
    }
    StringBuilder localStringBuilder = new StringBuilder(paramString1.length() - paramString2.length() + paramString3.length());
    
    int j = 0;
    do
    {
      localStringBuilder.append(paramString1.substring(j, i)).append(paramString3);
      j = i + paramString2.length();
      i = paramString1.indexOf(paramString2, j);
    } while (i >= 0);
    localStringBuilder.append(paramString1.substring(j));
    
    return localStringBuilder.toString();
  }
  
  public static String quoteIdentifier(String paramString)
  {
    int i = paramString.length();
    StringBuilder localStringBuilder = new StringBuilder(i + 2);
    localStringBuilder.append('"');
    for (int j = 0; j < i; j++)
    {
      char c = paramString.charAt(j);
      if (c == '"') {
        localStringBuilder.append(c);
      }
      localStringBuilder.append(c);
    }
    return '"';
  }
  
  public static boolean isNullOrEmpty(String paramString)
  {
    return (paramString == null) || (paramString.length() == 0);
  }
  
  public static String quoteRemarkSQL(String paramString)
  {
    paramString = replaceAll(paramString, "*/", "++/");
    return replaceAll(paramString, "/*", "/++");
  }
  
  public static String pad(String paramString1, int paramInt, String paramString2, boolean paramBoolean)
  {
    if (paramInt < 0) {
      paramInt = 0;
    }
    if (paramInt < paramString1.length()) {
      return paramString1.substring(0, paramInt);
    }
    if (paramInt == paramString1.length()) {
      return paramString1;
    }
    char c;
    if ((paramString2 == null) || (paramString2.length() == 0)) {
      c = ' ';
    } else {
      c = paramString2.charAt(0);
    }
    StringBuilder localStringBuilder = new StringBuilder(paramInt);
    paramInt -= paramString1.length();
    if (paramBoolean) {
      localStringBuilder.append(paramString1);
    }
    for (int i = 0; i < paramInt; i++) {
      localStringBuilder.append(c);
    }
    if (!paramBoolean) {
      localStringBuilder.append(paramString1);
    }
    return localStringBuilder.toString();
  }
  
  public static char[] cloneCharArray(char[] paramArrayOfChar)
  {
    if (paramArrayOfChar == null) {
      return null;
    }
    int i = paramArrayOfChar.length;
    if (i == 0) {
      return paramArrayOfChar;
    }
    char[] arrayOfChar = new char[i];
    System.arraycopy(paramArrayOfChar, 0, arrayOfChar, 0, i);
    return arrayOfChar;
  }
  
  public static String trim(String paramString1, boolean paramBoolean1, boolean paramBoolean2, String paramString2)
  {
    int i = (paramString2 == null) || (paramString2.length() < 1) ? ' ' : paramString2.charAt(0);
    int j;
    int k;
    if (paramBoolean1)
    {
      j = paramString1.length();k = 0;
      while ((k < j) && (paramString1.charAt(k) == i)) {
        k++;
      }
      paramString1 = k == 0 ? paramString1 : paramString1.substring(k);
    }
    if (paramBoolean2)
    {
      j = paramString1.length() - 1;
      k = j;
      while ((k >= 0) && (paramString1.charAt(k) == i)) {
        k--;
      }
      paramString1 = k == j ? paramString1 : paramString1.substring(0, k + 1);
    }
    return paramString1;
  }
  
  public static String cache(String paramString)
  {
    if (!SysProperties.OBJECT_CACHE) {
      return paramString;
    }
    if (paramString == null) {
      return paramString;
    }
    if (paramString.length() == 0) {
      return "";
    }
    int i = paramString.hashCode();
    String[] arrayOfString = getCache();
    if (arrayOfString != null)
    {
      int j = i & SysProperties.OBJECT_CACHE_SIZE - 1;
      String str = arrayOfString[j];
      if ((str != null) && 
        (paramString.equals(str))) {
        return str;
      }
      arrayOfString[j] = paramString;
    }
    return paramString;
  }
  
  public static String fromCacheOrNew(String paramString)
  {
    if (!SysProperties.OBJECT_CACHE) {
      return paramString;
    }
    if (paramString == null) {
      return paramString;
    }
    if (paramString.length() == 0) {
      return "";
    }
    int i = paramString.hashCode();
    String[] arrayOfString = getCache();
    int j = i & SysProperties.OBJECT_CACHE_SIZE - 1;
    if (arrayOfString == null) {
      return paramString;
    }
    String str = arrayOfString[j];
    if ((str != null) && 
      (paramString.equals(str))) {
      return str;
    }
    paramString = new String(paramString);
    arrayOfString[j] = paramString;
    return paramString;
  }
  
  public static void clearCache()
  {
    softCache = new SoftReference(null);
  }
  
  public static byte[] convertHexToBytes(String paramString)
  {
    int i = paramString.length();
    if (i % 2 != 0) {
      throw DbException.get(90003, paramString);
    }
    i /= 2;
    byte[] arrayOfByte = new byte[i];
    int j = 0;
    int[] arrayOfInt = HEX_DECODE;
    try
    {
      for (int k = 0; k < i; k++)
      {
        int m = arrayOfInt[paramString.charAt(k + k)] << 4 | arrayOfInt[paramString.charAt(k + k + 1)];
        j |= m;
        arrayOfByte[k] = ((byte)m);
      }
    }
    catch (ArrayIndexOutOfBoundsException localArrayIndexOutOfBoundsException)
    {
      throw DbException.get(90004, paramString);
    }
    if ((j & 0xFF00) != 0) {
      throw DbException.get(90004, paramString);
    }
    return arrayOfByte;
  }
  
  public static String convertBytesToHex(byte[] paramArrayOfByte)
  {
    return convertBytesToHex(paramArrayOfByte, paramArrayOfByte.length);
  }
  
  public static String convertBytesToHex(byte[] paramArrayOfByte, int paramInt)
  {
    char[] arrayOfChar1 = new char[paramInt + paramInt];
    char[] arrayOfChar2 = HEX;
    for (int i = 0; i < paramInt; i++)
    {
      int j = paramArrayOfByte[i] & 0xFF;
      arrayOfChar1[(i + i)] = arrayOfChar2[(j >> 4)];
      arrayOfChar1[(i + i + 1)] = arrayOfChar2[(j & 0xF)];
    }
    return new String(arrayOfChar1);
  }
  
  public static boolean isNumber(String paramString)
  {
    if (paramString.length() == 0) {
      return false;
    }
    for (char c : paramString.toCharArray()) {
      if (!Character.isDigit(c)) {
        return false;
      }
    }
    return true;
  }
  
  public static void appendZeroPadded(StringBuilder paramStringBuilder, int paramInt, long paramLong)
  {
    if (paramInt == 2)
    {
      if (paramLong < 10L) {
        paramStringBuilder.append('0');
      }
      paramStringBuilder.append(paramLong);
    }
    else
    {
      String str = Long.toString(paramLong);
      paramInt -= str.length();
      while (paramInt > 0)
      {
        paramStringBuilder.append('0');
        paramInt--;
      }
      paramStringBuilder.append(str);
    }
  }
  
  public static String escapeMetaDataPattern(String paramString)
  {
    if ((paramString == null) || (paramString.length() == 0)) {
      return paramString;
    }
    return replaceAll(paramString, "\\", "\\\\");
  }
}
