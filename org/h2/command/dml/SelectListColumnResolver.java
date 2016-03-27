package org.h2.command.dml;

import java.util.ArrayList;
import org.h2.expression.Expression;
import org.h2.expression.ExpressionColumn;
import org.h2.table.Column;
import org.h2.table.ColumnResolver;
import org.h2.table.TableFilter;
import org.h2.value.Value;

public class SelectListColumnResolver
  implements ColumnResolver
{
  private final Select select;
  private final Expression[] expressions;
  private final Column[] columns;
  
  SelectListColumnResolver(Select paramSelect)
  {
    this.select = paramSelect;
    int i = paramSelect.getColumnCount();
    this.columns = new Column[i];
    this.expressions = new Expression[i];
    ArrayList localArrayList = paramSelect.getExpressions();
    for (int j = 0; j < i; j++)
    {
      Expression localExpression = (Expression)localArrayList.get(j);
      Column localColumn = new Column(localExpression.getAlias(), 0);
      localColumn.setTable(null, j);
      this.columns[j] = localColumn;
      this.expressions[j] = localExpression.getNonAliasExpression();
    }
  }
  
  public Column[] getColumns()
  {
    return this.columns;
  }
  
  public String getSchemaName()
  {
    return null;
  }
  
  public Select getSelect()
  {
    return this.select;
  }
  
  public Column[] getSystemColumns()
  {
    return null;
  }
  
  public Column getRowIdColumn()
  {
    return null;
  }
  
  public String getTableAlias()
  {
    return null;
  }
  
  public TableFilter getTableFilter()
  {
    return null;
  }
  
  public Value getValue(Column paramColumn)
  {
    return null;
  }
  
  public Expression optimize(ExpressionColumn paramExpressionColumn, Column paramColumn)
  {
    return this.expressions[paramColumn.getColumnId()];
  }
}
