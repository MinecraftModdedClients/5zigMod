package org.h2.command.ddl;

import java.util.ArrayList;
import org.h2.command.Prepared;
import org.h2.engine.Database;
import org.h2.engine.DbSettings;
import org.h2.engine.Session;
import org.h2.engine.User;
import org.h2.expression.Parameter;
import org.h2.result.ResultInterface;
import org.h2.table.Column;
import org.h2.table.Table;
import org.h2.util.StatementBuilder;
import org.h2.value.Value;
import org.h2.value.ValueInt;
import org.h2.value.ValueNull;

public class Analyze
  extends DefineCommand
{
  private int sampleRows;
  
  public Analyze(Session paramSession)
  {
    super(paramSession);
    this.sampleRows = paramSession.getDatabase().getSettings().analyzeSample;
  }
  
  public int update()
  {
    this.session.commit(true);
    this.session.getUser().checkAdmin();
    Database localDatabase = this.session.getDatabase();
    for (Table localTable : localDatabase.getAllTablesAndViews(false)) {
      analyzeTable(this.session, localTable, this.sampleRows, true);
    }
    return 0;
  }
  
  public static void analyzeTable(Session paramSession, Table paramTable, int paramInt, boolean paramBoolean)
  {
    if ((!paramTable.getTableType().equals("TABLE")) || (paramTable.isHidden()) || (paramSession == null)) {
      return;
    }
    if (!paramBoolean)
    {
      if (paramSession.getDatabase().isSysTableLocked()) {
        return;
      }
      if (paramTable.hasSelectTrigger()) {
        return;
      }
    }
    if ((paramTable.isTemporary()) && (!paramTable.isGlobalTemporary()) && (paramSession.findLocalTempTable(paramTable.getName()) == null)) {
      return;
    }
    if ((paramTable.isLockedExclusively()) && (!paramTable.isLockedExclusivelyBy(paramSession))) {
      return;
    }
    if (!paramSession.getUser().hasRight(paramTable, 1)) {
      return;
    }
    if (paramSession.getCancel() != 0L) {
      return;
    }
    Database localDatabase = paramSession.getDatabase();
    StatementBuilder localStatementBuilder = new StatementBuilder("SELECT ");
    Column[] arrayOfColumn = paramTable.getColumns();
    for (Object localObject3 : arrayOfColumn)
    {
      localStatementBuilder.appendExceptFirst(", ");
      int m = ((Column)localObject3).getType();
      if ((m == 15) || (m == 16)) {
        localStatementBuilder.append("MAX(NULL)");
      } else {
        localStatementBuilder.append("SELECTIVITY(").append(((Column)localObject3).getSQL()).append(')');
      }
    }
    localStatementBuilder.append(" FROM ").append(paramTable.getSQL());
    if (paramInt > 0) {
      localStatementBuilder.append(" LIMIT ? SAMPLE_SIZE ? ");
    }
    ??? = localStatementBuilder.toString();
    Prepared localPrepared = paramSession.prepare((String)???);
    if (paramInt > 0)
    {
      localObject2 = localPrepared.getParameters();
      ((Parameter)((ArrayList)localObject2).get(0)).setValue(ValueInt.get(1));
      ((Parameter)((ArrayList)localObject2).get(1)).setValue(ValueInt.get(paramInt));
    }
    Object localObject2 = localPrepared.query(0);
    ((ResultInterface)localObject2).next();
    for (int k = 0; k < arrayOfColumn.length; k++)
    {
      Value localValue = localObject2.currentRow()[k];
      if (localValue != ValueNull.INSTANCE)
      {
        int n = localValue.getInt();
        arrayOfColumn[k].setSelectivity(n);
      }
    }
    if (paramBoolean)
    {
      localDatabase.updateMeta(paramSession, paramTable);
    }
    else
    {
      Session localSession = localDatabase.getSystemSession();
      if (localSession != paramSession) {
        synchronized (localSession)
        {
          synchronized (localDatabase)
          {
            localDatabase.updateMeta(localSession, paramTable);
            localSession.commit(true);
          }
        }
      }
    }
  }
  
  public void setTop(int paramInt)
  {
    this.sampleRows = paramInt;
  }
  
  public int getType()
  {
    return 21;
  }
}
