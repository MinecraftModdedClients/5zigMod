package org.h2.tools;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.h2.api.Trigger;

public abstract class TriggerAdapter
  implements Trigger
{
  protected String schemaName;
  protected String triggerName;
  protected String tableName;
  protected boolean before;
  protected int type;
  private SimpleResultSet oldResultSet;
  private SimpleResultSet newResultSet;
  private TriggerRowSource oldSource;
  private TriggerRowSource newSource;
  
  public void init(Connection paramConnection, String paramString1, String paramString2, String paramString3, boolean paramBoolean, int paramInt)
    throws SQLException
  {
    ResultSet localResultSet = paramConnection.getMetaData().getColumns(null, paramString1, paramString3, null);
    
    this.oldSource = new TriggerRowSource();
    this.newSource = new TriggerRowSource();
    this.oldResultSet = new SimpleResultSet(this.oldSource);
    this.newResultSet = new SimpleResultSet(this.newSource);
    while (localResultSet.next())
    {
      String str = localResultSet.getString("COLUMN_NAME");
      int i = localResultSet.getInt("DATA_TYPE");
      int j = localResultSet.getInt("COLUMN_SIZE");
      int k = localResultSet.getInt("DECIMAL_DIGITS");
      this.oldResultSet.addColumn(str, i, j, k);
      this.newResultSet.addColumn(str, i, j, k);
    }
    this.schemaName = paramString1;
    this.triggerName = paramString2;
    this.tableName = paramString3;
    this.before = paramBoolean;
    this.type = paramInt;
  }
  
  static class TriggerRowSource
    implements SimpleRowSource
  {
    private Object[] row;
    
    void setRow(Object[] paramArrayOfObject)
    {
      this.row = paramArrayOfObject;
    }
    
    public Object[] readRow()
    {
      return this.row;
    }
    
    public void close() {}
    
    public void reset() {}
  }
  
  public void fire(Connection paramConnection, Object[] paramArrayOfObject1, Object[] paramArrayOfObject2)
    throws SQLException
  {
    fire(paramConnection, wrap(this.oldResultSet, this.oldSource, paramArrayOfObject1), wrap(this.newResultSet, this.newSource, paramArrayOfObject2));
  }
  
  public abstract void fire(Connection paramConnection, ResultSet paramResultSet1, ResultSet paramResultSet2)
    throws SQLException;
  
  private static SimpleResultSet wrap(SimpleResultSet paramSimpleResultSet, TriggerRowSource paramTriggerRowSource, Object[] paramArrayOfObject)
    throws SQLException
  {
    if (paramArrayOfObject == null) {
      return null;
    }
    paramTriggerRowSource.setRow(paramArrayOfObject);
    paramSimpleResultSet.next();
    return paramSimpleResultSet;
  }
  
  public void remove()
    throws SQLException
  {}
  
  public void close()
    throws SQLException
  {}
}
