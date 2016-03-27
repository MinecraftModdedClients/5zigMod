package org.h2.engine;

import java.sql.Connection;
import java.sql.SQLException;
import org.h2.api.Aggregate;
import org.h2.api.AggregateFunction;
import org.h2.command.Parser;
import org.h2.message.DbException;
import org.h2.table.Table;
import org.h2.util.JdbcUtils;
import org.h2.value.DataType;

public class UserAggregate
  extends DbObjectBase
{
  private String className;
  private Class<?> javaClass;
  
  public UserAggregate(Database paramDatabase, int paramInt, String paramString1, String paramString2, boolean paramBoolean)
  {
    initDbObjectBase(paramDatabase, paramInt, paramString1, "function");
    this.className = paramString2;
    if (!paramBoolean) {
      getInstance();
    }
  }
  
  public Aggregate getInstance()
  {
    if (this.javaClass == null) {
      this.javaClass = JdbcUtils.loadUserClass(this.className);
    }
    try
    {
      Object localObject1 = this.javaClass.newInstance();
      Object localObject2;
      if ((localObject1 instanceof Aggregate)) {
        localObject2 = (Aggregate)localObject1;
      }
      return new AggregateWrapper((AggregateFunction)localObject1);
    }
    catch (Exception localException)
    {
      throw DbException.convert(localException);
    }
  }
  
  public String getCreateSQLForCopy(Table paramTable, String paramString)
  {
    throw DbException.throwInternalError();
  }
  
  public String getDropSQL()
  {
    return "DROP AGGREGATE IF EXISTS " + getSQL();
  }
  
  public String getCreateSQL()
  {
    return "CREATE FORCE AGGREGATE " + getSQL() + " FOR " + Parser.quoteIdentifier(this.className);
  }
  
  public int getType()
  {
    return 14;
  }
  
  public synchronized void removeChildrenAndResources(Session paramSession)
  {
    this.database.removeMeta(paramSession, getId());
    this.className = null;
    this.javaClass = null;
    invalidate();
  }
  
  public void checkRename()
  {
    throw DbException.getUnsupportedException("AGGREGATE");
  }
  
  public String getJavaClassName()
  {
    return this.className;
  }
  
  private static class AggregateWrapper
    implements Aggregate
  {
    private final AggregateFunction aggregateFunction;
    
    AggregateWrapper(AggregateFunction paramAggregateFunction)
    {
      this.aggregateFunction = paramAggregateFunction;
    }
    
    public void init(Connection paramConnection)
      throws SQLException
    {
      this.aggregateFunction.init(paramConnection);
    }
    
    public int getInternalType(int[] paramArrayOfInt)
      throws SQLException
    {
      int[] arrayOfInt = new int[paramArrayOfInt.length];
      for (int i = 0; i < paramArrayOfInt.length; i++) {
        arrayOfInt[i] = DataType.convertTypeToSQLType(paramArrayOfInt[i]);
      }
      return DataType.convertSQLTypeToValueType(this.aggregateFunction.getType(arrayOfInt));
    }
    
    public void add(Object paramObject)
      throws SQLException
    {
      this.aggregateFunction.add(paramObject);
    }
    
    public Object getResult()
      throws SQLException
    {
      return this.aggregateFunction.getResult();
    }
  }
}
