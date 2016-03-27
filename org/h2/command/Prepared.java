package org.h2.command;

import java.util.ArrayList;
import org.h2.engine.Database;
import org.h2.engine.DbSettings;
import org.h2.engine.QueryStatisticsData;
import org.h2.engine.Session;
import org.h2.expression.Expression;
import org.h2.expression.Parameter;
import org.h2.message.DbException;
import org.h2.message.Trace;
import org.h2.result.ResultInterface;
import org.h2.util.StatementBuilder;
import org.h2.value.Value;

public abstract class Prepared
{
  protected Session session;
  protected String sqlStatement;
  protected boolean create = true;
  protected ArrayList<Parameter> parameters;
  protected boolean prepareAlways;
  private long modificationMetaId;
  private Command command;
  private int objectId;
  private int currentRowNumber;
  private int rowScanCount;
  
  public Prepared(Session paramSession)
  {
    this.session = paramSession;
    this.modificationMetaId = paramSession.getDatabase().getModificationMetaId();
  }
  
  public abstract boolean isTransactional();
  
  public abstract ResultInterface queryMeta();
  
  public abstract int getType();
  
  public boolean isReadOnly()
  {
    return false;
  }
  
  public boolean needRecompile()
  {
    Database localDatabase = this.session.getDatabase();
    if (localDatabase == null) {
      throw DbException.get(90067, "database closed");
    }
    return (this.prepareAlways) || (this.modificationMetaId < localDatabase.getModificationMetaId()) || (localDatabase.getSettings().recompileAlways);
  }
  
  long getModificationMetaId()
  {
    return this.modificationMetaId;
  }
  
  void setModificationMetaId(long paramLong)
  {
    this.modificationMetaId = paramLong;
  }
  
  public void setParameterList(ArrayList<Parameter> paramArrayList)
  {
    this.parameters = paramArrayList;
  }
  
  public ArrayList<Parameter> getParameters()
  {
    return this.parameters;
  }
  
  protected void checkParameters()
  {
    if (this.parameters != null)
    {
      int i = 0;
      for (int j = this.parameters.size(); i < j; i++)
      {
        Parameter localParameter = (Parameter)this.parameters.get(i);
        localParameter.checkSet();
      }
    }
  }
  
  public void setCommand(Command paramCommand)
  {
    this.command = paramCommand;
  }
  
  public boolean isQuery()
  {
    return false;
  }
  
  public void prepare() {}
  
  public int update()
  {
    throw DbException.get(90001);
  }
  
  public ResultInterface query(int paramInt)
  {
    throw DbException.get(90002);
  }
  
  public void setSQL(String paramString)
  {
    this.sqlStatement = paramString;
  }
  
  public String getSQL()
  {
    return this.sqlStatement;
  }
  
  protected int getCurrentObjectId()
  {
    return this.objectId;
  }
  
  protected int getObjectId()
  {
    int i = this.objectId;
    if (i == 0) {
      i = this.session.getDatabase().allocateObjectId();
    } else {
      this.objectId = 0;
    }
    return i;
  }
  
  public String getPlanSQL()
  {
    return null;
  }
  
  public void checkCanceled()
  {
    this.session.checkCanceled();
    Command localCommand = this.command != null ? this.command : this.session.getCurrentCommand();
    if (localCommand != null) {
      localCommand.checkCanceled();
    }
  }
  
  public void setObjectId(int paramInt)
  {
    this.objectId = paramInt;
    this.create = false;
  }
  
  public void setSession(Session paramSession)
  {
    this.session = paramSession;
  }
  
  void trace(long paramLong, int paramInt)
  {
    long l;
    if ((this.session.getTrace().isInfoEnabled()) && (paramLong > 0L))
    {
      l = System.currentTimeMillis() - paramLong;
      String str = Trace.formatParams(this.parameters);
      this.session.getTrace().infoSQL(this.sqlStatement, str, paramInt, l);
    }
    if (this.session.getDatabase().getQueryStatistics())
    {
      l = System.currentTimeMillis() - paramLong;
      this.session.getDatabase().getQueryStatisticsData().update(toString(), l, paramInt);
    }
  }
  
  public void setPrepareAlways(boolean paramBoolean)
  {
    this.prepareAlways = paramBoolean;
  }
  
  protected void setCurrentRowNumber(int paramInt)
  {
    if ((++this.rowScanCount & 0x7F) == 0) {
      checkCanceled();
    }
    this.currentRowNumber = paramInt;
    setProgress();
  }
  
  public int getCurrentRowNumber()
  {
    return this.currentRowNumber;
  }
  
  private void setProgress()
  {
    if ((this.currentRowNumber & 0x7F) == 0) {
      this.session.getDatabase().setProgress(7, this.sqlStatement, this.currentRowNumber, 0);
    }
  }
  
  public String toString()
  {
    return this.sqlStatement;
  }
  
  protected static String getSQL(Value[] paramArrayOfValue)
  {
    StatementBuilder localStatementBuilder = new StatementBuilder();
    for (Value localValue : paramArrayOfValue)
    {
      localStatementBuilder.appendExceptFirst(", ");
      if (localValue != null) {
        localStatementBuilder.append(localValue.getSQL());
      }
    }
    return localStatementBuilder.toString();
  }
  
  protected static String getSQL(Expression[] paramArrayOfExpression)
  {
    StatementBuilder localStatementBuilder = new StatementBuilder();
    for (Expression localExpression : paramArrayOfExpression)
    {
      localStatementBuilder.appendExceptFirst(", ");
      if (localExpression != null) {
        localStatementBuilder.append(localExpression.getSQL());
      }
    }
    return localStatementBuilder.toString();
  }
  
  protected DbException setRow(DbException paramDbException, int paramInt, String paramString)
  {
    StringBuilder localStringBuilder = new StringBuilder();
    if (this.sqlStatement != null) {
      localStringBuilder.append(this.sqlStatement);
    }
    localStringBuilder.append(" -- ");
    if (paramInt > 0) {
      localStringBuilder.append("row #").append(paramInt + 1).append(' ');
    }
    localStringBuilder.append('(').append(paramString).append(')');
    return paramDbException.addSQL(localStringBuilder.toString());
  }
  
  public boolean isCacheable()
  {
    return false;
  }
}
