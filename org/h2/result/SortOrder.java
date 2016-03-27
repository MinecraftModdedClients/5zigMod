package org.h2.result;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import org.h2.command.dml.SelectOrderBy;
import org.h2.engine.Database;
import org.h2.engine.SysProperties;
import org.h2.expression.Expression;
import org.h2.expression.ExpressionColumn;
import org.h2.table.Column;
import org.h2.table.TableFilter;
import org.h2.util.StatementBuilder;
import org.h2.util.StringUtils;
import org.h2.util.Utils;
import org.h2.value.Value;
import org.h2.value.ValueNull;

public class SortOrder
  implements Comparator<Value[]>
{
  public static final int ASCENDING = 0;
  public static final int DESCENDING = 1;
  public static final int NULLS_FIRST = 2;
  public static final int NULLS_LAST = 4;
  private static final int DEFAULT_NULL_SORT = SysProperties.SORT_NULLS_HIGH ? 1 : -1;
  private final Database database;
  private final int[] queryColumnIndexes;
  private final int[] sortTypes;
  private final ArrayList<SelectOrderBy> orderList;
  
  public SortOrder(Database paramDatabase, int[] paramArrayOfInt1, int[] paramArrayOfInt2, ArrayList<SelectOrderBy> paramArrayList)
  {
    this.database = paramDatabase;
    this.queryColumnIndexes = paramArrayOfInt1;
    this.sortTypes = paramArrayOfInt2;
    this.orderList = paramArrayList;
  }
  
  public String getSQL(Expression[] paramArrayOfExpression, int paramInt)
  {
    StatementBuilder localStatementBuilder = new StatementBuilder();
    int i = 0;
    for (int m : this.queryColumnIndexes)
    {
      localStatementBuilder.appendExceptFirst(", ");
      if (m < paramInt) {
        localStatementBuilder.append(m + 1);
      } else {
        localStatementBuilder.append('=').append(StringUtils.unEnclose(paramArrayOfExpression[m].getSQL()));
      }
      int n = this.sortTypes[(i++)];
      if ((n & 0x1) != 0) {
        localStatementBuilder.append(" DESC");
      }
      if ((n & 0x2) != 0) {
        localStatementBuilder.append(" NULLS FIRST");
      } else if ((n & 0x4) != 0) {
        localStatementBuilder.append(" NULLS LAST");
      }
    }
    return localStatementBuilder.toString();
  }
  
  public static int compareNull(boolean paramBoolean, int paramInt)
  {
    if ((paramInt & 0x2) != 0) {
      return paramBoolean ? -1 : 1;
    }
    if ((paramInt & 0x4) != 0) {
      return paramBoolean ? 1 : -1;
    }
    int i = paramBoolean ? DEFAULT_NULL_SORT : -DEFAULT_NULL_SORT;
    return (paramInt & 0x1) == 0 ? i : -i;
  }
  
  public int compare(Value[] paramArrayOfValue1, Value[] paramArrayOfValue2)
  {
    int i = 0;
    for (int j = this.queryColumnIndexes.length; i < j; i++)
    {
      int k = this.queryColumnIndexes[i];
      int m = this.sortTypes[i];
      Value localValue1 = paramArrayOfValue1[k];
      Value localValue2 = paramArrayOfValue2[k];
      boolean bool1 = localValue1 == ValueNull.INSTANCE;boolean bool2 = localValue2 == ValueNull.INSTANCE;
      if ((bool1) || (bool2))
      {
        if (bool1 != bool2) {
          return compareNull(bool1, m);
        }
      }
      else
      {
        int n = this.database.compare(localValue1, localValue2);
        if (n != 0) {
          return (m & 0x1) == 0 ? n : -n;
        }
      }
    }
    return 0;
  }
  
  public void sort(ArrayList<Value[]> paramArrayList)
  {
    Collections.sort(paramArrayList, this);
  }
  
  public void sort(ArrayList<Value[]> paramArrayList, int paramInt1, int paramInt2)
  {
    int i = paramArrayList.size();
    if ((paramArrayList.isEmpty()) || (paramInt1 >= i) || (paramInt2 == 0)) {
      return;
    }
    if (paramInt1 < 0) {
      paramInt1 = 0;
    }
    if (paramInt1 + paramInt2 > i) {
      paramInt2 = i - paramInt1;
    }
    if ((paramInt2 == 1) && (paramInt1 == 0))
    {
      paramArrayList.set(0, Collections.min(paramArrayList, this));
      return;
    }
    Value[][] arrayOfValue = (Value[][])paramArrayList.toArray(new Value[i][]);
    Utils.sortTopN(arrayOfValue, paramInt1, paramInt2, this);
    int j = 0;
    for (int k = Math.min(paramInt1 + paramInt2, i); j < k; j++) {
      paramArrayList.set(j, arrayOfValue[j]);
    }
  }
  
  public int[] getQueryColumnIndexes()
  {
    return this.queryColumnIndexes;
  }
  
  public Column getColumn(int paramInt, TableFilter paramTableFilter)
  {
    if (this.orderList == null) {
      return null;
    }
    SelectOrderBy localSelectOrderBy = (SelectOrderBy)this.orderList.get(paramInt);
    Expression localExpression = localSelectOrderBy.expression;
    if (localExpression == null) {
      return null;
    }
    localExpression = localExpression.getNonAliasExpression();
    if (localExpression.isConstant()) {
      return null;
    }
    if (!(localExpression instanceof ExpressionColumn)) {
      return null;
    }
    ExpressionColumn localExpressionColumn = (ExpressionColumn)localExpression;
    if (localExpressionColumn.getTableFilter() != paramTableFilter) {
      return null;
    }
    return localExpressionColumn.getColumn();
  }
  
  public int[] getSortTypes()
  {
    return this.sortTypes;
  }
}
