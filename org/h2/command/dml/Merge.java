package org.h2.command.dml;

import java.util.ArrayList;
import java.util.Iterator;
import org.h2.command.Command;
import org.h2.command.Prepared;
import org.h2.engine.Session;
import org.h2.engine.User;
import org.h2.expression.Expression;
import org.h2.expression.Parameter;
import org.h2.index.Index;
import org.h2.message.DbException;
import org.h2.result.LocalResult;
import org.h2.result.ResultInterface;
import org.h2.result.Row;
import org.h2.table.Column;
import org.h2.table.Table;
import org.h2.util.New;
import org.h2.util.StatementBuilder;
import org.h2.value.Value;

public class Merge
  extends Prepared
{
  private Table table;
  private Column[] columns;
  private Column[] keys;
  private final ArrayList<Expression[]> list = New.arrayList();
  private Query query;
  private Prepared update;
  
  public Merge(Session paramSession)
  {
    super(paramSession);
  }
  
  public void setCommand(Command paramCommand)
  {
    super.setCommand(paramCommand);
    if (this.query != null) {
      this.query.setCommand(paramCommand);
    }
  }
  
  public void setTable(Table paramTable)
  {
    this.table = paramTable;
  }
  
  public void setColumns(Column[] paramArrayOfColumn)
  {
    this.columns = paramArrayOfColumn;
  }
  
  public void setKeys(Column[] paramArrayOfColumn)
  {
    this.keys = paramArrayOfColumn;
  }
  
  public void setQuery(Query paramQuery)
  {
    this.query = paramQuery;
  }
  
  public void addRow(Expression[] paramArrayOfExpression)
  {
    this.list.add(paramArrayOfExpression);
  }
  
  public int update()
  {
    this.session.getUser().checkRight(this.table, 4);
    this.session.getUser().checkRight(this.table, 8);
    setCurrentRowNumber(0);
    int i;
    Object localObject1;
    int i1;
    Object localObject2;
    if (this.list.size() > 0)
    {
      i = 0;
      int j = 0;
      for (int k = this.list.size(); j < k; j++)
      {
        setCurrentRowNumber(j + 1);
        localObject1 = (Expression[])this.list.get(j);
        Row localRow = this.table.getTemplateRow();
        int n = 0;
        for (i1 = this.columns.length; n < i1; n++)
        {
          localObject2 = this.columns[n];
          int i2 = ((Column)localObject2).getColumnId();
          Object localObject3 = localObject1[n];
          if (localObject3 != null) {
            try
            {
              Value localValue = ((Column)localObject2).convert(((Expression)localObject3).getValue(this.session));
              localRow.setValue(i2, localValue);
            }
            catch (DbException localDbException2)
            {
              throw setRow(localDbException2, i, getSQL((Expression[])localObject1));
            }
          }
        }
        merge(localRow);
        i++;
      }
    }
    else
    {
      LocalResult localLocalResult = this.query.query(0);
      i = 0;
      this.table.fire(this.session, 3, true);
      this.table.lock(this.session, true, false);
      while (localLocalResult.next())
      {
        i++;
        Value[] arrayOfValue = localLocalResult.currentRow();
        localObject1 = this.table.getTemplateRow();
        setCurrentRowNumber(i);
        for (int m = 0; m < this.columns.length; m++)
        {
          Column localColumn = this.columns[m];
          i1 = localColumn.getColumnId();
          try
          {
            localObject2 = localColumn.convert(arrayOfValue[m]);
            ((Row)localObject1).setValue(i1, (Value)localObject2);
          }
          catch (DbException localDbException1)
          {
            throw setRow(localDbException1, i, getSQL(arrayOfValue));
          }
        }
        merge((Row)localObject1);
      }
      localLocalResult.close();
      this.table.fire(this.session, 3, false);
    }
    return i;
  }
  
  private void merge(Row paramRow)
  {
    ArrayList localArrayList = this.update.getParameters();
    Column localColumn;
    Object localObject1;
    Object localObject2;
    for (int i = 0; i < this.columns.length; i++)
    {
      localColumn = this.columns[i];
      localObject1 = paramRow.getValue(localColumn.getColumnId());
      localObject2 = (Parameter)localArrayList.get(i);
      ((Parameter)localObject2).setValue((Value)localObject1);
    }
    for (i = 0; i < this.keys.length; i++)
    {
      localColumn = this.keys[i];
      localObject1 = paramRow.getValue(localColumn.getColumnId());
      if (localObject1 == null) {
        throw DbException.get(90081, localColumn.getSQL());
      }
      localObject2 = (Parameter)localArrayList.get(this.columns.length + i);
      ((Parameter)localObject2).setValue((Value)localObject1);
    }
    i = this.update.update();
    if (i == 0) {
      try
      {
        this.table.validateConvertUpdateSequence(this.session, paramRow);
        boolean bool = this.table.fireBeforeRow(this.session, null, paramRow);
        if (!bool)
        {
          this.table.lock(this.session, true, false);
          this.table.addRow(this.session, paramRow);
          this.session.log(this.table, (short)0, paramRow);
          this.table.fireAfterRow(this.session, null, paramRow, false);
        }
      }
      catch (DbException localDbException)
      {
        if (localDbException.getErrorCode() == 23505)
        {
          localObject1 = (Index)localDbException.getSource();
          if (localObject1 != null)
          {
            localObject2 = ((Index)localObject1).getColumns();
            int j = 0;
            if (localObject2.length <= this.keys.length) {
              for (int k = 0; k < localObject2.length; k++) {
                if (localObject2[k] != this.keys[k])
                {
                  j = 0;
                  break;
                }
              }
            }
            if (j != 0) {
              throw DbException.get(90131, this.table.getName());
            }
          }
        }
        throw localDbException;
      }
    } else if (i != 1) {
      throw DbException.get(23505, this.table.getSQL());
    }
  }
  
  public String getPlanSQL()
  {
    StatementBuilder localStatementBuilder = new StatementBuilder("MERGE INTO ");
    localStatementBuilder.append(this.table.getSQL()).append('(');
    Object localObject1;
    for (localObject1 : this.columns)
    {
      localStatementBuilder.appendExceptFirst(", ");
      localStatementBuilder.append(((Column)localObject1).getSQL());
    }
    localStatementBuilder.append(')');
    if (this.keys != null)
    {
      localStatementBuilder.append(" KEY(");
      localStatementBuilder.resetCount();
      for (localObject1 : this.keys)
      {
        localStatementBuilder.appendExceptFirst(", ");
        localStatementBuilder.append(((Column)localObject1).getSQL());
      }
      localStatementBuilder.append(')');
    }
    localStatementBuilder.append('\n');
    int i;
    if (this.list.size() > 0)
    {
      localStatementBuilder.append("VALUES ");
      i = 0;
      for (Expression[] arrayOfExpression : this.list)
      {
        if (i++ > 0) {
          localStatementBuilder.append(", ");
        }
        localStatementBuilder.append('(');
        localStatementBuilder.resetCount();
        for (Object localObject2 : arrayOfExpression)
        {
          localStatementBuilder.appendExceptFirst(", ");
          if (localObject2 == null) {
            localStatementBuilder.append("DEFAULT");
          } else {
            localStatementBuilder.append(((Expression)localObject2).getSQL());
          }
        }
        localStatementBuilder.append(')');
      }
    }
    else
    {
      localStatementBuilder.append(this.query.getPlanSQL());
    }
    return localStatementBuilder.toString();
  }
  
  public void prepare()
  {
    if (this.columns == null) {
      if ((this.list.size() > 0) && (((Expression[])this.list.get(0)).length == 0)) {
        this.columns = new Column[0];
      } else {
        this.columns = this.table.getColumns();
      }
    }
    int i;
    if (this.list.size() > 0)
    {
      for (localObject1 = this.list.iterator(); ((Iterator)localObject1).hasNext();)
      {
        localObject2 = (Expression[])((Iterator)localObject1).next();
        if (localObject2.length != this.columns.length) {
          throw DbException.get(21002);
        }
        for (i = 0; i < localObject2.length; i++)
        {
          Object localObject3 = localObject2[i];
          if (localObject3 != null) {
            localObject2[i] = ((Expression)localObject3).optimize(this.session);
          }
        }
      }
    }
    else
    {
      this.query.prepare();
      if (this.query.getColumnCount() != this.columns.length) {
        throw DbException.get(21002);
      }
    }
    if (this.keys == null)
    {
      localObject1 = this.table.getPrimaryKey();
      if (localObject1 == null) {
        throw DbException.get(90057, "PRIMARY KEY");
      }
      this.keys = ((Index)localObject1).getColumns();
    }
    Object localObject1 = new StatementBuilder("UPDATE ");
    ((StatementBuilder)localObject1).append(this.table.getSQL()).append(" SET ");
    Object localObject4;
    for (localObject4 : this.columns)
    {
      ((StatementBuilder)localObject1).appendExceptFirst(", ");
      ((StatementBuilder)localObject1).append(((Column)localObject4).getSQL()).append("=?");
    }
    ((StatementBuilder)localObject1).append(" WHERE ");
    ((StatementBuilder)localObject1).resetCount();
    for (localObject4 : this.keys)
    {
      ((StatementBuilder)localObject1).appendExceptFirst(" AND ");
      ((StatementBuilder)localObject1).append(((Column)localObject4).getSQL()).append("=?");
    }
    Object localObject2 = ((StatementBuilder)localObject1).toString();
    this.update = this.session.prepare((String)localObject2);
  }
  
  public boolean isTransactional()
  {
    return true;
  }
  
  public ResultInterface queryMeta()
  {
    return null;
  }
  
  public int getType()
  {
    return 62;
  }
  
  public boolean isCacheable()
  {
    return true;
  }
}
