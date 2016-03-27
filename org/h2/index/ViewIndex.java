package org.h2.index;

import java.util.ArrayList;
import org.h2.command.dml.Query;
import org.h2.command.dml.SelectUnion;
import org.h2.engine.Session;
import org.h2.expression.Parameter;
import org.h2.message.DbException;
import org.h2.result.LocalResult;
import org.h2.result.Row;
import org.h2.result.SearchRow;
import org.h2.result.SortOrder;
import org.h2.table.Column;
import org.h2.table.IndexColumn;
import org.h2.table.Table;
import org.h2.table.TableFilter;
import org.h2.table.TableView;
import org.h2.util.IntArray;
import org.h2.util.New;
import org.h2.util.SmallLRUCache;
import org.h2.util.SynchronizedVerifier;
import org.h2.util.Utils;
import org.h2.value.Value;

public class ViewIndex
  extends BaseIndex
  implements SpatialIndex
{
  private final TableView view;
  private final String querySQL;
  private final ArrayList<Parameter> originalParameters;
  private final SmallLRUCache<IntArray, CostElement> costCache = SmallLRUCache.newInstance(64);
  private boolean recursive;
  private final int[] indexMasks;
  private Query query;
  private final Session createSession;
  
  public ViewIndex(TableView paramTableView, String paramString, ArrayList<Parameter> paramArrayList, boolean paramBoolean)
  {
    initBaseIndex(paramTableView, 0, null, null, IndexType.createNonUnique(false));
    this.view = paramTableView;
    this.querySQL = paramString;
    this.originalParameters = paramArrayList;
    this.recursive = paramBoolean;
    this.columns = new Column[0];
    this.createSession = null;
    this.indexMasks = null;
  }
  
  public ViewIndex(TableView paramTableView, ViewIndex paramViewIndex, Session paramSession, int[] paramArrayOfInt)
  {
    initBaseIndex(paramTableView, 0, null, null, IndexType.createNonUnique(false));
    this.view = paramTableView;
    this.querySQL = paramViewIndex.querySQL;
    this.originalParameters = paramViewIndex.originalParameters;
    this.recursive = paramViewIndex.recursive;
    this.indexMasks = paramArrayOfInt;
    this.createSession = paramSession;
    this.columns = new Column[0];
    if (!this.recursive) {
      this.query = getQuery(paramSession, paramArrayOfInt);
    }
  }
  
  public Session getSession()
  {
    return this.createSession;
  }
  
  public String getPlanSQL()
  {
    return this.query == null ? null : this.query.getPlanSQL();
  }
  
  public void close(Session paramSession) {}
  
  public void add(Session paramSession, Row paramRow)
  {
    throw DbException.getUnsupportedException("VIEW");
  }
  
  public void remove(Session paramSession, Row paramRow)
  {
    throw DbException.getUnsupportedException("VIEW");
  }
  
  public synchronized double getCost(Session paramSession, int[] paramArrayOfInt, TableFilter paramTableFilter, SortOrder paramSortOrder)
  {
    if (this.recursive) {
      return 1000.0D;
    }
    IntArray localIntArray1 = new IntArray(paramArrayOfInt == null ? Utils.EMPTY_INT_ARRAY : paramArrayOfInt);
    
    SynchronizedVerifier.check(this.costCache);
    CostElement localCostElement = (CostElement)this.costCache.get(localIntArray1);
    if (localCostElement != null)
    {
      long l = System.currentTimeMillis();
      if (l < localCostElement.evaluatedAt + 10000L) {
        return localCostElement.cost;
      }
    }
    Query localQuery = (Query)paramSession.prepare(this.querySQL, true);
    if (paramArrayOfInt != null)
    {
      IntArray localIntArray2 = new IntArray();
      for (int i = 0; i < paramArrayOfInt.length; i++)
      {
        j = paramArrayOfInt[i];
        if (j != 0) {
          localIntArray2.add(i);
        }
      }
      i = localIntArray2.size();
      for (int j = 0; j < i; j++)
      {
        int k = localIntArray2.get(j);
        int m = paramArrayOfInt[k];
        int n = localQuery.getParameters().size() + this.view.getParameterOffset();
        Parameter localParameter;
        if ((m & 0x1) != 0)
        {
          localParameter = new Parameter(n);
          localQuery.addGlobalCondition(localParameter, k, 16);
        }
        else if ((m & 0x10) != 0)
        {
          localParameter = new Parameter(n);
          localQuery.addGlobalCondition(localParameter, k, 11);
        }
        else
        {
          if ((m & 0x2) != 0)
          {
            localParameter = new Parameter(n);
            localQuery.addGlobalCondition(localParameter, k, 1);
          }
          if ((m & 0x4) != 0)
          {
            localParameter = new Parameter(n);
            localQuery.addGlobalCondition(localParameter, k, 3);
          }
        }
      }
      String str = localQuery.getPlanSQL();
      localQuery = (Query)paramSession.prepare(str, true);
    }
    double d = localQuery.getCost();
    localCostElement = new CostElement();
    localCostElement.evaluatedAt = System.currentTimeMillis();
    localCostElement.cost = d;
    this.costCache.put(localIntArray1, localCostElement);
    return d;
  }
  
  public Cursor find(Session paramSession, SearchRow paramSearchRow1, SearchRow paramSearchRow2)
  {
    return find(paramSession, paramSearchRow1, paramSearchRow2, null);
  }
  
  public Cursor findByGeometry(TableFilter paramTableFilter, SearchRow paramSearchRow)
  {
    return find(paramTableFilter.getSession(), null, null, paramSearchRow);
  }
  
  private Cursor find(Session paramSession, SearchRow paramSearchRow1, SearchRow paramSearchRow2, SearchRow paramSearchRow3)
  {
    Object localObject2;
    Object localObject3;
    if (this.recursive)
    {
      localObject1 = this.view.getRecursiveResult();
      if (localObject1 != null)
      {
        ((LocalResult)localObject1).reset();
        return new ViewCursor(this, (LocalResult)localObject1, paramSearchRow1, paramSearchRow2);
      }
      if (this.query == null) {
        this.query = ((Query)this.createSession.prepare(this.querySQL, true));
      }
      if (!(this.query instanceof SelectUnion)) {
        throw DbException.get(42001, "recursive queries without UNION ALL");
      }
      SelectUnion localSelectUnion = (SelectUnion)this.query;
      if (localSelectUnion.getUnionType() != 1) {
        throw DbException.get(42001, "recursive queries without UNION ALL");
      }
      Query localQuery = localSelectUnion.getLeft();
      
      localQuery.disableCache();
      localObject2 = localQuery.query(0);
      LocalResult localLocalResult2 = localSelectUnion.getEmptyResult();
      
      localLocalResult2.setMaxMemoryRows(Integer.MAX_VALUE);
      while (((LocalResult)localObject2).next()) {
        localLocalResult2.addRow(((LocalResult)localObject2).currentRow());
      }
      localObject3 = localSelectUnion.getRight();
      ((LocalResult)localObject2).reset();
      this.view.setRecursiveResult((LocalResult)localObject2);
      
      ((Query)localObject3).disableCache();
      for (;;)
      {
        localObject2 = ((Query)localObject3).query(0);
        if (((LocalResult)localObject2).getRowCount() == 0) {
          break;
        }
        while (((LocalResult)localObject2).next()) {
          localLocalResult2.addRow(((LocalResult)localObject2).currentRow());
        }
        ((LocalResult)localObject2).reset();
        this.view.setRecursiveResult((LocalResult)localObject2);
      }
      this.view.setRecursiveResult(null);
      localLocalResult2.done();
      return new ViewCursor(this, localLocalResult2, paramSearchRow1, paramSearchRow2);
    }
    Object localObject1 = this.query.getParameters();
    int i;
    int m;
    if (this.originalParameters != null)
    {
      i = 0;
      for (j = this.originalParameters.size(); i < j; i++)
      {
        localObject2 = (Parameter)this.originalParameters.get(i);
        m = ((Parameter)localObject2).getIndex();
        localObject3 = ((Parameter)localObject2).getValue(paramSession);
        setParameter((ArrayList)localObject1, m, (Value)localObject3);
      }
    }
    if (paramSearchRow1 != null) {
      i = paramSearchRow1.getColumnCount();
    } else if (paramSearchRow2 != null) {
      i = paramSearchRow2.getColumnCount();
    } else if (paramSearchRow3 != null) {
      i = paramSearchRow3.getColumnCount();
    } else {
      i = 0;
    }
    int j = this.originalParameters == null ? 0 : this.originalParameters.size();
    j += this.view.getParameterOffset();
    for (int k = 0; k < i; k++)
    {
      m = this.indexMasks[k];
      if ((m & 0x1) != 0) {
        setParameter((ArrayList)localObject1, j++, paramSearchRow1.getValue(k));
      }
      if ((m & 0x2) != 0) {
        setParameter((ArrayList)localObject1, j++, paramSearchRow1.getValue(k));
      }
      if ((m & 0x4) != 0) {
        setParameter((ArrayList)localObject1, j++, paramSearchRow2.getValue(k));
      }
      if ((m & 0x10) != 0) {
        setParameter((ArrayList)localObject1, j++, paramSearchRow3.getValue(k));
      }
    }
    LocalResult localLocalResult1 = this.query.query(0);
    return new ViewCursor(this, localLocalResult1, paramSearchRow1, paramSearchRow2);
  }
  
  private static void setParameter(ArrayList<Parameter> paramArrayList, int paramInt, Value paramValue)
  {
    if (paramInt >= paramArrayList.size()) {
      return;
    }
    Parameter localParameter = (Parameter)paramArrayList.get(paramInt);
    localParameter.setValue(paramValue);
  }
  
  private Query getQuery(Session paramSession, int[] paramArrayOfInt)
  {
    Query localQuery = (Query)paramSession.prepare(this.querySQL, true);
    if (paramArrayOfInt == null) {
      return localQuery;
    }
    if (!localQuery.allowGlobalConditions()) {
      return localQuery;
    }
    int i = this.originalParameters == null ? 0 : this.originalParameters.size();
    
    i += this.view.getParameterOffset();
    IntArray localIntArray = new IntArray();
    int j = 0;
    for (int k = 0; k < paramArrayOfInt.length; k++)
    {
      int m = paramArrayOfInt[k];
      if (m != 0)
      {
        j++;
        localIntArray.add(k);
        if (Integer.bitCount(m) > 1) {
          localIntArray.add(k);
        }
      }
    }
    k = localIntArray.size();
    ArrayList localArrayList = New.arrayList();
    for (int n = 0; n < k;)
    {
      i1 = localIntArray.get(n);
      localArrayList.add(this.table.getColumn(i1));
      i2 = paramArrayOfInt[i1];
      Parameter localParameter;
      if ((i2 & 0x1) != 0)
      {
        localParameter = new Parameter(i + n);
        localQuery.addGlobalCondition(localParameter, i1, 16);
        n++;
      }
      if ((i2 & 0x2) != 0)
      {
        localParameter = new Parameter(i + n);
        localQuery.addGlobalCondition(localParameter, i1, 1);
        n++;
      }
      if ((i2 & 0x4) != 0)
      {
        localParameter = new Parameter(i + n);
        localQuery.addGlobalCondition(localParameter, i1, 3);
        n++;
      }
      if ((i2 & 0x10) != 0)
      {
        localParameter = new Parameter(i + n);
        localQuery.addGlobalCondition(localParameter, i1, 11);
        n++;
      }
    }
    int i2;
    this.columns = new Column[localArrayList.size()];
    localArrayList.toArray(this.columns);
    
    this.indexColumns = new IndexColumn[j];
    this.columnIds = new int[j];
    n = 0;
    for (int i1 = 0; n < 2; n++) {
      for (i2 = 0; i2 < paramArrayOfInt.length; i2++)
      {
        int i3 = paramArrayOfInt[i2];
        if (i3 != 0) {
          if (n == 0 ? 
            (i3 & 0x1) != 0 : 
            
            (i3 & 0x1) == 0)
          {
            IndexColumn localIndexColumn = new IndexColumn();
            localIndexColumn.column = this.table.getColumn(i2);
            this.indexColumns[i1] = localIndexColumn;
            this.columnIds[i1] = localIndexColumn.column.getColumnId();
            i1++;
          }
        }
      }
    }
    String str = localQuery.getPlanSQL();
    localQuery = (Query)paramSession.prepare(str, true);
    return localQuery;
  }
  
  public void remove(Session paramSession)
  {
    throw DbException.getUnsupportedException("VIEW");
  }
  
  public void truncate(Session paramSession)
  {
    throw DbException.getUnsupportedException("VIEW");
  }
  
  public void checkRename()
  {
    throw DbException.getUnsupportedException("VIEW");
  }
  
  public boolean needRebuild()
  {
    return false;
  }
  
  public boolean canGetFirstOrLast()
  {
    return false;
  }
  
  public Cursor findFirstOrLast(Session paramSession, boolean paramBoolean)
  {
    throw DbException.getUnsupportedException("VIEW");
  }
  
  public void setRecursive(boolean paramBoolean)
  {
    this.recursive = paramBoolean;
  }
  
  public long getRowCount(Session paramSession)
  {
    return 0L;
  }
  
  public long getRowCountApproximation()
  {
    return 0L;
  }
  
  public long getDiskSpaceUsed()
  {
    return 0L;
  }
  
  public boolean isRecursive()
  {
    return this.recursive;
  }
  
  static class CostElement
  {
    long evaluatedAt;
    double cost;
  }
}
