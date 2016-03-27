package org.h2.expression;

import org.h2.engine.Database;
import org.h2.engine.Mode;
import org.h2.engine.Session;
import org.h2.message.DbException;
import org.h2.table.ColumnResolver;
import org.h2.table.TableFilter;
import org.h2.util.MathUtils;
import org.h2.value.DataType;
import org.h2.value.Value;
import org.h2.value.ValueInt;
import org.h2.value.ValueNull;
import org.h2.value.ValueString;

public class Operation
  extends Expression
{
  public static final int CONCAT = 0;
  public static final int PLUS = 1;
  public static final int MINUS = 2;
  public static final int MULTIPLY = 3;
  public static final int DIVIDE = 4;
  public static final int NEGATE = 5;
  public static final int MODULUS = 6;
  private int opType;
  private Expression left;
  private Expression right;
  private int dataType;
  private boolean convertRight = true;
  
  public Operation(int paramInt, Expression paramExpression1, Expression paramExpression2)
  {
    this.opType = paramInt;
    this.left = paramExpression1;
    this.right = paramExpression2;
  }
  
  public String getSQL()
  {
    String str;
    if (this.opType == 5) {
      str = "- " + this.left.getSQL();
    } else {
      str = this.left.getSQL() + " " + getOperationToken() + " " + this.right.getSQL();
    }
    return "(" + str + ")";
  }
  
  private String getOperationToken()
  {
    switch (this.opType)
    {
    case 5: 
      return "-";
    case 0: 
      return "||";
    case 1: 
      return "+";
    case 2: 
      return "-";
    case 3: 
      return "*";
    case 4: 
      return "/";
    case 6: 
      return "%";
    }
    throw DbException.throwInternalError("opType=" + this.opType);
  }
  
  public Value getValue(Session paramSession)
  {
    Value localValue1 = this.left.getValue(paramSession).convertTo(this.dataType);
    Value localValue2;
    if (this.right == null)
    {
      localValue2 = null;
    }
    else
    {
      localValue2 = this.right.getValue(paramSession);
      if (this.convertRight) {
        localValue2 = localValue2.convertTo(this.dataType);
      }
    }
    switch (this.opType)
    {
    case 5: 
      return localValue1 == ValueNull.INSTANCE ? localValue1 : localValue1.negate();
    case 0: 
      Mode localMode = paramSession.getDatabase().getMode();
      if (localValue1 == ValueNull.INSTANCE)
      {
        if (localMode.nullConcatIsNull) {
          return ValueNull.INSTANCE;
        }
        return localValue2;
      }
      if (localValue2 == ValueNull.INSTANCE)
      {
        if (localMode.nullConcatIsNull) {
          return ValueNull.INSTANCE;
        }
        return localValue1;
      }
      String str1 = localValue1.getString();String str2 = localValue2.getString();
      StringBuilder localStringBuilder = new StringBuilder(str1.length() + str2.length());
      localStringBuilder.append(str1).append(str2);
      return ValueString.get(localStringBuilder.toString());
    case 1: 
      if ((localValue1 == ValueNull.INSTANCE) || (localValue2 == ValueNull.INSTANCE)) {
        return ValueNull.INSTANCE;
      }
      return localValue1.add(localValue2);
    case 2: 
      if ((localValue1 == ValueNull.INSTANCE) || (localValue2 == ValueNull.INSTANCE)) {
        return ValueNull.INSTANCE;
      }
      return localValue1.subtract(localValue2);
    case 3: 
      if ((localValue1 == ValueNull.INSTANCE) || (localValue2 == ValueNull.INSTANCE)) {
        return ValueNull.INSTANCE;
      }
      return localValue1.multiply(localValue2);
    case 4: 
      if ((localValue1 == ValueNull.INSTANCE) || (localValue2 == ValueNull.INSTANCE)) {
        return ValueNull.INSTANCE;
      }
      return localValue1.divide(localValue2);
    case 6: 
      if ((localValue1 == ValueNull.INSTANCE) || (localValue2 == ValueNull.INSTANCE)) {
        return ValueNull.INSTANCE;
      }
      return localValue1.modulus(localValue2);
    }
    throw DbException.throwInternalError("type=" + this.opType);
  }
  
  public void mapColumns(ColumnResolver paramColumnResolver, int paramInt)
  {
    this.left.mapColumns(paramColumnResolver, paramInt);
    if (this.right != null) {
      this.right.mapColumns(paramColumnResolver, paramInt);
    }
  }
  
  public Expression optimize(Session paramSession)
  {
    this.left = this.left.optimize(paramSession);
    switch (this.opType)
    {
    case 5: 
      this.dataType = this.left.getType();
      if (this.dataType == -1) {
        this.dataType = 6;
      }
      break;
    case 0: 
      this.right = this.right.optimize(paramSession);
      this.dataType = 13;
      if ((this.left.isConstant()) && (this.right.isConstant())) {
        return ValueExpression.get(getValue(paramSession));
      }
      break;
    case 1: 
    case 2: 
    case 3: 
    case 4: 
    case 6: 
      this.right = this.right.optimize(paramSession);
      int i = this.left.getType();
      int j = this.right.getType();
      if (((i == 0) && (j == 0)) || ((i == -1) && (j == -1)))
      {
        if ((this.opType == 1) && (paramSession.getDatabase().getMode().allowPlusForStringConcat))
        {
          this.dataType = 13;
          this.opType = 0;
        }
        else
        {
          this.dataType = 6;
        }
      }
      else
      {
        if ((i == 10) || (i == 11) || (i == 9) || (j == 10) || (j == 11) || (j == 9))
        {
          Function localFunction;
          if (this.opType == 1)
          {
            if (j != Value.getHigherOrder(i, j))
            {
              swap();
              int k = i;
              i = j;
              j = k;
            }
            if (i == 4)
            {
              localFunction = Function.getFunction(paramSession.getDatabase(), "DATEADD");
              localFunction.setParameter(0, ValueExpression.get(ValueString.get("DAY")));
              localFunction.setParameter(1, this.left);
              localFunction.setParameter(2, this.right);
              localFunction.doneWithParameters();
              return localFunction.optimize(paramSession);
            }
            if ((i == 6) || (i == 8) || (i == 7))
            {
              localFunction = Function.getFunction(paramSession.getDatabase(), "DATEADD");
              localFunction.setParameter(0, ValueExpression.get(ValueString.get("SECOND")));
              this.left = new Operation(3, ValueExpression.get(ValueInt.get(86400)), this.left);
              
              localFunction.setParameter(1, this.left);
              localFunction.setParameter(2, this.right);
              localFunction.doneWithParameters();
              return localFunction.optimize(paramSession);
            }
            if ((i == 9) && (j == 9))
            {
              this.dataType = 9;
              return this;
            }
            if (i == 9)
            {
              this.dataType = 11;
              return this;
            }
          }
          else if (this.opType == 2)
          {
            if (((i == 10) || (i == 11)) && (j == 4))
            {
              localFunction = Function.getFunction(paramSession.getDatabase(), "DATEADD");
              localFunction.setParameter(0, ValueExpression.get(ValueString.get("DAY")));
              this.right = new Operation(5, this.right, null);
              this.right = this.right.optimize(paramSession);
              localFunction.setParameter(1, this.right);
              localFunction.setParameter(2, this.left);
              localFunction.doneWithParameters();
              return localFunction.optimize(paramSession);
            }
            if (((i == 10) || (i == 11)) && ((j == 6) || (j == 8) || (j == 7)))
            {
              localFunction = Function.getFunction(paramSession.getDatabase(), "DATEADD");
              localFunction.setParameter(0, ValueExpression.get(ValueString.get("SECOND")));
              this.right = new Operation(3, ValueExpression.get(ValueInt.get(86400)), this.right);
              
              this.right = new Operation(5, this.right, null);
              this.right = this.right.optimize(paramSession);
              localFunction.setParameter(1, this.right);
              localFunction.setParameter(2, this.left);
              localFunction.doneWithParameters();
              return localFunction.optimize(paramSession);
            }
            if ((i == 10) || (i == 11))
            {
              if (j == 9)
              {
                this.dataType = 11;
                return this;
              }
              if ((j == 10) || (j == 11))
              {
                localFunction = Function.getFunction(paramSession.getDatabase(), "DATEDIFF");
                localFunction.setParameter(0, ValueExpression.get(ValueString.get("DAY")));
                localFunction.setParameter(1, this.right);
                localFunction.setParameter(2, this.left);
                localFunction.doneWithParameters();
                return localFunction.optimize(paramSession);
              }
            }
            else if ((i == 9) && (j == 9))
            {
              this.dataType = 9;
              return this;
            }
          }
          else if (this.opType == 3)
          {
            if (i == 9)
            {
              this.dataType = 9;
              this.convertRight = false;
              return this;
            }
            if (j == 9)
            {
              swap();
              this.dataType = 9;
              this.convertRight = false;
              return this;
            }
          }
          else if ((this.opType == 4) && 
            (i == 9))
          {
            this.dataType = 9;
            this.convertRight = false;
            return this;
          }
          throw DbException.getUnsupportedException(DataType.getDataType(i).name + " " + getOperationToken() + " " + DataType.getDataType(j).name);
        }
        this.dataType = Value.getHigherOrder(i, j);
        if ((DataType.isStringType(this.dataType)) && (paramSession.getDatabase().getMode().allowPlusForStringConcat)) {
          this.opType = 0;
        }
      }
      break;
    default: 
      DbException.throwInternalError("type=" + this.opType);
    }
    if ((this.left.isConstant()) && ((this.right == null) || (this.right.isConstant()))) {
      return ValueExpression.get(getValue(paramSession));
    }
    return this;
  }
  
  private void swap()
  {
    Expression localExpression = this.left;
    this.left = this.right;
    this.right = localExpression;
  }
  
  public void setEvaluatable(TableFilter paramTableFilter, boolean paramBoolean)
  {
    this.left.setEvaluatable(paramTableFilter, paramBoolean);
    if (this.right != null) {
      this.right.setEvaluatable(paramTableFilter, paramBoolean);
    }
  }
  
  public int getType()
  {
    return this.dataType;
  }
  
  public long getPrecision()
  {
    if (this.right != null)
    {
      switch (this.opType)
      {
      case 0: 
        return this.left.getPrecision() + this.right.getPrecision();
      }
      return Math.max(this.left.getPrecision(), this.right.getPrecision());
    }
    return this.left.getPrecision();
  }
  
  public int getDisplaySize()
  {
    if (this.right != null)
    {
      switch (this.opType)
      {
      case 0: 
        return MathUtils.convertLongToInt(this.left.getDisplaySize() + this.right.getDisplaySize());
      }
      return Math.max(this.left.getDisplaySize(), this.right.getDisplaySize());
    }
    return this.left.getDisplaySize();
  }
  
  public int getScale()
  {
    if (this.right != null) {
      return Math.max(this.left.getScale(), this.right.getScale());
    }
    return this.left.getScale();
  }
  
  public void updateAggregate(Session paramSession)
  {
    this.left.updateAggregate(paramSession);
    if (this.right != null) {
      this.right.updateAggregate(paramSession);
    }
  }
  
  public boolean isEverything(ExpressionVisitor paramExpressionVisitor)
  {
    return (this.left.isEverything(paramExpressionVisitor)) && ((this.right == null) || (this.right.isEverything(paramExpressionVisitor)));
  }
  
  public int getCost()
  {
    return this.left.getCost() + 1 + (this.right == null ? 0 : this.right.getCost());
  }
}
