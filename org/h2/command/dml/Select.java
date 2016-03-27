package org.h2.command.dml;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import org.h2.engine.Database;
import org.h2.engine.DbSettings;
import org.h2.engine.Session;
import org.h2.engine.SysProperties;
import org.h2.expression.Comparison;
import org.h2.expression.ConditionAndOr;
import org.h2.expression.Expression;
import org.h2.expression.ExpressionColumn;
import org.h2.expression.ExpressionVisitor;
import org.h2.expression.Parameter;
import org.h2.expression.Wildcard;
import org.h2.index.Cursor;
import org.h2.index.Index;
import org.h2.index.IndexType;
import org.h2.message.DbException;
import org.h2.result.LocalResult;
import org.h2.result.ResultInterface;
import org.h2.result.ResultTarget;
import org.h2.result.SearchRow;
import org.h2.result.SortOrder;
import org.h2.schema.Schema;
import org.h2.table.Column;
import org.h2.table.ColumnResolver;
import org.h2.table.IndexColumn;
import org.h2.table.Table;
import org.h2.table.TableFilter;
import org.h2.util.New;
import org.h2.util.StatementBuilder;
import org.h2.util.StringUtils;
import org.h2.util.ValueHashMap;
import org.h2.value.Value;
import org.h2.value.ValueArray;
import org.h2.value.ValueNull;

public class Select
  extends Query
{
  private TableFilter topTableFilter;
  private final ArrayList<TableFilter> filters = New.arrayList();
  private final ArrayList<TableFilter> topFilters = New.arrayList();
  private ArrayList<Expression> expressions;
  private Expression[] expressionArray;
  private Expression having;
  private Expression condition;
  private int visibleColumnCount;
  private int distinctColumnCount;
  private ArrayList<SelectOrderBy> orderList;
  private ArrayList<Expression> group;
  private int[] groupIndex;
  private boolean[] groupByExpression;
  private HashMap<Expression, Object> currentGroup;
  private int havingIndex;
  private boolean isGroupQuery;
  private boolean isGroupSortedQuery;
  private boolean isForUpdate;
  private boolean isForUpdateMvcc;
  private double cost;
  private boolean isQuickAggregateQuery;
  private boolean isDistinctQuery;
  private boolean isPrepared;
  private boolean checkInit;
  private boolean sortUsingIndex;
  private SortOrder sort;
  private int currentGroupRowId;
  
  public Select(Session paramSession)
  {
    super(paramSession);
  }
  
  public void addTableFilter(TableFilter paramTableFilter, boolean paramBoolean)
  {
    this.filters.add(paramTableFilter);
    if (paramBoolean) {
      this.topFilters.add(paramTableFilter);
    }
  }
  
  public ArrayList<TableFilter> getTopFilters()
  {
    return this.topFilters;
  }
  
  public void setExpressions(ArrayList<Expression> paramArrayList)
  {
    this.expressions = paramArrayList;
  }
  
  public void setGroupQuery()
  {
    this.isGroupQuery = true;
  }
  
  public void setGroupBy(ArrayList<Expression> paramArrayList)
  {
    this.group = paramArrayList;
  }
  
  public ArrayList<Expression> getGroupBy()
  {
    return this.group;
  }
  
  public HashMap<Expression, Object> getCurrentGroup()
  {
    return this.currentGroup;
  }
  
  public int getCurrentGroupRowId()
  {
    return this.currentGroupRowId;
  }
  
  public void setOrder(ArrayList<SelectOrderBy> paramArrayList)
  {
    this.orderList = paramArrayList;
  }
  
  public void addCondition(Expression paramExpression)
  {
    if (this.condition == null) {
      this.condition = paramExpression;
    } else {
      this.condition = new ConditionAndOr(0, paramExpression, this.condition);
    }
  }
  
  private void queryGroupSorted(int paramInt, ResultTarget paramResultTarget)
  {
    int i = 0;
    setCurrentRowNumber(0);
    this.currentGroup = null;
    Object localObject = null;
    while (this.topTableFilter.next())
    {
      setCurrentRowNumber(i + 1);
      if ((this.condition == null) || (Boolean.TRUE.equals(this.condition.getBooleanValue(this.session))))
      {
        i++;
        Value[] arrayOfValue = new Value[this.groupIndex.length];
        for (int j = 0; j < this.groupIndex.length; j++)
        {
          int k = this.groupIndex[j];
          Expression localExpression2 = (Expression)this.expressions.get(k);
          arrayOfValue[j] = localExpression2.getValue(this.session);
        }
        if (localObject == null)
        {
          localObject = arrayOfValue;
          this.currentGroup = New.hashMap();
        }
        else if (!Arrays.equals((Object[])localObject, arrayOfValue))
        {
          addGroupSortedRow((Value[])localObject, paramInt, paramResultTarget);
          localObject = arrayOfValue;
          this.currentGroup = New.hashMap();
        }
        this.currentGroupRowId += 1;
        for (j = 0; j < paramInt; j++) {
          if ((this.groupByExpression == null) || (this.groupByExpression[j] == 0))
          {
            Expression localExpression1 = (Expression)this.expressions.get(j);
            localExpression1.updateAggregate(this.session);
          }
        }
      }
    }
    if (localObject != null) {
      addGroupSortedRow((Value[])localObject, paramInt, paramResultTarget);
    }
  }
  
  private void addGroupSortedRow(Value[] paramArrayOfValue, int paramInt, ResultTarget paramResultTarget)
  {
    Value[] arrayOfValue = new Value[paramInt];
    for (int i = 0; (this.groupIndex != null) && (i < this.groupIndex.length); i++) {
      arrayOfValue[this.groupIndex[i]] = paramArrayOfValue[i];
    }
    for (i = 0; i < paramInt; i++) {
      if ((this.groupByExpression == null) || (this.groupByExpression[i] == 0))
      {
        Expression localExpression = (Expression)this.expressions.get(i);
        arrayOfValue[i] = localExpression.getValue(this.session);
      }
    }
    if (isHavingNullOrFalse(arrayOfValue)) {
      return;
    }
    arrayOfValue = keepOnlyDistinct(arrayOfValue, paramInt);
    paramResultTarget.addRow(arrayOfValue);
  }
  
  private Value[] keepOnlyDistinct(Value[] paramArrayOfValue, int paramInt)
  {
    if (paramInt == this.distinctColumnCount) {
      return paramArrayOfValue;
    }
    Value[] arrayOfValue = new Value[this.distinctColumnCount];
    System.arraycopy(paramArrayOfValue, 0, arrayOfValue, 0, this.distinctColumnCount);
    return arrayOfValue;
  }
  
  private boolean isHavingNullOrFalse(Value[] paramArrayOfValue)
  {
    if (this.havingIndex >= 0)
    {
      Value localValue = paramArrayOfValue[this.havingIndex];
      if (localValue == ValueNull.INSTANCE) {
        return true;
      }
      if (!Boolean.TRUE.equals(localValue.getBoolean())) {
        return true;
      }
    }
    return false;
  }
  
  private Index getGroupSortedIndex()
  {
    if ((this.groupIndex == null) || (this.groupByExpression == null)) {
      return null;
    }
    ArrayList localArrayList = this.topTableFilter.getTable().getIndexes();
    if (localArrayList != null)
    {
      int i = 0;
      for (int j = localArrayList.size(); i < j; i++)
      {
        Index localIndex = (Index)localArrayList.get(i);
        if (!localIndex.getIndexType().isScan()) {
          if (!localIndex.getIndexType().isHash()) {
            if (isGroupSortedIndex(this.topTableFilter, localIndex)) {
              return localIndex;
            }
          }
        }
      }
    }
    return null;
  }
  
  private boolean isGroupSortedIndex(TableFilter paramTableFilter, Index paramIndex)
  {
    Column[] arrayOfColumn = paramIndex.getColumns();
    
    boolean[] arrayOfBoolean = new boolean[arrayOfColumn.length];
    
    int i = 0;
    label130:
    for (int j = this.expressions.size(); i < j; i++) {
      if (this.groupByExpression[i] != 0)
      {
        Expression localExpression = ((Expression)this.expressions.get(i)).getNonAliasExpression();
        if (!(localExpression instanceof ExpressionColumn)) {
          return false;
        }
        ExpressionColumn localExpressionColumn = (ExpressionColumn)localExpression;
        for (int k = 0; k < arrayOfColumn.length; k++) {
          if ((paramTableFilter == localExpressionColumn.getTableFilter()) && 
            (arrayOfColumn[k].equals(localExpressionColumn.getColumn())))
          {
            arrayOfBoolean[k] = true;
            break label130;
          }
        }
        return false;
      }
    }
    for (i = 1; i < arrayOfBoolean.length; i++) {
      if ((arrayOfBoolean[(i - 1)] == 0) && (arrayOfBoolean[i] != 0)) {
        return false;
      }
    }
    return true;
  }
  
  private int getGroupByExpressionCount()
  {
    if (this.groupByExpression == null) {
      return 0;
    }
    int i = 0;
    for (int m : this.groupByExpression) {
      if (m != 0) {
        i++;
      }
    }
    return i;
  }
  
  private void queryGroup(int paramInt, LocalResult paramLocalResult)
  {
    ValueHashMap localValueHashMap = ValueHashMap.newInstance();
    
    int i = 0;
    setCurrentRowNumber(0);
    this.currentGroup = null;
    ValueArray localValueArray1 = ValueArray.get(new Value[0]);
    int j = getSampleSizeValue(this.session);
    Object localObject3;
    while (this.topTableFilter.next())
    {
      setCurrentRowNumber(i + 1);
      if ((this.condition == null) || (Boolean.TRUE.equals(this.condition.getBooleanValue(this.session))))
      {
        i++;
        if (this.groupIndex == null)
        {
          localObject1 = localValueArray1;
        }
        else
        {
          localObject2 = new Value[this.groupIndex.length];
          for (k = 0; k < this.groupIndex.length; k++)
          {
            m = this.groupIndex[k];
            localObject3 = (Expression)this.expressions.get(m);
            localObject2[k] = ((Expression)localObject3).getValue(this.session);
          }
          localObject1 = ValueArray.get((Value[])localObject2);
        }
        localObject2 = (HashMap)localValueHashMap.get((Value)localObject1);
        if (localObject2 == null)
        {
          localObject2 = new HashMap();
          localValueHashMap.put((Value)localObject1, localObject2);
        }
        this.currentGroup = ((HashMap)localObject2);
        this.currentGroupRowId += 1;
        int k = paramInt;
        for (int m = 0; m < k; m++) {
          if ((this.groupByExpression == null) || (this.groupByExpression[m] == 0))
          {
            localObject3 = (Expression)this.expressions.get(m);
            ((Expression)localObject3).updateAggregate(this.session);
          }
        }
        if ((j > 0) && (i >= j)) {
          break;
        }
      }
    }
    if ((this.groupIndex == null) && (localValueHashMap.size() == 0)) {
      localValueHashMap.put(localValueArray1, new HashMap());
    }
    Object localObject1 = localValueHashMap.keys();
    for (Object localObject2 = ((ArrayList)localObject1).iterator(); ((Iterator)localObject2).hasNext();)
    {
      Value localValue = (Value)((Iterator)localObject2).next();
      ValueArray localValueArray2 = (ValueArray)localValue;
      this.currentGroup = ((HashMap)localValueHashMap.get(localValueArray2));
      localObject3 = localValueArray2.getList();
      Value[] arrayOfValue = new Value[paramInt];
      for (int n = 0; (this.groupIndex != null) && (n < this.groupIndex.length); n++) {
        arrayOfValue[this.groupIndex[n]] = localObject3[n];
      }
      for (n = 0; n < paramInt; n++) {
        if ((this.groupByExpression == null) || (this.groupByExpression[n] == 0))
        {
          Expression localExpression = (Expression)this.expressions.get(n);
          arrayOfValue[n] = localExpression.getValue(this.session);
        }
      }
      if (!isHavingNullOrFalse(arrayOfValue))
      {
        arrayOfValue = keepOnlyDistinct(arrayOfValue, paramInt);
        paramLocalResult.addRow(arrayOfValue);
      }
    }
  }
  
  private Index getSortIndex()
  {
    if (this.sort == null) {
      return null;
    }
    ArrayList localArrayList1 = New.arrayList();
    int k;
    Object localObject2;
    for (k : this.sort.getQueryColumnIndexes())
    {
      if ((k < 0) || (k >= this.expressions.size())) {
        throw DbException.getInvalidValueException("ORDER BY", Integer.valueOf(k + 1));
      }
      Expression localExpression = (Expression)this.expressions.get(k);
      localExpression = localExpression.getNonAliasExpression();
      if (!localExpression.isConstant())
      {
        if (!(localExpression instanceof ExpressionColumn)) {
          return null;
        }
        localObject2 = (ExpressionColumn)localExpression;
        if (((ExpressionColumn)localObject2).getTableFilter() != this.topTableFilter) {
          return null;
        }
        localArrayList1.add(((ExpressionColumn)localObject2).getColumn());
      }
    }
    ??? = (Column[])localArrayList1.toArray(new Column[localArrayList1.size()]);
    int[] arrayOfInt = this.sort.getSortTypes();
    if (???.length == 0) {
      return this.topTableFilter.getTable().getScanIndex(this.session);
    }
    ArrayList localArrayList2 = this.topTableFilter.getTable().getIndexes();
    if (localArrayList2 != null)
    {
      k = 0;
      for (int m = localArrayList2.size(); k < m; k++)
      {
        localObject2 = (Index)localArrayList2.get(k);
        if (((Index)localObject2).getCreateSQL() != null) {
          if (!((Index)localObject2).getIndexType().isHash())
          {
            IndexColumn[] arrayOfIndexColumn = ((Index)localObject2).getIndexColumns();
            if (arrayOfIndexColumn.length >= ???.length)
            {
              int n = 1;
              for (int i1 = 0; i1 < ???.length; i1++)
              {
                IndexColumn localIndexColumn = arrayOfIndexColumn[i1];
                Object localObject3 = ???[i1];
                if (localIndexColumn.column != localObject3)
                {
                  n = 0;
                  break;
                }
                if (localIndexColumn.sortType != arrayOfInt[i1])
                {
                  n = 0;
                  break;
                }
              }
              if (n != 0) {
                return (Index)localObject2;
              }
            }
          }
        }
      }
    }
    if ((???.length == 1) && (???[0].getColumnId() == -1))
    {
      Index localIndex = this.topTableFilter.getTable().getScanIndex(this.session);
      if (localIndex.isRowIdIndex()) {
        return localIndex;
      }
    }
    return null;
  }
  
  private void queryDistinct(ResultTarget paramResultTarget, long paramLong)
  {
    if ((paramLong > 0L) && (this.offsetExpr != null))
    {
      i = this.offsetExpr.getValue(this.session).getInt();
      if (i > 0) {
        paramLong += i;
      }
    }
    int i = 0;
    setCurrentRowNumber(0);
    Index localIndex = this.topTableFilter.getIndex();
    SearchRow localSearchRow1 = null;
    int j = localIndex.getColumns()[0].getColumnId();
    int k = getSampleSizeValue(this.session);
    for (;;)
    {
      setCurrentRowNumber(i + 1);
      Cursor localCursor = localIndex.findNext(this.session, localSearchRow1, null);
      if (!localCursor.next()) {
        break;
      }
      SearchRow localSearchRow2 = localCursor.getSearchRow();
      Value localValue = localSearchRow2.getValue(j);
      if (localSearchRow1 == null) {
        localSearchRow1 = this.topTableFilter.getTable().getTemplateSimpleRow(true);
      }
      localSearchRow1.setValue(j, localValue);
      Value[] arrayOfValue = { localValue };
      paramResultTarget.addRow(arrayOfValue);
      i++;
      if (((this.sort == null) || (this.sortUsingIndex)) && (paramLong > 0L) && (i >= paramLong)) {
        break;
      }
      if ((k > 0) && (i >= k)) {
        break;
      }
    }
  }
  
  private void queryFlat(int paramInt, ResultTarget paramResultTarget, long paramLong)
  {
    if ((paramLong > 0L) && (this.offsetExpr != null))
    {
      i = this.offsetExpr.getValue(this.session).getInt();
      if (i > 0) {
        paramLong += i;
      }
    }
    int i = 0;
    setCurrentRowNumber(0);
    ArrayList localArrayList = null;
    if (this.isForUpdateMvcc) {
      localArrayList = New.arrayList();
    }
    int j = getSampleSizeValue(this.session);
    while (this.topTableFilter.next())
    {
      setCurrentRowNumber(i + 1);
      if ((this.condition == null) || (Boolean.TRUE.equals(this.condition.getBooleanValue(this.session))))
      {
        Value[] arrayOfValue = new Value[paramInt];
        for (int k = 0; k < paramInt; k++)
        {
          Expression localExpression = (Expression)this.expressions.get(k);
          arrayOfValue[k] = localExpression.getValue(this.session);
        }
        if (this.isForUpdateMvcc) {
          this.topTableFilter.lockRowAdd(localArrayList);
        }
        paramResultTarget.addRow(arrayOfValue);
        i++;
        if (((this.sort == null) || (this.sortUsingIndex)) && (paramLong > 0L) && (paramResultTarget.getRowCount() >= paramLong)) {
          break;
        }
        if ((j > 0) && (i >= j)) {
          break;
        }
      }
    }
    if (this.isForUpdateMvcc) {
      this.topTableFilter.lockRows(localArrayList);
    }
  }
  
  private void queryQuick(int paramInt, ResultTarget paramResultTarget)
  {
    Value[] arrayOfValue = new Value[paramInt];
    for (int i = 0; i < paramInt; i++)
    {
      Expression localExpression = (Expression)this.expressions.get(i);
      arrayOfValue[i] = localExpression.getValue(this.session);
    }
    paramResultTarget.addRow(arrayOfValue);
  }
  
  public ResultInterface queryMeta()
  {
    LocalResult localLocalResult = new LocalResult(this.session, this.expressionArray, this.visibleColumnCount);
    
    localLocalResult.done();
    return localLocalResult;
  }
  
  protected LocalResult queryWithoutCache(int paramInt, ResultTarget paramResultTarget)
  {
    int i = paramInt == 0 ? -1 : paramInt;
    if (this.limitExpr != null)
    {
      Value localValue = this.limitExpr.getValue(this.session);
      int k = localValue == ValueNull.INSTANCE ? -1 : localValue.getInt();
      if (i < 0) {
        i = k;
      } else if (k >= 0) {
        i = Math.min(k, i);
      }
    }
    int j = this.expressions.size();
    LocalResult localLocalResult = null;
    if ((paramResultTarget == null) || (!this.session.getDatabase().getSettings().optimizeInsertFromSelect)) {
      localLocalResult = createLocalResult(localLocalResult);
    }
    if ((this.sort != null) && ((!this.sortUsingIndex) || (this.distinct)))
    {
      localLocalResult = createLocalResult(localLocalResult);
      localLocalResult.setSortOrder(this.sort);
    }
    if ((this.distinct) && (!this.isDistinctQuery))
    {
      localLocalResult = createLocalResult(localLocalResult);
      localLocalResult.setDistinct();
    }
    if (this.randomAccessResult) {
      localLocalResult = createLocalResult(localLocalResult);
    }
    if ((this.isGroupQuery) && (!this.isGroupSortedQuery)) {
      localLocalResult = createLocalResult(localLocalResult);
    }
    if ((i >= 0) || (this.offsetExpr != null)) {
      localLocalResult = createLocalResult(localLocalResult);
    }
    this.topTableFilter.startQuery(this.session);
    this.topTableFilter.reset();
    boolean bool = (this.isForUpdate) && (!this.isForUpdateMvcc);
    if (this.isForUpdateMvcc)
    {
      if (this.isGroupQuery) {
        throw DbException.getUnsupportedException("FOR UPDATE && GROUP");
      }
      if (this.distinct) {
        throw DbException.getUnsupportedException("FOR UPDATE && DISTINCT");
      }
      if (this.isQuickAggregateQuery) {
        throw DbException.getUnsupportedException("FOR UPDATE && AGGREGATE");
      }
      if (this.topTableFilter.getJoin() != null) {
        throw DbException.getUnsupportedException("FOR UPDATE && JOIN");
      }
    }
    this.topTableFilter.lock(this.session, bool, bool);
    ResultTarget localResultTarget = localLocalResult != null ? localLocalResult : paramResultTarget;
    if (i != 0) {
      if (this.isQuickAggregateQuery) {
        queryQuick(j, localResultTarget);
      } else if (this.isGroupQuery)
      {
        if (this.isGroupSortedQuery) {
          queryGroupSorted(j, localResultTarget);
        } else {
          queryGroup(j, localLocalResult);
        }
      }
      else if (this.isDistinctQuery) {
        queryDistinct(localResultTarget, i);
      } else {
        queryFlat(j, localResultTarget, i);
      }
    }
    if (this.offsetExpr != null) {
      localLocalResult.setOffset(this.offsetExpr.getValue(this.session).getInt());
    }
    if (i >= 0) {
      localLocalResult.setLimit(i);
    }
    if (localLocalResult != null)
    {
      localLocalResult.done();
      if (paramResultTarget != null)
      {
        while (localLocalResult.next()) {
          paramResultTarget.addRow(localLocalResult.currentRow());
        }
        localLocalResult.close();
        return null;
      }
      return localLocalResult;
    }
    return null;
  }
  
  private LocalResult createLocalResult(LocalResult paramLocalResult)
  {
    return paramLocalResult != null ? paramLocalResult : new LocalResult(this.session, this.expressionArray, this.visibleColumnCount);
  }
  
  private void expandColumnList()
  {
    Database localDatabase = this.session.getDatabase();
    for (int i = 0; i < this.expressions.size(); i++)
    {
      Expression localExpression = (Expression)this.expressions.get(i);
      if (localExpression.isWildcard())
      {
        String str1 = localExpression.getSchemaName();
        String str2 = localExpression.getTableAlias();
        Object localObject2;
        Object localObject3;
        Object localObject4;
        if (str2 == null)
        {
          int j = i;
          this.expressions.remove(i);
          for (localObject2 = this.filters.iterator(); ((Iterator)localObject2).hasNext();)
          {
            localObject3 = (TableFilter)((Iterator)localObject2).next();
            localObject4 = new Wildcard(((TableFilter)localObject3).getTable().getSchema().getName(), ((TableFilter)localObject3).getTableAlias());
            
            this.expressions.add(i++, localObject4);
          }
          i = j - 1;
        }
        else
        {
          Object localObject1 = null;
          for (localObject2 = this.filters.iterator(); ((Iterator)localObject2).hasNext();)
          {
            localObject3 = (TableFilter)((Iterator)localObject2).next();
            if ((localDatabase.equalsIdentifiers(str2, ((TableFilter)localObject3).getTableAlias())) && (
              (str1 == null) || (localDatabase.equalsIdentifiers(str1, ((TableFilter)localObject3).getSchemaName()))))
            {
              localObject1 = localObject3;
              break;
            }
          }
          if (localObject1 == null) {
            throw DbException.get(42102, str2);
          }
          localObject2 = ((TableFilter)localObject1).getTable();
          localObject3 = ((TableFilter)localObject1).getTableAlias();
          this.expressions.remove(i);
          localObject4 = ((Table)localObject2).getColumns();
          for (Column localColumn : localObject4) {
            if (!((TableFilter)localObject1).isNaturalJoinColumn(localColumn))
            {
              ExpressionColumn localExpressionColumn = new ExpressionColumn(this.session.getDatabase(), null, (String)localObject3, localColumn.getName());
              
              this.expressions.add(i++, localExpressionColumn);
            }
          }
          i--;
        }
      }
    }
  }
  
  public void init()
  {
    if ((SysProperties.CHECK) && (this.checkInit)) {
      DbException.throwInternalError();
    }
    expandColumnList();
    this.visibleColumnCount = this.expressions.size();
    ArrayList localArrayList;
    if ((this.orderList != null) || (this.group != null))
    {
      localArrayList = New.arrayList();
      for (int i = 0; i < this.visibleColumnCount; i++)
      {
        Expression localExpression1 = (Expression)this.expressions.get(i);
        localExpression1 = localExpression1.getNonAliasExpression();
        String str1 = localExpression1.getSQL();
        localArrayList.add(str1);
      }
    }
    else
    {
      localArrayList = null;
    }
    if (this.orderList != null) {
      initOrder(this.session, this.expressions, localArrayList, this.orderList, this.visibleColumnCount, this.distinct, this.filters);
    }
    this.distinctColumnCount = this.expressions.size();
    if (this.having != null)
    {
      this.expressions.add(this.having);
      this.havingIndex = (this.expressions.size() - 1);
      this.having = null;
    }
    else
    {
      this.havingIndex = -1;
    }
    Database localDatabase = this.session.getDatabase();
    if (this.group != null)
    {
      int j = this.group.size();
      int k = localArrayList.size();
      this.groupIndex = new int[j];
      int i2;
      for (int m = 0; m < j; m++)
      {
        Expression localExpression2 = (Expression)this.group.get(m);
        String str2 = localExpression2.getSQL();
        i2 = -1;
        Object localObject3;
        for (int i3 = 0; i3 < k; i3++)
        {
          localObject3 = (String)localArrayList.get(i3);
          if (localDatabase.equalsIdentifiers((String)localObject3, str2))
          {
            i2 = i3;
            break;
          }
        }
        if (i2 < 0) {
          for (i3 = 0; i3 < k; i3++)
          {
            localObject3 = (Expression)this.expressions.get(i3);
            if (localDatabase.equalsIdentifiers(str2, ((Expression)localObject3).getAlias()))
            {
              i2 = i3;
              break;
            }
          }
        }
        if (i2 < 0)
        {
          i3 = this.expressions.size();
          this.groupIndex[m] = i3;
          this.expressions.add(localExpression2);
        }
        else
        {
          this.groupIndex[m] = i2;
        }
      }
      this.groupByExpression = new boolean[this.expressions.size()];
      for (i2 : this.groupIndex) {
        this.groupByExpression[i2] = true;
      }
      this.group = null;
    }
    for (Object localObject1 = this.filters.iterator(); ((Iterator)localObject1).hasNext();)
    {
      localObject2 = (TableFilter)((Iterator)localObject1).next();
      mapColumns((ColumnResolver)localObject2, 0);
    }
    Object localObject2;
    if (this.havingIndex >= 0)
    {
      localObject1 = (Expression)this.expressions.get(this.havingIndex);
      localObject2 = new SelectListColumnResolver(this);
      ((Expression)localObject1).mapColumns((ColumnResolver)localObject2, 0);
    }
    this.checkInit = true;
  }
  
  public void prepare()
  {
    if (this.isPrepared) {
      return;
    }
    if ((SysProperties.CHECK) && (!this.checkInit)) {
      DbException.throwInternalError("not initialized");
    }
    if (this.orderList != null)
    {
      this.sort = prepareOrder(this.orderList, this.expressions.size());
      this.orderList = null;
    }
    Object localObject2;
    for (int i = 0; i < this.expressions.size(); i++)
    {
      localObject2 = (Expression)this.expressions.get(i);
      this.expressions.set(i, ((Expression)localObject2).optimize(this.session));
    }
    Object localObject1;
    if (this.condition != null)
    {
      this.condition = this.condition.optimize(this.session);
      for (localObject1 = this.filters.iterator(); ((Iterator)localObject1).hasNext();)
      {
        localObject2 = (TableFilter)((Iterator)localObject1).next();
        if ((!((TableFilter)localObject2).isJoinOuter()) && (!((TableFilter)localObject2).isJoinOuterIndirect())) {
          this.condition.createIndexConditions(this.session, (TableFilter)localObject2);
        }
      }
    }
    if ((this.isGroupQuery) && (this.groupIndex == null) && (this.havingIndex < 0) && (this.filters.size() == 1)) {
      if (this.condition == null)
      {
        localObject1 = ((TableFilter)this.filters.get(0)).getTable();
        localObject2 = ExpressionVisitor.getOptimizableVisitor((Table)localObject1);
        
        this.isQuickAggregateQuery = isEverything((ExpressionVisitor)localObject2);
      }
    }
    this.cost = preparePlan();
    Object localObject3;
    int k;
    if ((this.distinct) && (this.session.getDatabase().getSettings().optimizeDistinct) && (!this.isGroupQuery) && (this.filters.size() == 1) && (this.expressions.size() == 1) && (this.condition == null))
    {
      localObject1 = (Expression)this.expressions.get(0);
      localObject1 = ((Expression)localObject1).getNonAliasExpression();
      if ((localObject1 instanceof ExpressionColumn))
      {
        localObject2 = ((ExpressionColumn)localObject1).getColumn();
        int j = ((Column)localObject2).getSelectivity();
        localObject3 = this.topTableFilter.getTable().getIndexForColumn((Column)localObject2);
        if ((localObject3 != null) && (j != 50) && (j < 20))
        {
          k = localObject3.getIndexColumns()[0].sortType == 0 ? 1 : 0;
          
          Index localIndex = this.topTableFilter.getIndex();
          if ((((Index)localObject3).canFindNext()) && (k != 0) && ((localIndex == null) || (localIndex.getIndexType().isScan()) || (localObject3 == localIndex)))
          {
            IndexType localIndexType = ((Index)localObject3).getIndexType();
            if ((!localIndexType.isHash()) && ((!localIndexType.isUnique()) || (((Index)localObject3).getColumns().length > 1)))
            {
              this.topTableFilter.setIndex((Index)localObject3);
              this.isDistinctQuery = true;
            }
          }
        }
      }
    }
    if ((this.sort != null) && (!this.isQuickAggregateQuery) && (!this.isGroupQuery))
    {
      localObject1 = getSortIndex();
      if (localObject1 != null)
      {
        localObject2 = this.topTableFilter.getIndex();
        if ((((Index)localObject2).getIndexType().isScan()) || (localObject2 == localObject1))
        {
          this.topTableFilter.setIndex((Index)localObject1);
          if (!this.topTableFilter.hasInComparisons()) {
            this.sortUsingIndex = true;
          }
        }
        else if (((Index)localObject1).getIndexColumns().length >= ((Index)localObject2).getIndexColumns().length)
        {
          IndexColumn[] arrayOfIndexColumn = ((Index)localObject1).getIndexColumns();
          localObject3 = ((Index)localObject2).getIndexColumns();
          k = 0;
          for (int m = 0; m < localObject3.length; m++)
          {
            if (arrayOfIndexColumn[m].column != localObject3[m].column)
            {
              k = 0;
              break;
            }
            if (arrayOfIndexColumn[m].sortType != localObject3[m].sortType) {
              k = 1;
            }
          }
          if (k != 0)
          {
            this.topTableFilter.setIndex((Index)localObject1);
            this.sortUsingIndex = true;
          }
        }
      }
    }
    if ((!this.isQuickAggregateQuery) && (this.isGroupQuery) && (getGroupByExpressionCount() > 0))
    {
      localObject1 = getGroupSortedIndex();
      localObject2 = this.topTableFilter.getIndex();
      if ((localObject1 != null) && ((((Index)localObject2).getIndexType().isScan()) || (localObject2 == localObject1)))
      {
        this.topTableFilter.setIndex((Index)localObject1);
        this.isGroupSortedQuery = true;
      }
    }
    this.expressionArray = new Expression[this.expressions.size()];
    this.expressions.toArray(this.expressionArray);
    this.isPrepared = true;
  }
  
  public double getCost()
  {
    return this.cost;
  }
  
  public HashSet<Table> getTables()
  {
    HashSet localHashSet = New.hashSet();
    for (TableFilter localTableFilter : this.filters) {
      localHashSet.add(localTableFilter.getTable());
    }
    return localHashSet;
  }
  
  public void fireBeforeSelectTriggers()
  {
    int i = 0;
    for (int j = this.filters.size(); i < j; i++)
    {
      TableFilter localTableFilter = (TableFilter)this.filters.get(i);
      localTableFilter.getTable().fire(this.session, 8, true);
    }
  }
  
  private double preparePlan()
  {
    TableFilter[] arrayOfTableFilter = (TableFilter[])this.topFilters.toArray(new TableFilter[this.topFilters.size()]);
    for (Object localObject2 : arrayOfTableFilter) {
      ((TableFilter)localObject2).setFullCondition(this.condition);
    }
    ??? = new Optimizer(arrayOfTableFilter, this.condition, this.session);
    ((Optimizer)???).optimize();
    this.topTableFilter = ((Optimizer)???).getTopFilter();
    double d = ((Optimizer)???).getCost();
    
    setEvaluatableRecursive(this.topTableFilter);
    
    this.topTableFilter.prepare();
    return d;
  }
  
  private void setEvaluatableRecursive(TableFilter paramTableFilter)
  {
    for (; paramTableFilter != null; paramTableFilter = paramTableFilter.getJoin())
    {
      paramTableFilter.setEvaluatable(paramTableFilter, true);
      if (this.condition != null) {
        this.condition.setEvaluatable(paramTableFilter, true);
      }
      TableFilter localTableFilter = paramTableFilter.getNestedJoin();
      if (localTableFilter != null) {
        setEvaluatableRecursive(localTableFilter);
      }
      Expression localExpression1 = paramTableFilter.getJoinCondition();
      if ((localExpression1 != null) && 
        (!localExpression1.isEverything(ExpressionVisitor.EVALUATABLE_VISITOR))) {
        if (this.session.getDatabase().getSettings().nestedJoins)
        {
          localExpression1 = localExpression1.optimize(this.session);
          if ((!paramTableFilter.isJoinOuter()) && (!paramTableFilter.isJoinOuterIndirect()))
          {
            paramTableFilter.removeJoinCondition();
            addCondition(localExpression1);
          }
        }
        else
        {
          if (paramTableFilter.isJoinOuter())
          {
            localExpression1 = localExpression1.optimize(this.session);
            
            throw DbException.get(90136, localExpression1.getSQL());
          }
          paramTableFilter.removeJoinCondition();
          
          localExpression1 = localExpression1.optimize(this.session);
          addCondition(localExpression1);
        }
      }
      localExpression1 = paramTableFilter.getFilterCondition();
      if ((localExpression1 != null) && 
        (!localExpression1.isEverything(ExpressionVisitor.EVALUATABLE_VISITOR)))
      {
        paramTableFilter.removeFilterCondition();
        addCondition(localExpression1);
      }
      for (Expression localExpression2 : this.expressions) {
        localExpression2.setEvaluatable(paramTableFilter, true);
      }
    }
  }
  
  public String getPlanSQL()
  {
    Expression[] arrayOfExpression = (Expression[])this.expressions.toArray(new Expression[this.expressions.size()]);
    
    StatementBuilder localStatementBuilder = new StatementBuilder("SELECT");
    if (this.distinct) {
      localStatementBuilder.append(" DISTINCT");
    }
    for (int i = 0; i < this.visibleColumnCount; i++)
    {
      localStatementBuilder.appendExceptFirst(",");
      localStatementBuilder.append('\n');
      localStatementBuilder.append(StringUtils.indent(arrayOfExpression[i].getSQL(), 4, false));
    }
    localStatementBuilder.append("\nFROM ");
    TableFilter localTableFilter1 = this.topTableFilter;
    int j;
    if (localTableFilter1 != null)
    {
      localStatementBuilder.resetCount();
      j = 0;
      do
      {
        localStatementBuilder.appendExceptFirst("\n");
        localStatementBuilder.append(localTableFilter1.getPlanSQL(j++ > 0));
        localTableFilter1 = localTableFilter1.getJoin();
      } while (localTableFilter1 != null);
    }
    else
    {
      localStatementBuilder.resetCount();
      j = 0;
      for (TableFilter localTableFilter2 : this.topFilters) {
        do
        {
          localStatementBuilder.appendExceptFirst("\n");
          localStatementBuilder.append(localTableFilter2.getPlanSQL(j++ > 0));
          localTableFilter2 = localTableFilter2.getJoin();
        } while (localTableFilter2 != null);
      }
    }
    if (this.condition != null) {
      localStatementBuilder.append("\nWHERE ").append(StringUtils.unEnclose(this.condition.getSQL()));
    }
    if (this.groupIndex != null)
    {
      localStatementBuilder.append("\nGROUP BY ");
      localStatementBuilder.resetCount();
      for (int n : this.groupIndex)
      {
        Expression localExpression = arrayOfExpression[n];
        localExpression = localExpression.getNonAliasExpression();
        localStatementBuilder.appendExceptFirst(", ");
        localStatementBuilder.append(StringUtils.unEnclose(localExpression.getSQL()));
      }
    }
    if (this.group != null)
    {
      localStatementBuilder.append("\nGROUP BY ");
      localStatementBuilder.resetCount();
      for (??? = this.group.iterator(); ((Iterator)???).hasNext();)
      {
        localObject2 = (Expression)((Iterator)???).next();
        localStatementBuilder.appendExceptFirst(", ");
        localStatementBuilder.append(StringUtils.unEnclose(((Expression)localObject2).getSQL()));
      }
    }
    Object localObject2;
    if (this.having != null)
    {
      ??? = this.having;
      localStatementBuilder.append("\nHAVING ").append(StringUtils.unEnclose(((Expression)???).getSQL()));
    }
    else if (this.havingIndex >= 0)
    {
      ??? = arrayOfExpression[this.havingIndex];
      localStatementBuilder.append("\nHAVING ").append(StringUtils.unEnclose(((Expression)???).getSQL()));
    }
    if (this.sort != null) {
      localStatementBuilder.append("\nORDER BY ").append(this.sort.getSQL(arrayOfExpression, this.visibleColumnCount));
    }
    if (this.orderList != null)
    {
      localStatementBuilder.append("\nORDER BY ");
      localStatementBuilder.resetCount();
      for (??? = this.orderList.iterator(); ((Iterator)???).hasNext();)
      {
        localObject2 = (SelectOrderBy)((Iterator)???).next();
        localStatementBuilder.appendExceptFirst(", ");
        localStatementBuilder.append(StringUtils.unEnclose(((SelectOrderBy)localObject2).getSQL()));
      }
    }
    if (this.limitExpr != null)
    {
      localStatementBuilder.append("\nLIMIT ").append(StringUtils.unEnclose(this.limitExpr.getSQL()));
      if (this.offsetExpr != null) {
        localStatementBuilder.append(" OFFSET ").append(StringUtils.unEnclose(this.offsetExpr.getSQL()));
      }
    }
    if (this.sampleSizeExpr != null) {
      localStatementBuilder.append("\nSAMPLE_SIZE ").append(StringUtils.unEnclose(this.sampleSizeExpr.getSQL()));
    }
    if (this.isForUpdate) {
      localStatementBuilder.append("\nFOR UPDATE");
    }
    if (this.isQuickAggregateQuery) {
      localStatementBuilder.append("\n/* direct lookup */");
    }
    if (this.isDistinctQuery) {
      localStatementBuilder.append("\n/* distinct */");
    }
    if (this.sortUsingIndex) {
      localStatementBuilder.append("\n/* index sorted */");
    }
    if ((this.isGroupQuery) && 
      (this.isGroupSortedQuery)) {
      localStatementBuilder.append("\n/* group sorted */");
    }
    return localStatementBuilder.toString();
  }
  
  public void setHaving(Expression paramExpression)
  {
    this.having = paramExpression;
  }
  
  public Expression getHaving()
  {
    return this.having;
  }
  
  public int getColumnCount()
  {
    return this.visibleColumnCount;
  }
  
  public TableFilter getTopTableFilter()
  {
    return this.topTableFilter;
  }
  
  public ArrayList<Expression> getExpressions()
  {
    return this.expressions;
  }
  
  public void setForUpdate(boolean paramBoolean)
  {
    this.isForUpdate = paramBoolean;
    if ((this.session.getDatabase().getSettings().selectForUpdateMvcc) && (this.session.getDatabase().isMultiVersion())) {
      this.isForUpdateMvcc = paramBoolean;
    }
  }
  
  public void mapColumns(ColumnResolver paramColumnResolver, int paramInt)
  {
    for (Expression localExpression : this.expressions) {
      localExpression.mapColumns(paramColumnResolver, paramInt);
    }
    if (this.condition != null) {
      this.condition.mapColumns(paramColumnResolver, paramInt);
    }
  }
  
  public void setEvaluatable(TableFilter paramTableFilter, boolean paramBoolean)
  {
    for (Expression localExpression : this.expressions) {
      localExpression.setEvaluatable(paramTableFilter, paramBoolean);
    }
    if (this.condition != null) {
      this.condition.setEvaluatable(paramTableFilter, paramBoolean);
    }
  }
  
  public boolean isQuickAggregateQuery()
  {
    return this.isQuickAggregateQuery;
  }
  
  public void addGlobalCondition(Parameter paramParameter, int paramInt1, int paramInt2)
  {
    addParameter(paramParameter);
    
    Expression localExpression = (Expression)this.expressions.get(paramInt1);
    localExpression = localExpression.getNonAliasExpression();
    if (localExpression.isEverything(ExpressionVisitor.QUERY_COMPARABLE_VISITOR)) {
      localObject = new Comparison(this.session, paramInt2, localExpression, paramParameter);
    } else {
      localObject = new Comparison(this.session, 16, paramParameter, paramParameter);
    }
    Object localObject = ((Expression)localObject).optimize(this.session);
    int i = 1;
    if (this.isGroupQuery)
    {
      i = 0;
      for (int j = 0; (this.groupIndex != null) && (j < this.groupIndex.length); j++) {
        if (this.groupIndex[j] == paramInt1)
        {
          i = 1;
          break;
        }
      }
      if (i == 0)
      {
        if (this.havingIndex >= 0) {
          this.having = ((Expression)this.expressions.get(this.havingIndex));
        }
        if (this.having == null) {
          this.having = ((Expression)localObject);
        } else {
          this.having = new ConditionAndOr(0, this.having, (Expression)localObject);
        }
      }
    }
    if (i != 0) {
      if (this.condition == null) {
        this.condition = ((Expression)localObject);
      } else {
        this.condition = new ConditionAndOr(0, this.condition, (Expression)localObject);
      }
    }
  }
  
  public void updateAggregate(Session paramSession)
  {
    for (Expression localExpression : this.expressions) {
      localExpression.updateAggregate(paramSession);
    }
    if (this.condition != null) {
      this.condition.updateAggregate(paramSession);
    }
    if (this.having != null) {
      this.having.updateAggregate(paramSession);
    }
  }
  
  public boolean isEverything(ExpressionVisitor paramExpressionVisitor)
  {
    int i;
    TableFilter localTableFilter;
    switch (paramExpressionVisitor.getType())
    {
    case 2: 
      if (this.isForUpdate) {
        return false;
      }
      i = 0;
      for (j = this.filters.size(); i < j; i++)
      {
        localTableFilter = (TableFilter)this.filters.get(i);
        if (!localTableFilter.getTable().isDeterministic()) {
          return false;
        }
      }
      break;
    case 4: 
      i = 0;
      for (j = this.filters.size(); i < j; i++)
      {
        localTableFilter = (TableFilter)this.filters.get(i);
        long l = localTableFilter.getTable().getMaxDataModificationId();
        paramExpressionVisitor.addDataModificationId(l);
      }
      break;
    case 3: 
      if (!this.session.getDatabase().getSettings().optimizeEvaluatableSubqueries) {
        return false;
      }
      break;
    case 7: 
      i = 0;
      for (j = this.filters.size(); i < j; i++)
      {
        localTableFilter = (TableFilter)this.filters.get(i);
        Table localTable = localTableFilter.getTable();
        paramExpressionVisitor.addDependency(localTable);
        localTable.addDependencies(paramExpressionVisitor.getDependencies());
      }
      break;
    }
    ExpressionVisitor localExpressionVisitor = paramExpressionVisitor.incrementQueryLevel(1);
    int j = 1;
    int k = 0;
    for (int m = this.expressions.size(); k < m; k++)
    {
      Expression localExpression = (Expression)this.expressions.get(k);
      if (!localExpression.isEverything(localExpressionVisitor))
      {
        j = 0;
        break;
      }
    }
    if ((j != 0) && (this.condition != null) && (!this.condition.isEverything(localExpressionVisitor))) {
      j = 0;
    }
    if ((j != 0) && (this.having != null) && (!this.having.isEverything(localExpressionVisitor))) {
      j = 0;
    }
    return j;
  }
  
  public boolean isReadOnly()
  {
    return isEverything(ExpressionVisitor.READONLY_VISITOR);
  }
  
  public boolean isCacheable()
  {
    return !this.isForUpdate;
  }
  
  public int getType()
  {
    return 66;
  }
  
  public boolean allowGlobalConditions()
  {
    if ((this.offsetExpr == null) && ((this.limitExpr == null) || (this.sort == null))) {
      return true;
    }
    return false;
  }
  
  public SortOrder getSortOrder()
  {
    return this.sort;
  }
}
