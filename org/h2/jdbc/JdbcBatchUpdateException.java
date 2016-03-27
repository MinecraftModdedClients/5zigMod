package org.h2.jdbc;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.sql.BatchUpdateException;
import java.sql.SQLException;

public class JdbcBatchUpdateException
  extends BatchUpdateException
{
  private static final long serialVersionUID = 1L;
  
  JdbcBatchUpdateException(SQLException paramSQLException, int[] paramArrayOfInt)
  {
    super(paramSQLException.getMessage(), paramSQLException.getSQLState(), paramSQLException.getErrorCode(), paramArrayOfInt);
    setNextException(paramSQLException);
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
      if (getNextException() != null) {
        getNextException().printStackTrace(paramPrintWriter);
      }
    }
  }
  
  public void printStackTrace(PrintStream paramPrintStream)
  {
    if (paramPrintStream != null)
    {
      super.printStackTrace(paramPrintStream);
      if (getNextException() != null) {
        getNextException().printStackTrace(paramPrintStream);
      }
    }
  }
}
