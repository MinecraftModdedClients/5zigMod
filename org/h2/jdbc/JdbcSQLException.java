package org.h2.jdbc;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.sql.SQLException;

public class JdbcSQLException
  extends SQLException
{
  public static final String HIDE_SQL = "--hide--";
  private static final long serialVersionUID = 1L;
  private final String originalMessage;
  private final Throwable cause;
  private final String stackTrace;
  private String message;
  private String sql;
  
  public JdbcSQLException(String paramString1, String paramString2, String paramString3, int paramInt, Throwable paramThrowable, String paramString4)
  {
    super(paramString1, paramString3, paramInt);
    this.originalMessage = paramString1;
    setSQL(paramString2);
    this.cause = paramThrowable;
    this.stackTrace = paramString4;
    buildMessage();
    initCause(paramThrowable);
  }
  
  public String getMessage()
  {
    return this.message;
  }
  
  public String getOriginalMessage()
  {
    return this.originalMessage;
  }
  
  public void printStackTrace()
  {
    printStackTrace(System.err);
  }
  
  public void printStackTrace(PrintWriter paramPrintWriter)
  {
    if (paramPrintWriter != null)
    {
      super.printStackTrace(paramPrintWriter);
      
      SQLException localSQLException = getNextException();
      for (int i = 0; (i < 100) && (localSQLException != null); i++)
      {
        paramPrintWriter.println(localSQLException.toString());
        localSQLException = localSQLException.getNextException();
      }
      if (localSQLException != null) {
        paramPrintWriter.println("(truncated)");
      }
    }
  }
  
  public void printStackTrace(PrintStream paramPrintStream)
  {
    if (paramPrintStream != null)
    {
      super.printStackTrace(paramPrintStream);
      
      SQLException localSQLException = getNextException();
      for (int i = 0; (i < 100) && (localSQLException != null); i++)
      {
        paramPrintStream.println(localSQLException.toString());
        localSQLException = localSQLException.getNextException();
      }
      if (localSQLException != null) {
        paramPrintStream.println("(truncated)");
      }
    }
  }
  
  public Throwable getOriginalCause()
  {
    return this.cause;
  }
  
  public String getSQL()
  {
    return this.sql;
  }
  
  public void setSQL(String paramString)
  {
    if ((paramString != null) && (paramString.contains("--hide--"))) {
      paramString = "-";
    }
    this.sql = paramString;
    buildMessage();
  }
  
  private void buildMessage()
  {
    StringBuilder localStringBuilder = new StringBuilder(this.originalMessage == null ? "- " : this.originalMessage);
    if (this.sql != null) {
      localStringBuilder.append("; SQL statement:\n").append(this.sql);
    }
    localStringBuilder.append(" [").append(getErrorCode()).append('-').append(183).append(']');
    
    this.message = localStringBuilder.toString();
  }
  
  public String toString()
  {
    if (this.stackTrace == null) {
      return super.toString();
    }
    return this.stackTrace;
  }
}
