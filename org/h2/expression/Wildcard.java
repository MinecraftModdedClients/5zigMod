package org.h2.expression;

import org.h2.engine.Session;
import org.h2.message.DbException;
import org.h2.table.ColumnResolver;
import org.h2.table.TableFilter;
import org.h2.util.StringUtils;
import org.h2.value.Value;

public class Wildcard
  extends Expression
{
  private final String schema;
  private final String table;
  
  public Wildcard(String paramString1, String paramString2)
  {
    this.schema = paramString1;
    this.table = paramString2;
  }
  
  public boolean isWildcard()
  {
    return true;
  }
  
  public Value getValue(Session paramSession)
  {
    throw DbException.throwInternalError();
  }
  
  public int getType()
  {
    throw DbException.throwInternalError();
  }
  
  public void mapColumns(ColumnResolver paramColumnResolver, int paramInt)
  {
    throw DbException.get(42000, this.table);
  }
  
  public Expression optimize(Session paramSession)
  {
    throw DbException.get(42000, this.table);
  }
  
  public void setEvaluatable(TableFilter paramTableFilter, boolean paramBoolean)
  {
    DbException.throwInternalError();
  }
  
  public int getScale()
  {
    throw DbException.throwInternalError();
  }
  
  public long getPrecision()
  {
    throw DbException.throwInternalError();
  }
  
  public int getDisplaySize()
  {
    throw DbException.throwInternalError();
  }
  
  public String getTableAlias()
  {
    return this.table;
  }
  
  public String getSchemaName()
  {
    return this.schema;
  }
  
  public String getSQL()
  {
    if (this.table == null) {
      return "*";
    }
    return StringUtils.quoteIdentifier(this.table) + ".*";
  }
  
  public void updateAggregate(Session paramSession)
  {
    DbException.throwInternalError();
  }
  
  public boolean isEverything(ExpressionVisitor paramExpressionVisitor)
  {
    throw DbException.throwInternalError();
  }
  
  public int getCost()
  {
    throw DbException.throwInternalError();
  }
}
