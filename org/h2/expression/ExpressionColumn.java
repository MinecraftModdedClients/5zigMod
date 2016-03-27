package org.h2.expression;

import java.util.HashMap;
import org.h2.command.Parser;
import org.h2.command.dml.Select;
import org.h2.command.dml.SelectListColumnResolver;
import org.h2.engine.Database;
import org.h2.engine.DbSettings;
import org.h2.engine.Session;
import org.h2.index.IndexCondition;
import org.h2.message.DbException;
import org.h2.schema.Constant;
import org.h2.schema.Schema;
import org.h2.table.Column;
import org.h2.table.ColumnResolver;
import org.h2.table.Table;
import org.h2.table.TableFilter;
import org.h2.value.Value;
import org.h2.value.ValueBoolean;

public class ExpressionColumn
  extends Expression
{
  private final Database database;
  private final String schemaName;
  private final String tableAlias;
  private final String columnName;
  private ColumnResolver columnResolver;
  private int queryLevel;
  private Column column;
  private boolean evaluatable;
  
  public ExpressionColumn(Database paramDatabase, Column paramColumn)
  {
    this.database = paramDatabase;
    this.column = paramColumn;
    this.schemaName = null;
    this.tableAlias = null;
    this.columnName = null;
  }
  
  public ExpressionColumn(Database paramDatabase, String paramString1, String paramString2, String paramString3)
  {
    this.database = paramDatabase;
    this.schemaName = paramString1;
    this.tableAlias = paramString2;
    this.columnName = paramString3;
  }
  
  public String getSQL()
  {
    boolean bool = this.database.getSettings().databaseToUpper;
    String str1;
    if (this.column != null) {
      str1 = this.column.getSQL();
    } else {
      str1 = bool ? Parser.quoteIdentifier(this.columnName) : this.columnName;
    }
    String str2;
    if (this.tableAlias != null)
    {
      str2 = bool ? Parser.quoteIdentifier(this.tableAlias) : this.tableAlias;
      str1 = str2 + "." + str1;
    }
    if (this.schemaName != null)
    {
      str2 = bool ? Parser.quoteIdentifier(this.schemaName) : this.schemaName;
      str1 = str2 + "." + str1;
    }
    return str1;
  }
  
  public TableFilter getTableFilter()
  {
    return this.columnResolver == null ? null : this.columnResolver.getTableFilter();
  }
  
  public void mapColumns(ColumnResolver paramColumnResolver, int paramInt)
  {
    if ((this.tableAlias != null) && (!this.database.equalsIdentifiers(this.tableAlias, paramColumnResolver.getTableAlias()))) {
      return;
    }
    if ((this.schemaName != null) && (!this.database.equalsIdentifiers(this.schemaName, paramColumnResolver.getSchemaName()))) {
      return;
    }
    for (Column localColumn2 : paramColumnResolver.getColumns())
    {
      String str = localColumn2.getName();
      if (this.database.equalsIdentifiers(this.columnName, str))
      {
        mapColumn(paramColumnResolver, localColumn2, paramInt);
        return;
      }
    }
    if (this.database.equalsIdentifiers("_ROWID_", this.columnName))
    {
      ??? = paramColumnResolver.getRowIdColumn();
      if (??? != null)
      {
        mapColumn(paramColumnResolver, (Column)???, paramInt);
        return;
      }
    }
    ??? = paramColumnResolver.getSystemColumns();
    for (??? = 0; (??? != null) && (??? < ???.length); ???++)
    {
      Column localColumn1 = ???[???];
      if (this.database.equalsIdentifiers(this.columnName, localColumn1.getName()))
      {
        mapColumn(paramColumnResolver, localColumn1, paramInt);
        return;
      }
    }
  }
  
  private void mapColumn(ColumnResolver paramColumnResolver, Column paramColumn, int paramInt)
  {
    if (this.columnResolver == null)
    {
      this.queryLevel = paramInt;
      this.column = paramColumn;
      this.columnResolver = paramColumnResolver;
    }
    else if ((this.queryLevel == paramInt) && (this.columnResolver != paramColumnResolver) && 
      (!(paramColumnResolver instanceof SelectListColumnResolver)))
    {
      throw DbException.get(90059, this.columnName);
    }
  }
  
  public Expression optimize(Session paramSession)
  {
    if (this.columnResolver == null)
    {
      Schema localSchema = paramSession.getDatabase().findSchema(this.tableAlias == null ? paramSession.getCurrentSchemaName() : this.tableAlias);
      if (localSchema != null)
      {
        localObject = localSchema.findConstant(this.columnName);
        if (localObject != null) {
          return ((Constant)localObject).getValue();
        }
      }
      Object localObject = this.columnName;
      if (this.tableAlias != null)
      {
        localObject = this.tableAlias + "." + (String)localObject;
        if (this.schemaName != null) {
          localObject = this.schemaName + "." + (String)localObject;
        }
      }
      throw DbException.get(42122, (String)localObject);
    }
    return this.columnResolver.optimize(this, this.column);
  }
  
  public void updateAggregate(Session paramSession)
  {
    Value localValue1 = this.columnResolver.getValue(this.column);
    Select localSelect = this.columnResolver.getSelect();
    if (localSelect == null) {
      throw DbException.get(90016, getSQL());
    }
    HashMap localHashMap = localSelect.getCurrentGroup();
    if (localHashMap == null) {
      return;
    }
    Value localValue2 = (Value)localHashMap.get(this);
    if (localValue2 == null) {
      localHashMap.put(this, localValue1);
    } else if (!this.database.areEqual(localValue1, localValue2)) {
      throw DbException.get(90016, getSQL());
    }
  }
  
  public Value getValue(Session paramSession)
  {
    Select localSelect = this.columnResolver.getSelect();
    if (localSelect != null)
    {
      localObject = localSelect.getCurrentGroup();
      if (localObject != null)
      {
        Value localValue = (Value)((HashMap)localObject).get(this);
        if (localValue != null) {
          return localValue;
        }
      }
    }
    Object localObject = this.columnResolver.getValue(this.column);
    if (localObject == null)
    {
      this.columnResolver.getValue(this.column);
      throw DbException.get(90016, getSQL());
    }
    return (Value)localObject;
  }
  
  public int getType()
  {
    return this.column.getType();
  }
  
  public void setEvaluatable(TableFilter paramTableFilter, boolean paramBoolean)
  {
    if ((this.columnResolver != null) && (paramTableFilter == this.columnResolver.getTableFilter())) {
      this.evaluatable = paramBoolean;
    }
  }
  
  public Column getColumn()
  {
    return this.column;
  }
  
  public int getScale()
  {
    return this.column.getScale();
  }
  
  public long getPrecision()
  {
    return this.column.getPrecision();
  }
  
  public int getDisplaySize()
  {
    return this.column.getDisplaySize();
  }
  
  public String getOriginalColumnName()
  {
    return this.columnName;
  }
  
  public String getOriginalTableAliasName()
  {
    return this.tableAlias;
  }
  
  public String getColumnName()
  {
    return this.columnName != null ? this.columnName : this.column.getName();
  }
  
  public String getSchemaName()
  {
    Table localTable = this.column.getTable();
    return localTable == null ? null : localTable.getSchema().getName();
  }
  
  public String getTableName()
  {
    Table localTable = this.column.getTable();
    return localTable == null ? null : localTable.getName();
  }
  
  public String getAlias()
  {
    return this.column == null ? null : this.column.getName();
  }
  
  public boolean isAutoIncrement()
  {
    return this.column.getSequence() != null;
  }
  
  public int getNullable()
  {
    return this.column.isNullable() ? 1 : 0;
  }
  
  public boolean isEverything(ExpressionVisitor paramExpressionVisitor)
  {
    switch (paramExpressionVisitor.getType())
    {
    case 1: 
      return false;
    case 2: 
    case 5: 
    case 8: 
      return true;
    case 0: 
      return this.queryLevel < paramExpressionVisitor.getQueryLevel();
    case 3: 
      if (this.database.getSettings().nestedJoins)
      {
        if (paramExpressionVisitor.getQueryLevel() < this.queryLevel) {
          return true;
        }
        if (getTableFilter() == null) {
          return false;
        }
        return getTableFilter().isEvaluatable();
      }
      return (this.evaluatable) || (paramExpressionVisitor.getQueryLevel() < this.queryLevel);
    case 4: 
      paramExpressionVisitor.addDataModificationId(this.column.getTable().getMaxDataModificationId());
      return true;
    case 6: 
      return this.columnResolver != paramExpressionVisitor.getResolver();
    case 7: 
      if (this.column != null) {
        paramExpressionVisitor.addDependency(this.column.getTable());
      }
      return true;
    case 9: 
      paramExpressionVisitor.addColumn(this.column);
      return true;
    }
    throw DbException.throwInternalError("type=" + paramExpressionVisitor.getType());
  }
  
  public int getCost()
  {
    return 2;
  }
  
  public void createIndexConditions(Session paramSession, TableFilter paramTableFilter)
  {
    TableFilter localTableFilter = getTableFilter();
    if ((paramTableFilter == localTableFilter) && (this.column.getType() == 1))
    {
      IndexCondition localIndexCondition = IndexCondition.get(0, this, ValueExpression.get(ValueBoolean.get(true)));
      
      paramTableFilter.addIndexCondition(localIndexCondition);
    }
  }
  
  public Expression getNotIfPossible(Session paramSession)
  {
    return new Comparison(paramSession, 0, this, ValueExpression.get(ValueBoolean.get(false)));
  }
}
