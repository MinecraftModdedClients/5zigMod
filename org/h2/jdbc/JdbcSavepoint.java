package org.h2.jdbc;

import java.sql.SQLException;
import java.sql.Savepoint;
import org.h2.command.CommandInterface;
import org.h2.message.DbException;
import org.h2.message.Trace;
import org.h2.message.TraceObject;
import org.h2.util.StringUtils;

public class JdbcSavepoint
  extends TraceObject
  implements Savepoint
{
  private static final String SYSTEM_SAVEPOINT_PREFIX = "SYSTEM_SAVEPOINT_";
  private final int savepointId;
  private final String name;
  private JdbcConnection conn;
  
  JdbcSavepoint(JdbcConnection paramJdbcConnection, int paramInt1, String paramString, Trace paramTrace, int paramInt2)
  {
    setTrace(paramTrace, 6, paramInt2);
    this.conn = paramJdbcConnection;
    this.savepointId = paramInt1;
    this.name = paramString;
  }
  
  void release()
  {
    this.conn = null;
  }
  
  static String getName(String paramString, int paramInt)
  {
    if (paramString != null) {
      return StringUtils.quoteJavaString(paramString);
    }
    return "SYSTEM_SAVEPOINT_" + paramInt;
  }
  
  void rollback()
  {
    checkValid();
    this.conn.prepareCommand("ROLLBACK TO SAVEPOINT " + getName(this.name, this.savepointId), Integer.MAX_VALUE).executeUpdate();
  }
  
  private void checkValid()
  {
    if (this.conn == null) {
      throw DbException.get(90063, getName(this.name, this.savepointId));
    }
  }
  
  public int getSavepointId()
    throws SQLException
  {
    try
    {
      debugCodeCall("getSavepointId");
      checkValid();
      if (this.name != null) {
        throw DbException.get(90065);
      }
      return this.savepointId;
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public String getSavepointName()
    throws SQLException
  {
    try
    {
      debugCodeCall("getSavepointName");
      checkValid();
      if (this.name == null) {
        throw DbException.get(90064);
      }
      return this.name;
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public String toString()
  {
    return getTraceObjectName() + ": id=" + this.savepointId + " name=" + this.name;
  }
}
