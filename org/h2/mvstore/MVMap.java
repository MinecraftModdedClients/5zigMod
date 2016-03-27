package org.h2.mvstore;

import java.util.AbstractList;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import org.h2.mvstore.type.DataType;
import org.h2.mvstore.type.ObjectDataType;
import org.h2.util.New;

public class MVMap<K, V>
  extends AbstractMap<K, V>
  implements ConcurrentMap<K, V>
{
  protected MVStore store;
  protected volatile Page root;
  protected volatile long writeVersion;
  private int id;
  private long createVersion;
  private final DataType keyType;
  private final DataType valueType;
  private ConcurrentArrayList<Page> oldRoots = new ConcurrentArrayList();
  private boolean closed;
  private boolean readOnly;
  private boolean isVolatile;
  
  protected MVMap(DataType paramDataType1, DataType paramDataType2)
  {
    this.keyType = paramDataType1;
    this.valueType = paramDataType2;
    this.root = Page.createEmpty(this, -1L);
  }
  
  static String getMapRootKey(int paramInt)
  {
    return "root." + Integer.toHexString(paramInt);
  }
  
  static String getMapKey(int paramInt)
  {
    return "map." + Integer.toHexString(paramInt);
  }
  
  protected void init(MVStore paramMVStore, HashMap<String, Object> paramHashMap)
  {
    this.store = paramMVStore;
    this.id = DataUtils.readHexInt(paramHashMap, "id", 0);
    this.createVersion = DataUtils.readHexLong(paramHashMap, "createVersion", 0L);
    this.writeVersion = paramMVStore.getCurrentVersion();
  }
  
  public synchronized V put(K paramK, V paramV)
  {
    DataUtils.checkArgument(paramV != null, "The value may not be null", new Object[0]);
    beforeWrite();
    long l = this.writeVersion;
    Page localPage = this.root.copy(l);
    localPage = splitRootIfNeeded(localPage, l);
    Object localObject = put(localPage, l, paramK, paramV);
    newRoot(localPage);
    return (V)localObject;
  }
  
  synchronized Page putBranch(Page paramPage, K paramK, V paramV)
  {
    DataUtils.checkArgument(paramV != null, "The value may not be null", new Object[0]);
    long l = this.writeVersion;
    Page localPage = paramPage.copy(l);
    localPage = splitRootIfNeeded(localPage, l);
    put(localPage, l, paramK, paramV);
    return localPage;
  }
  
  protected Page splitRootIfNeeded(Page paramPage, long paramLong)
  {
    if ((paramPage.getMemory() <= this.store.getPageSplitSize()) || (paramPage.getKeyCount() <= 1)) {
      return paramPage;
    }
    int i = paramPage.getKeyCount() / 2;
    long l = paramPage.getTotalCount();
    Object localObject = paramPage.getKey(i);
    Page localPage = paramPage.split(i);
    Object[] arrayOfObject = { localObject };
    Page.PageReference[] arrayOfPageReference = { new Page.PageReference(paramPage, paramPage.getPos(), paramPage.getTotalCount()), new Page.PageReference(localPage, localPage.getPos(), localPage.getTotalCount()) };
    
    paramPage = Page.create(this, paramLong, arrayOfObject, null, arrayOfPageReference, l, 0);
    
    return paramPage;
  }
  
  protected Object put(Page paramPage, long paramLong, Object paramObject1, Object paramObject2)
  {
    int i = paramPage.binarySearch(paramObject1);
    if (paramPage.isLeaf())
    {
      if (i < 0)
      {
        i = -i - 1;
        paramPage.insertLeaf(i, paramObject1, paramObject2);
        return null;
      }
      return paramPage.setValue(i, paramObject2);
    }
    if (i < 0) {
      i = -i - 1;
    } else {
      i++;
    }
    Page localPage1 = paramPage.getChildPage(i).copy(paramLong);
    if ((localPage1.getMemory() > this.store.getPageSplitSize()) && (localPage1.getKeyCount() > 1))
    {
      int j = localPage1.getKeyCount() / 2;
      Object localObject2 = localPage1.getKey(j);
      Page localPage2 = localPage1.split(j);
      paramPage.setChild(i, localPage2);
      paramPage.insertNode(i, localObject2, localPage1);
      
      return put(paramPage, paramLong, paramObject1, paramObject2);
    }
    Object localObject1 = put(localPage1, paramLong, paramObject1, paramObject2);
    paramPage.setChild(i, localPage1);
    return localObject1;
  }
  
  public K firstKey()
  {
    return (K)getFirstLast(true);
  }
  
  public K lastKey()
  {
    return (K)getFirstLast(false);
  }
  
  public K getKey(long paramLong)
  {
    if ((paramLong < 0L) || (paramLong >= size())) {
      return null;
    }
    Page localPage = this.root;
    long l1 = 0L;
    for (;;)
    {
      if (localPage.isLeaf())
      {
        if (paramLong >= l1 + localPage.getKeyCount()) {
          return null;
        }
        return (K)localPage.getKey((int)(paramLong - l1));
      }
      int i = 0;int j = getChildPageCount(localPage);
      for (; i < j; i++)
      {
        long l2 = localPage.getCounts(i);
        if (paramLong < l2 + l1) {
          break;
        }
        l1 += l2;
      }
      if (i == j) {
        return null;
      }
      localPage = localPage.getChildPage(i);
    }
  }
  
  public List<K> keyList()
  {
    new AbstractList()
    {
      public K get(int paramAnonymousInt)
      {
        return (K)MVMap.this.getKey(paramAnonymousInt);
      }
      
      public int size()
      {
        return MVMap.this.size();
      }
      
      public int indexOf(Object paramAnonymousObject)
      {
        return (int)MVMap.this.getKeyIndex(paramAnonymousObject);
      }
    };
  }
  
  public long getKeyIndex(K paramK)
  {
    if (size() == 0) {
      return -1L;
    }
    Page localPage = this.root;
    long l = 0L;
    for (;;)
    {
      int i = localPage.binarySearch(paramK);
      if (localPage.isLeaf())
      {
        if (i < 0) {
          return -l + i;
        }
        return l + i;
      }
      if (i < 0) {
        i = -i - 1;
      } else {
        i++;
      }
      for (int j = 0; j < i; j++) {
        l += localPage.getCounts(j);
      }
      localPage = localPage.getChildPage(i);
    }
  }
  
  protected K getFirstLast(boolean paramBoolean)
  {
    if (size() == 0) {
      return null;
    }
    Page localPage = this.root;
    for (;;)
    {
      if (localPage.isLeaf()) {
        return (K)localPage.getKey(paramBoolean ? 0 : localPage.getKeyCount() - 1);
      }
      localPage = localPage.getChildPage(paramBoolean ? 0 : getChildPageCount(localPage) - 1);
    }
  }
  
  public K higherKey(K paramK)
  {
    return (K)getMinMax(paramK, false, true);
  }
  
  public K ceilingKey(K paramK)
  {
    return (K)getMinMax(paramK, false, false);
  }
  
  public K floorKey(K paramK)
  {
    return (K)getMinMax(paramK, true, false);
  }
  
  public K lowerKey(K paramK)
  {
    return (K)getMinMax(paramK, true, true);
  }
  
  protected K getMinMax(K paramK, boolean paramBoolean1, boolean paramBoolean2)
  {
    return (K)getMinMax(this.root, paramK, paramBoolean1, paramBoolean2);
  }
  
  private K getMinMax(Page paramPage, K paramK, boolean paramBoolean1, boolean paramBoolean2)
  {
    if (paramPage.isLeaf())
    {
      i = paramPage.binarySearch(paramK);
      if (i < 0) {
        i = -i - (paramBoolean1 ? 2 : 1);
      } else if (paramBoolean2) {
        i += (paramBoolean1 ? -1 : 1);
      }
      if ((i < 0) || (i >= paramPage.getKeyCount())) {
        return null;
      }
      return (K)paramPage.getKey(i);
    }
    int i = paramPage.binarySearch(paramK);
    if (i < 0) {
      i = -i - 1;
    } else {
      i++;
    }
    for (;;)
    {
      if ((i < 0) || (i >= getChildPageCount(paramPage))) {
        return null;
      }
      Object localObject = getMinMax(paramPage.getChildPage(i), paramK, paramBoolean1, paramBoolean2);
      if (localObject != null) {
        return (K)localObject;
      }
      i += (paramBoolean1 ? -1 : 1);
    }
  }
  
  public V get(Object paramObject)
  {
    return (V)binarySearch(this.root, paramObject);
  }
  
  protected Object binarySearch(Page paramPage, Object paramObject)
  {
    int i = paramPage.binarySearch(paramObject);
    if (!paramPage.isLeaf())
    {
      if (i < 0) {
        i = -i - 1;
      } else {
        i++;
      }
      paramPage = paramPage.getChildPage(i);
      return binarySearch(paramPage, paramObject);
    }
    if (i >= 0) {
      return paramPage.getValue(i);
    }
    return null;
  }
  
  public boolean containsKey(Object paramObject)
  {
    return get(paramObject) != null;
  }
  
  protected Page binarySearchPage(Page paramPage, Object paramObject)
  {
    int i = paramPage.binarySearch(paramObject);
    if (!paramPage.isLeaf())
    {
      if (i < 0) {
        i = -i - 1;
      } else {
        i++;
      }
      paramPage = paramPage.getChildPage(i);
      return binarySearchPage(paramPage, paramObject);
    }
    if (i >= 0) {
      return paramPage;
    }
    return null;
  }
  
  public synchronized void clear()
  {
    beforeWrite();
    this.root.removeAllRecursive();
    newRoot(Page.createEmpty(this, this.writeVersion));
  }
  
  void close()
  {
    this.closed = true;
  }
  
  public boolean isClosed()
  {
    return this.closed;
  }
  
  public V remove(Object paramObject)
  {
    beforeWrite();
    Object localObject1 = get(paramObject);
    if (localObject1 == null) {
      return null;
    }
    long l = this.writeVersion;
    synchronized (this)
    {
      Page localPage = this.root.copy(l);
      localObject1 = remove(localPage, l, paramObject);
      if ((!localPage.isLeaf()) && (localPage.getTotalCount() == 0L))
      {
        localPage.removePage();
        localPage = Page.createEmpty(this, localPage.getVersion());
      }
      newRoot(localPage);
    }
    return (V)localObject1;
  }
  
  public synchronized V putIfAbsent(K paramK, V paramV)
  {
    Object localObject = get(paramK);
    if (localObject == null) {
      put(paramK, paramV);
    }
    return (V)localObject;
  }
  
  public synchronized boolean remove(Object paramObject1, Object paramObject2)
  {
    Object localObject = get(paramObject1);
    if (areValuesEqual(localObject, paramObject2))
    {
      remove(paramObject1);
      return true;
    }
    return false;
  }
  
  public boolean areValuesEqual(Object paramObject1, Object paramObject2)
  {
    if (paramObject1 == paramObject2) {
      return true;
    }
    if ((paramObject1 == null) || (paramObject2 == null)) {
      return false;
    }
    return this.valueType.compare(paramObject1, paramObject2) == 0;
  }
  
  public synchronized boolean replace(K paramK, V paramV1, V paramV2)
  {
    Object localObject = get(paramK);
    if (areValuesEqual(localObject, paramV1))
    {
      put(paramK, paramV2);
      return true;
    }
    return false;
  }
  
  public synchronized V replace(K paramK, V paramV)
  {
    Object localObject = get(paramK);
    if (localObject != null)
    {
      put(paramK, paramV);
      return (V)localObject;
    }
    return null;
  }
  
  protected Object remove(Page paramPage, long paramLong, Object paramObject)
  {
    int i = paramPage.binarySearch(paramObject);
    Object localObject = null;
    if (paramPage.isLeaf())
    {
      if (i >= 0)
      {
        localObject = paramPage.getValue(i);
        paramPage.remove(i);
      }
      return localObject;
    }
    if (i < 0) {
      i = -i - 1;
    } else {
      i++;
    }
    Page localPage1 = paramPage.getChildPage(i);
    Page localPage2 = localPage1.copy(paramLong);
    localObject = remove(localPage2, paramLong, paramObject);
    if ((localObject == null) || (localPage2.getTotalCount() != 0L))
    {
      paramPage.setChild(i, localPage2);
    }
    else if (paramPage.getKeyCount() == 0)
    {
      paramPage.setChild(i, localPage2);
      localPage2.removePage();
    }
    else
    {
      paramPage.remove(i);
    }
    return localObject;
  }
  
  protected void newRoot(Page paramPage)
  {
    if (this.root != paramPage)
    {
      removeUnusedOldVersions();
      if (this.root.getVersion() != paramPage.getVersion())
      {
        Page localPage = (Page)this.oldRoots.peekLast();
        if ((localPage == null) || (localPage.getVersion() != this.root.getVersion())) {
          this.oldRoots.add(this.root);
        }
      }
      this.root = paramPage;
    }
  }
  
  int compare(Object paramObject1, Object paramObject2)
  {
    return this.keyType.compare(paramObject1, paramObject2);
  }
  
  public DataType getKeyType()
  {
    return this.keyType;
  }
  
  public DataType getValueType()
  {
    return this.valueType;
  }
  
  Page readPage(long paramLong)
  {
    return this.store.readPage(this, paramLong);
  }
  
  void setRootPos(long paramLong1, long paramLong2)
  {
    this.root = (paramLong1 == 0L ? Page.createEmpty(this, -1L) : readPage(paramLong1));
    this.root.setVersion(paramLong2);
  }
  
  public Iterator<K> keyIterator(K paramK)
  {
    return new Cursor(this, this.root, paramK);
  }
  
  boolean rewrite(Set<Integer> paramSet)
  {
    long l = this.store.getCurrentVersion() - 1L;
    if (l < this.createVersion) {
      return true;
    }
    MVMap localMVMap;
    try
    {
      localMVMap = openVersion(l);
    }
    catch (IllegalArgumentException localIllegalArgumentException)
    {
      return true;
    }
    try
    {
      rewrite(localMVMap.root, paramSet);
      return true;
    }
    catch (IllegalStateException localIllegalStateException)
    {
      if (DataUtils.getErrorCode(localIllegalStateException.getMessage()) == 9) {
        return false;
      }
      throw localIllegalStateException;
    }
  }
  
  private int rewrite(Page paramPage, Set<Integer> paramSet)
  {
    if (paramPage.isLeaf())
    {
      long l1 = paramPage.getPos();
      int k = DataUtils.getPageChunkId(l1);
      if (!paramSet.contains(Integer.valueOf(k))) {
        return 0;
      }
      if (paramPage.getKeyCount() > 0)
      {
        Object localObject1 = paramPage.getKey(0);
        Object localObject2 = get(localObject1);
        if (localObject2 != null) {
          replace(localObject1, localObject2, localObject2);
        }
      }
      return 1;
    }
    int i = 0;
    for (int j = 0; j < getChildPageCount(paramPage); j++)
    {
      long l3 = paramPage.getChildPagePos(j);
      if ((l3 != 0L) && (DataUtils.getPageType(l3) == 0))
      {
        int n = DataUtils.getPageChunkId(l3);
        if (!paramSet.contains(Integer.valueOf(n))) {}
      }
      else
      {
        i += rewrite(paramPage.getChildPage(j), paramSet);
      }
    }
    if (i == 0)
    {
      long l2 = paramPage.getPos();
      int m = DataUtils.getPageChunkId(l2);
      if (paramSet.contains(Integer.valueOf(m)))
      {
        Page localPage = paramPage;
        while (!localPage.isLeaf()) {
          localPage = localPage.getChildPage(0);
        }
        Object localObject3 = localPage.getKey(0);
        Object localObject4 = get(localObject3);
        if (localObject4 != null) {
          replace(localObject3, localObject4, localObject4);
        }
        i++;
      }
    }
    return i;
  }
  
  public Cursor<K, V> cursor(K paramK)
  {
    return new Cursor(this, this.root, paramK);
  }
  
  public Set<Map.Entry<K, V>> entrySet()
  {
    final MVMap localMVMap = this;
    final Page localPage = this.root;
    new AbstractSet()
    {
      public Iterator<Map.Entry<K, V>> iterator()
      {
        final Cursor localCursor = new Cursor(localMVMap, localPage, null);
        new Iterator()
        {
          public boolean hasNext()
          {
            return localCursor.hasNext();
          }
          
          public Map.Entry<K, V> next()
          {
            Object localObject = localCursor.next();
            return new DataUtils.MapEntry(localObject, localCursor.getValue());
          }
          
          public void remove()
          {
            throw DataUtils.newUnsupportedOperationException("Removing is not supported");
          }
        };
      }
      
      public int size()
      {
        return MVMap.this.size();
      }
      
      public boolean contains(Object paramAnonymousObject)
      {
        return MVMap.this.containsKey(paramAnonymousObject);
      }
    };
  }
  
  public Set<K> keySet()
  {
    final MVMap localMVMap = this;
    final Page localPage = this.root;
    new AbstractSet()
    {
      public Iterator<K> iterator()
      {
        return new Cursor(localMVMap, localPage, null);
      }
      
      public int size()
      {
        return MVMap.this.size();
      }
      
      public boolean contains(Object paramAnonymousObject)
      {
        return MVMap.this.containsKey(paramAnonymousObject);
      }
    };
  }
  
  public Page getRoot()
  {
    return this.root;
  }
  
  public String getName()
  {
    return this.store.getMapName(this.id);
  }
  
  public MVStore getStore()
  {
    return this.store;
  }
  
  public int getId()
  {
    return this.id;
  }
  
  void rollbackTo(long paramLong)
  {
    beforeWrite();
    if (paramLong > this.createVersion) {
      if (this.root.getVersion() >= paramLong) {
        for (;;)
        {
          Page localPage = (Page)this.oldRoots.peekLast();
          if (localPage == null) {
            break;
          }
          this.oldRoots.removeLast(localPage);
          this.root = localPage;
          if (this.root.getVersion() < paramLong) {
            break;
          }
        }
      }
    }
  }
  
  void removeUnusedOldVersions()
  {
    long l = this.store.getOldestVersionToKeep();
    if (l == -1L) {
      return;
    }
    Page localPage1 = (Page)this.oldRoots.peekLast();
    for (;;)
    {
      Page localPage2 = (Page)this.oldRoots.peekFirst();
      if ((localPage2 == null) || (localPage2.getVersion() >= l) || (localPage2 == localPage1)) {
        break;
      }
      this.oldRoots.removeFirst(localPage2);
    }
  }
  
  public boolean isReadOnly()
  {
    return this.readOnly;
  }
  
  public void setVolatile(boolean paramBoolean)
  {
    this.isVolatile = paramBoolean;
  }
  
  public boolean isVolatile()
  {
    return this.isVolatile;
  }
  
  protected void beforeWrite()
  {
    if (this.closed) {
      throw DataUtils.newIllegalStateException(4, "This map is closed", new Object[0]);
    }
    if (this.readOnly) {
      throw DataUtils.newUnsupportedOperationException("This map is read-only");
    }
    this.store.beforeWrite(this);
  }
  
  public int hashCode()
  {
    return this.id;
  }
  
  public boolean equals(Object paramObject)
  {
    return this == paramObject;
  }
  
  public int size()
  {
    long l = sizeAsLong();
    return l > 2147483647L ? Integer.MAX_VALUE : (int)l;
  }
  
  public long sizeAsLong()
  {
    return this.root.getTotalCount();
  }
  
  public boolean isEmpty()
  {
    return (this.root.isLeaf()) && (this.root.getKeyCount() == 0);
  }
  
  public long getCreateVersion()
  {
    return this.createVersion;
  }
  
  protected void removePage(long paramLong, int paramInt)
  {
    this.store.removePage(this, paramLong, paramInt);
  }
  
  public MVMap<K, V> openVersion(long paramLong)
  {
    if (this.readOnly) {
      throw DataUtils.newUnsupportedOperationException("This map is read-only; need to call the method on the writable map");
    }
    DataUtils.checkArgument(paramLong >= this.createVersion, "Unknown version {0}; this map was created in version is {1}", new Object[] { Long.valueOf(paramLong), Long.valueOf(this.createVersion) });
    
    Object localObject1 = null;
    
    Page localPage1 = this.root;
    if ((paramLong >= localPage1.getVersion()) && ((paramLong == this.writeVersion) || (localPage1.getVersion() >= 0L) || (paramLong <= this.createVersion) || (this.store.getFileStore() == null)))
    {
      localObject1 = localPage1;
    }
    else
    {
      localObject2 = (Page)this.oldRoots.peekFirst();
      if ((localObject2 == null) || (paramLong < ((Page)localObject2).getVersion())) {
        return this.store.openMapVersion(paramLong, this.id, this);
      }
      Iterator localIterator = this.oldRoots.iterator();
      while (localIterator.hasNext())
      {
        Page localPage2 = (Page)localIterator.next();
        if (localPage2.getVersion() > paramLong) {
          break;
        }
        localObject2 = localPage2;
      }
      localObject1 = localObject2;
    }
    Object localObject2 = openReadOnly();
    ((MVMap)localObject2).root = ((Page)localObject1);
    return (MVMap<K, V>)localObject2;
  }
  
  MVMap<K, V> openReadOnly()
  {
    MVMap localMVMap = new MVMap(this.keyType, this.valueType);
    localMVMap.readOnly = true;
    HashMap localHashMap = New.hashMap();
    localHashMap.put("id", Integer.valueOf(this.id));
    localHashMap.put("createVersion", Long.valueOf(this.createVersion));
    localMVMap.init(this.store, localHashMap);
    localMVMap.root = this.root;
    return localMVMap;
  }
  
  public long getVersion()
  {
    return this.root.getVersion();
  }
  
  protected int getChildPageCount(Page paramPage)
  {
    return paramPage.getRawChildPageCount();
  }
  
  public String getType()
  {
    return null;
  }
  
  String asString(String paramString)
  {
    StringBuilder localStringBuilder = new StringBuilder();
    if (paramString != null) {
      DataUtils.appendMap(localStringBuilder, "name", paramString);
    }
    if (this.createVersion != 0L) {
      DataUtils.appendMap(localStringBuilder, "createVersion", Long.valueOf(this.createVersion));
    }
    String str = getType();
    if (str != null) {
      DataUtils.appendMap(localStringBuilder, "type", str);
    }
    return localStringBuilder.toString();
  }
  
  void setWriteVersion(long paramLong)
  {
    this.writeVersion = paramLong;
  }
  
  void copyFrom(MVMap<K, V> paramMVMap)
  {
    beforeWrite();
    newRoot(copy(paramMVMap.root, null));
  }
  
  private Page copy(Page paramPage, CursorPos paramCursorPos)
  {
    Page localPage1 = Page.create(this, this.writeVersion, paramPage);
    if (paramPage.isLeaf())
    {
      Page localPage2 = localPage1;
      for (CursorPos localCursorPos2 = paramCursorPos; localCursorPos2 != null; localCursorPos2 = localCursorPos2.parent)
      {
        localCursorPos2.page.setChild(localCursorPos2.index, localPage2);
        localCursorPos2.page = localCursorPos2.page.copy(this.writeVersion);
        localPage2 = localCursorPos2.page;
        if (localCursorPos2.parent == null)
        {
          newRoot(localCursorPos2.page);
          beforeWrite();
        }
      }
    }
    else
    {
      for (int i = 0; i < getChildPageCount(localPage1); i++) {
        localPage1.setChild(i, null);
      }
      CursorPos localCursorPos1 = new CursorPos(localPage1, 0, paramCursorPos);
      for (int j = 0; j < getChildPageCount(localPage1); j++)
      {
        localCursorPos1.index = j;
        long l = paramPage.getChildPagePos(j);
        if (l != 0L) {
          copy(paramPage.getChildPage(j), localCursorPos1);
        }
      }
      localPage1 = localCursorPos1.page;
    }
    return localPage1;
  }
  
  public String toString()
  {
    return asString(null);
  }
  
  public static class Builder<K, V>
    implements MVMap.MapBuilder<MVMap<K, V>, K, V>
  {
    protected DataType keyType;
    protected DataType valueType;
    
    public Builder<K, V> keyType(DataType paramDataType)
    {
      this.keyType = paramDataType;
      return this;
    }
    
    public DataType getKeyType()
    {
      return this.keyType;
    }
    
    public DataType getValueType()
    {
      return this.valueType;
    }
    
    public Builder<K, V> valueType(DataType paramDataType)
    {
      this.valueType = paramDataType;
      return this;
    }
    
    public MVMap<K, V> create()
    {
      if (this.keyType == null) {
        this.keyType = new ObjectDataType();
      }
      if (this.valueType == null) {
        this.valueType = new ObjectDataType();
      }
      return new MVMap(this.keyType, this.valueType);
    }
  }
  
  public static abstract interface MapBuilder<M extends MVMap<K, V>, K, V>
  {
    public abstract M create();
  }
}
