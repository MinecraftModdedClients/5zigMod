package org.h2.table;

import java.util.ArrayList;
import java.util.Iterator;
import org.h2.command.Parser;
import org.h2.command.dml.Select;
import org.h2.engine.Database;
import org.h2.engine.DbSettings;
import org.h2.engine.Mode;
import org.h2.engine.Session;
import org.h2.engine.SysProperties;
import org.h2.engine.User;
import org.h2.expression.ConditionAndOr;
import org.h2.expression.Expression;
import org.h2.expression.ExpressionColumn;
import org.h2.index.Index;
import org.h2.index.IndexCondition;
import org.h2.index.IndexCursor;
import org.h2.message.DbException;
import org.h2.result.Row;
import org.h2.result.SearchRow;
import org.h2.result.SortOrder;
import org.h2.schema.Schema;
import org.h2.util.New;
import org.h2.util.StatementBuilder;
import org.h2.util.StringUtils;
import org.h2.value.Value;
import org.h2.value.ValueLong;
import org.h2.value.ValueNull;

public class TableFilter
  implements ColumnResolver
{
  private static final int BEFORE_FIRST = 0;
  private static final int FOUND = 1;
  private static final int AFTER_LAST = 2;
  private static final int NULL_ROW = 3;
  protected boolean joinOuterIndirect;
  private Session session;
  private final Table table;
  private final Select select;
  private String alias;
  private Index index;
  private int scanCount;
  private boolean evaluatable;
  private boolean used;
  private final IndexCursor cursor;
  private final ArrayList<IndexCondition> indexConditions = New.arrayList();
  private Expression filterCondition;
  private Expression joinCondition;
  private SearchRow currentSearchRow;
  private Row current;
  private int state;
  private TableFilter join;
  private boolean joinOuter;
  private TableFilter nestedJoin;
  private ArrayList<Column> naturalJoinColumns;
  private boolean foundOne;
  private Expression fullCondition;
  private final int hashCode;
  
  public TableFilter(Session paramSession, Table paramTable, String paramString, boolean paramBoolean, Select paramSelect)
  {
    this.session = paramSession;
    this.table = paramTable;
    this.alias = paramString;
    this.select = paramSelect;
    this.cursor = new IndexCursor(this);
    if (!paramBoolean) {
      paramSession.getUser().checkRight(paramTable, 1);
    }
    this.hashCode = paramSession.nextObjectId();
  }
  
  public Select getSelect()
  {
    return this.select;
  }
  
  public Table getTable()
  {
    return this.table;
  }
  
  public void lock(Session paramSession, boolean paramBoolean1, boolean paramBoolean2)
  {
    this.table.lock(paramSession, paramBoolean1, paramBoolean2);
    if (this.join != null) {
      this.join.lock(paramSession, paramBoolean1, paramBoolean2);
    }
  }
  
  public PlanItem getBestPlanItem(Session paramSession, int paramInt)
  {
    PlanItem localPlanItem;
    if (this.indexConditions.size() == 0)
    {
      localPlanItem = new PlanItem();
      localPlanItem.setIndex(this.table.getScanIndex(paramSession));
      localPlanItem.cost = localPlanItem.getIndex().getCost(paramSession, null, null, null);
    }
    else
    {
      int i = this.table.getColumns().length;
      int[] arrayOfInt = new int[i];
      for (Object localObject = this.indexConditions.iterator(); ((Iterator)localObject).hasNext();)
      {
        IndexCondition localIndexCondition = (IndexCondition)((Iterator)localObject).next();
        if (localIndexCondition.isEvaluatable())
        {
          if (localIndexCondition.isAlwaysFalse())
          {
            arrayOfInt = null;
            break;
          }
          int j = localIndexCondition.getColumn().getColumnId();
          if (j >= 0) {
            arrayOfInt[j] |= localIndexCondition.getMask(this.indexConditions);
          }
        }
      }
      localObject = null;
      if (this.select != null) {
        localObject = this.select.getSortOrder();
      }
      localPlanItem = this.table.getBestPlanItem(paramSession, arrayOfInt, this, (SortOrder)localObject);
      
      localPlanItem.cost -= localPlanItem.cost * this.indexConditions.size() / 100.0D / paramInt;
    }
    if (this.nestedJoin != null)
    {
      setEvaluatable(this.nestedJoin);
      localPlanItem.setNestedJoinPlan(this.nestedJoin.getBestPlanItem(paramSession, paramInt));
      
      localPlanItem.cost += localPlanItem.cost * localPlanItem.getNestedJoinPlan().cost;
    }
    if (this.join != null)
    {
      setEvaluatable(this.join);
      localPlanItem.setJoinPlan(this.join.getBestPlanItem(paramSession, paramInt));
      
      localPlanItem.cost += localPlanItem.cost * localPlanItem.getJoinPlan().cost;
    }
    return localPlanItem;
  }
  
  private void setEvaluatable(TableFilter paramTableFilter)
  {
    if (this.session.getDatabase().getSettings().nestedJoins) {
      setEvaluatable(true);
    } else {
      do
      {
        Expression localExpression = paramTableFilter.getJoinCondition();
        if (localExpression != null) {
          localExpression.setEvaluatable(this, true);
        }
        TableFilter localTableFilter = paramTableFilter.getNestedJoin();
        if (localTableFilter != null) {
          setEvaluatable(localTableFilter);
        }
        paramTableFilter = paramTableFilter.getJoin();
      } while (paramTableFilter != null);
    }
  }
  
  public void setPlanItem(PlanItem paramPlanItem)
  {
    if (paramPlanItem == null) {
      return;
    }
    setIndex(paramPlanItem.getIndex());
    if ((this.nestedJoin != null) && 
      (paramPlanItem.getNestedJoinPlan() != null)) {
      this.nestedJoin.setPlanItem(paramPlanItem.getNestedJoinPlan());
    }
    if ((this.join != null) && 
      (paramPlanItem.getJoinPlan() != null)) {
      this.join.setPlanItem(paramPlanItem.getJoinPlan());
    }
  }
  
  public void prepare()
  {
    for (int i = 0; i < this.indexConditions.size(); i++)
    {
      IndexCondition localIndexCondition = (IndexCondition)this.indexConditions.get(i);
      if (!localIndexCondition.isAlwaysFalse())
      {
        Column localColumn = localIndexCondition.getColumn();
        if ((localColumn.getColumnId() >= 0) && 
          (this.index.getColumnIndex(localColumn) < 0))
        {
          this.indexConditions.remove(i);
          i--;
        }
      }
    }
    if (this.nestedJoin != null)
    {
      if ((SysProperties.CHECK) && (this.nestedJoin == this)) {
        DbException.throwInternalError("self join");
      }
      this.nestedJoin.prepare();
    }
    if (this.join != null)
    {
      if ((SysProperties.CHECK) && (this.join == this)) {
        DbException.throwInternalError("self join");
      }
      this.join.prepare();
    }
    if (this.filterCondition != null) {
      this.filterCondition = this.filterCondition.optimize(this.session);
    }
    if (this.joinCondition != null) {
      this.joinCondition = this.joinCondition.optimize(this.session);
    }
  }
  
  public void startQuery(Session paramSession)
  {
    this.session = paramSession;
    this.scanCount = 0;
    if (this.nestedJoin != null) {
      this.nestedJoin.startQuery(paramSession);
    }
    if (this.join != null) {
      this.join.startQuery(paramSession);
    }
  }
  
  public void reset()
  {
    if (this.nestedJoin != null) {
      this.nestedJoin.reset();
    }
    if (this.join != null) {
      this.join.reset();
    }
    this.state = 0;
    this.foundOne = false;
  }
  
  public boolean next()
  {
    if (this.state == 2) {
      return false;
    }
    if (this.state == 0)
    {
      this.cursor.find(this.session, this.indexConditions);
      if (!this.cursor.isAlwaysFalse())
      {
        if (this.nestedJoin != null) {
          this.nestedJoin.reset();
        }
        if (this.join != null) {
          this.join.reset();
        }
      }
    }
    else if ((this.join != null) && (this.join.next()))
    {
      return true;
    }
    while (this.state != 3)
    {
      if (this.cursor.isAlwaysFalse())
      {
        this.state = 2;
      }
      else if (this.nestedJoin != null)
      {
        if (this.state == 0) {
          this.state = 1;
        }
      }
      else
      {
        if ((++this.scanCount & 0xFFF) == 0) {
          checkTimeout();
        }
        if (this.cursor.next())
        {
          this.currentSearchRow = this.cursor.getSearchRow();
          this.current = null;
          this.state = 1;
        }
        else
        {
          this.state = 2;
        }
      }
      if ((this.nestedJoin != null) && (this.state == 1) && 
        (!this.nestedJoin.next()))
      {
        this.state = 2;
        if ((!this.joinOuter) || (this.foundOne)) {}
      }
      else
      {
        if (this.state == 2)
        {
          if ((!this.joinOuter) || (this.foundOne)) {
            break;
          }
          setNullRow();
        }
        if (isOk(this.filterCondition))
        {
          boolean bool = isOk(this.joinCondition);
          if (this.state == 1)
          {
            if (bool) {
              this.foundOne = true;
            }
          }
          else if (this.join != null)
          {
            this.join.reset();
            if (!this.join.next()) {}
          }
          else if ((this.state == 3) || (bool))
          {
            return true;
          }
        }
      }
    }
    this.state = 2;
    return false;
  }
  
  protected void setNullRow()
  {
    this.state = 3;
    this.current = this.table.getNullRow();
    this.currentSearchRow = this.current;
    if (this.nestedJoin != null) {
      this.nestedJoin.visit(new TableFilterVisitor()
      {
        public void accept(TableFilter paramAnonymousTableFilter)
        {
          paramAnonymousTableFilter.setNullRow();
        }
      });
    }
  }
  
  private void checkTimeout()
  {
    this.session.checkCanceled();
  }
  
  private boolean isOk(Expression paramExpression)
  {
    if (paramExpression == null) {
      return true;
    }
    return Boolean.TRUE.equals(paramExpression.getBooleanValue(this.session));
  }
  
  public Row get()
  {
    if ((this.current == null) && (this.currentSearchRow != null)) {
      this.current = this.cursor.get();
    }
    return this.current;
  }
  
  public void set(Row paramRow)
  {
    this.current = paramRow;
    this.currentSearchRow = paramRow;
  }
  
  public String getTableAlias()
  {
    if (this.alias != null) {
      return this.alias;
    }
    return this.table.getName();
  }
  
  public void addIndexCondition(IndexCondition paramIndexCondition)
  {
    this.indexConditions.add(paramIndexCondition);
  }
  
  public void addFilterCondition(Expression paramExpression, boolean paramBoolean)
  {
    if (paramBoolean)
    {
      if (this.joinCondition == null) {
        this.joinCondition = paramExpression;
      } else {
        this.joinCondition = new ConditionAndOr(0, this.joinCondition, paramExpression);
      }
    }
    else if (this.filterCondition == null) {
      this.filterCondition = paramExpression;
    } else {
      this.filterCondition = new ConditionAndOr(0, this.filterCondition, paramExpression);
    }
  }
  
  public void addJoin(TableFilter paramTableFilter, boolean paramBoolean1, boolean paramBoolean2, final Expression paramExpression)
  {
    if (paramExpression != null)
    {
      paramExpression.mapColumns(this, 0);
      if (this.session.getDatabase().getSettings().nestedJoins)
      {
        visit(new TableFilterVisitor()
        {
          public void accept(TableFilter paramAnonymousTableFilter)
          {
            paramExpression.mapColumns(paramAnonymousTableFilter, 0);
          }
        });
        paramTableFilter.visit(new TableFilterVisitor()
        {
          public void accept(TableFilter paramAnonymousTableFilter)
          {
            paramExpression.mapColumns(paramAnonymousTableFilter, 0);
          }
        });
      }
    }
    if ((paramBoolean2) && (this.session.getDatabase().getSettings().nestedJoins))
    {
      if (this.nestedJoin != null) {
        throw DbException.throwInternalError();
      }
      this.nestedJoin = paramTableFilter;
      paramTableFilter.joinOuter = paramBoolean1;
      if (paramBoolean1) {
        visit(new TableFilterVisitor()
        {
          public void accept(TableFilter paramAnonymousTableFilter)
          {
            paramAnonymousTableFilter.joinOuterIndirect = true;
          }
        });
      }
      if (paramExpression != null) {
        paramTableFilter.mapAndAddFilter(paramExpression);
      }
    }
    else if (this.join == null)
    {
      this.join = paramTableFilter;
      paramTableFilter.joinOuter = paramBoolean1;
      if (this.session.getDatabase().getSettings().nestedJoins)
      {
        if (paramBoolean1) {
          paramTableFilter.visit(new TableFilterVisitor()
          {
            public void accept(TableFilter paramAnonymousTableFilter)
            {
              paramAnonymousTableFilter.joinOuterIndirect = true;
            }
          });
        }
      }
      else if (paramBoolean1)
      {
        TableFilter localTableFilter = paramTableFilter.join;
        while (localTableFilter != null)
        {
          localTableFilter.joinOuter = true;
          localTableFilter = localTableFilter.join;
        }
      }
      if (paramExpression != null) {
        paramTableFilter.mapAndAddFilter(paramExpression);
      }
    }
    else
    {
      this.join.addJoin(paramTableFilter, paramBoolean1, paramBoolean2, paramExpression);
    }
  }
  
  public void mapAndAddFilter(Expression paramExpression)
  {
    paramExpression.mapColumns(this, 0);
    addFilterCondition(paramExpression, true);
    paramExpression.createIndexConditions(this.session, this);
    if (this.nestedJoin != null)
    {
      paramExpression.mapColumns(this.nestedJoin, 0);
      paramExpression.createIndexConditions(this.session, this.nestedJoin);
    }
    if (this.join != null) {
      this.join.mapAndAddFilter(paramExpression);
    }
  }
  
  public TableFilter getJoin()
  {
    return this.join;
  }
  
  public boolean isJoinOuter()
  {
    return this.joinOuter;
  }
  
  public boolean isJoinOuterIndirect()
  {
    return this.joinOuterIndirect;
  }
  
  public String getPlanSQL(boolean paramBoolean)
  {
    StringBuilder localStringBuilder = new StringBuilder();
    if (paramBoolean) {
      if (this.joinOuter) {
        localStringBuilder.append("LEFT OUTER JOIN ");
      } else {
        localStringBuilder.append("INNER JOIN ");
      }
    }
    Object localObject1;
    Object localObject2;
    Object localObject3;
    if (this.nestedJoin != null)
    {
      localObject1 = new StringBuffer();
      localObject2 = this.nestedJoin;
      do
      {
        ((StringBuffer)localObject1).append(((TableFilter)localObject2).getPlanSQL(localObject2 != this.nestedJoin));
        ((StringBuffer)localObject1).append('\n');
        localObject2 = ((TableFilter)localObject2).getJoin();
      } while (localObject2 != null);
      localObject3 = ((StringBuffer)localObject1).toString();
      int i = !((String)localObject3).startsWith("(") ? 1 : 0;
      if (i != 0) {
        localStringBuilder.append("(\n");
      }
      localStringBuilder.append(StringUtils.indent((String)localObject3, 4, false));
      if (i != 0) {
        localStringBuilder.append(')');
      }
      if (paramBoolean)
      {
        localStringBuilder.append(" ON ");
        if (this.joinCondition == null) {
          localStringBuilder.append("1=1");
        } else {
          localStringBuilder.append(StringUtils.unEnclose(this.joinCondition.getSQL()));
        }
      }
      return localStringBuilder.toString();
    }
    localStringBuilder.append(this.table.getSQL());
    if (this.alias != null) {
      localStringBuilder.append(' ').append(Parser.quoteIdentifier(this.alias));
    }
    if (this.index != null)
    {
      localStringBuilder.append('\n');
      localObject1 = new StatementBuilder();
      ((StatementBuilder)localObject1).append(this.index.getPlanSQL());
      if (this.indexConditions.size() > 0)
      {
        ((StatementBuilder)localObject1).append(": ");
        for (localObject2 = this.indexConditions.iterator(); ((Iterator)localObject2).hasNext();)
        {
          localObject3 = (IndexCondition)((Iterator)localObject2).next();
          ((StatementBuilder)localObject1).appendExceptFirst("\n    AND ");
          ((StatementBuilder)localObject1).append(((IndexCondition)localObject3).getSQL());
        }
      }
      localObject2 = StringUtils.quoteRemarkSQL(((StatementBuilder)localObject1).toString());
      if (((String)localObject2).indexOf('\n') >= 0) {
        localObject2 = (String)localObject2 + "\n";
      }
      localStringBuilder.append(StringUtils.indent("/* " + (String)localObject2 + " */", 4, false));
    }
    if (paramBoolean)
    {
      localStringBuilder.append("\n    ON ");
      if (this.joinCondition == null) {
        localStringBuilder.append("1=1");
      } else {
        localStringBuilder.append(StringUtils.unEnclose(this.joinCondition.getSQL()));
      }
    }
    if (this.filterCondition != null)
    {
      localStringBuilder.append('\n');
      localObject1 = StringUtils.unEnclose(this.filterCondition.getSQL());
      localObject1 = "/* WHERE " + StringUtils.quoteRemarkSQL((String)localObject1) + "\n*/";
      localStringBuilder.append(StringUtils.indent((String)localObject1, 4, false));
    }
    if (this.scanCount > 0) {
      localStringBuilder.append("\n    /* scanCount: ").append(this.scanCount).append(" */");
    }
    return localStringBuilder.toString();
  }
  
  void removeUnusableIndexConditions()
  {
    for (int i = 0; i < this.indexConditions.size(); i++)
    {
      IndexCondition localIndexCondition = (IndexCondition)this.indexConditions.get(i);
      if (!localIndexCondition.isEvaluatable()) {
        this.indexConditions.remove(i--);
      }
    }
  }
  
  public Index getIndex()
  {
    return this.index;
  }
  
  public void setIndex(Index paramIndex)
  {
    this.index = paramIndex;
    this.cursor.setIndex(paramIndex);
  }
  
  public void setUsed(boolean paramBoolean)
  {
    this.used = paramBoolean;
  }
  
  public boolean isUsed()
  {
    return this.used;
  }
  
  void setSession(Session paramSession)
  {
    this.session = paramSession;
  }
  
  public void removeJoin()
  {
    this.join = null;
  }
  
  public Expression getJoinCondition()
  {
    return this.joinCondition;
  }
  
  public void removeJoinCondition()
  {
    this.joinCondition = null;
  }
  
  public Expression getFilterCondition()
  {
    return this.filterCondition;
  }
  
  public void removeFilterCondition()
  {
    this.filterCondition = null;
  }
  
  public void setFullCondition(Expression paramExpression)
  {
    this.fullCondition = paramExpression;
    if (this.join != null) {
      this.join.setFullCondition(paramExpression);
    }
  }
  
  void optimizeFullCondition(boolean paramBoolean)
  {
    if (this.fullCondition != null)
    {
      this.fullCondition.addFilterConditions(this, (paramBoolean) || (this.joinOuter));
      if (this.nestedJoin != null) {
        this.nestedJoin.optimizeFullCondition((paramBoolean) || (this.joinOuter));
      }
      if (this.join != null) {
        this.join.optimizeFullCondition((paramBoolean) || (this.joinOuter));
      }
    }
  }
  
  public void setEvaluatable(TableFilter paramTableFilter, boolean paramBoolean)
  {
    paramTableFilter.setEvaluatable(paramBoolean);
    if (this.filterCondition != null) {
      this.filterCondition.setEvaluatable(paramTableFilter, paramBoolean);
    }
    if (this.joinCondition != null) {
      this.joinCondition.setEvaluatable(paramTableFilter, paramBoolean);
    }
    if (this.nestedJoin != null) {
      if (this == paramTableFilter) {
        this.nestedJoin.setEvaluatable(this.nestedJoin, paramBoolean);
      }
    }
    if (this.join != null) {
      this.join.setEvaluatable(paramTableFilter, paramBoolean);
    }
  }
  
  public void setEvaluatable(boolean paramBoolean)
  {
    this.evaluatable = paramBoolean;
  }
  
  public String getSchemaName()
  {
    return this.table.getSchema().getName();
  }
  
  public Column[] getColumns()
  {
    return this.table.getColumns();
  }
  
  public Column[] getSystemColumns()
  {
    if (!this.session.getDatabase().getMode().systemColumns) {
      return null;
    }
    Column[] arrayOfColumn = new Column[3];
    arrayOfColumn[0] = new Column("oid", 4);
    arrayOfColumn[0].setTable(this.table, 0);
    arrayOfColumn[1] = new Column("ctid", 13);
    arrayOfColumn[1].setTable(this.table, 0);
    arrayOfColumn[2] = new Column("CTID", 13);
    arrayOfColumn[2].setTable(this.table, 0);
    return arrayOfColumn;
  }
  
  public Column getRowIdColumn()
  {
    if (this.session.getDatabase().getSettings().rowId) {
      return this.table.getRowIdColumn();
    }
    return null;
  }
  
  public Value getValue(Column paramColumn)
  {
    if (this.currentSearchRow == null) {
      return null;
    }
    int i = paramColumn.getColumnId();
    if (i == -1) {
      return ValueLong.get(this.currentSearchRow.getKey());
    }
    if (this.current == null)
    {
      Value localValue = this.currentSearchRow.getValue(i);
      if (localValue != null) {
        return localValue;
      }
      this.current = this.cursor.get();
      if (this.current == null) {
        return ValueNull.INSTANCE;
      }
    }
    return this.current.getValue(i);
  }
  
  public TableFilter getTableFilter()
  {
    return this;
  }
  
  public void setAlias(String paramString)
  {
    this.alias = paramString;
  }
  
  public Expression optimize(ExpressionColumn paramExpressionColumn, Column paramColumn)
  {
    return paramExpressionColumn;
  }
  
  public String toString()
  {
    return this.alias != null ? this.alias : this.table.toString();
  }
  
  public void addNaturalJoinColumn(Column paramColumn)
  {
    if (this.naturalJoinColumns == null) {
      this.naturalJoinColumns = New.arrayList();
    }
    this.naturalJoinColumns.add(paramColumn);
  }
  
  public boolean isNaturalJoinColumn(Column paramColumn)
  {
    return (this.naturalJoinColumns != null) && (this.naturalJoinColumns.contains(paramColumn));
  }
  
  public int hashCode()
  {
    return this.hashCode;
  }
  
  public boolean hasInComparisons()
  {
    for (IndexCondition localIndexCondition : this.indexConditions)
    {
      int i = localIndexCondition.getCompareType();
      if ((i == 10) || (i == 9)) {
        return true;
      }
    }
    return false;
  }
  
  public void lockRowAdd(ArrayList<Row> paramArrayList)
  {
    if (this.state == 1) {
      paramArrayList.add(get());
    }
  }
  
  public void lockRows(ArrayList<Row> paramArrayList)
  {
    for (Row localRow1 : paramArrayList)
    {
      Row localRow2 = localRow1.getCopy();
      this.table.removeRow(this.session, localRow1);
      this.session.log(this.table, (short)1, localRow1);
      this.table.addRow(this.session, localRow2);
      this.session.log(this.table, (short)0, localRow2);
    }
  }
  
  public TableFilter getNestedJoin()
  {
    return this.nestedJoin;
  }
  
  public void visit(TableFilterVisitor paramTableFilterVisitor)
  {
    TableFilter localTableFilter1 = this;
    do
    {
      paramTableFilterVisitor.accept(localTableFilter1);
      TableFilter localTableFilter2 = localTableFilter1.nestedJoin;
      if (localTableFilter2 != null) {
        localTableFilter2.visit(paramTableFilterVisitor);
      }
      localTableFilter1 = localTableFilter1.join;
    } while (localTableFilter1 != null);
  }
  
  public boolean isEvaluatable()
  {
    return this.evaluatable;
  }
  
  public Session getSession()
  {
    return this.session;
  }
  
  public static abstract interface TableFilterVisitor
  {
    public abstract void accept(TableFilter paramTableFilter);
  }
}
