package org.h2.command.dml;

import java.sql.ResultSet;
import org.h2.command.Prepared;
import org.h2.engine.Session;
import org.h2.expression.Expression;
import org.h2.expression.ExpressionVisitor;
import org.h2.result.LocalResult;
import org.h2.result.ResultInterface;
import org.h2.value.Value;

public class Call
  extends Prepared
{
  private boolean isResultSet;
  private Expression expression;
  private Expression[] expressions;
  
  public Call(Session paramSession)
  {
    super(paramSession);
  }
  
  public ResultInterface queryMeta()
  {
    LocalResult localLocalResult;
    if (this.isResultSet)
    {
      Expression[] arrayOfExpression = this.expression.getExpressionColumns(this.session);
      localLocalResult = new LocalResult(this.session, arrayOfExpression, arrayOfExpression.length);
    }
    else
    {
      localLocalResult = new LocalResult(this.session, this.expressions, 1);
    }
    localLocalResult.done();
    return localLocalResult;
  }
  
  public int update()
  {
    Value localValue = this.expression.getValue(this.session);
    int i = localValue.getType();
    switch (i)
    {
    case 18: 
      return super.update();
    case -1: 
    case 0: 
      return 0;
    }
    return localValue.getInt();
  }
  
  public ResultInterface query(int paramInt)
  {
    setCurrentRowNumber(1);
    Value localValue = this.expression.getValue(this.session);
    if (this.isResultSet)
    {
      localValue = localValue.convertTo(18);
      localObject = localValue.getResultSet();
      return LocalResult.read(this.session, (ResultSet)localObject, paramInt);
    }
    Object localObject = new LocalResult(this.session, this.expressions, 1);
    Value[] arrayOfValue = { localValue };
    ((LocalResult)localObject).addRow(arrayOfValue);
    ((LocalResult)localObject).done();
    return (ResultInterface)localObject;
  }
  
  public void prepare()
  {
    this.expression = this.expression.optimize(this.session);
    this.expressions = new Expression[] { this.expression };
    this.isResultSet = (this.expression.getType() == 18);
    if (this.isResultSet) {
      this.prepareAlways = true;
    }
  }
  
  public void setExpression(Expression paramExpression)
  {
    this.expression = paramExpression;
  }
  
  public boolean isQuery()
  {
    return true;
  }
  
  public boolean isTransactional()
  {
    return true;
  }
  
  public boolean isReadOnly()
  {
    return this.expression.isEverything(ExpressionVisitor.READONLY_VISITOR);
  }
  
  public int getType()
  {
    return 57;
  }
  
  public boolean isCacheable()
  {
    return !this.isResultSet;
  }
}
