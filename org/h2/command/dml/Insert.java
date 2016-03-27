package org.h2.command.dml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import org.h2.command.Command;
import org.h2.command.Prepared;
import org.h2.engine.Session;
import org.h2.engine.User;
import org.h2.expression.Comparison;
import org.h2.expression.ConditionAndOr;
import org.h2.expression.Expression;
import org.h2.expression.ExpressionColumn;
import org.h2.expression.Parameter;
import org.h2.index.Index;
import org.h2.index.IndexType;
import org.h2.message.DbException;
import org.h2.result.LocalResult;
import org.h2.result.ResultInterface;
import org.h2.result.ResultTarget;
import org.h2.result.Row;
import org.h2.table.Column;
import org.h2.table.Table;
import org.h2.util.New;
import org.h2.util.StatementBuilder;
import org.h2.value.Value;
import org.h2.value.ValueNull;

public class Insert
  extends Prepared
  implements ResultTarget
{
  private Table table;
  private Column[] columns;
  private final ArrayList<Expression[]> list = New.arrayList();
  private Query query;
  private boolean sortedInsertMode;
  private int rowNumber;
  private boolean insertFromSelect;
  private HashMap<Column, Expression> duplicateKeyAssignmentMap;
  
  public Insert(Session paramSession)
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
  
  public void setQuery(Query paramQuery)
  {
    this.query = paramQuery;
  }
  
  public void addAssignmentForDuplicate(Column paramColumn, Expression paramExpression)
  {
    if (this.duplicateKeyAssignmentMap == null) {
      this.duplicateKeyAssignmentMap = New.hashMap();
    }
    if (this.duplicateKeyAssignmentMap.containsKey(paramColumn)) {
      throw DbException.get(42121, paramColumn.getName());
    }
    this.duplicateKeyAssignmentMap.put(paramColumn, paramExpression);
  }
  
  public void addRow(Expression[] paramArrayOfExpression)
  {
    this.list.add(paramArrayOfExpression);
  }
  
  public int update()
  {
    Index localIndex = null;
    if (this.sortedInsertMode)
    {
      localIndex = this.table.getScanIndex(this.session);
      localIndex.setSortedInsertMode(true);
    }
    try
    {
      return insertRows();
    }
    finally
    {
      if (localIndex != null) {
        localIndex.setSortedInsertMode(false);
      }
    }
  }
  
  private int insertRows()
  {
    this.session.getUser().checkRight(this.table, 4);
    setCurrentRowNumber(0);
    this.table.fire(this.session, 1, true);
    this.rowNumber = 0;
    int i = this.list.size();
    if (i > 0)
    {
      int j = this.columns.length;
      for (int k = 0; k < i; k++)
      {
        this.session.startStatementWithinTransaction();
        Row localRow = this.table.getTemplateRow();
        Expression[] arrayOfExpression = (Expression[])this.list.get(k);
        setCurrentRowNumber(k + 1);
        for (int m = 0; m < j; m++)
        {
          Column localColumn = this.columns[m];
          int n = localColumn.getColumnId();
          Expression localExpression = arrayOfExpression[m];
          if (localExpression != null)
          {
            localExpression = localExpression.optimize(this.session);
            try
            {
              Value localValue = localColumn.convert(localExpression.getValue(this.session));
              localRow.setValue(n, localValue);
            }
            catch (DbException localDbException2)
            {
              throw setRow(localDbException2, k, getSQL(arrayOfExpression));
            }
          }
        }
        this.rowNumber += 1;
        this.table.validateConvertUpdateSequence(this.session, localRow);
        boolean bool = this.table.fireBeforeRow(this.session, null, localRow);
        if (!bool)
        {
          this.table.lock(this.session, true, false);
          try
          {
            this.table.addRow(this.session, localRow);
          }
          catch (DbException localDbException1)
          {
            handleOnDuplicate(localDbException1);
          }
          this.session.log(this.table, (short)0, localRow);
          this.table.fireAfterRow(this.session, null, localRow, false);
        }
      }
    }
    else
    {
      this.table.lock(this.session, true, false);
      if (this.insertFromSelect)
      {
        this.query.query(0, this);
      }
      else
      {
        LocalResult localLocalResult = this.query.query(0);
        while (localLocalResult.next())
        {
          Value[] arrayOfValue = localLocalResult.currentRow();
          addRow(arrayOfValue);
        }
        localLocalResult.close();
      }
    }
    this.table.fire(this.session, 1, false);
    return this.rowNumber;
  }
  
  public void addRow(Value[] paramArrayOfValue)
  {
    Row localRow = this.table.getTemplateRow();
    setCurrentRowNumber(++this.rowNumber);
    int i = 0;
    for (int j = this.columns.length; i < j; i++)
    {
      Column localColumn = this.columns[i];
      int k = localColumn.getColumnId();
      try
      {
        Value localValue = localColumn.convert(paramArrayOfValue[i]);
        localRow.setValue(k, localValue);
      }
      catch (DbException localDbException)
      {
        throw setRow(localDbException, this.rowNumber, getSQL(paramArrayOfValue));
      }
    }
    this.table.validateConvertUpdateSequence(this.session, localRow);
    i = this.table.fireBeforeRow(this.session, null, localRow);
    if (i == 0)
    {
      this.table.addRow(this.session, localRow);
      this.session.log(this.table, (short)0, localRow);
      this.table.fireAfterRow(this.session, null, localRow, false);
    }
  }
  
  public int getRowCount()
  {
    return this.rowNumber;
  }
  
  public String getPlanSQL()
  {
    StatementBuilder localStatementBuilder = new StatementBuilder("INSERT INTO ");
    localStatementBuilder.append(this.table.getSQL()).append('(');
    Object localObject1;
    for (localObject1 : this.columns)
    {
      localStatementBuilder.appendExceptFirst(", ");
      localStatementBuilder.append(((Column)localObject1).getSQL());
    }
    localStatementBuilder.append(")\n");
    if (this.insertFromSelect) {
      localStatementBuilder.append("DIRECT ");
    }
    if (this.sortedInsertMode) {
      localStatementBuilder.append("SORTED ");
    }
    int i;
    if (this.list.size() > 0)
    {
      localStatementBuilder.append("VALUES ");
      i = 0;
      if (this.list.size() > 1) {
        localStatementBuilder.append('\n');
      }
      for (Expression[] arrayOfExpression : this.list)
      {
        if (i++ > 0) {
          localStatementBuilder.append(",\n");
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
    if (this.list.size() > 0)
    {
      for (Expression[] arrayOfExpression : this.list)
      {
        if (arrayOfExpression.length != this.columns.length) {
          throw DbException.get(21002);
        }
        int i = 0;
        for (int j = arrayOfExpression.length; i < j; i++)
        {
          Expression localExpression = arrayOfExpression[i];
          if (localExpression != null)
          {
            localExpression = localExpression.optimize(this.session);
            if ((localExpression instanceof Parameter))
            {
              Parameter localParameter = (Parameter)localExpression;
              localParameter.setColumn(this.columns[i]);
            }
            arrayOfExpression[i] = localExpression;
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
  }
  
  public boolean isTransactional()
  {
    return true;
  }
  
  public ResultInterface queryMeta()
  {
    return null;
  }
  
  public void setSortedInsertMode(boolean paramBoolean)
  {
    this.sortedInsertMode = paramBoolean;
  }
  
  public int getType()
  {
    return 61;
  }
  
  public void setInsertFromSelect(boolean paramBoolean)
  {
    this.insertFromSelect = paramBoolean;
  }
  
  public boolean isCacheable()
  {
    return (this.duplicateKeyAssignmentMap == null) || (this.duplicateKeyAssignmentMap.isEmpty());
  }
  
  private void handleOnDuplicate(DbException paramDbException)
  {
    if (paramDbException.getErrorCode() != 23505) {
      throw paramDbException;
    }
    if ((this.duplicateKeyAssignmentMap == null) || (this.duplicateKeyAssignmentMap.isEmpty())) {
      throw paramDbException;
    }
    ArrayList localArrayList = new ArrayList(this.duplicateKeyAssignmentMap.size());
    for (int i = 0; i < this.columns.length; i++)
    {
      localObject1 = this.session.getCurrentSchemaName() + "." + this.table.getName() + "." + this.columns[i].getName();
      
      localArrayList.add(localObject1);
      this.session.setVariable((String)localObject1, ((Expression[])this.list.get(getCurrentRowNumber() - 1))[i].getValue(this.session));
    }
    StatementBuilder localStatementBuilder = new StatementBuilder("UPDATE ");
    localStatementBuilder.append(this.table.getSQL()).append(" SET ");
    for (Object localObject1 = this.duplicateKeyAssignmentMap.keySet().iterator(); ((Iterator)localObject1).hasNext();)
    {
      localObject2 = (Column)((Iterator)localObject1).next();
      localStatementBuilder.appendExceptFirst(", ");
      localObject3 = (Expression)this.duplicateKeyAssignmentMap.get(localObject2);
      localStatementBuilder.append(((Column)localObject2).getSQL()).append("=").append(((Expression)localObject3).getSQL());
    }
    localStatementBuilder.append(" WHERE ");
    localObject1 = searchForUpdateIndex();
    if (localObject1 == null) {
      throw DbException.getUnsupportedException("Unable to apply ON DUPLICATE KEY UPDATE, no index found!");
    }
    localStatementBuilder.append(prepareUpdateCondition((Index)localObject1).getSQL());
    Object localObject2 = localStatementBuilder.toString();
    Object localObject3 = this.session.prepare((String)localObject2);
    for (Iterator localIterator = ((Prepared)localObject3).getParameters().iterator(); localIterator.hasNext();)
    {
      localObject4 = (Parameter)localIterator.next();
      Parameter localParameter = (Parameter)this.parameters.get(((Parameter)localObject4).getIndex());
      ((Parameter)localObject4).setValue(localParameter.getValue(this.session));
    }
    Object localObject4;
    ((Prepared)localObject3).update();
    for (localIterator = localArrayList.iterator(); localIterator.hasNext();)
    {
      localObject4 = (String)localIterator.next();
      this.session.setVariable((String)localObject4, ValueNull.INSTANCE);
    }
  }
  
  private Index searchForUpdateIndex()
  {
    Object localObject = null;
    for (Index localIndex : this.table.getIndexes()) {
      if ((localIndex.getIndexType().isPrimaryKey()) || (localIndex.getIndexType().isUnique()))
      {
        for (Column localColumn1 : localIndex.getColumns())
        {
          for (Column localColumn2 : this.columns)
          {
            if (localColumn1.getName() == localColumn2.getName())
            {
              localObject = localIndex;
              break;
            }
            localObject = null;
          }
          if (localObject == null) {
            break;
          }
        }
        if (localObject != null) {
          break;
        }
      }
    }
    return (Index)localObject;
  }
  
  private Expression prepareUpdateCondition(Index paramIndex)
  {
    Object localObject = null;
    for (Column localColumn : paramIndex.getColumns())
    {
      ExpressionColumn localExpressionColumn = new ExpressionColumn(this.session.getDatabase(), this.session.getCurrentSchemaName(), this.table.getName(), localColumn.getName());
      for (int k = 0; k < this.columns.length; k++) {
        if (localExpressionColumn.getColumnName().equals(this.columns[k].getName())) {
          if (localObject == null) {
            localObject = new Comparison(this.session, 0, localExpressionColumn, ((Expression[])this.list.get(getCurrentRowNumber() - 1))[(k++)]);
          } else {
            localObject = new ConditionAndOr(0, (Expression)localObject, new Comparison(this.session, 0, localExpressionColumn, ((Expression[])this.list.get(0))[(k++)]));
          }
        }
      }
    }
    return (Expression)localObject;
  }
}
