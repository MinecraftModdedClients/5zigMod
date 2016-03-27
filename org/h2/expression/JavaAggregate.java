package org.h2.expression;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import org.h2.api.Aggregate;
import org.h2.command.Parser;
import org.h2.command.dml.Select;
import org.h2.engine.Session;
import org.h2.engine.UserAggregate;
import org.h2.message.DbException;
import org.h2.table.ColumnResolver;
import org.h2.table.TableFilter;
import org.h2.util.StatementBuilder;
import org.h2.value.DataType;
import org.h2.value.Value;
import org.h2.value.ValueNull;

public class JavaAggregate
  extends Expression
{
  private final UserAggregate userAggregate;
  private final Select select;
  private final Expression[] args;
  private int[] argTypes;
  private int dataType;
  private Connection userConnection;
  private int lastGroupRowId;
  
  public JavaAggregate(UserAggregate paramUserAggregate, Expression[] paramArrayOfExpression, Select paramSelect)
  {
    this.userAggregate = paramUserAggregate;
    this.args = paramArrayOfExpression;
    this.select = paramSelect;
  }
  
  public int getCost()
  {
    int i = 5;
    for (Expression localExpression : this.args) {
      i += localExpression.getCost();
    }
    return i;
  }
  
  public long getPrecision()
  {
    return 2147483647L;
  }
  
  public int getDisplaySize()
  {
    return Integer.MAX_VALUE;
  }
  
  public int getScale()
  {
    return DataType.getDataType(this.dataType).defaultScale;
  }
  
  public String getSQL()
  {
    StatementBuilder localStatementBuilder = new StatementBuilder();
    localStatementBuilder.append(Parser.quoteIdentifier(this.userAggregate.getName())).append('(');
    for (Expression localExpression : this.args)
    {
      localStatementBuilder.appendExceptFirst(", ");
      localStatementBuilder.append(localExpression.getSQL());
    }
    return localStatementBuilder.append(')').toString();
  }
  
  public int getType()
  {
    return this.dataType;
  }
  
  public boolean isEverything(ExpressionVisitor paramExpressionVisitor)
  {
    switch (paramExpressionVisitor.getType())
    {
    case 1: 
    case 2: 
      return false;
    case 7: 
      paramExpressionVisitor.addDependency(this.userAggregate);
      break;
    }
    for (Expression localExpression : this.args) {
      if ((localExpression != null) && (!localExpression.isEverything(paramExpressionVisitor))) {
        return false;
      }
    }
    return true;
  }
  
  public void mapColumns(ColumnResolver paramColumnResolver, int paramInt)
  {
    for (Expression localExpression : this.args) {
      localExpression.mapColumns(paramColumnResolver, paramInt);
    }
  }
  
  public Expression optimize(Session paramSession)
  {
    this.userConnection = paramSession.createConnection(false);
    int i = this.args.length;
    this.argTypes = new int[i];
    for (int j = 0; j < i; j++)
    {
      Expression localExpression = this.args[j];
      this.args[j] = localExpression.optimize(paramSession);
      int k = localExpression.getType();
      this.argTypes[j] = k;
    }
    try
    {
      Aggregate localAggregate = getInstance();
      this.dataType = localAggregate.getInternalType(this.argTypes);
    }
    catch (SQLException localSQLException)
    {
      throw DbException.convert(localSQLException);
    }
    return this;
  }
  
  public void setEvaluatable(TableFilter paramTableFilter, boolean paramBoolean)
  {
    for (Expression localExpression : this.args) {
      localExpression.setEvaluatable(paramTableFilter, paramBoolean);
    }
  }
  
  private Aggregate getInstance()
    throws SQLException
  {
    Aggregate localAggregate = this.userAggregate.getInstance();
    localAggregate.init(this.userConnection);
    return localAggregate;
  }
  
  public Value getValue(Session paramSession)
  {
    HashMap localHashMap = this.select.getCurrentGroup();
    if (localHashMap == null) {
      throw DbException.get(90054, getSQL());
    }
    try
    {
      Aggregate localAggregate = (Aggregate)localHashMap.get(this);
      if (localAggregate == null) {
        localAggregate = getInstance();
      }
      Object localObject = localAggregate.getResult();
      if (localObject == null) {
        return ValueNull.INSTANCE;
      }
      return DataType.convertToValue(paramSession, localObject, this.dataType);
    }
    catch (SQLException localSQLException)
    {
      throw DbException.convert(localSQLException);
    }
  }
  
  public void updateAggregate(Session paramSession)
  {
    HashMap localHashMap = this.select.getCurrentGroup();
    if (localHashMap == null) {
      return;
    }
    int i = this.select.getCurrentGroupRowId();
    if (this.lastGroupRowId == i) {
      return;
    }
    this.lastGroupRowId = i;
    
    Aggregate localAggregate = (Aggregate)localHashMap.get(this);
    try
    {
      if (localAggregate == null)
      {
        localAggregate = getInstance();
        localHashMap.put(this, localAggregate);
      }
      Object[] arrayOfObject = new Object[this.args.length];
      Object localObject = null;
      int j = 0;
      for (int k = this.args.length; j < k; j++)
      {
        Value localValue = this.args[j].getValue(paramSession);
        localValue = localValue.convertTo(this.argTypes[j]);
        localObject = localValue.getObject();
        arrayOfObject[j] = localObject;
      }
      if (this.args.length == 1) {
        localAggregate.add(localObject);
      } else {
        localAggregate.add(arrayOfObject);
      }
    }
    catch (SQLException localSQLException)
    {
      throw DbException.convert(localSQLException);
    }
  }
}
