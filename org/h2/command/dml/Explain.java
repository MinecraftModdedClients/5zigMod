package org.h2.command.dml;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import org.h2.command.Prepared;
import org.h2.engine.Database;
import org.h2.engine.Session;
import org.h2.expression.Expression;
import org.h2.expression.ExpressionColumn;
import org.h2.mvstore.db.MVTableEngine.Store;
import org.h2.result.LocalResult;
import org.h2.result.ResultInterface;
import org.h2.store.PageStore;
import org.h2.table.Column;
import org.h2.value.Value;
import org.h2.value.ValueString;

public class Explain
  extends Prepared
{
  private Prepared command;
  private LocalResult result;
  private boolean executeCommand;
  
  public Explain(Session paramSession)
  {
    super(paramSession);
  }
  
  public void setCommand(Prepared paramPrepared)
  {
    this.command = paramPrepared;
  }
  
  public void prepare()
  {
    this.command.prepare();
  }
  
  public void setExecuteCommand(boolean paramBoolean)
  {
    this.executeCommand = paramBoolean;
  }
  
  public ResultInterface queryMeta()
  {
    return query(-1);
  }
  
  public ResultInterface query(int paramInt)
  {
    Column localColumn = new Column("PLAN", 13);
    Database localDatabase = this.session.getDatabase();
    ExpressionColumn localExpressionColumn = new ExpressionColumn(localDatabase, localColumn);
    Expression[] arrayOfExpression = { localExpressionColumn };
    this.result = new LocalResult(this.session, arrayOfExpression, 1);
    if (paramInt >= 0)
    {
      String str;
      if (this.executeCommand)
      {
        PageStore localPageStore = null;
        MVTableEngine.Store localStore = null;
        if (localDatabase.isPersistent())
        {
          localPageStore = localDatabase.getPageStore();
          if (localPageStore != null) {
            localPageStore.statisticsStart();
          }
          localStore = localDatabase.getMvStore();
          if (localStore != null) {
            localStore.statisticsStart();
          }
        }
        if (this.command.isQuery()) {
          this.command.query(paramInt);
        } else {
          this.command.update();
        }
        str = this.command.getPlanSQL();
        Object localObject1 = null;
        if (localPageStore != null) {
          localObject1 = localPageStore.statisticsEnd();
        } else if (localStore != null) {
          localObject1 = localStore.statisticsEnd();
        }
        if (localObject1 != null)
        {
          int i = 0;
          for (Object localObject2 = ((Map)localObject1).entrySet().iterator(); ((Iterator)localObject2).hasNext();)
          {
            localObject3 = (Map.Entry)((Iterator)localObject2).next();
            i += ((Integer)((Map.Entry)localObject3).getValue()).intValue();
          }
          Object localObject3;
          if (i > 0)
          {
            localObject1 = new TreeMap((Map)localObject1);
            localObject2 = new StringBuilder();
            if (((Map)localObject1).size() > 1) {
              ((StringBuilder)localObject2).append("total: ").append(i).append('\n');
            }
            for (localObject3 = ((Map)localObject1).entrySet().iterator(); ((Iterator)localObject3).hasNext();)
            {
              Map.Entry localEntry = (Map.Entry)((Iterator)localObject3).next();
              int j = ((Integer)localEntry.getValue()).intValue();
              int k = (int)(100L * j / i);
              ((StringBuilder)localObject2).append((String)localEntry.getKey()).append(": ").append(j);
              if (((Map)localObject1).size() > 1) {
                ((StringBuilder)localObject2).append(" (").append(k).append("%)");
              }
              ((StringBuilder)localObject2).append('\n');
            }
            str = str + "\n/*\n" + ((StringBuilder)localObject2).toString() + "*/";
          }
        }
      }
      else
      {
        str = this.command.getPlanSQL();
      }
      add(str);
    }
    this.result.done();
    return this.result;
  }
  
  private void add(String paramString)
  {
    Value[] arrayOfValue = { ValueString.get(paramString) };
    this.result.addRow(arrayOfValue);
  }
  
  public boolean isQuery()
  {
    return true;
  }
  
  public boolean isTransactional()
  {
    return true;
  }
  
  public boolean isReadOnly()
  {
    return this.command.isReadOnly();
  }
  
  public int getType()
  {
    return 60;
  }
}
