package org.h2.expression;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import org.h2.command.dml.Select;
import org.h2.command.dml.SelectOrderBy;
import org.h2.engine.Session;
import org.h2.index.Cursor;
import org.h2.index.Index;
import org.h2.message.DbException;
import org.h2.result.SearchRow;
import org.h2.result.SortOrder;
import org.h2.table.Column;
import org.h2.table.ColumnResolver;
import org.h2.table.IndexColumn;
import org.h2.table.Table;
import org.h2.table.TableFilter;
import org.h2.util.New;
import org.h2.util.StatementBuilder;
import org.h2.util.StringUtils;
import org.h2.value.DataType;
import org.h2.value.Value;
import org.h2.value.ValueArray;
import org.h2.value.ValueLong;
import org.h2.value.ValueNull;
import org.h2.value.ValueString;

public class Aggregate
  extends Expression
{
  public static final int COUNT_ALL = 0;
  public static final int COUNT = 1;
  public static final int GROUP_CONCAT = 2;
  static final int SUM = 3;
  static final int MIN = 4;
  static final int MAX = 5;
  static final int AVG = 6;
  static final int STDDEV_POP = 7;
  static final int STDDEV_SAMP = 8;
  static final int VAR_POP = 9;
  static final int VAR_SAMP = 10;
  static final int BOOL_OR = 11;
  static final int BOOL_AND = 12;
  static final int SELECTIVITY = 13;
  static final int HISTOGRAM = 14;
  private static final HashMap<String, Integer> AGGREGATES = ;
  private final int type;
  private final Select select;
  private final boolean distinct;
  private Expression on;
  private Expression groupConcatSeparator;
  private ArrayList<SelectOrderBy> groupConcatOrderList;
  private SortOrder groupConcatSort;
  private int dataType;
  private int scale;
  private long precision;
  private int displaySize;
  private int lastGroupRowId;
  
  public Aggregate(int paramInt, Expression paramExpression, Select paramSelect, boolean paramBoolean)
  {
    this.type = paramInt;
    this.on = paramExpression;
    this.select = paramSelect;
    this.distinct = paramBoolean;
  }
  
  static
  {
    addAggregate("COUNT", 1);
    addAggregate("SUM", 3);
    addAggregate("MIN", 4);
    addAggregate("MAX", 5);
    addAggregate("AVG", 6);
    addAggregate("GROUP_CONCAT", 2);
    addAggregate("STDDEV_SAMP", 8);
    addAggregate("STDDEV", 8);
    addAggregate("STDDEV_POP", 7);
    addAggregate("STDDEVP", 7);
    addAggregate("VAR_POP", 9);
    addAggregate("VARP", 9);
    addAggregate("VAR_SAMP", 10);
    addAggregate("VAR", 10);
    addAggregate("VARIANCE", 10);
    addAggregate("BOOL_OR", 11);
    
    addAggregate("SOME", 11);
    addAggregate("BOOL_AND", 12);
    
    addAggregate("EVERY", 12);
    addAggregate("SELECTIVITY", 13);
    addAggregate("HISTOGRAM", 14);
  }
  
  private static void addAggregate(String paramString, int paramInt)
  {
    AGGREGATES.put(paramString, Integer.valueOf(paramInt));
  }
  
  public static int getAggregateType(String paramString)
  {
    Integer localInteger = (Integer)AGGREGATES.get(paramString);
    return localInteger == null ? -1 : localInteger.intValue();
  }
  
  public void setGroupConcatOrder(ArrayList<SelectOrderBy> paramArrayList)
  {
    this.groupConcatOrderList = paramArrayList;
  }
  
  public void setGroupConcatSeparator(Expression paramExpression)
  {
    this.groupConcatSeparator = paramExpression;
  }
  
  private SortOrder initOrder(Session paramSession)
  {
    int i = this.groupConcatOrderList.size();
    int[] arrayOfInt1 = new int[i];
    int[] arrayOfInt2 = new int[i];
    for (int j = 0; j < i; j++)
    {
      SelectOrderBy localSelectOrderBy = (SelectOrderBy)this.groupConcatOrderList.get(j);
      arrayOfInt1[j] = (j + 1);
      int k = localSelectOrderBy.descending ? 1 : 0;
      arrayOfInt2[j] = k;
    }
    return new SortOrder(paramSession.getDatabase(), arrayOfInt1, arrayOfInt2, null);
  }
  
  public void updateAggregate(Session paramSession)
  {
    HashMap localHashMap = this.select.getCurrentGroup();
    if (localHashMap == null) {
      return;
    }
    int i = this.select.getCurrentGroupRowId();
    if (this.lastGroupRowId == i) {
      return;
    }
    this.lastGroupRowId = i;
    
    AggregateData localAggregateData = (AggregateData)localHashMap.get(this);
    if (localAggregateData == null)
    {
      localAggregateData = AggregateData.create(this.type);
      localHashMap.put(this, localAggregateData);
    }
    Object localObject = this.on == null ? null : this.on.getValue(paramSession);
    if ((this.type == 2) && 
      (localObject != ValueNull.INSTANCE))
    {
      localObject = ((Value)localObject).convertTo(13);
      if (this.groupConcatOrderList != null)
      {
        int j = this.groupConcatOrderList.size();
        Value[] arrayOfValue = new Value[1 + j];
        arrayOfValue[0] = localObject;
        for (int k = 0; k < j; k++)
        {
          SelectOrderBy localSelectOrderBy = (SelectOrderBy)this.groupConcatOrderList.get(k);
          arrayOfValue[(k + 1)] = localSelectOrderBy.expression.getValue(paramSession);
        }
        localObject = ValueArray.get(arrayOfValue);
      }
    }
    localAggregateData.add(paramSession.getDatabase(), this.dataType, this.distinct, (Value)localObject);
  }
  
  public Value getValue(Session paramSession)
  {
    final Object localObject3;
    Object localObject4;
    Object localObject5;
    if (this.select.isQuickAggregateQuery())
    {
      switch (this.type)
      {
      case 0: 
      case 1: 
        localObject1 = this.select.getTopTableFilter().getTable();
        return ValueLong.get(((Table)localObject1).getRowCount(paramSession));
      case 4: 
      case 5: 
        boolean bool = this.type == 4;
        localObject2 = getColumnIndex();
        int i = localObject2.getIndexColumns()[0].sortType;
        if ((i & 0x1) != 0) {
          bool = !bool;
        }
        localObject3 = ((Index)localObject2).findFirstOrLast(paramSession, bool);
        localObject4 = ((Cursor)localObject3).getSearchRow();
        if (localObject4 == null) {
          localObject5 = ValueNull.INSTANCE;
        } else {
          localObject5 = ((SearchRow)localObject4).getValue(localObject2.getColumns()[0].getColumnId());
        }
        return (Value)localObject5;
      }
      DbException.throwInternalError("type=" + this.type);
    }
    Object localObject1 = this.select.getCurrentGroup();
    if (localObject1 == null) {
      throw DbException.get(90054, getSQL());
    }
    AggregateData localAggregateData = (AggregateData)((HashMap)localObject1).get(this);
    if (localAggregateData == null) {
      localAggregateData = AggregateData.create(this.type);
    }
    Object localObject2 = localAggregateData.getValue(paramSession.getDatabase(), this.dataType, this.distinct);
    if (this.type == 2)
    {
      ArrayList localArrayList = ((AggregateDataGroupConcat)localAggregateData).getList();
      if ((localArrayList == null) || (localArrayList.size() == 0)) {
        return ValueNull.INSTANCE;
      }
      if (this.groupConcatOrderList != null)
      {
        localObject3 = this.groupConcatSort;
        Collections.sort(localArrayList, new Comparator()
        {
          public int compare(Value paramAnonymousValue1, Value paramAnonymousValue2)
          {
            Value[] arrayOfValue1 = ((ValueArray)paramAnonymousValue1).getList();
            Value[] arrayOfValue2 = ((ValueArray)paramAnonymousValue2).getList();
            return localObject3.compare(arrayOfValue1, arrayOfValue2);
          }
        });
      }
      localObject3 = new StatementBuilder();
      localObject4 = this.groupConcatSeparator == null ? "," : this.groupConcatSeparator.getValue(paramSession).getString();
      for (localObject5 = localArrayList.iterator(); ((Iterator)localObject5).hasNext();)
      {
        Value localValue = (Value)((Iterator)localObject5).next();
        String str;
        if (localValue.getType() == 17) {
          str = ((ValueArray)localValue).getList()[0].getString();
        } else {
          str = localValue.getString();
        }
        if (str != null)
        {
          if (localObject4 != null) {
            ((StatementBuilder)localObject3).appendExceptFirst((String)localObject4);
          }
          ((StatementBuilder)localObject3).append(str);
        }
      }
      localObject2 = ValueString.get(((StatementBuilder)localObject3).toString());
    }
    return (Value)localObject2;
  }
  
  public int getType()
  {
    return this.dataType;
  }
  
  public void mapColumns(ColumnResolver paramColumnResolver, int paramInt)
  {
    if (this.on != null) {
      this.on.mapColumns(paramColumnResolver, paramInt);
    }
    if (this.groupConcatOrderList != null) {
      for (SelectOrderBy localSelectOrderBy : this.groupConcatOrderList) {
        localSelectOrderBy.expression.mapColumns(paramColumnResolver, paramInt);
      }
    }
    if (this.groupConcatSeparator != null) {
      this.groupConcatSeparator.mapColumns(paramColumnResolver, paramInt);
    }
  }
  
  public Expression optimize(Session paramSession)
  {
    if (this.on != null)
    {
      this.on = this.on.optimize(paramSession);
      this.dataType = this.on.getType();
      this.scale = this.on.getScale();
      this.precision = this.on.getPrecision();
      this.displaySize = this.on.getDisplaySize();
    }
    if (this.groupConcatOrderList != null)
    {
      for (SelectOrderBy localSelectOrderBy : this.groupConcatOrderList) {
        localSelectOrderBy.expression = localSelectOrderBy.expression.optimize(paramSession);
      }
      this.groupConcatSort = initOrder(paramSession);
    }
    if (this.groupConcatSeparator != null) {
      this.groupConcatSeparator = this.groupConcatSeparator.optimize(paramSession);
    }
    switch (this.type)
    {
    case 2: 
      this.dataType = 13;
      this.scale = 0;
      this.precision = (this.displaySize = Integer.MAX_VALUE);
      break;
    case 0: 
    case 1: 
      this.dataType = 5;
      this.scale = 0;
      this.precision = 19L;
      this.displaySize = 20;
      break;
    case 13: 
      this.dataType = 4;
      this.scale = 0;
      this.precision = 10L;
      this.displaySize = 11;
      break;
    case 14: 
      this.dataType = 17;
      this.scale = 0;
      this.precision = (this.displaySize = Integer.MAX_VALUE);
      break;
    case 3: 
      if (this.dataType == 1)
      {
        this.dataType = 5;
      }
      else
      {
        if (!DataType.supportsAdd(this.dataType)) {
          throw DbException.get(90015, getSQL());
        }
        this.dataType = DataType.getAddProofType(this.dataType);
      }
      break;
    case 6: 
      if (!DataType.supportsAdd(this.dataType)) {
        throw DbException.get(90015, getSQL());
      }
      break;
    case 4: 
    case 5: 
      break;
    case 7: 
    case 8: 
    case 9: 
    case 10: 
      this.dataType = 7;
      this.precision = 17L;
      this.displaySize = 24;
      this.scale = 0;
      break;
    case 11: 
    case 12: 
      this.dataType = 1;
      this.precision = 1L;
      this.displaySize = 5;
      this.scale = 0;
      break;
    default: 
      DbException.throwInternalError("type=" + this.type);
    }
    return this;
  }
  
  public void setEvaluatable(TableFilter paramTableFilter, boolean paramBoolean)
  {
    if (this.on != null) {
      this.on.setEvaluatable(paramTableFilter, paramBoolean);
    }
    if (this.groupConcatOrderList != null) {
      for (SelectOrderBy localSelectOrderBy : this.groupConcatOrderList) {
        localSelectOrderBy.expression.setEvaluatable(paramTableFilter, paramBoolean);
      }
    }
    if (this.groupConcatSeparator != null) {
      this.groupConcatSeparator.setEvaluatable(paramTableFilter, paramBoolean);
    }
  }
  
  public int getScale()
  {
    return this.scale;
  }
  
  public long getPrecision()
  {
    return this.precision;
  }
  
  public int getDisplaySize()
  {
    return this.displaySize;
  }
  
  private String getSQLGroupConcat()
  {
    StatementBuilder localStatementBuilder = new StatementBuilder("GROUP_CONCAT(");
    if (this.distinct) {
      localStatementBuilder.append("DISTINCT ");
    }
    localStatementBuilder.append(this.on.getSQL());
    if (this.groupConcatOrderList != null)
    {
      localStatementBuilder.append(" ORDER BY ");
      for (SelectOrderBy localSelectOrderBy : this.groupConcatOrderList)
      {
        localStatementBuilder.appendExceptFirst(", ");
        localStatementBuilder.append(localSelectOrderBy.expression.getSQL());
        if (localSelectOrderBy.descending) {
          localStatementBuilder.append(" DESC");
        }
      }
    }
    if (this.groupConcatSeparator != null) {
      localStatementBuilder.append(" SEPARATOR ").append(this.groupConcatSeparator.getSQL());
    }
    return localStatementBuilder.append(')').toString();
  }
  
  public String getSQL()
  {
    String str;
    switch (this.type)
    {
    case 2: 
      return getSQLGroupConcat();
    case 0: 
      return "COUNT(*)";
    case 1: 
      str = "COUNT";
      break;
    case 13: 
      str = "SELECTIVITY";
      break;
    case 14: 
      str = "HISTOGRAM";
      break;
    case 3: 
      str = "SUM";
      break;
    case 4: 
      str = "MIN";
      break;
    case 5: 
      str = "MAX";
      break;
    case 6: 
      str = "AVG";
      break;
    case 7: 
      str = "STDDEV_POP";
      break;
    case 8: 
      str = "STDDEV_SAMP";
      break;
    case 9: 
      str = "VAR_POP";
      break;
    case 10: 
      str = "VAR_SAMP";
      break;
    case 12: 
      str = "BOOL_AND";
      break;
    case 11: 
      str = "BOOL_OR";
      break;
    default: 
      throw DbException.throwInternalError("type=" + this.type);
    }
    if (this.distinct) {
      return str + "(DISTINCT " + this.on.getSQL() + ")";
    }
    return str + StringUtils.enclose(this.on.getSQL());
  }
  
  private Index getColumnIndex()
  {
    if ((this.on instanceof ExpressionColumn))
    {
      ExpressionColumn localExpressionColumn = (ExpressionColumn)this.on;
      Column localColumn = localExpressionColumn.getColumn();
      TableFilter localTableFilter = localExpressionColumn.getTableFilter();
      if (localTableFilter != null)
      {
        Table localTable = localTableFilter.getTable();
        Index localIndex = localTable.getIndexForColumn(localColumn);
        return localIndex;
      }
    }
    return null;
  }
  
  public boolean isEverything(ExpressionVisitor paramExpressionVisitor)
  {
    if (paramExpressionVisitor.getType() == 1)
    {
      switch (this.type)
      {
      case 1: 
        if ((!this.distinct) && (this.on.getNullable() == 0)) {
          return paramExpressionVisitor.getTable().canGetRowCount();
        }
        return false;
      case 0: 
        return paramExpressionVisitor.getTable().canGetRowCount();
      case 4: 
      case 5: 
        Index localIndex = getColumnIndex();
        return localIndex != null;
      }
      return false;
    }
    if ((this.on != null) && (!this.on.isEverything(paramExpressionVisitor))) {
      return false;
    }
    if ((this.groupConcatSeparator != null) && (!this.groupConcatSeparator.isEverything(paramExpressionVisitor))) {
      return false;
    }
    if (this.groupConcatOrderList != null)
    {
      int i = 0;
      for (int j = this.groupConcatOrderList.size(); i < j; i++)
      {
        SelectOrderBy localSelectOrderBy = (SelectOrderBy)this.groupConcatOrderList.get(i);
        if (!localSelectOrderBy.expression.isEverything(paramExpressionVisitor)) {
          return false;
        }
      }
    }
    return true;
  }
  
  public int getCost()
  {
    return this.on == null ? 1 : this.on.getCost() + 1;
  }
}
