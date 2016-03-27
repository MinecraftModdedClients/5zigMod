package org.h2.command.dml;

import org.h2.expression.Expression;

public class SelectOrderBy
{
  public Expression expression;
  public Expression columnIndexExpr;
  public boolean descending;
  public boolean nullsFirst;
  public boolean nullsLast;
  
  public String getSQL()
  {
    StringBuilder localStringBuilder = new StringBuilder();
    if (this.expression != null) {
      localStringBuilder.append('=').append(this.expression.getSQL());
    } else {
      localStringBuilder.append(this.columnIndexExpr.getSQL());
    }
    if (this.descending) {
      localStringBuilder.append(" DESC");
    }
    if (this.nullsFirst) {
      localStringBuilder.append(" NULLS FIRST");
    } else if (this.nullsLast) {
      localStringBuilder.append(" NULLS LAST");
    }
    return localStringBuilder.toString();
  }
}
