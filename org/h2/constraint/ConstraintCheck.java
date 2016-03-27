package org.h2.constraint;

import java.util.HashSet;
import java.util.Iterator;
import org.h2.command.Prepared;
import org.h2.engine.Database;
import org.h2.engine.Session;
import org.h2.expression.Expression;
import org.h2.expression.ExpressionVisitor;
import org.h2.index.Index;
import org.h2.message.DbException;
import org.h2.result.ResultInterface;
import org.h2.result.Row;
import org.h2.schema.Schema;
import org.h2.table.Column;
import org.h2.table.Table;
import org.h2.table.TableFilter;
import org.h2.util.New;
import org.h2.util.StringUtils;
import org.h2.value.Value;

public class ConstraintCheck
  extends Constraint
{
  private TableFilter filter;
  private Expression expr;
  
  public ConstraintCheck(Schema paramSchema, int paramInt, String paramString, Table paramTable)
  {
    super(paramSchema, paramInt, paramString, paramTable);
  }
  
  public String getConstraintType()
  {
    return "CHECK";
  }
  
  public void setTableFilter(TableFilter paramTableFilter)
  {
    this.filter = paramTableFilter;
  }
  
  public void setExpression(Expression paramExpression)
  {
    this.expr = paramExpression;
  }
  
  public String getCreateSQLForCopy(Table paramTable, String paramString)
  {
    StringBuilder localStringBuilder = new StringBuilder("ALTER TABLE ");
    localStringBuilder.append(paramTable.getSQL()).append(" ADD CONSTRAINT ");
    if (paramTable.isHidden()) {
      localStringBuilder.append("IF NOT EXISTS ");
    }
    localStringBuilder.append(paramString);
    if (this.comment != null) {
      localStringBuilder.append(" COMMENT ").append(StringUtils.quoteStringSQL(this.comment));
    }
    localStringBuilder.append(" CHECK").append(StringUtils.enclose(this.expr.getSQL())).append(" NOCHECK");
    
    return localStringBuilder.toString();
  }
  
  private String getShortDescription()
  {
    return getName() + ": " + this.expr.getSQL();
  }
  
  public String getCreateSQLWithoutIndexes()
  {
    return getCreateSQL();
  }
  
  public String getCreateSQL()
  {
    return getCreateSQLForCopy(this.table, getSQL());
  }
  
  public void removeChildrenAndResources(Session paramSession)
  {
    this.table.removeConstraint(this);
    this.database.removeMeta(paramSession, getId());
    this.filter = null;
    this.expr = null;
    this.table = null;
    invalidate();
  }
  
  public void checkRow(Session paramSession, Table paramTable, Row paramRow1, Row paramRow2)
  {
    if (paramRow2 == null) {
      return;
    }
    this.filter.set(paramRow2);
    Boolean localBoolean;
    try
    {
      localBoolean = this.expr.getValue(paramSession).getBoolean();
    }
    catch (DbException localDbException)
    {
      throw DbException.get(23514, localDbException, new String[] { getShortDescription() });
    }
    if (Boolean.FALSE.equals(localBoolean)) {
      throw DbException.get(23513, getShortDescription());
    }
  }
  
  public boolean usesIndex(Index paramIndex)
  {
    return false;
  }
  
  public void setIndexOwner(Index paramIndex)
  {
    DbException.throwInternalError();
  }
  
  public HashSet<Column> getReferencedColumns(Table paramTable)
  {
    HashSet localHashSet = New.hashSet();
    this.expr.isEverything(ExpressionVisitor.getColumnsVisitor(localHashSet));
    for (Iterator localIterator = localHashSet.iterator(); localIterator.hasNext();) {
      if (((Column)localIterator.next()).getTable() != paramTable) {
        localIterator.remove();
      }
    }
    return localHashSet;
  }
  
  public Expression getExpression()
  {
    return this.expr;
  }
  
  public boolean isBefore()
  {
    return true;
  }
  
  public void checkExistingData(Session paramSession)
  {
    if (paramSession.getDatabase().isStarting()) {
      return;
    }
    String str = "SELECT 1 FROM " + this.filter.getTable().getSQL() + " WHERE NOT(" + this.expr.getSQL() + ")";
    
    ResultInterface localResultInterface = paramSession.prepare(str).query(1);
    if (localResultInterface.next()) {
      throw DbException.get(23513, getName());
    }
  }
  
  public Index getUniqueIndex()
  {
    return null;
  }
  
  public void rebuild() {}
  
  public boolean isEverything(ExpressionVisitor paramExpressionVisitor)
  {
    return this.expr.isEverything(paramExpressionVisitor);
  }
}
