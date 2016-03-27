package org.h2.tools;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import org.h2.engine.SysProperties;
import org.h2.message.DbException;
import org.h2.store.fs.FileUtils;
import org.h2.util.IOUtils;
import org.h2.util.JdbcUtils;
import org.h2.util.New;
import org.h2.util.StringUtils;

public class Csv
  implements SimpleRowSource
{
  private String[] columnNames;
  private String characterSet = SysProperties.FILE_ENCODING;
  private char escapeCharacter = '"';
  private char fieldDelimiter = '"';
  private char fieldSeparatorRead = ',';
  private String fieldSeparatorWrite = ",";
  private boolean caseSensitiveColumnNames;
  private boolean preserveWhitespace;
  private boolean writeColumnHeader = true;
  private char lineComment;
  private String lineSeparator = SysProperties.LINE_SEPARATOR;
  private String nullString = "";
  private String fileName;
  private Reader input;
  private char[] inputBuffer;
  private int inputBufferPos;
  private int inputBufferStart = -1;
  private int inputBufferEnd;
  private Writer output;
  private boolean endOfLine;
  private boolean endOfFile;
  
  private int writeResultSet(ResultSet paramResultSet)
    throws SQLException
  {
    try
    {
      int i = 0;
      ResultSetMetaData localResultSetMetaData = paramResultSet.getMetaData();
      int j = localResultSetMetaData.getColumnCount();
      String[] arrayOfString = new String[j];
      int[] arrayOfInt = new int[j];
      for (int k = 0; k < j; k++)
      {
        arrayOfString[k] = localResultSetMetaData.getColumnLabel(k + 1);
        arrayOfInt[k] = localResultSetMetaData.getColumnType(k + 1);
      }
      if (this.writeColumnHeader) {
        writeRow(arrayOfString);
      }
      while (paramResultSet.next())
      {
        for (k = 0; k < j; k++)
        {
          Object localObject1;
          switch (arrayOfInt[k])
          {
          case 91: 
            localObject1 = paramResultSet.getDate(k + 1);
            break;
          case 92: 
            localObject1 = paramResultSet.getTime(k + 1);
            break;
          case 93: 
            localObject1 = paramResultSet.getTimestamp(k + 1);
            break;
          default: 
            localObject1 = paramResultSet.getString(k + 1);
          }
          arrayOfString[k] = (localObject1 == null ? null : localObject1.toString());
        }
        writeRow(arrayOfString);
        i++;
      }
      this.output.close();
      return i;
    }
    catch (IOException localIOException)
    {
      throw DbException.convertIOException(localIOException, null);
    }
    finally
    {
      close();
      JdbcUtils.closeSilently(paramResultSet);
    }
  }
  
  public int write(Writer paramWriter, ResultSet paramResultSet)
    throws SQLException
  {
    this.output = paramWriter;
    return writeResultSet(paramResultSet);
  }
  
  public int write(String paramString1, ResultSet paramResultSet, String paramString2)
    throws SQLException
  {
    init(paramString1, paramString2);
    try
    {
      initWrite();
      return writeResultSet(paramResultSet);
    }
    catch (IOException localIOException)
    {
      throw convertException("IOException writing " + paramString1, localIOException);
    }
  }
  
  public int write(Connection paramConnection, String paramString1, String paramString2, String paramString3)
    throws SQLException
  {
    Statement localStatement = paramConnection.createStatement();
    ResultSet localResultSet = localStatement.executeQuery(paramString2);
    int i = write(paramString1, localResultSet, paramString3);
    localStatement.close();
    return i;
  }
  
  public ResultSet read(String paramString1, String[] paramArrayOfString, String paramString2)
    throws SQLException
  {
    init(paramString1, paramString2);
    try
    {
      return readResultSet(paramArrayOfString);
    }
    catch (IOException localIOException)
    {
      throw convertException("IOException reading " + paramString1, localIOException);
    }
  }
  
  public ResultSet read(Reader paramReader, String[] paramArrayOfString)
    throws IOException
  {
    init(null, null);
    this.input = paramReader;
    return readResultSet(paramArrayOfString);
  }
  
  private ResultSet readResultSet(String[] paramArrayOfString)
    throws IOException
  {
    this.columnNames = paramArrayOfString;
    initRead();
    SimpleResultSet localSimpleResultSet = new SimpleResultSet(this);
    makeColumnNamesUnique();
    for (String str : this.columnNames) {
      localSimpleResultSet.addColumn(str, 12, Integer.MAX_VALUE, 0);
    }
    return localSimpleResultSet;
  }
  
  private void makeColumnNamesUnique()
  {
    for (int i = 0; i < this.columnNames.length; i++)
    {
      StringBuilder localStringBuilder = new StringBuilder();
      String str1 = this.columnNames[i];
      if ((str1 == null) || (str1.length() == 0)) {
        localStringBuilder.append('C').append(i + 1);
      } else {
        localStringBuilder.append(str1);
      }
      for (int j = 0; j < i; j++)
      {
        String str2 = this.columnNames[j];
        if (localStringBuilder.toString().equals(str2))
        {
          localStringBuilder.append('1');
          j = -1;
        }
      }
      this.columnNames[i] = localStringBuilder.toString();
    }
  }
  
  private void init(String paramString1, String paramString2)
  {
    this.fileName = paramString1;
    if (paramString2 != null) {
      this.characterSet = paramString2;
    }
  }
  
  private void initWrite()
    throws IOException
  {
    if (this.output == null) {
      try
      {
        Object localObject = FileUtils.newOutputStream(this.fileName, false);
        localObject = new BufferedOutputStream((OutputStream)localObject, 4096);
        this.output = new BufferedWriter(new OutputStreamWriter((OutputStream)localObject, this.characterSet));
      }
      catch (Exception localException)
      {
        close();
        throw DbException.convertToIOException(localException);
      }
    }
  }
  
  private void writeRow(String[] paramArrayOfString)
    throws IOException
  {
    for (int i = 0; i < paramArrayOfString.length; i++)
    {
      if ((i > 0) && 
        (this.fieldSeparatorWrite != null)) {
        this.output.write(this.fieldSeparatorWrite);
      }
      String str = paramArrayOfString[i];
      if (str != null)
      {
        if (this.escapeCharacter != 0)
        {
          if (this.fieldDelimiter != 0) {
            this.output.write(this.fieldDelimiter);
          }
          this.output.write(escape(str));
          if (this.fieldDelimiter != 0) {
            this.output.write(this.fieldDelimiter);
          }
        }
        else
        {
          this.output.write(str);
        }
      }
      else if ((this.nullString != null) && (this.nullString.length() > 0)) {
        this.output.write(this.nullString);
      }
    }
    this.output.write(this.lineSeparator);
  }
  
  private String escape(String paramString)
  {
    if ((paramString.indexOf(this.fieldDelimiter) < 0) && (
      (this.escapeCharacter == this.fieldDelimiter) || (paramString.indexOf(this.escapeCharacter) < 0))) {
      return paramString;
    }
    int i = paramString.length();
    StringBuilder localStringBuilder = new StringBuilder(i);
    for (int j = 0; j < i; j++)
    {
      char c = paramString.charAt(j);
      if ((c == this.fieldDelimiter) || (c == this.escapeCharacter)) {
        localStringBuilder.append(this.escapeCharacter);
      }
      localStringBuilder.append(c);
    }
    return localStringBuilder.toString();
  }
  
  private void initRead()
    throws IOException
  {
    if (this.input == null) {
      try
      {
        Object localObject = FileUtils.newInputStream(this.fileName);
        localObject = new BufferedInputStream((InputStream)localObject, 4096);
        this.input = new InputStreamReader((InputStream)localObject, this.characterSet);
      }
      catch (IOException localIOException)
      {
        close();
        throw localIOException;
      }
    }
    if (!this.input.markSupported()) {
      this.input = new BufferedReader(this.input);
    }
    this.input.mark(1);
    int i = this.input.read();
    if (i != 65279) {
      this.input.reset();
    }
    this.inputBuffer = new char[' '];
    if (this.columnNames == null) {
      readHeader();
    }
  }
  
  private void readHeader()
    throws IOException
  {
    ArrayList localArrayList = New.arrayList();
    for (;;)
    {
      String str = readValue();
      if (str == null)
      {
        if (this.endOfLine)
        {
          if (this.endOfFile) {
            break;
          }
          if (localArrayList.size() > 0) {
            break;
          }
        }
        else
        {
          str = "COLUMN" + localArrayList.size();
          localArrayList.add(str);
        }
      }
      else
      {
        if (str.length() == 0) {
          str = "COLUMN" + localArrayList.size();
        } else if ((!this.caseSensitiveColumnNames) && (isSimpleColumnName(str))) {
          str = str.toUpperCase();
        }
        localArrayList.add(str);
        if (this.endOfLine) {
          break;
        }
      }
    }
    this.columnNames = new String[localArrayList.size()];
    localArrayList.toArray(this.columnNames);
  }
  
  private static boolean isSimpleColumnName(String paramString)
  {
    int i = 0;
    for (int j = paramString.length(); i < j; i++)
    {
      char c = paramString.charAt(i);
      if (i == 0)
      {
        if ((c != '_') && (!Character.isLetter(c))) {
          return false;
        }
      }
      else if ((c != '_') && (!Character.isLetterOrDigit(c))) {
        return false;
      }
    }
    if (paramString.length() == 0) {
      return false;
    }
    return true;
  }
  
  private void pushBack()
  {
    this.inputBufferPos -= 1;
  }
  
  private int readChar()
    throws IOException
  {
    if (this.inputBufferPos >= this.inputBufferEnd) {
      return readBuffer();
    }
    return this.inputBuffer[(this.inputBufferPos++)];
  }
  
  private int readBuffer()
    throws IOException
  {
    if (this.endOfFile) {
      return -1;
    }
    int i;
    if (this.inputBufferStart >= 0)
    {
      i = this.inputBufferPos - this.inputBufferStart;
      if (i > 0)
      {
        char[] arrayOfChar = this.inputBuffer;
        if (i + 4096 > arrayOfChar.length) {
          this.inputBuffer = new char[arrayOfChar.length * 2];
        }
        System.arraycopy(arrayOfChar, this.inputBufferStart, this.inputBuffer, 0, i);
      }
      this.inputBufferStart = 0;
    }
    else
    {
      i = 0;
    }
    this.inputBufferPos = i;
    int j = this.input.read(this.inputBuffer, i, 4096);
    if (j == -1)
    {
      this.inputBufferEnd = 64512;
      this.endOfFile = true;
      
      this.inputBufferPos += 1;
      return -1;
    }
    this.inputBufferEnd = (i + j);
    return this.inputBuffer[(this.inputBufferPos++)];
  }
  
  private String readValue()
    throws IOException
  {
    this.endOfLine = false;
    this.inputBufferStart = this.inputBufferPos;
    int i;
    label108:
    do
    {
      i = readChar();
      if (i == this.fieldDelimiter)
      {
        int j = 0;
        this.inputBufferStart = this.inputBufferPos;
        do
        {
          for (;;)
          {
            i = readChar();
            if (i == this.fieldDelimiter)
            {
              i = readChar();
              if (i != this.fieldDelimiter)
              {
                k = 2;
                break label108;
              }
              j = 1;
            }
            else
            {
              if (i != this.escapeCharacter) {
                break;
              }
              i = readChar();
              if (i < 0)
              {
                k = 1;
                break label108;
              }
              j = 1;
            }
          }
        } while (i >= 0);
        int k = 1;
        
        String str2 = new String(this.inputBuffer, this.inputBufferStart, this.inputBufferPos - this.inputBufferStart - k);
        if (j != 0) {
          str2 = unEscape(str2);
        }
        this.inputBufferStart = -1;
        while (i != this.fieldSeparatorRead)
        {
          if ((i == 10) || (i < 0) || (i == 13))
          {
            this.endOfLine = true;
            break;
          }
          if ((i != 32) && (i != 9))
          {
            pushBack();
            break;
          }
          i = readChar();
        }
        return str2;
      }
      if ((i == 10) || (i < 0) || (i == 13))
      {
        this.endOfLine = true;
        return null;
      }
      if (i == this.fieldSeparatorRead) {
        return null;
      }
    } while (i <= 32);
    if ((this.lineComment != 0) && (i == this.lineComment))
    {
      this.inputBufferStart = -1;
      for (;;)
      {
        i = readChar();
        if ((i != 10) && (i >= 0)) {
          if (i == 13) {
            break;
          }
        }
      }
      this.endOfLine = true;
      return null;
    }
    do
    {
      i = readChar();
      if (i == this.fieldSeparatorRead) {
        break;
      }
    } while ((i != 10) && (i >= 0) && (i != 13));
    this.endOfLine = true;
    
    String str1 = new String(this.inputBuffer, this.inputBufferStart, this.inputBufferPos - this.inputBufferStart - 1);
    if (!this.preserveWhitespace) {
      str1 = str1.trim();
    }
    this.inputBufferStart = -1;
    
    return readNull(str1);
  }
  
  private String readNull(String paramString)
  {
    return paramString.equals(this.nullString) ? null : paramString;
  }
  
  private String unEscape(String paramString)
  {
    StringBuilder localStringBuilder = new StringBuilder(paramString.length());
    int i = 0;
    char[] arrayOfChar = null;
    for (;;)
    {
      int j = paramString.indexOf(this.escapeCharacter, i);
      if (j < 0)
      {
        j = paramString.indexOf(this.fieldDelimiter, i);
        if (j < 0) {
          break;
        }
      }
      if (arrayOfChar == null) {
        arrayOfChar = paramString.toCharArray();
      }
      localStringBuilder.append(arrayOfChar, i, j - i);
      if (j == paramString.length() - 1)
      {
        i = paramString.length();
        break;
      }
      localStringBuilder.append(arrayOfChar[(j + 1)]);
      i = j + 2;
    }
    localStringBuilder.append(paramString.substring(i));
    return localStringBuilder.toString();
  }
  
  public Object[] readRow()
    throws SQLException
  {
    if (this.input == null) {
      return null;
    }
    String[] arrayOfString = new String[this.columnNames.length];
    try
    {
      int i = 0;
      for (;;)
      {
        String str = readValue();
        if ((str == null) && 
          (this.endOfLine))
        {
          if (i != 0) {
            break;
          }
          if (this.endOfFile) {
            return null;
          }
        }
        else
        {
          if (i < arrayOfString.length) {
            arrayOfString[(i++)] = str;
          }
          if (this.endOfLine) {
            break;
          }
        }
      }
    }
    catch (IOException localIOException)
    {
      throw convertException("IOException reading from " + this.fileName, localIOException);
    }
    return arrayOfString;
  }
  
  private static SQLException convertException(String paramString, Exception paramException)
  {
    return DbException.get(90028, paramException, new String[] { paramString }).getSQLException();
  }
  
  public void close()
  {
    IOUtils.closeSilently(this.input);
    this.input = null;
    IOUtils.closeSilently(this.output);
    this.output = null;
  }
  
  public void reset()
    throws SQLException
  {
    throw new SQLException("Method is not supported", "CSV");
  }
  
  public void setFieldSeparatorWrite(String paramString)
  {
    this.fieldSeparatorWrite = paramString;
  }
  
  public String getFieldSeparatorWrite()
  {
    return this.fieldSeparatorWrite;
  }
  
  public void setCaseSensitiveColumnNames(boolean paramBoolean)
  {
    this.caseSensitiveColumnNames = paramBoolean;
  }
  
  public boolean getCaseSensitiveColumnNames()
  {
    return this.caseSensitiveColumnNames;
  }
  
  public void setFieldSeparatorRead(char paramChar)
  {
    this.fieldSeparatorRead = paramChar;
  }
  
  public char getFieldSeparatorRead()
  {
    return this.fieldSeparatorRead;
  }
  
  public void setLineCommentCharacter(char paramChar)
  {
    this.lineComment = paramChar;
  }
  
  public char getLineCommentCharacter()
  {
    return this.lineComment;
  }
  
  public void setFieldDelimiter(char paramChar)
  {
    this.fieldDelimiter = paramChar;
  }
  
  public char getFieldDelimiter()
  {
    return this.fieldDelimiter;
  }
  
  public void setEscapeCharacter(char paramChar)
  {
    this.escapeCharacter = paramChar;
  }
  
  public char getEscapeCharacter()
  {
    return this.escapeCharacter;
  }
  
  public void setLineSeparator(String paramString)
  {
    this.lineSeparator = paramString;
  }
  
  public String getLineSeparator()
  {
    return this.lineSeparator;
  }
  
  public void setNullString(String paramString)
  {
    this.nullString = paramString;
  }
  
  public String getNullString()
  {
    return this.nullString;
  }
  
  public void setPreserveWhitespace(boolean paramBoolean)
  {
    this.preserveWhitespace = paramBoolean;
  }
  
  public boolean getPreserveWhitespace()
  {
    return this.preserveWhitespace;
  }
  
  public void setWriteColumnHeader(boolean paramBoolean)
  {
    this.writeColumnHeader = paramBoolean;
  }
  
  public boolean getWriteColumnHeader()
  {
    return this.writeColumnHeader;
  }
  
  public String setOptions(String paramString)
  {
    Object localObject = null;
    String[] arrayOfString1 = StringUtils.arraySplit(paramString, ' ', false);
    for (String str1 : arrayOfString1) {
      if (str1.length() != 0)
      {
        int k = str1.indexOf('=');
        String str2 = StringUtils.trim(str1.substring(0, k), true, true, " ");
        String str3 = str1.substring(k + 1);
        char c = str3.length() == 0 ? '\000' : str3.charAt(0);
        if (isParam(str2, new String[] { "escape", "esc", "escapeCharacter" }))
        {
          setEscapeCharacter(c);
        }
        else if (isParam(str2, new String[] { "fieldDelimiter", "fieldDelim" }))
        {
          setFieldDelimiter(c);
        }
        else if (isParam(str2, new String[] { "fieldSeparator", "fieldSep" }))
        {
          setFieldSeparatorRead(c);
          setFieldSeparatorWrite(str3);
        }
        else if (isParam(str2, new String[] { "lineComment", "lineCommentCharacter" }))
        {
          setLineCommentCharacter(c);
        }
        else if (isParam(str2, new String[] { "lineSeparator", "lineSep" }))
        {
          setLineSeparator(str3);
        }
        else if (isParam(str2, new String[] { "null", "nullString" }))
        {
          setNullString(str3);
        }
        else if (isParam(str2, new String[] { "charset", "characterSet" }))
        {
          localObject = str3;
        }
        else if (isParam(str2, new String[] { "preserveWhitespace" }))
        {
          setPreserveWhitespace(Boolean.parseBoolean(str3));
        }
        else if (isParam(str2, new String[] { "writeColumnHeader" }))
        {
          setWriteColumnHeader(Boolean.parseBoolean(str3));
        }
        else if (isParam(str2, new String[] { "caseSensitiveColumnNames" }))
        {
          setCaseSensitiveColumnNames(Boolean.parseBoolean(str3));
        }
        else
        {
          throw DbException.getUnsupportedException(str2);
        }
      }
    }
    return (String)localObject;
  }
  
  private static boolean isParam(String paramString, String... paramVarArgs)
  {
    for (String str : paramVarArgs) {
      if (paramString.equalsIgnoreCase(str)) {
        return true;
      }
    }
    return false;
  }
}
