package org.h2.constraint;

import java.util.ArrayList;
import java.util.HashSet;
import org.h2.command.Parser;
import org.h2.command.Prepared;
import org.h2.engine.Database;
import org.h2.engine.Session;
import org.h2.expression.Expression;
import org.h2.expression.Parameter;
import org.h2.index.Cursor;
import org.h2.index.Index;
import org.h2.message.DbException;
import org.h2.result.ResultInterface;
import org.h2.result.Row;
import org.h2.result.SearchRow;
import org.h2.schema.Schema;
import org.h2.table.Column;
import org.h2.table.IndexColumn;
import org.h2.table.Table;
import org.h2.util.New;
import org.h2.util.StatementBuilder;
import org.h2.util.StringUtils;
import org.h2.value.Value;
import org.h2.value.ValueNull;

public class ConstraintReferential
  extends Constraint
{
  public static final int RESTRICT = 0;
  public static final int CASCADE = 1;
  public static final int SET_DEFAULT = 2;
  public static final int SET_NULL = 3;
  private IndexColumn[] columns;
  private IndexColumn[] refColumns;
  private int deleteAction;
  private int updateAction;
  private Table refTable;
  private Index index;
  private Index refIndex;
  private boolean indexOwner;
  private boolean refIndexOwner;
  private String deleteSQL;
  private String updateSQL;
  private boolean skipOwnTable;
  
  public ConstraintReferential(Schema paramSchema, int paramInt, String paramString, Table paramTable)
  {
    super(paramSchema, paramInt, paramString, paramTable);
  }
  
  public String getConstraintType()
  {
    return "REFERENTIAL";
  }
  
  private static void appendAction(StatementBuilder paramStatementBuilder, int paramInt)
  {
    switch (paramInt)
    {
    case 1: 
      paramStatementBuilder.append("CASCADE");
      break;
    case 2: 
      paramStatementBuilder.append("SET DEFAULT");
      break;
    case 3: 
      paramStatementBuilder.append("SET NULL");
      break;
    default: 
      DbException.throwInternalError("action=" + paramInt);
    }
  }
  
  public String getCreateSQLForCopy(Table paramTable, String paramString)
  {
    return getCreateSQLForCopy(paramTable, this.refTable, paramString, true);
  }
  
  public String getCreateSQLForCopy(Table paramTable1, Table paramTable2, String paramString, boolean paramBoolean)
  {
    StatementBuilder localStatementBuilder = new StatementBuilder("ALTER TABLE ");
    String str = paramTable1.getSQL();
    localStatementBuilder.append(str).append(" ADD CONSTRAINT ");
    if (paramTable1.isHidden()) {
      localStatementBuilder.append("IF NOT EXISTS ");
    }
    localStatementBuilder.append(paramString);
    if (this.comment != null) {
      localStatementBuilder.append(" COMMENT ").append(StringUtils.quoteStringSQL(this.comment));
    }
    IndexColumn[] arrayOfIndexColumn1 = this.columns;
    IndexColumn[] arrayOfIndexColumn2 = this.refColumns;
    localStatementBuilder.append(" FOREIGN KEY(");
    for (Object localObject2 : arrayOfIndexColumn1)
    {
      localStatementBuilder.appendExceptFirst(", ");
      localStatementBuilder.append(((IndexColumn)localObject2).getSQL());
    }
    localStatementBuilder.append(')');
    if ((paramBoolean) && (this.indexOwner) && (paramTable1 == this.table)) {
      localStatementBuilder.append(" INDEX ").append(this.index.getSQL());
    }
    localStatementBuilder.append(" REFERENCES ");
    if (this.table == this.refTable) {
      ??? = paramTable1.getSQL();
    } else {
      ??? = paramTable2.getSQL();
    }
    localStatementBuilder.append((String)???).append('(');
    localStatementBuilder.resetCount();
    for (IndexColumn localIndexColumn : arrayOfIndexColumn2)
    {
      localStatementBuilder.appendExceptFirst(", ");
      localStatementBuilder.append(localIndexColumn.getSQL());
    }
    localStatementBuilder.append(')');
    if ((paramBoolean) && (this.refIndexOwner) && (paramTable1 == this.table)) {
      localStatementBuilder.append(" INDEX ").append(this.refIndex.getSQL());
    }
    if (this.deleteAction != 0)
    {
      localStatementBuilder.append(" ON DELETE ");
      appendAction(localStatementBuilder, this.deleteAction);
    }
    if (this.updateAction != 0)
    {
      localStatementBuilder.append(" ON UPDATE ");
      appendAction(localStatementBuilder, this.updateAction);
    }
    return localStatementBuilder.append(" NOCHECK").toString();
  }
  
  private String getShortDescription(Index paramIndex, SearchRow paramSearchRow)
  {
    StatementBuilder localStatementBuilder = new StatementBuilder(getName());
    localStatementBuilder.append(": ").append(this.table.getSQL()).append(" FOREIGN KEY(");
    Object localObject2;
    for (localObject2 : this.columns)
    {
      localStatementBuilder.appendExceptFirst(", ");
      localStatementBuilder.append(((IndexColumn)localObject2).getSQL());
    }
    localStatementBuilder.append(") REFERENCES ").append(this.refTable.getSQL()).append('(');
    localStatementBuilder.resetCount();
    for (localObject2 : this.refColumns)
    {
      localStatementBuilder.appendExceptFirst(", ");
      localStatementBuilder.append(((IndexColumn)localObject2).getSQL());
    }
    localStatementBuilder.append(')');
    if ((paramIndex != null) && (paramSearchRow != null))
    {
      localStatementBuilder.append(" (");
      localStatementBuilder.resetCount();
      ??? = paramIndex.getColumns();
      ??? = Math.min(this.columns.length, ???.length);
      for (??? = 0; ??? < ???; ???++)
      {
        int k = ???[???].getColumnId();
        Value localValue = paramSearchRow.getValue(k);
        localStatementBuilder.appendExceptFirst(", ");
        localStatementBuilder.append(localValue == null ? "" : localValue.toString());
      }
      localStatementBuilder.append(')');
    }
    return localStatementBuilder.toString();
  }
  
  public String getCreateSQLWithoutIndexes()
  {
    return getCreateSQLForCopy(this.table, this.refTable, getSQL(), false);
  }
  
  public String getCreateSQL()
  {
    return getCreateSQLForCopy(this.table, getSQL());
  }
  
  public void setColumns(IndexColumn[] paramArrayOfIndexColumn)
  {
    this.columns = paramArrayOfIndexColumn;
  }
  
  public IndexColumn[] getColumns()
  {
    return this.columns;
  }
  
  public HashSet<Column> getReferencedColumns(Table paramTable)
  {
    HashSet localHashSet = New.hashSet();
    IndexColumn localIndexColumn;
    if (paramTable == this.table) {
      for (localIndexColumn : this.columns) {
        localHashSet.add(localIndexColumn.column);
      }
    } else if (paramTable == this.refTable) {
      for (localIndexColumn : this.refColumns) {
        localHashSet.add(localIndexColumn.column);
      }
    }
    return localHashSet;
  }
  
  public void setRefColumns(IndexColumn[] paramArrayOfIndexColumn)
  {
    this.refColumns = paramArrayOfIndexColumn;
  }
  
  public IndexColumn[] getRefColumns()
  {
    return this.refColumns;
  }
  
  public void setRefTable(Table paramTable)
  {
    this.refTable = paramTable;
    if (paramTable.isTemporary()) {
      setTemporary(true);
    }
  }
  
  public void setIndex(Index paramIndex, boolean paramBoolean)
  {
    this.index = paramIndex;
    this.indexOwner = paramBoolean;
  }
  
  public void setRefIndex(Index paramIndex, boolean paramBoolean)
  {
    this.refIndex = paramIndex;
    this.refIndexOwner = paramBoolean;
  }
  
  public void removeChildrenAndResources(Session paramSession)
  {
    this.table.removeConstraint(this);
    this.refTable.removeConstraint(this);
    if (this.indexOwner) {
      this.table.removeIndexOrTransferOwnership(paramSession, this.index);
    }
    if (this.refIndexOwner) {
      this.refTable.removeIndexOrTransferOwnership(paramSession, this.refIndex);
    }
    this.database.removeMeta(paramSession, getId());
    this.refTable = null;
    this.index = null;
    this.refIndex = null;
    this.columns = null;
    this.refColumns = null;
    this.deleteSQL = null;
    this.updateSQL = null;
    this.table = null;
    invalidate();
  }
  
  public void checkRow(Session paramSession, Table paramTable, Row paramRow1, Row paramRow2)
  {
    if (!this.database.getReferentialIntegrity()) {
      return;
    }
    if ((!this.table.getCheckForeignKeyConstraints()) || (!this.refTable.getCheckForeignKeyConstraints())) {
      return;
    }
    if ((paramTable == this.table) && 
      (!this.skipOwnTable)) {
      checkRowOwnTable(paramSession, paramRow1, paramRow2);
    }
    if (paramTable == this.refTable) {
      checkRowRefTable(paramSession, paramRow1, paramRow2);
    }
  }
  
  private void checkRowOwnTable(Session paramSession, Row paramRow1, Row paramRow2)
  {
    if (paramRow2 == null) {
      return;
    }
    int i = paramRow1 != null ? 1 : 0;
    Object localObject;
    for (IndexColumn localIndexColumn : this.columns)
    {
      int i1 = localIndexColumn.column.getColumnId();
      localObject = paramRow2.getValue(i1);
      if (localObject == ValueNull.INSTANCE) {
        return;
      }
      if ((i != 0) && 
        (!this.database.areEqual((Value)localObject, paramRow1.getValue(i1)))) {
        i = 0;
      }
    }
    if (i != 0) {
      return;
    }
    int n;
    Value localValue1;
    int i2;
    if (this.refTable == this.table)
    {
      int j = 1;
      ??? = 0;
      for (??? = this.columns.length; ??? < ???; ???++)
      {
        n = this.columns[???].column.getColumnId();
        localValue1 = paramRow2.getValue(n);
        localObject = this.refColumns[???].column;
        i2 = ((Column)localObject).getColumnId();
        Value localValue2 = paramRow2.getValue(i2);
        if (!this.database.areEqual(localValue2, localValue1))
        {
          j = 0;
          break;
        }
      }
      if (j != 0) {
        return;
      }
    }
    Row localRow = this.refTable.getTemplateRow();
    ??? = 0;
    for (??? = this.columns.length; ??? < ???; ???++)
    {
      n = this.columns[???].column.getColumnId();
      localValue1 = paramRow2.getValue(n);
      localObject = this.refColumns[???].column;
      i2 = ((Column)localObject).getColumnId();
      localRow.setValue(i2, ((Column)localObject).convert(localValue1));
    }
    if (!existsRow(paramSession, this.refIndex, localRow, null)) {
      throw DbException.get(23506, getShortDescription(this.refIndex, localRow));
    }
  }
  
  private boolean existsRow(Session paramSession, Index paramIndex, SearchRow paramSearchRow, Row paramRow)
  {
    Table localTable = paramIndex.getTable();
    localTable.lock(paramSession, false, false);
    Cursor localCursor = paramIndex.find(paramSession, paramSearchRow, paramSearchRow);
    while (localCursor.next())
    {
      SearchRow localSearchRow = localCursor.getSearchRow();
      if ((paramRow == null) || (localSearchRow.getKey() != paramRow.getKey()))
      {
        Column[] arrayOfColumn = paramIndex.getColumns();
        int i = 1;
        int j = Math.min(this.columns.length, arrayOfColumn.length);
        for (int k = 0; k < j; k++)
        {
          int m = arrayOfColumn[k].getColumnId();
          Value localValue1 = paramSearchRow.getValue(m);
          Value localValue2 = localSearchRow.getValue(m);
          if (localTable.compareTypeSave(localValue1, localValue2) != 0)
          {
            i = 0;
            break;
          }
        }
        if (i != 0) {
          return true;
        }
      }
    }
    return false;
  }
  
  private boolean isEqual(Row paramRow1, Row paramRow2)
  {
    return this.refIndex.compareRows(paramRow1, paramRow2) == 0;
  }
  
  private void checkRow(Session paramSession, Row paramRow)
  {
    SearchRow localSearchRow = this.table.getTemplateSimpleRow(false);
    int i = 0;
    for (int j = this.columns.length; i < j; i++)
    {
      Column localColumn1 = this.refColumns[i].column;
      int k = localColumn1.getColumnId();
      Column localColumn2 = this.columns[i].column;
      Value localValue = localColumn2.convert(paramRow.getValue(k));
      if (localValue == ValueNull.INSTANCE) {
        return;
      }
      localSearchRow.setValue(localColumn2.getColumnId(), localValue);
    }
    Row localRow = this.refTable == this.table ? paramRow : null;
    if (existsRow(paramSession, this.index, localSearchRow, localRow)) {
      throw DbException.get(23503, getShortDescription(this.index, localSearchRow));
    }
  }
  
  private void checkRowRefTable(Session paramSession, Row paramRow1, Row paramRow2)
  {
    if (paramRow1 == null) {
      return;
    }
    if ((paramRow2 != null) && (isEqual(paramRow1, paramRow2))) {
      return;
    }
    Object localObject;
    if (paramRow2 == null)
    {
      if (this.deleteAction == 0)
      {
        checkRow(paramSession, paramRow1);
      }
      else
      {
        int i = this.deleteAction == 1 ? 0 : this.columns.length;
        localObject = getDelete(paramSession);
        setWhere((Prepared)localObject, i, paramRow1);
        updateWithSkipCheck((Prepared)localObject);
      }
    }
    else if (this.updateAction == 0)
    {
      checkRow(paramSession, paramRow1);
    }
    else
    {
      Prepared localPrepared = getUpdate(paramSession);
      if (this.updateAction == 1)
      {
        localObject = localPrepared.getParameters();
        int j = 0;
        for (int k = this.columns.length; j < k; j++)
        {
          Parameter localParameter = (Parameter)((ArrayList)localObject).get(j);
          Column localColumn = this.refColumns[j].column;
          localParameter.setValue(paramRow2.getValue(localColumn.getColumnId()));
        }
      }
      setWhere(localPrepared, this.columns.length, paramRow1);
      updateWithSkipCheck(localPrepared);
    }
  }
  
  private void updateWithSkipCheck(Prepared paramPrepared)
  {
    try
    {
      this.skipOwnTable = true;
      paramPrepared.update();
    }
    finally
    {
      this.skipOwnTable = false;
    }
  }
  
  private void setWhere(Prepared paramPrepared, int paramInt, Row paramRow)
  {
    int i = 0;
    for (int j = this.refColumns.length; i < j; i++)
    {
      int k = this.refColumns[i].column.getColumnId();
      Value localValue = paramRow.getValue(k);
      ArrayList localArrayList = paramPrepared.getParameters();
      Parameter localParameter = (Parameter)localArrayList.get(paramInt + i);
      localParameter.setValue(localValue);
    }
  }
  
  public int getDeleteAction()
  {
    return this.deleteAction;
  }
  
  public void setDeleteAction(int paramInt)
  {
    if ((paramInt == this.deleteAction) && (this.deleteSQL == null)) {
      return;
    }
    if (this.deleteAction != 0) {
      throw DbException.get(90045, "ON DELETE");
    }
    this.deleteAction = paramInt;
    buildDeleteSQL();
  }
  
  private void buildDeleteSQL()
  {
    if (this.deleteAction == 0) {
      return;
    }
    StatementBuilder localStatementBuilder = new StatementBuilder();
    if (this.deleteAction == 1) {
      localStatementBuilder.append("DELETE FROM ").append(this.table.getSQL());
    } else {
      appendUpdate(localStatementBuilder);
    }
    appendWhere(localStatementBuilder);
    this.deleteSQL = localStatementBuilder.toString();
  }
  
  private Prepared getUpdate(Session paramSession)
  {
    return prepare(paramSession, this.updateSQL, this.updateAction);
  }
  
  private Prepared getDelete(Session paramSession)
  {
    return prepare(paramSession, this.deleteSQL, this.deleteAction);
  }
  
  public int getUpdateAction()
  {
    return this.updateAction;
  }
  
  public void setUpdateAction(int paramInt)
  {
    if ((paramInt == this.updateAction) && (this.updateSQL == null)) {
      return;
    }
    if (this.updateAction != 0) {
      throw DbException.get(90045, "ON UPDATE");
    }
    this.updateAction = paramInt;
    buildUpdateSQL();
  }
  
  private void buildUpdateSQL()
  {
    if (this.updateAction == 0) {
      return;
    }
    StatementBuilder localStatementBuilder = new StatementBuilder();
    appendUpdate(localStatementBuilder);
    appendWhere(localStatementBuilder);
    this.updateSQL = localStatementBuilder.toString();
  }
  
  public void rebuild()
  {
    buildUpdateSQL();
    buildDeleteSQL();
  }
  
  private Prepared prepare(Session paramSession, String paramString, int paramInt)
  {
    Prepared localPrepared = paramSession.prepare(paramString);
    if (paramInt != 1)
    {
      ArrayList localArrayList = localPrepared.getParameters();
      int i = 0;
      for (int j = this.columns.length; i < j; i++)
      {
        Column localColumn = this.columns[i].column;
        Parameter localParameter = (Parameter)localArrayList.get(i);
        Object localObject;
        if (paramInt == 3)
        {
          localObject = ValueNull.INSTANCE;
        }
        else
        {
          Expression localExpression = localColumn.getDefaultExpression();
          if (localExpression == null) {
            throw DbException.get(23507, localColumn.getName());
          }
          localObject = localExpression.getValue(paramSession);
        }
        localParameter.setValue((Value)localObject);
      }
    }
    return localPrepared;
  }
  
  private void appendUpdate(StatementBuilder paramStatementBuilder)
  {
    paramStatementBuilder.append("UPDATE ").append(this.table.getSQL()).append(" SET ");
    paramStatementBuilder.resetCount();
    for (IndexColumn localIndexColumn : this.columns)
    {
      paramStatementBuilder.appendExceptFirst(" , ");
      paramStatementBuilder.append(Parser.quoteIdentifier(localIndexColumn.column.getName())).append("=?");
    }
  }
  
  private void appendWhere(StatementBuilder paramStatementBuilder)
  {
    paramStatementBuilder.append(" WHERE ");
    paramStatementBuilder.resetCount();
    for (IndexColumn localIndexColumn : this.columns)
    {
      paramStatementBuilder.appendExceptFirst(" AND ");
      paramStatementBuilder.append(Parser.quoteIdentifier(localIndexColumn.column.getName())).append("=?");
    }
  }
  
  public Table getRefTable()
  {
    return this.refTable;
  }
  
  public boolean usesIndex(Index paramIndex)
  {
    return (paramIndex == this.index) || (paramIndex == this.refIndex);
  }
  
  public void setIndexOwner(Index paramIndex)
  {
    if (this.index == paramIndex) {
      this.indexOwner = true;
    } else if (this.refIndex == paramIndex) {
      this.refIndexOwner = true;
    } else {
      DbException.throwInternalError();
    }
  }
  
  public boolean isBefore()
  {
    return false;
  }
  
  public void checkExistingData(Session paramSession)
  {
    if (paramSession.getDatabase().isStarting()) {
      return;
    }
    paramSession.startStatementWithinTransaction();
    StatementBuilder localStatementBuilder = new StatementBuilder("SELECT 1 FROM (SELECT ");
    IndexColumn localIndexColumn;
    for (localIndexColumn : this.columns)
    {
      localStatementBuilder.appendExceptFirst(", ");
      localStatementBuilder.append(localIndexColumn.getSQL());
    }
    localStatementBuilder.append(" FROM ").append(this.table.getSQL()).append(" WHERE ");
    localStatementBuilder.resetCount();
    for (localIndexColumn : this.columns)
    {
      localStatementBuilder.appendExceptFirst(" AND ");
      localStatementBuilder.append(localIndexColumn.getSQL()).append(" IS NOT NULL ");
    }
    localStatementBuilder.append(" ORDER BY ");
    localStatementBuilder.resetCount();
    for (localIndexColumn : this.columns)
    {
      localStatementBuilder.appendExceptFirst(", ");
      localStatementBuilder.append(localIndexColumn.getSQL());
    }
    localStatementBuilder.append(") C WHERE NOT EXISTS(SELECT 1 FROM ").append(this.refTable.getSQL()).append(" P WHERE ");
    
    localStatementBuilder.resetCount();
    int i = 0;
    for (Object localObject2 : this.columns)
    {
      localStatementBuilder.appendExceptFirst(" AND ");
      localStatementBuilder.append("C.").append(((IndexColumn)localObject2).getSQL()).append('=').append("P.").append(this.refColumns[(i++)].getSQL());
    }
    localStatementBuilder.append(')');
    ??? = localStatementBuilder.toString();
    ResultInterface localResultInterface = paramSession.prepare((String)???).query(1);
    if (localResultInterface.next()) {
      throw DbException.get(23506, getShortDescription(null, null));
    }
  }
  
  public Index getUniqueIndex()
  {
    return this.refIndex;
  }
}
