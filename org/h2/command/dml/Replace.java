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

public class Replace
  extends Prepared
{
  private Table table;
  private Column[] columns;
  private Column[] keys;
  private final ArrayList<Expression[]> list = New.arrayList();
  private Query query;
  private Prepared update;
  
  public Replace(Session paramSession)
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
        replace(localRow);
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
        replace((Row)localObject1);
      }
      localLocalResult.close();
      this.table.fire(this.session, 3, false);
    }
    return i;
  }
  
  private void replace(Row paramRow)
  {
    int i = update(paramRow);
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
          Index localIndex = (Index)localDbException.getSource();
          if (localIndex != null)
          {
            Column[] arrayOfColumn = localIndex.getColumns();
            int j = 0;
            if (arrayOfColumn.length <= this.keys.length) {
              for (int k = 0; k < arrayOfColumn.length; k++) {
                if (arrayOfColumn[k] != this.keys[k])
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
  
  private int update(Row paramRow)
  {
    if (this.update == null) {
      return 0;
    }
    ArrayList localArrayList = this.update.getParameters();
    Column localColumn;
    Value localValue;
    Parameter localParameter;
    for (int i = 0; i < this.columns.length; i++)
    {
      localColumn = this.columns[i];
      localValue = paramRow.getValue(localColumn.getColumnId());
      localParameter = (Parameter)localArrayList.get(i);
      localParameter.setValue(localValue);
    }
    for (i = 0; i < this.keys.length; i++)
    {
      localColumn = this.keys[i];
      localValue = paramRow.getValue(localColumn.getColumnId());
      if (localValue == null) {
        throw DbException.get(90081, localColumn.getSQL());
      }
      localParameter = (Parameter)localArrayList.get(this.columns.length + i);
      localParameter.setValue(localValue);
    }
    return this.update.update();
  }
  
  public String getPlanSQL()
  {
    StatementBuilder localStatementBuilder = new StatementBuilder("REPLACE INTO ");
    localStatementBuilder.append(this.table.getSQL()).append('(');
    Object localObject1;
    for (localObject1 : this.columns)
    {
      localStatementBuilder.appendExceptFirst(", ");
      localStatementBuilder.append(((Column)localObject1).getSQL());
    }
    localStatementBuilder.append(')');
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
    int j;
    Expression localExpression;
    if (this.list.size() > 0)
    {
      for (localObject1 = this.list.iterator(); ((Iterator)localObject1).hasNext();)
      {
        Expression[] arrayOfExpression = (Expression[])((Iterator)localObject1).next();
        if (arrayOfExpression.length != this.columns.length) {
          throw DbException.get(21002);
        }
        for (j = 0; j < arrayOfExpression.length; j++)
        {
          localExpression = arrayOfExpression[j];
          if (localExpression != null) {
            arrayOfExpression[j] = localExpression.optimize(this.session);
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
    for (localExpression : this.keys)
    {
      int m = 0;
      for (Column localColumn : this.columns) {
        if (localColumn.getColumnId() == localExpression.getColumnId())
        {
          m = 1;
          break;
        }
      }
      if (m == 0) {
        return;
      }
    }
    Object localObject1 = new StatementBuilder("UPDATE ");
    ((StatementBuilder)localObject1).append(this.table.getSQL()).append(" SET ");
    Object localObject3;
    for (localObject3 : this.columns)
    {
      ((StatementBuilder)localObject1).appendExceptFirst(", ");
      ((StatementBuilder)localObject1).append(((Column)localObject3).getSQL()).append("=?");
    }
    ((StatementBuilder)localObject1).append(" WHERE ");
    ((StatementBuilder)localObject1).resetCount();
    for (localObject3 : this.keys)
    {
      ((StatementBuilder)localObject1).appendExceptFirst(" AND ");
      ((StatementBuilder)localObject1).append(((Column)localObject3).getSQL()).append("=?");
    }
    ??? = ((StatementBuilder)localObject1).toString();
    this.update = this.session.prepare((String)???);
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
    return 63;
  }
  
  public boolean isCacheable()
  {
    return true;
  }
}
