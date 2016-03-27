package org.h2.message;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Map;
import org.h2.util.StringUtils;

public class TraceObject
{
  protected static final int CALLABLE_STATEMENT = 0;
  protected static final int CONNECTION = 1;
  protected static final int DATABASE_META_DATA = 2;
  protected static final int PREPARED_STATEMENT = 3;
  protected static final int RESULT_SET = 4;
  protected static final int RESULT_SET_META_DATA = 5;
  protected static final int SAVEPOINT = 6;
  protected static final int STATEMENT = 8;
  protected static final int BLOB = 9;
  protected static final int CLOB = 10;
  protected static final int PARAMETER_META_DATA = 11;
  protected static final int DATA_SOURCE = 12;
  protected static final int XA_DATA_SOURCE = 13;
  protected static final int XID = 15;
  protected static final int ARRAY = 16;
  private static final int LAST = 17;
  private static final int[] ID = new int[17];
  private static final String[] PREFIX = { "call", "conn", "dbMeta", "prep", "rs", "rsMeta", "sp", "ex", "stat", "blob", "clob", "pMeta", "ds", "xads", "xares", "xid", "ar" };
  protected Trace trace;
  private int traceType;
  private int id;
  
  protected void setTrace(Trace paramTrace, int paramInt1, int paramInt2)
  {
    this.trace = paramTrace;
    this.traceType = paramInt1;
    this.id = paramInt2;
  }
  
  public int getTraceId()
  {
    return this.id;
  }
  
  public String getTraceObjectName()
  {
    return PREFIX[this.traceType] + this.id;
  }
  
  protected static int getNextId(int paramInt)
  {
    int tmp4_3 = paramInt; int[] tmp4_0 = ID; int tmp6_5 = tmp4_0[tmp4_3];tmp4_0[tmp4_3] = (tmp6_5 + 1);return tmp6_5;
  }
  
  protected boolean isDebugEnabled()
  {
    return this.trace.isDebugEnabled();
  }
  
  protected boolean isInfoEnabled()
  {
    return this.trace.isInfoEnabled();
  }
  
  protected void debugCodeAssign(String paramString1, int paramInt1, int paramInt2, String paramString2)
  {
    if (this.trace.isDebugEnabled()) {
      this.trace.debugCode(paramString1 + " " + PREFIX[paramInt1] + paramInt2 + " = " + getTraceObjectName() + "." + paramString2 + ";");
    }
  }
  
  protected void debugCodeCall(String paramString)
  {
    if (this.trace.isDebugEnabled()) {
      this.trace.debugCode(getTraceObjectName() + "." + paramString + "();");
    }
  }
  
  protected void debugCodeCall(String paramString, long paramLong)
  {
    if (this.trace.isDebugEnabled()) {
      this.trace.debugCode(getTraceObjectName() + "." + paramString + "(" + paramLong + ");");
    }
  }
  
  protected void debugCodeCall(String paramString1, String paramString2)
  {
    if (this.trace.isDebugEnabled()) {
      this.trace.debugCode(getTraceObjectName() + "." + paramString1 + "(" + quote(paramString2) + ");");
    }
  }
  
  protected void debugCode(String paramString)
  {
    if (this.trace.isDebugEnabled()) {
      this.trace.debugCode(getTraceObjectName() + "." + paramString);
    }
  }
  
  protected static String quote(String paramString)
  {
    return StringUtils.quoteJavaString(paramString);
  }
  
  protected static String quoteTime(Time paramTime)
  {
    if (paramTime == null) {
      return "null";
    }
    return "Time.valueOf(\"" + paramTime.toString() + "\")";
  }
  
  protected static String quoteTimestamp(Timestamp paramTimestamp)
  {
    if (paramTimestamp == null) {
      return "null";
    }
    return "Timestamp.valueOf(\"" + paramTimestamp.toString() + "\")";
  }
  
  protected static String quoteDate(Date paramDate)
  {
    if (paramDate == null) {
      return "null";
    }
    return "Date.valueOf(\"" + paramDate.toString() + "\")";
  }
  
  protected static String quoteBigDecimal(BigDecimal paramBigDecimal)
  {
    if (paramBigDecimal == null) {
      return "null";
    }
    return "new BigDecimal(\"" + paramBigDecimal.toString() + "\")";
  }
  
  protected static String quoteBytes(byte[] paramArrayOfByte)
  {
    if (paramArrayOfByte == null) {
      return "null";
    }
    return "org.h2.util.StringUtils.convertHexToBytes(\"" + StringUtils.convertBytesToHex(paramArrayOfByte) + "\")";
  }
  
  protected static String quoteArray(String[] paramArrayOfString)
  {
    return StringUtils.quoteJavaStringArray(paramArrayOfString);
  }
  
  protected static String quoteIntArray(int[] paramArrayOfInt)
  {
    return StringUtils.quoteJavaIntArray(paramArrayOfInt);
  }
  
  protected static String quoteMap(Map<String, Class<?>> paramMap)
  {
    if (paramMap == null) {
      return "null";
    }
    if (paramMap.size() == 0) {
      return "new Map()";
    }
    return "new Map() /* " + paramMap.toString() + " */";
  }
  
  protected SQLException logAndConvert(Exception paramException)
  {
    SQLException localSQLException = DbException.toSQLException(paramException);
    if (this.trace == null)
    {
      DbException.traceThrowable(localSQLException);
    }
    else
    {
      int i = localSQLException.getErrorCode();
      if ((i >= 23000) && (i < 24000)) {
        this.trace.info(localSQLException, "exception");
      } else {
        this.trace.error(localSQLException, "exception");
      }
    }
    return localSQLException;
  }
  
  protected SQLException unsupported(String paramString)
    throws SQLException
  {
    try
    {
      throw DbException.getUnsupportedException(paramString);
    }
    catch (Exception localException)
    {
      return logAndConvert(localException);
    }
  }
}
