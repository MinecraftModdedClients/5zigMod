package org.h2.expression;

import java.util.Arrays;
import org.h2.engine.Database;
import org.h2.engine.DbSettings;
import org.h2.engine.Session;
import org.h2.engine.SysProperties;
import org.h2.index.IndexCondition;
import org.h2.message.DbException;
import org.h2.table.ColumnResolver;
import org.h2.table.Table;
import org.h2.table.TableFilter;
import org.h2.util.New;
import org.h2.value.Value;
import org.h2.value.ValueBoolean;
import org.h2.value.ValueGeometry;
import org.h2.value.ValueNull;

public class Comparison
  extends Condition
{
  public static final int NULL_SAFE = 16;
  public static final int EQUAL = 0;
  public static final int EQUAL_NULL_SAFE = 16;
  public static final int BIGGER_EQUAL = 1;
  public static final int BIGGER = 2;
  public static final int SMALLER_EQUAL = 3;
  public static final int SMALLER = 4;
  public static final int NOT_EQUAL = 5;
  public static final int NOT_EQUAL_NULL_SAFE = 21;
  public static final int IS_NULL = 6;
  public static final int IS_NOT_NULL = 7;
  public static final int FALSE = 8;
  public static final int IN_LIST = 9;
  public static final int IN_QUERY = 10;
  public static final int SPATIAL_INTERSECTS = 11;
  private final Database database;
  private int compareType;
  private Expression left;
  private Expression right;
  
  public Comparison(Session paramSession, int paramInt, Expression paramExpression1, Expression paramExpression2)
  {
    this.database = paramSession.getDatabase();
    this.left = paramExpression1;
    this.right = paramExpression2;
    this.compareType = paramInt;
  }
  
  public String getSQL()
  {
    String str;
    switch (this.compareType)
    {
    case 6: 
      str = this.left.getSQL() + " IS NULL";
      break;
    case 7: 
      str = this.left.getSQL() + " IS NOT NULL";
      break;
    case 11: 
      str = "INTERSECTS(" + this.left.getSQL() + ", " + this.right.getSQL() + ")";
      break;
    default: 
      str = this.left.getSQL() + " " + getCompareOperator(this.compareType) + " " + this.right.getSQL();
    }
    return "(" + str + ")";
  }
  
  static String getCompareOperator(int paramInt)
  {
    switch (paramInt)
    {
    case 0: 
      return "=";
    case 16: 
      return "IS";
    case 1: 
      return ">=";
    case 2: 
      return ">";
    case 3: 
      return "<=";
    case 4: 
      return "<";
    case 5: 
      return "<>";
    case 21: 
      return "IS NOT";
    case 11: 
      return "&&";
    }
    throw DbException.throwInternalError("compareType=" + paramInt);
  }
  
  public Expression optimize(Session paramSession)
  {
    this.left = this.left.optimize(paramSession);
    if (this.right != null)
    {
      this.right = this.right.optimize(paramSession);
      Object localObject;
      if (((this.right instanceof ExpressionColumn)) && (
        (this.left.isConstant()) || ((this.left instanceof Parameter))))
      {
        localObject = this.left;
        this.left = this.right;
        this.right = ((Expression)localObject);
        this.compareType = getReversedCompareType(this.compareType);
      }
      if ((this.left instanceof ExpressionColumn)) {
        if (this.right.isConstant())
        {
          localObject = this.right.getValue(paramSession);
          if ((localObject == ValueNull.INSTANCE) && 
            ((this.compareType & 0x10) == 0)) {
            return ValueExpression.getNull();
          }
        }
        else if ((this.right instanceof Parameter))
        {
          ((Parameter)this.right).setColumn(((ExpressionColumn)this.left).getColumn());
        }
      }
    }
    if ((this.compareType == 6) || (this.compareType == 7))
    {
      if (this.left.isConstant()) {
        return ValueExpression.get(getValue(paramSession));
      }
    }
    else
    {
      if ((SysProperties.CHECK) && ((this.left == null) || (this.right == null))) {
        DbException.throwInternalError();
      }
      if ((this.left == ValueExpression.getNull()) || (this.right == ValueExpression.getNull())) {
        if ((this.compareType & 0x10) == 0) {
          return ValueExpression.getNull();
        }
      }
      if ((this.left.isConstant()) && (this.right.isConstant())) {
        return ValueExpression.get(getValue(paramSession));
      }
    }
    return this;
  }
  
  public Value getValue(Session paramSession)
  {
    Value localValue1 = this.left.getValue(paramSession);
    if (this.right == null)
    {
      boolean bool1;
      switch (this.compareType)
      {
      case 6: 
        bool1 = localValue1 == ValueNull.INSTANCE;
        break;
      case 7: 
        bool1 = localValue1 != ValueNull.INSTANCE;
        break;
      default: 
        throw DbException.throwInternalError("type=" + this.compareType);
      }
      return ValueBoolean.get(bool1);
    }
    if ((localValue1 == ValueNull.INSTANCE) && 
      ((this.compareType & 0x10) == 0)) {
      return ValueNull.INSTANCE;
    }
    Value localValue2 = this.right.getValue(paramSession);
    if ((localValue2 == ValueNull.INSTANCE) && 
      ((this.compareType & 0x10) == 0)) {
      return ValueNull.INSTANCE;
    }
    int i = Value.getHigherOrder(this.left.getType(), this.right.getType());
    localValue1 = localValue1.convertTo(i);
    localValue2 = localValue2.convertTo(i);
    boolean bool2 = compareNotNull(this.database, localValue1, localValue2, this.compareType);
    return ValueBoolean.get(bool2);
  }
  
  static boolean compareNotNull(Database paramDatabase, Value paramValue1, Value paramValue2, int paramInt)
  {
    boolean bool;
    switch (paramInt)
    {
    case 0: 
    case 16: 
      bool = paramDatabase.areEqual(paramValue1, paramValue2);
      break;
    case 5: 
    case 21: 
      bool = !paramDatabase.areEqual(paramValue1, paramValue2);
      break;
    case 1: 
      bool = paramDatabase.compare(paramValue1, paramValue2) >= 0;
      break;
    case 2: 
      bool = paramDatabase.compare(paramValue1, paramValue2) > 0;
      break;
    case 3: 
      bool = paramDatabase.compare(paramValue1, paramValue2) <= 0;
      break;
    case 4: 
      bool = paramDatabase.compare(paramValue1, paramValue2) < 0;
      break;
    case 11: 
      ValueGeometry localValueGeometry1 = (ValueGeometry)paramValue1.convertTo(22);
      ValueGeometry localValueGeometry2 = (ValueGeometry)paramValue2.convertTo(22);
      bool = localValueGeometry1.intersectsBoundingBox(localValueGeometry2);
      break;
    case 6: 
    case 7: 
    case 8: 
    case 9: 
    case 10: 
    case 12: 
    case 13: 
    case 14: 
    case 15: 
    case 17: 
    case 18: 
    case 19: 
    case 20: 
    default: 
      throw DbException.throwInternalError("type=" + paramInt);
    }
    return bool;
  }
  
  private int getReversedCompareType(int paramInt)
  {
    switch (this.compareType)
    {
    case 0: 
    case 5: 
    case 11: 
    case 16: 
    case 21: 
      return paramInt;
    case 1: 
      return 3;
    case 2: 
      return 4;
    case 3: 
      return 1;
    case 4: 
      return 2;
    }
    throw DbException.throwInternalError("type=" + this.compareType);
  }
  
  public Expression getNotIfPossible(Session paramSession)
  {
    if (this.compareType == 11) {
      return null;
    }
    int i = getNotCompareType();
    return new Comparison(paramSession, i, this.left, this.right);
  }
  
  private int getNotCompareType()
  {
    switch (this.compareType)
    {
    case 0: 
      return 5;
    case 16: 
      return 21;
    case 5: 
      return 0;
    case 21: 
      return 16;
    case 1: 
      return 4;
    case 2: 
      return 3;
    case 3: 
      return 2;
    case 4: 
      return 1;
    case 6: 
      return 7;
    case 7: 
      return 6;
    }
    throw DbException.throwInternalError("type=" + this.compareType);
  }
  
  public void createIndexConditions(Session paramSession, TableFilter paramTableFilter)
  {
    if (!paramTableFilter.getTable().isQueryComparable()) {
      return;
    }
    ExpressionColumn localExpressionColumn1 = null;
    if ((this.left instanceof ExpressionColumn))
    {
      localExpressionColumn1 = (ExpressionColumn)this.left;
      if (paramTableFilter != localExpressionColumn1.getTableFilter()) {
        localExpressionColumn1 = null;
      }
    }
    if (this.right == null)
    {
      if (localExpressionColumn1 != null) {
        switch (this.compareType)
        {
        case 6: 
          if (paramSession.getDatabase().getSettings().optimizeIsNull) {
            paramTableFilter.addIndexCondition(IndexCondition.get(16, localExpressionColumn1, ValueExpression.getNull()));
          }
          break;
        }
      }
      return;
    }
    ExpressionColumn localExpressionColumn2 = null;
    if ((this.right instanceof ExpressionColumn))
    {
      localExpressionColumn2 = (ExpressionColumn)this.right;
      if (paramTableFilter != localExpressionColumn2.getTableFilter()) {
        localExpressionColumn2 = null;
      }
    }
    if ((localExpressionColumn1 == null) && (localExpressionColumn2 == null)) {
      return;
    }
    if ((localExpressionColumn1 != null) && (localExpressionColumn2 != null)) {
      return;
    }
    ExpressionVisitor localExpressionVisitor;
    if (localExpressionColumn1 == null)
    {
      localExpressionVisitor = ExpressionVisitor.getNotFromResolverVisitor(paramTableFilter);
      if (!this.left.isEverything(localExpressionVisitor)) {
        return;
      }
    }
    else if (localExpressionColumn2 == null)
    {
      localExpressionVisitor = ExpressionVisitor.getNotFromResolverVisitor(paramTableFilter);
      if (!this.right.isEverything(localExpressionVisitor)) {
        return;
      }
    }
    else
    {
      return;
    }
    int i;
    switch (this.compareType)
    {
    case 5: 
    case 21: 
      i = 0;
      break;
    case 0: 
    case 1: 
    case 2: 
    case 3: 
    case 4: 
    case 11: 
    case 16: 
      i = 1;
      break;
    case 6: 
    case 7: 
    case 8: 
    case 9: 
    case 10: 
    case 12: 
    case 13: 
    case 14: 
    case 15: 
    case 17: 
    case 18: 
    case 19: 
    case 20: 
    default: 
      throw DbException.throwInternalError("type=" + this.compareType);
    }
    if (i != 0) {
      if (localExpressionColumn1 != null)
      {
        paramTableFilter.addIndexCondition(IndexCondition.get(this.compareType, localExpressionColumn1, this.right));
      }
      else if (localExpressionColumn2 != null)
      {
        int j = getReversedCompareType(this.compareType);
        paramTableFilter.addIndexCondition(IndexCondition.get(j, localExpressionColumn2, this.left));
      }
    }
  }
  
  public void setEvaluatable(TableFilter paramTableFilter, boolean paramBoolean)
  {
    this.left.setEvaluatable(paramTableFilter, paramBoolean);
    if (this.right != null) {
      this.right.setEvaluatable(paramTableFilter, paramBoolean);
    }
  }
  
  public void updateAggregate(Session paramSession)
  {
    this.left.updateAggregate(paramSession);
    if (this.right != null) {
      this.right.updateAggregate(paramSession);
    }
  }
  
  public void addFilterConditions(TableFilter paramTableFilter, boolean paramBoolean)
  {
    if ((this.compareType == 6) && (paramBoolean)) {
      return;
    }
    super.addFilterConditions(paramTableFilter, paramBoolean);
  }
  
  public void mapColumns(ColumnResolver paramColumnResolver, int paramInt)
  {
    this.left.mapColumns(paramColumnResolver, paramInt);
    if (this.right != null) {
      this.right.mapColumns(paramColumnResolver, paramInt);
    }
  }
  
  public boolean isEverything(ExpressionVisitor paramExpressionVisitor)
  {
    return (this.left.isEverything(paramExpressionVisitor)) && ((this.right == null) || (this.right.isEverything(paramExpressionVisitor)));
  }
  
  public int getCost()
  {
    return this.left.getCost() + (this.right == null ? 0 : this.right.getCost()) + 1;
  }
  
  Expression getIfEquals(Expression paramExpression)
  {
    if (this.compareType == 0)
    {
      String str = paramExpression.getSQL();
      if (this.left.getSQL().equals(str)) {
        return this.right;
      }
      if (this.right.getSQL().equals(str)) {
        return this.left;
      }
    }
    return null;
  }
  
  Expression getAdditional(Session paramSession, Comparison paramComparison, boolean paramBoolean)
  {
    if ((this.compareType == paramComparison.compareType) && (this.compareType == 0))
    {
      boolean bool1 = this.left.isConstant();
      boolean bool2 = this.right.isConstant();
      boolean bool3 = paramComparison.left.isConstant();
      boolean bool4 = paramComparison.right.isConstant();
      String str1 = this.left.getSQL();
      String str2 = paramComparison.left.getSQL();
      String str3 = this.right.getSQL();
      String str4 = paramComparison.right.getSQL();
      if (paramBoolean)
      {
        if (((!bool2) || (!bool4)) && (str1.equals(str2))) {
          return new Comparison(paramSession, 0, this.right, paramComparison.right);
        }
        if (((!bool2) || (!bool3)) && (str1.equals(str4))) {
          return new Comparison(paramSession, 0, this.right, paramComparison.left);
        }
        if (((!bool1) || (!bool4)) && (str3.equals(str2))) {
          return new Comparison(paramSession, 0, this.left, paramComparison.right);
        }
        if (((!bool1) || (!bool3)) && (str3.equals(str4))) {
          return new Comparison(paramSession, 0, this.left, paramComparison.left);
        }
      }
      else
      {
        Database localDatabase = paramSession.getDatabase();
        if ((bool2) && (bool4) && (str1.equals(str2))) {
          return new ConditionIn(localDatabase, this.left, New.arrayList(Arrays.asList(new Expression[] { this.right, paramComparison.right })));
        }
        if ((bool2) && (bool3) && (str1.equals(str4))) {
          return new ConditionIn(localDatabase, this.left, New.arrayList(Arrays.asList(new Expression[] { this.right, paramComparison.left })));
        }
        if ((bool1) && (bool4) && (str3.equals(str2))) {
          return new ConditionIn(localDatabase, this.right, New.arrayList(Arrays.asList(new Expression[] { this.left, paramComparison.right })));
        }
        if ((bool1) && (bool3) && (str3.equals(str4))) {
          return new ConditionIn(localDatabase, this.right, New.arrayList(Arrays.asList(new Expression[] { this.left, paramComparison.left })));
        }
      }
    }
    return null;
  }
  
  public Expression getExpression(boolean paramBoolean)
  {
    return paramBoolean ? this.left : this.right;
  }
}
