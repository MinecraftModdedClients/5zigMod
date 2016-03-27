package org.h2.table;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import org.h2.command.Prepared;
import org.h2.command.dml.Query;
import org.h2.engine.Database;
import org.h2.engine.DbObject;
import org.h2.engine.Session;
import org.h2.engine.User;
import org.h2.expression.Alias;
import org.h2.expression.Expression;
import org.h2.expression.ExpressionColumn;
import org.h2.expression.ExpressionVisitor;
import org.h2.expression.Parameter;
import org.h2.index.Index;
import org.h2.index.IndexType;
import org.h2.index.ViewIndex;
import org.h2.message.DbException;
import org.h2.result.LocalResult;
import org.h2.result.Row;
import org.h2.result.SortOrder;
import org.h2.schema.Schema;
import org.h2.util.New;
import org.h2.util.SmallLRUCache;
import org.h2.util.StatementBuilder;
import org.h2.util.StringUtils;
import org.h2.util.SynchronizedVerifier;

public class TableView
  extends Table
{
  private static final long ROW_COUNT_APPROXIMATION = 100L;
  private String querySQL;
  private ArrayList<Table> tables;
  private String[] columnNames;
  private Query viewQuery;
  private ViewIndex index;
  private boolean recursive;
  private DbException createException;
  private final SmallLRUCache<CacheKey, ViewIndex> indexCache = SmallLRUCache.newInstance(64);
  private long lastModificationCheck;
  private long maxDataModificationId;
  private User owner;
  private Query topQuery;
  private LocalResult recursiveResult;
  private boolean tableExpression;
  
  public TableView(Schema paramSchema, int paramInt, String paramString1, String paramString2, ArrayList<Parameter> paramArrayList, String[] paramArrayOfString, Session paramSession, boolean paramBoolean)
  {
    super(paramSchema, paramInt, paramString1, false, true);
    init(paramString2, paramArrayList, paramArrayOfString, paramSession, paramBoolean);
  }
  
  public void replace(String paramString, String[] paramArrayOfString, Session paramSession, boolean paramBoolean1, boolean paramBoolean2)
  {
    String str = this.querySQL;
    String[] arrayOfString = this.columnNames;
    boolean bool = this.recursive;
    init(paramString, null, paramArrayOfString, paramSession, paramBoolean1);
    DbException localDbException = recompile(paramSession, paramBoolean2);
    if (localDbException != null)
    {
      init(str, null, arrayOfString, paramSession, bool);
      recompile(paramSession, true);
      throw localDbException;
    }
  }
  
  private synchronized void init(String paramString, ArrayList<Parameter> paramArrayList, String[] paramArrayOfString, Session paramSession, boolean paramBoolean)
  {
    this.querySQL = paramString;
    this.columnNames = paramArrayOfString;
    this.recursive = paramBoolean;
    this.index = new ViewIndex(this, paramString, paramArrayList, paramBoolean);
    SynchronizedVerifier.check(this.indexCache);
    this.indexCache.clear();
    initColumnsAndTables(paramSession);
  }
  
  private static Query compileViewQuery(Session paramSession, String paramString)
  {
    Prepared localPrepared = paramSession.prepare(paramString);
    if (!(localPrepared instanceof Query)) {
      throw DbException.getSyntaxError(paramString, 0);
    }
    return (Query)localPrepared;
  }
  
  public synchronized DbException recompile(Session paramSession, boolean paramBoolean)
  {
    try
    {
      compileViewQuery(paramSession, this.querySQL);
    }
    catch (DbException localDbException1)
    {
      if (!paramBoolean) {
        return localDbException1;
      }
    }
    ArrayList localArrayList = getViews();
    if (localArrayList != null) {
      localArrayList = New.arrayList(localArrayList);
    }
    SynchronizedVerifier.check(this.indexCache);
    this.indexCache.clear();
    initColumnsAndTables(paramSession);
    if (localArrayList != null) {
      for (TableView localTableView : localArrayList)
      {
        DbException localDbException2 = localTableView.recompile(paramSession, paramBoolean);
        if ((localDbException2 != null) && (!paramBoolean)) {
          return localDbException2;
        }
      }
    }
    return paramBoolean ? null : this.createException;
  }
  
  private void initColumnsAndTables(Session paramSession)
  {
    removeViewFromTables();
    Column[] arrayOfColumn;
    try
    {
      Query localQuery = compileViewQuery(paramSession, this.querySQL);
      this.querySQL = localQuery.getPlanSQL();
      this.tables = New.arrayList(localQuery.getTables());
      ArrayList localArrayList1 = localQuery.getExpressions();
      ArrayList localArrayList2 = New.arrayList();
      int j = 0;
      for (int k = localQuery.getColumnCount(); j < k; j++)
      {
        Expression localExpression1 = (Expression)localArrayList1.get(j);
        String str = null;
        if ((this.columnNames != null) && (this.columnNames.length > j)) {
          str = this.columnNames[j];
        }
        if (str == null) {
          str = localExpression1.getAlias();
        }
        int m = localExpression1.getType();
        long l = localExpression1.getPrecision();
        int n = localExpression1.getScale();
        int i1 = localExpression1.getDisplaySize();
        Column localColumn = new Column(str, m, l, n, i1);
        localColumn.setTable(this, j);
        
        ExpressionColumn localExpressionColumn = null;
        Expression localExpression2;
        if ((localExpression1 instanceof ExpressionColumn))
        {
          localExpressionColumn = (ExpressionColumn)localExpression1;
        }
        else if ((localExpression1 instanceof Alias))
        {
          localExpression2 = localExpression1.getNonAliasExpression();
          if ((localExpression2 instanceof ExpressionColumn)) {
            localExpressionColumn = (ExpressionColumn)localExpression2;
          }
        }
        if (localExpressionColumn != null)
        {
          localExpression2 = localExpressionColumn.getColumn().getCheckConstraint(paramSession, str);
          if (localExpression2 != null) {
            localColumn.addCheckConstraint(paramSession, localExpression2);
          }
        }
        localArrayList2.add(localColumn);
      }
      arrayOfColumn = new Column[localArrayList2.size()];
      localArrayList2.toArray(arrayOfColumn);
      this.createException = null;
      this.viewQuery = localQuery;
    }
    catch (DbException localDbException)
    {
      localDbException.addSQL(getCreateSQL());
      this.createException = localDbException;
      
      this.tables = New.arrayList();
      arrayOfColumn = new Column[0];
      if ((this.recursive) && (this.columnNames != null))
      {
        arrayOfColumn = new Column[this.columnNames.length];
        for (int i = 0; i < this.columnNames.length; i++) {
          arrayOfColumn[i] = new Column(this.columnNames[i], 13);
        }
        this.index.setRecursive(true);
        this.createException = null;
      }
    }
    setColumns(arrayOfColumn);
    if (getId() != 0) {
      addViewToTables();
    }
  }
  
  public boolean isInvalid()
  {
    return this.createException != null;
  }
  
  public PlanItem getBestPlanItem(Session paramSession, int[] paramArrayOfInt, TableFilter paramTableFilter, SortOrder paramSortOrder)
  {
    PlanItem localPlanItem = new PlanItem();
    localPlanItem.cost = this.index.getCost(paramSession, paramArrayOfInt, paramTableFilter, paramSortOrder);
    CacheKey localCacheKey = new CacheKey(paramArrayOfInt, paramSession);
    synchronized (this)
    {
      SynchronizedVerifier.check(this.indexCache);
      ViewIndex localViewIndex1 = (ViewIndex)this.indexCache.get(localCacheKey);
      if (localViewIndex1 != null)
      {
        localPlanItem.setIndex(localViewIndex1);
        return localPlanItem;
      }
    }
    ??? = new ViewIndex(this, this.index, paramSession, paramArrayOfInt);
    synchronized (this)
    {
      ViewIndex localViewIndex2 = (ViewIndex)this.indexCache.get(localCacheKey);
      if (localViewIndex2 != null)
      {
        localPlanItem.setIndex(localViewIndex2);
        return localPlanItem;
      }
      this.indexCache.put(localCacheKey, ???);
      localPlanItem.setIndex((Index)???);
    }
    return localPlanItem;
  }
  
  public boolean isQueryComparable()
  {
    if (!super.isQueryComparable()) {
      return false;
    }
    for (Table localTable : this.tables) {
      if (!localTable.isQueryComparable()) {
        return false;
      }
    }
    if ((this.topQuery != null) && (!this.topQuery.isEverything(ExpressionVisitor.QUERY_COMPARABLE_VISITOR))) {
      return false;
    }
    return true;
  }
  
  public String getDropSQL()
  {
    return "DROP VIEW IF EXISTS " + getSQL() + " CASCADE";
  }
  
  public String getCreateSQLForCopy(Table paramTable, String paramString)
  {
    return getCreateSQL(false, true, paramString);
  }
  
  public String getCreateSQL()
  {
    return getCreateSQL(false, true);
  }
  
  public String getCreateSQL(boolean paramBoolean1, boolean paramBoolean2)
  {
    return getCreateSQL(paramBoolean1, paramBoolean2, getSQL());
  }
  
  private String getCreateSQL(boolean paramBoolean1, boolean paramBoolean2, String paramString)
  {
    StatementBuilder localStatementBuilder = new StatementBuilder("CREATE ");
    if (paramBoolean1) {
      localStatementBuilder.append("OR REPLACE ");
    }
    if (paramBoolean2) {
      localStatementBuilder.append("FORCE ");
    }
    localStatementBuilder.append("VIEW ");
    localStatementBuilder.append(paramString);
    if (this.comment != null) {
      localStatementBuilder.append(" COMMENT ").append(StringUtils.quoteStringSQL(this.comment));
    }
    String str;
    if ((this.columns != null) && (this.columns.length > 0))
    {
      localStatementBuilder.append('(');
      for (str : this.columns)
      {
        localStatementBuilder.appendExceptFirst(", ");
        localStatementBuilder.append(str.getSQL());
      }
      localStatementBuilder.append(')');
    }
    else if (this.columnNames != null)
    {
      localStatementBuilder.append('(');
      for (str : this.columnNames)
      {
        localStatementBuilder.appendExceptFirst(", ");
        localStatementBuilder.append(str);
      }
      localStatementBuilder.append(')');
    }
    return localStatementBuilder.append(" AS\n").append(this.querySQL).toString();
  }
  
  public void checkRename() {}
  
  public boolean lock(Session paramSession, boolean paramBoolean1, boolean paramBoolean2)
  {
    return false;
  }
  
  public void close(Session paramSession) {}
  
  public void unlock(Session paramSession) {}
  
  public boolean isLockedExclusively()
  {
    return false;
  }
  
  public Index addIndex(Session paramSession, String paramString1, int paramInt, IndexColumn[] paramArrayOfIndexColumn, IndexType paramIndexType, boolean paramBoolean, String paramString2)
  {
    throw DbException.getUnsupportedException("VIEW");
  }
  
  public void removeRow(Session paramSession, Row paramRow)
  {
    throw DbException.getUnsupportedException("VIEW");
  }
  
  public void addRow(Session paramSession, Row paramRow)
  {
    throw DbException.getUnsupportedException("VIEW");
  }
  
  public void checkSupportAlter()
  {
    throw DbException.getUnsupportedException("VIEW");
  }
  
  public void truncate(Session paramSession)
  {
    throw DbException.getUnsupportedException("VIEW");
  }
  
  public long getRowCount(Session paramSession)
  {
    throw DbException.throwInternalError();
  }
  
  public boolean canGetRowCount()
  {
    return false;
  }
  
  public boolean canDrop()
  {
    return true;
  }
  
  public String getTableType()
  {
    return "VIEW";
  }
  
  public void removeChildrenAndResources(Session paramSession)
  {
    removeViewFromTables();
    super.removeChildrenAndResources(paramSession);
    this.database.removeMeta(paramSession, getId());
    this.querySQL = null;
    this.index = null;
    invalidate();
  }
  
  public String getSQL()
  {
    if (isTemporary()) {
      return "(\n" + StringUtils.indent(this.querySQL) + ")";
    }
    return super.getSQL();
  }
  
  public String getQuery()
  {
    return this.querySQL;
  }
  
  public Index getScanIndex(Session paramSession)
  {
    if (this.createException != null)
    {
      localObject = this.createException.getMessage();
      throw DbException.get(90109, this.createException, new String[] { getSQL(), localObject });
    }
    Object localObject = getBestPlanItem(paramSession, null, null, null);
    return ((PlanItem)localObject).getIndex();
  }
  
  public boolean canReference()
  {
    return false;
  }
  
  public ArrayList<Index> getIndexes()
  {
    return null;
  }
  
  public long getMaxDataModificationId()
  {
    if (this.createException != null) {
      return Long.MAX_VALUE;
    }
    if (this.viewQuery == null) {
      return Long.MAX_VALUE;
    }
    long l = this.database.getModificationDataId();
    if ((l > this.lastModificationCheck) && (this.maxDataModificationId <= l))
    {
      this.maxDataModificationId = this.viewQuery.getMaxDataModificationId();
      this.lastModificationCheck = l;
    }
    return this.maxDataModificationId;
  }
  
  public Index getUniqueIndex()
  {
    return null;
  }
  
  private void removeViewFromTables()
  {
    if (this.tables != null)
    {
      for (Table localTable : this.tables) {
        localTable.removeView(this);
      }
      this.tables.clear();
    }
  }
  
  private void addViewToTables()
  {
    for (Table localTable : this.tables) {
      localTable.addView(this);
    }
  }
  
  private void setOwner(User paramUser)
  {
    this.owner = paramUser;
  }
  
  public User getOwner()
  {
    return this.owner;
  }
  
  public static TableView createTempView(Session paramSession, User paramUser, String paramString, Query paramQuery1, Query paramQuery2)
  {
    Schema localSchema = paramSession.getDatabase().getSchema("PUBLIC");
    String str = paramQuery1.getPlanSQL();
    TableView localTableView = new TableView(localSchema, 0, paramString, str, paramQuery1.getParameters(), null, paramSession, false);
    if (localTableView.createException != null) {
      throw localTableView.createException;
    }
    localTableView.setTopQuery(paramQuery2);
    localTableView.setOwner(paramUser);
    localTableView.setTemporary(true);
    return localTableView;
  }
  
  private void setTopQuery(Query paramQuery)
  {
    this.topQuery = paramQuery;
  }
  
  public long getRowCountApproximation()
  {
    return 100L;
  }
  
  public long getDiskSpaceUsed()
  {
    return 0L;
  }
  
  public int getParameterOffset()
  {
    return this.topQuery == null ? 0 : this.topQuery.getParameters().size();
  }
  
  public boolean isDeterministic()
  {
    if ((this.recursive) || (this.viewQuery == null)) {
      return false;
    }
    return this.viewQuery.isEverything(ExpressionVisitor.DETERMINISTIC_VISITOR);
  }
  
  public void setRecursiveResult(LocalResult paramLocalResult)
  {
    if (this.recursiveResult != null) {
      this.recursiveResult.close();
    }
    this.recursiveResult = paramLocalResult;
  }
  
  public LocalResult getRecursiveResult()
  {
    return this.recursiveResult;
  }
  
  public void setTableExpression(boolean paramBoolean)
  {
    this.tableExpression = paramBoolean;
  }
  
  public boolean isTableExpression()
  {
    return this.tableExpression;
  }
  
  public void addDependencies(HashSet<DbObject> paramHashSet)
  {
    super.addDependencies(paramHashSet);
    if (this.tables != null) {
      for (Table localTable : this.tables) {
        if (!"VIEW".equals(localTable.getTableType())) {
          localTable.addDependencies(paramHashSet);
        }
      }
    }
  }
  
  private static final class CacheKey
  {
    private final int[] masks;
    private final Session session;
    
    public CacheKey(int[] paramArrayOfInt, Session paramSession)
    {
      this.masks = paramArrayOfInt;
      this.session = paramSession;
    }
    
    public int hashCode()
    {
      int i = 1;
      i = 31 * i + Arrays.hashCode(this.masks);
      i = 31 * i + this.session.hashCode();
      return i;
    }
    
    public boolean equals(Object paramObject)
    {
      if (this == paramObject) {
        return true;
      }
      if (paramObject == null) {
        return false;
      }
      if (getClass() != paramObject.getClass()) {
        return false;
      }
      CacheKey localCacheKey = (CacheKey)paramObject;
      if (this.session != localCacheKey.session) {
        return false;
      }
      if (!Arrays.equals(this.masks, localCacheKey.masks)) {
        return false;
      }
      return true;
    }
  }
}
