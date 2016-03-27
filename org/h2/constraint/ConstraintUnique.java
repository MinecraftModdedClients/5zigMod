package org.h2.constraint;

import java.util.HashSet;
import org.h2.command.Parser;
import org.h2.engine.Database;
import org.h2.engine.Session;
import org.h2.index.Index;
import org.h2.result.Row;
import org.h2.schema.Schema;
import org.h2.table.Column;
import org.h2.table.IndexColumn;
import org.h2.table.Table;
import org.h2.util.New;
import org.h2.util.StatementBuilder;
import org.h2.util.StringUtils;

public class ConstraintUnique
  extends Constraint
{
  private Index index;
  private boolean indexOwner;
  private IndexColumn[] columns;
  private final boolean primaryKey;
  
  public ConstraintUnique(Schema paramSchema, int paramInt, String paramString, Table paramTable, boolean paramBoolean)
  {
    super(paramSchema, paramInt, paramString, paramTable);
    this.primaryKey = paramBoolean;
  }
  
  public String getConstraintType()
  {
    return this.primaryKey ? "PRIMARY KEY" : "UNIQUE";
  }
  
  public String getCreateSQLForCopy(Table paramTable, String paramString)
  {
    return getCreateSQLForCopy(paramTable, paramString, true);
  }
  
  private String getCreateSQLForCopy(Table paramTable, String paramString, boolean paramBoolean)
  {
    StatementBuilder localStatementBuilder = new StatementBuilder("ALTER TABLE ");
    localStatementBuilder.append(paramTable.getSQL()).append(" ADD CONSTRAINT ");
    if (paramTable.isHidden()) {
      localStatementBuilder.append("IF NOT EXISTS ");
    }
    localStatementBuilder.append(paramString);
    if (this.comment != null) {
      localStatementBuilder.append(" COMMENT ").append(StringUtils.quoteStringSQL(this.comment));
    }
    localStatementBuilder.append(' ').append(getTypeName()).append('(');
    for (IndexColumn localIndexColumn : this.columns)
    {
      localStatementBuilder.appendExceptFirst(", ");
      localStatementBuilder.append(Parser.quoteIdentifier(localIndexColumn.column.getName()));
    }
    localStatementBuilder.append(')');
    if ((paramBoolean) && (this.indexOwner) && (paramTable == this.table)) {
      localStatementBuilder.append(" INDEX ").append(this.index.getSQL());
    }
    return localStatementBuilder.toString();
  }
  
  private String getTypeName()
  {
    if (this.primaryKey) {
      return "PRIMARY KEY";
    }
    return "UNIQUE";
  }
  
  public String getCreateSQLWithoutIndexes()
  {
    return getCreateSQLForCopy(this.table, getSQL(), false);
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
  
  public void setIndex(Index paramIndex, boolean paramBoolean)
  {
    this.index = paramIndex;
    this.indexOwner = paramBoolean;
  }
  
  public void removeChildrenAndResources(Session paramSession)
  {
    this.table.removeConstraint(this);
    if (this.indexOwner) {
      this.table.removeIndexOrTransferOwnership(paramSession, this.index);
    }
    this.database.removeMeta(paramSession, getId());
    this.index = null;
    this.columns = null;
    this.table = null;
    invalidate();
  }
  
  public void checkRow(Session paramSession, Table paramTable, Row paramRow1, Row paramRow2) {}
  
  public boolean usesIndex(Index paramIndex)
  {
    return paramIndex == this.index;
  }
  
  public void setIndexOwner(Index paramIndex)
  {
    this.indexOwner = true;
  }
  
  public HashSet<Column> getReferencedColumns(Table paramTable)
  {
    HashSet localHashSet = New.hashSet();
    for (IndexColumn localIndexColumn : this.columns) {
      localHashSet.add(localIndexColumn.column);
    }
    return localHashSet;
  }
  
  public boolean isBefore()
  {
    return true;
  }
  
  public void checkExistingData(Session paramSession) {}
  
  public Index getUniqueIndex()
  {
    return this.index;
  }
  
  public void rebuild() {}
}
