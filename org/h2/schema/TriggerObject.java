package org.h2.schema;

import java.sql.SQLException;
import org.h2.api.Trigger;
import org.h2.command.Parser;
import org.h2.engine.Database;
import org.h2.engine.Session;
import org.h2.jdbc.JdbcConnection;
import org.h2.message.DbException;
import org.h2.result.Row;
import org.h2.table.Table;
import org.h2.util.JdbcUtils;
import org.h2.util.StatementBuilder;
import org.h2.value.DataType;
import org.h2.value.Value;

public class TriggerObject
  extends SchemaObjectBase
{
  public static final int DEFAULT_QUEUE_SIZE = 1024;
  private boolean insteadOf;
  private boolean before;
  private int typeMask;
  private boolean rowBased;
  private boolean onRollback;
  private int queueSize = 1024;
  private boolean noWait;
  private Table table;
  private String triggerClassName;
  private Trigger triggerCallback;
  
  public TriggerObject(Schema paramSchema, int paramInt, String paramString, Table paramTable)
  {
    initSchemaObjectBase(paramSchema, paramInt, paramString, "trigger");
    this.table = paramTable;
    setTemporary(paramTable.isTemporary());
  }
  
  public void setBefore(boolean paramBoolean)
  {
    this.before = paramBoolean;
  }
  
  public void setInsteadOf(boolean paramBoolean)
  {
    this.insteadOf = paramBoolean;
  }
  
  private synchronized void load()
  {
    if (this.triggerCallback != null) {
      return;
    }
    try
    {
      Session localSession = this.database.getSystemSession();
      JdbcConnection localJdbcConnection = localSession.createConnection(false);
      Object localObject = JdbcUtils.loadUserClass(this.triggerClassName).newInstance();
      this.triggerCallback = ((Trigger)localObject);
      this.triggerCallback.init(localJdbcConnection, getSchema().getName(), getName(), this.table.getName(), this.before, this.typeMask);
    }
    catch (Throwable localThrowable)
    {
      this.triggerCallback = null;
      throw DbException.get(90043, localThrowable, new String[] { getName(), this.triggerClassName, localThrowable.toString() });
    }
  }
  
  public void setTriggerClassName(String paramString, boolean paramBoolean)
  {
    this.triggerClassName = paramString;
    try
    {
      load();
    }
    catch (DbException localDbException)
    {
      if (!paramBoolean) {
        throw localDbException;
      }
    }
  }
  
  public void fire(Session paramSession, int paramInt, boolean paramBoolean)
  {
    if ((this.rowBased) || (this.before != paramBoolean) || ((this.typeMask & paramInt) == 0)) {
      return;
    }
    load();
    JdbcConnection localJdbcConnection = paramSession.createConnection(false);
    boolean bool = false;
    if (paramInt != 8) {
      bool = paramSession.setCommitOrRollbackDisabled(true);
    }
    Value localValue = paramSession.getLastScopeIdentity();
    try
    {
      this.triggerCallback.fire(localJdbcConnection, null, null);
    }
    catch (Throwable localThrowable)
    {
      throw DbException.get(90044, localThrowable, new String[] { getName(), this.triggerClassName, localThrowable.toString() });
    }
    finally
    {
      paramSession.setLastScopeIdentity(localValue);
      if (paramInt != 8) {
        paramSession.setCommitOrRollbackDisabled(bool);
      }
    }
  }
  
  private static Object[] convertToObjectList(Row paramRow)
  {
    if (paramRow == null) {
      return null;
    }
    int i = paramRow.getColumnCount();
    Object[] arrayOfObject = new Object[i];
    for (int j = 0; j < i; j++) {
      arrayOfObject[j] = paramRow.getValue(j).getObject();
    }
    return arrayOfObject;
  }
  
  public boolean fireRow(Session paramSession, Row paramRow1, Row paramRow2, boolean paramBoolean1, boolean paramBoolean2)
  {
    if ((!this.rowBased) || (this.before != paramBoolean1)) {
      return false;
    }
    if ((paramBoolean2) && (!this.onRollback)) {
      return false;
    }
    load();
    
    int i = 0;
    if (((this.typeMask & 0x1) != 0) && 
      (paramRow1 == null) && (paramRow2 != null)) {
      i = 1;
    }
    if (((this.typeMask & 0x2) != 0) && 
      (paramRow1 != null) && (paramRow2 != null)) {
      i = 1;
    }
    if (((this.typeMask & 0x4) != 0) && 
      (paramRow1 != null) && (paramRow2 == null)) {
      i = 1;
    }
    if (i == 0) {
      return false;
    }
    Object[] arrayOfObject1 = convertToObjectList(paramRow1);
    Object[] arrayOfObject2 = convertToObjectList(paramRow2);
    Object[] arrayOfObject3;
    if ((this.before) && (arrayOfObject2 != null))
    {
      arrayOfObject3 = new Object[arrayOfObject2.length];
      System.arraycopy(arrayOfObject2, 0, arrayOfObject3, 0, arrayOfObject2.length);
    }
    else
    {
      arrayOfObject3 = null;
    }
    JdbcConnection localJdbcConnection = paramSession.createConnection(false);
    boolean bool1 = paramSession.getAutoCommit();
    boolean bool2 = paramSession.setCommitOrRollbackDisabled(true);
    Value localValue1 = paramSession.getLastScopeIdentity();
    try
    {
      paramSession.setAutoCommit(false);
      this.triggerCallback.fire(localJdbcConnection, arrayOfObject1, arrayOfObject2);
      if (arrayOfObject3 != null) {
        for (int j = 0; j < arrayOfObject2.length; j++)
        {
          Object localObject1 = arrayOfObject2[j];
          if (localObject1 != arrayOfObject3[j])
          {
            Value localValue2 = DataType.convertToValue(paramSession, localObject1, -1);
            paramRow2.setValue(j, localValue2);
          }
        }
      }
    }
    catch (Exception localException)
    {
      if (!this.onRollback) {
        throw DbException.convert(localException);
      }
    }
    finally
    {
      paramSession.setLastScopeIdentity(localValue1);
      paramSession.setCommitOrRollbackDisabled(bool2);
      paramSession.setAutoCommit(bool1);
    }
    return this.insteadOf;
  }
  
  public void setTypeMask(int paramInt)
  {
    this.typeMask = paramInt;
  }
  
  public void setRowBased(boolean paramBoolean)
  {
    this.rowBased = paramBoolean;
  }
  
  public void setQueueSize(int paramInt)
  {
    this.queueSize = paramInt;
  }
  
  public int getQueueSize()
  {
    return this.queueSize;
  }
  
  public void setNoWait(boolean paramBoolean)
  {
    this.noWait = paramBoolean;
  }
  
  public boolean isNoWait()
  {
    return this.noWait;
  }
  
  public void setOnRollback(boolean paramBoolean)
  {
    this.onRollback = paramBoolean;
  }
  
  public String getDropSQL()
  {
    return null;
  }
  
  public String getCreateSQLForCopy(Table paramTable, String paramString)
  {
    StringBuilder localStringBuilder = new StringBuilder("CREATE FORCE TRIGGER ");
    localStringBuilder.append(paramString);
    if (this.insteadOf) {
      localStringBuilder.append(" INSTEAD OF ");
    } else if (this.before) {
      localStringBuilder.append(" BEFORE ");
    } else {
      localStringBuilder.append(" AFTER ");
    }
    localStringBuilder.append(getTypeNameList());
    localStringBuilder.append(" ON ").append(paramTable.getSQL());
    if (this.rowBased) {
      localStringBuilder.append(" FOR EACH ROW");
    }
    if (this.noWait) {
      localStringBuilder.append(" NOWAIT");
    } else {
      localStringBuilder.append(" QUEUE ").append(this.queueSize);
    }
    localStringBuilder.append(" CALL ").append(Parser.quoteIdentifier(this.triggerClassName));
    return localStringBuilder.toString();
  }
  
  public String getTypeNameList()
  {
    StatementBuilder localStatementBuilder = new StatementBuilder();
    if ((this.typeMask & 0x1) != 0)
    {
      localStatementBuilder.appendExceptFirst(", ");
      localStatementBuilder.append("INSERT");
    }
    if ((this.typeMask & 0x2) != 0)
    {
      localStatementBuilder.appendExceptFirst(", ");
      localStatementBuilder.append("UPDATE");
    }
    if ((this.typeMask & 0x4) != 0)
    {
      localStatementBuilder.appendExceptFirst(", ");
      localStatementBuilder.append("DELETE");
    }
    if ((this.typeMask & 0x8) != 0)
    {
      localStatementBuilder.appendExceptFirst(", ");
      localStatementBuilder.append("SELECT");
    }
    if (this.onRollback)
    {
      localStatementBuilder.appendExceptFirst(", ");
      localStatementBuilder.append("ROLLBACK");
    }
    return localStatementBuilder.toString();
  }
  
  public String getCreateSQL()
  {
    return getCreateSQLForCopy(this.table, getSQL());
  }
  
  public int getType()
  {
    return 4;
  }
  
  public void removeChildrenAndResources(Session paramSession)
  {
    this.table.removeTrigger(this);
    this.database.removeMeta(paramSession, getId());
    if (this.triggerCallback != null) {
      try
      {
        this.triggerCallback.remove();
      }
      catch (SQLException localSQLException)
      {
        throw DbException.convert(localSQLException);
      }
    }
    this.table = null;
    this.triggerClassName = null;
    this.triggerCallback = null;
    invalidate();
  }
  
  public void checkRename() {}
  
  public Table getTable()
  {
    return this.table;
  }
  
  public boolean isBefore()
  {
    return this.before;
  }
  
  public String getTriggerClassName()
  {
    return this.triggerClassName;
  }
  
  public void close()
    throws SQLException
  {
    if (this.triggerCallback != null) {
      this.triggerCallback.close();
    }
  }
  
  public boolean isSelectTrigger()
  {
    return (this.typeMask & 0x8) != 0;
  }
}
