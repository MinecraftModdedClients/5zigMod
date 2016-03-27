package org.h2.command.dml;

import java.util.ArrayList;
import java.util.HashSet;
import org.h2.command.Prepared;
import org.h2.engine.Database;
import org.h2.engine.Session;
import org.h2.expression.Alias;
import org.h2.expression.Expression;
import org.h2.expression.ExpressionColumn;
import org.h2.expression.ExpressionVisitor;
import org.h2.expression.Parameter;
import org.h2.expression.ValueExpression;
import org.h2.message.DbException;
import org.h2.result.LocalResult;
import org.h2.result.ResultTarget;
import org.h2.result.SortOrder;
import org.h2.table.ColumnResolver;
import org.h2.table.Table;
import org.h2.table.TableFilter;
import org.h2.util.New;
import org.h2.value.Value;
import org.h2.value.ValueInt;
import org.h2.value.ValueNull;

public abstract class Query
  extends Prepared
{
  protected Expression limitExpr;
  protected Expression offsetExpr;
  protected Expression sampleSizeExpr;
  protected boolean distinct;
  protected boolean randomAccessResult;
  private boolean noCache;
  private int lastLimit;
  private long lastEvaluated;
  private LocalResult lastResult;
  private Value[] lastParameters;
  private boolean cacheableChecked;
  
  Query(Session paramSession)
  {
    super(paramSession);
  }
  
  protected abstract LocalResult queryWithoutCache(int paramInt, ResultTarget paramResultTarget);
  
  public abstract void init();
  
  public abstract ArrayList<Expression> getExpressions();
  
  public abstract double getCost();
  
  public int getCostAsExpression()
  {
    return (int)Math.min(1000000.0D, 10.0D + 10.0D * getCost());
  }
  
  public abstract HashSet<Table> getTables();
  
  public abstract void setOrder(ArrayList<SelectOrderBy> paramArrayList);
  
  public abstract void setForUpdate(boolean paramBoolean);
  
  public abstract int getColumnCount();
  
  public abstract void mapColumns(ColumnResolver paramColumnResolver, int paramInt);
  
  public abstract void setEvaluatable(TableFilter paramTableFilter, boolean paramBoolean);
  
  public abstract void addGlobalCondition(Parameter paramParameter, int paramInt1, int paramInt2);
  
  public abstract boolean allowGlobalConditions();
  
  public abstract boolean isEverything(ExpressionVisitor paramExpressionVisitor);
  
  public abstract void updateAggregate(Session paramSession);
  
  public abstract void fireBeforeSelectTriggers();
  
  public void setDistinct(boolean paramBoolean)
  {
    this.distinct = paramBoolean;
  }
  
  public boolean isDistinct()
  {
    return this.distinct;
  }
  
  public void setRandomAccessResult(boolean paramBoolean)
  {
    this.randomAccessResult = paramBoolean;
  }
  
  public boolean isQuery()
  {
    return true;
  }
  
  public boolean isTransactional()
  {
    return true;
  }
  
  public void disableCache()
  {
    this.noCache = true;
  }
  
  private boolean sameResultAsLast(Session paramSession, Value[] paramArrayOfValue1, Value[] paramArrayOfValue2, long paramLong)
  {
    if (!this.cacheableChecked)
    {
      long l = getMaxDataModificationId();
      this.noCache = (l == Long.MAX_VALUE);
      this.cacheableChecked = true;
    }
    if (this.noCache) {
      return false;
    }
    Database localDatabase = paramSession.getDatabase();
    for (int i = 0; i < paramArrayOfValue1.length; i++)
    {
      Value localValue1 = paramArrayOfValue2[i];Value localValue2 = paramArrayOfValue1[i];
      if ((localValue1.getType() != localValue2.getType()) || (!localDatabase.areEqual(localValue1, localValue2))) {
        return false;
      }
    }
    if ((!isEverything(ExpressionVisitor.DETERMINISTIC_VISITOR)) || (!isEverything(ExpressionVisitor.INDEPENDENT_VISITOR))) {
      return false;
    }
    if ((localDatabase.getModificationDataId() > paramLong) && (getMaxDataModificationId() > paramLong)) {
      return false;
    }
    return true;
  }
  
  public final Value[] getParameterValues()
  {
    ArrayList localArrayList = getParameters();
    if (localArrayList == null) {
      localArrayList = New.arrayList();
    }
    int i = localArrayList.size();
    Value[] arrayOfValue = new Value[i];
    for (int j = 0; j < i; j++)
    {
      Value localValue = ((Parameter)localArrayList.get(j)).getParamValue();
      arrayOfValue[j] = localValue;
    }
    return arrayOfValue;
  }
  
  public LocalResult query(int paramInt)
  {
    return query(paramInt, null);
  }
  
  LocalResult query(int paramInt, ResultTarget paramResultTarget)
  {
    fireBeforeSelectTriggers();
    if ((this.noCache) || (!this.session.getDatabase().getOptimizeReuseResults())) {
      return queryWithoutCache(paramInt, paramResultTarget);
    }
    Value[] arrayOfValue = getParameterValues();
    long l = this.session.getDatabase().getModificationDataId();
    if ((isEverything(ExpressionVisitor.DETERMINISTIC_VISITOR)) && 
      (this.lastResult != null) && (!this.lastResult.isClosed()) && (paramInt == this.lastLimit)) {
      if (sameResultAsLast(this.session, arrayOfValue, this.lastParameters, this.lastEvaluated))
      {
        this.lastResult = this.lastResult.createShallowCopy(this.session);
        if (this.lastResult != null)
        {
          this.lastResult.reset();
          return this.lastResult;
        }
      }
    }
    this.lastParameters = arrayOfValue;
    closeLastResult();
    LocalResult localLocalResult = queryWithoutCache(paramInt, paramResultTarget);
    this.lastResult = localLocalResult;
    this.lastEvaluated = l;
    this.lastLimit = paramInt;
    return localLocalResult;
  }
  
  private void closeLastResult()
  {
    if (this.lastResult != null) {
      this.lastResult.close();
    }
  }
  
  static void initOrder(Session paramSession, ArrayList<Expression> paramArrayList, ArrayList<String> paramArrayList1, ArrayList<SelectOrderBy> paramArrayList2, int paramInt, boolean paramBoolean, ArrayList<TableFilter> paramArrayList3)
  {
    Database localDatabase = paramSession.getDatabase();
    for (SelectOrderBy localSelectOrderBy : paramArrayList2)
    {
      Expression localExpression1 = localSelectOrderBy.expression;
      if (localExpression1 != null)
      {
        int i = 0;
        int j = paramArrayList.size();
        if ((localExpression1 instanceof ExpressionColumn))
        {
          localObject1 = (ExpressionColumn)localExpression1;
          String str1 = ((ExpressionColumn)localObject1).getOriginalTableAliasName();
          String str2 = ((ExpressionColumn)localObject1).getOriginalColumnName();
          for (int n = 0; n < paramInt; n++)
          {
            boolean bool = false;
            Expression localExpression2 = (Expression)paramArrayList.get(n);
            Object localObject2;
            Object localObject3;
            Object localObject4;
            if ((localExpression2 instanceof ExpressionColumn))
            {
              localObject2 = (ExpressionColumn)localExpression2;
              bool = localDatabase.equalsIdentifiers(str2, ((ExpressionColumn)localObject2).getColumnName());
              if ((bool) && (str1 != null))
              {
                localObject3 = ((ExpressionColumn)localObject2).getOriginalTableAliasName();
                if (localObject3 == null)
                {
                  bool = false;
                  if (paramArrayList3 != null)
                  {
                    int i1 = 0;
                    for (int i2 = paramArrayList3.size(); i1 < i2; i1++)
                    {
                      localObject4 = (TableFilter)paramArrayList3.get(i1);
                      if (localDatabase.equalsIdentifiers(((TableFilter)localObject4).getTableAlias(), str1))
                      {
                        bool = true;
                        break;
                      }
                    }
                  }
                }
                else
                {
                  bool = localDatabase.equalsIdentifiers((String)localObject3, str1);
                }
              }
            }
            else
            {
              if (!(localExpression2 instanceof Alias)) {
                continue;
              }
              if ((str1 == null) && (localDatabase.equalsIdentifiers(str2, localExpression2.getAlias())))
              {
                bool = true;
              }
              else
              {
                localObject2 = localExpression2.getNonAliasExpression();
                if ((localObject2 instanceof ExpressionColumn))
                {
                  localObject3 = (ExpressionColumn)localObject2;
                  String str4 = ((ExpressionColumn)localObject1).getSQL();
                  String str5 = ((ExpressionColumn)localObject3).getSQL();
                  localObject4 = ((ExpressionColumn)localObject3).getColumnName();
                  bool = localDatabase.equalsIdentifiers(str2, (String)localObject4);
                  if (!localDatabase.equalsIdentifiers(str4, str5)) {
                    bool = false;
                  }
                }
              }
            }
            if (bool)
            {
              j = n;
              i = 1;
              break;
            }
          }
        }
        else
        {
          localObject1 = localExpression1.getSQL();
          if (paramArrayList1 != null)
          {
            int k = 0;
            for (int m = paramArrayList1.size(); k < m; k++)
            {
              String str3 = (String)paramArrayList1.get(k);
              if (localDatabase.equalsIdentifiers(str3, (String)localObject1))
              {
                j = k;
                i = 1;
                break;
              }
            }
          }
        }
        if (i == 0)
        {
          if (paramBoolean) {
            throw DbException.get(90068, localExpression1.getSQL());
          }
          paramArrayList.add(localExpression1);
          localObject1 = localExpression1.getSQL();
          paramArrayList1.add(localObject1);
        }
        localSelectOrderBy.columnIndexExpr = ValueExpression.get(ValueInt.get(j + 1));
        Object localObject1 = ((Expression)paramArrayList.get(j)).getNonAliasExpression();
        localSelectOrderBy.expression = ((Expression)localObject1);
      }
    }
  }
  
  public SortOrder prepareOrder(ArrayList<SelectOrderBy> paramArrayList, int paramInt)
  {
    int i = paramArrayList.size();
    int[] arrayOfInt1 = new int[i];
    int[] arrayOfInt2 = new int[i];
    for (int j = 0; j < i; j++)
    {
      SelectOrderBy localSelectOrderBy = (SelectOrderBy)paramArrayList.get(j);
      
      int m = 0;
      Expression localExpression = localSelectOrderBy.columnIndexExpr;
      Value localValue = localExpression.getValue(null);
      int k;
      if (localValue == ValueNull.INSTANCE)
      {
        k = 0;
      }
      else
      {
        k = localValue.getInt();
        if (k < 0)
        {
          m = 1;
          k = -k;
        }
        k--;
        if ((k < 0) || (k >= paramInt)) {
          throw DbException.get(90068, "" + (k + 1));
        }
      }
      arrayOfInt1[j] = k;
      boolean bool = localSelectOrderBy.descending;
      if (m != 0) {
        bool = !bool;
      }
      int n = bool ? 1 : 0;
      if (localSelectOrderBy.nullsFirst) {
        n += 2;
      } else if (localSelectOrderBy.nullsLast) {
        n += 4;
      }
      arrayOfInt2[j] = n;
    }
    return new SortOrder(this.session.getDatabase(), arrayOfInt1, arrayOfInt2, paramArrayList);
  }
  
  public void setOffset(Expression paramExpression)
  {
    this.offsetExpr = paramExpression;
  }
  
  public Expression getOffset()
  {
    return this.offsetExpr;
  }
  
  public void setLimit(Expression paramExpression)
  {
    this.limitExpr = paramExpression;
  }
  
  public Expression getLimit()
  {
    return this.limitExpr;
  }
  
  void addParameter(Parameter paramParameter)
  {
    if (this.parameters == null) {
      this.parameters = New.arrayList();
    }
    this.parameters.add(paramParameter);
  }
  
  public void setSampleSize(Expression paramExpression)
  {
    this.sampleSizeExpr = paramExpression;
  }
  
  int getSampleSizeValue(Session paramSession)
  {
    if (this.sampleSizeExpr == null) {
      return 0;
    }
    Value localValue = this.sampleSizeExpr.optimize(paramSession).getValue(paramSession);
    if (localValue == ValueNull.INSTANCE) {
      return 0;
    }
    return localValue.getInt();
  }
  
  public final long getMaxDataModificationId()
  {
    ExpressionVisitor localExpressionVisitor = ExpressionVisitor.getMaxModificationIdVisitor();
    isEverything(localExpressionVisitor);
    return localExpressionVisitor.getMaxDataModificationId();
  }
}
