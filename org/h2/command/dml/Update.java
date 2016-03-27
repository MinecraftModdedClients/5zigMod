package org.h2.command.dml;

import java.util.ArrayList;
import java.util.HashMap;
import org.h2.command.Prepared;
import org.h2.engine.Session;
import org.h2.engine.User;
import org.h2.expression.Expression;
import org.h2.expression.Parameter;
import org.h2.expression.ValueExpression;
import org.h2.message.DbException;
import org.h2.result.ResultInterface;
import org.h2.result.Row;
import org.h2.result.RowList;
import org.h2.table.Column;
import org.h2.table.PlanItem;
import org.h2.table.Table;
import org.h2.table.TableFilter;
import org.h2.util.New;
import org.h2.util.StatementBuilder;
import org.h2.util.StringUtils;
import org.h2.value.Value;
import org.h2.value.ValueNull;

public class Update
  extends Prepared
{
  private Expression condition;
  private TableFilter tableFilter;
  private Expression limitExpr;
  private final ArrayList<Column> columns = New.arrayList();
  private final HashMap<Column, Expression> expressionMap = New.hashMap();
  
  public Update(Session paramSession)
  {
    super(paramSession);
  }
  
  public void setTableFilter(TableFilter paramTableFilter)
  {
    this.tableFilter = paramTableFilter;
  }
  
  public void setCondition(Expression paramExpression)
  {
    this.condition = paramExpression;
  }
  
  public void setAssignment(Column paramColumn, Expression paramExpression)
  {
    if (this.expressionMap.containsKey(paramColumn)) {
      throw DbException.get(42121, paramColumn.getName());
    }
    this.columns.add(paramColumn);
    this.expressionMap.put(paramColumn, paramExpression);
    if ((paramExpression instanceof Parameter))
    {
      Parameter localParameter = (Parameter)paramExpression;
      localParameter.setColumn(paramColumn);
    }
  }
  
  public int update()
  {
    this.tableFilter.startQuery(this.session);
    this.tableFilter.reset();
    RowList localRowList = new RowList(this.session);
    try
    {
      Table localTable = this.tableFilter.getTable();
      this.session.getUser().checkRight(localTable, 8);
      localTable.fire(this.session, 2, true);
      localTable.lock(this.session, true, false);
      int i = localTable.getColumns().length;
      
      setCurrentRowNumber(0);
      int j = 0;
      Column[] arrayOfColumn = localTable.getColumns();
      int k = -1;
      Object localObject1;
      if (this.limitExpr != null)
      {
        localObject1 = this.limitExpr.getValue(this.session);
        if (localObject1 != ValueNull.INSTANCE) {
          k = ((Value)localObject1).getInt();
        }
      }
      Row localRow;
      while (this.tableFilter.next())
      {
        setCurrentRowNumber(j + 1);
        if ((k >= 0) && (j >= k)) {
          break;
        }
        if ((this.condition == null) || (Boolean.TRUE.equals(this.condition.getBooleanValue(this.session))))
        {
          localObject1 = this.tableFilter.get();
          localRow = localTable.getTemplateRow();
          for (int m = 0; m < i; m++)
          {
            Expression localExpression = (Expression)this.expressionMap.get(arrayOfColumn[m]);
            Value localValue;
            if (localExpression == null)
            {
              localValue = ((Row)localObject1).getValue(m);
            }
            else
            {
              Column localColumn;
              if (localExpression == ValueExpression.getDefault())
              {
                localColumn = localTable.getColumn(m);
                localValue = localTable.getDefaultValue(this.session, localColumn);
              }
              else
              {
                localColumn = localTable.getColumn(m);
                localValue = localColumn.convert(localExpression.getValue(this.session));
              }
            }
            localRow.setValue(m, localValue);
          }
          localTable.validateConvertUpdateSequence(this.session, localRow);
          m = 0;
          boolean bool;
          if (localTable.fireRow()) {
            bool = localTable.fireBeforeRow(this.session, (Row)localObject1, localRow);
          }
          if (!bool)
          {
            localRowList.add((Row)localObject1);
            localRowList.add(localRow);
          }
          j++;
        }
      }
      localTable.updateRows(this, this.session, localRowList);
      if (localTable.fireRow())
      {
        localRowList.invalidateCache();
        for (localRowList.reset(); localRowList.hasNext();)
        {
          localObject1 = localRowList.next();
          localRow = localRowList.next();
          localTable.fireAfterRow(this.session, (Row)localObject1, localRow, false);
        }
      }
      localTable.fire(this.session, 2, false);
      return j;
    }
    finally
    {
      localRowList.close();
    }
  }
  
  public String getPlanSQL()
  {
    StatementBuilder localStatementBuilder = new StatementBuilder("UPDATE ");
    localStatementBuilder.append(this.tableFilter.getPlanSQL(false)).append("\nSET\n    ");
    int i = 0;
    for (int j = this.columns.size(); i < j; i++)
    {
      Column localColumn = (Column)this.columns.get(i);
      Expression localExpression = (Expression)this.expressionMap.get(localColumn);
      localStatementBuilder.appendExceptFirst(",\n    ");
      localStatementBuilder.append(localColumn.getName()).append(" = ").append(localExpression.getSQL());
    }
    if (this.condition != null) {
      localStatementBuilder.append("\nWHERE ").append(StringUtils.unEnclose(this.condition.getSQL()));
    }
    return localStatementBuilder.toString();
  }
  
  public void prepare()
  {
    if (this.condition != null)
    {
      this.condition.mapColumns(this.tableFilter, 0);
      this.condition = this.condition.optimize(this.session);
      this.condition.createIndexConditions(this.session, this.tableFilter);
    }
    int i = 0;
    for (int j = this.columns.size(); i < j; i++)
    {
      Column localColumn = (Column)this.columns.get(i);
      Expression localExpression = (Expression)this.expressionMap.get(localColumn);
      localExpression.mapColumns(this.tableFilter, 0);
      this.expressionMap.put(localColumn, localExpression.optimize(this.session));
    }
    PlanItem localPlanItem = this.tableFilter.getBestPlanItem(this.session, 1);
    this.tableFilter.setPlanItem(localPlanItem);
    this.tableFilter.prepare();
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
    return 68;
  }
  
  public void setLimit(Expression paramExpression)
  {
    this.limitExpr = paramExpression;
  }
  
  public boolean isCacheable()
  {
    return true;
  }
}
