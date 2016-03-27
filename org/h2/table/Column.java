package org.h2.table;

import java.util.HashSet;
import org.h2.command.Parser;
import org.h2.engine.Database;
import org.h2.engine.Mode;
import org.h2.engine.Session;
import org.h2.expression.ConditionAndOr;
import org.h2.expression.Expression;
import org.h2.expression.ExpressionVisitor;
import org.h2.expression.SequenceValue;
import org.h2.expression.ValueExpression;
import org.h2.message.DbException;
import org.h2.result.Row;
import org.h2.schema.Schema;
import org.h2.schema.SchemaObject;
import org.h2.schema.Sequence;
import org.h2.util.MathUtils;
import org.h2.util.StringUtils;
import org.h2.value.DataType;
import org.h2.value.Value;
import org.h2.value.ValueDate;
import org.h2.value.ValueInt;
import org.h2.value.ValueLong;
import org.h2.value.ValueNull;
import org.h2.value.ValueString;
import org.h2.value.ValueTime;
import org.h2.value.ValueTimestamp;
import org.h2.value.ValueUuid;

public class Column
{
  public static final String ROWID = "_ROWID_";
  public static final int NOT_NULLABLE = 0;
  public static final int NULLABLE = 1;
  public static final int NULLABLE_UNKNOWN = 2;
  private final int type;
  private long precision;
  private int scale;
  private int displaySize;
  private Table table;
  private String name;
  private int columnId;
  private boolean nullable = true;
  private Expression defaultExpression;
  private Expression checkConstraint;
  private String checkConstraintSQL;
  private String originalSQL;
  private boolean autoIncrement;
  private long start;
  private long increment;
  private boolean convertNullToDefault;
  private Sequence sequence;
  private boolean isComputed;
  private TableFilter computeTableFilter;
  private int selectivity;
  private SingleColumnResolver resolver;
  private String comment;
  private boolean primaryKey;
  
  public Column(String paramString, int paramInt)
  {
    this(paramString, paramInt, -1L, -1, -1);
  }
  
  public Column(String paramString, int paramInt1, long paramLong, int paramInt2, int paramInt3)
  {
    this.name = paramString;
    this.type = paramInt1;
    if ((paramLong == -1L) && (paramInt2 == -1) && (paramInt3 == -1))
    {
      DataType localDataType = DataType.getDataType(paramInt1);
      paramLong = localDataType.defaultPrecision;
      paramInt2 = localDataType.defaultScale;
      paramInt3 = localDataType.defaultDisplaySize;
    }
    this.precision = paramLong;
    this.scale = paramInt2;
    this.displaySize = paramInt3;
  }
  
  public boolean equals(Object paramObject)
  {
    if (paramObject == this) {
      return true;
    }
    if (!(paramObject instanceof Column)) {
      return false;
    }
    Column localColumn = (Column)paramObject;
    if ((this.table == null) || (localColumn.table == null) || (this.name == null) || (localColumn.name == null)) {
      return false;
    }
    if (this.table != localColumn.table) {
      return false;
    }
    return this.name.equals(localColumn.name);
  }
  
  public int hashCode()
  {
    if ((this.table == null) || (this.name == null)) {
      return 0;
    }
    return this.table.getId() ^ this.name.hashCode();
  }
  
  public Column getClone()
  {
    Column localColumn = new Column(this.name, this.type, this.precision, this.scale, this.displaySize);
    localColumn.copy(this);
    return localColumn;
  }
  
  public Value convert(Value paramValue)
  {
    try
    {
      return paramValue.convertTo(this.type);
    }
    catch (DbException localDbException)
    {
      if (localDbException.getErrorCode() == 22018)
      {
        String str = (this.table == null ? "" : new StringBuilder().append(this.table.getName()).append(": ").toString()) + getCreateSQL();
        
        throw DbException.get(22018, paramValue.getSQL() + " (" + str + ")");
      }
      throw localDbException;
    }
  }
  
  boolean getComputed()
  {
    return this.isComputed;
  }
  
  synchronized Value computeValue(Session paramSession, Row paramRow)
  {
    this.computeTableFilter.setSession(paramSession);
    this.computeTableFilter.set(paramRow);
    return this.defaultExpression.getValue(paramSession);
  }
  
  public void setComputedExpression(Expression paramExpression)
  {
    this.isComputed = true;
    this.defaultExpression = paramExpression;
  }
  
  public void setTable(Table paramTable, int paramInt)
  {
    this.table = paramTable;
    this.columnId = paramInt;
  }
  
  public Table getTable()
  {
    return this.table;
  }
  
  public void setDefaultExpression(Session paramSession, Expression paramExpression)
  {
    if (paramExpression != null)
    {
      paramExpression = paramExpression.optimize(paramSession);
      if (paramExpression.isConstant()) {
        paramExpression = ValueExpression.get(paramExpression.getValue(paramSession));
      }
    }
    this.defaultExpression = paramExpression;
  }
  
  public int getColumnId()
  {
    return this.columnId;
  }
  
  public String getSQL()
  {
    return Parser.quoteIdentifier(this.name);
  }
  
  public String getName()
  {
    return this.name;
  }
  
  public int getType()
  {
    return this.type;
  }
  
  public long getPrecision()
  {
    return this.precision;
  }
  
  public void setPrecision(long paramLong)
  {
    this.precision = paramLong;
  }
  
  public int getDisplaySize()
  {
    return this.displaySize;
  }
  
  public int getScale()
  {
    return this.scale;
  }
  
  public void setNullable(boolean paramBoolean)
  {
    this.nullable = paramBoolean;
  }
  
  public Value validateConvertUpdateSequence(Session paramSession, Value paramValue)
  {
    if (paramValue == null) {
      if (this.defaultExpression == null)
      {
        paramValue = ValueNull.INSTANCE;
      }
      else
      {
        synchronized (this)
        {
          paramValue = this.defaultExpression.getValue(paramSession).convertTo(this.type);
        }
        if (this.primaryKey) {
          paramSession.setLastIdentity(paramValue);
        }
      }
    }
    ??? = paramSession.getDatabase().getMode();
    if (paramValue == ValueNull.INSTANCE)
    {
      if (this.convertNullToDefault) {
        synchronized (this)
        {
          paramValue = this.defaultExpression.getValue(paramSession).convertTo(this.type);
        }
      }
      if ((paramValue == ValueNull.INSTANCE) && (!this.nullable)) {
        if (((Mode)???).convertInsertNullToZero)
        {
          ??? = DataType.getDataType(this.type);
          if (((DataType)???).decimal) {
            paramValue = ValueInt.get(0).convertTo(this.type);
          } else if (((DataType)???).type == 11) {
            paramValue = ValueTimestamp.fromMillis(paramSession.getTransactionStart());
          } else if (((DataType)???).type == 9) {
            paramValue = ValueTime.fromNanos(0L);
          } else if (((DataType)???).type == 10) {
            paramValue = ValueDate.fromMillis(paramSession.getTransactionStart());
          } else {
            paramValue = ValueString.get("").convertTo(this.type);
          }
        }
        else
        {
          throw DbException.get(23502, this.name);
        }
      }
    }
    if (this.checkConstraint != null)
    {
      this.resolver.setValue(paramValue);
      synchronized (this)
      {
        ??? = this.checkConstraint.getValue(paramSession);
      }
      if (Boolean.FALSE.equals(((Value)???).getBoolean())) {
        throw DbException.get(23513, this.checkConstraint.getSQL());
      }
    }
    paramValue = paramValue.convertScale(((Mode)???).convertOnlyToSmallerScale, this.scale);
    if ((this.precision > 0L) && 
      (!paramValue.checkPrecision(this.precision)))
    {
      ??? = paramValue.getTraceSQL();
      if (((String)???).length() > 127) {
        ??? = ((String)???).substring(0, 128) + "...";
      }
      throw DbException.get(22001, new String[] { getCreateSQL(), (String)??? + " (" + paramValue.getPrecision() + ")" });
    }
    updateSequenceIfRequired(paramSession, paramValue);
    return paramValue;
  }
  
  private void updateSequenceIfRequired(Session paramSession, Value paramValue)
  {
    if (this.sequence != null)
    {
      long l1 = this.sequence.getCurrentValue();
      long l2 = this.sequence.getIncrement();
      long l3 = paramValue.getLong();
      int i = 0;
      if ((l2 > 0L) && (l3 > l1)) {
        i = 1;
      } else if ((l2 < 0L) && (l3 < l1)) {
        i = 1;
      }
      if (i != 0)
      {
        this.sequence.modify(Long.valueOf(l3 + l2), null, null, null);
        paramSession.setLastIdentity(ValueLong.get(l3));
        this.sequence.flush(paramSession);
      }
    }
  }
  
  public void convertAutoIncrementToSequence(Session paramSession, Schema paramSchema, int paramInt, boolean paramBoolean)
  {
    if (!this.autoIncrement) {
      DbException.throwInternalError();
    }
    if ("IDENTITY".equals(this.originalSQL)) {
      this.originalSQL = "BIGINT";
    } else if ("SERIAL".equals(this.originalSQL)) {
      this.originalSQL = "INT";
    }
    String str;
    for (;;)
    {
      localObject1 = ValueUuid.getNewRandom();
      localObject2 = ((ValueUuid)localObject1).getString();
      localObject2 = ((String)localObject2).replace('-', '_').toUpperCase();
      str = "SYSTEM_SEQUENCE_" + (String)localObject2;
      if (paramSchema.findSequence(str) == null) {
        break;
      }
    }
    Object localObject1 = new Sequence(paramSchema, paramInt, str, this.start, this.increment);
    if (paramBoolean) {
      ((Sequence)localObject1).setTemporary(true);
    } else {
      paramSession.getDatabase().addSchemaObject(paramSession, (SchemaObject)localObject1);
    }
    setAutoIncrement(false, 0L, 0L);
    Object localObject2 = new SequenceValue((Sequence)localObject1);
    setDefaultExpression(paramSession, (Expression)localObject2);
    setSequence((Sequence)localObject1);
  }
  
  public void prepareExpression(Session paramSession)
  {
    if (this.defaultExpression != null)
    {
      this.computeTableFilter = new TableFilter(paramSession, this.table, null, false, null);
      this.defaultExpression.mapColumns(this.computeTableFilter, 0);
      this.defaultExpression = this.defaultExpression.optimize(paramSession);
    }
  }
  
  public String getCreateSQL()
  {
    StringBuilder localStringBuilder = new StringBuilder();
    if (this.name != null) {
      localStringBuilder.append(Parser.quoteIdentifier(this.name)).append(' ');
    }
    if (this.originalSQL != null)
    {
      localStringBuilder.append(this.originalSQL);
    }
    else
    {
      localStringBuilder.append(DataType.getDataType(this.type).name);
      switch (this.type)
      {
      case 6: 
        localStringBuilder.append('(').append(this.precision).append(", ").append(this.scale).append(')');
        break;
      case 12: 
      case 13: 
      case 14: 
      case 21: 
        if (this.precision < 2147483647L) {
          localStringBuilder.append('(').append(this.precision).append(')');
        }
        break;
      }
    }
    if (this.defaultExpression != null)
    {
      String str = this.defaultExpression.getSQL();
      if (str != null) {
        if (this.isComputed) {
          localStringBuilder.append(" AS ").append(str);
        } else if (this.defaultExpression != null) {
          localStringBuilder.append(" DEFAULT ").append(str);
        }
      }
    }
    if (!this.nullable) {
      localStringBuilder.append(" NOT NULL");
    }
    if (this.convertNullToDefault) {
      localStringBuilder.append(" NULL_TO_DEFAULT");
    }
    if (this.sequence != null) {
      localStringBuilder.append(" SEQUENCE ").append(this.sequence.getSQL());
    }
    if (this.selectivity != 0) {
      localStringBuilder.append(" SELECTIVITY ").append(this.selectivity);
    }
    if (this.comment != null) {
      localStringBuilder.append(" COMMENT ").append(StringUtils.quoteStringSQL(this.comment));
    }
    if (this.checkConstraint != null) {
      localStringBuilder.append(" CHECK ").append(this.checkConstraintSQL);
    }
    return localStringBuilder.toString();
  }
  
  public boolean isNullable()
  {
    return this.nullable;
  }
  
  public void setOriginalSQL(String paramString)
  {
    this.originalSQL = paramString;
  }
  
  public String getOriginalSQL()
  {
    return this.originalSQL;
  }
  
  public Expression getDefaultExpression()
  {
    return this.defaultExpression;
  }
  
  public boolean isAutoIncrement()
  {
    return this.autoIncrement;
  }
  
  public void setAutoIncrement(boolean paramBoolean, long paramLong1, long paramLong2)
  {
    this.autoIncrement = paramBoolean;
    this.start = paramLong1;
    this.increment = paramLong2;
    this.nullable = false;
    if (paramBoolean) {
      this.convertNullToDefault = true;
    }
  }
  
  public void setConvertNullToDefault(boolean paramBoolean)
  {
    this.convertNullToDefault = paramBoolean;
  }
  
  public void rename(String paramString)
  {
    this.name = paramString;
  }
  
  public void setSequence(Sequence paramSequence)
  {
    this.sequence = paramSequence;
  }
  
  public Sequence getSequence()
  {
    return this.sequence;
  }
  
  public int getSelectivity()
  {
    return this.selectivity == 0 ? 50 : this.selectivity;
  }
  
  public void setSelectivity(int paramInt)
  {
    paramInt = paramInt > 100 ? 100 : paramInt < 0 ? 0 : paramInt;
    this.selectivity = paramInt;
  }
  
  public void addCheckConstraint(Session paramSession, Expression paramExpression)
  {
    if (paramExpression == null) {
      return;
    }
    this.resolver = new SingleColumnResolver(this);
    synchronized (this)
    {
      String str = this.name;
      if (this.name == null) {
        this.name = "VALUE";
      }
      paramExpression.mapColumns(this.resolver, 0);
      this.name = str;
    }
    paramExpression = paramExpression.optimize(paramSession);
    this.resolver.setValue(ValueNull.INSTANCE);
    synchronized (this)
    {
      paramExpression.getValue(paramSession);
    }
    if (this.checkConstraint == null) {
      this.checkConstraint = paramExpression;
    } else {
      this.checkConstraint = new ConditionAndOr(0, this.checkConstraint, paramExpression);
    }
    this.checkConstraintSQL = getCheckConstraintSQL(paramSession, this.name);
  }
  
  public void removeCheckConstraint()
  {
    this.checkConstraint = null;
    this.checkConstraintSQL = null;
  }
  
  public Expression getCheckConstraint(Session paramSession, String paramString)
  {
    if (this.checkConstraint == null) {
      return null;
    }
    Parser localParser = new Parser(paramSession);
    String str1;
    synchronized (this)
    {
      String str2 = this.name;
      this.name = paramString;
      str1 = this.checkConstraint.getSQL();
      this.name = str2;
    }
    ??? = localParser.parseExpression(str1);
    return (Expression)???;
  }
  
  String getDefaultSQL()
  {
    return this.defaultExpression == null ? null : this.defaultExpression.getSQL();
  }
  
  int getPrecisionAsInt()
  {
    return MathUtils.convertLongToInt(this.precision);
  }
  
  DataType getDataType()
  {
    return DataType.getDataType(this.type);
  }
  
  String getCheckConstraintSQL(Session paramSession, String paramString)
  {
    Expression localExpression = getCheckConstraint(paramSession, paramString);
    return localExpression == null ? "" : localExpression.getSQL();
  }
  
  public void setComment(String paramString)
  {
    this.comment = paramString;
  }
  
  public String getComment()
  {
    return this.comment;
  }
  
  public void setPrimaryKey(boolean paramBoolean)
  {
    this.primaryKey = paramBoolean;
  }
  
  boolean isEverything(ExpressionVisitor paramExpressionVisitor)
  {
    if ((paramExpressionVisitor.getType() == 7) && 
      (this.sequence != null)) {
      paramExpressionVisitor.getDependencies().add(this.sequence);
    }
    if ((this.defaultExpression != null) && (!this.defaultExpression.isEverything(paramExpressionVisitor))) {
      return false;
    }
    if ((this.checkConstraint != null) && (!this.checkConstraint.isEverything(paramExpressionVisitor))) {
      return false;
    }
    return true;
  }
  
  public boolean isPrimaryKey()
  {
    return this.primaryKey;
  }
  
  public String toString()
  {
    return this.name;
  }
  
  public boolean isWideningConversion(Column paramColumn)
  {
    if (this.type != paramColumn.type) {
      return false;
    }
    if (this.precision > paramColumn.precision) {
      return false;
    }
    if (this.scale != paramColumn.scale) {
      return false;
    }
    if ((this.nullable) && (!paramColumn.nullable)) {
      return false;
    }
    if (this.convertNullToDefault != paramColumn.convertNullToDefault) {
      return false;
    }
    if (this.primaryKey != paramColumn.primaryKey) {
      return false;
    }
    if ((this.autoIncrement) || (paramColumn.autoIncrement)) {
      return false;
    }
    if ((this.checkConstraint != null) || (paramColumn.checkConstraint != null)) {
      return false;
    }
    if ((this.convertNullToDefault) || (paramColumn.convertNullToDefault)) {
      return false;
    }
    if ((this.defaultExpression != null) || (paramColumn.defaultExpression != null)) {
      return false;
    }
    if ((this.isComputed) || (paramColumn.isComputed)) {
      return false;
    }
    return true;
  }
  
  public void copy(Column paramColumn)
  {
    this.checkConstraint = paramColumn.checkConstraint;
    this.checkConstraintSQL = paramColumn.checkConstraintSQL;
    this.displaySize = paramColumn.displaySize;
    this.name = paramColumn.name;
    this.precision = paramColumn.precision;
    this.scale = paramColumn.scale;
    
    this.nullable = paramColumn.nullable;
    this.defaultExpression = paramColumn.defaultExpression;
    this.originalSQL = paramColumn.originalSQL;
    
    this.convertNullToDefault = paramColumn.convertNullToDefault;
    this.sequence = paramColumn.sequence;
    this.comment = paramColumn.comment;
    this.computeTableFilter = paramColumn.computeTableFilter;
    this.isComputed = paramColumn.isComputed;
    this.selectivity = paramColumn.selectivity;
    this.primaryKey = paramColumn.primaryKey;
  }
}
