package org.h2.jdbc;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import org.h2.command.CommandInterface;
import org.h2.engine.SessionInterface;
import org.h2.expression.ParameterInterface;
import org.h2.message.DbException;
import org.h2.result.ResultInterface;
import org.h2.util.DateTimeUtils;
import org.h2.util.IOUtils;
import org.h2.util.New;
import org.h2.value.DataType;
import org.h2.value.Value;
import org.h2.value.ValueBoolean;
import org.h2.value.ValueByte;
import org.h2.value.ValueBytes;
import org.h2.value.ValueDate;
import org.h2.value.ValueDecimal;
import org.h2.value.ValueDouble;
import org.h2.value.ValueFloat;
import org.h2.value.ValueInt;
import org.h2.value.ValueLong;
import org.h2.value.ValueNull;
import org.h2.value.ValueShort;
import org.h2.value.ValueString;
import org.h2.value.ValueTime;
import org.h2.value.ValueTimestamp;

public class JdbcPreparedStatement
  extends JdbcStatement
  implements PreparedStatement
{
  protected CommandInterface command;
  private final String sqlStatement;
  private ArrayList<Value[]> batchParameters;
  private HashMap<String, Integer> cachedColumnLabelMap;
  
  JdbcPreparedStatement(JdbcConnection paramJdbcConnection, String paramString, int paramInt1, int paramInt2, int paramInt3, boolean paramBoolean)
  {
    super(paramJdbcConnection, paramInt1, paramInt2, paramInt3, paramBoolean);
    setTrace(this.session.getTrace(), 3, paramInt1);
    this.sqlStatement = paramString;
    this.command = paramJdbcConnection.prepareCommand(paramString, this.fetchSize);
  }
  
  void setCachedColumnLabelMap(HashMap<String, Integer> paramHashMap)
  {
    this.cachedColumnLabelMap = paramHashMap;
  }
  
  public ResultSet executeQuery()
    throws SQLException
  {
    try
    {
      int i = getNextId(4);
      if (isDebugEnabled()) {
        debugCodeAssign("ResultSet", 4, i, "executeQuery()");
      }
      synchronized (this.session)
      {
        checkClosed();
        closeOldResultSet();
        
        boolean bool1 = this.resultSetType != 1003;
        boolean bool2 = this.resultSetConcurrency == 1008;
        ResultInterface localResultInterface;
        try
        {
          setExecutingStatement(this.command);
          localResultInterface = this.command.executeQuery(this.maxRows, bool1);
        }
        finally
        {
          setExecutingStatement(null);
        }
        this.resultSet = new JdbcResultSet(this.conn, this, localResultInterface, i, this.closedByResultSet, bool1, bool2, this.cachedColumnLabelMap);
      }
      return this.resultSet;
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  /* Error */
  public int executeUpdate()
    throws SQLException
  {
    // Byte code:
    //   0: aload_0
    //   1: ldc 29
    //   3: invokevirtual 30	org/h2/jdbc/JdbcPreparedStatement:debugCodeCall	(Ljava/lang/String;)V
    //   6: aload_0
    //   7: invokevirtual 31	org/h2/jdbc/JdbcPreparedStatement:checkClosedForWrite	()Z
    //   10: pop
    //   11: aload_0
    //   12: invokespecial 32	org/h2/jdbc/JdbcPreparedStatement:executeUpdateInternal	()I
    //   15: istore_1
    //   16: aload_0
    //   17: invokevirtual 33	org/h2/jdbc/JdbcPreparedStatement:afterWriting	()V
    //   20: iload_1
    //   21: ireturn
    //   22: astore_2
    //   23: aload_0
    //   24: invokevirtual 33	org/h2/jdbc/JdbcPreparedStatement:afterWriting	()V
    //   27: aload_2
    //   28: athrow
    //   29: astore_1
    //   30: aload_0
    //   31: aload_1
    //   32: invokevirtual 28	org/h2/jdbc/JdbcPreparedStatement:logAndConvert	(Ljava/lang/Exception;)Ljava/sql/SQLException;
    //   35: athrow
    // Line number table:
    //   Java source line #140	-> byte code offset #0
    //   Java source line #141	-> byte code offset #6
    //   Java source line #143	-> byte code offset #11
    //   Java source line #145	-> byte code offset #16
    //   Java source line #147	-> byte code offset #29
    //   Java source line #148	-> byte code offset #30
    // Local variable table:
    //   start	length	slot	name	signature
    //   0	36	0	this	JdbcPreparedStatement
    //   29	3	1	localException	Exception
    //   22	6	2	localObject	Object
    // Exception table:
    //   from	to	target	type
    //   11	16	22	finally
    //   22	23	22	finally
    //   0	20	29	java/lang/Exception
    //   22	29	29	java/lang/Exception
  }
  
  private int executeUpdateInternal()
    throws SQLException
  {
    closeOldResultSet();
    synchronized (this.session)
    {
      try
      {
        setExecutingStatement(this.command);
        this.updateCount = this.command.executeUpdate();
      }
      finally
      {
        setExecutingStatement(null);
      }
    }
    return this.updateCount;
  }
  
  /* Error */
  public boolean execute()
    throws SQLException
  {
    // Byte code:
    //   0: iconst_4
    //   1: invokestatic 10	org/h2/jdbc/JdbcPreparedStatement:getNextId	(I)I
    //   4: istore_1
    //   5: aload_0
    //   6: invokevirtual 11	org/h2/jdbc/JdbcPreparedStatement:isDebugEnabled	()Z
    //   9: ifeq +9 -> 18
    //   12: aload_0
    //   13: ldc 36
    //   15: invokevirtual 30	org/h2/jdbc/JdbcPreparedStatement:debugCodeCall	(Ljava/lang/String;)V
    //   18: aload_0
    //   19: invokevirtual 31	org/h2/jdbc/JdbcPreparedStatement:checkClosedForWrite	()Z
    //   22: pop
    //   23: aload_0
    //   24: getfield 23	org/h2/jdbc/JdbcPreparedStatement:conn	Lorg/h2/jdbc/JdbcConnection;
    //   27: invokevirtual 37	org/h2/jdbc/JdbcConnection:getSession	()Lorg/h2/engine/SessionInterface;
    //   30: dup
    //   31: astore_3
    //   32: monitorenter
    //   33: aload_0
    //   34: invokevirtual 16	org/h2/jdbc/JdbcPreparedStatement:closeOldResultSet	()V
    //   37: aload_0
    //   38: aload_0
    //   39: getfield 8	org/h2/jdbc/JdbcPreparedStatement:command	Lorg/h2/command/CommandInterface;
    //   42: invokevirtual 19	org/h2/jdbc/JdbcPreparedStatement:setExecutingStatement	(Lorg/h2/command/CommandInterface;)V
    //   45: aload_0
    //   46: getfield 8	org/h2/jdbc/JdbcPreparedStatement:command	Lorg/h2/command/CommandInterface;
    //   49: invokeinterface 38 1 0
    //   54: ifeq +86 -> 140
    //   57: iconst_1
    //   58: istore_2
    //   59: aload_0
    //   60: getfield 17	org/h2/jdbc/JdbcPreparedStatement:resultSetType	I
    //   63: sipush 1003
    //   66: if_icmpeq +7 -> 73
    //   69: iconst_1
    //   70: goto +4 -> 74
    //   73: iconst_0
    //   74: istore 4
    //   76: aload_0
    //   77: getfield 18	org/h2/jdbc/JdbcPreparedStatement:resultSetConcurrency	I
    //   80: sipush 1008
    //   83: if_icmpne +7 -> 90
    //   86: iconst_1
    //   87: goto +4 -> 91
    //   90: iconst_0
    //   91: istore 5
    //   93: aload_0
    //   94: getfield 8	org/h2/jdbc/JdbcPreparedStatement:command	Lorg/h2/command/CommandInterface;
    //   97: aload_0
    //   98: getfield 20	org/h2/jdbc/JdbcPreparedStatement:maxRows	I
    //   101: iload 4
    //   103: invokeinterface 21 3 0
    //   108: astore 6
    //   110: aload_0
    //   111: new 22	org/h2/jdbc/JdbcResultSet
    //   114: dup
    //   115: aload_0
    //   116: getfield 23	org/h2/jdbc/JdbcPreparedStatement:conn	Lorg/h2/jdbc/JdbcConnection;
    //   119: aload_0
    //   120: aload 6
    //   122: iload_1
    //   123: aload_0
    //   124: getfield 24	org/h2/jdbc/JdbcPreparedStatement:closedByResultSet	Z
    //   127: iload 4
    //   129: iload 5
    //   131: invokespecial 39	org/h2/jdbc/JdbcResultSet:<init>	(Lorg/h2/jdbc/JdbcConnection;Lorg/h2/jdbc/JdbcStatement;Lorg/h2/result/ResultInterface;IZZZ)V
    //   134: putfield 26	org/h2/jdbc/JdbcPreparedStatement:resultSet	Lorg/h2/jdbc/JdbcResultSet;
    //   137: goto +18 -> 155
    //   140: iconst_0
    //   141: istore_2
    //   142: aload_0
    //   143: aload_0
    //   144: getfield 8	org/h2/jdbc/JdbcPreparedStatement:command	Lorg/h2/command/CommandInterface;
    //   147: invokeinterface 34 1 0
    //   152: putfield 35	org/h2/jdbc/JdbcPreparedStatement:updateCount	I
    //   155: aload_0
    //   156: aconst_null
    //   157: invokevirtual 19	org/h2/jdbc/JdbcPreparedStatement:setExecutingStatement	(Lorg/h2/command/CommandInterface;)V
    //   160: goto +13 -> 173
    //   163: astore 7
    //   165: aload_0
    //   166: aconst_null
    //   167: invokevirtual 19	org/h2/jdbc/JdbcPreparedStatement:setExecutingStatement	(Lorg/h2/command/CommandInterface;)V
    //   170: aload 7
    //   172: athrow
    //   173: aload_3
    //   174: monitorexit
    //   175: goto +10 -> 185
    //   178: astore 8
    //   180: aload_3
    //   181: monitorexit
    //   182: aload 8
    //   184: athrow
    //   185: iload_2
    //   186: istore_3
    //   187: aload_0
    //   188: invokevirtual 33	org/h2/jdbc/JdbcPreparedStatement:afterWriting	()V
    //   191: iload_3
    //   192: ireturn
    //   193: astore 9
    //   195: aload_0
    //   196: invokevirtual 33	org/h2/jdbc/JdbcPreparedStatement:afterWriting	()V
    //   199: aload 9
    //   201: athrow
    //   202: astore_1
    //   203: aload_0
    //   204: aload_1
    //   205: invokevirtual 28	org/h2/jdbc/JdbcPreparedStatement:logAndConvert	(Ljava/lang/Exception;)Ljava/sql/SQLException;
    //   208: athrow
    // Line number table:
    //   Java source line #177	-> byte code offset #0
    //   Java source line #178	-> byte code offset #5
    //   Java source line #179	-> byte code offset #12
    //   Java source line #181	-> byte code offset #18
    //   Java source line #184	-> byte code offset #23
    //   Java source line #185	-> byte code offset #33
    //   Java source line #187	-> byte code offset #37
    //   Java source line #188	-> byte code offset #45
    //   Java source line #189	-> byte code offset #57
    //   Java source line #190	-> byte code offset #59
    //   Java source line #191	-> byte code offset #76
    //   Java source line #192	-> byte code offset #93
    //   Java source line #193	-> byte code offset #110
    //   Java source line #196	-> byte code offset #137
    //   Java source line #197	-> byte code offset #140
    //   Java source line #198	-> byte code offset #142
    //   Java source line #201	-> byte code offset #155
    //   Java source line #202	-> byte code offset #160
    //   Java source line #201	-> byte code offset #163
    //   Java source line #203	-> byte code offset #173
    //   Java source line #204	-> byte code offset #185
    //   Java source line #206	-> byte code offset #187
    //   Java source line #208	-> byte code offset #202
    //   Java source line #209	-> byte code offset #203
    // Local variable table:
    //   start	length	slot	name	signature
    //   0	209	0	this	JdbcPreparedStatement
    //   4	119	1	i	int
    //   202	3	1	localException	Exception
    //   58	128	2	localObject1	Object
    //   74	54	4	bool1	boolean
    //   91	39	5	bool2	boolean
    //   108	13	6	localResultInterface	ResultInterface
    //   163	8	7	localObject2	Object
    //   178	5	8	localObject3	Object
    //   193	7	9	localObject4	Object
    // Exception table:
    //   from	to	target	type
    //   37	155	163	finally
    //   163	165	163	finally
    //   33	175	178	finally
    //   178	182	178	finally
    //   23	187	193	finally
    //   193	195	193	finally
    //   0	191	202	java/lang/Exception
    //   193	202	202	java/lang/Exception
  }
  
  public void clearParameters()
    throws SQLException
  {
    try
    {
      debugCodeCall("clearParameters");
      checkClosed();
      ArrayList localArrayList = this.command.getParameters();
      int i = 0;
      for (int j = localArrayList.size(); i < j; i++)
      {
        ParameterInterface localParameterInterface = (ParameterInterface)localArrayList.get(i);
        
        localParameterInterface.setValue(null, this.batchParameters == null);
      }
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public ResultSet executeQuery(String paramString)
    throws SQLException
  {
    try
    {
      debugCodeCall("executeQuery", paramString);
      throw DbException.get(90130);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void addBatch(String paramString)
    throws SQLException
  {
    try
    {
      debugCodeCall("addBatch", paramString);
      throw DbException.get(90130);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public int executeUpdate(String paramString)
    throws SQLException
  {
    try
    {
      debugCodeCall("executeUpdate", paramString);
      throw DbException.get(90130);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public boolean execute(String paramString)
    throws SQLException
  {
    try
    {
      debugCodeCall("execute", paramString);
      throw DbException.get(90130);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void setNull(int paramInt1, int paramInt2)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("setNull(" + paramInt1 + ", " + paramInt2 + ");");
      }
      setParameter(paramInt1, ValueNull.INSTANCE);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void setInt(int paramInt1, int paramInt2)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("setInt(" + paramInt1 + ", " + paramInt2 + ");");
      }
      setParameter(paramInt1, ValueInt.get(paramInt2));
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void setString(int paramInt, String paramString)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("setString(" + paramInt + ", " + quote(paramString) + ");");
      }
      Value localValue = paramString == null ? ValueNull.INSTANCE : ValueString.get(paramString);
      setParameter(paramInt, localValue);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void setBigDecimal(int paramInt, BigDecimal paramBigDecimal)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("setBigDecimal(" + paramInt + ", " + quoteBigDecimal(paramBigDecimal) + ");");
      }
      ValueDecimal localValueDecimal = paramBigDecimal == null ? ValueNull.INSTANCE : ValueDecimal.get(paramBigDecimal);
      setParameter(paramInt, localValueDecimal);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void setDate(int paramInt, Date paramDate)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("setDate(" + paramInt + ", " + quoteDate(paramDate) + ");");
      }
      ValueDate localValueDate = paramDate == null ? ValueNull.INSTANCE : ValueDate.get(paramDate);
      setParameter(paramInt, localValueDate);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void setTime(int paramInt, Time paramTime)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("setTime(" + paramInt + ", " + quoteTime(paramTime) + ");");
      }
      ValueTime localValueTime = paramTime == null ? ValueNull.INSTANCE : ValueTime.get(paramTime);
      setParameter(paramInt, localValueTime);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void setTimestamp(int paramInt, Timestamp paramTimestamp)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("setTimestamp(" + paramInt + ", " + quoteTimestamp(paramTimestamp) + ");");
      }
      ValueTimestamp localValueTimestamp = paramTimestamp == null ? ValueNull.INSTANCE : ValueTimestamp.get(paramTimestamp);
      setParameter(paramInt, localValueTimestamp);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void setObject(int paramInt, Object paramObject)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("setObject(" + paramInt + ", x);");
      }
      if (paramObject == null) {
        setParameter(paramInt, ValueNull.INSTANCE);
      } else {
        setParameter(paramInt, DataType.convertToValue(this.session, paramObject, -1));
      }
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void setObject(int paramInt1, Object paramObject, int paramInt2)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("setObject(" + paramInt1 + ", x, " + paramInt2 + ");");
      }
      int i = DataType.convertSQLTypeToValueType(paramInt2);
      if (paramObject == null)
      {
        setParameter(paramInt1, ValueNull.INSTANCE);
      }
      else
      {
        Value localValue = DataType.convertToValue(this.conn.getSession(), paramObject, i);
        setParameter(paramInt1, localValue.convertTo(i));
      }
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void setObject(int paramInt1, Object paramObject, int paramInt2, int paramInt3)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("setObject(" + paramInt1 + ", x, " + paramInt2 + ", " + paramInt3 + ");");
      }
      setObject(paramInt1, paramObject, paramInt2);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void setBoolean(int paramInt, boolean paramBoolean)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("setBoolean(" + paramInt + ", " + paramBoolean + ");");
      }
      setParameter(paramInt, ValueBoolean.get(paramBoolean));
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void setByte(int paramInt, byte paramByte)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("setByte(" + paramInt + ", " + paramByte + ");");
      }
      setParameter(paramInt, ValueByte.get(paramByte));
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void setShort(int paramInt, short paramShort)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("setShort(" + paramInt + ", (short) " + paramShort + ");");
      }
      setParameter(paramInt, ValueShort.get(paramShort));
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void setLong(int paramInt, long paramLong)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("setLong(" + paramInt + ", " + paramLong + "L);");
      }
      setParameter(paramInt, ValueLong.get(paramLong));
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void setFloat(int paramInt, float paramFloat)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("setFloat(" + paramInt + ", " + paramFloat + "f);");
      }
      setParameter(paramInt, ValueFloat.get(paramFloat));
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void setDouble(int paramInt, double paramDouble)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("setDouble(" + paramInt + ", " + paramDouble + "d);");
      }
      setParameter(paramInt, ValueDouble.get(paramDouble));
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void setRef(int paramInt, Ref paramRef)
    throws SQLException
  {
    throw unsupported("ref");
  }
  
  public void setDate(int paramInt, Date paramDate, Calendar paramCalendar)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("setDate(" + paramInt + ", " + quoteDate(paramDate) + ", calendar);");
      }
      if (paramDate == null) {
        setParameter(paramInt, ValueNull.INSTANCE);
      } else {
        setParameter(paramInt, DateTimeUtils.convertDate(paramDate, paramCalendar));
      }
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void setTime(int paramInt, Time paramTime, Calendar paramCalendar)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("setTime(" + paramInt + ", " + quoteTime(paramTime) + ", calendar);");
      }
      if (paramTime == null) {
        setParameter(paramInt, ValueNull.INSTANCE);
      } else {
        setParameter(paramInt, DateTimeUtils.convertTime(paramTime, paramCalendar));
      }
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void setTimestamp(int paramInt, Timestamp paramTimestamp, Calendar paramCalendar)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("setTimestamp(" + paramInt + ", " + quoteTimestamp(paramTimestamp) + ", calendar);");
      }
      if (paramTimestamp == null) {
        setParameter(paramInt, ValueNull.INSTANCE);
      } else {
        setParameter(paramInt, DateTimeUtils.convertTimestamp(paramTimestamp, paramCalendar));
      }
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  /**
   * @deprecated
   */
  public void setUnicodeStream(int paramInt1, InputStream paramInputStream, int paramInt2)
    throws SQLException
  {
    throw unsupported("unicodeStream");
  }
  
  public void setNull(int paramInt1, int paramInt2, String paramString)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("setNull(" + paramInt1 + ", " + paramInt2 + ", " + quote(paramString) + ");");
      }
      setNull(paramInt1, paramInt2);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void setBlob(int paramInt, Blob paramBlob)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("setBlob(" + paramInt + ", x);");
      }
      checkClosedForWrite();
      try
      {
        Object localObject1;
        if (paramBlob == null) {
          localObject1 = ValueNull.INSTANCE;
        } else {
          localObject1 = this.conn.createBlob(paramBlob.getBinaryStream(), -1L);
        }
        setParameter(paramInt, (Value)localObject1);
      }
      finally
      {
        afterWriting();
      }
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void setBlob(int paramInt, InputStream paramInputStream)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("setBlob(" + paramInt + ", x);");
      }
      checkClosedForWrite();
      try
      {
        Value localValue = this.conn.createBlob(paramInputStream, -1L);
        setParameter(paramInt, localValue);
      }
      finally
      {
        afterWriting();
      }
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void setClob(int paramInt, Clob paramClob)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("setClob(" + paramInt + ", x);");
      }
      checkClosedForWrite();
      try
      {
        Object localObject1;
        if (paramClob == null) {
          localObject1 = ValueNull.INSTANCE;
        } else {
          localObject1 = this.conn.createClob(paramClob.getCharacterStream(), -1L);
        }
        setParameter(paramInt, (Value)localObject1);
      }
      finally
      {
        afterWriting();
      }
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void setClob(int paramInt, Reader paramReader)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("setClob(" + paramInt + ", x);");
      }
      checkClosedForWrite();
      try
      {
        Object localObject1;
        if (paramReader == null) {
          localObject1 = ValueNull.INSTANCE;
        } else {
          localObject1 = this.conn.createClob(paramReader, -1L);
        }
        setParameter(paramInt, (Value)localObject1);
      }
      finally
      {
        afterWriting();
      }
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void setArray(int paramInt, Array paramArray)
    throws SQLException
  {
    throw unsupported("setArray");
  }
  
  public void setBytes(int paramInt, byte[] paramArrayOfByte)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("setBytes(" + paramInt + ", " + quoteBytes(paramArrayOfByte) + ");");
      }
      ValueBytes localValueBytes = paramArrayOfByte == null ? ValueNull.INSTANCE : ValueBytes.get(paramArrayOfByte);
      setParameter(paramInt, localValueBytes);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void setBinaryStream(int paramInt, InputStream paramInputStream, long paramLong)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("setBinaryStream(" + paramInt + ", x, " + paramLong + "L);");
      }
      checkClosedForWrite();
      try
      {
        Value localValue = this.conn.createBlob(paramInputStream, paramLong);
        setParameter(paramInt, localValue);
      }
      finally
      {
        afterWriting();
      }
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void setBinaryStream(int paramInt1, InputStream paramInputStream, int paramInt2)
    throws SQLException
  {
    setBinaryStream(paramInt1, paramInputStream, paramInt2);
  }
  
  public void setBinaryStream(int paramInt, InputStream paramInputStream)
    throws SQLException
  {
    setBinaryStream(paramInt, paramInputStream, -1);
  }
  
  public void setAsciiStream(int paramInt1, InputStream paramInputStream, int paramInt2)
    throws SQLException
  {
    setAsciiStream(paramInt1, paramInputStream, paramInt2);
  }
  
  public void setAsciiStream(int paramInt, InputStream paramInputStream, long paramLong)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("setAsciiStream(" + paramInt + ", x, " + paramLong + "L);");
      }
      checkClosedForWrite();
      try
      {
        Value localValue = this.conn.createClob(IOUtils.getAsciiReader(paramInputStream), paramLong);
        setParameter(paramInt, localValue);
      }
      finally
      {
        afterWriting();
      }
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void setAsciiStream(int paramInt, InputStream paramInputStream)
    throws SQLException
  {
    setAsciiStream(paramInt, paramInputStream, -1);
  }
  
  public void setCharacterStream(int paramInt1, Reader paramReader, int paramInt2)
    throws SQLException
  {
    setCharacterStream(paramInt1, paramReader, paramInt2);
  }
  
  public void setCharacterStream(int paramInt, Reader paramReader)
    throws SQLException
  {
    setCharacterStream(paramInt, paramReader, -1);
  }
  
  public void setCharacterStream(int paramInt, Reader paramReader, long paramLong)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("setCharacterStream(" + paramInt + ", x, " + paramLong + "L);");
      }
      checkClosedForWrite();
      try
      {
        Value localValue = this.conn.createClob(paramReader, paramLong);
        setParameter(paramInt, localValue);
      }
      finally
      {
        afterWriting();
      }
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void setURL(int paramInt, URL paramURL)
    throws SQLException
  {
    throw unsupported("url");
  }
  
  public ResultSetMetaData getMetaData()
    throws SQLException
  {
    try
    {
      debugCodeCall("getMetaData");
      checkClosed();
      ResultInterface localResultInterface = this.command.getMetaData();
      if (localResultInterface == null) {
        return null;
      }
      int i = getNextId(5);
      if (isDebugEnabled()) {
        debugCodeAssign("ResultSetMetaData", 5, i, "getMetaData()");
      }
      String str = this.conn.getCatalog();
      return new JdbcResultSetMetaData(null, this, localResultInterface, str, this.session.getTrace(), i);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void clearBatch()
    throws SQLException
  {
    try
    {
      debugCodeCall("clearBatch");
      checkClosed();
      this.batchParameters = null;
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void close()
    throws SQLException
  {
    try
    {
      super.close();
      this.batchParameters = null;
      if (this.command != null)
      {
        this.command.close();
        this.command = null;
      }
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  /* Error */
  public int[] executeBatch()
    throws SQLException
  {
    // Byte code:
    //   0: aload_0
    //   1: ldc -108
    //   3: invokevirtual 30	org/h2/jdbc/JdbcPreparedStatement:debugCodeCall	(Ljava/lang/String;)V
    //   6: aload_0
    //   7: getfield 45	org/h2/jdbc/JdbcPreparedStatement:batchParameters	Ljava/util/ArrayList;
    //   10: ifnonnull +10 -> 20
    //   13: aload_0
    //   14: invokestatic 149	org/h2/util/New:arrayList	()Ljava/util/ArrayList;
    //   17: putfield 45	org/h2/jdbc/JdbcPreparedStatement:batchParameters	Ljava/util/ArrayList;
    //   20: aload_0
    //   21: getfield 45	org/h2/jdbc/JdbcPreparedStatement:batchParameters	Ljava/util/ArrayList;
    //   24: invokevirtual 42	java/util/ArrayList:size	()I
    //   27: istore_1
    //   28: iload_1
    //   29: newarray <illegal type>
    //   31: astore_2
    //   32: iconst_0
    //   33: istore_3
    //   34: aconst_null
    //   35: astore 4
    //   37: aload_0
    //   38: invokevirtual 31	org/h2/jdbc/JdbcPreparedStatement:checkClosedForWrite	()Z
    //   41: pop
    //   42: iconst_0
    //   43: istore 5
    //   45: iload 5
    //   47: iload_1
    //   48: if_icmpge +132 -> 180
    //   51: aload_0
    //   52: getfield 45	org/h2/jdbc/JdbcPreparedStatement:batchParameters	Ljava/util/ArrayList;
    //   55: iload 5
    //   57: invokevirtual 43	java/util/ArrayList:get	(I)Ljava/lang/Object;
    //   60: checkcast 150	[Lorg/h2/value/Value;
    //   63: astore 6
    //   65: aload_0
    //   66: getfield 8	org/h2/jdbc/JdbcPreparedStatement:command	Lorg/h2/command/CommandInterface;
    //   69: invokeinterface 41 1 0
    //   74: astore 7
    //   76: iconst_0
    //   77: istore 8
    //   79: iload 8
    //   81: aload 6
    //   83: arraylength
    //   84: if_icmpge +38 -> 122
    //   87: aload 6
    //   89: iload 8
    //   91: aaload
    //   92: astore 9
    //   94: aload 7
    //   96: iload 8
    //   98: invokevirtual 43	java/util/ArrayList:get	(I)Ljava/lang/Object;
    //   101: checkcast 44	org/h2/expression/ParameterInterface
    //   104: astore 10
    //   106: aload 10
    //   108: aload 9
    //   110: iconst_0
    //   111: invokeinterface 46 3 0
    //   116: iinc 8 1
    //   119: goto -40 -> 79
    //   122: aload_2
    //   123: iload 5
    //   125: aload_0
    //   126: invokespecial 32	org/h2/jdbc/JdbcPreparedStatement:executeUpdateInternal	()I
    //   129: iastore
    //   130: goto +44 -> 174
    //   133: astore 8
    //   135: aload_0
    //   136: aload 8
    //   138: invokevirtual 28	org/h2/jdbc/JdbcPreparedStatement:logAndConvert	(Ljava/lang/Exception;)Ljava/sql/SQLException;
    //   141: astore 9
    //   143: aload 4
    //   145: ifnonnull +10 -> 155
    //   148: aload 9
    //   150: astore 4
    //   152: goto +14 -> 166
    //   155: aload 9
    //   157: aload 4
    //   159: invokevirtual 151	java/sql/SQLException:setNextException	(Ljava/sql/SQLException;)V
    //   162: aload 9
    //   164: astore 4
    //   166: aload_2
    //   167: iload 5
    //   169: bipush -3
    //   171: iastore
    //   172: iconst_1
    //   173: istore_3
    //   174: iinc 5 1
    //   177: goto -132 -> 45
    //   180: aload_0
    //   181: aconst_null
    //   182: putfield 45	org/h2/jdbc/JdbcPreparedStatement:batchParameters	Ljava/util/ArrayList;
    //   185: iload_3
    //   186: ifeq +18 -> 204
    //   189: new 152	org/h2/jdbc/JdbcBatchUpdateException
    //   192: dup
    //   193: aload 4
    //   195: aload_2
    //   196: invokespecial 153	org/h2/jdbc/JdbcBatchUpdateException:<init>	(Ljava/sql/SQLException;[I)V
    //   199: astore 5
    //   201: aload 5
    //   203: athrow
    //   204: aload_2
    //   205: astore 5
    //   207: aload_0
    //   208: invokevirtual 33	org/h2/jdbc/JdbcPreparedStatement:afterWriting	()V
    //   211: aload 5
    //   213: areturn
    //   214: astore 11
    //   216: aload_0
    //   217: invokevirtual 33	org/h2/jdbc/JdbcPreparedStatement:afterWriting	()V
    //   220: aload 11
    //   222: athrow
    //   223: astore_1
    //   224: aload_0
    //   225: aload_1
    //   226: invokevirtual 28	org/h2/jdbc/JdbcPreparedStatement:logAndConvert	(Ljava/lang/Exception;)Ljava/sql/SQLException;
    //   229: athrow
    // Line number table:
    //   Java source line #1161	-> byte code offset #0
    //   Java source line #1162	-> byte code offset #6
    //   Java source line #1165	-> byte code offset #13
    //   Java source line #1167	-> byte code offset #20
    //   Java source line #1168	-> byte code offset #28
    //   Java source line #1169	-> byte code offset #32
    //   Java source line #1170	-> byte code offset #34
    //   Java source line #1171	-> byte code offset #37
    //   Java source line #1173	-> byte code offset #42
    //   Java source line #1174	-> byte code offset #51
    //   Java source line #1175	-> byte code offset #65
    //   Java source line #1177	-> byte code offset #76
    //   Java source line #1178	-> byte code offset #87
    //   Java source line #1179	-> byte code offset #94
    //   Java source line #1180	-> byte code offset #106
    //   Java source line #1177	-> byte code offset #116
    //   Java source line #1183	-> byte code offset #122
    //   Java source line #1194	-> byte code offset #130
    //   Java source line #1184	-> byte code offset #133
    //   Java source line #1185	-> byte code offset #135
    //   Java source line #1186	-> byte code offset #143
    //   Java source line #1187	-> byte code offset #148
    //   Java source line #1189	-> byte code offset #155
    //   Java source line #1190	-> byte code offset #162
    //   Java source line #1192	-> byte code offset #166
    //   Java source line #1193	-> byte code offset #172
    //   Java source line #1173	-> byte code offset #174
    //   Java source line #1196	-> byte code offset #180
    //   Java source line #1197	-> byte code offset #185
    //   Java source line #1198	-> byte code offset #189
    //   Java source line #1199	-> byte code offset #201
    //   Java source line #1201	-> byte code offset #204
    //   Java source line #1203	-> byte code offset #207
    //   Java source line #1205	-> byte code offset #223
    //   Java source line #1206	-> byte code offset #224
    // Local variable table:
    //   start	length	slot	name	signature
    //   0	230	0	this	JdbcPreparedStatement
    //   27	22	1	i	int
    //   223	3	1	localException1	Exception
    //   31	174	2	arrayOfInt	int[]
    //   33	153	3	j	int
    //   35	159	4	localObject1	Object
    //   43	132	5	k	int
    //   199	13	5	localObject2	Object
    //   63	25	6	arrayOfValue	Value[]
    //   74	21	7	localArrayList	ArrayList
    //   77	40	8	m	int
    //   133	4	8	localException2	Exception
    //   92	71	9	localObject3	Object
    //   104	3	10	localParameterInterface	ParameterInterface
    //   214	7	11	localObject4	Object
    // Exception table:
    //   from	to	target	type
    //   122	130	133	java/lang/Exception
    //   42	207	214	finally
    //   214	216	214	finally
    //   0	211	223	java/lang/Exception
    //   214	223	223	java/lang/Exception
  }
  
  public void addBatch()
    throws SQLException
  {
    try
    {
      debugCodeCall("addBatch");
      checkClosedForWrite();
      try
      {
        ArrayList localArrayList = this.command.getParameters();
        
        int i = localArrayList.size();
        Value[] arrayOfValue = new Value[i];
        for (int j = 0; j < i; j++)
        {
          ParameterInterface localParameterInterface = (ParameterInterface)localArrayList.get(j);
          Value localValue = localParameterInterface.getParamValue();
          arrayOfValue[j] = localValue;
        }
        if (this.batchParameters == null) {
          this.batchParameters = New.arrayList();
        }
        this.batchParameters.add(arrayOfValue);
      }
      finally
      {
        afterWriting();
      }
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public int executeUpdate(String paramString, int paramInt)
    throws SQLException
  {
    try
    {
      debugCode("executeUpdate(" + quote(paramString) + ", " + paramInt + ");");
      throw DbException.get(90130);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public int executeUpdate(String paramString, int[] paramArrayOfInt)
    throws SQLException
  {
    try
    {
      debugCode("executeUpdate(" + quote(paramString) + ", " + quoteIntArray(paramArrayOfInt) + ");");
      
      throw DbException.get(90130);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public int executeUpdate(String paramString, String[] paramArrayOfString)
    throws SQLException
  {
    try
    {
      debugCode("executeUpdate(" + quote(paramString) + ", " + quoteArray(paramArrayOfString) + ");");
      
      throw DbException.get(90130);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public boolean execute(String paramString, int paramInt)
    throws SQLException
  {
    try
    {
      debugCode("execute(" + quote(paramString) + ", " + paramInt + ");");
      throw DbException.get(90130);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public boolean execute(String paramString, int[] paramArrayOfInt)
    throws SQLException
  {
    try
    {
      debugCode("execute(" + quote(paramString) + ", " + quoteIntArray(paramArrayOfInt) + ");");
      throw DbException.get(90130);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public boolean execute(String paramString, String[] paramArrayOfString)
    throws SQLException
  {
    try
    {
      debugCode("execute(" + quote(paramString) + ", " + quoteArray(paramArrayOfString) + ");");
      throw DbException.get(90130);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public ParameterMetaData getParameterMetaData()
    throws SQLException
  {
    try
    {
      int i = getNextId(11);
      if (isDebugEnabled()) {
        debugCodeAssign("ParameterMetaData", 11, i, "getParameterMetaData()");
      }
      checkClosed();
      return new JdbcParameterMetaData(this.session.getTrace(), this, this.command, i);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  private void setParameter(int paramInt, Value paramValue)
  {
    checkClosed();
    paramInt--;
    ArrayList localArrayList = this.command.getParameters();
    if ((paramInt < 0) || (paramInt >= localArrayList.size())) {
      throw DbException.getInvalidValueException("parameterIndex", Integer.valueOf(paramInt + 1));
    }
    ParameterInterface localParameterInterface = (ParameterInterface)localArrayList.get(paramInt);
    
    localParameterInterface.setValue(paramValue, this.batchParameters == null);
  }
  
  public void setRowId(int paramInt, RowId paramRowId)
    throws SQLException
  {
    throw unsupported("rowId");
  }
  
  public void setNString(int paramInt, String paramString)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("setNString(" + paramInt + ", " + quote(paramString) + ");");
      }
      Value localValue = paramString == null ? ValueNull.INSTANCE : ValueString.get(paramString);
      setParameter(paramInt, localValue);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void setNCharacterStream(int paramInt, Reader paramReader, long paramLong)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("setNCharacterStream(" + paramInt + ", x, " + paramLong + "L);");
      }
      checkClosedForWrite();
      try
      {
        Value localValue = this.conn.createClob(paramReader, paramLong);
        setParameter(paramInt, localValue);
      }
      finally
      {
        afterWriting();
      }
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void setNCharacterStream(int paramInt, Reader paramReader)
    throws SQLException
  {
    setNCharacterStream(paramInt, paramReader, -1L);
  }
  
  public void setNClob(int paramInt, NClob paramNClob)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("setNClob(" + paramInt + ", x);");
      }
      checkClosedForWrite();
      Object localObject;
      if (paramNClob == null) {
        localObject = ValueNull.INSTANCE;
      } else {
        localObject = this.conn.createClob(paramNClob.getCharacterStream(), -1L);
      }
      setParameter(paramInt, (Value)localObject);
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void setNClob(int paramInt, Reader paramReader)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("setNClob(" + paramInt + ", x);");
      }
      checkClosedForWrite();
      try
      {
        Value localValue = this.conn.createClob(paramReader, -1L);
        setParameter(paramInt, localValue);
      }
      finally
      {
        afterWriting();
      }
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void setClob(int paramInt, Reader paramReader, long paramLong)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("setClob(" + paramInt + ", x, " + paramLong + "L);");
      }
      checkClosedForWrite();
      try
      {
        Value localValue = this.conn.createClob(paramReader, paramLong);
        setParameter(paramInt, localValue);
      }
      finally
      {
        afterWriting();
      }
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void setBlob(int paramInt, InputStream paramInputStream, long paramLong)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("setBlob(" + paramInt + ", x, " + paramLong + "L);");
      }
      checkClosedForWrite();
      try
      {
        Value localValue = this.conn.createBlob(paramInputStream, paramLong);
        setParameter(paramInt, localValue);
      }
      finally
      {
        afterWriting();
      }
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void setNClob(int paramInt, Reader paramReader, long paramLong)
    throws SQLException
  {
    try
    {
      if (isDebugEnabled()) {
        debugCode("setNClob(" + paramInt + ", x, " + paramLong + "L);");
      }
      checkClosedForWrite();
      try
      {
        Value localValue = this.conn.createClob(paramReader, paramLong);
        setParameter(paramInt, localValue);
      }
      finally
      {
        afterWriting();
      }
    }
    catch (Exception localException)
    {
      throw logAndConvert(localException);
    }
  }
  
  public void setSQLXML(int paramInt, SQLXML paramSQLXML)
    throws SQLException
  {
    throw unsupported("SQLXML");
  }
  
  public String toString()
  {
    return getTraceObjectName() + ": " + this.command;
  }
  
  protected boolean checkClosed(boolean paramBoolean)
  {
    if (super.checkClosed(paramBoolean))
    {
      ArrayList localArrayList1 = this.command.getParameters();
      this.command = this.conn.prepareCommand(this.sqlStatement, this.fetchSize);
      ArrayList localArrayList2 = this.command.getParameters();
      int i = 0;
      for (int j = localArrayList1.size(); i < j; i++)
      {
        ParameterInterface localParameterInterface1 = (ParameterInterface)localArrayList1.get(i);
        Value localValue = localParameterInterface1.getParamValue();
        if (localValue != null)
        {
          ParameterInterface localParameterInterface2 = (ParameterInterface)localArrayList2.get(i);
          localParameterInterface2.setValue(localValue, false);
        }
      }
      return true;
    }
    return false;
  }
}
