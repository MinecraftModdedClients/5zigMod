package org.h2.expression;

import java.util.HashSet;
import org.h2.engine.DbObject;
import org.h2.table.Column;
import org.h2.table.ColumnResolver;
import org.h2.table.Table;

public class ExpressionVisitor
{
  public static final int INDEPENDENT = 0;
  public static final ExpressionVisitor INDEPENDENT_VISITOR = new ExpressionVisitor(0);
  public static final int OPTIMIZABLE_MIN_MAX_COUNT_ALL = 1;
  public static final int DETERMINISTIC = 2;
  public static final ExpressionVisitor DETERMINISTIC_VISITOR = new ExpressionVisitor(2);
  public static final int EVALUATABLE = 3;
  public static final ExpressionVisitor EVALUATABLE_VISITOR = new ExpressionVisitor(3);
  public static final int SET_MAX_DATA_MODIFICATION_ID = 4;
  public static final int READONLY = 5;
  public static final ExpressionVisitor READONLY_VISITOR = new ExpressionVisitor(5);
  public static final int NOT_FROM_RESOLVER = 6;
  public static final int GET_DEPENDENCIES = 7;
  public static final int QUERY_COMPARABLE = 8;
  public static final int GET_COLUMNS = 9;
  public static final ExpressionVisitor QUERY_COMPARABLE_VISITOR = new ExpressionVisitor(8);
  private final int type;
  private final int queryLevel;
  private final HashSet<DbObject> dependencies;
  private final HashSet<Column> columns;
  private final Table table;
  private final long[] maxDataModificationId;
  private final ColumnResolver resolver;
  
  private ExpressionVisitor(int paramInt1, int paramInt2, HashSet<DbObject> paramHashSet, HashSet<Column> paramHashSet1, Table paramTable, ColumnResolver paramColumnResolver, long[] paramArrayOfLong)
  {
    this.type = paramInt1;
    this.queryLevel = paramInt2;
    this.dependencies = paramHashSet;
    this.columns = paramHashSet1;
    this.table = paramTable;
    this.resolver = paramColumnResolver;
    this.maxDataModificationId = paramArrayOfLong;
  }
  
  private ExpressionVisitor(int paramInt)
  {
    this.type = paramInt;
    this.queryLevel = 0;
    this.dependencies = null;
    this.columns = null;
    this.table = null;
    this.resolver = null;
    this.maxDataModificationId = null;
  }
  
  public static ExpressionVisitor getDependenciesVisitor(HashSet<DbObject> paramHashSet)
  {
    return new ExpressionVisitor(7, 0, paramHashSet, null, null, null, null);
  }
  
  public static ExpressionVisitor getOptimizableVisitor(Table paramTable)
  {
    return new ExpressionVisitor(1, 0, null, null, paramTable, null, null);
  }
  
  static ExpressionVisitor getNotFromResolverVisitor(ColumnResolver paramColumnResolver)
  {
    return new ExpressionVisitor(6, 0, null, null, null, paramColumnResolver, null);
  }
  
  public static ExpressionVisitor getColumnsVisitor(HashSet<Column> paramHashSet)
  {
    return new ExpressionVisitor(9, 0, null, paramHashSet, null, null, null);
  }
  
  public static ExpressionVisitor getMaxModificationIdVisitor()
  {
    return new ExpressionVisitor(4, 0, null, null, null, null, new long[1]);
  }
  
  public void addDependency(DbObject paramDbObject)
  {
    this.dependencies.add(paramDbObject);
  }
  
  void addColumn(Column paramColumn)
  {
    this.columns.add(paramColumn);
  }
  
  public HashSet<DbObject> getDependencies()
  {
    return this.dependencies;
  }
  
  public ExpressionVisitor incrementQueryLevel(int paramInt)
  {
    return new ExpressionVisitor(this.type, this.queryLevel + paramInt, this.dependencies, this.columns, this.table, this.resolver, this.maxDataModificationId);
  }
  
  public ColumnResolver getResolver()
  {
    return this.resolver;
  }
  
  public void addDataModificationId(long paramLong)
  {
    long l = this.maxDataModificationId[0];
    if (paramLong > l) {
      this.maxDataModificationId[0] = paramLong;
    }
  }
  
  public long getMaxDataModificationId()
  {
    return this.maxDataModificationId[0];
  }
  
  int getQueryLevel()
  {
    return this.queryLevel;
  }
  
  public Table getTable()
  {
    return this.table;
  }
  
  public int getType()
  {
    return this.type;
  }
}
