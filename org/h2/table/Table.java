package org.h2.table;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.h2.command.Prepared;
import org.h2.constraint.Constraint;
import org.h2.engine.Database;
import org.h2.engine.DbObject;
import org.h2.engine.Right;
import org.h2.engine.Session;
import org.h2.engine.Session.Savepoint;
import org.h2.expression.Expression;
import org.h2.expression.ExpressionVisitor;
import org.h2.index.Index;
import org.h2.index.IndexType;
import org.h2.message.DbException;
import org.h2.result.Row;
import org.h2.result.RowList;
import org.h2.result.SearchRow;
import org.h2.result.SimpleRow;
import org.h2.result.SimpleRowValue;
import org.h2.result.SortOrder;
import org.h2.schema.Schema;
import org.h2.schema.SchemaObject;
import org.h2.schema.SchemaObjectBase;
import org.h2.schema.Sequence;
import org.h2.schema.TriggerObject;
import org.h2.util.New;
import org.h2.value.CompareMode;
import org.h2.value.Value;
import org.h2.value.ValueNull;

public abstract class Table
  extends SchemaObjectBase
{
  public static final int TYPE_CACHED = 0;
  public static final int TYPE_MEMORY = 1;
  public static final String TABLE_LINK = "TABLE LINK";
  public static final String SYSTEM_TABLE = "SYSTEM TABLE";
  public static final String TABLE = "TABLE";
  public static final String VIEW = "VIEW";
  public static final String EXTERNAL_TABLE_ENGINE = "EXTERNAL";
  protected Column[] columns;
  protected CompareMode compareMode;
  protected boolean isHidden;
  private final HashMap<String, Column> columnMap;
  private final boolean persistIndexes;
  private final boolean persistData;
  private ArrayList<TriggerObject> triggers;
  private ArrayList<Constraint> constraints;
  private ArrayList<Sequence> sequences;
  private ArrayList<TableView> views;
  private boolean checkForeignKeyConstraints = true;
  private boolean onCommitDrop;
  private boolean onCommitTruncate;
  private Row nullRow;
  
  public Table(Schema paramSchema, int paramInt, String paramString, boolean paramBoolean1, boolean paramBoolean2)
  {
    this.columnMap = paramSchema.getDatabase().newStringMap();
    initSchemaObjectBase(paramSchema, paramInt, paramString, "table");
    this.persistIndexes = paramBoolean1;
    this.persistData = paramBoolean2;
    this.compareMode = paramSchema.getDatabase().getCompareMode();
  }
  
  public void rename(String paramString)
  {
    super.rename(paramString);
    if (this.constraints != null)
    {
      int i = 0;
      for (int j = this.constraints.size(); i < j; i++)
      {
        Constraint localConstraint = (Constraint)this.constraints.get(i);
        localConstraint.rebuild();
      }
    }
  }
  
  public abstract boolean lock(Session paramSession, boolean paramBoolean1, boolean paramBoolean2);
  
  public abstract void close(Session paramSession);
  
  public abstract void unlock(Session paramSession);
  
  public abstract Index addIndex(Session paramSession, String paramString1, int paramInt, IndexColumn[] paramArrayOfIndexColumn, IndexType paramIndexType, boolean paramBoolean, String paramString2);
  
  public Row getRow(Session paramSession, long paramLong)
  {
    return null;
  }
  
  public abstract void removeRow(Session paramSession, Row paramRow);
  
  public abstract void truncate(Session paramSession);
  
  public abstract void addRow(Session paramSession, Row paramRow);
  
  public void commit(short paramShort, Row paramRow) {}
  
  public abstract void checkSupportAlter();
  
  public abstract String getTableType();
  
  public abstract Index getScanIndex(Session paramSession);
  
  public abstract Index getUniqueIndex();
  
  public abstract ArrayList<Index> getIndexes();
  
  public abstract boolean isLockedExclusively();
  
  public abstract long getMaxDataModificationId();
  
  public abstract boolean isDeterministic();
  
  public abstract boolean canGetRowCount();
  
  public boolean canReference()
  {
    return true;
  }
  
  public abstract boolean canDrop();
  
  public abstract long getRowCount(Session paramSession);
  
  public abstract long getRowCountApproximation();
  
  public abstract long getDiskSpaceUsed();
  
  public Column getRowIdColumn()
  {
    return null;
  }
  
  public String getCreateSQLForCopy(Table paramTable, String paramString)
  {
    throw DbException.throwInternalError();
  }
  
  public boolean isQueryComparable()
  {
    ExpressionVisitor localExpressionVisitor = ExpressionVisitor.QUERY_COMPARABLE_VISITOR;
    for (Column localColumn : this.columns) {
      if (!localColumn.isEverything(localExpressionVisitor)) {
        return false;
      }
    }
    return true;
  }
  
  public void addDependencies(HashSet<DbObject> paramHashSet)
  {
    if (paramHashSet.contains(this)) {
      return;
    }
    if (this.sequences != null) {
      for (localObject1 = this.sequences.iterator(); ((Iterator)localObject1).hasNext();)
      {
        localObject2 = (Sequence)((Iterator)localObject1).next();
        paramHashSet.add(localObject2);
      }
    }
    Object localObject2;
    Object localObject1 = ExpressionVisitor.getDependenciesVisitor(paramHashSet);
    for (Object localObject3 : this.columns) {
      ((Column)localObject3).isEverything((ExpressionVisitor)localObject1);
    }
    if (this.constraints != null) {
      for (localObject2 = this.constraints.iterator(); ((Iterator)localObject2).hasNext();)
      {
        Constraint localConstraint = (Constraint)((Iterator)localObject2).next();
        localConstraint.isEverything((ExpressionVisitor)localObject1);
      }
    }
    paramHashSet.add(this);
  }
  
  public ArrayList<DbObject> getChildren()
  {
    ArrayList localArrayList1 = New.arrayList();
    ArrayList localArrayList2 = getIndexes();
    if (localArrayList2 != null) {
      localArrayList1.addAll(localArrayList2);
    }
    if (this.constraints != null) {
      localArrayList1.addAll(this.constraints);
    }
    if (this.triggers != null) {
      localArrayList1.addAll(this.triggers);
    }
    if (this.sequences != null) {
      localArrayList1.addAll(this.sequences);
    }
    if (this.views != null) {
      localArrayList1.addAll(this.views);
    }
    ArrayList localArrayList3 = this.database.getAllRights();
    for (Right localRight : localArrayList3) {
      if (localRight.getGrantedTable() == this) {
        localArrayList1.add(localRight);
      }
    }
    return localArrayList1;
  }
  
  protected void setColumns(Column[] paramArrayOfColumn)
  {
    this.columns = paramArrayOfColumn;
    if (this.columnMap.size() > 0) {
      this.columnMap.clear();
    }
    for (int i = 0; i < paramArrayOfColumn.length; i++)
    {
      Column localColumn = paramArrayOfColumn[i];
      int j = localColumn.getType();
      if (j == -1) {
        throw DbException.get(50004, localColumn.getSQL());
      }
      localColumn.setTable(this, i);
      String str = localColumn.getName();
      if (this.columnMap.get(str) != null) {
        throw DbException.get(42121, str);
      }
      this.columnMap.put(str, localColumn);
    }
  }
  
  public void renameColumn(Column paramColumn, String paramString)
  {
    for (Column localColumn : this.columns) {
      if (localColumn != paramColumn) {
        if (localColumn.getName().equals(paramString)) {
          throw DbException.get(42121, paramString);
        }
      }
    }
    this.columnMap.remove(paramColumn.getName());
    paramColumn.rename(paramString);
    this.columnMap.put(paramString, paramColumn);
  }
  
  public boolean isLockedExclusivelyBy(Session paramSession)
  {
    return false;
  }
  
  public void updateRows(Prepared paramPrepared, Session paramSession, RowList paramRowList)
  {
    Session.Savepoint localSavepoint = paramSession.setSavepoint();
    
    int i = 0;
    for (paramRowList.reset(); paramRowList.hasNext();)
    {
      i++;
      if ((i & 0x7F) == 0) {
        paramPrepared.checkCanceled();
      }
      localRow = paramRowList.next();
      paramRowList.next();
      removeRow(paramSession, localRow);
      paramSession.log(this, (short)1, localRow);
    }
    Row localRow;
    for (paramRowList.reset(); paramRowList.hasNext();)
    {
      i++;
      if ((i & 0x7F) == 0) {
        paramPrepared.checkCanceled();
      }
      paramRowList.next();
      localRow = paramRowList.next();
      try
      {
        addRow(paramSession, localRow);
      }
      catch (DbException localDbException)
      {
        if (localDbException.getErrorCode() == 90131)
        {
          paramSession.rollbackTo(localSavepoint, false);
          paramSession.startStatementWithinTransaction();
          localSavepoint = paramSession.setSavepoint();
        }
        throw localDbException;
      }
      paramSession.log(this, (short)0, localRow);
    }
  }
  
  public ArrayList<TableView> getViews()
  {
    return this.views;
  }
  
  public void removeChildrenAndResources(Session paramSession)
  {
    while ((this.views != null) && (this.views.size() > 0))
    {
      localObject = (TableView)this.views.get(0);
      this.views.remove(0);
      this.database.removeSchemaObject(paramSession, (SchemaObject)localObject);
    }
    while ((this.triggers != null) && (this.triggers.size() > 0))
    {
      localObject = (TriggerObject)this.triggers.get(0);
      this.triggers.remove(0);
      this.database.removeSchemaObject(paramSession, (SchemaObject)localObject);
    }
    while ((this.constraints != null) && (this.constraints.size() > 0))
    {
      localObject = (Constraint)this.constraints.get(0);
      this.constraints.remove(0);
      this.database.removeSchemaObject(paramSession, (SchemaObject)localObject);
    }
    for (Object localObject = this.database.getAllRights().iterator(); ((Iterator)localObject).hasNext();)
    {
      Right localRight = (Right)((Iterator)localObject).next();
      if (localRight.getGrantedTable() == this) {
        this.database.removeDatabaseObject(paramSession, localRight);
      }
    }
    this.database.removeMeta(paramSession, getId());
    while ((this.sequences != null) && (this.sequences.size() > 0))
    {
      localObject = (Sequence)this.sequences.get(0);
      this.sequences.remove(0);
      if (!isTemporary()) {
        if (this.database.getDependentTable((SchemaObject)localObject, this) == null) {
          this.database.removeSchemaObject(paramSession, (SchemaObject)localObject);
        }
      }
    }
  }
  
  public void dropSingleColumnConstraintsAndIndexes(Session paramSession, Column paramColumn)
  {
    ArrayList localArrayList1 = New.arrayList();
    if (this.constraints != null)
    {
      int i = 0;
      for (int j = this.constraints.size(); i < j; i++)
      {
        Constraint localConstraint = (Constraint)this.constraints.get(i);
        HashSet localHashSet = localConstraint.getReferencedColumns(this);
        if (localHashSet.contains(paramColumn)) {
          if (localHashSet.size() == 1) {
            localArrayList1.add(localConstraint);
          } else {
            throw DbException.get(90083, localConstraint.getSQL());
          }
        }
      }
    }
    ArrayList localArrayList2 = New.arrayList();
    ArrayList localArrayList3 = getIndexes();
    if (localArrayList3 != null)
    {
      int k = 0;
      for (int m = localArrayList3.size(); k < m; k++)
      {
        Index localIndex = (Index)localArrayList3.get(k);
        if (localIndex.getCreateSQL() != null) {
          if (localIndex.getColumnIndex(paramColumn) >= 0) {
            if (localIndex.getColumns().length == 1) {
              localArrayList2.add(localIndex);
            } else {
              throw DbException.get(90083, localIndex.getSQL());
            }
          }
        }
      }
    }
    for (Iterator localIterator = localArrayList1.iterator(); localIterator.hasNext();)
    {
      localObject = (Constraint)localIterator.next();
      paramSession.getDatabase().removeSchemaObject(paramSession, (SchemaObject)localObject);
    }
    Object localObject;
    for (localIterator = localArrayList2.iterator(); localIterator.hasNext();)
    {
      localObject = (Index)localIterator.next();
      if (getIndexes().contains(localObject)) {
        paramSession.getDatabase().removeSchemaObject(paramSession, (SchemaObject)localObject);
      }
    }
  }
  
  public Row getTemplateRow()
  {
    return new Row(new Value[this.columns.length], -1);
  }
  
  public SearchRow getTemplateSimpleRow(boolean paramBoolean)
  {
    if (paramBoolean) {
      return new SimpleRowValue(this.columns.length);
    }
    return new SimpleRow(new Value[this.columns.length]);
  }
  
  synchronized Row getNullRow()
  {
    if (this.nullRow == null)
    {
      this.nullRow = new Row(new Value[this.columns.length], 1);
      for (int i = 0; i < this.columns.length; i++) {
        this.nullRow.setValue(i, ValueNull.INSTANCE);
      }
    }
    return this.nullRow;
  }
  
  public Column[] getColumns()
  {
    return this.columns;
  }
  
  public int getType()
  {
    return 0;
  }
  
  public Column getColumn(int paramInt)
  {
    return this.columns[paramInt];
  }
  
  public Column getColumn(String paramString)
  {
    Column localColumn = (Column)this.columnMap.get(paramString);
    if (localColumn == null) {
      throw DbException.get(42122, paramString);
    }
    return localColumn;
  }
  
  public boolean doesColumnExist(String paramString)
  {
    return this.columnMap.containsKey(paramString);
  }
  
  public PlanItem getBestPlanItem(Session paramSession, int[] paramArrayOfInt, TableFilter paramTableFilter, SortOrder paramSortOrder)
  {
    PlanItem localPlanItem = new PlanItem();
    localPlanItem.setIndex(getScanIndex(paramSession));
    localPlanItem.cost = localPlanItem.getIndex().getCost(paramSession, null, null, null);
    ArrayList localArrayList = getIndexes();
    if ((localArrayList != null) && (paramArrayOfInt != null))
    {
      int i = 1;
      for (int j = localArrayList.size(); i < j; i++)
      {
        Index localIndex = (Index)localArrayList.get(i);
        double d = localIndex.getCost(paramSession, paramArrayOfInt, paramTableFilter, paramSortOrder);
        if (d < localPlanItem.cost)
        {
          localPlanItem.cost = d;
          localPlanItem.setIndex(localIndex);
        }
      }
    }
    return localPlanItem;
  }
  
  public Index findPrimaryKey()
  {
    ArrayList localArrayList = getIndexes();
    if (localArrayList != null)
    {
      int i = 0;
      for (int j = localArrayList.size(); i < j; i++)
      {
        Index localIndex = (Index)localArrayList.get(i);
        if (localIndex.getIndexType().isPrimaryKey()) {
          return localIndex;
        }
      }
    }
    return null;
  }
  
  public Index getPrimaryKey()
  {
    Index localIndex = findPrimaryKey();
    if (localIndex != null) {
      return localIndex;
    }
    throw DbException.get(42112, "PRIMARY_KEY_");
  }
  
  public void validateConvertUpdateSequence(Session paramSession, Row paramRow)
  {
    for (int i = 0; i < this.columns.length; i++)
    {
      Value localValue1 = paramRow.getValue(i);
      Column localColumn = this.columns[i];
      if (localColumn.getComputed())
      {
        localValue1 = null;
        localValue2 = localColumn.computeValue(paramSession, paramRow);
      }
      Value localValue2 = localColumn.validateConvertUpdateSequence(paramSession, localValue1);
      if (localValue2 != localValue1) {
        paramRow.setValue(i, localValue2);
      }
    }
  }
  
  private static void remove(ArrayList<? extends DbObject> paramArrayList, DbObject paramDbObject)
  {
    if (paramArrayList != null)
    {
      int i = paramArrayList.indexOf(paramDbObject);
      if (i >= 0) {
        paramArrayList.remove(i);
      }
    }
  }
  
  public void removeIndex(Index paramIndex)
  {
    ArrayList localArrayList = getIndexes();
    if (localArrayList != null)
    {
      remove(localArrayList, paramIndex);
      if (paramIndex.getIndexType().isPrimaryKey()) {
        for (Column localColumn : paramIndex.getColumns()) {
          localColumn.setPrimaryKey(false);
        }
      }
    }
  }
  
  public void removeView(TableView paramTableView)
  {
    remove(this.views, paramTableView);
  }
  
  public void removeConstraint(Constraint paramConstraint)
  {
    remove(this.constraints, paramConstraint);
  }
  
  public final void removeSequence(Sequence paramSequence)
  {
    remove(this.sequences, paramSequence);
  }
  
  public void removeTrigger(TriggerObject paramTriggerObject)
  {
    remove(this.triggers, paramTriggerObject);
  }
  
  public void addView(TableView paramTableView)
  {
    this.views = add(this.views, paramTableView);
  }
  
  public void addConstraint(Constraint paramConstraint)
  {
    if ((this.constraints == null) || (this.constraints.indexOf(paramConstraint) < 0)) {
      this.constraints = add(this.constraints, paramConstraint);
    }
  }
  
  public ArrayList<Constraint> getConstraints()
  {
    return this.constraints;
  }
  
  public void addSequence(Sequence paramSequence)
  {
    this.sequences = add(this.sequences, paramSequence);
  }
  
  public void addTrigger(TriggerObject paramTriggerObject)
  {
    this.triggers = add(this.triggers, paramTriggerObject);
  }
  
  private static <T> ArrayList<T> add(ArrayList<T> paramArrayList, T paramT)
  {
    if (paramArrayList == null) {
      paramArrayList = New.arrayList();
    }
    paramArrayList.add(paramT);
    return paramArrayList;
  }
  
  public void fire(Session paramSession, int paramInt, boolean paramBoolean)
  {
    if (this.triggers != null) {
      for (TriggerObject localTriggerObject : this.triggers) {
        localTriggerObject.fire(paramSession, paramInt, paramBoolean);
      }
    }
  }
  
  public boolean hasSelectTrigger()
  {
    if (this.triggers != null) {
      for (TriggerObject localTriggerObject : this.triggers) {
        if (localTriggerObject.isSelectTrigger()) {
          return true;
        }
      }
    }
    return false;
  }
  
  public boolean fireRow()
  {
    return ((this.constraints != null) && (this.constraints.size() > 0)) || ((this.triggers != null) && (this.triggers.size() > 0));
  }
  
  public boolean fireBeforeRow(Session paramSession, Row paramRow1, Row paramRow2)
  {
    boolean bool = fireRow(paramSession, paramRow1, paramRow2, true, false);
    fireConstraints(paramSession, paramRow1, paramRow2, true);
    return bool;
  }
  
  private void fireConstraints(Session paramSession, Row paramRow1, Row paramRow2, boolean paramBoolean)
  {
    if (this.constraints != null)
    {
      int i = 0;
      for (int j = this.constraints.size(); i < j; i++)
      {
        Constraint localConstraint = (Constraint)this.constraints.get(i);
        if (localConstraint.isBefore() == paramBoolean) {
          localConstraint.checkRow(paramSession, this, paramRow1, paramRow2);
        }
      }
    }
  }
  
  public void fireAfterRow(Session paramSession, Row paramRow1, Row paramRow2, boolean paramBoolean)
  {
    fireRow(paramSession, paramRow1, paramRow2, false, paramBoolean);
    if (!paramBoolean) {
      fireConstraints(paramSession, paramRow1, paramRow2, false);
    }
  }
  
  private boolean fireRow(Session paramSession, Row paramRow1, Row paramRow2, boolean paramBoolean1, boolean paramBoolean2)
  {
    if (this.triggers != null) {
      for (TriggerObject localTriggerObject : this.triggers)
      {
        boolean bool = localTriggerObject.fireRow(paramSession, paramRow1, paramRow2, paramBoolean1, paramBoolean2);
        if (bool) {
          return true;
        }
      }
    }
    return false;
  }
  
  public boolean isGlobalTemporary()
  {
    return false;
  }
  
  public boolean canTruncate()
  {
    return false;
  }
  
  public void setCheckForeignKeyConstraints(Session paramSession, boolean paramBoolean1, boolean paramBoolean2)
  {
    if ((paramBoolean1) && (paramBoolean2) && 
      (this.constraints != null)) {
      for (Constraint localConstraint : this.constraints) {
        localConstraint.checkExistingData(paramSession);
      }
    }
    this.checkForeignKeyConstraints = paramBoolean1;
  }
  
  public boolean getCheckForeignKeyConstraints()
  {
    return this.checkForeignKeyConstraints;
  }
  
  public Index getIndexForColumn(Column paramColumn)
  {
    ArrayList localArrayList = getIndexes();
    if (localArrayList != null)
    {
      int i = 1;
      for (int j = localArrayList.size(); i < j; i++)
      {
        Index localIndex = (Index)localArrayList.get(i);
        if (localIndex.canGetFirstOrLast())
        {
          int k = localIndex.getColumnIndex(paramColumn);
          if (k == 0) {
            return localIndex;
          }
        }
      }
    }
    return null;
  }
  
  public boolean getOnCommitDrop()
  {
    return this.onCommitDrop;
  }
  
  public void setOnCommitDrop(boolean paramBoolean)
  {
    this.onCommitDrop = paramBoolean;
  }
  
  public boolean getOnCommitTruncate()
  {
    return this.onCommitTruncate;
  }
  
  public void setOnCommitTruncate(boolean paramBoolean)
  {
    this.onCommitTruncate = paramBoolean;
  }
  
  public void removeIndexOrTransferOwnership(Session paramSession, Index paramIndex)
  {
    int i = 0;
    if (this.constraints != null) {
      for (Constraint localConstraint : this.constraints) {
        if (localConstraint.usesIndex(paramIndex))
        {
          localConstraint.setIndexOwner(paramIndex);
          this.database.updateMeta(paramSession, localConstraint);
          i = 1;
        }
      }
    }
    if (i == 0) {
      this.database.removeSchemaObject(paramSession, paramIndex);
    }
  }
  
  public ArrayList<Session> checkDeadlock(Session paramSession1, Session paramSession2, Set<Session> paramSet)
  {
    return null;
  }
  
  public boolean isPersistIndexes()
  {
    return this.persistIndexes;
  }
  
  public boolean isPersistData()
  {
    return this.persistData;
  }
  
  public int compareTypeSave(Value paramValue1, Value paramValue2)
  {
    if (paramValue1 == paramValue2) {
      return 0;
    }
    int i = Value.getHigherOrder(paramValue1.getType(), paramValue2.getType());
    paramValue1 = paramValue1.convertTo(i);
    paramValue2 = paramValue2.convertTo(i);
    return paramValue1.compareTypeSave(paramValue2, this.compareMode);
  }
  
  public CompareMode getCompareMode()
  {
    return this.compareMode;
  }
  
  public void checkWritingAllowed()
  {
    this.database.checkWritingAllowed();
  }
  
  public Value getDefaultValue(Session paramSession, Column paramColumn)
  {
    Expression localExpression = paramColumn.getDefaultExpression();
    Value localValue;
    if (localExpression == null) {
      localValue = paramColumn.validateConvertUpdateSequence(paramSession, null);
    } else {
      localValue = localExpression.getValue(paramSession);
    }
    return paramColumn.convert(localValue);
  }
  
  public boolean isHidden()
  {
    return this.isHidden;
  }
  
  public void setHidden(boolean paramBoolean)
  {
    this.isHidden = paramBoolean;
  }
  
  public boolean isMVStore()
  {
    return false;
  }
}
