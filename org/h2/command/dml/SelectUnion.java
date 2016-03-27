package org.h2.command.dml;

import java.util.ArrayList;
import java.util.HashSet;
import org.h2.engine.Database;
import org.h2.engine.DbSettings;
import org.h2.engine.Session;
import org.h2.engine.SysProperties;
import org.h2.expression.Expression;
import org.h2.expression.ExpressionColumn;
import org.h2.expression.ExpressionVisitor;
import org.h2.expression.Parameter;
import org.h2.expression.ValueExpression;
import org.h2.message.DbException;
import org.h2.result.LocalResult;
import org.h2.result.ResultInterface;
import org.h2.result.ResultTarget;
import org.h2.result.SortOrder;
import org.h2.table.Column;
import org.h2.table.ColumnResolver;
import org.h2.table.Table;
import org.h2.table.TableFilter;
import org.h2.util.New;
import org.h2.util.StringUtils;
import org.h2.value.Value;
import org.h2.value.ValueInt;
import org.h2.value.ValueNull;

public class SelectUnion
  extends Query
{
  public static final int UNION = 0;
  public static final int UNION_ALL = 1;
  public static final int EXCEPT = 2;
  public static final int INTERSECT = 3;
  private int unionType;
  private final Query left;
  private Query right;
  private ArrayList<Expression> expressions;
  private Expression[] expressionArray;
  private ArrayList<SelectOrderBy> orderList;
  private SortOrder sort;
  private boolean isPrepared;
  private boolean checkInit;
  private boolean isForUpdate;
  
  public SelectUnion(Session paramSession, Query paramQuery)
  {
    super(paramSession);
    this.left = paramQuery;
  }
  
  public void setUnionType(int paramInt)
  {
    this.unionType = paramInt;
  }
  
  public int getUnionType()
  {
    return this.unionType;
  }
  
  public void setRight(Query paramQuery)
  {
    this.right = paramQuery;
  }
  
  public Query getLeft()
  {
    return this.left;
  }
  
  public Query getRight()
  {
    return this.right;
  }
  
  public void setSQL(String paramString)
  {
    this.sqlStatement = paramString;
  }
  
  public void setOrder(ArrayList<SelectOrderBy> paramArrayList)
  {
    this.orderList = paramArrayList;
  }
  
  private Value[] convert(Value[] paramArrayOfValue, int paramInt)
  {
    Value[] arrayOfValue;
    if (paramInt == paramArrayOfValue.length) {
      arrayOfValue = paramArrayOfValue;
    } else {
      arrayOfValue = new Value[paramInt];
    }
    for (int i = 0; i < paramInt; i++)
    {
      Expression localExpression = (Expression)this.expressions.get(i);
      arrayOfValue[i] = paramArrayOfValue[i].convertTo(localExpression.getType());
    }
    return arrayOfValue;
  }
  
  public ResultInterface queryMeta()
  {
    int i = this.left.getColumnCount();
    LocalResult localLocalResult = new LocalResult(this.session, this.expressionArray, i);
    localLocalResult.done();
    return localLocalResult;
  }
  
  public LocalResult getEmptyResult()
  {
    int i = this.left.getColumnCount();
    return new LocalResult(this.session, this.expressionArray, i);
  }
  
  protected LocalResult queryWithoutCache(int paramInt, ResultTarget paramResultTarget)
  {
    if (paramInt != 0)
    {
      if (this.limitExpr == null)
      {
        i = -1;
      }
      else
      {
        localObject1 = this.limitExpr.getValue(this.session);
        i = localObject1 == ValueNull.INSTANCE ? -1 : ((Value)localObject1).getInt();
      }
      if (i < 0) {
        i = paramInt;
      } else {
        i = Math.min(i, paramInt);
      }
      this.limitExpr = ValueExpression.get(ValueInt.get(i));
    }
    if ((this.session.getDatabase().getSettings().optimizeInsertFromSelect) && 
      (this.unionType == 1) && (paramResultTarget != null) && 
      (this.sort == null) && (!this.distinct) && (paramInt == 0) && (this.offsetExpr == null) && (this.limitExpr == null))
    {
      this.left.query(0, paramResultTarget);
      this.right.query(0, paramResultTarget);
      return null;
    }
    int i = this.left.getColumnCount();
    Object localObject1 = new LocalResult(this.session, this.expressionArray, i);
    if (this.sort != null) {
      ((LocalResult)localObject1).setSortOrder(this.sort);
    }
    if (this.distinct)
    {
      this.left.setDistinct(true);
      this.right.setDistinct(true);
      ((LocalResult)localObject1).setDistinct();
    }
    if (this.randomAccessResult) {
      ((LocalResult)localObject1).setRandomAccess();
    }
    switch (this.unionType)
    {
    case 0: 
    case 2: 
      this.left.setDistinct(true);
      this.right.setDistinct(true);
      ((LocalResult)localObject1).setDistinct();
      break;
    case 1: 
      break;
    case 3: 
      this.left.setDistinct(true);
      this.right.setDistinct(true);
      break;
    default: 
      DbException.throwInternalError("type=" + this.unionType);
    }
    LocalResult localLocalResult1 = this.left.query(0);
    LocalResult localLocalResult2 = this.right.query(0);
    localLocalResult1.reset();
    localLocalResult2.reset();
    switch (this.unionType)
    {
    case 0: 
    case 1: 
      while (localLocalResult1.next()) {
        ((LocalResult)localObject1).addRow(convert(localLocalResult1.currentRow(), i));
      }
    }
    Object localObject2;
    while (localLocalResult2.next())
    {
      ((LocalResult)localObject1).addRow(convert(localLocalResult2.currentRow(), i)); continue;
      while (localLocalResult1.next()) {
        ((LocalResult)localObject1).addRow(convert(localLocalResult1.currentRow(), i));
      }
      while (localLocalResult2.next())
      {
        ((LocalResult)localObject1).removeDistinct(convert(localLocalResult2.currentRow(), i)); continue;
        
        localObject2 = new LocalResult(this.session, this.expressionArray, i);
        ((LocalResult)localObject2).setDistinct();
        ((LocalResult)localObject2).setRandomAccess();
        while (localLocalResult1.next()) {
          ((LocalResult)localObject2).addRow(convert(localLocalResult1.currentRow(), i));
        }
        while (localLocalResult2.next())
        {
          Value[] arrayOfValue = convert(localLocalResult2.currentRow(), i);
          if (((LocalResult)localObject2).containsDistinct(arrayOfValue)) {
            ((LocalResult)localObject1).addRow(arrayOfValue);
          }
          continue;
          
          DbException.throwInternalError("type=" + this.unionType);
        }
      }
    }
    if (this.offsetExpr != null) {
      ((LocalResult)localObject1).setOffset(this.offsetExpr.getValue(this.session).getInt());
    }
    if (this.limitExpr != null)
    {
      localObject2 = this.limitExpr.getValue(this.session);
      if (localObject2 != ValueNull.INSTANCE) {
        ((LocalResult)localObject1).setLimit(((Value)localObject2).getInt());
      }
    }
    localLocalResult1.close();
    localLocalResult2.close();
    ((LocalResult)localObject1).done();
    if (paramResultTarget != null)
    {
      while (((LocalResult)localObject1).next()) {
        paramResultTarget.addRow(((LocalResult)localObject1).currentRow());
      }
      ((LocalResult)localObject1).close();
      return null;
    }
    return (LocalResult)localObject1;
  }
  
  public void init()
  {
    if ((SysProperties.CHECK) && (this.checkInit)) {
      DbException.throwInternalError();
    }
    this.checkInit = true;
    this.left.init();
    this.right.init();
    int i = this.left.getColumnCount();
    if (i != this.right.getColumnCount()) {
      throw DbException.get(21002);
    }
    ArrayList localArrayList = this.left.getExpressions();
    
    this.expressions = New.arrayList();
    for (int j = 0; j < i; j++)
    {
      Expression localExpression = (Expression)localArrayList.get(j);
      this.expressions.add(localExpression);
    }
  }
  
  public void prepare()
  {
    if (this.isPrepared) {
      return;
    }
    if ((SysProperties.CHECK) && (!this.checkInit)) {
      DbException.throwInternalError("not initialized");
    }
    this.isPrepared = true;
    this.left.prepare();
    this.right.prepare();
    int i = this.left.getColumnCount();
    
    this.expressions = New.arrayList();
    ArrayList localArrayList1 = this.left.getExpressions();
    ArrayList localArrayList2 = this.right.getExpressions();
    for (int j = 0; j < i; j++)
    {
      Expression localExpression1 = (Expression)localArrayList1.get(j);
      Expression localExpression2 = (Expression)localArrayList2.get(j);
      int k = Value.getHigherOrder(localExpression1.getType(), localExpression2.getType());
      long l = Math.max(localExpression1.getPrecision(), localExpression2.getPrecision());
      int m = Math.max(localExpression1.getScale(), localExpression2.getScale());
      int n = Math.max(localExpression1.getDisplaySize(), localExpression2.getDisplaySize());
      Column localColumn = new Column(localExpression1.getAlias(), k, l, m, n);
      ExpressionColumn localExpressionColumn = new ExpressionColumn(this.session.getDatabase(), localColumn);
      this.expressions.add(localExpressionColumn);
    }
    if (this.orderList != null)
    {
      initOrder(this.session, this.expressions, null, this.orderList, getColumnCount(), true, null);
      this.sort = prepareOrder(this.orderList, this.expressions.size());
      this.orderList = null;
    }
    this.expressionArray = new Expression[this.expressions.size()];
    this.expressions.toArray(this.expressionArray);
  }
  
  public double getCost()
  {
    return this.left.getCost() + this.right.getCost();
  }
  
  public HashSet<Table> getTables()
  {
    HashSet localHashSet = this.left.getTables();
    localHashSet.addAll(this.right.getTables());
    return localHashSet;
  }
  
  public ArrayList<Expression> getExpressions()
  {
    return this.expressions;
  }
  
  public void setForUpdate(boolean paramBoolean)
  {
    this.left.setForUpdate(paramBoolean);
    this.right.setForUpdate(paramBoolean);
    this.isForUpdate = paramBoolean;
  }
  
  public int getColumnCount()
  {
    return this.left.getColumnCount();
  }
  
  public void mapColumns(ColumnResolver paramColumnResolver, int paramInt)
  {
    this.left.mapColumns(paramColumnResolver, paramInt);
    this.right.mapColumns(paramColumnResolver, paramInt);
  }
  
  public void setEvaluatable(TableFilter paramTableFilter, boolean paramBoolean)
  {
    this.left.setEvaluatable(paramTableFilter, paramBoolean);
    this.right.setEvaluatable(paramTableFilter, paramBoolean);
  }
  
  public void addGlobalCondition(Parameter paramParameter, int paramInt1, int paramInt2)
  {
    addParameter(paramParameter);
    switch (this.unionType)
    {
    case 0: 
    case 1: 
    case 3: 
      this.left.addGlobalCondition(paramParameter, paramInt1, paramInt2);
      this.right.addGlobalCondition(paramParameter, paramInt1, paramInt2);
      break;
    case 2: 
      this.left.addGlobalCondition(paramParameter, paramInt1, paramInt2);
      break;
    default: 
      DbException.throwInternalError("type=" + this.unionType);
    }
  }
  
  public String getPlanSQL()
  {
    StringBuilder localStringBuilder = new StringBuilder();
    localStringBuilder.append('(').append(this.left.getPlanSQL()).append(')');
    switch (this.unionType)
    {
    case 1: 
      localStringBuilder.append("\nUNION ALL\n");
      break;
    case 0: 
      localStringBuilder.append("\nUNION\n");
      break;
    case 3: 
      localStringBuilder.append("\nINTERSECT\n");
      break;
    case 2: 
      localStringBuilder.append("\nEXCEPT\n");
      break;
    default: 
      DbException.throwInternalError("type=" + this.unionType);
    }
    localStringBuilder.append('(').append(this.right.getPlanSQL()).append(')');
    Expression[] arrayOfExpression = (Expression[])this.expressions.toArray(new Expression[this.expressions.size()]);
    if (this.sort != null) {
      localStringBuilder.append("\nORDER BY ").append(this.sort.getSQL(arrayOfExpression, arrayOfExpression.length));
    }
    if (this.limitExpr != null)
    {
      localStringBuilder.append("\nLIMIT ").append(StringUtils.unEnclose(this.limitExpr.getSQL()));
      if (this.offsetExpr != null) {
        localStringBuilder.append("\nOFFSET ").append(StringUtils.unEnclose(this.offsetExpr.getSQL()));
      }
    }
    if (this.sampleSizeExpr != null) {
      localStringBuilder.append("\nSAMPLE_SIZE ").append(StringUtils.unEnclose(this.sampleSizeExpr.getSQL()));
    }
    if (this.isForUpdate) {
      localStringBuilder.append("\nFOR UPDATE");
    }
    return localStringBuilder.toString();
  }
  
  public LocalResult query(int paramInt, ResultTarget paramResultTarget)
  {
    return queryWithoutCache(paramInt, paramResultTarget);
  }
  
  public boolean isEverything(ExpressionVisitor paramExpressionVisitor)
  {
    return (this.left.isEverything(paramExpressionVisitor)) && (this.right.isEverything(paramExpressionVisitor));
  }
  
  public boolean isReadOnly()
  {
    return (this.left.isReadOnly()) && (this.right.isReadOnly());
  }
  
  public void updateAggregate(Session paramSession)
  {
    this.left.updateAggregate(paramSession);
    this.right.updateAggregate(paramSession);
  }
  
  public void fireBeforeSelectTriggers()
  {
    this.left.fireBeforeSelectTriggers();
    this.right.fireBeforeSelectTriggers();
  }
  
  public int getType()
  {
    return 66;
  }
  
  public boolean allowGlobalConditions()
  {
    return (this.left.allowGlobalConditions()) && (this.right.allowGlobalConditions());
  }
}
