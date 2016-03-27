package org.h2.table;

import org.h2.command.dml.Select;
import org.h2.expression.Expression;
import org.h2.expression.ExpressionColumn;
import org.h2.value.Value;

public class SingleColumnResolver
  implements ColumnResolver
{
  private final Column column;
  private Value value;
  
  SingleColumnResolver(Column paramColumn)
  {
    this.column = paramColumn;
  }
  
  public String getTableAlias()
  {
    return null;
  }
  
  void setValue(Value paramValue)
  {
    this.value = paramValue;
  }
  
  public Value getValue(Column paramColumn)
  {
    return this.value;
  }
  
  public Column[] getColumns()
  {
    return new Column[] { this.column };
  }
  
  public String getSchemaName()
  {
    return null;
  }
  
  public TableFilter getTableFilter()
  {
    return null;
  }
  
  public Select getSelect()
  {
    return null;
  }
  
  public Column[] getSystemColumns()
  {
    return null;
  }
  
  public Column getRowIdColumn()
  {
    return null;
  }
  
  public Expression optimize(ExpressionColumn paramExpressionColumn, Column paramColumn)
  {
    return paramExpressionColumn;
  }
}
