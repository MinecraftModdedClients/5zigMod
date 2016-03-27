package org.h2.index;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import org.h2.engine.Database;
import org.h2.engine.Session;
import org.h2.engine.SysProperties;
import org.h2.message.DbException;
import org.h2.message.Trace;
import org.h2.result.Row;
import org.h2.result.SearchRow;
import org.h2.result.SortOrder;
import org.h2.store.LobStorageInterface;
import org.h2.store.Page;
import org.h2.store.PageStore;
import org.h2.table.Column;
import org.h2.table.IndexColumn;
import org.h2.table.RegularTable;
import org.h2.table.Table;
import org.h2.table.TableFilter;
import org.h2.util.MathUtils;
import org.h2.util.New;
import org.h2.value.Value;
import org.h2.value.ValueNull;

public class PageDataIndex
  extends PageIndex
{
  private final PageStore store;
  private final RegularTable tableData;
  private long lastKey;
  private long rowCount;
  private HashSet<Row> delta;
  private int rowCountDiff;
  private final HashMap<Integer, Integer> sessionRowCount;
  private int mainIndexColumn = -1;
  private DbException fastDuplicateKeyException;
  private int memoryPerPage;
  private int memoryCount;
  private final boolean multiVersion;
  
  public PageDataIndex(RegularTable paramRegularTable, int paramInt, IndexColumn[] paramArrayOfIndexColumn, IndexType paramIndexType, boolean paramBoolean, Session paramSession)
  {
    initBaseIndex(paramRegularTable, paramInt, paramRegularTable.getName() + "_DATA", paramArrayOfIndexColumn, paramIndexType);
    this.multiVersion = this.database.isMultiVersion();
    if (this.multiVersion)
    {
      this.sessionRowCount = New.hashMap();
      this.isMultiVersion = true;
    }
    else
    {
      this.sessionRowCount = null;
    }
    this.tableData = paramRegularTable;
    this.store = this.database.getPageStore();
    this.store.addIndex(this);
    if (!this.database.isPersistent()) {
      throw DbException.throwInternalError(paramRegularTable.getName());
    }
    Object localObject;
    if (paramBoolean)
    {
      this.rootPageId = this.store.allocatePage();
      this.store.addMeta(this, paramSession);
      localObject = PageDataLeaf.create(this, this.rootPageId, 0);
      this.store.update((Page)localObject);
    }
    else
    {
      this.rootPageId = this.store.getRootPageId(paramInt);
      localObject = getPage(this.rootPageId, 0);
      this.lastKey = ((PageData)localObject).getLastKey();
      this.rowCount = ((PageData)localObject).getRowCount();
    }
    if (this.trace.isDebugEnabled()) {
      this.trace.debug("{0} opened rows: {1}", new Object[] { this, Long.valueOf(this.rowCount) });
    }
    paramRegularTable.setRowCount(this.rowCount);
    this.memoryPerPage = (240 + this.store.getPageSize() >> 2);
  }
  
  public DbException getDuplicateKeyException(String paramString)
  {
    if (this.fastDuplicateKeyException == null) {
      this.fastDuplicateKeyException = super.getDuplicateKeyException(null);
    }
    return this.fastDuplicateKeyException;
  }
  
  /* Error */
  public void add(Session paramSession, Row paramRow)
  {
    // Byte code:
    //   0: iconst_0
    //   1: istore_3
    //   2: aload_0
    //   3: getfield 2	org/h2/index/PageDataIndex:mainIndexColumn	I
    //   6: iconst_m1
    //   7: if_icmpeq +21 -> 28
    //   10: aload_2
    //   11: aload_2
    //   12: aload_0
    //   13: getfield 2	org/h2/index/PageDataIndex:mainIndexColumn	I
    //   16: invokevirtual 44	org/h2/result/Row:getValue	(I)Lorg/h2/value/Value;
    //   19: invokevirtual 45	org/h2/value/Value:getLong	()J
    //   22: invokevirtual 46	org/h2/result/Row:setKey	(J)V
    //   25: goto +31 -> 56
    //   28: aload_2
    //   29: invokevirtual 47	org/h2/result/Row:getKey	()J
    //   32: lconst_0
    //   33: lcmp
    //   34: ifne +22 -> 56
    //   37: aload_2
    //   38: aload_0
    //   39: dup
    //   40: getfield 30	org/h2/index/PageDataIndex:lastKey	J
    //   43: lconst_1
    //   44: ladd
    //   45: dup2_x1
    //   46: putfield 30	org/h2/index/PageDataIndex:lastKey	J
    //   49: l2i
    //   50: i2l
    //   51: invokevirtual 46	org/h2/result/Row:setKey	(J)V
    //   54: iconst_1
    //   55: istore_3
    //   56: aload_0
    //   57: getfield 16	org/h2/index/PageDataIndex:tableData	Lorg/h2/table/RegularTable;
    //   60: invokevirtual 48	org/h2/table/RegularTable:getContainsLargeObject	()Z
    //   63: ifeq +77 -> 140
    //   66: iconst_0
    //   67: istore 4
    //   69: aload_2
    //   70: invokevirtual 49	org/h2/result/Row:getColumnCount	()I
    //   73: istore 5
    //   75: iload 4
    //   77: iload 5
    //   79: if_icmpge +61 -> 140
    //   82: aload_2
    //   83: iload 4
    //   85: invokevirtual 44	org/h2/result/Row:getValue	(I)Lorg/h2/value/Value;
    //   88: astore 6
    //   90: aload 6
    //   92: aload_0
    //   93: getfield 10	org/h2/index/PageDataIndex:database	Lorg/h2/engine/Database;
    //   96: aload_0
    //   97: invokevirtual 50	org/h2/index/PageDataIndex:getId	()I
    //   100: invokevirtual 51	org/h2/value/Value:link	(Lorg/h2/store/DataHandler;I)Lorg/h2/value/Value;
    //   103: astore 7
    //   105: aload 7
    //   107: invokevirtual 52	org/h2/value/Value:isLinked	()Z
    //   110: ifeq +9 -> 119
    //   113: aload_1
    //   114: aload 7
    //   116: invokevirtual 53	org/h2/engine/Session:unlinkAtCommitStop	(Lorg/h2/value/Value;)V
    //   119: aload 6
    //   121: aload 7
    //   123: if_acmpeq +11 -> 134
    //   126: aload_2
    //   127: iload 4
    //   129: aload 7
    //   131: invokevirtual 54	org/h2/result/Row:setValue	(ILorg/h2/value/Value;)V
    //   134: iinc 4 1
    //   137: goto -62 -> 75
    //   140: aload_0
    //   141: getfield 33	org/h2/index/PageDataIndex:trace	Lorg/h2/message/Trace;
    //   144: invokevirtual 34	org/h2/message/Trace:isDebugEnabled	()Z
    //   147: ifeq +27 -> 174
    //   150: aload_0
    //   151: getfield 33	org/h2/index/PageDataIndex:trace	Lorg/h2/message/Trace;
    //   154: ldc 55
    //   156: iconst_2
    //   157: anewarray 36	java/lang/Object
    //   160: dup
    //   161: iconst_0
    //   162: aload_0
    //   163: invokevirtual 56	org/h2/index/PageDataIndex:getName	()Ljava/lang/String;
    //   166: aastore
    //   167: dup
    //   168: iconst_1
    //   169: aload_2
    //   170: aastore
    //   171: invokevirtual 38	org/h2/message/Trace:debug	(Ljava/lang/String;[Ljava/lang/Object;)V
    //   174: lconst_0
    //   175: lstore 4
    //   177: aload_0
    //   178: aload_1
    //   179: aload_2
    //   180: invokespecial 57	org/h2/index/PageDataIndex:addTry	(Lorg/h2/engine/Session;Lorg/h2/result/Row;)V
    //   183: aload_0
    //   184: getfield 18	org/h2/index/PageDataIndex:store	Lorg/h2/store/PageStore;
    //   187: invokevirtual 58	org/h2/store/PageStore:incrementChangeCount	()V
    //   190: goto +96 -> 286
    //   193: astore 6
    //   195: aload 6
    //   197: aload_0
    //   198: getfield 42	org/h2/index/PageDataIndex:fastDuplicateKeyException	Lorg/h2/message/DbException;
    //   201: if_acmpeq +6 -> 207
    //   204: aload 6
    //   206: athrow
    //   207: iload_3
    //   208: ifne +8 -> 216
    //   211: aload_0
    //   212: invokevirtual 60	org/h2/index/PageDataIndex:getNewDuplicateKeyException	()Lorg/h2/message/DbException;
    //   215: athrow
    //   216: lload 4
    //   218: lconst_0
    //   219: lcmp
    //   220: ifne +24 -> 244
    //   223: aload_2
    //   224: aload_2
    //   225: invokevirtual 47	org/h2/result/Row:getKey	()J
    //   228: l2d
    //   229: invokestatic 61	java/lang/Math:random	()D
    //   232: ldc2_w 62
    //   235: dmul
    //   236: dadd
    //   237: d2l
    //   238: invokevirtual 46	org/h2/result/Row:setKey	(J)V
    //   241: goto +14 -> 255
    //   244: aload_2
    //   245: aload_2
    //   246: invokevirtual 47	org/h2/result/Row:getKey	()J
    //   249: lload 4
    //   251: ladd
    //   252: invokevirtual 46	org/h2/result/Row:setKey	(J)V
    //   255: lload 4
    //   257: lconst_1
    //   258: ladd
    //   259: lstore 4
    //   261: aload_0
    //   262: getfield 18	org/h2/index/PageDataIndex:store	Lorg/h2/store/PageStore;
    //   265: invokevirtual 58	org/h2/store/PageStore:incrementChangeCount	()V
    //   268: goto +15 -> 283
    //   271: astore 8
    //   273: aload_0
    //   274: getfield 18	org/h2/index/PageDataIndex:store	Lorg/h2/store/PageStore;
    //   277: invokevirtual 58	org/h2/store/PageStore:incrementChangeCount	()V
    //   280: aload 8
    //   282: athrow
    //   283: goto -106 -> 177
    //   286: aload_0
    //   287: aload_0
    //   288: getfield 30	org/h2/index/PageDataIndex:lastKey	J
    //   291: aload_2
    //   292: invokevirtual 47	org/h2/result/Row:getKey	()J
    //   295: invokestatic 64	java/lang/Math:max	(JJ)J
    //   298: putfield 30	org/h2/index/PageDataIndex:lastKey	J
    //   301: return
    // Line number table:
    //   Java source line #107	-> byte code offset #0
    //   Java source line #108	-> byte code offset #2
    //   Java source line #109	-> byte code offset #10
    //   Java source line #111	-> byte code offset #28
    //   Java source line #112	-> byte code offset #37
    //   Java source line #113	-> byte code offset #54
    //   Java source line #116	-> byte code offset #56
    //   Java source line #117	-> byte code offset #66
    //   Java source line #118	-> byte code offset #82
    //   Java source line #119	-> byte code offset #90
    //   Java source line #120	-> byte code offset #105
    //   Java source line #121	-> byte code offset #113
    //   Java source line #123	-> byte code offset #119
    //   Java source line #124	-> byte code offset #126
    //   Java source line #117	-> byte code offset #134
    //   Java source line #130	-> byte code offset #140
    //   Java source line #131	-> byte code offset #150
    //   Java source line #133	-> byte code offset #174
    //   Java source line #136	-> byte code offset #177
    //   Java source line #154	-> byte code offset #183
    //   Java source line #138	-> byte code offset #193
    //   Java source line #139	-> byte code offset #195
    //   Java source line #140	-> byte code offset #204
    //   Java source line #142	-> byte code offset #207
    //   Java source line #143	-> byte code offset #211
    //   Java source line #145	-> byte code offset #216
    //   Java source line #148	-> byte code offset #223
    //   Java source line #150	-> byte code offset #244
    //   Java source line #152	-> byte code offset #255
    //   Java source line #154	-> byte code offset #261
    //   Java source line #155	-> byte code offset #268
    //   Java source line #154	-> byte code offset #271
    //   Java source line #157	-> byte code offset #286
    //   Java source line #158	-> byte code offset #301
    // Local variable table:
    //   start	length	slot	name	signature
    //   0	302	0	this	PageDataIndex
    //   0	302	1	paramSession	Session
    //   0	302	2	paramRow	Row
    //   1	207	3	i	int
    //   67	68	4	j	int
    //   175	85	4	l	long
    //   73	7	5	k	int
    //   88	32	6	localValue1	Value
    //   193	12	6	localDbException	DbException
    //   103	27	7	localValue2	Value
    //   271	10	8	localObject	Object
    // Exception table:
    //   from	to	target	type
    //   177	183	193	org/h2/message/DbException
    //   177	183	271	finally
    //   193	261	271	finally
    //   271	273	271	finally
  }
  
  public DbException getNewDuplicateKeyException()
  {
    String str = "PRIMARY KEY ON " + this.table.getSQL();
    if ((this.mainIndexColumn >= 0) && (this.mainIndexColumn < this.indexColumns.length)) {
      str = str + "(" + this.indexColumns[this.mainIndexColumn].getSQL() + ")";
    }
    DbException localDbException = DbException.get(23505, str);
    localDbException.setSource(this);
    return localDbException;
  }
  
  private void addTry(Session paramSession, Row paramRow)
  {
    for (;;)
    {
      Object localObject1 = getPage(this.rootPageId, 0);
      int i = ((PageData)localObject1).addRowTry(paramRow);
      if (i == -1) {
        break;
      }
      if (this.trace.isDebugEnabled()) {
        this.trace.debug("{0} split", new Object[] { this });
      }
      long l = i == 0 ? paramRow.getKey() : ((PageData)localObject1).getKey(i - 1);
      Object localObject2 = localObject1;
      PageData localPageData = ((PageData)localObject1).split(i);
      int j = this.store.allocatePage();
      ((PageData)localObject2).setPageId(j);
      ((PageData)localObject2).setParentPageId(this.rootPageId);
      localPageData.setParentPageId(this.rootPageId);
      PageDataNode localPageDataNode = PageDataNode.create(this, this.rootPageId, 0);
      localPageDataNode.init((PageData)localObject2, l, localPageData);
      this.store.update((Page)localObject2);
      this.store.update(localPageData);
      this.store.update(localPageDataNode);
      localObject1 = localPageDataNode;
    }
    paramRow.setDeleted(false);
    if (this.multiVersion)
    {
      if (this.delta == null) {
        this.delta = New.hashSet();
      }
      boolean bool = this.delta.remove(paramRow);
      if (!bool) {
        this.delta.add(paramRow);
      }
      incrementRowCount(paramSession.getId(), 1);
    }
    invalidateRowCount();
    this.rowCount += 1L;
    this.store.logAddOrRemoveRow(paramSession, this.tableData.getId(), paramRow, true);
  }
  
  PageDataOverflow getPageOverflow(int paramInt)
  {
    Page localPage = this.store.getPage(paramInt);
    if ((localPage instanceof PageDataOverflow)) {
      return (PageDataOverflow)localPage;
    }
    throw DbException.get(90030, localPage == null ? "null" : localPage.toString());
  }
  
  PageData getPage(int paramInt1, int paramInt2)
  {
    Page localPage = this.store.getPage(paramInt1);
    if (localPage == null)
    {
      localObject = PageDataLeaf.create(this, paramInt1, paramInt2);
      
      this.store.logUndo((Page)localObject, null);
      this.store.update((Page)localObject);
      return (PageData)localObject;
    }
    if (!(localPage instanceof PageData)) {
      throw DbException.get(90030, "" + localPage);
    }
    Object localObject = (PageData)localPage;
    if ((paramInt2 != -1) && 
      (((PageData)localObject).getParentPageId() != paramInt2)) {
      throw DbException.throwInternalError(localObject + " parent " + ((PageData)localObject).getParentPageId() + " expected " + paramInt2);
    }
    return (PageData)localObject;
  }
  
  public boolean canGetFirstOrLast()
  {
    return false;
  }
  
  long getKey(SearchRow paramSearchRow, long paramLong1, long paramLong2)
  {
    if (paramSearchRow == null) {
      return paramLong1;
    }
    Value localValue = paramSearchRow.getValue(this.mainIndexColumn);
    if (localValue == null) {
      throw DbException.throwInternalError(paramSearchRow.toString());
    }
    if (localValue == ValueNull.INSTANCE) {
      return paramLong2;
    }
    return localValue.getLong();
  }
  
  public Cursor find(Session paramSession, SearchRow paramSearchRow1, SearchRow paramSearchRow2)
  {
    long l1 = paramSearchRow1 == null ? Long.MIN_VALUE : paramSearchRow1.getKey();
    long l2 = paramSearchRow2 == null ? Long.MAX_VALUE : paramSearchRow2.getKey();
    PageData localPageData = getPage(this.rootPageId, 0);
    return localPageData.find(paramSession, l1, l2, this.isMultiVersion);
  }
  
  Cursor find(Session paramSession, long paramLong1, long paramLong2, boolean paramBoolean)
  {
    PageData localPageData = getPage(this.rootPageId, 0);
    return localPageData.find(paramSession, paramLong1, paramLong2, paramBoolean);
  }
  
  public Cursor findFirstOrLast(Session paramSession, boolean paramBoolean)
  {
    throw DbException.throwInternalError();
  }
  
  long getLastKey()
  {
    PageData localPageData = getPage(this.rootPageId, 0);
    return localPageData.getLastKey();
  }
  
  public double getCost(Session paramSession, int[] paramArrayOfInt, TableFilter paramTableFilter, SortOrder paramSortOrder)
  {
    long l = 10L * (this.tableData.getRowCountApproximation() + 1000L);
    
    return l;
  }
  
  public boolean needRebuild()
  {
    return false;
  }
  
  public void remove(Session paramSession, Row paramRow)
  {
    Object localObject1;
    if (this.tableData.getContainsLargeObject())
    {
      int i = 0;
      for (int j = paramRow.getColumnCount(); i < j; i++)
      {
        localObject1 = paramRow.getValue(i);
        if (((Value)localObject1).isLinked()) {
          paramSession.unlinkAtCommit((Value)localObject1);
        }
      }
    }
    if (this.trace.isDebugEnabled()) {
      this.trace.debug("{0} remove {1}", new Object[] { getName(), paramRow });
    }
    if (this.rowCount == 1L) {
      removeAllRows();
    } else {
      try
      {
        long l = paramRow.getKey();
        localObject1 = getPage(this.rootPageId, 0);
        ((PageData)localObject1).remove(l);
        invalidateRowCount();
        this.rowCount -= 1L;
      }
      finally
      {
        this.store.incrementChangeCount();
      }
    }
    if (this.multiVersion)
    {
      paramRow.setDeleted(true);
      if (this.delta == null) {
        this.delta = New.hashSet();
      }
      boolean bool = this.delta.remove(paramRow);
      if (!bool) {
        this.delta.add(paramRow);
      }
      incrementRowCount(paramSession.getId(), -1);
    }
    this.store.logAddOrRemoveRow(paramSession, this.tableData.getId(), paramRow, false);
  }
  
  public void remove(Session paramSession)
  {
    if (this.trace.isDebugEnabled()) {
      this.trace.debug("{0} remove", new Object[] { this });
    }
    removeAllRows();
    this.store.free(this.rootPageId);
    this.store.removeMeta(this, paramSession);
  }
  
  public void truncate(Session paramSession)
  {
    if (this.trace.isDebugEnabled()) {
      this.trace.debug("{0} truncate", new Object[] { this });
    }
    this.store.logTruncate(paramSession, this.tableData.getId());
    removeAllRows();
    if ((this.tableData.getContainsLargeObject()) && (this.tableData.isPersistData()))
    {
      paramSession.commit(false);
      this.database.getLobStorage().removeAllForTable(this.table.getId());
    }
    if (this.multiVersion) {
      this.sessionRowCount.clear();
    }
    this.tableData.setRowCount(0L);
  }
  
  private void removeAllRows()
  {
    try
    {
      Object localObject1 = getPage(this.rootPageId, 0);
      ((PageData)localObject1).freeRecursive();
      localObject1 = PageDataLeaf.create(this, this.rootPageId, 0);
      this.store.removeFromCache(this.rootPageId);
      this.store.update((Page)localObject1);
      this.rowCount = 0L;
      this.lastKey = 0L;
    }
    finally
    {
      this.store.incrementChangeCount();
    }
  }
  
  public void checkRename()
  {
    throw DbException.getUnsupportedException("PAGE");
  }
  
  public Row getRow(Session paramSession, long paramLong)
  {
    return getRowWithKey(paramLong);
  }
  
  public Row getRowWithKey(long paramLong)
  {
    PageData localPageData = getPage(this.rootPageId, 0);
    return localPageData.getRowWithKey(paramLong);
  }
  
  PageStore getPageStore()
  {
    return this.store;
  }
  
  public long getRowCountApproximation()
  {
    return this.rowCount;
  }
  
  public long getRowCount(Session paramSession)
  {
    if (this.multiVersion)
    {
      Integer localInteger = (Integer)this.sessionRowCount.get(Integer.valueOf(paramSession.getId()));
      long l = localInteger == null ? 0L : localInteger.intValue();
      l += this.rowCount;
      l -= this.rowCountDiff;
      return l;
    }
    return this.rowCount;
  }
  
  public long getDiskSpaceUsed()
  {
    PageData localPageData = getPage(this.rootPageId, 0);
    return localPageData.getDiskSpaceUsed();
  }
  
  public String getCreateSQL()
  {
    return null;
  }
  
  public int getColumnIndex(Column paramColumn)
  {
    return -1;
  }
  
  public void close(Session paramSession)
  {
    if (this.trace.isDebugEnabled()) {
      this.trace.debug("{0} close", new Object[] { this });
    }
    if (this.delta != null) {
      this.delta.clear();
    }
    this.rowCountDiff = 0;
    if (this.sessionRowCount != null) {
      this.sessionRowCount.clear();
    }
    writeRowCount();
  }
  
  Iterator<Row> getDelta()
  {
    if (this.delta == null)
    {
      List localList = Collections.emptyList();
      return localList.iterator();
    }
    return this.delta.iterator();
  }
  
  private void incrementRowCount(int paramInt1, int paramInt2)
  {
    if (this.multiVersion)
    {
      Integer localInteger1 = Integer.valueOf(paramInt1);
      Integer localInteger2 = (Integer)this.sessionRowCount.get(localInteger1);
      int i = localInteger2 == null ? 0 : localInteger2.intValue();
      this.sessionRowCount.put(localInteger1, Integer.valueOf(i + paramInt2));
      this.rowCountDiff += paramInt2;
    }
  }
  
  public void commit(int paramInt, Row paramRow)
  {
    if (this.multiVersion)
    {
      if (this.delta != null) {
        this.delta.remove(paramRow);
      }
      incrementRowCount(paramRow.getSessionId(), paramInt == 1 ? 1 : -1);
    }
  }
  
  void setRootPageId(Session paramSession, int paramInt)
  {
    this.store.removeMeta(this, paramSession);
    this.rootPageId = paramInt;
    this.store.addMeta(this, paramSession);
    this.store.addIndex(this);
  }
  
  public void setMainIndexColumn(int paramInt)
  {
    this.mainIndexColumn = paramInt;
  }
  
  public int getMainIndexColumn()
  {
    return this.mainIndexColumn;
  }
  
  public String toString()
  {
    return getName();
  }
  
  private void invalidateRowCount()
  {
    PageData localPageData = getPage(this.rootPageId, 0);
    localPageData.setRowCountStored(-1);
  }
  
  public void writeRowCount()
  {
    if ((SysProperties.MODIFY_ON_WRITE) && (this.rootPageId == 0)) {
      return;
    }
    try
    {
      PageData localPageData = getPage(this.rootPageId, 0);
      localPageData.setRowCountStored(MathUtils.convertLongToInt(this.rowCount));
    }
    finally
    {
      this.store.incrementChangeCount();
    }
  }
  
  public String getPlanSQL()
  {
    return this.table.getSQL() + ".tableScan";
  }
  
  int getMemoryPerPage()
  {
    return this.memoryPerPage;
  }
  
  void memoryChange(int paramInt)
  {
    if (this.memoryCount < 64) {
      this.memoryPerPage += (paramInt - this.memoryPerPage) / ++this.memoryCount;
    } else {
      this.memoryPerPage += (paramInt > this.memoryPerPage ? 1 : -1) + (paramInt - this.memoryPerPage) / 64;
    }
  }
  
  public boolean isRowIdIndex()
  {
    return true;
  }
}
