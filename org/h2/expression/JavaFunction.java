package org.h2.expression;

import org.h2.command.Parser;
import org.h2.engine.Database;
import org.h2.engine.DbSettings;
import org.h2.engine.FunctionAlias;
import org.h2.engine.FunctionAlias.JavaMethod;
import org.h2.engine.Session;
import org.h2.schema.Schema;
import org.h2.table.ColumnResolver;
import org.h2.table.TableFilter;
import org.h2.util.StatementBuilder;
import org.h2.value.DataType;
import org.h2.value.Value;
import org.h2.value.ValueArray;
import org.h2.value.ValueNull;
import org.h2.value.ValueResultSet;

public class JavaFunction
  extends Expression
  implements FunctionCall
{
  private final FunctionAlias functionAlias;
  private final FunctionAlias.JavaMethod javaMethod;
  private final Expression[] args;
  
  public JavaFunction(FunctionAlias paramFunctionAlias, Expression[] paramArrayOfExpression)
  {
    this.functionAlias = paramFunctionAlias;
    this.javaMethod = paramFunctionAlias.findJavaMethod(paramArrayOfExpression);
    this.args = paramArrayOfExpression;
  }
  
  public Value getValue(Session paramSession)
  {
    return this.javaMethod.getValue(paramSession, this.args, false);
  }
  
  public int getType()
  {
    return this.javaMethod.getDataType();
  }
  
  public void mapColumns(ColumnResolver paramColumnResolver, int paramInt)
  {
    for (Expression localExpression : this.args) {
      localExpression.mapColumns(paramColumnResolver, paramInt);
    }
  }
  
  public Expression optimize(Session paramSession)
  {
    boolean bool = isDeterministic();
    int i = 0;
    for (int j = this.args.length; i < j; i++)
    {
      Expression localExpression = this.args[i].optimize(paramSession);
      this.args[i] = localExpression;
      bool &= localExpression.isConstant();
    }
    if (bool) {
      return ValueExpression.get(getValue(paramSession));
    }
    return this;
  }
  
  public void setEvaluatable(TableFilter paramTableFilter, boolean paramBoolean)
  {
    for (Expression localExpression : this.args) {
      if (localExpression != null) {
        localExpression.setEvaluatable(paramTableFilter, paramBoolean);
      }
    }
  }
  
  public int getScale()
  {
    return DataType.getDataType(getType()).defaultScale;
  }
  
  public long getPrecision()
  {
    return 2147483647L;
  }
  
  public int getDisplaySize()
  {
    return Integer.MAX_VALUE;
  }
  
  public String getSQL()
  {
    StatementBuilder localStatementBuilder = new StatementBuilder();
    if ((this.functionAlias.getDatabase().getSettings().functionsInSchema) || (!this.functionAlias.getSchema().getName().equals("PUBLIC"))) {
      localStatementBuilder.append(Parser.quoteIdentifier(this.functionAlias.getSchema().getName())).append('.');
    }
    localStatementBuilder.append(Parser.quoteIdentifier(this.functionAlias.getName())).append('(');
    for (Expression localExpression : this.args)
    {
      localStatementBuilder.appendExceptFirst(", ");
      localStatementBuilder.append(localExpression.getSQL());
    }
    return localStatementBuilder.append(')').toString();
  }
  
  public void updateAggregate(Session paramSession)
  {
    for (Expression localExpression : this.args) {
      if (localExpression != null) {
        localExpression.updateAggregate(paramSession);
      }
    }
  }
  
  public String getName()
  {
    return this.functionAlias.getName();
  }
  
  public ValueResultSet getValueForColumnList(Session paramSession, Expression[] paramArrayOfExpression)
  {
    Value localValue = this.javaMethod.getValue(paramSession, paramArrayOfExpression, true);
    return localValue == ValueNull.INSTANCE ? null : (ValueResultSet)localValue;
  }
  
  public Expression[] getArgs()
  {
    return this.args;
  }
  
  public boolean isEverything(ExpressionVisitor paramExpressionVisitor)
  {
    switch (paramExpressionVisitor.getType())
    {
    case 2: 
      if (!isDeterministic()) {
        return false;
      }
      break;
    case 7: 
      paramExpressionVisitor.addDependency(this.functionAlias);
      break;
    }
    for (Expression localExpression : this.args) {
      if ((localExpression != null) && (!localExpression.isEverything(paramExpressionVisitor))) {
        return false;
      }
    }
    return true;
  }
  
  public int getCost()
  {
    int i = this.javaMethod.hasConnectionParam() ? 25 : 5;
    for (Expression localExpression : this.args) {
      i += localExpression.getCost();
    }
    return i;
  }
  
  public boolean isDeterministic()
  {
    return this.functionAlias.isDeterministic();
  }
  
  public Expression[] getExpressionColumns(Session paramSession)
  {
    switch (getType())
    {
    case 18: 
      ValueResultSet localValueResultSet = getValueForColumnList(paramSession, getArgs());
      return getExpressionColumns(paramSession, localValueResultSet.getResultSet());
    case 17: 
      return getExpressionColumns(paramSession, (ValueArray)getValue(paramSession));
    }
    return super.getExpressionColumns(paramSession);
  }
  
  public boolean isBufferResultSetToLocalTemp()
  {
    return this.functionAlias.isBufferResultSetToLocalTemp();
  }
}
